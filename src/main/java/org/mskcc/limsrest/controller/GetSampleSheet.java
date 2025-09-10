package org.mskcc.limsrest.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.GetSampleSheetTask;
import org.mskcc.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * REST Controller for generating Illumina sample sheets.
 * 
 * This endpoint generates sample sheets for demultiplexing Illumina sequencing
 * runs.
 * It takes run information and returns a structured sample sheet with all
 * required
 * fields for demultiplexing.
 * 
 * IMPORTANT: This endpoint performs ONLY READ-ONLY operations on the database.
 * No data is modified, created, or deleted. All operations are queries and data
 * retrieval only.
 * 
 * Example usage:
 * GET /getSampleSheet?runId=RUNID_123&flowCellId=FLOWCELL_456
 * 
 * @author Rajiev Timal
 */
@RestController
@RequestMapping("/")
public class GetSampleSheet {
    private static final Log log = LogFactory.getLog(GetSampleSheet.class);

    private final ConnectionLIMS conn;

    public GetSampleSheet(ConnectionLIMS conn) {
        this.conn = conn;
    }

    /**
     * Generates a sample sheet for Illumina sequencing runs
     * 
     * READ-ONLY OPERATION: This endpoint only queries data from the database.
     * No modifications, creations, or deletions are performed.
     * 
     * @param runId        The sequencing run ID (required)
     * @param flowCellId   The flow cell ID (optional, will be derived from run if
     *                     not provided)
     * @param experimentId The experiment ID (optional, will be derived from runId
     *                     if not provided)
     * @param format       Response format: "table" (default) returns structured
     *                     data, "csv" returns CSV text
     * @return ResponseEntity containing the sample sheet data
     */
    @PreAuthorize("hasRole('ADMIN')") // Temporarily disabled for local testing
    @GetMapping("/getSampleSheet")
    public ResponseEntity<String> getSampleSheet(
            @RequestParam(value = "experimentId", required = false) String experimentId) {

        // Handle null or invalid experimentId x
        if (experimentId == null || experimentId.trim().isEmpty()) {
            log.error("experimentId is required but was null or empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(Constants.ERROR, "experimentId is required")
                    .body("Error: experimentId is required");
        }

        log.info(String.format("Starting getSampleSheet for experimentId: %s", experimentId));
        try {
            GetSampleSheetTask task = new GetSampleSheetTask(experimentId, this.conn);
            try {
                Map<String, Object> result = task.execute();
                String runID = result.get("runId").toString();
                // For now, we need to modify GetSampleSheetTask to return CSV content
                // This is a temporary solution until task is properly updated
                String csvContent = result != null && result.containsKey("csvContent")
                        ? (String) result.get("csvContent")
                        : "Sample sheet generation completed, but CSV content not available";
                String filename = String.format("SampleSheet_%s.csv", runID);

                return ResponseEntity.ok()
                        .header("Content-Type", "text/csv")
                        .header("Content-Disposition", "attachment; filename=" + filename)
                        .body(csvContent);

            } catch (Throwable e) {
                log.error(String.format("Error generating sample sheet for experimentId: %s", experimentId), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(Constants.ERROR, e.getMessage())
                        .body("Error: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error(String.format("Error generating sample sheet for experimentId: %s", experimentId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(Constants.ERROR, e.getMessage())
                    .body("Error: " + e.getMessage());
        }
    }
}