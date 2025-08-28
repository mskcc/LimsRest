package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StatusResponse implements Serializable {
    private String application;
    private String status;
    private String timestamp;
    private String limsConnection;
    private String overallStatus;
    private String message;

    public String getApplication() {
        return application;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getLimsConnection() {
        return limsConnection;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public String getMessage() {
        return message;
    }
} 