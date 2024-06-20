package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sloan.cmo.objects.Sample;
import lombok.*;

import java.util.List;

@Setter @ToString
@NoArgsConstructor @AllArgsConstructor
public class PoolInfo {
    private List<DataRecord> librarySample;
    private BarcodeSummary sampleBarcode;
}
