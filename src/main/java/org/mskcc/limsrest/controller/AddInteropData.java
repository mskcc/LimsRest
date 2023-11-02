package org.mskcc.limsrest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.LimsException;
import org.mskcc.limsrest.service.SetOrCreateInteropData;
import org.mskcc.limsrest.util.Messages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/")
public class AddInteropData {
    private static final Log log = LogFactory.getLog(AddInteropData.class);
    private ConnectionLIMS conn;
    private static ObjectMapper objectMapper;

    public AddInteropData(ConnectionLIMS connectionQueue) {
        this.conn = connectionQueue;
        objectMapper = new ObjectMapper();
    }

    @RequestMapping(value = "/addInteropData", method = RequestMethod.POST)
    public ResponseEntity<String> addInteropData(@RequestBody String body) {
        log.info("Starting /addInteropData");

        JsonNode jsonNode = null;
        try {
            log.info("Request body: " + body);
            jsonNode = objectMapper.readTree(body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (jsonNode == null) {
            return ResponseEntity.badRequest().body("Request body is empty");
        }
        if (!jsonNode.isArray()) {
            return ResponseEntity.badRequest().body("Request is not a json array - " + body);
        }

        Iterator<JsonNode> iterator = jsonNode.elements();

        List<Map<String, Object>> allFields = new ArrayList<>();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String run = node.get("run").textValue();
            log.info("interop run: " + run);
            node.get("data").forEach(lane -> allFields.add(singleLaneSummary(run, lane)));
        }

        SetOrCreateInteropData setOrCreateInteropData = new SetOrCreateInteropData(allFields, conn);
        String returnCode = setOrCreateInteropData.execute();
        try {
            if (returnCode.startsWith(Messages.ERROR_IN)) {
                throw new LimsException(returnCode);
            }
            returnCode = "Record Ids:" + returnCode;
        } catch (Exception e) {
            log.error(e);
            returnCode = e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(returnCode);
        }

        return ResponseEntity.ok(returnCode);
    }

    private Map<String, Object> singleLaneSummary(String run, JsonNode node) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("i_Run", run);
        fields.put("i_Runwithnumberprefixremoved", node.get("runname").textValue());
        fields.put("i_Read", node.get("read").intValue());
        fields.put("i_Lane", node.get("lane").intValue());
        fields.put("i_Density", node.get("density").doubleValue());
        fields.put("i_Density_stddev", node.get("density_stddev").doubleValue());
        fields.put("i_ClusterPF", node.get("clusterpf").doubleValue());
        fields.put("i_ClusterPF_stddev", node.get("clusterpf_stddev").doubleValue());
        fields.put("i_ReadsM", node.get("readsm").doubleValue());
        fields.put("i_ReadsPFM", node.get("readspfm").doubleValue());
        fields.put("i_Q30", node.get("q30").doubleValue());
        fields.put("i_Aligned", node.get("aligned").doubleValue());
        fields.put("i_Aligned_stddev", node.get("aligned_stddev").doubleValue());
        fields.put("i_ErrorRate", node.get("errorrate").doubleValue());
        fields.put("i_Errorrate_stddev", node.get("errorrate_stddev").doubleValue());
        fields.put("i_Percent_Occupied", node.get("percent_occupied").doubleValue());
        return fields;
    }
}