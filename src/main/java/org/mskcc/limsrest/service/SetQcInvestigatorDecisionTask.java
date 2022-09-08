package org.mskcc.limsrest.service;


import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.service.assignedprocess.*;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

/*
 * A queued task that sets Investigator Decision in QC Report tables.
 *
 * @author Lisa Wagner
 */

public class SetQcInvestigatorDecisionTask extends LimsTask {

    private static Log log = LogFactory.getLog(SetQcInvestigatorDecisionTask.class);
    List<Map<String, Object>> data;
    protected QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner();

    public SetQcInvestigatorDecisionTask() {
    }

    public void init(final List<Map<String, Object>> data) {

        this.data = data;

    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        int count = 0;
        try {

            for (Map entry : data) {
                String datatype = (String) entry.get("datatype");

                List<Object> records = (List<Object>) entry.get("records");
                List<Map<String, Object>> decisions = (List<Map<String, Object>>) entry.get("decisions");
                List<DataRecord> matched = dataRecordManager.queryDataRecords(datatype, "RecordId", records, user);

                for (DataRecord match :
                        matched) { //match is of type QC RNA/DNA/Library Report
                    for (Map field : decisions) {
                        if (String.valueOf(match.getRecordId()).equals(String.valueOf(field.get("RecordId")))) {
                            match.setDataField("InvestigatorDecision", field.get("InvestigatorDecision"), user);
                            if(field.get("InvestigatorDecision").toString().toLowerCase().contains("continue processing")) {
                                log.info("investigator decision is continue processing.");
                                String newStatus = "Ready for - Assign from Investigator Decisions";
                                if(match.getParentsOfType("Sample", user) != null && match.getParentsOfType
                                        ("Sample", user).size() > 0) {
                                    match.getParentsOfType("Sample", user).get(0).setDataField(DT_Sample.
                                            EXEMPLAR_SAMPLE_STATUS, newStatus, user);
                                    log.info("After assigning sample status to " + newStatus + "!");

                                    DataRecord sample = match.getParentsOfType("Sample", user).get(0);
                                    String igoId = sample.getDataField("SampleId", user).toString();
                                    log.info("Sample's IGO Id is: " + igoId);
                                    List<DataRecord> qcRecordDna = dataRecordManager.queryDataRecords("QcReportDna",
                                            "SampleId = '" + igoId + "'", user);
                                    List<DataRecord> qcRecordRna = dataRecordManager.queryDataRecords("QcReportRna",
                                            "SampleId = '" + igoId + "'", user);
                                    List<DataRecord> qcRecordLibrary = dataRecordManager.queryDataRecords("QcReportLibrary",
                                            "SampleId = '" + igoId + "'", user);
                                    DataRecord qcStat = null;
                                    if(qcRecordDna != null && qcRecordDna.size() > 0) {
                                        qcStat = qcRecordDna.get(0);
                                        log.info("seqQc is assigned with a dna qc report record!");
                                    }
                                    else if(qcRecordRna != null && qcRecordRna.size() > 0) {
                                        qcStat = qcRecordRna.get(0);
                                        log.info("seqQc is assigned with a rna qc report record!");
                                    }
                                    else if(qcRecordLibrary != null && qcRecordLibrary.size() > 0) {
                                        qcStat = qcRecordLibrary.get(0);
                                        log.info("seqQc is assigned with a rna qc report record!");
                                    }
                                    log.info("qcStat igo id is:" + qcStat.getDataField("SampleId", user));
                                    qcStatusAwareProcessAssigner.assign(dataRecordManager, user, qcStat, QcStatus.fromString(newStatus));
                                    log.info("After assign is completed!");
                                }
                            } else if (field.get("InvestigatorDecision").toString().toLowerCase().contains("stop processing")) {
                                String newStatus = "Awaiting Processing";
                                if(match.getParentsOfType("Sample", user) != null && match.getParentsOfType
                                        ("Sample", user).size() > 0) {
                                    match.getParentsOfType("Sample", user).get(0).setDataField(DT_Sample.
                                            EXEMPLAR_SAMPLE_STATUS, newStatus, user);
                                    log.info("After assigning sample status to " + newStatus + "!");
                                }
                            }
                            count++;
                        }
                    }
                }
                dataRecordManager.storeAndCommit("Storing QC Decision", null, user);
                log.info(count + " Investigator Decisions set in " + datatype);
            }


        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return Messages.ERROR_IN + " SETTING INVESTIGATOR DECISION: " + e.getMessage();
        }
        return count + " Investigator Decisions set";
    }
}
