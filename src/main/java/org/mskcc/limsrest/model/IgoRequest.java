package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Setter
public class IgoRequest implements Serializable {
    protected String requestId;
    protected String recipe;
    protected String projectManagerName;
    protected String piEmail;
    protected String labHeadName;
    protected String labHeadEmail;
    protected String investigatorName;
    protected String investigatorEmail;
    protected String dataAnalystName;
    protected String dataAnalystEmail;
    protected String otherContactEmails;
    protected String dataAccessEmails;
    protected String qcAccessEmails;
    protected String strand; // only for RNASeq
    protected String libraryType; // only for RNASeq
    protected List<RequestSample> requestSamples;
    protected List<String> pooledNormals;

    public IgoRequest(String requestId) {
        this.requestId = requestId;
    }

    public IgoRequest(String requestId, List<RequestSample> requestSamples) {
        this.requestId = requestId;
        this.requestSamples = requestSamples;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestId() {
        return requestId;
    }

    public String getRecipe() {
        return recipe;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public String getPiEmail() {
        return piEmail;
    }

    public String getLabHeadName() {
        return labHeadName;
    }

    public String getLabHeadEmail() {
        return labHeadEmail;
    }

    public String getInvestigatorName() {
        return investigatorName;
    }

    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    public String getDataAnalystName() {
        return dataAnalystName;
    }

    public String getDataAnalystEmail() {
        return dataAnalystEmail;
    }

    public String getOtherContactEmails() {
        return otherContactEmails;
    }

    public String getDataAccessEmails() {
        return dataAccessEmails;
    }

    public String getQcAccessEmails() {
        return qcAccessEmails;
    }

    public String getStrand() {
        return strand;
    }

    public String getLibraryType() {
        return libraryType;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<RequestSample> getSamples() {
        return requestSamples;
    }

    public List<String> getPooledNormals() {
        return pooledNormals;
    }
}