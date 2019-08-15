
package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * A queued task that takes a list of request id and map of values and returns them 
 * 
 * @author Aaron Gabow
 */
@Service
public class GetRequest extends LimsTask {
    String igoUser;
    String[] requestIds;
    String[] possibleRequestFields;

    public void init(String igoUser, String[] requestIds, String[] requestFields) {
        this.igoUser = igoUser;
        this.requestIds = requestIds;
        this.possibleRequestFields = requestFields;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
        LinkedList<RequestDetailed> rds = new LinkedList<>();

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (String r : requestIds) {
                sb.append("'");
                sb.append(r);
                sb.append("',");
            }
            if (sb.length() > 1) {
                sb.setLength(sb.length() - 1);
            }
            sb.append(")");
            List<DataRecord> matchedRequests = dataRecordManager.queryDataRecords("Request", "RequestId in " + sb.toString(), user);
            if (matchedRequests.size() == 0) {
                throw new LimsException("No Request record in the lims matches the ids: " + sb.toString());
            }

            for (DataRecord request : matchedRequests) {
                RequestDetailed rd = new RequestDetailed();
                annotateRequestDetailed(rd, request);
                rds.push(rd);
            }
        } catch (Throwable e) {
            RequestDetailed rd = new RequestDetailed();
            rd.setRequestType(Messages.ERROR_IN + " GetBanked: " + e.getMessage());
            rds.addFirst(rd);
            return rds;
        }

        return rds;
    }
}