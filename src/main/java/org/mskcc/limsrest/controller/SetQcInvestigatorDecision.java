package org.mskcc.limsrest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;

import org.mskcc.limsrest.service.SetQcInvestigatorDecisionTask;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping("/")
public class SetQcInvestigatorDecision {
    private static Log log = LogFactory.getLog(SetQcInvestigatorDecision.class);
    private final ConnectionLIMS conn;
    private ObjectMapper objectMapper;

    public SetQcInvestigatorDecision(ConnectionLIMS conn) {
        this.conn = conn;
        objectMapper = new ObjectMapper();

    }
    @PostMapping("/setInvestigatorDecision")
    @ApiOperation(tags="/setInvestigatorDecision", httpMethod = "POST", notes = "Set investigator decisions for records in Sample QC tables. Expects json object [{datatype:\"report\",samples: [{\"RecordId\" : recordId, \"InvestigatorDecision\": decision}]}]", response = String.class, value = "Set investigator decisions.")
    public ResponseEntity<String> response(@RequestBody String body) {
        log.info("Starting post /setQcInvestigatorDecision");

        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (jsonNode == null || !jsonNode.isArray()) {
            return ResponseEntity.badRequest().body("Request body is empty or not json array!");
        }
        Iterator<JsonNode> iterator = jsonNode.elements();
        List<Map<String, Object>> data = new ArrayList<>();

        String datatype = "";
        try {
            while (iterator.hasNext()) {
                List<Map<String, Object>> allFields = new ArrayList<>();
                List<Object> allRecords = new ArrayList<>();

                JsonNode node = iterator.next();
                datatype = node.get("dataType").textValue();
                Iterator<JsonNode> sampleIterator = node.get("samples").elements();
                while (sampleIterator.hasNext()) {
                    JsonNode sampleNode = sampleIterator.next();

                    allRecords.add(sampleNode.get("recordId").asText());
                    log.info(sampleNode.get("recordId").getClass().getName());
                    allFields.add(decisionMap(sampleNode));
                }
                Map<String, Object> decision = new HashMap<>();
                decision.put("datatype", datatype);
                decision.put("records", allRecords);
                decision.put("decisions", allFields);


                data.add(decision);
            }
        } catch (Exception e) {
            log.error(e);
            ResponseEntity.badRequest();
        }

        SetQcInvestigatorDecisionTask task = new SetQcInvestigatorDecisionTask(data, conn);
        String response = task.execute();
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> decisionMap(JsonNode node) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("RecordId", node.get("recordId").asText());
        fields.put("InvestigatorDecision", node.get("investigatorDecision").textValue());
        return fields;
    }
}