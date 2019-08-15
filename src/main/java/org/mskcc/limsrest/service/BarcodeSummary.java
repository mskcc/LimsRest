package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;

public class BarcodeSummary {
    private String name;
    private String barcodId; // Typo
    private String barcodeTag;

    public BarcodeSummary(String name, String id, String barcodeTag) {
        this.name = name;
        this.barcodId = id;
        this.barcodeTag = barcodeTag;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getName() {
        return name;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBarcodId() {
        return barcodId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getBarcodeTag() {
        return barcodeTag;
    }
}
