package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.ConnectionLIMS;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A queued task that takes a sample id and a status, and creates a child aliquot
 * 
 * @author Aaron Gabow
 */
public class AddChildSample {
    String sampleId;
    String status;
    String igoUser;
    String additionalType;
    String childId;
    ConnectionLIMS conn;

    public AddChildSample(String sampleId, String status, String additionalType, String igoUser, String childId, ConnectionLIMS conn) {
        this.status = status;
        this.sampleId = sampleId;
        this.igoUser = igoUser;
        this.additionalType = additionalType;
        this.childId = childId;
        this.conn = conn;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String execute() {
        VeloxConnection vConn = conn.getConnection();
        User user = vConn.getUser();
        DataRecordManager drm = vConn.getDataRecordManager();

        String newId = "";
        try {
            List<DataRecord> samps = drm.queryDataRecords("Sample", "SampleId = '" + sampleId + "'", user);

            if (samps.size() != 1) {
                return "ERROR: This service must match exactly one sample";
            }
            DataRecord parentSample = samps.get(0);

            if (!childId.equals("NULL")) {
                List<DataRecord> destSamps = drm.queryDataRecords("Sample", "SampleId = '" + childId + "'", user);
                if (destSamps.size() != 1) {
                    return "ERROR: If a child sample is specified, it must already exist";
                }
                parentSample.addChild(destSamps.get(0), user);
                drm.storeAndCommit(igoUser + " made " + childId + " a child sample for " + sampleId, user);
                return "Existing sample " + childId;
            }

            DataRecord[] childrenSamples = parentSample.getChildrenOfType("Sample", user);
            int max = 0;
            Pattern endPattern = Pattern.compile(".*_(\\d+)$");
            for (int i = 0; i < childrenSamples.length; i++) {
                String childId = childrenSamples[i].getStringVal("SampleId", user);
                Matcher matcher = endPattern.matcher(childId);
                if (matcher.matches()) {
                    int ending = Integer.parseInt(matcher.group(1));
                    if (ending >= max) {
                        max = ending;
                    }
                }
            }
            max += 1;
            newId = sampleId + "_" + max;
            DataRecord child = parentSample.addChild("Sample", user);
            if (!additionalType.equals("NULL")) {
                child.addChild(additionalType, user);
            }
            Map<String, Object> parentFields = parentSample.getFields(user);
            child.setDataField("SampleId", newId, user);
            child.setDataField("OtherSampleId", parentFields.get("OtherSampleId"), user);
            child.setDataField("UserSampleID", parentFields.get("UserSampleID"), user);
            child.setDataField("ExemplarSampleType", parentFields.get("ExemplarSampleType"), user);
            child.setDataField("RequestId", parentFields.get("RequestId"), user);
            child.setDataField("Concentration", parentFields.get("Concentration"), user);
            child.setDataField("ConcentrationUnits", parentFields.get("ConcentrationUnits"), user);
            child.setDataField("TotalMass", parentFields.get("TotalMass"), user);
            child.setDataField("Volume", parentFields.get("Volume"), user);
            child.setDataField("Species", parentFields.get("Species"), user);
            child.setDataField("Preservation", parentFields.get("Preservation"), user);
            child.setDataField("Recipe", parentFields.get("Recipe"), user);
            child.setDataField("IsControl", parentFields.get("IsControl"), user);
            child.setDataField("TumorOrNormal", parentFields.get("TumorOrNormal"), user);
            child.setDataField("ExemplarSampleStatus", status, user);

            drm.storeAndCommit(igoUser + " made a child sample for " + sampleId, user);
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return "ERROR IN ADDING CHILD SAMPLE: " + e.getMessage() + "TRACE: " + sw.toString();

        }
        return "New sample " + newId;
    }
}