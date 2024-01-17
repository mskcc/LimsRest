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

@RestController
@RequestMapping("/")
public class GetDLPFieldMapFile {
    private static Log log = LogFactory.getLog(GetDLPFieldMapFile.class);
    private final ConnectionLIMS conn;

    public GetDLPFieldMapFile(ConnectionLIMS conn) {
        this.conn = conn;
    }

    @ApiOperation(httpMethod = "GET", value = "Get Attachment File by DLP chip number.", response = Byte.class, tags = "/getDLPFieldMapFile")
    @GetMapping("/getDLPFieldMapFile")
    public ResponseEntity<byte[]> getDLPFieldMapFile(@RequestParam(value = "chipNumber", required = true) String chipNumber) throws IOException {
        log.info("Starting /getDLPFieldMapFile");

        if (chipNumber.equals("") || chipNumber.length() != 7)
            throw new IOException("Invalid DLP chip number, expected string length must be 7");

        GetAttachmentFileTask task = new GetAttachmentFileTask(null, chipNumber, conn);
        HashMap<String, Object> file = task.execute();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.get("fileName"));

        log.info("Returning DLP Field Map (.fld) file " + file.get("fileName"));
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<byte[]> response = new ResponseEntity<>((byte[]) file.get("data"), headers, HttpStatus.OK);
        return response;
    }
}