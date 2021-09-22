package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.util.Messages;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A queued task that takes a request id or a banked sample names and returns multiple banked samples.
 *
 * @author Aaron Gabow
 */
public class GetBanked extends LimsTask {
    private static final Log log = LogFactory.getLog(GetBanked.class);

    protected String project;
    protected String[] sampleNames;
    protected String investigator;
    protected String serviceId;

    public void init(String project) {
        this.project = project;
    }

    public void init(String[] names) {
        if (names != null)
            this.sampleNames = names.clone();
    }

    public void initInvestigator(String name) {
        this.investigator = name;
    }

    public void initServiceId(String id) {
        this.serviceId = id;
    }

    /*
     * Typical where strings like: "where Investigator = 'ohc'" or "where ServiceId = 'IGO-011713'"
     */
    protected static String buildSqlWhereString(String project, String[] sampleNames, String serviceId, String investigator) {
        boolean isFirstCondition = true;
        StringBuilder sqlWhere = new StringBuilder();
        if (project != null) {
            sqlWhere.append("RequestId = '");
            sqlWhere.append(project);
            sqlWhere.append("'");
            isFirstCondition = false;
        }
        if (sampleNames != null) {
            if (!isFirstCondition) {
                sqlWhere.append(" AND ");
            }
            sqlWhere.append("OtherSampleId in (");
            for (int i = 0; i < sampleNames.length - 1; i++) {
                sqlWhere.append("'");
                sqlWhere.append(sampleNames[i]);
                sqlWhere.append("',");
            }
            sqlWhere.append("'");
            sqlWhere.append(sampleNames[sampleNames.length - 1]);
            sqlWhere.append("'");
            sqlWhere.append(")");
            isFirstCondition = false;
        }
        if (serviceId != null) {
            if (!isFirstCondition) {
                sqlWhere.append(" AND ");
            }
            sqlWhere.append("ServiceId = '");
            sqlWhere.append(serviceId);
            sqlWhere.append("'");
            isFirstCondition = false;
        }
        if (investigator != null) {
            if (!isFirstCondition) {
                sqlWhere.append(" AND ");
            }
            sqlWhere.append("Investigator = '");
            sqlWhere.append(investigator);
            sqlWhere.append("'");
        }
        return sqlWhere.toString();
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        LinkedList<SampleSummary> sampleInfoList = new LinkedList();

        try {
            if (project == null && sampleNames == null && serviceId == null && investigator == null) {
                throw new LimsException("You must provide at least one - a request, a sample name, an investigator or a service id");
            }
            String sqlWhereString = buildSqlWhereString(project, sampleNames, serviceId, investigator);
            log.info("GetBanked BankedSample SQL - BankedSample where " + sqlWhereString);

            List<DataRecord> banked = dataRecordManager.queryDataRecords("BankedSample", sqlWhereString, user);
            for (DataRecord sample : banked) {
                SampleSummary ss = new SampleSummary();
                Map<String, Object> sampleFields = sample.getFields(user);

                ss.setAssay((String) sampleFields.get("Assay"));
                ss.setBarcodeId(sample.getStringVal("BarcodeId", user));
                ss.setCellCount(sample.getStringVal("CellCount", user));
                ss.setClinicalInfo((String) sampleFields.get("ClinicalInfo"));
                ss.setCmoPatientId((String) sampleFields.get("CMOPatientId"));
                ss.setCollectionYear((String) sampleFields.get("CollectionYear"));
                ss.setColPosition(sample.getStringVal("ColPosition", user));
                Double concentration = (Double) sampleFields.get("Concentration");
                if (concentration != null)
                    ss.addConcentration(concentration);
                ss.addConcentrationUnits((String) sampleFields.get("ConcentrationUnits"));
                Long date = (Long) sampleFields.get("DateCreated");
                if (date != null)
                    ss.setDropOffDate(date);
                Double purity = (Double) sampleFields.get("EstimatedPurity");
                if (purity != null)
                    ss.setEstimatedPurity(purity);
                ss.setGender((String) sampleFields.get("Gender"));
                ss.setGeneticAlterations((String) sampleFields.get("GeneticAlterations"));
                ss.setInvestigator(sample.getStringVal("Investigator", user));
                ss.setMicronicTubeBarcode(sample.getStringVal("MicronicTubeBarcode", user));
                ss.setNaToExtract(sample.getPickListVal("NAtoExtract", user));
                ss.setNormalizedPatientId((String) sampleFields.get("NormalizedPatientId"));
                ss.setNumTubes(sample.getStringVal("NumTubes", user));
                ss.addCmoId((String) sampleFields.get("OtherSampleId"));
                ss.setPatientId((String) sampleFields.get("PatientId"));
                ss.setPlateId(sample.getStringVal("PlateId", user));
                ss.setPlatform((String) sampleFields.get("Platform"));
                ss.setPreservation((String) sampleFields.get("Preservation"));
                ss.setRecipe(sample.getStringVal("Recipe", user));
                ss.setRecordId((Long) sampleFields.get("RecordId"));
                ss.addRequest((String) sampleFields.get("RequestId"));
                ss.setReadSummary(sample.getStringVal("RequestedReads", user));
                if (sampleFields.containsKey("RequestedReads")){
                    ss.setRequestedReads((String) sampleFields.get("RequestedReads"));
                }
                else{
                    ss.setRequestedReads("");
                }
                if (sampleFields.containsKey("RequestedCoverage")){
                    ss.setRequestedCoverage((String)sampleFields.get("RequestedCoverage"));
                }
                else{
                    ss.setRequestedCoverage("");
                }
                ss.setRowPosition(sample.getStringVal("RowPosition", user));
                ss.setRunType(sample.getStringVal("RunType", user));
                ss.setSampleClass(sample.getStringVal("SampleClass", user));
                ss.setSampleType(sample.getStringVal("SampleType", user));
                ss.setServiceId((String) sampleFields.get("ServiceId"));
                ss.setSpecimenType(sample.getStringVal("SpecimenType", user));
                ss.setSpikeInGenes((String) sampleFields.get("SpikeInGenes"));
                ss.setTissueType((String) sampleFields.get("TissueSite"));
                ss.setTubeId((String) sampleFields.get("TubeBarcode"));
                ss.setTumorOrNormal((String) sampleFields.get("TumorOrNormal"));
                if (sampleFields.containsKey("TumorType"))
                    ss.setTumorType((String) sampleFields.get("TumorType"));
                ss.addExpName((String) sampleFields.get("UserSampleID"));
                Integer numberOfAmplicons = (Integer) sampleFields.get("NumberOfAmplicons");
                if(numberOfAmplicons != null) {
                    ss.setNumberOfAmplicons(numberOfAmplicons);
                }
                Double volume = (Double) sampleFields.get("Volume");
                if (volume != null)
                    ss.addVolume(volume);

                if (sampleFields.containsKey("Organism") && sampleFields.get("Organism") != null && !sampleFields.get("Organism").equals("")) {
                    ss.setOrganism((String) sampleFields.get("Organism"));
                } else if (sampleFields.containsKey("Species")) {
                    ss.setOrganism((String) sampleFields.get("Species"));
                }

                if ("ERROR".equals(ss.getBaseId())) {
                    ss.addBaseId("");
                }
                sampleInfoList.add(ss);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            SampleSummary ss = new SampleSummary();
            ss.addCmoId(Messages.ERROR_IN + " GetBanked: " + e.getMessage());
            sampleInfoList.addFirst(ss);
            return sampleInfoList;
        }

        return sampleInfoList;
    }
}