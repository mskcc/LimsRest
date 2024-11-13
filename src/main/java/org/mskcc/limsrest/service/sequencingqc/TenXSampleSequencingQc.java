package org.mskcc.limsrest.service.sequencingqc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class TenXSampleSequencingQc {
    String sampleId;
    String otherSampleId;
    String sequencerRunFolder;
    String seqQCStatus;
    Long antibodyReadsPerCell;
    double cellNumber;
    double chCellNumber;
    String cellsAssignedToSample;
    int fractionUnrecognized;
    double meanReadsPerCell;
    int chMeanReadsPerCell;
    double atacMeanRawReadsPerCell;
    double meanReadsPerSpot;
    int medianUMIsPerCellBarcode;
    double medianGenesOrFragmentsPerCell;
    double atacMedianHighQulityFragPerCell;
    double medianIGLUmisPerCell;
    double medianTraIghUmisPerCell;
    double medianTrbIgkUmisPerCell;
    double readsMappedConfidentlyToGenome;
    double atacConfidentlyMappedReadsPair;
    double readsMappedToTranscriptome;
    double vdjReadsMapped;
    double readMappedConfidentlyToProbSet;
    double samplesAssignedAtLeastOneCell;
    double seqSaturation;
    double chSeqSaturation;
    int totalReads;
    int chTotalReads;
    int atacTotalReads;

    public Map<String, Object> getTenXSequencingQcValues() {
        Map<String, Object> tenXQcValues = new HashMap<>();
        tenXQcValues.put("SampleId", this.sampleId);
        tenXQcValues.put("OtherSampleId", this.otherSampleId);
        tenXQcValues.put("SequencerRunFolder", this.sequencerRunFolder);
        tenXQcValues.put("SeqQCStatus", this.seqQCStatus);
        tenXQcValues.put("AntibodyReadsPerCell", this.antibodyReadsPerCell);
        tenXQcValues.put("CellNumber", this.cellNumber);
        tenXQcValues.put("ChCellNumber", this.chCellNumber);
        tenXQcValues.put("CellsAssignedToSample", this.cellsAssignedToSample);
        tenXQcValues.put("FractionUnrecognized", this.fractionUnrecognized);
        tenXQcValues.put("MeanReadsPerCell", this.meanReadsPerCell);
        tenXQcValues.put("ChMeanReadsPerCell", this.chMeanReadsPerCell);
        tenXQcValues.put("AtacMeanRawReadsPerCell", this.atacMeanRawReadsPerCell);
        tenXQcValues.put("MeanReadsPerSpot", this.meanReadsPerSpot);
        tenXQcValues.put("MedianchUmisPerCellBarcode", this.medianUMIsPerCellBarcode);
        tenXQcValues.put("MedianGenesOrFragmentsPerCell", this.medianGenesOrFragmentsPerCell);
        tenXQcValues.put("AtacMedianHighQultyFragPerCell", this.atacMedianHighQulityFragPerCell);
        tenXQcValues.put("MedianIGLUmisPerCell", this.medianIGLUmisPerCell);
        tenXQcValues.put("MedianTraIghUmisPerCell", this.medianTraIghUmisPerCell);
        tenXQcValues.put("MedianTrbIgkUmisPerCell", this.medianTrbIgkUmisPerCell);
        tenXQcValues.put("ReadsMappedConfidentlyToGenome", this.readsMappedConfidentlyToGenome);
        tenXQcValues.put("AtacConfidentlyMappedReadsPair", this.atacConfidentlyMappedReadsPair);
        tenXQcValues.put("ReadsMappedToTranscriptome", this.readsMappedToTranscriptome);
        tenXQcValues.put("VdjReadsMApped", this.vdjReadsMapped);
        tenXQcValues.put("ReadMappedConfidentlyToProbSet", this.readMappedConfidentlyToProbSet);
        tenXQcValues.put("SamplesAssignedAtLeastOneCell", this.samplesAssignedAtLeastOneCell);
        tenXQcValues.put("SeqSaturation", this.seqSaturation);
        tenXQcValues.put("ChSeqSaturation", this.chSeqSaturation);
        tenXQcValues.put("TotalReads", this.totalReads);
        tenXQcValues.put("ChTotalReads", this.chTotalReads);
        tenXQcValues.put("AtacTotalReads", this.atacTotalReads);
        return tenXQcValues;
    }
}
