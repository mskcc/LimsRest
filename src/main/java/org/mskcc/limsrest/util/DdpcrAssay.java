package org.mskcc.limsrest.util;


import java.util.Map;

// base class for ddPCR Assays
public class DdpcrAssay {

    public String assayName;
    public String nameAminoAcidChange;
    public String nameNucleotideChange;
    public Double assayVolume;
    public String assayType;
    public String fluorophore;
    public String assayReference;
    public String species;
    public String enzyme;
    public Long expirationDate;
    public String storageLocationBarcode;
    public String rowPosition;
    public String colPosition;
    public String projectNumber;
    public String optimalTemperature;
    public Integer ramp;
    public Integer denaturationTime;
    public Integer annealingAndExtensionTimes;
    public Integer numberOfAdditionalCycles;
    public String needAdditionalReagent;
    public String additionalComments;
    public String assayPassOrFail;
    public String quoteNumber;
    public Long quoteExpirationDate;
    public Long issueDate;
    public String assayAccessionNumber;
    public String assayId;
    public String miqeContext;
    public Long recordId;
    public String storageUnitPath;

    public DdpcrAssay() {
    }

    public DdpcrAssay(Map<String, Object> sampleFields) {
        this.assayName = (String) sampleFields.get("AssayName");
        this.nameAminoAcidChange = (String) sampleFields.get("NameAminoAcidChange");
        this.nameNucleotideChange = (String) sampleFields.get("NameNucleotideChange");
        this.assayVolume = (Double) sampleFields.get("AssayVolume");
        this.assayType = (String) sampleFields.get("AssayType");
        this.fluorophore = (String) sampleFields.get("Fluorophore");
        this.assayReference = (String) sampleFields.get("AssayReference");
        this.species = (String) sampleFields.get("Species");
        this.enzyme = (String) sampleFields.get("Enzyme");
        this.expirationDate = (Long) sampleFields.get("ExpirationDate");
        this.storageLocationBarcode = (String) sampleFields.get("StorageLocationBarcode");
        this.rowPosition = (String) sampleFields.get("RowPosition");
        this.colPosition = (String) sampleFields.get("ColPosition");
        this.projectNumber = (String) sampleFields.get("ProjectNumber");
        this.optimalTemperature = (String) sampleFields.get("OptimalTemperature");
        this.ramp = (Integer) sampleFields.get("Ramp");
        this.denaturationTime = (Integer) sampleFields.get("DenaturationTime");
        this.annealingAndExtensionTimes = (Integer) sampleFields.get("AnnealingAndExtensionTimes");
        this.numberOfAdditionalCycles = (Integer) sampleFields.get("NumberOfAdditionalCycles");
        this.needAdditionalReagent = (String) sampleFields.get("NeedAdditionalReagent");
        this.additionalComments = (String) sampleFields.get("AdditionalComments");
        this.assayPassOrFail = (String) sampleFields.get("AssayPassOrFail");
        this.quoteNumber = (String) sampleFields.get("QuoteNumber");
        this.quoteExpirationDate = (Long) sampleFields.get("QuoteExpirationDate");
        this.issueDate = (Long) sampleFields.get("IssueDate");
        this.assayAccessionNumber = (String) sampleFields.get("AssayAccessionNumber");
        this.assayId = (String) sampleFields.get("AssayId");
        this.miqeContext = (String) sampleFields.get("MiqeContext");
        this.recordId = (Long) sampleFields.get("RecordId");
        this.storageUnitPath = (String) sampleFields.get("StorageUnitPath");
    }

}
