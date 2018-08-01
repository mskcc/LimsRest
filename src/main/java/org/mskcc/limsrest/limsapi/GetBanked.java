package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.staticstrings.Messages;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mskcc.util.CommonUtils.runAndCatchNpe;


/**
 * A queued task that takes a request id or a banked sample names and returns multiple banked samples
 *
 * @author Aaron Gabow
 */
@Service
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
                annotateSampleDetailed(ss, sample);

                ss.setInvestigator(sample.getStringVal("Investigator", user));
                ss.setPlateId(sample.getStringVal("PlateId", user));
                ss.setRowPosition(sample.getStringVal("RowPosition", user));
                ss.setColPosition(sample.getStringVal("ColPosition", user));
                ss.setBarcodeId(sample.getStringVal("BarcodeId", user));
                ss.setRecipe(sample.getStringVal("Recipe", user));
                ss.setSpecimenType(sample.getStringVal("SpecimenType", user));
                ss.setReadSummary(sample.getStringVal("RequestedReads", user));
                ss.setCellCount(sample.getStringVal("CellCount", user));
                ss.setMicronicTubeBarcode(sample.getStringVal("MicronicTubeBarcode", user));
                ss.setNaToExtract(sample.getPickListVal("NAtoExtract", user));
                ss.setNumTubes(sample.getStringVal("NumTubes", user));
                ss.setRunType(sample.getStringVal("RunType", user));
                ss.setSampleClass(sample.getStringVal("SampleClass", user));
                ss.setSampleType(sample.getStringVal("SampleType", user));

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

    public void annotateSampleDetailed(SampleSummary ss, DataRecord sample) {
        try {
            Map sampleFields = sample.getFields(user);
            ss.setRecordId((Long) sampleFields.get("RecordId"));
            if (sampleFields.containsKey("Organism") && sampleFields.get("Organism") != null && !sampleFields.get("Organism").equals("")) {
                ss.setSpecies((String) sampleFields.get("Organism"));
            } else if (sampleFields.containsKey("Species")) {
                ss.setSpecies((String) sampleFields.get("Species"));
            }
            ss.setAssay((String) sampleFields.get("Assay"));
            ss.setClinicalInfo((String) sampleFields.get("ClinicalInfo"));
            ss.setCollectionYear((String) sampleFields.get("CollectionYear"));
            if (sampleFields.containsKey("TumorType"))
                ss.setTumorType((String) sampleFields.get("TumorType"));
            ss.setGender((String) sampleFields.get("Gender"));
            ss.addExpName((String) sampleFields.get("UserSampleID"));
            ss.setGeneticAlterations((String) sampleFields.get("GeneticAlterations"));
            ss.setPatientId((String) sampleFields.get("PatientId"));
            ss.setNormalizedPatientId((String) sampleFields.get("NormalizedPatientId"));
            ss.setCmoPatientId((String) sampleFields.get("CMOPatientId"));
            ss.setPreservation((String) sampleFields.get("Preservation"));
            ss.setSpecimenType((String) sampleFields.get("SpecimenType"));
            ss.setSpikeInGenes((String) sampleFields.get("SpikeInGenes"));
            ss.setTubeId((String) sampleFields.get("TubeBarcode"));
            ss.setTissueSite((String) sampleFields.get("TissueSite"));
            ss.addRequest((String) sampleFields.get("RequestId"));
            ss.addCmoId((String) sampleFields.get("OtherSampleId"));
            ss.setTumorOrNormal((String) sampleFields.get("TumorOrNormal"));
            ss.addConcentrationUnits((String) sampleFields.get("ConcentrationUnits"));
            ss.setPlatform((String) sampleFields.get("Platform"));
            ss.setServiceId((String) sampleFields.get("ServiceId"));

            // .getOrDefault() fails if DB has null so just null check in if
            Double volume = (Double) sampleFields.get("Volume");
            if (volume != null)
                ss.addVolume(volume);
            Double concentration = (Double) sampleFields.get("Concentration");
            if (concentration != null)
                ss.addConcentration(concentration);
            Double purity = (Double) sampleFields.get("EstimatedPurity");
            if (purity != null)
                ss.setEstimatedPurity(purity);
            Long date = (Long) sampleFields.get("DateCreated");
            if (date != null)
                ss.setDropOffDate(date);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            ss.addCmoId(Messages.ERROR_IN + " Annotation:" + e.getMessage());
        }
    }
}