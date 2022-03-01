package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import javafx.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetQCComment;

import java.util.Date;
import java.util.Map;

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
        VeloxConnection vConn = conn.getConnection();
        user = vConn.getUser();
        dataRecordManager = vConn.getDataRecordManager();
        user = conn.getConnection().getUser();
    }
}
