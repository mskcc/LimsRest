package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class TenXStats {
    String sampleId;
    String otherSampleId;
    String request;
    String sequencerRunFolder;
    String seqQCStatus;
    Long antibodyReadsPerCell;
    int cellNumber;
    int chCellNumber;
    String cellsAssignedToSample;
    int fractionUnrecognized;
    int meanReadsPerCell;
    int chMeanReadsPerCell;
    int atacMeanRawReadsPerCell;
    int meanReadsPerSpot;
    int medianUMIsPerCellBarcode;
    int medianGenesOrFragmentsPerCell;
    int atacMedianHighQulityFragPerCell;
    int medianIGLUmisPerCell;
    int medianTraIghUmisPerCell;
    int medianTrbIgkUmisPerCell;
    int readsMappedConfidentlyToGenome;
    int atacConfidentlyMappedReadsPair;
    int readsMappedToTranscriptome;
    int vdjReadsMapped;
    int readMappedConfidentlyToProbSet;
    int samplesAssignedAtLeastOneCell;
    int seqSaturation;
    int chSeqSaturation;
    int totalReads;
    int chTotalReads;
    int atacTotalReads;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleId() {
        return this.sampleId;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSampleName() {
        return this.otherSampleId;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getRequest() {
        return this.request;
    }
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getSeqRunFolder() {
        return this.sequencerRunFolder;
    }

}
