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

    public Map<String, Object> getTenXSequencingQcValues() {
        Map<String, Object> tenXQcValues = new HashMap<>();
        tenXQcValues.put("SampleId", this.sampleId);
        tenXQcValues.put("OtherSampleId", this.otherSampleId);
        tenXQcValues.put("Request", this.request);
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
