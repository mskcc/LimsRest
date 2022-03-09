package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetQCComment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class AddOrCreateQCComment {
    private static Log log = LogFactory.getLog(GetQCComment.class);
    private ConnectionLIMS conn;
    DataRecordManager dataRecordManager;
    User user;

    String projectId;
    String comment;
    Date date;



    public AddOrCreateQCComment(String projectId, String comment, Date date, ConnectionLIMS conn) {
        this.projectId = projectId;
        this.comment = comment;
        this.date = date;
        this.conn = conn;
    }


    public Map<String, Pair<String, Date>> execute() {
        Map<String, Pair<String, Date>> commentToProjectIdDateMap = new HashMap<>();
        VeloxConnection vConn = conn.getConnection();
        user = vConn.getUser();
        dataRecordManager = vConn.getDataRecordManager();
        user = conn.getConnection().getUser();

        JSONObject projectComments = getCommentsFromDb();
        for (String key : projectComments.keySet()) {
            commentToProjectIdDateMap = getQcComments(projectComments.getJSONObject(key));
        }

        String projectId = ""; // parsed projectId from JSON object
        //ProjectCommentsModel.
        if(Objects.isNull(getExistingQCCommentRecods(projectId)) || getExistingQCCommentRecods(projectId).size() == 0) {

        }
        return commentToProjectIdDateMap;
    }

    public Map<String, Pair<String, Date>> getQcComments(JSONObject projectCommentsAndDate) {
        String projectId = String.valueOf(projectCommentsAndDate.get("projectId"));
        String comment = String.valueOf(projectCommentsAndDate.get("comment"));
        Date commentDate = (Date) projectCommentsAndDate.get("commentDate");

    }

    /**
     * Returns a list of existing comments for a project
     * */
    public List<DataRecord> getExistingQCCommentRecods(String projectId) {
        List<DataRecord> existingComments = new LinkedList<>();
        //ProjectCommentsModel table to be queried for the input projectId

        if(existingComments.size() > 0) {
            log.info(String.format("Found already existing comments for project: %s", projectId));
        }

        return  existingComments;
    }

    /**
     * Gets comments from the LIMS database
     * */
    public JSONObject getCommentsFromDb() {
        HttpURLConnection con;
        String url = null;
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
            log.info(String.format("Error while querying LimsRest endpoint using url %s.\n%s:%s", url, ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e)));
            return new JSONObject();
        }
    }


}
