package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.AuditLog;
import com.velox.api.datarecord.AuditLogEntry;
import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GetPassingSamples extends LimsTask {
    protected String project;
    private static Log log = LogFactory.getLog(GetPassingSamples.class);
    private int day;
    private int month;
    private int year;

    public void init(String project, Integer day, Integer month, Integer year) {
        this.project = project;
        if (year != null) {
            this.day = day;
            this.month = month;
            this.year = year;
        }
    }

    @PreAuthorize("hasRole('READ')")
    public Object execute(VeloxConnection conn) {
        RequestSummary rs = new RequestSummary(this.project);
        try {
            List<DataRecord> requestList = this.dataRecordManager.queryDataRecords("Request", "RequestId = '" + this.project + "'", this.user);
            for (DataRecord r : requestList) {
                annotateRequestSummary(rs, r);
                try {
                    String[] readme = r.getStringVal("ReadMe", this.user).split("\n");
                    for (int i = 0; i < readme.length; i++) {
                        if (readme[i].startsWith("DELIVERY:")) {
                            rs.setSpecialDelivery(readme[i]);
                        }
                    }
                } catch (NullPointerException npe) {
                }
                List<List<Map<String, Object>>> allChildSampleFields = this.dataRecordManager.getFieldsForChildrenOfType(requestList, "Sample", this.user);
                HashMap<String, Map<String, Object>> sampleId2CmoInfoFields = new HashMap<>();
                if (allChildSampleFields.size() > 0) {
                    for (Map<String, Object> sampleFields : allChildSampleFields.get(0)) {
                        sampleId2CmoInfoFields.put((String) sampleFields.get("OtherSampleId"), sampleFields);
                    }
                }
                List<DataRecord> qcs = new LinkedList<>();
                HashMap<String, SampleSummary> id2samp = new HashMap<>();
                HashMap<String, String> id2PostQcStatus = new HashMap<>();
                qcs = this.dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "Request = '" + this.project + "'", this.user);
                if (qcs.size() == 0) {
                    qcs = r.getDescendantsOfType("SeqAnalysisSampleQC", this.user);
                }
                List<DataRecord> pqcs = r.getDescendantsOfType("PostSeqAnalysisQC", this.user); //this should be more resistant than SeqAnalysisSampleQC was initially to ending up a child of a parent Request
                for (DataRecord pqc : pqcs) {
                    try {
                        String pqcRunFolder = pqc.getStringVal("SequencerRunFolder", user);
                        pqcRunFolder = pqcRunFolder.replaceFirst("_[A-Z][0-9]{1,2}$", "");
                        id2PostQcStatus.put(pqc.getStringVal("OtherSampleId", user) + "_" + pqcRunFolder, pqc.getPickListVal("PostSeqQCStatus", user));
                    } catch (NullPointerException npe) {
                    }
                }
                HashMap<String, String> originalName2CorrectedName = new HashMap<>();
                List<DataRecord> cmoInfos = r.getDescendantsOfType("SampleCMOInfoRecords", user);  //this.dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "RequestId = '" + this.project + "'", this.user);
                for (DataRecord cmoInfo : cmoInfos) {
                    originalName2CorrectedName.put(cmoInfo.getStringVal("OtherSampleId", this.user), cmoInfo.getStringVal("CorrectedCMOID", this.user));
                }
                for (DataRecord qc : qcs) {
                    DataRecord parentSample = qc.getParentsOfType("Sample", this.user).get(0);
                    Map<String, Object> parentFields = parentSample.getFields(this.user);
                    String parentId = (String) parentFields.get("OtherSampleId");
                    if (!id2samp.containsKey(parentId)) {
                        id2samp.put(parentId, new SampleSummary());
                    }
                    SampleSummary ss = id2samp.get(parentId);
                    BasicQc qcSummary = new BasicQc();
                    String status = "Failed";
                    if (year != 0) {
                        GregorianCalendar cal = new GregorianCalendar(year, month, day);
                        long queryTime = cal.getTimeInMillis();

                        AuditLog auditlog = user.getAuditLog();
                        List<AuditLogEntry> qcHistory = auditlog.getAuditLogHistory(qc, false, user);
                        Collections.sort(qcHistory, new Comparator<AuditLogEntry>() {
                            public int compare(AuditLogEntry a1, AuditLogEntry a2) {
                                if (a1.timestamp == a2.timestamp) {
                                    return a1.dataFieldName.compareTo(a2.dataFieldName);
                                }
                                return Long.valueOf(a1.timestamp).compareTo(Long.valueOf(a2.timestamp));
                            }
                        }); //must be sorted for the following to work

                        for (AuditLogEntry logline : qcHistory) {
                            if (logline.dataFieldName.equals("SeqQCStatus") && logline.timestamp < queryTime) {
                                status = logline.newValue;
                            }
                        }

                    } else {
                        try {
                            status = qc.getSelectionVal("SeqQCStatus", user);
                        } catch (NullPointerException npe) {
                        }
                    }
                    String qcRunFolder = "";
                    try {
                        qcRunFolder = qc.getStringVal("SequencerRunFolder", user).replaceFirst("_[A-Z][0-9]{1,2}$", "");
                    } catch (NullPointerException npe) {
                    }
                    if ((!status.equals("Passed")) || (id2PostQcStatus.containsKey(parentId + "_" + qcRunFolder) && !id2PostQcStatus.get(parentId + "_" + qcRunFolder).equals("Passed"))) {
                        continue;
                    }
                    qcSummary.setQcStatus(status);
                    try {
                        qcSummary.setFileLocation("/ifs/archive/GCL/hiseq/FASTQ/" + qc.getStringVal("SequencerRunFolder", user));
                    } catch (NullPointerException npe) {
                    }
                    try {
                        qcSummary.setRun(qc.getStringVal("SequencerRunFolder", user));
                    } catch (NullPointerException npe) {
                    }
                    if (parentSample != null) {

                        try {
                            ss.setSpecies((String) parentFields.get("Species"));
                        } catch (NullPointerException npe) {
                        }
                        try {
                            ss.setRecipe((String) parentFields.get("Recipe"));
                        } catch (NullPointerException npe) {
                        }
                        try {
                            ss.setTumorOrNormal((String) parentFields.get("TumorOrNormal"));
                        } catch (NullPointerException npe) {
                        }
                        ss.addBaseId((String) parentFields.get("SampleId"));
                        ss.addCmoId(parentId);
                        try {
                            ss.addExpName((String) parentFields.get("UserSampleID"));
                        } catch (NullPointerException npe) {
                        }
                        try {
                            if (originalName2CorrectedName.containsKey(parentFields.get("OtherSampleId")) && !originalName2CorrectedName.get(parentFields.get("OtherSampleId")).equals(parentFields.get("OtherSampleId"))) {
                                ss.setCorrectedCmoId(originalName2CorrectedName.get(parentFields.get("OtherSampleId")));
                            } else {
                                ss.setCorrectedCmoId((String) parentFields.get("OtherSampleId"));
                            }
                        } catch (NullPointerException npe) {
                        }
                    }
                    ss.addBasicQc(qcSummary);
                    rs.addSample(ss);
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            rs.setRestStatus(e.getMessage());
        }

        return rs;
    }
}
