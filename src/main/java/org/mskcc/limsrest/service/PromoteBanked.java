package org.mskcc.limsrest.service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;
import com.velox.sloan.cmo.utilities.UuidGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.service.cmoinfo.SampleTypeCorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.service.cmoinfo.converter.BankedSampleToCorrectedCmoSampleIdConverter;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.service.promote.BankedSampleToSampleConverter;
import org.mskcc.limsrest.util.Constants;
import org.mskcc.limsrest.util.Messages;
import org.mskcc.limsrest.util.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;

import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mskcc.limsrest.util.Utils.getRecordStringValue;

/**
 * A queued task that takes banked ids, a service id and optionally a request  and project.
 * <BR>
 * It will create the project and request if needed, then it transforms the banked samples to real samples
 *
 * @author Aaron Gabow
 */
public class PromoteBanked extends LimsTask {
    private static final Log log = LogFactory.getLog(PromoteBanked.class);

    private static final List<String> HUMAN_RECIPES =
            Arrays.asList("IMPACT341", "IMPACT410", "IMPACT410+", "IMPACT468", "HemePACT_v3", "HemePACT_v4", "MSK-ACCESS_v1");

    private static final List<String> INDEX_MATERIALS =
            Arrays.asList("DNA Library", "Pooled Library");


    private static final HumanSamplePredicate humanSamplePredicate = new HumanSamplePredicate();
    //private static final String LIBRARY_SAMPLE_TYPE = "DNA Library";

    private final CorrectedCmoIdConverter<BankedSample> bankedSampleToCorrectedCmoSampleIdConverter = new BankedSampleToCorrectedCmoSampleIdConverter();
    //@Autowired
    private SampleTypeCorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator = new SampleTypeCorrectedCmoSampleIdGenerator();
    private final BankedSampleToSampleConverter bankedSampleToSampleConverter = new BankedSampleToSampleConverter();

    String[] bankedIds;
    String requestId;
    String serviceId;
    String projectId;
    String igoUser;
    String materials;
    boolean dryrun = false;
    private Multimap<String, String> errors = HashMultimap.create();
    private Map<Integer, Double> coverageToSeqReqWES = ImmutableMap.<Integer, Double>builder()
            .put(30, 20.0)
            .put(70, 45.0)
            .put(100, 60.0)
            .put(150, 95.0)
            .put(200, 120.0)
            .put(250, 160.0)
            .build();

    public PromoteBanked() {
    }

//    /**
//     * Determines requested reads & coverage based on,
//     *      1) BankedSample Requested Reads
//     *      2) Recipe (IMPACT, HemePACT, & ACCESS) have specific requirements
//     *
//     * Requested during LIMS meeting 2018-8-28
//     * Automated addition of sequencing requirements on promote for ACCESS. Added 2020-07-17
//     *
//     * @param recipe, "Recipe" field of BankedSample DataRecord
//     * @param tumorOrNormal, "TumorOrNormal" field of BankedSample DataRecord
//     * @param bankedSampleRequestedReads, "RequestedReads" of BankedSample DataRecord
//     */
//    public static Map<String, Number> getCovReadsRequirementsMap(String recipe, String tumorOrNormal, String bankedSampleRequestedReads) {
//        Map<String, Number> coverageReadsMap = new HashMap<>();
//
//        // Take Promoted Requested Reads from the banked sample DataRecord if available
//        double requestedReads = 0.0;
//        if (bankedSampleRequestedReads != null && !bankedSampleRequestedReads.equals("") &&
//                !bankedSampleRequestedReads.equals("<10 million") && !bankedSampleRequestedReads.equals(">100 million") &&
//                !bankedSampleRequestedReads.equals("Does Not Apply")) {
//            Double rrMapped;
//            Pattern depthPattern = Pattern.compile("([0-9]+)[xX]");
//            Matcher depthMatch = depthPattern.matcher(bankedSampleRequestedReads);
//            if (bankedSampleRequestedReads.equals("MiSeq-SingleRun")) {
//                rrMapped = 0.0;
//            } else if (!depthMatch.find()) {
//                if (bankedSampleRequestedReads.contains("-"))
//                    rrMapped = selectLarger(bankedSampleRequestedReads);
//                else
//                    rrMapped = Double.parseDouble(bankedSampleRequestedReads.trim());
//            } else { //the value is expressed as a coverage
//                rrMapped = Double.parseDouble(depthMatch.group(1));
//            }
//            requestedReads = rrMapped;
//            coverageReadsMap.put("RequestedReads", requestedReads);
//        }
//
//        if (!"Tumor".equals(tumorOrNormal) && !"Normal".equals(tumorOrNormal)) {
//            return coverageReadsMap;
//        }
//
//        // Override requestedReads & coverageTarget in coverageReadsMap for certain recipes
//        boolean normal = "Normal".equals(tumorOrNormal);
//        int coverageTarget = 0;
//        if (recipe.contains("Heme")) {
//            if (normal) {
//                coverageTarget = 250;
//                requestedReads = 10.0;
//            } else {
//                coverageTarget = 500;
//                requestedReads = 20.0;
//            }
//            log.info(String.format("Overriding Heme Request (Recipe: %s) - Coverage Target: %d, Requested Reads: %f",
//                    recipe, coverageTarget, requestedReads));
//        } else if (recipe.contains("IMPACT")) { //'M-IMPACT_v1', 'IMPACT468'
//            if (normal) {
//                coverageTarget = 250;
//                requestedReads = 7.0;
//            } else {
//                coverageTarget = 500;
//                requestedReads = 14.0;
//            }
//            log.info(String.format("Overriding IMPACT Request (Recipe: %s) - Coverage Target: %d, Requested Reads: %f",
//                    recipe, coverageTarget, requestedReads));
//        } else if (recipe.contains("ACCESS")) {
//            coverageTarget = 1000;
//            if (normal) {
//                requestedReads = 6.0;
//            } else {
//                requestedReads = 60.0;
//            }
//            log.info(String.format("Overriding ACCESS Request (Recipe: %s) - Coverage Target: %d, Requested Reads: %f",
//                    recipe, coverageTarget, requestedReads));
//        }
//        coverageReadsMap.put("CoverageTarget", coverageTarget);
//        coverageReadsMap.put("RequestedReads", requestedReads); // Update set by recipe
//
//        return coverageReadsMap;
//    }


//    /**
//     * Get Requested Reads and Coverage values from Reference values defined in LIMS DataType 'ApplicationReadCoverageRef'
//     * @param recipe
//     * @param tumorOrNormal
//     * @param panelName
//     * @param runType
//     * @param species
//     * @param applicationReadCoverageRefs
//     * @return
//     */
//    public Map<String, Object> getRequestedReadsForCoverage(Object recipe, Object tumorOrNormal, Object panelName, Object runType, Object species, Object coverage, List<DataRecord> applicationReadCoverageRefs, User limsUser){
//        Map<String, Object> readCoverage = new HashMap<>();
//        try {
//        if (Objects.isNull(recipe)){
//            log.error(String.format("Cannot set Read/Coverage values because of missing Recipe value on Banked Sample, Recipe -> %s", recipe, tumorOrNormal));
//            return readCoverage;
//        }
//        boolean foundMatch = false;
//        for (DataRecord ref : applicationReadCoverageRefs){
//            Object refRecipe = ref.getValue("PlatformApplication", limsUser);
//            Object refTumorNormal = ref.getValue("TumorNormal", limsUser);
//            Object refPanelName = ref.getValue("CapturePanel", limsUser);
//            Object refRunType = ref.getValue("SequencingRunType", limsUser);
//            Object refCoverage = ref.getValue("Coverage", limsUser) != null ? ref.getValue("Coverage", limsUser).toString().replace("X", ""): null;
//            Object refOnly = ref.getValue("ReferenceOnly", limsUser);
//            if (Objects.equals(recipe, refRecipe) && Objects.equals(tumorOrNormal, refTumorNormal) && Objects.equals(panelName, refPanelName) && Objects.equals(runType, refRunType) && Objects.equals(coverage, refCoverage)
//                    && !Objects.isNull(refOnly) && !(boolean)refOnly) {
//                foundMatch = true;
//                log.info(String.format("Found matching 'ApplicationReadCoverageRef' record with RecordId %d for given Banked Sample metadata.", ref.getRecordId()));
//                log.info("Recipe: " + recipe + ", RefVal: " + refRecipe );
//                log.info("TumorNormal: " + tumorOrNormal + ", RefVal: " + refTumorNormal );
//                log.info("PanelName: " + panelName + ", RefVal: " + refPanelName );
//                log.info("RunType: " + runType + ", RefVal: " + refRunType );
//                log.info("Coverage: " + coverage + ", RefVal: " + refCoverage );
//                readCoverage.put("SequencingRunType", refRunType);
//                readCoverage.put("CoverageTarget", refCoverage);
//                Object human = "Human";
//                Object mouse = "Mouse";
//                // populate Requested reads based on Species. ApplicationReadCoverageRef has different RequestedReads values for Human and Mouse species.
//                // default to values for Human species if Species is null.
//                if (species != null) {
//                    if (Objects.equals(human, species)) {
//                        readCoverage.put("RequestedReads", ref.getValue("MillionReadsHuman", limsUser));
//                    }
//                    if (Objects.equals(mouse, species)) {
//                        readCoverage.put("RequestedReads", ref.getValue("MillionReadsMouse", limsUser));
//                    }
//                } else {
//                    log.info("Banked Sample Species does not match 'Human' or 'Mouse'. Will apply 'Human' RequestedReads values for Coverage -> RequestedReads conversion");
//                    readCoverage.put("RequestedReads", ref.getValue("MillionReadsHuman", limsUser));
//                }
//            }
//        }
//        if (!foundMatch){
//            log.error(String.format("Cannot find matching records in 'ApplicationReadCoverageRef' using given Banked Sample metadata -> %s: %s, %s: %s, %s: %s, %s: %s, %s: %s.",
//                    "Recipe", recipe, "TumorOrNormal", tumorOrNormal, "CapturePanel", panelName, "SequencingRunType", runType, "Coverage", coverage));
//        }
//        } catch (RemoteException | NotFound e) {
//            log.info(String.format("%s error while fetching ReadCoverage values. %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
//        }
//        return readCoverage;
//    }

//
//    /**
//     * Method to check if the Requested Reads value should come from 'ApplicationReadCoverageRef' table in LIMS.
//     * @param recipe
//     * @param applicationReadCoverageRefs
//     * @return
//     */
//    public boolean needReadCoverageReference(String recipe, List<DataRecord> applicationReadCoverageRefs, User limsUser){
//        try{
//            for (DataRecord rec : applicationReadCoverageRefs){
//                Object refRecipe = rec.getValue("PlatformApplication", limsUser);
//                if (Objects.equals(refRecipe, recipe)){
//                    return true;
//                }
//            }
//        }catch (RemoteException | NotFound e) {
//            log.info(String.format("%s error while fetching ReadCoverage values. %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
//        }
//        return false;
//    }

//    /**
//     * Method to check if the BankedSample record field values have a field and value for Coverage.
//     * @param dataFields
//     * @param limsUser
//     * @return
//     */
//    private boolean hasCoverageFieldInDataType(Map<String, Object> dataFields, User limsUser){
//        try{
//            for (String field : dataFields.keySet()){
//                if (field.equalsIgnoreCase("RequestedCoverage")){
//                    return true;
//                }
//            }
//        }catch (Exception e) {
//            log.info(String.format("%s error while fetching ReadCoverage values. %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
//        }
//        log.error("Cannot find'RequestedCoverage' field definitions in 'BankedSample' DataType. Will skip Coverage -> Reads conversion.");
//        return false;
//    }


    public void init(String[] bankedIds, String projectId, String requestId, String serviceId, String igoUser, String materials, boolean dryrun) {
        this.bankedIds = bankedIds;
        this.projectId = projectId;
        this.requestId = requestId;
        this.serviceId = serviceId;
        this.igoUser = igoUser;
        this.materials = materials;
        this.dryrun = dryrun;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<String> execute(VeloxConnection conn) {
        log.info("Executing...dryrun=" + dryrun);
        if (dryrun) {
            String nextRequest = "";
            if (requestId.equals("NULL") && projectId.equals("NULL")) {
                try {
                    List<DataRecord> mappedReq = dataRecordManager.queryDataRecords("Request", "IlabRequest = '" +
                            serviceId + "'", user);
                    if (mappedReq.size() > 0) {
                        DataRecord req = mappedReq.get(0);
                        String requestId = req.getStringVal("RequestId", user);
                        nextRequest = "Would promote to Request " + requestId + " because Service ID matches.";
                    } else {
                        nextRequest = "Would promote to a new Request.";
                    }
                } catch (Exception e) {
                    nextRequest = "Would promote to a new Request.";
                }
            } else if (!requestId.equals("NULL")) {
                nextRequest = "Would promote to existing Request " + requestId;
            } else {
                nextRequest = "Would promote to a new Request in Project " + projectId;
            }

            return ResponseEntity.ok(nextRequest);
        } else {

            try {

                //GET ALL BANKED SAMPLES
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


                //GET INDEXES IF MATERIAL IS INDEX MATERIAL
                boolean indexNeeded = false;
                HashMap<String, String> barcodeId2Sequence = new HashMap<>();
                for (String material : INDEX_MATERIALS) {
                    if (materials.contains(material)) {
                        indexNeeded = true;
                        break;
                    }
                }
                log.info((indexNeeded ? "" : "No ") + "Index needed.");
                if (indexNeeded) {

                    List<DataRecord> validBarcodeList = dataRecordManager.queryDataRecords("IndexAssignment", "IndexType != " +
                            "'IDT_TRIM'", user);
                    for (DataRecord knownBarcode : validBarcodeList) {
                        barcodeId2Sequence.put(knownBarcode.getStringVal("IndexId", user), knownBarcode.getStringVal
                                ("IndexTag", user));
                    }
                }


                DataRecord req = null;
                //TODO THREAD WARNING: Not thread safe. Depends on the queue being single consumer thread to handle concurrency
                if (requestId.equals("NULL") && projectId.equals("NULL")) {
                    log.info("Creating new request.");
                    try {
                        List<DataRecord> mappedReq = dataRecordManager.queryDataRecords("Request", "IlabRequest = '" +
                                serviceId + "'", user);
                        if (mappedReq.size() > 0) {
                            req = mappedReq.get(0);
                            requestId = req.getStringVal("RequestId", user);
                        } else {
                            requestId = util.getNextProjectId();
                            DataRecord proj = null;
                            List<DataRecord> projs = dataRecordManager.queryDataRecords("Directory", "DirectoryName = " +
                                    "'Projects'", user);
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
                    log.info("Adding based off project id: " + projectId);
                    List<DataRecord> projectList = dataRecordManager.queryDataRecords("Project", "ProjectId = '" +
                            projectId + "'", user);
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
                        log.info("Adding a new request for project: " + projectId);
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
                log.info("Using request: " + req.getStringVal(("RequestId"), user));

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
                // get Coverage -> RequestedReads Reference table values from 'ApplicationReadCoverageRef' table in LIMS.
                List<DataRecord> readCoverageRefs = dataRecordManager.queryDataRecords("ApplicationReadCoverageRef", "ReferenceOnly != 1", user);
                log.info("ApplicationReadCoverageRef records: " + readCoverageRefs.size());
                for (DataRecord bankedSample : bankedList) {
                    createRecords(bankedSample, req, requestId, barcodeId2Sequence, plateId2Plate, existentIds, maxId, offset);
                    offset++;
                    bankedSample.setDataField("Promoted", Boolean.TRUE, user);
                    bankedSample.setDataField("RequestId", requestId, user);
                }
                log.info(igoUser + "  promoted the banked samples " + sb.toString());
                dataRecordManager.storeAndCommit(igoUser + "  promoted the banked samples " + sb.toString() + "into " + requestId, user);
            } catch (Exception e) {
                log.error(e);

                MultiValueMap<String, String> headers = new HttpHeaders();
                headers.add(Constants.ERRORS,
                        Messages.ERROR_IN + " PROMOTING BANKED SAMPLE: " + e.toString() + ": " + e.getMessage());

                return new ResponseEntity<>(headers, HttpStatus.OK);
            }
        }

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(Constants.WARNINGS, getErrors());
        headers.add(Constants.STATUS, Messages.SUCCESS);

        return new ResponseEntity<>("Successfully promoted sample(s) into " + requestId, headers, HttpStatus.OK );
    }

    private String getErrors() {
        StringBuilder message = new StringBuilder();

        for (String sampleId : errors.keySet()) {
            Collection<String> errorMessages = errors.get(sampleId);
            message.append(String.format("Banked sample %s errors: %s,", sampleId,
                    StringUtils.join(errorMessages, System.lineSeparator())));
        }

        return message.toString();
    }

    public void logBankedState(DataRecord bankedSample, AuditLog auditLog) throws RemoteException {
        StringBuilder logBuilder = new StringBuilder();
        Map<String, Object> fields = bankedSample.getFields(user);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() != null && !"".equals(entry.getValue())) {
                logBuilder.append("EDITING field: ").append(entry.getKey()).append(" to ").append(entry.getValue());
                auditLog.logInfo(logBuilder.toString(), bankedSample, user);
                logBuilder.setLength(0);
            }
        }
    }

    public void createRecords(DataRecord bankedSampleRecord, DataRecord req, String requestId,
                              HashMap<String, String> barcodeId2Sequence, HashMap<String, DataRecord> plateId2Plate,
                              HashSet<String> existentIds, int maxExistentId, int offset)
            throws LimsException, InvalidValue, AlreadyExists, NotFound, IoError, RemoteException, ServerException {
        try {
            AuditLog auditLog = user.getAuditLog();
            logBankedState(bankedSampleRecord, auditLog);
        } catch (RemoteException rme) {
            log.info("ERROR: could not add the audit log information for promote");
        }
        Map<String, Object> bankedFields = bankedSampleRecord.getFields(user);
        BankedSample bankedSample = new BankedSample((String) bankedFields.get(BankedSample.USER_SAMPLE_ID), bankedFields);

        if (bankedSample.getPromoted()) {
            throw new LimsException(String.format("Trying to promote a banked sample: %s that has already been " +
                    "promoted to sample", bankedSample.getUserSampleID()));
        }

        String correctedCmoSampleId = getCorrectedCmoSampleId(bankedSample, requestId);
        log.info(String.format("Generated corrected cmo id: %s", correctedCmoSampleId));

        String otherSampleId = bankedSample.getOtherSampleId();
        if (existentIds.contains(otherSampleId)) {
            throw new LimsException("There already is a sample in the project with the name: " + otherSampleId);
        }

        SloanCMOUtils util = new SloanCMOUtils(managerContext);
        log.info("uuid generating");
        UuidGenerator uuidGen = new UuidGenerator();
        String uuid;
        try {
            uuid = uuidGen.integerToUUID(Integer.parseInt(util.getNextBankedId()), 32);
        } catch (Exception e) {
            throw new LimsException("UUID generation failed for sample due to " + e.getMessage());
        }
        //add a sample to requestList.get(0) with a new sample
        //copy fields
        String newIgoId = requestId + "_" + (maxExistentId + offset);
        try {
            DataRecord promotedSampleRecord = req.addChild("Sample", user);
            String barcodeId = bankedSample.getBarcodeId();
            String runType = bankedSample.getRunType();
            String plateId = bankedSample.getPlateId();
            if (runType == null) {
                runType = "";
            }
            Sample promotedSample = getPromotedSample(bankedSample, uuid, newIgoId, requestId);
            promotedSampleRecord.setFields(promotedSample.getFields(), user);

            if (plateId != null && !plateId.equals("")) {
                DataRecord plate;
                if (plateId2Plate.containsKey(plateId)) {
                    plate = plateId2Plate.get(plateId);
                } else {
                    List<DataRecord> plateList = dataRecordManager.queryDataRecords("Plate", "PlateId = '" + plateId
                            + "'", user);
                    if (plateList.size() == 0) {
                        plate = dataRecordManager.addDataRecord("Plate", user);
                        plate.setDataField("PlateId", plateId, user);
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
                bcFields.put("OtherSampleId", otherSampleId);
                bcFields.put("SampleId", newIgoId);
                bcFields.put("IndexId", barcodeId);
                bcFields.put("AltId", uuid);
                bcFields.put("IndexTag", barcodeId2Sequence.get(barcodeId));
                bcFields.put("IndexPrimerVolume", 10.0);
                bcFields.put("ResuspensionBufferVolume", 10.0);
                promotedSampleRecord.addChild("IndexBarcode", bcFields, user);
            }

            if (bankedFields.containsKey("TumorOrNormal") && "Tumor".equals(bankedFields.get("TumorOrNormal"))) {
                HashMap<String, Object> pairingMap = new HashMap<>();
                pairingMap.put("TumorId", bankedFields.get("SampleId"));
                req.addChild("PairingInfo", pairingMap, user);
            }

            Map<String, Object> cmoFields = getCmoFields(bankedFields, correctedCmoSampleId, requestId, newIgoId, uuid);
            promotedSampleRecord.addChild("SampleCMOInfoRecords", cmoFields, user);
            Map<String, Object> seqRequirementMap = new HashMap<>();
            seqRequirementMap.put("OtherSampleId", otherSampleId);
            seqRequirementMap.put("SampleId", newIgoId);
            seqRequirementMap.put("SequencingRunType", runType);
            String recipe = (String) bankedFields.getOrDefault("Recipe", "");
            String tumorOrNormal = (String)bankedFields.getOrDefault("TumorOrNormal", null);
            Object capturePanel = bankedFields.getOrDefault("CapturePanel", null);
            Object species = bankedFields.getOrDefault("Species", null);
            Object requestedCoverage = bankedFields.getOrDefault("RequestedCoverage", null);
            String bankedSampleRequestedReads = bankedSample.getRequestedReads();

            try {
                // BankedSample values determine whether banked sample values should be re-assigned. Log for debugging
                String bankedSampleLoggedValues = String.format("Promoting w/ Banked Sample Values. Recipe: %s, TumorOrNormal: %s, CapturePanel: %s, Species: %s, RequestedCoverage: %s, Requested Reads: %s",
                        recipe,
                        tumorOrNormal == null ? "null" : tumorOrNormal,
                        capturePanel == null ? "null" : capturePanel.toString(),
                        species == null ? "null" : species.toString(),
                        requestedCoverage == null ? "null" : requestedCoverage.toString(),
                        bankedSampleRequestedReads);
                log.info(bankedSampleLoggedValues);
            } catch (Exception e){
                log.error(String.format("Failed to log Banked Sample Values: %s. Error: %s", otherSampleId, e.getMessage()));
            }

            // Take runtype from "BankedSample" record. Default to "PE100" if not present
            Object seqRunType = bankedFields.getOrDefault("RunType", null);
            if(seqRunType == null){
                seqRunType = "PE100";
            }
            seqRequirementMap.put("SequencingRunType", seqRunType);
            seqRequirementMap.put("CoverageTarget", bankedFields.getOrDefault("RequestedCoverage", null));
            seqRequirementMap.put("RequestedReads", bankedFields.getOrDefault("RequestedReads", null));
//            // Update Reads & Coverage: "CoverageTarget", "RequestedReads" for WES
//            if ("WholeExomeSequencing".equalsIgnoreCase(recipe)) {
//                seqRequirementMap.putAll(getCovReadsRequirementsMap_forWES(bankedSampleRequestedReads));
//            }

//            log.info(String.format("Sequencing Requirements before Coverage -> Reads conversion logic run: %s", seqRequirementMap.toString()));
            // Check if Coverage is a separate field in Banked Sample Datatype and Coverage->Reads conversion is required based on Recipe in Banked Sample data.
//            if (hasCoverageFieldInDataType(bankedFields, user) && needReadCoverageReference(recipe, readCoverageRefs, user)){
//                log.info("Banked Samples need Coveage mapping to reads.");
//                // if true update Reads Coverage values from the 'ApplicationReadCoverageRef' in LIMS.
//                Map<String, Object> updatedSeqRequirements = getRequestedReadsForCoverage(recipe, tumorOrNormal,
//                        capturePanel, seqRunType, species, requestedCoverage, readCoverageRefs, user);
//                if (!updatedSeqRequirements.isEmpty()) {
//                    seqRequirementMap.putAll(updatedSeqRequirements);
//                    log.info("Sequencing Requirements updated: " + seqRequirementMap.toString());
//                }
//            }
            promotedSampleRecord.addChild("SeqRequirement", seqRequirementMap, user);
        } catch (NullPointerException npe) {
        }
    }

    protected static double selectLarger(String requestedReads) {
        // example "30-40 million" => 40.0
        String[] parts = requestedReads.split("[ -]+");
        requestedReads = parts[1].trim();
        return Double.parseDouble(requestedReads);
    }

    /**
     * Determines requested reads & coverage based on requestedReads of BankedSample DataRecord and internal mapping
     * of coverage to type of sequencing run in @coverageToSeqReqWES
     *
     * @param requestedReads
     * @return
     */
    public Map<String, Object> getCovReadsRequirementsMap_forWES(String requestedReads) {
        Map<String, Object> coverageReadsMap = new HashMap<>();
        if (requestedReads != null) {
            // E.g. 70x -> 70
            Matcher depthMatch = Pattern.compile("([0-9]+)[xX]").matcher(requestedReads);
            if (depthMatch.find()) {
                Object coverageTarget = Integer.valueOf(depthMatch.group(1));
                coverageReadsMap.put("CoverageTarget", coverageTarget);
                coverageReadsMap.put("RequestedReads", coverageToSeqReqWES.getOrDefault(coverageTarget, null));
            }
        }
        return coverageReadsMap;
    }

    private void checkSampleTypeAndPatientId(BankedSample bankedSample) {
        Utils.requireNonNullNorEmpty(bankedSample.getCMOPatientId(), String.format("Cmo Patient id is empty for" +
                " banked " +
                "sample: %s", bankedSample.getId()));
        Utils.requireNonNullNorEmpty(bankedSample.getSampleType(), String.format("Sample Type is empty for " +
                "banked sample: " +
                "%s", bankedSample.getId()));
    }

    private Sample getPromotedSample(BankedSample bankedSample, String uuid, String newIgoId, String
            assignedRequestId) {
        return bankedSampleToSampleConverter.convert(bankedSample, uuid, newIgoId, assignedRequestId);
    }

    Map<String, Object> getCmoFields(Map<String, Object> bankedFields, String correctedCmoSampleId, String
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
        cmoFields.put(CmoSampleInfo.DMPLIBRARY_OUTPUT, bankedFields.getOrDefault(BankedSample
                .NON_LIMS_LIBRARY_OUTPUT, ""));

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

    String getCorrectedCmoSampleId(BankedSample bankedSample, String requestId) {
        try {
            checkSampleTypeAndPatientId(bankedSample);

            if (shouldGenerateCmoId(bankedSample)) {
                CorrectedCmoSampleView sampleView = createFrom(bankedSample);
                String cmoSampleId = correctedCmoSampleIdGenerator.generate(sampleView, requestId, dataRecordManager, user);
                log.info(String.format("Generated CMO Sample id for banked sample with id: %s (%s) is: %s", bankedSample
                        .getUserSampleID(), bankedSample.getOtherSampleId(), cmoSampleId));
                return cmoSampleId;
            } else {
                log.info(String.format("Non-Human sample: %s with species: %s won't have cmo sample id generated.",
                        bankedSample.getUserSampleID(), bankedSample.getSpecies()));
                return "";
            }
        } catch (Exception e) {
            String message = String.format("Corrected cmo id autogeneration failed for banked sample: %s",
                    bankedSample.getUserSampleID());
            log.warn(message, e);
            errors.put(bankedSample.getUserSampleID(), String.format("%s. Cause: %s", message, e.getMessage()));

            return "";
        }
    }

    protected static boolean shouldGenerateCmoId(BankedSample bankedSample) {
        return isHumanSample(bankedSample) || HUMAN_RECIPES.contains(bankedSample.getRecipe());
    }

    protected static boolean isHumanSample(BankedSample bankedSample) {
        return !StringUtils.isEmpty(bankedSample.getSpecies()) && humanSamplePredicate.test(bankedSample);
    }

    private CorrectedCmoSampleView createFrom(BankedSample bankedSample) throws LimsException {
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
}
