package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.util.Messages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mskcc.limsrest.util.Utils.runAndCatchNpe;

public class GetSampleQcTask {
    private static Log log = LogFactory.getLog(GetSampleQcTask.class);
    protected String[] projectList;
    private ConnectionLIMS conn;

    @Value("${delphiRestUrl}")
    private String delphiRestUrl;

    public GetSampleQcTask(String[] projectList, ConnectionLIMS conn) {
        this.projectList = projectList;
        this.conn = conn;
    }

    @PreAuthorize("hasRole('READ')")
    public List<RequestSummary> execute() {
        List<RequestSummary> rss = new LinkedList<>();
        try {
            VeloxConnection vConn = conn.getConnection();
            User user = vConn.getUser();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();

            String projects = Stream.of(projectList).collect(Collectors.joining("','", "'", "'"));
            log.info("Project " + projects);
            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId in (" + projects + ")", user);
            HashMap<String, String>  alt2base = new HashMap<>(); // AltId->SampleId
            for (DataRecord r : requestList) {
                DataRecord[] baseSamples = r.getChildrenOfType("Sample", user);
                for (DataRecord bs : baseSamples){
                    try {
                        alt2base.put(bs.getStringVal("AltId", user), bs.getStringVal("SampleId", user));
                    } catch (NullPointerException npe){
                        log.info("Problem trying to populate base id mapping");
                    }
                }

                String project = r.getStringVal("RequestId", user);
                RequestSummary rs = new RequestSummary(project);
                HashSet<String> runSet = new HashSet<>();
                annotateRequestSummary(rs, r, user); // fill in all data at request level except sample number
                if (r.getValue("SampleNumber", user) != null)
                    rs.setSampleNumber(r.getShortVal("SampleNumber", user));

                List<DataRecord> qcRecords;
                qcRecords = dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "Request = '" + project + "'", user);
                if (qcRecords.size() == 0){
                    qcRecords = r.getDescendantsOfType("SeqAnalysisSampleQC", user);
                }
                for (DataRecord qc : qcRecords) {
                    log.info("Getting QC Site records for sample.");
                    SampleSummary ss = new SampleSummary();
                    DataRecord parentSample = qc.getParentsOfType("Sample", user).get(0);
                    SampleQcSummary qcSummary = annotateQcSummary(qc, user);
                    if (parentSample != null) {
                        annotateSampleSummary(ss, parentSample, user);
                        try {
                            if (!parentSample.getStringVal("AltId", user).equals("")){
                                ss.addBaseId(alt2base.get(parentSample.getStringVal("AltId", user)));
                                log.info(parentSample.getStringVal("AltId", user));
                                log.info(alt2base.get(parentSample.getStringVal("AltId", user)));
                            } else{
                                DataRecord searchSample = parentSample;
                                boolean canSearch = true;
                                while (searchSample.getParentsOfType("Request", user).size() == 0 && canSearch){
                                    List<DataRecord> searchParents = searchSample.getParentsOfType("Sample", user);
                                    if(searchParents.size() == 0){
                                        canSearch = false;
                                    } else{
                                        searchSample = searchParents.get(0);
                                    }
                                }
                                ss.addBaseId(searchSample.getStringVal("SampleId", user));
                            }
                        } catch (Exception e){
                            log.info("Problem trying to access base id mapping");
                        }
                        DataRecord[] childSamples = parentSample.getChildrenOfType("Sample", user);
                        if (childSamples.length > 0){
                            ss.setInitialPool(childSamples[0].getStringVal("SampleId", user));
                        }
                        DataRecord[] qcData = parentSample.getChildrenOfType("QCDatum", user);
                        long created = -1;
                        for (DataRecord datum : qcData){
                            Map<String, Object> qcFields = datum.getFields(user);
                            if (qcFields.containsKey("MapToSample") && (boolean)qcFields.get("MapToSample") && (long)qcFields.get("DateCreated") > created){
                                qcSummary.setQcControl((Double)qcFields.get("CalculatedConcentration"));
                                qcSummary.setQcUnits((String)qcFields.get("ConcentrationUnits"));
                                created = (Long)qcFields.get("DateCreated");
                            }
                            if (qcFields.containsKey("DatumType") && qcFields.get("DatumType").equals("Quant-it")){
                                qcSummary.setQuantIt((Double)qcFields.get("CalculatedConcentration"));
                                qcSummary.setQuantUnits((String)qcFields.get("ConcentrationUnits"));
                            }
                        }
                        DataRecord[] requirements = parentSample.getChildrenOfType("SeqRequirement", user);

                        Deque<DataRecord> queue = new LinkedList<>();
                        queue.addLast(parentSample);
                        Set<DataRecord> visited = new HashSet<>();
                        while ((requirements.length == 0) &&  !queue.isEmpty()) {
                            DataRecord current = queue.removeFirst();
                            requirements = current.getChildrenOfType("SeqRequirement", user);
                            List<DataRecord> parents = current.getParentsOfType("Sample", user);
                            for (DataRecord parent : parents){
                                if (!visited.contains(parent)){
                                    visited.add(parent);
                                    queue.addLast(parent);
                                }
                            }
                        }
                        if (requirements.length > 0){
                            try {
                                ss.setReadNumber((long)requirements[0].getDoubleVal("RequestedReads", user));
                            } catch(NullPointerException npe){
                                ss.setReadNumber(Long.valueOf(0));
                            }
                            try {
                                ss.setCoverage(requirements[0].getIntegerVal("CoverageTarget", user));
                            } catch (NullPointerException npe){
                                ss.setCoverage(0);
                            }
                        }

                        // calculate yield trying to find a Protocol Record to get elution volume and use corresponding sample's concentration to multiply for yield
                        // Jira IGOWEB-1250 - concentration calculation is not 100% correct for all cases but nobody has explained what needs to change
                        try {
                            DataRecord[] protocols = parentSample.getChildrenOfType("DNALibraryPrepProtocol2", user);
                            if (protocols.length > 0){
                                ss.setYield(protocols[0].getDoubleVal("ElutionVol", user) * parentSample.getDoubleVal("Concentration", user));
                            } else{
                                DataRecord[] assignments = parentSample.getChildrenOfType("MolarConcentrationAssignment", user);
                                if(assignments.length > 0){
                                    ss.setYield(assignments[0].getDoubleVal("Concentration", user));
                                }
                            }
                        } catch (NullPointerException e){
                            ss.setYield(0);
                        }
                    }
                    runSet.add(qcSummary.getRun());
                    ss.setQc(qcSummary);
                    rs.addSample(ss);
                }

                try {
                    // skip no longer relevant // TODO fully remove dead code
                    // addPooledNormals(rs, runSet);
                } catch (Exception e) {
                    log.error("Failed to add QC stats for pooled normals." + e.getMessage(), e);
                }

                rss.add(rs);
            }
        } catch (Throwable e) {
            log.info(e.getMessage(), e);
            RequestSummary rs = RequestSummary.errorMessage(e.getMessage());
            rss.add(rs);
        }

        return rss;
    }

    protected void addPooledNormals(RequestSummary rs, HashSet<String> runSet) {
        log.info("Querying for pooled normals for runs: " + String.join(",", runSet));

        for (String run : runSet) {
            // TODO planned refactor of this endpoint code along with LimsHelperScripts.CreateSampleQc
            String url = delphiRestUrl + "ngs-stats/picardstats-controls/run/" + run;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<List<QCSiteStats>> statsResponse =
                    restTemplate.exchange(url,
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<QCSiteStats>>() {
                            });
            List<QCSiteStats> stats = statsResponse.getBody();
            if (stats == null) {
                log.info("No pooled normals found for run: " + run);
                return;
            }

            log.info("Returned: " + stats);

            for (QCSiteStats s: stats) {
                SampleSummary ss = new SampleSummary();
                ss.addConcentration(1.0);
                ss.addConcentrationUnits("ng/uL");
                ss.addBaseId("0");
                ss.addVolume(1.0);
                ss.setSpecies(s.getReferenceGenome());
                ss.setCoverage(0);
                ss.setReadNumber(Long.valueOf("0"));
                ss.addCmoId("PooledNormal");
                ss.setTumorOrNormal("Control");
                ss.setInitialPool("Pooled Normal");
                ss.setRecordId(Long.valueOf("-1"));
                ss.addRequest("Pooled Normal");

                ss.setQc(s.toSampleSummary());

                rs.addSample(ss);
            }
        }
    }

    public static SampleQcSummary annotateQcSummary(DataRecord qc, User user) {
        SampleQcSummary qcSummary = new SampleQcSummary();
        try {
            Map<String, Object> qcFields = qc.getFields(user);

            String qcStatus = (String) qcFields.get("SeqQCStatus");
            log.info("Building QC record with status: " + qcStatus);
            //qcFields.forEach((key, value) -> System.out.println(key + ":" + value));

            Boolean passedQc = (Boolean) qcFields.getOrDefault("PassedQc", Boolean.FALSE);
            if (passedQc == null)
                passedQc = Boolean.FALSE;
            if (passedQc && "Passed".equals(qcStatus))
                qcSummary.setQcStatus(QcStatus.IGO_COMPLETE.getText());
            else
                qcSummary.setQcStatus(qcStatus);

            runAndCatchNpe(() -> qcSummary.setRecordId((Long) qcFields.get("RecordId")));
            runAndCatchNpe(() -> qcSummary.setSampleName((String) qcFields.get("OtherSampleId")));
            runAndCatchNpe(() -> qcSummary.setBaitSet((String) qcFields.get("BaitSet")));
            runAndCatchNpe(() -> qcSummary.setMeanTargetCoverage((Double) qcFields.get("MeanTargetCoverage")));
            runAndCatchNpe(() -> qcSummary.setMEAN_COVERAGE((Double) qcFields.get("MeanCoverage")));
            runAndCatchNpe(() -> qcSummary.setPercentAdapters((Double) qcFields.get("PercentAdapters")));
            runAndCatchNpe(() -> qcSummary.setPercentDuplication((Double) qcFields.get("PercentDuplication")));
            runAndCatchNpe(() -> qcSummary.setPercentOffBait((Double) qcFields.get("PercentOffBait")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_MAPQ((Double) qcFields.get("PercentExcMapQ")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_DUPE((Double) qcFields.get("PercentExcDupe")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_BASEQ((Double) qcFields.get("PercentExcBaseQ")));
            runAndCatchNpe(() -> qcSummary.setPCT_EXC_TOTAL((Double) qcFields.get("PercentExcTotal")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget10x((Double) qcFields.get("PercentTarget10X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget30x((Double) qcFields.get("PercentTarget30X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget40x((Double) qcFields.get("PercentTarget40X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget80x((Double) qcFields.get("PercentTarget80X")));
            runAndCatchNpe(() -> qcSummary.setPercentTarget100x((Double) qcFields.get("PercentTarget100X")));
            runAndCatchNpe(() -> qcSummary.setReadsDuped((Long) qcFields.get("ReadPairDupes")));
            runAndCatchNpe(() -> qcSummary.setReadsExamined((Long) qcFields.get("ReadsExamined")));
            runAndCatchNpe(() -> qcSummary.setTotalReads((Long) qcFields.get("TotalReads")));
            runAndCatchNpe(() -> qcSummary.setUnmappedReads((Long) qcFields.get("UnmappedDupes")));
            runAndCatchNpe(() -> qcSummary.setUnpairedExamined((Long) qcFields.get("UnpairedReads")));
            runAndCatchNpe(() -> qcSummary.setZeroCoveragePercent((Double) qcFields.get("ZeroCoveragePercent")));
            runAndCatchNpe(() -> qcSummary.setRun((String) qcFields.get("SequencerRunFolder")));
            runAndCatchNpe(() -> qcSummary.setReviewed((Boolean) qcFields.get("Reviewed")));
            runAndCatchNpe(() -> qcSummary.setPercentRibosomalBases((Double) qcFields.get("PercentRibosomalBases")));
            runAndCatchNpe(() -> qcSummary.setPercentCodingBases((Double) qcFields.get("PercentCodingBases")));
            runAndCatchNpe(() -> qcSummary.setPercentUtrBases((Double) qcFields.get("PercentUtrBases")));
            runAndCatchNpe(() -> qcSummary.setPercentIntronicBases((Double) qcFields.get("PercentIntronicBases")));
            runAndCatchNpe(() -> qcSummary.setPercentIntergenicBases((Double) qcFields.get("PercentIntergenicBases")));
            runAndCatchNpe(() -> qcSummary.setPercentMrnaBases((Double) qcFields.get("PercentMrnaBases")));
            runAndCatchNpe(() -> qcSummary.setRecipe((String) qcFields.get("Recipe")));
            runAndCatchNpe(() -> qcSummary.setStatsVersion((String) qcFields.get("StatsVersion")));
        } catch (Throwable e) {
            log.info(e.getMessage(), e);
            qcSummary.setSampleName(Messages.ERROR_IN + " Annotation:" + e.getMessage());
        }
        return qcSummary;
    }

    public static void annotateRequestSummary(RequestSummary rs, DataRecord request, User user) {
        try {
            Map<String, Object> requestFields = request.getFields(user);
            annotateRequestSummary(rs, requestFields);
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage());
            log.info(sw.toString());
            rs.setInvestigator(Messages.ERROR_IN + " Annotation:" + e.getMessage());
        }
    }

    public static void annotateRequestSummary(RequestSummary rs, Map<String, Object> requestFields) {
        try {
            runAndCatchNpe(() -> rs.setPi((String) requestFields.get("LaboratoryHead")));
            runAndCatchNpe(() -> rs.setInvestigator((String) requestFields.get("Investigator")));
            runAndCatchNpe(() -> rs.setPiEmail((String) requestFields.get("LabHeadEmail")));
            runAndCatchNpe(() -> rs.setDataAccessEmails((String) requestFields.get("DataAccessEmails")));
            runAndCatchNpe(() -> rs.setInvestigatorEmail((String) requestFields.get("Investigatoremail")));
            runAndCatchNpe(() -> rs.setIsCmoRequest((Boolean)requestFields.get("IsCmoRequest")));
            runAndCatchNpe(() -> rs.setAnalysisRequested((Boolean) requestFields.get("BICAnalysis")));
            runAndCatchNpe(() -> rs.setAnalysisType((String) requestFields.get("AnalysisType")));
            runAndCatchNpe(() -> rs.setCmoProjectId((String) requestFields.get("CMOProjectID")));
            runAndCatchNpe(() -> rs.setProjectManager((String) requestFields.get("ProjectManager")));
            runAndCatchNpe(() -> rs.setRequestName((String) requestFields.get("RequestName")));
        } catch (Throwable e) {
            log.error(e);
            rs.setInvestigator("Annotation failed:" + e.getMessage());
        }
    }

    public static void annotateSampleSummary(SampleSummary ss, DataRecord sample, User user) {
        try {
            Map<String, Object> sampleFields = sample.getFields(user);
            annotateSampleSummary(ss, sampleFields);
        } catch (Throwable e) {
            ss.addCmoId("Annotation failed:" + e.getMessage());
        }
    }

    public static void annotateSampleSummary(SampleSummary ss, Map<String, Object> sampleFields) {
        try {
            ss.setRecordId((Long) sampleFields.get("RecordId"));
            ss.setSpecies((String) sampleFields.get("Species"));
            runAndCatchNpe(() -> ss.setRecipe((String) sampleFields.get("Recipe")));
            runAndCatchNpe(() -> ss.setTumorOrNormal((String) sampleFields.get("TumorOrNormal")));
            runAndCatchNpe(() -> ss.setTumorType((String) sampleFields.get("TumorType")));
            runAndCatchNpe(() -> ss.setGender((String) sampleFields.get("Gender")));
            ss.addRequest((String) sampleFields.get("RequestId"));
            ss.addBaseId((String) sampleFields.get("SampleId"));
            ss.addCmoId((String) sampleFields.get("OtherSampleId"));
            ss.addUniqueIdentifier((String) (sampleFields.get("SampleId").toString() + "_" + sampleFields.get("Recipe")));
            runAndCatchNpe(() -> ss.addExpName((String) sampleFields.get("UserSampleID")));
            runAndCatchNpe(() -> ss.setSpecimenType((String) sampleFields.get("SpecimenType")));
            runAndCatchNpe(() -> ss.addConcentration((Double) sampleFields.get("Concentration")));
            runAndCatchNpe(() -> ss.addConcentrationUnits((String) sampleFields.get("ConcentrationUnits")));
            runAndCatchNpe(() -> ss.addVolume((Double) sampleFields.get("Volume")));
            runAndCatchNpe(() -> ss.setPlatform((String) sampleFields.get("Platform")));
            ss.setDropOffDate((Long) sampleFields.get("DateCreated"));
        } catch (Throwable e) {
            ss.addCmoId("Annotation failed:" + e.getMessage());
        }
    }
}