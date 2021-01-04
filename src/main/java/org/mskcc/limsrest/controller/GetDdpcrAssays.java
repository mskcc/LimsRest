package org.mskcc.limsrest.controller;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionPoolLIMS;
import org.mskcc.limsrest.service.GetDdpcrAssaysTask;
import org.mskcc.limsrest.util.DdpcrAssay;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.mskcc.limsrest.util.Utils.getResponseEntity;

@RestController
@RequestMapping("/")
public class GetDdpcrAssays {
    private static Log log = LogFactory.getLog(GetDdpcrAssays.class);
    private final ConnectionPoolLIMS conn;

    public GetDdpcrAssays(ConnectionPoolLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getDdpcrAssays")
    @ApiOperation(tags = "/getDdpcrAssays", httpMethod = "GET", value = "Returns all ddPCR Assays present in LIMS ddPCR Assay datatype.", notes = "Returns assay metadata like expiration date, storage location, etc.")
    public ResponseEntity<Map<String, Object>> getContent(
            HttpServletRequest request) {
        log.info("Starting get /getDdpcrAssays");
        Map<String, Object> resp = new HashMap<>();

        GetDdpcrAssaysTask task = new GetDdpcrAssaysTask();
        task.init();
        Future<Object> result = conn.submitTask(task);

        List<DdpcrAssay> assays;
        try {
            assays = (List<DdpcrAssay>) result.get();
            if (assays.size() == 0) {
                resp.put("message", "No Assays found.");
            } else {
                resp.put("assays", assays);
            }
        } catch (Exception e) {
            log.error(e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return getResponseEntity(resp, HttpStatus.OK);
    }


}
