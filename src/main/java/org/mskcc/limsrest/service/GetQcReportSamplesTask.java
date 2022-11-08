package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.QcReportSampleList.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/*
 * A queued task that takes a request id and a list of investigator sample ids and returns associated samples in report tables
 *
 * @author Lisa Wagner
 */
public class GetQcReportSamplesTask {
    private static Log log = LogFactory.getLog(GetQcReportSamplesTask.class);
    public List<String> otherSampleIds;
    protected String requestId;
    private ConnectionLIMS conn;

    public GetQcReportSamplesTask(List<String> otherSampleIds, String requestId, ConnectionLIMS conn) {
        this.otherSampleIds = otherSampleIds;
        this.requestId = requestId;
        this.conn = conn;
    }

    @PreAuthorize("hasRole('READ')")
    public QcReportSampleList execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        QcReportSampleList rsl = new QcReportSampleList(requestId, otherSampleIds);
        try {
            log.info("Gathering Report samples for " + otherSampleIds.size() + " samples.");
            getQcSamples(rsl, "QcReportDna", drm, user);
            getQcSamples(rsl, "QcReportRna", drm, user);
            getQcSamples(rsl, "QcReportLibrary", drm, user);
            rsl.setPathologyReportSamples(getPathologySamples("QcDatum", drm, user));
            rsl.setCovidReportSamples(getCovidSamples("Covid19TestProtocol5", drm, user));

            log.info("Gathering Attachments for " + requestId + ".");
            List<HashMap<String, Object>> attachments = new ArrayList<>();
            attachments.addAll(getAttachments(requestId, drm, user));

            rsl.setAttachments(attachments);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);

        }
        return rsl;
    }

    protected void getQcSamples(QcReportSampleList rsl, String dataType, DataRecordManager drm, User user) throws Exception {
//      only include samples where SampleId contains RequestId
        List<DataRecord> reportSamplesByRequest = drm.queryDataRecords(dataType, "SampleId LIKE '%" + requestId + "%'", user);
//        convert otherSampleIds to List<String>
        List<String> otherSampleIdsInRequest = otherSampleIds.stream()
                .map(object -> Objects.toString(object, null))
                .collect(Collectors.toList());
        int reportSamples = 0;
        if (reportSamplesByRequest.isEmpty()) {
            log.info("Request not found in " + dataType + ".");
            return;
        } else {
//        fetch chip ids for DLP pool samples, done here to avoid loop issues
            List<String> chipIds = getChipIds(requestId, drm, user);
            try {
                ReportSample reportSample;
                for (DataRecord sampleRecord : reportSamplesByRequest) {
                    Map<String, Object> sampleFields = sampleRecord.getFields(user);
                    String otherSampleId = sampleFields.get("OtherSampleId").toString();
                    String igoId = sampleFields.get("SampleId").toString();
//                check if reportSample's otherSampleId contains at least one id from this request
                    if (otherSampleIdsInRequest.parallelStream().anyMatch(otherSampleId::contains)) {
//                    is the reportSample a pool?
                        if (igoId.toLowerCase().contains("pool")) {
//                            check if pool only contains samples from the same request
                            boolean is_match = true;
                            if (otherSampleId.contains(",")) {
                                String[] poolids = otherSampleId.split(",");
                                for (String id : poolids) {
                                    if (!otherSampleIdsInRequest.parallelStream().anyMatch(id::contains)) {
                                        is_match = false;
                                        break;
                                    }
                                }
                            }
                            if (is_match) {
                                reportSample = new ReportSample.PoolReportSample(sampleFields);
                                rsl.poolReportSamples.add(reportSample);
                            }
//                        not a pool
                        } else {
//                            reject if igo id has anything but a number after the request ID
//                            (to avoid 10626_B samples showing up for 10626 which would happen if both requests contain
//                            the same investigator sample ids. Future fix: Add request id to report tables.
                            String igoIdPattern = requestId.toLowerCase() + "_\\d.*";
                            Pattern pattern = Pattern.compile(igoIdPattern, Pattern.CASE_INSENSITIVE);
                            if (pattern.matcher(igoId).matches()) {
                                switch (dataType) {
                                    case "QcReportDna":
                                        reportSample = new ReportSample.DnaReportSample(sampleFields);
                                        rsl.dnaReportSamples.add(reportSample);
                                        reportSamples += 1;
                                        break;
                                    case "QcReportRna":
                                        reportSample = new ReportSample.RnaReportSample(sampleFields);
                                        rsl.rnaReportSamples.add(reportSample);
                                        reportSamples += 1;
                                        break;
                                    case "QcReportLibrary":
                                        reportSample = new ReportSample.LibraryReportSample(sampleFields);
                                        rsl.libraryReportSamples.add(reportSample);
                                        reportSamples += 1;
                                        break;
                                    default:
                                        reportSample = new ReportSample();
                                }
                            }
                        }
//                    if report sample id does not contain any of the request's sample's ids, it might be a DLP Pool
                    } else if (sampleFields.get("Recipe").toString().equals("DLP") && igoId.toLowerCase().contains("pool")) {
//                check if reportSample's otherSampleId contains at least one chipId from this DLP request
                        if (chipIds.parallelStream().anyMatch(otherSampleId::contains)) {
                            reportSample = new ReportSample.PoolReportSample(sampleFields);
                            rsl.poolReportSamples.add(reportSample);
                        }
                    } else {
                        log.info("0 Samples found in " + dataType);
                    }
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                return;
            }
        }
        log.info(rsl.poolReportSamples.size() + " Pools and " + reportSamples + " Samples found in " + dataType + ".");
        return;
    }


    protected List<PathologySample> getPathologySamples(String dataType, DataRecordManager drm, User user) throws Exception {
//      check whether request exists in QC Report table, speeds up by ~30 %
        List<DataRecord> reportSamplesByRequest = drm.queryDataRecords(dataType, "SampleId LIKE '" + requestId + "%'", user);
        List<PathologySample> pathologySamples = new ArrayList<>();

        if (reportSamplesByRequest.isEmpty()) {
            log.info("Request not found in " + dataType + ".");
            return pathologySamples;
        } else {
            PathologySample pathologySample;
//          List<DataRecord> sampleList = dataRecordManager.queryDataRecords(dataType, "OtherSampleId", otherSampleIds, this.user);
            for (DataRecord sampleRecord : reportSamplesByRequest) {
                try {
                    Map<String, Object> sampleFields = sampleRecord.getFields(user);
//                  only include samples where SampleId contains RequestId
                    if (sampleFields.get("SampleId").toString().contains(requestId) && sampleFields.get("SampleFinalQCStatus").toString().equals("Failed") && sampleFields.get("DatumType").toString().equals("Pathology Review")) {
                        pathologySample = new PathologySample(sampleFields);
                        pathologySamples.add(pathologySample);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
            log.info(pathologySamples.size() + " Samples found in Pathology/" + dataType + ".");
            return pathologySamples;
        }
    }

    // COVID19 samples can be linked to a request by their OtherSampleId being the concatenation of investigatorId and iLabServiceId
    protected List<CovidSample> getCovidSamples(String dataType, DataRecordManager drm, User user) throws Exception {
        List<CovidSample> covidSamples = new ArrayList<>();

        final String query = String.format("RequestId = '%s'",  requestId);
        List<DataRecord> requestRecordList = drm.queryDataRecords("Request", query, user);
        if (requestRecordList.isEmpty()){
            log.info(requestId + " not found in " + RequestModel.DATA_TYPE_NAME);
            return covidSamples;
        }
        DataRecord requestRecord = requestRecordList.get(0);
        String serviceId = (String) requestRecord.getDataField("IlabRequest", user);

        List<DataRecord> reportSamples = drm.queryDataRecords(dataType, "OtherSampleId", Collections.singletonList(otherSampleIds), user);

        CovidSample covidSample;
        for (DataRecord sampleRecord : reportSamples) {
            try {
                Map<String, Object> sampleFields = sampleRecord.getFields(user);
                    covidSample = new CovidSample(sampleFields, serviceId);
                    covidSamples.add(covidSample);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
        log.info(covidSamples.size() + " Samples found in " + dataType + ".");
        return covidSamples;
    }


    protected List<HashMap<String, Object>> getAttachments(String requestId, DataRecordManager drm, User user) {
        List<HashMap<String, Object>> attachments = new ArrayList<>();
        try {
            List<DataRecord> attachmentRequestRecords = drm.queryDataRecords("Attachment", "FilePath LIKE '%" + requestId + "%'", user);
            if (attachmentRequestRecords.size() > 0) {
                String attachmentPattern = requestId + "_(DNA_QC|RNA_QC|Library_QC|Pool_QC|cDNA_QC)_*\\d*\\.pdf";
                Pattern pattern = Pattern.compile(attachmentPattern, Pattern.CASE_INSENSITIVE);
                for (DataRecord record : attachmentRequestRecords) {
                    String fileName = record.getDataField("FilePath", user).toString();
                    if (pattern.matcher(fileName).matches()) {
                        HashMap<String, Object> attachmentInfo = new HashMap<>();
                        attachmentInfo.put("recordId", record.getDataField("RecordId", user));
                        attachmentInfo.put("fileName", record.getDataField("FilePath", user));
                        attachmentInfo.put("hideFromSampleQC", record.getDataField("HideFromSampleQC", user));
                        attachments.add(attachmentInfo);
                    }
                }
            }
            log.info(attachmentRequestRecords.size() + " Attachments and " + attachments.size() + " correctly named Attachments found for " + requestId + ".");
            return attachments;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    protected List<String> getChipIds(String requestId, DataRecordManager drm, User user) throws Exception {
        List<DataRecord> dlpSamples = drm.queryDataRecords("DLPLibraryPreparationProtocol1", "SampleId LIKE '%" + requestId + "%'", user);
        List<String> chipIds = new ArrayList<>();
        for (DataRecord dlpSample : dlpSamples) {
            Map<String, Object> dlpFields = dlpSample.getFields(user);
            String chipId = dlpFields.get("ChipID").toString();
            if (!chipId.isEmpty()) {
                chipIds.add(chipId);
            }
        }
        return chipIds;
    }
}