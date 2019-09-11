package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.service.QcReportSampleList.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.*;


/*
 * A queued task that takes a request id and a list of investigator sample ids and returns associated samples in report tables
 *
 * @author Lisa Wagner
 */
public class GetQcReportSamplesTask extends LimsTask {
    private static Log log = LogFactory.getLog(GetQcReportSamplesTask.class);
    private List<Object> otherSampleIds;
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
        log.info("Gathering Report samples for " + otherSampleIds.size() + " samples.");

        try {
            rsl.setDnaReportSamples(getQcSamples("QcReportDna"));
            rsl.setRnaReportSamples(getQcSamples("QcReportRna"));
            rsl.setLibraryReportSamples(getQcSamples("QcReportLibrary"));
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
    private List<ReportSample> getQcSamples(String dataType) throws Exception {
        // check whether request exists in QC Report table, speeds up by ~30 %
        List<DataRecord> request = dataRecordManager.queryDataRecords(dataType, "SampleId LIKE '" + requestId + "%'", this.user);
        List<ReportSample> reportSamples = new ArrayList<>();

        if (request.isEmpty()) {
            log.info("Request not found in " + dataType + ".");
            return reportSamples;
        } else {
            ReportSample reportSample;

            List<DataRecord> sampleList = dataRecordManager.queryDataRecords(dataType, "OtherSampleId", otherSampleIds, this.user);

            for (DataRecord sampleRecord : sampleList) {
                try {
                    Map<String, Object> sampleFields = sampleRecord.getFields(user);

//                  only include samples where SampleId contains RequestId
                    if (sampleFields.get("SampleId").toString().contains(requestId)) {
                        switch (dataType) {
                            case "QcReportDna":
                                reportSample = new ReportSample.DnaReportSample(sampleFields);
                                break;
                            case "QcReportRna":
                                reportSample = new ReportSample.RnaReportSample(sampleFields);
                                break;
                            case "QcReportLibrary":
                                reportSample = new ReportSample.LibraryReportSample(sampleFields);
                                break;
                            default:
                                reportSample = new ReportSample();
                        }
                        reportSamples.add(reportSample);
                    }

                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
            log.info(reportSamples.size() + " Samples found in " + dataType + ".");
            return reportSamples;
        }

    }


}