package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
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
public class GetQcReportSamplesTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetQcReportSamplesTask.class);
    public List<Object> otherSampleIds;
    protected String requestId;

    public GetQcReportSamplesTask() {
    }

    public GetQcReportSamplesTask(List<Object> otherSampleIds, String requestId) {
        this.otherSampleIds = otherSampleIds;
        this.requestId = requestId;
    }

    public void init(final String requestId, final List<Object> otherSampleIds) {
        this.otherSampleIds = otherSampleIds;
        this.requestId = requestId;
    }


    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {

        QcReportSampleList rsl = new QcReportSampleList(requestId, otherSampleIds);

        try {
            log.info("Gathering Report samples for " + otherSampleIds.size() + " samples.");
            getQcSamples(rsl, "QcReportDna");
            getQcSamples(rsl, "QcReportRna");
            getQcSamples(rsl, "QcReportLibrary");
            rsl.setPathologyReportSamples(getPathologySamples("QcDatum"));

            log.info("Gathering Attachments for " + requestId + ".");
            List<HashMap<String, Object>> attachments = new ArrayList<>();
            attachments.addAll(getAttachments(requestId));

            rsl.setAttachments(attachments);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);

        }
        return rsl;
    }

    /**
     * Returns samples in QC Table based on RequestId and otherSampleId
     *
     * @param dataType LIMS table
     * @return List of ReportSamples
     * @throws Exception
     */
    protected void getQcSamples(QcReportSampleList rsl, String dataType) throws Exception {
        // only include samples where SampleId contains RequestId
        List<DataRecord> reportSamplesByRequest = dataRecordManager.queryDataRecords(dataType, "SampleId LIKE '%" + requestId + "%'", this.user);
//        fetch chip ids for DLP pool samples, done here to avoid loop issues

//        convert otherSampleIds to List<String>
        List<String> otherSampleIdsInRequest = otherSampleIds.stream()
                .map(object -> Objects.toString(object, null))
                .collect(Collectors.toList());

        int reportSamples = 0;
        if (reportSamplesByRequest.isEmpty()) {
            log.info("Request not found in " + dataType + ".");
            return;
        } else {
            List<String> chipIds = getChipIds(requestId);
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
                            //  not a pool
                        } else {
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
//                    if report sample id does not contain any of the request's sample's ids, it might be a DLP Pool
                    } else if (sampleFields.get("Recipe").toString().equals("DLP") && igoId.toLowerCase().contains("pool")) {
//                check if reportSample's otherSampleId contains at least one chipId from this DLP request
                        if (chipIds.parallelStream().anyMatch(otherSampleId::contains)) {
                            reportSample = new ReportSample.PoolReportSample(sampleFields);
                            rsl.poolReportSamples.add(reportSample);
//
                        }
                    } else {
                        log.info("0 Samples found in " + dataType);
                        return;
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


    protected List<PathologySample> getPathologySamples(String dataType) throws Exception {
//      check whether request exists in QC Report table, speeds up by ~30 %
        List<DataRecord> reportSamplesByRequest = dataRecordManager.queryDataRecords(dataType, "SampleId LIKE '" + requestId + "%'", this.user);
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

    protected List<HashMap<String, Object>> getAttachments(String requestId) {
        List<HashMap<String, Object>> attachments = new ArrayList<>();
        try {
            List<DataRecord> attachmentRequestRecords = dataRecordManager.queryDataRecords("Attachment", "FilePath LIKE '%" + requestId + "%'", this.user);
            if (attachmentRequestRecords.size() > 0) {
                String attachmentPattern = requestId + "_(DNA_QC|RNA_QC|Library_QC|Pool_QC|cDNA_QC)_*\\d*\\.pdf";
                Pattern pattern = Pattern.compile(attachmentPattern, Pattern.CASE_INSENSITIVE);
                for (DataRecord record : attachmentRequestRecords) {
                    String fileName = record.getDataField("FilePath", user).toString();
                    if (pattern.matcher(fileName).matches()) {
                        HashMap<String, Object> attachmentInfo = new HashMap<>();
                        attachmentInfo.put("recordId", record.getDataField("RecordId", user));
                        attachmentInfo.put("fileName", record.getDataField("FilePath", user));
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

    protected List<String> getChipIds(String requestId) throws Exception {
        List<DataRecord> dlpSamples = dataRecordManager.queryDataRecords("DLPLibraryPreparationProtocol1", "SampleId LIKE '%" + requestId + "%'", this.user);
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