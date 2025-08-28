package org.mskcc.limsrest.model;

/**
 * Model class representing a single row in an Illumina sample sheet
 */
public class SampleSheetRow {
    private String lane;
    private String sampleId;
    private String samplePlate;
    private String sampleWell;
    private String indexId;
    private String indexTag;
    private String indexTag2;
    private String sampleProject;
    private String baitSet;
    private String description;
    
    // Additional fields for internal tracking
    private String igoId;
    private String otherId;
    private String recipe;
    private String species;
    private String requestId;
    private String pi;
    
    public SampleSheetRow() {}
    
    public String getLane() { return lane; }
    public void setLane(String lane) { this.lane = lane; }
    
    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }
    
    public String getSamplePlate() { return samplePlate; }
    public void setSamplePlate(String samplePlate) { this.samplePlate = samplePlate; }
    
    public String getSampleWell() { return sampleWell; }
    public void setSampleWell(String sampleWell) { this.sampleWell = sampleWell; }
    
    public String getIndexId() { return indexId; }
    public void setIndexId(String indexId) { this.indexId = indexId; }
    
    public String getIndexTag() { return indexTag; }
    public void setIndexTag(String indexTag) { this.indexTag = indexTag; }
    
    public String getIndexTag2() { return indexTag2; }
    public void setIndexTag2(String indexTag2) { this.indexTag2 = indexTag2; }
    
    public String getSampleProject() { return sampleProject; }
    public void setSampleProject(String sampleProject) { this.sampleProject = sampleProject; }
    
    public String getBaitSet() { return baitSet; }
    public void setBaitSet(String baitSet) { this.baitSet = baitSet; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIgoId() { return igoId; }
    public void setIgoId(String igoId) { this.igoId = igoId; }
    
    public String getOtherId() { return otherId; }
    public void setOtherId(String otherId) { this.otherId = otherId; }
    
    public String getRecipe() { return recipe; }
    public void setRecipe(String recipe) { this.recipe = recipe; }
    
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getPi() { return pi; }
    public void setPi(String pi) { this.pi = pi; }
} 