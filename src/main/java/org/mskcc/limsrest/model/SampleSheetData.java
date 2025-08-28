package org.mskcc.limsrest.model;

import java.util.List;
import java.util.Map;

/**
 * Model class representing Illumina sample sheet data structure
 */
public class SampleSheetData {
    private String runId;
    private String flowCellId;
    private String instrument;
    private String date;
    private String application;
    private List<String> reads;
    private Integer barcodeMismatches1;
    private Integer barcodeMismatches2;
    private boolean isDualBarcoded;
    private List<SampleSheetRow> samples;
    private List<String> warnings;
    private String csvContent;  // For CSV format response
    
    public SampleSheetData() {}
    
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    
    public String getFlowCellId() { return flowCellId; }
    public void setFlowCellId(String flowCellId) { this.flowCellId = flowCellId; }
    
    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }
    
    public List<String> getReads() { return reads; }
    public void setReads(List<String> reads) { this.reads = reads; }
    
    public Integer getBarcodeMismatches1() { return barcodeMismatches1; }
    public void setBarcodeMismatches1(Integer barcodeMismatches1) { this.barcodeMismatches1 = barcodeMismatches1; }
    
    public Integer getBarcodeMismatches2() { return barcodeMismatches2; }
    public void setBarcodeMismatches2(Integer barcodeMismatches2) { this.barcodeMismatches2 = barcodeMismatches2; }
    
    public boolean isDualBarcoded() { return isDualBarcoded; }
    public void setDualBarcoded(boolean dualBarcoded) { isDualBarcoded = dualBarcoded; }
    
    public List<SampleSheetRow> getSamples() { return samples; }
    public void setSamples(List<SampleSheetRow> samples) { this.samples = samples; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public String getCsvContent() { return csvContent; }
    public void setCsvContent(String csvContent) { this.csvContent = csvContent; }
} 