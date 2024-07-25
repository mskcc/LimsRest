package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BarcodeSummary {
    private String name;
    private String barcodId; // Typo
    private String barcodeTag;

    public BarcodeSummary(String barcodId, String barcodeTag) {
        this.barcodId = barcodId;
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
