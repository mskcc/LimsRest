package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

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

    public IgoRequest() {}

    public IgoRequest(String requestId) {
        this.requestId = requestId;
    }

    public IgoRequest(String requestId, List<RequestSample> requestSamples) {
        this.requestId = requestId;
        this.requestSamples = requestSamples;
    }

    /**
     * IgoRequest contructor
     * @param requestId
     * @param recipe
     * @param projectManagerName
     * @param piEmail
     * @param labHeadName
     * @param labHeadEmail
     * @param investigatorName
     * @param investigatorEmail
     * @param dataAnalystName
     * @param dataAnalystEmail
     * @param otherContactEmails
     * @param dataAccessEmails
     * @param qcAccessEmails
     * @param strand
     * @param libraryType
     */
    public IgoRequest(String requestId, String recipe, String projectManagerName,
            String piEmail, String labHeadName, String labHeadEmail,
            String investigatorName, String investigatorEmail, String dataAnalystName,
            String dataAnalystEmail, String otherContactEmails, String dataAccessEmails,
            String qcAccessEmails, String strand, String libraryType) {
        this.requestId = requestId;
        this.recipe = recipe;
        this.projectManagerName = projectManagerName;
        this.piEmail = piEmail;
        this.labHeadName = labHeadName;
        this.labHeadEmail = labHeadEmail;
        this.investigatorName = investigatorName;
        this.investigatorEmail = investigatorEmail;
        this.dataAnalystName = dataAnalystName;
        this.dataAnalystEmail = dataAnalystEmail;
        this.otherContactEmails = otherContactEmails;
        this.dataAccessEmails = dataAccessEmails;
        this.qcAccessEmails = qcAccessEmails;
        this.strand = strand;
        this.libraryType = libraryType;
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<RequestSample> getSamples() {
        return requestSamples;
    }

    public void setSamples(List<RequestSample> requestSamples) {
        this.requestSamples = requestSamples;
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
