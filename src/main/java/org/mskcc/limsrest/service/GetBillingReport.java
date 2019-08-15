package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * A queued task that takes a request id and returns the billing report info 
 * 
 * @author Aaron Gabow, Zheng Zeng
 */
@Service @Deprecated // code started but not completed or used?
public class GetBillingReport extends LimsTask {
    private static Log log = LogFactory.getLog(GetBillingReport.class);

    protected String project;

    public void init(String project) {
        this.project = project;
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
// RUN Name, Project#, sample, Barcode, Lane #, Lab Head, Investigator, Date (start run), Date (end run), Run type, read length, SampleRef, # reads request_sample, Status of project 
// 151028_MOMO_0100_BC7MG0ANXX 6255 shLUC-DMSO_1 TS2 1,2 S Armstrong Chun-Wei Chen 10.28.2015 RNASeq 51/7/51 Mouse 40-50M Done 
        List<RunSummary> runResults = new LinkedList<>();

        try {
            if (project == null) {
                throw new Exception("Unable to generate billing report with no project specified");
            }
            List<DataRecord> runList = new LinkedList<DataRecord>();
            List<DataRecord> queriedRequests = dataRecordManager.queryDataRecords("Request", "RequestId = '" + project + "'", user);
            for (DataRecord req : queriedRequests) {
                log.info("Getting a record " + project);
                List<DataRecord> reqLanes = req.getDescendantsOfType("FlowCellLane", user);
                for (DataRecord lane : reqLanes) {
                    log.info("Getting a flow cell lane");
                    List<DataRecord> flowcell = lane.getParentsOfType("FlowCell", user);
                    if (flowcell.size() > 0) {
                        log.info("Getting a flow cell");
                        List<DataRecord> possibleRun = flowcell.get(0).getParentsOfType("IlluminaSeqExperiment", user);
                        if (possibleRun.size() > 0) {
                            log.info("Getting a run");
                            if (!runList.contains(possibleRun.get(0))) {
                                runList.add(possibleRun.get(0));

                                String run = "";
                                try {
                                    String[] runFolderElements = possibleRun.get(0).getStringVal("SequencerRunFolder", user).split("/");
                                    run = runFolderElements[runFolderElements.length - 1];
                                } catch (NullPointerException npe) {
                                }

                                String requestId = "";
                                try {
                                    requestId = req.getStringVal("RequestId", user);
                                } catch (NullPointerException npe) {
                                }

                                String labHead = "";
                                try {
                                    labHead = req.getStringVal("LaboratoryHead", user);
                                } catch (NullPointerException npe) {
                                }

                                String investigator = "";
                                try {
                                    investigator = req.getStringVal("Investigator", user);
                                } catch (NullPointerException npe) {
                                }

                                long dateRequested = 0;
                                try {
                                    dateRequested = req.getDateVal("RequestStartDate", user);
                                } catch (NullPointerException npe) {
                                }

                                long dateRunReceived = 0;
                                try {
                                    dateRunReceived = possibleRun.get(0).getDateVal("DateCreated", user);
                                } catch (NullPointerException npe) {
                                }

                                RunSummary summary = new RunSummary(run, "");
                                summary.setRequestId(requestId);
                                summary.setLabHead(labHead);
                                summary.setInvestigator(investigator);
                                summary.setStartDate(dateRequested);
                                summary.setReceivedDate(dateRunReceived);
                                runResults.add(summary);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            runResults.add(RunSummary.errorMessage(e.getMessage(), sw.toString()));
            log.error(e.getMessage(), e);
        }

        return runResults;
    }
}
