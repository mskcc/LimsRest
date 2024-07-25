package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.velox.api.datarecord.DataRecord;
import com.velox.sloan.cmo.objects.Sample;
import lombok.*;

import java.util.List;

@Setter @ToString
@NoArgsConstructor @AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PoolInfo {
    private List<DataRecord> librarySample;
    private BarcodeSummary sampleBarcode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<DataRecord> getLibrarySample() {
        return librarySample;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public BarcodeSummary getSampleBarcode() {
        return sampleBarcode;
    }
}
