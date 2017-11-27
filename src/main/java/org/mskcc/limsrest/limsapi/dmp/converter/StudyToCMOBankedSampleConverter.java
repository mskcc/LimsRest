package org.mskcc.limsrest.limsapi.dmp.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.mskcc.domain.RequestSpecies;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.dmp.CMOSampleRequestDetailsResponse;
import org.mskcc.limsrest.limsapi.dmp.Study;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudyToCMOBankedSampleConverter implements DMPToBankedSampleConverter {
    public static final String WELL_POSITION_REGEX = "([A-Z]+)([0-9]+)";
    private static final Logger LOGGER = Logger.getLogger(StudyToCMOBankedSampleConverter.class);

    @Override
    public BankedSample convert(Study study) {
        BankedSample bankedSample = new BankedSample(study.getInvestigatorSampleId());
        WellPosition wellPosition = getWellPosition(study.getWellPosition());

        bankedSample.setBarcodeId(getValue(study.getIndex()));
        bankedSample.setCollectionYear(getValue(study.getCollectionYear()));

        bankedSample.setColPosition(getValue(wellPosition.column));
        bankedSample.setConcentration(study.getConcentration());
        bankedSample.setConcentrationUnits(CMOSampleRequestDetailsResponse.Content.CONCENTRATION_UNITS);
        bankedSample.setGender(getValue(study.getSex()));
        bankedSample.setInvestigator(getValue(study.getPiName()));
        bankedSample.setNAtoExtract(getValue(study.getNucleidAcidType()));
        bankedSample.setOrganism(RequestSpecies.HUMAN.getValue());
        bankedSample.setOtherSampleId(getValue(study.getInvestigatorSampleId()));

        bankedSample.setPatientId(getValue(getPatientId(study.getDmpId())));
        bankedSample.setPlateId(getValue(study.getBarcodePlateId()));
        bankedSample.setPreservation(getValue(study.getPreservation()));
        bankedSample.setRowPosition(getValue(wellPosition.row));

        bankedSample.setSampleClass(getValue(study.getSampleClass()));
        bankedSample.setSampleOrigin(getSampleOrigin(study.getPreservation()));
        bankedSample.setSpecimenType(getValue(study.getSpecimenType()));
        bankedSample.setTumorType(getValue(study.getTumorType()));
        bankedSample.setUserSampleID(getValue(study.getInvestigatorSampleId()));
        bankedSample.setVolume(study.getVolume());

        return bankedSample;
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
        WellPosition emptyWellPosition = new WellPosition("", "");

        if (StringUtils.isEmpty(wellPosition))
            return emptyWellPosition;

        Pattern pattern = Pattern.compile(WELL_POSITION_REGEX);
        Matcher matcher = pattern.matcher(wellPosition);

        if (matcher.matches()) {
            String rowPosition = matcher.group(1);
            String columnPosition = matcher.group(2);
            return new WellPosition(rowPosition, columnPosition);
        }

        LOGGER.warn(String.format("Unable to resolve Row and/or Column Position from Well Position: %s", wellPosition));
        return emptyWellPosition;
    }

    private String getValue(String dmpValue) {
        return CMOSampleRequestDetailsResponse.Content.getStringCMOValueIfExists(dmpValue);
    }

    private String getSampleOrigin(String preservation) {
        return CMOSampleRequestDetailsResponse.Content.getCMOOrigin(preservation);
    }

    class WellPosition {
        private final String row;
        private final String column;

        public WellPosition(String row, String column) {
            this.row = row;
            this.column = column;
        }
    }
}
