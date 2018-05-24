package org.mskcc.limsrest.limsapi.promote;

import org.apache.log4j.Logger;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.Sample;

public class BankedSampleToSampleConverter {
    private static final Logger LOGGER = Logger.getLogger(BankedSampleToSampleConverter.class);

    public Sample convert(BankedSample bankedSample, String uuid, String newIgoId, String
            assignedRequestId) {
        Sample sample = new Sample(newIgoId);

        sample.setAltId(uuid);
        sample.setAssay(bankedSample.getAssay());

        sample.setCellCount(bankedSample.getCellCount());
        sample.setClinicalInfo(bankedSample.getClinicalInfo());
        sample.setCMOSampleClass(bankedSample.getSampleClass());
        sample.setCmoPatientId(bankedSample.getCMOPatientId());
        sample.setCollectionYear(bankedSample.getCollectionYear());
        sample.setColPosition(bankedSample.getColPosition());
        sample.setConcentration(bankedSample.getConcentration());
        sample.setConcentrationUnits(bankedSample.getConcentrationUnits());

        sample.setEstimatedPurity(bankedSample.getEstimatedPurity());
        sample.setExemplarSampleStatus(org.mskcc.util.Constants.RECEIVED);
        sample.setExemplarSampleType(bankedSample.getSampleType());

        sample.setGender(bankedSample.getGender());
        sample.setGeneticAlterations(bankedSample.getGeneticAlterations());

        sample.setMicronicTubeBarcode(bankedSample.getMicronicTubeBarcode());

        sample.setNAtoExtract(bankedSample.getNAtoExtract());

        sample.setOtherSampleId(bankedSample.getOtherSampleId());
        sample.setOrganism(bankedSample.getOrganism());

        sample.setPatientId(bankedSample.getPatientId());
        sample.setPlatform(bankedSample.getPlatform());
        sample.setPreservation(bankedSample.getPreservation());

        sample.setReceivedQuantity(bankedSample.getVolume());
        sample.setRecipe(bankedSample.getRecipe());
        sample.setRequestId(assignedRequestId);
        sample.setRowPosition(bankedSample.getRowPosition());

        sample.setSampleId(newIgoId);
        sample.setSampleOrigin(bankedSample.getSampleOrigin());
        sample.setSpecimenType(bankedSample.getSpecimenType());
        sample.setSpecies(bankedSample.getSpecies());
        sample.setSpikeInGenes(bankedSample.getSpikeInGenes());

        sample.setTissueLocation(bankedSample.getTissueSite());
        sample.setTubeBarcode(bankedSample.getTubeBarcode());
        sample.setTumorOrNormal(bankedSample.getTumorOrNormal());
        sample.setTumorType(bankedSample.getTumorType());

        sample.setUserSampleID(bankedSample.getUserSampleID());

        LOGGER.info(String.format("Using uuid: %s for sample: %s", uuid, sample.getIgoId()));

        return sample;
    }
}


