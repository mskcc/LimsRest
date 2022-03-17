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

    String requestId;
    String comment;
    Date date;



    public AddOrCreateQCComment(String requestId, String comment, Date date, ConnectionLIMS conn) {
        this.requestId = requestId;
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
        try {
            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestId + "'", user);
            addQCComment(commentToProjectIdDateMap, requestList);
        }
        catch (NotFound | IoError | RemoteException e) {
            log.error(String.format("Error while querying request table for requestId: %s.\n %s:%s", requestId,
                    ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
        }
        List<DataRecord> requestComments = getCommentsFromDb();


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
    }

    /**
     * Writes comment into LIMS database
     * */
    public void addQCComment(List<Map<String, Pair<String, Date>>> addedQCComment, List<DataRecord> request) {
        Map<String, Object> qcComments = new HashMap<>();
        for (Map<String, Pair<String, Date>> qcComment : addedQCComment) {
            for(Map.Entry<String, Pair<String, Date>> entry : qcComment.entrySet()) {
                qcComments.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            request.get(0).addChild("QCComment", user);
        }
        catch (RemoteException | ServerException | NotFound | IoError | RemoteException e) {

        }


    }


}
