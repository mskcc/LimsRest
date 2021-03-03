package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class RequestSampleList implements Serializable {
    private String requestId;
    private String recipe;
    private String projectManagerName;
    private String piEmail;
    private String labHeadName;
    private String labHeadEmail;
    private String investigatorName;
    private String investigatorEmail; // only for RNASeq
    private String dataAnalystName; // only for RNASeq
    private String dataAnalystEmail;
    private String otherContactEmails;
    private String dataAccessEmails;
    private String qcAccessEmails;
    private String strand;
    private String libraryType;
    private Boolean cmoRequest;
    private Boolean bicAnalysis;
    private List<RequestSample> samples;
    private List<String> pooledNormals;

    public RequestSampleList() {}

    public RequestSampleList(String requestId) {
        this.requestId = requestId;
    }

    public RequestSampleList(String requestId, List<RequestSample> samples) {
        this.requestId = requestId;
        this.samples = samples;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public String getPiEmail() {
        return piEmail;
    }

    public void setPiEmail(String piEmail) {
        this.piEmail = piEmail;
    }

    public String getLabHeadName() {
        return labHeadName;
    }

    public void setLabHeadName(String labHeadName) {
        this.labHeadName = labHeadName;
    }

    public String getLabHeadEmail() {
        return labHeadEmail;
    }

    public void setLabHeadEmail(String labHeadEmail) {
        this.labHeadEmail = labHeadEmail;
    }

    public String getInvestigatorName() {
        return investigatorName;
    }

    public void setInvestigatorName(String investigatorName) {
        this.investigatorName = investigatorName;
    }

    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    public void setInvestigatorEmail(String investigatorEmail) {
        this.investigatorEmail = investigatorEmail;
    }

    public String getDataAnalystName() {
        return dataAnalystName;
    }

    public void setDataAnalystName(String dataAnalystName) {
        this.dataAnalystName = dataAnalystName;
    }

    public String getDataAnalystEmail() {
        return dataAnalystEmail;
    }

    public void setDataAnalystEmail(String dataAnalystEmail) {
        this.dataAnalystEmail = dataAnalystEmail;
    }

    public String getOtherContactEmails() {
        return otherContactEmails;
    }

    public void setOtherContactEmails(String otherContactEmails) {
        this.otherContactEmails = otherContactEmails;
    }

    public String getDataAccessEmails() {
        return dataAccessEmails;
    }

    public void setDataAccessEmails(String dataAccessEmails) {
        this.dataAccessEmails = dataAccessEmails;
    }

    public String getQcAccessEmails() {
        return qcAccessEmails;
    }

    public void setQcAccessEmails(String qcAccessEmails) {
        this.qcAccessEmails = qcAccessEmails;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getLibraryType() {
        return libraryType;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType = libraryType;
    }

    public Boolean getCmoRequest() {
        return cmoRequest;
    }

    public void setCmoRequest(Boolean cmoRequest) {
        this.cmoRequest = cmoRequest;
    }

    public Boolean getBicAnalysis() {
        return bicAnalysis;
    }

    public void setBicAnalysis(Boolean bicAnalysis) {
        this.bicAnalysis = bicAnalysis;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<RequestSample> getSamples() {
        return samples;
    }

    public void setSamples(List<RequestSample> samples) {
        this.samples = samples;
    }

    public List<String> getPooledNormals() {
        return pooledNormals;
    }

    public void setPooledNormals(List<String> pooledNormals) {
        this.pooledNormals = pooledNormals;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
