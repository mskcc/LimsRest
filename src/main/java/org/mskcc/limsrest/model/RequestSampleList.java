package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class RequestSampleList implements Serializable {
    private String ilabRequestId;
    private String requestId;
    private String requestName;
    private String recipe;
    private String projectManagerName;
    private String piEmail;
    private String labHeadName, labHeadEmail;
    private String investigatorName;
    private String investigatorEmail; // only for RNASeq
    private String dataAnalystName; // only for RNASeq
    private String dataAnalystEmail;
    private String otherContactEmails;
    private String dataAccessEmails;
    private String qcAccessEmails;
    private String strand; // only for RNASeq
    private String libraryType; // only for RNASeq
    private Boolean isCmoRequest;
    private Boolean bicAnalysis;
    private Boolean neoAg;
    private Long deliveryDate = null;
    private String deliveryPath;

    private List<RequestSample> samples;
    private List<String> pooledNormals;

    public RequestSampleList(String requestId) {
        this.requestId = requestId;
    }

    public RequestSampleList(String requestId, List<RequestSample> samples) {
        this.requestId = requestId;
        this.samples = samples;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getIlabRequestId() {
        return ilabRequestId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestId() {
        return requestId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<RequestSample> getSamples() {
        return samples;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestName() { return requestName; }
}
