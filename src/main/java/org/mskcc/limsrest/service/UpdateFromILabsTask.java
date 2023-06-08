package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.*;
import org.apache.commons.lang3.tuple.Pair;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.ilabs.Filter;
import org.mskcc.limsrest.service.ilabs.GetGeneralInfo;
import org.yaml.snakeyaml.Yaml;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Pull ILabs request info and store it in the LIMS.
 */
public class UpdateFromILabsTask {
    private static final Log log = LogFactory.getLog(UpdateFromILabsTask.class);
    private static final String OUTBOX = "/skimcs/mohibullahlab/LIMS/AutomatedEmails/outbox/testEmail/";
    private static final String[] WHITELIST =
            new String[]{"04525_B", "04430", "05281_B", "05681_B_9", "04495", "05001", "05783_B", "05783_C", "05783_D", "05884_B", "03498", "CTRL-1095", "05737_B", "05022", "05500_AY", "06713", "06711",
                    "04773", "04919", "06811", "04998", "06000_BR", "06000_BZ", "06875", "06934", "05632_E", "06684_C", "06684_D", "06989", "07239", "07835", "07890", "07894_B", "07916", "07765_B",
                    "04500", "05054", "04996", "04531", "04864", "05080", "05226", "05107", "04657", "05220", "05268", "04931", "05108", "05197", "04971", "05605", "04755", "04376", "05359", "05574", "05751", "05699", "04920", "04298", "02756", "05822", "05427", "05665", "05638", "04592", "05632", "05028", "06642", "05309", "04622", "05043", "09094", "09095"};
    private ConnectionLIMS conn;

    private String tokenIGO, tokenCMO;

    public UpdateFromILabsTask(ConnectionLIMS conn, String tokenIGO, String tokenCMO) {
        this.conn = conn;
        this.tokenIGO = tokenIGO;
        this.tokenCMO = tokenCMO;
    }

    public Object execute() {
        VeloxConnection vConn = conn.getConnection();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();
        User user = vConn.getUser();

        System.out.println("Run " + LocalDateTime.now());
        Pair<String, String> ilabsConfigIGO = getIlabConfig("IGO");
        Pair<String, String> ilabsConfigCMO = getIlabConfig("CMO");
        System.out.println("Getting requests from LIMS");
        List<String> requests = getRequestIds(vConn, dataRecordManager, user);
        if (requests.isEmpty()) {
            System.out.println("Empty requests: LIMS update will not run.");
            return "";
        }

        for (String status : updateLims(vConn, dataRecordManager, user, ilabsConfigIGO, ilabsConfigCMO, requests)) {
            System.out.println(status);
        }

        return "Success";
    }

    public List<String> updateLims(VeloxConnection connection, DataRecordManager dataRecordManager, User user,
                                   Pair<String, String> ilabsConfigIGO, Pair<String, String> ilabsConfigCMO, List<String> requests) {
        try {
            Map<String, Map<String, String>> allIlabs = new HashMap<>();
            String core_id_igo = ilabsConfigIGO.getKey();
            String token_igo = ilabsConfigIGO.getValue();
            String core_id_cmo = ilabsConfigCMO.getKey();
            String token_cmo = ilabsConfigCMO.getValue();

            GetGeneralInfo ggi = new GetGeneralInfo(core_id_igo, token_igo, core_id_cmo, token_cmo);

            for (String request : requests) {
                System.out.println(request);
                try {
                    System.out.println("\nGetting iLabs info for " + request);
                    allIlabs.put(request, ggi.getServiceRequest(request));
                } catch (Exception e) {
                    String info = "Had trouble updating some general info files for " + request + "  Possible problems with ilabs. " + e.toString();
                    saveEmailToOutbox(info);
                }
            }

            System.out.println("\nUpdating Lims");
            return runProgram(connection, dataRecordManager, user, requests, allIlabs);
        } catch (Throwable e) {
            String info = "Had trouble updating some general info files. Possible problems with ilabs. " + e.toString();
            saveEmailToOutbox(info);
        }
        return new ArrayList<>();
    }

    public List<String> getRequestIds(VeloxConnection connection, DataRecordManager dataRecordManager, User user) {
        List<String> ids = new ArrayList<>();
        try {
            StringBuilder reqList = new StringBuilder();
            reqList.append("RequestId NOT IN (");
            for (int i = 0; i < WHITELIST.length; i++) {
                reqList.append("'");
                reqList.append(WHITELIST[i]);
                reqList.append("'");
                if (i < WHITELIST.length - 1) {
                    reqList.append(",");
                }
            }
            reqList.append(") AND ");
            reqList.append("(LABORATORYHEAD = '' or  LABORATORYHEAD is null or LABORATORYHEAD like 'FIELD NOT IN ILABS%' or LABORATORYHEAD like 'PROJECT NOT IN ILABS%')");
            String where = reqList.toString();
            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", where, user);
            for (int i = 0; i < requestList.size(); i++) {
                ids.add(requestList.get(i).getStringVal("RequestId", user));
            }
        } catch (Throwable e) {
            String info = "Had trouble finding the files to update. " + e.toString();
            saveEmailToOutbox(info);
        }
        return ids;
    }

    public List<String> runProgram(VeloxConnection connection, DataRecordManager dataRecordManager, User user,
                                   List<String> matchReqs, Map<String, Map<String, String>> allIlabs) {
        List<String> updateStatuses = new ArrayList<>();
        Map<String, String> igoPmName2ilabsPmName = new HashMap<>();
        igoPmName2ilabsPmName.put("Pavitra Rao", "Rao, Pavitra");

        try {
            StringBuilder reqList = new StringBuilder();
            reqList.append("RequestId IN (");
            for (int i = 0; i < matchReqs.size(); i++) {
                reqList.append("'");
                reqList.append(matchReqs.get(i));
                reqList.append("'");
                if (i < matchReqs.size() - 1) {
                    reqList.append(",");
                }
            }
            reqList.append(")");
            String where = reqList.toString();

            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", where, user);
            List<Map<String, Object>> blankRequestsFields = dataRecordManager.getFieldsForRecords(requestList, user);
            List<Map<String, Object>> ilabsFields = new LinkedList<>();

            StringBuffer updateList = new StringBuffer();
            boolean problemFound = false;
            for (int i = 0; i < blankRequestsFields.size(); i++) {
                Map<String, Object> currentFields = blankRequestsFields.get(i);
                Map<String, Object> requestFields = new HashMap<>();
                ilabsFields.add(requestFields);
                String reqId = (String) currentFields.get("RequestId");
                if (i > 0) {
                    updateList.append(", ");
                }
                updateList.append(reqId);
                if (!allIlabs.containsKey(reqId) ||
                        "PROJECT NOT IN ILABS".equals(allIlabs.get(reqId).get("PI"))) {
                    updateStatuses.add("PROBLEM UPDATING: Not in ilabs " + reqId);
                    problemFound = true;
                    continue;
                }
                // add header fields, these are expected on every form and always filled with at least "FIELD NOT IN ILABS"
                Map<String, String> field2val = allIlabs.get(reqId);
                requestFields.put("LaboratoryHead", Filter.toAscii(field2val.get("PI")));
                requestFields.put("iLabServiceRequestId", Filter.toAscii(field2val.get("ILAB_SERVICE_REQUEST_ID")));
                requestFields.put("Investigator", Filter.toAscii(field2val.get("INVEST")));
                requestFields.put("RoomNum", Filter.toAscii(field2val.get("ROOM")));
                requestFields.put("TelephoneNum", Filter.toAscii(field2val.get("PHONE")));
                requestFields.put("LabHeadEmail", Filter.toAscii(field2val.get("PIEMAIL")));
                requestFields.put("Investigatoremail", Filter.toAscii(field2val.get("INVESTEMAIL")));
                requestFields.put("MailTo", Filter.toAscii(field2val.get("ALLEMAILS")));

                if (field2val.get("SUMMARY") != null && field2val.get("SUMMARY").contains("PM:")) {
                    try {
                        String unmappedPm = field2val.get("SUMMARY").split("PM:")[1].trim();
                        if (igoPmName2ilabsPmName.containsKey(unmappedPm)) {
                            requestFields.put("ProjectManager", igoPmName2ilabsPmName.get(unmappedPm));
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        updateStatuses.add("PROBLEM UPDATING: No assigned PM yet " + reqId);
                        problemFound = true;
                        continue;
                    }
                }
                if (field2val.get("SUMMARY") != null && field2val.get("SUMMARY").toUpperCase().contains("NO PM")) {
                    System.out.println("no pm");
                    requestFields.put("ProjectManager", "NO PM");
                }
                int boundry = field2val.get("PROJ").length();
                if (boundry > 99) {
                    boundry = 99;
                }
                System.out.println(Filter.toAscii(field2val.get("PROJ").substring(0, boundry)));
                requestFields.put("ProjectName", Filter.toAscii(field2val.get("PROJ").substring(0, boundry)));
                // requestFields.put(field2val.get("CC"));
                // requestFields.put(field2val.get("FUND"));
                if (field2val.containsKey("COUNT") && !"FIELD NOT IN ILABS".equals(field2val.get("COUNT"))) {
                    try {
                        Double count = Double.parseDouble(field2val.get("COUNT"));
                        requestFields.put("SampleNumber", count);
                    } catch (NumberFormatException nfe) {
                        System.out.println("Skipping " + reqId + " because sample count NaN");
                    }
                }

//                if (field2val.containsKey("BARCODED_ANTIBODIES")) {
//                    if (field2val.get("BARCODED_ANTIBODIES").equals("Yes")) {
//                        requestFields.put("BarcodedAntibodies", Boolean.TRUE);
//                    }
//                    else {
//                        requestFields.put("BarcodedAntibodies", Boolean.FALSE);
//                    }
//                }
//                if (field2val.containsKey("SEQC_ANTIBODIES")) {
//                    if (field2val.get("SEQC_ANTIBODIES").contains("confirm")) {
//                        requestFields.put("SeqCAntibodies", Boolean.TRUE);
//                    }
//                    else {
//                        requestFields.put("SeqCAntibodies", Boolean.FALSE);
//                    }
//                }
                if (field2val.containsKey("TREATMENT")) {
                    if (field2val.get("TREATMENT").contains("Hashing") && field2val.get("TREATMENT").contains("Barcoding")) {
                        requestFields.put("Treatment", "Cell Hashing, Feature Barcoding");
                    }
                    else if (field2val.get("TREATMENT").contains("Barcoding")) {
                        requestFields.put("Treatment", "Feature Barcoding");
                    }
                    else  {
                        requestFields.put("Treatment", "Cell Hashing");
                    }
                }
//                if (field2val.containsKey("ADDITIONAL_VDJ")) {
//                    if (field2val.get("ADDITIONAL_VDJ").contains("Yes")) {
//                        requestFields.put("AdditionalVDJ", Boolean.TRUE);
//                    }
//                    else {
//                        requestFields.put("AdditionalVDJ", Boolean.FALSE);
//                    }
//                }
                if (field2val.containsKey("CELL_TYPES")) {
                    if (field2val.get("CELL_TYPES").contains("T Cells")) {
                        requestFields.put("CellTypes", "T Cells");
                    }
                    else {
                        requestFields.put("CellTypes", "B Cells");
                    }
                }

                if (field2val.containsKey("FASTQ") && field2val.get("ANALYSIS_TYPE").equals("FIELD NOT IN ILABS")) {
                    if ("FIELD NOT IN ILABS".equals(field2val.get("FASTQ")) || field2val.get("FASTQ").contains("BAM") || field2val.get("FASTQ").contains("Analysis Pipeline") ||
                            field2val.get("FASTQ").contains("Pipeline Anaylsis") || field2val.get("FASTQ").contains("Pipeline Analysis") || field2val.get("FASTQ").contains("Aligned reads plus bioinformatic analysis") ||
                            field2val.get("FASTQ").startsWith("BIC") || field2val.get("FASTQ").contains("institutional support")) {
                        requestFields.put("FASTQ", Boolean.TRUE);
                        requestFields.put("BICAnalysis", Boolean.TRUE);
                    } else {
                        requestFields.put("FASTQ", Boolean.TRUE);
                        requestFields.put("BICAnalysis", Boolean.FALSE);
                    }
                }
                if ((field2val.get("PI").startsWith("FIELD NOT IN ILABS") ||
                        field2val.get("PI").startsWith("PROJECT NOT IN ILABS")) &&
                        (System.currentTimeMillis() - (Long) currentFields.get("DateCreated") > 1000 * 60 * 60 * 2)) { //complain after two hours
                    updateStatuses.add("PROBLEM UPDATING: missing pi info " + reqId);
                    problemFound = true;
                } else {
                    updateStatuses.add("UPDATED: " + reqId);
                }
                // NEW FIELDS
                // Add new email fields, they are part of the header/always expected fields and will always be in the field2val map
                requestFields.put("DataAccessEmails", Filter.toAscii(field2val.get("DATA_ACCESS_EMAILS")));
                requestFields.put("QcAccessEmails", Filter.toAscii(field2val.get("QC_ACCESS_EMAILS")));
                // IF new fields are not in iLab but old one is, copy old field into new fields
                if ((field2val.get("DATA_ACCESS_EMAILS").equals("FIELD NOT IN ILABS") || field2val.get("DATA_ACCESS_EMAILS").equals("")) &&
                        (field2val.get("QC_ACCESS_EMAILS").equals("FIELD NOT IN ILABS") || field2val.get("QC_ACCESS_EMAILS").equals("")) &&
                        !field2val.get("ALLEMAILS").equals("FIELD NOT IN ILABS")) {
                    requestFields.put("QcAccessEmails", Filter.toAscii(field2val.get("ALLEMAILS")));
                    requestFields.put("DataAccessEmails", Filter.toAscii(field2val.get("ALLEMAILS")));
                }
//              IF old field not in iLab but new ones are, append and copy new fields into old field
                if (field2val.get("ALLEMAILS").equals("") || field2val.get("ALLEMAILS").equals("FIELD NOT IN ILABS")) {
                    System.out.println("Fillin in MailTO field if empty");
                    if (!field2val.get("DATA_ACCESS_EMAILS").equals("FIELD NOT IN ILABS") &&
                            !field2val.get("DATA_ACCESS_EMAILS").equals("") &&
                            !field2val.get("QC_ACCESS_EMAILS").equals("FIELD NOT IN ILABS") &&
                            !field2val.get("QC_ACCESS_EMAILS").equals("")) {
                        requestFields.put("MailTo", Filter.toAscii(field2val.get("DATA_ACCESS_EMAILS")) + "," + Filter.toAscii(field2val.get("QC_ACCESS_EMAILS")));
                    } else if (!field2val.get("QC_ACCESS_EMAILS").equals("FIELD NOT IN ILABS") && !field2val.get("QC_ACCESS_EMAILS").equals(""))
                        requestFields.put("MailTo", Filter.toAscii(field2val.get("QC_ACCESS_EMAILS")));
                    else if (!field2val.get("DATA_ACCESS_EMAILS").equals("FIELD NOT IN ILABS") && !field2val.get("DATA_ACCESS_EMAILS").equals(""))
                        requestFields.put("MailTo", Filter.toAscii(field2val.get("DATA_ACCESS_EMAILS")));
                    else {
                        System.out.println("Data and QC access emails are both blank.");
                        System.out.println("PI email is: " + Filter.toAscii(field2val.get("PIEMAIL")));
                        System.out.println("Invest email is: " + Filter.toAscii(field2val.get("INVESTEMAIL")));
                        requestFields.put("MailTo", Filter.toAscii(field2val.get("PIEMAIL")) + "," + Filter.toAscii(field2val.get("INVESTEMAIL")));
                    }
                }
//                IF new field analysis type present, set old fields according to that
                if (!field2val.get("ANALYSIS_TYPE").equals("FIELD NOT IN ILABS")) {
                    requestFields.put("FASTQ", Boolean.TRUE);
                    String analysisType = Filter.toAscii(field2val.get("ANALYSIS_TYPE"));
                    List<String> limsAnalysisTypes = new ArrayList<>();
                    if (analysisType.toUpperCase().contains("BIC")) {
                        requestFields.put("BICAnalysis", Boolean.TRUE);
                        limsAnalysisTypes.add("BIC");
                    }
                    if (analysisType.toUpperCase().contains("CCS")) {
                        limsAnalysisTypes.add("CCS");
                    }
                    if (analysisType.toUpperCase().contains("CAS")) {
                        limsAnalysisTypes.add("CAS");
                    }
                    if (analysisType.toUpperCase().contains("IGO")) {
                        limsAnalysisTypes.add("IGO");
                    }
                    if (analysisType.toUpperCase().contains("RAW DATA")) {
                        limsAnalysisTypes.add("FASTQ ONLY");
                    }

                    if (limsAnalysisTypes.size() > 0) {
                        requestFields.put("AnalysisType", String.join(",", limsAnalysisTypes));
                    } else {
                        requestFields.put("AnalysisType", "FASTQ ONLY");
                    }
                }
//                IF new field analysis type NOT present, set new fields according to old
                if (field2val.get("ANALYSIS_TYPE").equals("FIELD NOT IN ILABS") && !field2val.get("FASTQ").equals("FIELD NOT IN ILABS")) {
                    List<String> limsAnalysisTypes = new ArrayList<>();
                    if ((Boolean) requestFields.get("FASTQ")) {
                        limsAnalysisTypes.add("FASTQ ONLY");
                    }

                    if ((Boolean) requestFields.get("BICAnalysis")) {
                        limsAnalysisTypes.add("BIC");
                    }

                    if (limsAnalysisTypes.size() > 0) {
                        requestFields.put("AnalysisType", String.join(",", limsAnalysisTypes));
                    } else {
                        requestFields.put("AnalysisType", "FIELD NOT IN ILABS");
                    }
                }
            }
            System.out.println(updateList.toString());
            dataRecordManager.setFieldsForRecords(requestList, ilabsFields, user);
            dataRecordManager.storeAndCommit("Updated the Request ilabs information for requests " + updateList.toString(), user);
            if (problemFound) {
                StringBuilder probs = new StringBuilder();
                for (int i = 0; i < updateStatuses.size(); i++) {
                    if (i > 0) {
                        probs.append(", ");
                    }
                    probs.append(updateStatuses.get(i));
                }
                throw new IOException(probs.toString());
            }
        } catch (IOException e) {
            String info = "Had trouble updating some general info files " + e.toString();
            saveEmailToOutbox(info);
        } catch (Throwable e) {
            e.printStackTrace();
            String info = "Had trouble updating some general info files. Possible problems with ilabs. " + e.toString();
            saveEmailToOutbox(info);
        }
        return updateStatuses;
    }

    public void saveEmailToOutbox(String info) {
        try {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyymmdd_HHmmss");
            String strDate = dateFormat.format(date);

            System.out.println("Saving Email to outbox with error: " + info + " at time: " + strDate);

            String filename = OUTBOX + strDate + "iLab.txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            String fileContents = "Problem updating ilabs information for project \n" + info;
            writer.write(fileContents);
            writer.close();
        } catch (Exception e) {
            System.err.println("WARNING: Notification not working and errors are in excel files");
            e.printStackTrace();
        }
    }

    private Pair<String, String> getIlabConfig(String core) {
        if ("CMO".equals(core))
            return Pair.of("2892", tokenCMO);
        if ("IGO".equals(core))
            return Pair.of("3276", tokenIGO);
        return null;
    }
}