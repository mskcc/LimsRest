package org.mskcc.limsrest.limsapi;

import com.fasterxml.jackson.annotation.*;

@Deprecated
public class PairSummary {
    private String tumor;
    private String normal;
    private String sampleName;
    private String category;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleName() { return sampleName; }

    public void setSampleName(String sampleName) { this.sampleName = sampleName;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTumor() { return tumor; }

    public void setTumor(String tumor) { this.tumor = tumor; }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getNormal() { return normal; }

    public void setNormal(String normal) { this.normal = normal; }
}