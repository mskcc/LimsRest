package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetDdpcrAssaysTask;
import org.mskcc.limsrest.util.DdpcrAssay;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mskcc.limsrest.util.Utils.getResponseEntity;

@RestController
@RequestMapping("/")
public class GetDdpcrAssays {
    private static Log log = LogFactory.getLog(GetDdpcrAssays.class);
    private final ConnectionLIMS conn;

    public GetDdpcrAssays(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/getDdpcrAssays")
    public ResponseEntity<Map<String, Object>> getContent(
                                                          HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();

        GetDdpcrAssaysTask task = new GetDdpcrAssaysTask();
        List<DdpcrAssay> assays = task.execute(this.conn.getConnection());
        resp.put("status", "Success");
        resp.put("assays", assays);
        return getResponseEntity(resp, HttpStatus.OK);
    }
}
