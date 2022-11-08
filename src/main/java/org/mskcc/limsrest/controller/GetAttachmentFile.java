package org.mskcc.limsrest.controller;


import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetAttachmentFileTask;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;

//Get Attachment File by its record id.
@RestController
@RequestMapping("/")
public class GetAttachmentFile {
    private static Log log = LogFactory.getLog(GetAttachmentFile.class);
    private final ConnectionLIMS conn;

    public GetAttachmentFile(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @ApiOperation(httpMethod = "GET", value = "Get Attachment File by its record id.", response = Byte.class, tags = "/getAttachmentFile")
    @GetMapping("/getAttachmentFile")
    public ResponseEntity<byte[]> getAttachmentFile(@RequestParam(value = "recordId", required = true) String recordId) throws IOException {
        log.info("Starting /getAttachmentFile");

        GetAttachmentFileTask task = new GetAttachmentFileTask(recordId, conn);
        HashMap<String, Object> file = task.execute();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.get("fileName"));

        log.info("Returning Attachment " + file.get("fileName"));
        headers.setContentType(MediaType.APPLICATION_PDF);

        ResponseEntity<byte[]> response = new ResponseEntity<>((byte[]) file.get("data"), headers, HttpStatus.OK);
        return response;
    }
}