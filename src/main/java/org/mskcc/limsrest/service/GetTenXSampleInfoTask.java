package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.assignedprocess.QcStatus;
import org.mskcc.limsrest.util.Messages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class GetTenXSampleInfoTask {
    private static final Log log = LogFactory.getLog(GetTenXSampleInfoTask.class);
    private ConnectionLIMS conn;
    private String requestId;

    public GetTenXSampleInfoTask(String reqId, ConnectionLIMS conn) {
        conn = conn;
        requestId = reqId;
    }

    public Object execute() {
        VeloxConnection vConn = conn.getConnection();
        DataRecordManager dataRecordManager = vConn.getDataRecordManager();
        User user = vConn.getUser();

        try {
            requestId = requestId.split("_")[0];
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "requestId like '" + requestId + "%'", user);
            List<Map<String, String>> samplesRecipes = new LinkedList<>();

            for (DataRecord request : requests) {
                DataRecord[] listOfSamples = request.getChildrenOfType("Sample", user);
                Map<String, String> samplesToRecipes = new HashMap<>();
                for (DataRecord sample : listOfSamples) {
                    String recipe = sample.getStringVal("Recipe", user);

                    if (recipe.toLowerCase().contains("feature barcoding")) { // Feature Barcoding
                        String treatment = request.getStringVal("Treatment", user);
                        samplesToRecipes.put(sample.getStringVal("SampleId", user), recipe + ", " + treatment);
                    } else if (recipe.toLowerCase().contains("vdj")) { // VDJ
                        String cellTypes = request.getStringVal("CellTypes", user);
                        samplesToRecipes.put(sample.getStringVal("SampleId", user), recipe + ", " + cellTypes);
                    } else { // Gene Expression
                        samplesToRecipes.put(sample.getStringVal("SampleId", user), recipe);
                    }
                }
                samplesRecipes.add(samplesToRecipes);
            }

            return samplesRecipes;
        }
        catch (Exception e) {

        }
        return "Error";
    }

}