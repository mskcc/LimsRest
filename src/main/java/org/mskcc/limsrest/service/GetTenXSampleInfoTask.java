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

/**
 * A queued task that gets a request id and find all related requests (10X requests) under one iLab and for each request
 * returns a map of its related sample ids and samples recipes. For "Feature Barcoding", iLab's "Treatment" information
 * and for "VDJ" iLab's "Cell Type" info is concatenated to the recipe for running the 10X multi pipeline.
 *
 * @author Fahimeh Mirhaj
 */

public class GetTenXSampleInfoTask {
    private static final Log log = LogFactory.getLog(GetTenXSampleInfoTask.class);
    private ConnectionLIMS conn;
    private String requestId;

    public GetTenXSampleInfoTask(String reqId, ConnectionLIMS conn) {
        this.conn = conn;
        this.requestId = reqId;
    }

    public Object execute() {
        try {
            VeloxConnection vConn = conn.getConnection();
            DataRecordManager dataRecordManager = vConn.getDataRecordManager();
            User user = vConn.getUser();

            requestId = requestId.split("_")[0];

            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "requestId like '" + requestId + "%' AND RequestName like '%10X%'", user);
            List<Map<String, List<String>>> samplesRecipes = new LinkedList<>();

            for (DataRecord request : requests) {
                String ilabRequest = request.getStringVal("IlabRequest", user);
                DataRecord[] listOfSamples = request.getChildrenOfType("Sample", user);
                Map<String, List<String>> samplesToRecipes = new HashMap<>();
                for (DataRecord sample : listOfSamples) {
                    List<String> samplesInfo = new LinkedList<>(); // iLab request, sample name and recipe
                    String recipe = sample.getStringVal("Recipe", user);
                    String sampleName = sample.getStringVal("OtherSampleId", user);
                    log.info("recipe of sample " + sample.getStringVal("SampleId", user) + " is: " + recipe);
                    samplesInfo.add(ilabRequest);
                    samplesInfo.add(sampleName);
                    String treatment = request.getStringVal("Treatment", user);
                    log.info("treatment = " + treatment);
                    String cellTypes = request.getStringVal("CellTypes", user);
                    log.info("cellTypes = " + cellTypes);
                    recipe += ", " + treatment + ", " + cellTypes;
                    samplesInfo.add(recipe);
                    samplesToRecipes.put(sample.getStringVal("SampleId", user), samplesInfo);
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