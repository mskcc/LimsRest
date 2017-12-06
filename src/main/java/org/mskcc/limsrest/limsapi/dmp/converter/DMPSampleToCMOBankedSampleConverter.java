package org.mskcc.limsrest.limsapi.dmp.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.mskcc.domain.RequestSpecies;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.TumorType;
import org.mskcc.limsrest.limsapi.converter.ExternalToBankedSampleConverter;
import org.mskcc.limsrest.limsapi.dmp.CMOSampleRequestDetailsResponse;
import org.mskcc.limsrest.limsapi.dmp.DMPSample;
import org.mskcc.util.tumortype.TumorTypeRetriever;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DMPSampleToCMOBankedSampleConverter implements ExternalToBankedSampleConverter<DMPSample> {
    private static final Logger LOGGER = Logger.getLogger(DMPSampleToCMOBankedSampleConverter.class);

    private static final String WELL_POSITION_REGEX = "([A-Z])([0-9]+)";
    private Map<String, String> tumorTypeToCode;

    public DMPSampleToCMOBankedSampleConverter(TumorTypeRetriever tumorTypeRetriever) {
        this.tumorTypeToCode = getTumorTypeToCode(tumorTypeRetriever);
    }

    public Map<String, String> getTumorTypeToCode(TumorTypeRetriever tumorTypeRetriever) {
        Map<String, String> tumorTypeToCode = new HashMap<>();
        try {
            for (TumorType tumorType : tumorTypeRetriever.retrieve()) {
                tumorTypeToCode.put(String.format("%s:%s", tumorType.getTissueType(), tumorType.getTumorType()),
                        tumorType.getCode());
            }

            tumorTypeToCode.put(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE, "");
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve tumor types oncotree mapping.", e);
        }

        return tumorTypeToCode;
    }

    @Override
    public BankedSample convert(DMPSample dmpSample, long transactionId) {
        LOGGER.info(String.format("Converting study: %s to Banked Sample", dmpSample.getStudySampleId()));

        BankedSample bankedSample = new BankedSample(dmpSample.getInvestigatorSampleId());
        WellPosition wellPosition = getWellPosition(dmpSample.getWellPosition());

        bankedSample.setBarcodeId(getValue(getBarcodeId(dmpSample.getIndex())));
        bankedSample.setCollectionYear(getValue(dmpSample.getCollectionYear()));

        if (wellPosition != null)
            bankedSample.setColPosition(getValue(String.valueOf(wellPosition.column)));

        bankedSample.setConcentration(dmpSample.getConcentration());
        bankedSample.setConcentrationUnits(CMOSampleRequestDetailsResponse.Content.CONCENTRATION_UNITS);
        bankedSample.setServiceId(dmpSample.getTrackingId());
        bankedSample.setGender(getValue(dmpSample.getSex()));
        bankedSample.setInvestigator(getValue(dmpSample.getPiName()));
        bankedSample.setNAtoExtract(getNucleidAcid(dmpSample.getNucleidAcidType()));
        bankedSample.setNonLimsLibraryInput(dmpSample.getDnaInputIntoLibrary());
        bankedSample.setNonLimsLibraryOutput(dmpSample.getReceivedDnaMass());
        bankedSample.setOrganism(RequestSpecies.HUMAN.getValue());
        bankedSample.setOtherSampleId(getValue(dmpSample.getInvestigatorSampleId()));

        bankedSample.setPatientId(getValue(getPatientId(dmpSample.getDmpId())));
        bankedSample.setPlateId(getValue(dmpSample.getBarcodePlateId()));
        bankedSample.setPreservation(getValue(dmpSample.getPreservation()));
        bankedSample.setRowIndex(getRowIndex(wellPosition));

        if (wellPosition != null)
            bankedSample.setRowPosition(getValue(String.valueOf(wellPosition.row)));

        bankedSample.setSampleClass(getSampleClass(dmpSample.getSampleClass()));
        bankedSample.setSampleOrigin(getSampleOrigin(dmpSample.getPreservation()));
        bankedSample.setSampleType(getSampleType(dmpSample.getNucleidAcidType()));
        bankedSample.setServiceId(dmpSample.getTrackingId());
        bankedSample.setSpecimenType(getSpecimenType(dmpSample.getSpecimenType()));
        bankedSample.setTransactionId(transactionId);
        bankedSample.setTumorOrNormal(getValue(getTumorOrNormal(dmpSample.getSampleClass())));
        bankedSample.setTumorType(getValue(getTumorType(tumorTypeToCode, dmpSample.getTumorType())));
        bankedSample.setUserSampleID(getValue(dmpSample.getInvestigatorSampleId()));
        bankedSample.setVolume(dmpSample.getVolume());

        return bankedSample;
    }

    private String getSampleClass(String sampleClass) {
        return CMOSampleRequestDetailsResponse.Content.getCMOSampleClass(sampleClass);
    }

    private String getSampleType(String nucleidAcidType) {
        return CMOSampleRequestDetailsResponse.Content.getSampleType(nucleidAcidType);
    }

    private String getNucleidAcid(String nucleidAcidType) {
        return CMOSampleRequestDetailsResponse.Content.getNucleidAcidToExtract(nucleidAcidType);
    }

    private String getSpecimenType(String specimenType) {
        return CMOSampleRequestDetailsResponse.Content.getCMOSpecimenType(specimenType);
    }

    /**
     * getBarcodeId returns Barcode with removed "0" used for padding. If there are no padding "0" value stays
     * unchanged. If index is not in expected format value stays unchanged.
     * examples:
     * if index = "IDT01" getBarcodeId returns "IDT1"
     * if index = "DMP23" getBarcodeId returns "DMP23"
     *
     * @param index index (barcode) returned by DMP with or without padding "0"
     * @return barcode id without padding "0"
     */
    private String getBarcodeId(String index) {
        if (StringUtils.isEmpty(index))
            return "";

        Pattern pattern = Pattern.compile("([A-Za-z]+)(0*)([0-9]+)");
        Matcher matcher = pattern.matcher(index);

        if (matcher.matches()) {
            String alphaPart = matcher.group(1);
            String numericPart = matcher.group(3);

            String noPaddingBarcode = alphaPart + numericPart;

            if (!Objects.equals(index, noPaddingBarcode))
                LOGGER.info(String.format("Barcode: %s was formatted to IGO format: %s.", index, noPaddingBarcode));

            return noPaddingBarcode;
        }

        return index;
    }

    private int getRowIndex(WellPosition wellPosition) {
        if (wellPosition == null)
            return -1;

        char row = wellPosition.row;
        int column = wellPosition.column;

        int rowIndex = column * 100 + (row - (int) 'A');
        return rowIndex;
    }

    private String getTumorType(Map<String, String> tumorTypeToCode, String tumorType) {
        return tumorTypeToCode.get(tumorType);
    }

    private String getTumorOrNormal(String sampleClass) {
        if (Objects.equals(sampleClass, CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE))
            return sampleClass;
        if (Objects.equals(sampleClass, org.mskcc.util.Constants.NORMAL))
            return org.mskcc.util.Constants.NORMAL;
        return org.mskcc.util.Constants.TUMOR;
    }

    private String getPatientId(String dmpId) {
        if (StringUtils.isEmpty(dmpId)) {
            LOGGER.warn(String.format("DMP ID is empty. Patient ID won't be able to be resolved"));
            return "";
        }

        Pattern pattern = Pattern.compile("(P-[0-9]{7})-.*");
        Matcher matcher = pattern.matcher(dmpId);

        if (!matcher.matches()) {
            LOGGER.warn(String.format("Unable to retrieve Patient if from DMP Id: %s", dmpId));
            return "";
        }

        return matcher.group(1);
    }

    private WellPosition getWellPosition(String wellPosition) {
        WellPosition emptyWellPosition = null;

        try {
            if (StringUtils.isEmpty(wellPosition))
                return emptyWellPosition;

            Pattern pattern = Pattern.compile(WELL_POSITION_REGEX);
            Matcher matcher = pattern.matcher(wellPosition);

            if (matcher.matches()) {
                char rowPosition = matcher.group(1).toUpperCase().charAt(0);
                int columnPosition = Integer.parseInt(matcher.group(2));
                return new WellPosition(rowPosition, columnPosition);
            }

            LOGGER.warn(String.format("Unable to resolve Row and/or Column Position from Well Position: %s",
                    wellPosition));
            return emptyWellPosition;
        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to resolve Row and/or Column Position from Well Position: %s",
                    wellPosition), e);
            return emptyWellPosition;
        }
    }

    private String getValue(String dmpValue) {
        return CMOSampleRequestDetailsResponse.Content.getCMOValueIfExists(dmpValue);
    }

    private String getSampleOrigin(String preservation) {
        return CMOSampleRequestDetailsResponse.Content.getCMOOrigin(preservation);
    }

    class WellPosition {
        private final char row;
        private final int column;

        public WellPosition(char row, int column) {
            this.row = row;
            this.column = column;
        }
    }
}
