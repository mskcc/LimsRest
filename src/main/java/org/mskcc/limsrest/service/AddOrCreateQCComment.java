package org.mskcc.limsrest.service;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetQCComment;
import org.mskcc.limsrest.service.sequencingqc.SampleSequencingQc;
import org.mskcc.limsrest.service.sequencingqc.UpdateLimsSampleLevelSequencingQcTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

public class AddOrCreateQCComment {
    private static Log log = LogFactory.getLog(GetQCComment.class);
    private ConnectionLIMS conn;
    DataRecordManager dataRecordManager;
    User user;
    String appPropertyFile = "/app.properties";
    String requestId;
    String comment;
    Date date;
    String createdBy;



    public AddOrCreateQCComment(String requestId, String comment, Date date, ConnectionLIMS conn) {
        this.requestId = requestId;
        this.comment = comment;
        this.date = date;
        this.conn = conn;
    }


    public Map<String, Object> execute() {
        Map<String, Object> qcCommentValues = new HashMap<>();
        List<Map<String, Object>> listOfCommentsOnAProject = new LinkedList<>();
        VeloxConnection vConn = conn.getConnection();
        user = vConn.getUser();
        dataRecordManager = vConn.getDataRecordManager();
        user = conn.getConnection().getUser();

        JSONObject comments = getQCCommentsFromDb();
        if (comments.keySet().size() == 0) {
            log.error(String.format("Found no NGS-STATS for request with request id %s using url %s", requestId, getStatsUrl()));
        }
        for (String key : comments.keySet()) {
            qcCommentValues = getQcComments(comments.getJSONObject(key));
            requestId = String.valueOf(qcCommentValues.get("RequestId"));
            comment = String.valueOf(qcCommentValues.get("Comment"));
            date = (Date) qcCommentValues.get("Comment");
            createdBy = String.valueOf(qcCommentValues.get("CreatedBy"));
            // Adding to the list of comments

            //*************HERE***************
            Map<String, Object> commentToProjectIdDateMap = new HashMap<>();
            commentToProjectIdDateMap.put("requestId", requestId);
            commentToProjectIdDateMap.put("comment", comment);
            commentToProjectIdDateMap.put("dateCreated", date);
            commentToProjectIdDateMap.put("createdBy", createdBy);
            listOfCommentsOnAProject.add(commentToProjectIdDateMap);

        }
        try {
            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
            addQCComment(listOfCommentsOnAProject, requestList);
        }
        catch (NotFound | IoError | RemoteException e) {
            log.error(String.format("Error while querying request table for requestId: %s.\n %s:%s", requestId,
                    ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));
        }



        String requestId = ""; // parsed projectId from JSON object

        if(Objects.isNull(getExistingQCCommentRecods(requestId)) || getExistingQCCommentRecods(requestId).size() == 0) {

        }
        return commentToProjectIdDateMap;
    }

    /**
     * Returns a list of existing comments for a project from QC website
     * */
    public List<DataRecord> getExistingQCCommentRecods(String requestId) {
        List<DataRecord> existingComments = new LinkedList<>();
        //ProjectCommentsModel table to be queried for the input projectId

        if(existingComments.size() > 0) {
            log.info(String.format("Found already existing comments for request: %s", requestId));
        }

        return  existingComments;
    }

    /**
     * Gets comments from the LIMS database
     * */
    public List<DataRecord> getCommentsFromDb(JSONObject projectCommentsAndDate, DataRecordManager dataRecordManager) {
        String requestId = String.valueOf(projectCommentsAndDate.get("requestId"));
        String comment = String.valueOf(projectCommentsAndDate.get("comment"));
        Date commentDate = (Date) projectCommentsAndDate.get("commentDate");

        return null;
    }

    /**
     * Writes comment into LIMS database
     * */
    public void addQCComment(List<Map<String, Object>> listOfQCComment, List<DataRecord> request) {
        //Map<String, Object> qcComments = new HashMap<>();

        for (Map<String, Object> qcComment : listOfQCComment) {
            //for(Map.Entry<String, Object> entry : qcComment.entrySet()) {
                //qcComments.put(entry.getKey(), entry.getValue());
            //}
            try {
                request.get(0).addChild("QCComment", qcComment, user);
            }
            catch (ServerException | RemoteException e) {
                log.error(String.format("Failed to add record to QCComment table for request: %s, ERROR: %s%s", request.get(0)
                        , ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));

            }
        }

    }

    /**
     * get run Stats from ngs-stats database.
     *
     * @return
     */
    private JSONObject getQCCommentsFromDb() {
        HttpURLConnection con;
        String url = getStatsUrl();
        StringBuilder response = new StringBuilder();
        try {
            assert url != null;
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            return new JSONObject(response.toString());
        } catch (Exception e) {
            log.info(String.format("Error while querying ngs-stats endpoint using url %s.\n%s:%s", url, ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));
            return new JSONObject();
        }
    }

    /**
     * Method to get url to get stats from run-stats db.
     *
     * @return
     */
    private String getStatsUrl() {
        Properties properties = new Properties();
        String delphiRestUrl;
        try {
            properties.load(new FileReader(getResourceFile(appPropertyFile).replaceAll("%23", "#")));
            delphiRestUrl = properties.getProperty("delphiRestUrl");
        } catch (IOException e) {
            log.error(String.format("Error while parsing properties file:\n%s,%s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
            return null;
        }
        // TODO: Edit the link to retrieve qc comments
        return StringUtils.join(delphiRestUrl, "ngs-stats/", this.requestId);
    }

    /**
     * Method to get path to property file.
     *
     * @param propertyFile
     * @return
     */
    private String getResourceFile(String propertyFile) {
        return UpdateLimsSampleLevelSequencingQcTask.class.getResource(propertyFile).getPath();
    }


    /**
     * get QcCommentData to store in LIMS from QC Stats JSONObject.
     *
     * @param commentsData
     * @return
     */
    private Map<String, Object> getQcComments(JSONObject commentsData) {
        String requestId = String.valueOf(commentsData.get("requestId"));
        log.info("QC comments request ID: " + requestId);
        String comment = String.valueOf(commentsData.get("request"));
        Date dateCreated = (Date) commentsData.get("DateCreated");
        String createdBy = String.valueOf(commentsData.get("CreatedBy"));

        ProjectQCComment qc = new ProjectQCComment(requestId, comment, dateCreated, createdBy);
        return qc.getQcComments();
    }
}
