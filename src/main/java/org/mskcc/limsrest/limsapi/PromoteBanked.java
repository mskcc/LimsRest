package org.mskcc.limsrest.limsapi;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.velox.api.datarecord.*;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.*;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;
import com.velox.sloan.cmo.utilities.UuidGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.PairingInfo;
import org.mskcc.domain.Request;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.limsapi.cmoinfo.CorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.promote.BankedSampleToSampleConverter;
import org.mskcc.limsrest.staticstrings.Constants;
import org.mskcc.limsrest.staticstrings.Messages;
import org.mskcc.util.CommonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A queued task that takes banked ids, a service id and optionally a request  and project. It will create the
 * project and request if needed, then it transforms the banked samples to real samples
 *
 * @author Aaron Gabow
 */
@Service
public class PromoteBanked extends LimsTask {
    private static final Log log = LogFactory.getLog(PromoteBanked.class);

    private final CorrectedCmoIdConverter<BankedSample> bankedSampleToCorrectedCmoSampleIdConverter;
    private final CorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator;
    private final HumanSamplePredicate humanSamplePredicate = new HumanSamplePredicate();
    private final BankedSampleToSampleConverter bankedSampleToSampleConverter;
    private String[] bankedIds;
    private String requestId;
    private String serviceId;
    private String projectId;
    private String igoUser;
    private boolean dryrun = false;
    private Multimap<String, String> errors = HashMultimap.create();

    public PromoteBanked(CorrectedCmoIdConverter<BankedSample> bankedSampleToCorrectedCmoSampleIdConverter,
                         CorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator,
                         BankedSampleToSampleConverter bankedSampleToSampleConverter) {
        this.bankedSampleToCorrectedCmoSampleIdConverter = bankedSampleToCorrectedCmoSampleIdConverter;
        this.correctedCmoSampleIdGenerator = correctedCmoSampleIdGenerator;
        this.bankedSampleToSampleConverter = bankedSampleToSampleConverter;
    }

    public void init(String[] bankedIds, String projectId, String requestId, String serviceId, String igoUser, String
            dryrun) {
        this.bankedIds = bankedIds;
        this.requestId = requestId;
        this.projectId = projectId;
        this.serviceId = serviceId;
        this.igoUser = igoUser;
        if (dryrun.equals("true")) {
            this.dryrun = true;
        }
    }

    public static enum PromotedDestination{ NEW_PROJECT_AND_REQ, EXISTING_PROJECT, EXISTING_REQ }

    //execute the velox call
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<String> execute(VeloxConnection conn) {
        List<DataRecord> mappedRequest = getMappedRequestId(serviceId);
        PromotedDestination destination = determinePromoteDestination(projectId, requestId, mappedRequest);
        if (dryrun) {
            String nextRequest = "";
            if(destination == PromotedDestination.NEW_PROJECT_AND_REQ){
                nextRequest = "Promoting to a new request";
            } else if (destination == PromotedDestination.EXISTING_PROJECT){
                nextRequest = "Promoting to a new request in project " + projectId;
            } else if(destination == PromotedDestination.EXISTING_REQ && mappedRequest.size() != 0) {
                nextRequest = "Promoting to request " + requestId + " because service id matches";
            } else if(destination == PromotedDestination.EXISTING_REQ ) {
                nextRequest = "Promoting to existent request " + requestId;
            }
            return ResponseEntity.ok(nextRequest);
        }
        try {
            List<DataRecord> validBarcodeList = dataRecordManager.queryDataRecords(IndexAssignmentModel.DATA_TYPE_NAME,
                    IndexAssignmentModel.INDEX_TYPE + " != 'IDT_TRIM'", user);
            Map<String, String> barcodeId2Sequence = new HashMap<>();
            for (DataRecord knownBarcode : validBarcodeList) {
                barcodeId2Sequence.put(knownBarcode.getStringVal(IndexAssignmentModel.INDEX_ID, user),
                        knownBarcode.getStringVal(IndexAssignmentModel.INDEX_TAG, user));
            }
            DataRecord req = findOrCreateRequest(serviceId, projectId, requestId, mappedRequest, destination);
            //in case request id was not provided
            requestId = req.getStringVal(RequestModel.REQUEST_ID, user);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bankedIds.length - 1; i++) {
                sb.append("'");
                sb.append(bankedIds[i]);
                sb.append("',");
            }
            sb.append("'");
            sb.append(bankedIds[bankedIds.length - 1]);
            sb.append("'");

            List<DataRecord> bankeListInSubmitOrder = dataRecordManager.queryDataRecords("BankedSample",  "RecordId in (" + sb
                    .toString() + ") order by transactionId, rowIndex", user);

            if (bankeListInSubmitOrder.size() == 0) {
                throw new LimsException("No banked sample with ids '" + sb.toString() + "'");
            }
            DataRecord[] existentSamples = req.getChildrenOfType(Sample.DATA_TYPE_NAME, user);
            Set<String> existentIds = new HashSet<>();
            int maxId = 0;
            for (DataRecord e : existentSamples) {
                try {
                    String otherId = e.getStringVal(Sample.OTHER_SAMPLE_ID, user);
                    existentIds.add(otherId);
                    String sampleId = e.getStringVal(Sample.SAMPLE_ID, user);
                    String[] igoElements = unaliquotName(sampleId).split("_");
                    int currentId = Integer.parseInt(igoElements[igoElements.length - 1]);
                    if (currentId > maxId) {
                        maxId = currentId;
                    }
                } catch (NullPointerException npe) {}
            }
            int offset = 1;
            Map<String, DataRecord> plateId2Plate = new HashMap<>();
            for (DataRecord bankedSample : bankeListInSubmitOrder) {
                createRecords(bankedSample, req, requestId, barcodeId2Sequence, plateId2Plate, existentIds, maxId,
                        offset);
                offset++;
                bankedSample.setDataField(BankedSample.PROMOTED, Boolean.TRUE, user);
                bankedSample.setDataField(BankedSample.REQUEST_ID, requestId, user);
            }
            dataRecordManager.storeAndCommit(igoUser + "  promoted the banked samples " + sb.toString(), user);
        } catch (InvalidValue | AlreadyExists | NotFound | IoError | RemoteException | ServerException |
                LimsException | CommonUtils.NullOrEmptyException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage() + " TRACE: " + sw.toString());

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(Constants.ERRORS, Messages.ERROR_IN + " PROMOTING BANKED SAMPLE: " + e.getMessage());

            return new ResponseEntity<>(headers, HttpStatus.OK);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage() + " TRACE: " + sw.toString());

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add(Constants.ERRORS, Messages.ERROR_IN + " PROMOTING BANKED SAMPLE: " + e.toString() + ": " + e
                    .getMessage());

            return new ResponseEntity<>(headers, HttpStatus.OK);
        }

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(Constants.WARNINGS, getErrors());
        headers.add(Constants.STATUS, Messages.SUCCESS);

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    private String getErrors() {
        StringBuilder message = new StringBuilder();

        for (String sampleId : errors.keySet()) {
            Collection<String> errorMessages = errors.get(sampleId);
            message.append(String.format("Banked sample %s errors: \n%s\n", sampleId,
                    StringUtils.join(errorMessages, System.lineSeparator())));
        }

        return message.toString();
    }

    public void logBankedState(DataRecord bankedSample, StringBuilder logBuilder, AuditLog auditLog) throws RemoteException{
        Map<String, Object> fields = bankedSample.getFields(user);
        for(Map.Entry<String, Object> entry : fields.entrySet()){
           if(entry.getValue() != null &&  !"".equals(entry.getValue())){
              logBuilder.append("EDITING field: ").append(entry.getKey()).append(" to ").append(entry.getValue());
              auditLog.logInfo(logBuilder.toString(), bankedSample, user);
              logBuilder.setLength(0);
           }
        }
    }

    public void createRecords(DataRecord bankedSampleRecord, DataRecord req, String requestId, Map<String,
            String> barcodeId2Sequence, Map<String, DataRecord> plateId2Plate, Set<String> existentIds, int
                                      maxExistentId, int offset) throws LimsException, InvalidValue, AlreadyExists,
            NotFound, IoError,
            RemoteException, ServerException {
        try{
            AuditLog auditLog = user.getAuditLog();
            StringBuilder logBuilder = new StringBuilder();
            logBankedState(bankedSampleRecord, logBuilder,  auditLog);
        } catch(RemoteException rme){
            log.info("ERROR: could not add the audit log information for promote");
        }
        SloanCMOUtils util = new SloanCMOUtils(managerContext);
        Map<String, Object> bankedFields = bankedSampleRecord.getFields(user);
        BankedSample bankedSample = new BankedSample((String) bankedFields.get(BankedSample.USER_SAMPLE_ID),
                bankedFields);

        if (bankedSample.getPromoted()) {
            throw new LimsException(String.format("Trying to promote a banked sample: %s that has already been " +
                    "promoted to sample", bankedSample.getUserSampleID()));
        }

        String correctedCmoSampleId = getCorrectedCmoSampleId(bankedSample, requestId);
        String otherSampleId = bankedSample.getOtherSampleId();

        log.debug(String.format("Generated corrected cmo id: %s", correctedCmoSampleId));

        if (existentIds.contains(otherSampleId)) {
            throw new LimsException("There already is a sample in the project with the name: " + otherSampleId);
        }
        log.info("uuiding");
        UuidGenerator uuidGen = new UuidGenerator();
        String uuid;
        try {
            uuid = uuidGen.integerToUUID(Integer.parseInt(util.getNextBankedId()), 32);
        } catch (IOException e) {
            throw new LimsException("UUID generation failed for sample due to IOException " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new LimsException("UUID generation failed for sample due to ClassNotFoundException " + e.getMessage
                    ());
        } catch (Exception e) {
            throw new LimsException("UUID generation failed for sample due to " + e.getMessage());
        }
        //add a sample to requestList.get(0) with a new sample
        //copy fields
        String newIgoId = requestId + "_" + Integer.toString(maxExistentId + offset);
        try {
            DataRecord promotedSampleRecord = req.addChild("Sample", user);
            String barcodeId = bankedSample.getBarcodeId();
            String runType = bankedSample.getRunType();
            String plateId = bankedSample.getPlateId();
            if (runType == null) {
                runType = "";
            }
            Sample promotedSample = getPromotedSample(bankedSample, uuid, newIgoId, requestId);
            String requestedReads = bankedSample.getRequestedReads();

            promotedSampleRecord.setFields(promotedSample.getFields(), user);

            if (plateId != null && !plateId.equals("")) {
                DataRecord plate;
                if (plateId2Plate.containsKey(plateId)) {
                    plate = plateId2Plate.get(plateId);
                } else {
                    List<DataRecord> plateList = dataRecordManager.queryDataRecords(PlateModel.DATA_TYPE_NAME,
                            PlateModel.PLATE_ID + " = '" + plateId  + "'", user);
                    if (plateList.size() == 0) {
                        plate = dataRecordManager.addDataRecord(PlateModel.DATA_TYPE_NAME, user);
                        plate.setDataField(PlateModel.PLATE_ID, plateId, user);
                        plateId2Plate.put(plateId, plate);
                    } else {
                        plate = plateList.get(0);
                        plateId2Plate.put(plateId, plate);
                    }
                }
                plate.addChild(promotedSampleRecord, user);
            }
            if (barcodeId != null && !barcodeId.equals("") && !barcodeId2Sequence.containsKey(barcodeId)) {
                throw new LimsException("The LIMS does not know about the barcode " + barcodeId + ". Please make sure" +
                        " to the list of barcodes is up-to-date");
            }

            if (barcodeId != null && !barcodeId.equals("")) {
                Map<String, Object> bcFields = new HashMap<>();
                bcFields.put(BankedSample.OTHER_SAMPLE_ID, otherSampleId);
                bcFields.put("SampleId", newIgoId);
                bcFields.put(BankedSample.BARCODE_ID, barcodeId);
                bcFields.put("AltId", uuid);
                bcFields.put("IndexTag", barcodeId2Sequence.get(barcodeId));
                bcFields.put("IndexPrimerVolume", 10.0);
                bcFields.put("ResuspensionBufferVolume", 10.0);
                promotedSampleRecord.addChild(IndexBarcodeModel.DATA_TYPE_NAME, bcFields, user);
            }

            if (bankedFields.containsKey(BankedSample.TUMOR_OR_NORMAL) &&
                    "Tumor".equals(bankedFields.get(BankedSample.TUMOR_OR_NORMAL))) {
                HashMap<String, Object> pairingMap = new HashMap<>();
                pairingMap.put(PairingInfoModel.TUMOR_ID, bankedFields.get("SampleId"));
                req.addChild( PairingInfoModel.DATA_TYPE_NAME, pairingMap, user);
            }

            Map<String, Object> cmoFields = getCmoFields(bankedFields, correctedCmoSampleId, requestId, newIgoId, uuid);
            promotedSampleRecord.addChild(SampleCMOInfoRecordsModel.DATA_TYPE_NAME, cmoFields, user);

            Map<String, Object> srFields = new HashMap<>();
            srFields.put(SeqRequirementModel.OTHER_SAMPLE_ID, otherSampleId);
            srFields.put(SeqRequirementModel.SAMPLE_ID, newIgoId);
            srFields.put(SeqRequirementModel.SEQUENCING_RUN_TYPE, runType);
            if (requestedReads != null && !requestedReads.equals("") &&
                    !requestedReads.equals("<10 million") && !requestedReads.equals(">100 million") &&
                    !requestedReads.equals("Does Not Apply")) {

                Double rrMapped;

                Pattern depthPattern = Pattern.compile("([0-9]+)[xX]");
                Matcher depthMatch = depthPattern.matcher(requestedReads);
                if (requestedReads.equals("MiSeq-SingleRun")) {
                    rrMapped = 0.0;
                } else if (!depthMatch.find()) {
                    requestedReads = requestedReads.split("-")[0].trim();
                    rrMapped = Double.parseDouble(requestedReads);
                } else { //the value is expressed as a coverage
                    rrMapped = Double.parseDouble(depthMatch.group(1));
                }
                srFields.put(SeqRequirementModel.REQUESTED_READS, rrMapped);
            }
            promotedSampleRecord.addChild(SeqRequirementModel.DATA_TYPE_NAME, srFields, user);

        } catch (NullPointerException npe) {}
    }

    private Sample getPromotedSample(BankedSample bankedSample, String uuid, String newIgoId, String
            assignedRequestId) {
        return bankedSampleToSampleConverter.convert(bankedSample, uuid, newIgoId, assignedRequestId);
    }

    private Map<String, Object> getCmoFields(Map<String, Object> bankedFields, String correctedCmoSampleId, String
            assignedRequestId, String igoId, String uuid) {
        Map<String, Object> cmoFields = new HashMap<>();
        cmoFields.put(CmoSampleInfo.ALT_ID, uuid);
        cmoFields.put(CmoSampleInfo.CLINICAL_INFO, bankedFields.get(BankedSample.CLINICAL_INFO));
        cmoFields.put(CmoSampleInfo.CMO_PATIENT_ID, bankedFields.get(BankedSample.CMO_PATIENT_ID));
        cmoFields.put(CmoSampleInfo.CMOSAMPLE_CLASS, bankedFields.get(BankedSample.SAMPLE_CLASS));
        cmoFields.put(CmoSampleInfo.COLLECTION_YEAR, bankedFields.get(BankedSample.COLLECTION_YEAR));
        cmoFields.put(CmoSampleInfo.CORRECTED_CMOID, correctedCmoSampleId);

        cmoFields.put(CmoSampleInfo.CORRECTED_INVEST_PATIENT_ID, bankedFields.get(BankedSample.PATIENT_ID));

        cmoFields.put(CmoSampleInfo.DMPLIBRARY_INPUT, bankedFields.getOrDefault(BankedSample.NON_LIMS_LIBRARY_INPUT,
                ""));
        cmoFields.put(CmoSampleInfo.DMPLIBRARY_OUTPUT, bankedFields.getOrDefault(BankedSample.NON_LIMS_LIBRARY_OUTPUT, ""));

        cmoFields.put(CmoSampleInfo.ESTIMATED_PURITY, bankedFields.get(BankedSample.ESTIMATED_PURITY));
        cmoFields.put(CmoSampleInfo.GENDER, bankedFields.get(BankedSample.GENDER));
        cmoFields.put(CmoSampleInfo.GENETIC_ALTERATIONS, bankedFields.get(BankedSample.GENETIC_ALTERATIONS));
        cmoFields.put(CmoSampleInfo.NORMALIZED_PATIENT_ID, bankedFields.get(BankedSample.NORMALIZED_PATIENT_ID));
        cmoFields.put(CmoSampleInfo.OTHER_SAMPLE_ID, bankedFields.get(BankedSample.USER_SAMPLE_ID));
        cmoFields.put(CmoSampleInfo.PATIENT_ID, bankedFields.get(BankedSample.PATIENT_ID));
        cmoFields.put(CmoSampleInfo.PRESERVATION, bankedFields.get(BankedSample.PRESERVATION));

        cmoFields.put(CmoSampleInfo.REQUEST_ID, assignedRequestId);
        cmoFields.put(CmoSampleInfo.SAMPLE_ID, igoId);

        cmoFields.put(CmoSampleInfo.SAMPLE_ORIGIN, bankedFields.get(BankedSample.SAMPLE_ORIGIN));
        cmoFields.put(CmoSampleInfo.SPECIES, bankedFields.get(BankedSample.SPECIES));
        cmoFields.put(CmoSampleInfo.SPECIMEN_TYPE, bankedFields.get(BankedSample.SPECIMEN_TYPE));

        cmoFields.put(CmoSampleInfo.TISSUE_LOCATION, bankedFields.get(BankedSample.TISSUE_SITE));

        cmoFields.put(CmoSampleInfo.TUMOR_OR_NORMAL, bankedFields.get(BankedSample.TUMOR_OR_NORMAL));

        cmoFields.put(CmoSampleInfo.TUMOR_TYPE, bankedFields.get(BankedSample.TUMOR_TYPE));
        cmoFields.put(CmoSampleInfo.USER_SAMPLE_ID, bankedFields.get(BankedSample.USER_SAMPLE_ID));

        return cmoFields;
    }

    private String getCorrectedCmoSampleId(BankedSample bankedSample, String requestId) {
        try {
            if (!isHumanSample(bankedSample)) {
                log.info(String.format("Non-Human sample: %s with species: %s won't have cmo sample id generated.",
                        bankedSample.getUserSampleID(), bankedSample.getSpecies()));

                return "";
            }

            CorrectedCmoSampleView correctedCmoSampleView = createFrom(bankedSample);

            String cmoSampleId = correctedCmoSampleIdGenerator.generate(correctedCmoSampleView, requestId,
                    dataRecordManager, user);

            log.info(String.format("Generated CMO Sample id for banked sample with id: %s (%s) is: %s", bankedSample
                    .getUserSampleID(), bankedSample.getOtherSampleId(), cmoSampleId));
            return cmoSampleId;
        } catch (Exception e) {
            String message = String.format("Corrected cmo id autogeneration failed for banked sample: %s",
                    bankedSample.getUserSampleID());
            log.warn(message, e);
            errors.put(bankedSample.getUserSampleID(), String.format("%s. Cause: %s", message, e.getMessage()));

            return "";
        }
    }

    private boolean isHumanSample(BankedSample bankedSample) {
        return humanSamplePredicate.test(bankedSample);
    }

    private CorrectedCmoSampleView createFrom(BankedSample bankedSample) throws
            LimsException {
        return bankedSampleToCorrectedCmoSampleIdConverter.convert(bankedSample);
    }

    /**
     * Takes a sample id and removes the underscores and numbers indicating aliquots for the sample.
     * This method leaves the underscore number and possible letter associated with the core CMO id.
     *
     * @param sampleName
     * @return the sampleName with aliquot indicators stripped
     */
    public String unaliquotName(String sampleName) {
        Pattern endPattern = Pattern.compile("(_[0-9]+)[0-9_]*$");
        Matcher endMatch = endPattern.matcher(sampleName);
        if (!endMatch.find()) {
            return sampleName;
        }

        return sampleName.replaceFirst("(_[0-9]+)[0-9_]*$", endMatch.group(1));
    }

    private List<DataRecord> getMappedRequestId(String serviceId){
        try {
            return dataRecordManager.queryDataRecords(RequestModel.DATA_TYPE_NAME,
                    RequestModel.ILAB_REQUEST + " = '" + serviceId + "'", user);
        } catch (Exception e){
            log.warn("Having trouble checking for existing service id mapping", e);
        }
        return Collections.<DataRecord>emptyList();
    }

    protected static PromotedDestination determinePromoteDestination(String projectId, String requestId,
                                                            List<? extends Object> mappedRequest){
        if (requestId.equals("NULL") && projectId.equals("NULL")) {
            try {
                if (mappedRequest.size() > 0) {
                    return PromotedDestination.EXISTING_REQ;
                } else {
                    return PromotedDestination.NEW_PROJECT_AND_REQ;
                }
            } catch (Exception e) {
                return PromotedDestination.NEW_PROJECT_AND_REQ;
            }
        } else if (!requestId.equals("NULL")) {
            return PromotedDestination.EXISTING_REQ;
        } else {
            return PromotedDestination.EXISTING_PROJECT;
        }
    }

    private DataRecord findOrCreateRequest(String serviceId, String projectId, String requestId,
                                           List<DataRecord> mappedRequest, PromotedDestination destination) throws LimsException {
        DataRecord req = null;
        if(destination == PromotedDestination.EXISTING_REQ && mappedRequest.size() != 0) {
            req = mappedRequest.get(0);
        } else if(destination == PromotedDestination.EXISTING_REQ){
            try {
                List<DataRecord> requestList = dataRecordManager.queryDataRecords(RequestModel.DATA_TYPE_NAME,
                        RequestModel.REQUEST_ID + " = '" + requestId + "'", user);
                if (requestList.size() == 0) {
                    throw new LimsException("There is no request with id '" + requestId + "'");
                }
                req = requestList.get(0);
            }catch (IoError | RemoteException | NotFound  e){
                throw new LimsException("Could not access the request with id " + requestId);
            }
        } else if(destination == PromotedDestination.NEW_PROJECT_AND_REQ ){
            try{
                SloanCMOUtils util = new SloanCMOUtils(managerContext);
                requestId = util.getNextProjectId();
                DataRecord proj = null;
                List<DataRecord> projs = dataRecordManager.queryDataRecords(DirectoryModel.DATA_TYPE_NAME,
                        DirectoryModel.DIRECTORY_NAME  + " = 'Projects'", user);
                if (projs.size() > 0) {
                    proj = projs.get(0).addChild(ProjectModel.DATA_TYPE_NAME, user);
                } else {
                    proj = dataRecordManager.addDataRecord(ProjectModel.DATA_TYPE_NAME, user);
                }
                proj.setDataField(ProjectModel.PROJECT_ID, requestId, user);
                Map<String, Object> reqFields = new HashMap<>();
                reqFields.put(RequestModel.REQUEST_ID, requestId);
                reqFields.put(RequestModel.ILAB_REQUEST, serviceId);
                reqFields.put(RequestModel.PROJECT_ID, requestId);
                req = proj.addChild(RequestModel.DATA_TYPE_NAME, reqFields, user);
            } catch (Exception e) {
                throw new LimsException("Unable to create a new request for this project: " + e.getMessage());
            }
        } else if(destination == PromotedDestination.EXISTING_PROJECT){
            log.info("Adding based off project id: " + projectId);
            //a mapping to a new request for the project can happen from multiple promotes, so be careful not to
            // overcreate requests
            try {
                List<DataRecord> projectList = dataRecordManager.queryDataRecords(ProjectModel.DATA_TYPE_NAME,
                        ProjectModel.PROJECT_ID + " = '" + projectId + "'", user);
                if (projectList.size() == 0) {
                    throw new LimsException("There is no project with id '" + projectId + "'");
                }
                DataRecord[] allChildRequests = projectList.get(0).getChildrenOfType(RequestModel.DATA_TYPE_NAME, user);
                for (DataRecord possibleRequest : allChildRequests) {
                    String reqServiceId = "";
                    try {
                        reqServiceId = possibleRequest.getStringVal(RequestModel.ILAB_REQUEST, user);
                    } catch (NullPointerException npe) {
                    }
                    if (serviceId.equals(reqServiceId)) {
                        req = possibleRequest;
                    }
                }
                if (req == null) {
                    log.info("Adding a new request for project: " + projectId);
                    try {
                        SloanCMOUtils util = new SloanCMOUtils(managerContext);
                        requestId = util.getNextRequestId(projectId);
                        log.info("request id " + requestId);
                        Map<String, Object> reqFields = new HashMap<>();
                        reqFields.put(RequestModel.REQUEST_ID, requestId);
                        reqFields.put(RequestModel.ILAB_REQUEST, serviceId);
                        reqFields.put(RequestModel.PROJECT_ID, projectId);
                        req = projectList.get(0).addChild(RequestModel.DATA_TYPE_NAME, reqFields, user);
                    } catch (Exception e) {
                        throw new LimsException("Unable to create a new request for this project: " + e.getMessage());
                    }
                }
            } catch (NotFound | IoError | RemoteException e){
                throw new LimsException("Unable to access the project: " + projectId + " in the lims: " + e.getMessage());
            }
        }
        return  req;
    }
}
