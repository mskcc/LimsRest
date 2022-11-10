package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.SetRequestStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class SetStatuses {
    private final static Log log = LogFactory.getLog(SetStatuses.class);
    private final ConnectionLIMS conn;

    public SetStatuses(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @GetMapping("/setRequestStatuses")
    public List<String> getContent() {
        log.info("Starting /setRequestStatuses");
        SetRequestStatus task = new SetRequestStatus();
        return task.execute();
    }
}