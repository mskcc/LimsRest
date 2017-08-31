package org.mskcc.limsrest.limsapi;


import com.velox.api.datarecord.*;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;
import com.velox.sloan.cmo.utilities.UuidGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.BankedSample;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetriever;
import org.mskcc.limsrest.staticstrings.Messages;
import org.mskcc.limsrest.staticstrings.RelatedRecords;
import org.mskcc.util.VeloxConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A queued task that takes banked ids, a service id and optionally a request  and project. It will create the project and request if needed, then it transforms the banked samples to real samples
 *
 * @author Aaron Gabow
 */
@Service
public class PromoteBanked extends LimsTask {
    String[] bankedIds;
    String requestId;
    String serviceId;
    String projectId;
    String igoUser;
    boolean dryrun = false;

    private Log log = LogFactory.getLog(PromoteBanked.class);

    @Autowired
    @Qualifier("patientCmoSampleIdRetriever")
    private CmoSampleIdRetriever patientCmoSampleIdRetriever;

    @Autowired
    @Qualifier("cellLineCmoSampleIdRetriever")
    private CmoSampleIdRetriever cellLineCmoSampleIdRetriever;

    public PromoteBanked(CmoSampleIdRetriever patientCmoSampleIdRetriever, CmoSampleIdRetriever cellLineCmoSampleIdRetriever) {
        this.patientCmoSampleIdRetriever = patientCmoSampleIdRetriever;
        this.cellLineCmoSampleIdRetriever = cellLineCmoSampleIdRetriever;
    }

    public void init(String[] bankedIds, String projectId, String requestId, String serviceId, String igoUser, String dryrun) {
        this.bankedIds = bankedIds;
        this.projectId = projectId;
        this.requestId = requestId;
        this.serviceId = serviceId;
        this.igoUser = igoUser;
        if (dryrun.equals("true")) {
            this.dryrun = true;
        }
    }

    //execute the velox call
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
        if (dryrun) {
            String nextRequest = "";
            if (requestId.equals("NULL") && projectId.equals("NULL")) {
                try {
                    List<DataRecord> mappedReq = dataRecordManager.queryDataRecords("Request", "IlabRequest = '" + serviceId + "'", user);
                    if (mappedReq.size() > 0) {
                        DataRecord req = mappedReq.get(0);
                        String requestId = req.getStringVal("RequestId", user);
                        nextRequest = "Promoting to request " + requestId + " because service id matches";
                    } else {
                        nextRequest = "Promoting to a new request";
                    }
                } catch (Exception e) {
                    nextRequest = "Promoting to a new request";
                }
            } else if (!requestId.equals("NULL")) {
                nextRequest = "Promoting to existent request " + requestId;
            } else {
                nextRequest = "Promoting to a new request in project " + projectId;
            }
            return nextRequest;
        }
        try {
            List<DataRecord> validBarcodeList = dataRecordManager.queryDataRecords("IndexAssignment", "IndexType != 'IDT_TRIM'", user);
            HashMap<String, String> barcodeId2Sequence = new HashMap<>();
            for (DataRecord knownBarcode : validBarcodeList) {
                barcodeId2Sequence.put(knownBarcode.getStringVal("IndexId", user), knownBarcode.getStringVal("IndexTag", user));
            }
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bankedIds.length - 1; i++) {
                sb.append("'");
                sb.append(bankedIds[i]);
                sb.append("',");
            }
            sb.append("'");
            sb.append(bankedIds[bankedIds.length - 1]);
            sb.append("'");

            List<DataRecord> bankedList = dataRecordManager.queryDataRecords("BankedSample", "RecordId in (" + sb.toString() + ") order by transactionId, rowIndex", user);
            SloanCMOUtils util = new SloanCMOUtils(managerContext);

            DataRecord req = null;
            //THREAD WARNING: Not thread safe. Depends on the queue being single consumer thread to handle concurency
            if (requestId.equals("NULL") && projectId.equals("NULL")) {
                try {
                    List<DataRecord> mappedReq = dataRecordManager.queryDataRecords("Request", "IlabRequest = '" + serviceId + "'", user);
                    if (mappedReq.size() > 0) {
                        req = mappedReq.get(0);
                        requestId = req.getStringVal("RequestId", user);
                    } else {
                        requestId = util.getNextProjectId();
                        DataRecord proj = null;
                        List<DataRecord> projs = dataRecordManager.queryDataRecords("Directory", "DirectoryName = 'Projects'", user);
                        if (projs.size() > 0) {
                            proj = projs.get(0).addChild("Project", user);
                        } else {
                            proj = dataRecordManager.addDataRecord("Project", user);
                        }
                        proj.setDataField("ProjectId", requestId, user);
                        Map<String, Object> reqFields = new HashMap<>();
                        reqFields.put("RequestId", requestId);
                        reqFields.put("IlabRequest", serviceId);
                        reqFields.put("ProjectId", requestId);
                        req = proj.addChild("Request", reqFields, user);


                    }
                } catch (Exception e) {
                    throw new LimsException("Unable to create a new request for this project: " + e.getMessage());
                }
            } else if (!requestId.equals("NULL")) {
                List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
                if (requestList.size() == 0) {
                    throw new LimsException("There is no request with id '" + requestId + "'");
                }
                req = requestList.get(0);
            } else {
                log.info("Adding based off project id");
                List<DataRecord> projectList = dataRecordManager.queryDataRecords("Project", "ProjectId = '" + projectId + "'", user);
                if (projectList.size() == 0) {
                    throw new LimsException("There is no project with id '" + projectId + "'");
                }
                DataRecord[] allChildRequests = projectList.get(0).getChildrenOfType("Request", user);
                for (DataRecord possibleRequest : allChildRequests) {
                    String reqServiceId = "";
                    try {
                        reqServiceId = possibleRequest.getStringVal("IlabRequest", user);
                    } catch (NullPointerException npe) {
                    }
                    if (serviceId.equals(reqServiceId)) {
                        req = possibleRequest;
                        requestId = req.getStringVal("RequestId", user);
                    }
                }
                if (req == null) {
                    log.info("Adding a new request for project" + projectId);
                    try {
                        requestId = util.getNextRequestId(projectId);
                        log.info("request id " + requestId);
                        Map<String, Object> reqFields = new HashMap<>();
                        reqFields.put("RequestId", requestId);
                        reqFields.put("IlabRequest", serviceId);
                        reqFields.put("ProjectId", projectId);
                        req = projectList.get(0).addChild("Request", reqFields, user);
                    } catch (Exception e) {
                        throw new LimsException("Unable to create a new request for this project: " + e.getMessage());
                    }
                }
            }
            if (bankedList.size() == 0) {
                throw new LimsException("No banked sample with ids '" + sb.toString() + "'");
            }
            DataRecord[] existentSamples = req.getChildrenOfType("Sample", user);
            HashSet<String> existentIds = new HashSet<>();
            int maxId = 0;
            for (DataRecord e : existentSamples) {
                try {
                    String otherId = e.getStringVal("OtherSampleId", user);
                    existentIds.add(otherId);
                    String sampleId = e.getStringVal("SampleId", user);
                    String[] igoElements = unaliquotName(sampleId).split("_");
                    int currentId = Integer.parseInt(igoElements[igoElements.length - 1]);
                    if (currentId > maxId) {
                        maxId = currentId;
                    }
                } catch (NullPointerException npe) {
                }
            }
            int offset = 1;
            HashMap<String, DataRecord> plateId2Plate = new HashMap<>();
            for (DataRecord bankedSample : bankedList) {
                createRecords(bankedSample, req, requestId, barcodeId2Sequence, plateId2Plate, existentIds, maxId, offset);
                offset++;
                bankedSample.setDataField("Promoted", Boolean.TRUE, user);
                bankedSample.setDataField("RequestId", requestId, user);
            }
            dataRecordManager.storeAndCommit(igoUser + "  promoted the banked samples " + sb.toString(), user);
        } catch (InvalidValue | AlreadyExists | NotFound | IoError | RemoteException | ServerException | LimsException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage() + " TRACE: " + sw.toString());
            return Messages.ERROR_IN + " PROMOTING BANKED SAMPLE: " + e.getMessage();

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage() + " TRACE: " + sw.toString());
            return Messages.ERROR_IN + " PROMOTING BANKED SAMPLE: " + e.toString() + ": " + e.getMessage();
        }
        return Messages.SUCCESS;
    }

    public void createRecords(DataRecord bankedSample, DataRecord req, String requestId, HashMap<String, String> barcodeId2Sequence, HashMap<String, DataRecord> plateId2Plate, HashSet<String> existentIds, int maxExistentId, int offset) throws LimsException, InvalidValue, AlreadyExists, NotFound, IoError, RemoteException, ServerException {
        SloanCMOUtils util = new SloanCMOUtils(managerContext);
        Map<String, Object> bankedFields = bankedSample.getFields(user);
        if (bankedFields.get("Promoted") != null && (boolean) bankedFields.get("Promoted")) {
            throw new LimsException("Trying to promote a banked sample that has already been promoted to sample");
        }

        String otherSampleId = getCmoSampleId(bankedSample, bankedFields);
        bankedSample.setDataField(VeloxConstants.OTHER_SAMPLE_ID, otherSampleId, user);

        if (existentIds.contains(otherSampleId)) {
            throw new LimsException("There already is a sample in the project with the name " + otherSampleId);
        }
        log.info("uuiding");
        UuidGenerator uuidGen = new UuidGenerator();
        String uuid;
        try {
            uuid = uuidGen.integerToUUID(Integer.parseInt(util.getNextBankedId()), 32);
        } catch (IOException e) {
            throw new LimsException("UUID generation failed for sample due to IOException " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new LimsException("UUID generation failed for sample due to ClassNotFoundException " + e.getMessage());
        } catch (Exception e) {
            throw new LimsException("UUID generation failed for sample due to " + e.getMessage());
        }
        //add a sample to requestList.get(0) with a new sample
        //copy fields
        String newIgoId = requestId + "_" + Integer.toString(maxExistentId + offset);
        try {
            DataRecord promotedSample = req.addChild("Sample", user);
            String barcodeId = (String) bankedFields.get("BarcodeId");
            String runType = (String) bankedFields.get("RunType");
            String plateId = (String) bankedFields.get("PlateId");
            if (runType == null) {
                runType = "";
            }
            if (bankedFields.containsKey("SampleClass")) {
                bankedFields.put("CMOSampleClass", bankedFields.get("SampleClass"));
                bankedFields.remove("SampleClass");
            }
            String requestedReads = (String) bankedFields.get("RequestedReads");
            bankedFields.remove("Promoted");
            bankedFields.remove("PlateId");
            bankedFields.remove("Investigator");
            bankedFields.remove("BarcodeId");
            bankedFields.remove("ServiceId");
            bankedFields.remove("RunType");
            bankedFields.remove("RowIndex");
            bankedFields.remove("TransactionId");
            bankedFields.remove("RequestedReads");
            //remove micronic tube
            bankedFields.remove(RelatedRecords.MICRONIC_TUBE_REF);
            bankedFields.remove("NumTubes");
            bankedFields.remove("BarcodePosition");
            bankedFields.remove("SequencingReadLength");
            bankedFields.put("ReceivedQuantity", bankedFields.get("Volume"));
            bankedFields.remove("Volume");
            bankedFields.put("ExemplarSampleType", bankedFields.get("SampleType"));
            bankedFields.remove("SampleType");

            String tissueSite = (String) bankedFields.get("TissueSite");
            bankedFields.put("TissueLocation", tissueSite);
            bankedFields.remove("TissueSite");
            log.info("using uuid:" + uuid);
            bankedFields.put("AltId", uuid);
            if (plateId != null && !plateId.equals("")) {
                DataRecord plate;
                if (plateId2Plate.containsKey(plateId)) {
                    plate = plateId2Plate.get(plateId);
                } else {
                    List<DataRecord> plateList = dataRecordManager.queryDataRecords("Plate", "PlateId = '" + plateId + "'", user);
                    if (plateList.size() == 0) {
                        plate = dataRecordManager.addDataRecord("Plate", user);
                        plate.setDataField("PlateId", plateId, user);
                        plateId2Plate.put(plateId, plate);
                    } else {
                        plate = plateList.get(0);
                        plateId2Plate.put(plateId, plate);
                    }
                }
                plate.addChild(promotedSample, user);
            }
            if (barcodeId != null && !barcodeId.equals("") && !barcodeId2Sequence.containsKey(barcodeId)) {
                throw new LimsException("The LIMS does not know about the barcode " + barcodeId + ". Please make sure to the list of barcodes is up-to-date");
            }
            bankedFields.put("SampleId", newIgoId);
            bankedFields.put("ExemplarSampleStatus", "Received");
            promotedSample.setFields(bankedFields, user);

            if (barcodeId != null && !barcodeId.equals("")) {
                Map<String, Object> bcFields = new HashMap<>();
                bcFields.put("OtherSampleId", otherSampleId);
                bcFields.put("SampleId", newIgoId);
                bcFields.put("IndexId", barcodeId);
                bcFields.put("AltId", uuid);
                bcFields.put("IndexTag", barcodeId2Sequence.get(barcodeId));
                bcFields.put("IndexPrimerVolume", 10.0);
                bcFields.put("ResuspensionBufferVolume", 10.0);
                promotedSample.addChild("IndexBarcode", bcFields, user);
            }

            if (bankedFields.containsKey("TumorOrNormal") && "Tumor".equals(bankedFields.get("TumorOrNormal"))) {
                HashMap<String, Object> pairingMap = new HashMap<>();
                pairingMap.put("TumorId", bankedFields.get("SampleId"));
                req.addChild("PairingInfo", pairingMap, user);
            }

            Map<String, Object> cmoFields = new HashMap<>();
            cmoFields.put("UserSampleID", otherSampleId);
            cmoFields.put("CorrectedCMOID", otherSampleId);
            cmoFields.put("AltId", bankedFields.get("AltId"));
            cmoFields.put("CmoPatientId", bankedFields.get("PatientId"));
            cmoFields.put("CollectionYear", bankedFields.get("CollectionYear"));
            cmoFields.put("ClinicalInfo", bankedFields.get("ClinicalInfo"));
            cmoFields.put("EstimatedPurity", bankedFields.get("EstimatedPurity"));
            cmoFields.put("Gender", bankedFields.get("Gender"));
            cmoFields.put("GeneticAlterations", bankedFields.get("GeneticAlterations"));
            cmoFields.put("PatientId", bankedFields.get("PatientId"));
            cmoFields.put("Preservation", bankedFields.get("Preservation"));
            cmoFields.put("Species", bankedFields.get("Species"));
            cmoFields.put("TumorType", bankedFields.get("TumorType"));
            cmoFields.put(VeloxConstants.OTHER_SAMPLE_ID, bankedFields.get(VeloxConstants.OTHER_SAMPLE_ID));

            promotedSample.addChild("SampleCMOInfoRecords", cmoFields, user);

            Map<String, Object> srFields = new HashMap<>();
            srFields.put("OtherSampleId", otherSampleId);
            srFields.put("SampleId", newIgoId);
            srFields.put("SequencingRunType", runType);
            if (requestedReads != null && !requestedReads.equals("") &&
                    !requestedReads.equals("<10 million") && !requestedReads.equals(">100 million") && !requestedReads.equals("Does Not Apply")) {

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
                srFields.put("RequestedReads", rrMapped);
            }
            promotedSample.addChild("SeqRequirement", srFields, user);

        } catch (NullPointerException npe) {
        }
    }

    private String getCmoSampleId(DataRecord bankedSample, Map<String, Object> bankedFields) throws NotFound, RemoteException, IoError {
        List<String> patientCmoSampleIds = getPatientCmoSampleIds(bankedSample);

        if (isCellLine(bankedFields))
            return cellLineCmoSampleIdRetriever.retrieve(bankedFields, patientCmoSampleIds);
        return patientCmoSampleIdRetriever.retrieve(bankedFields, patientCmoSampleIds);
    }

    private boolean isCellLine(Map<String, Object> bankedFields) {
        return SpecimenType.fromValue(String.valueOf(bankedFields.get(BankedSample.SPECIMEN_TYPE))) == SpecimenType.CELLLINE;
    }

    private List<String> getPatientCmoSampleIds(DataRecord bankedSample) throws NotFound, RemoteException, IoError {
        String patientId = bankedSample.getStringVal("PatientId", user);
        List<DataRecord> sampleCmoInfoRecords = dataRecordManager.queryDataRecords(VeloxConstants.SAMPLE_CMO_INFO_RECORDS, "PatientId = '" + patientId + "'", user);
        List<String> patientCmoSampleIds = new ArrayList<>();

        for (DataRecord record : sampleCmoInfoRecords) {
            String cmoSampleId = record.getStringVal("CorrectedCMOID", user);
            patientCmoSampleIds.add(cmoSampleId);
        }
        return patientCmoSampleIds;
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
}
