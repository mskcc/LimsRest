package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GetSampleQc extends LimsTask {
    private Log log = LogFactory.getLog(GetSampleQc.class);
    protected String[] projectList;

    public void init(String[] project) {
        this.projectList = project;
    }

    @Override
    @PreAuthorize("hasRole('READ')")
    public Object execute(VeloxConnection conn) {
        List<RequestSummary> rss = new LinkedList<>();
        try {
            String projects = Stream.of(projectList).collect(Collectors.joining("','", "'", "'"));
            log.info("Project " + projects);
            List<DataRecord> requestList = this.dataRecordManager.queryDataRecords("Request", "RequestId in (" + projects + ")", this.user);
            HashMap<String, String>  alt2base = new HashMap<>(); // AltId->SampleId
            for (DataRecord r : requestList) {
                DataRecord[] baseSamples = r.getChildrenOfType("Sample", user);
                for (DataRecord bs : baseSamples){
                    try {
                        alt2base.put(bs.getStringVal("AltId", this.user), bs.getStringVal("SampleId", this.user));
                    } catch (NullPointerException npe){
                        log.info("Problem trying to populate base id mapping");
                    }
                }

                String project = r.getStringVal("RequestId", user);
                RequestSummary rs = new RequestSummary(project);
                annotateRequestSummary(rs, r); // fill in all data at request level except sample number
                rs.setSampleNumber((new Short(r.getShortVal("SampleNumber", user))).intValue());

                List<DataRecord> qcs;
                qcs = this.dataRecordManager.queryDataRecords("SeqAnalysisSampleQC", "Request = '" + project + "'", this.user);
                if (qcs.size() == 0){
                    qcs = r.getDescendantsOfType("SeqAnalysisSampleQC", this.user);
                }
                for (DataRecord qc : qcs) {
                    log.info("QCING ");
                    SampleSummary ss = new SampleSummary();
                    SampleQcSummary qcSummary = new SampleQcSummary();
                    DataRecord parentSample = (DataRecord)qc.getParentsOfType("Sample", this.user).get(0);
                    annotateQcSummary(qcSummary, qc);
                    if (parentSample != null) {
                        annotateSampleSummary(ss, parentSample);
                        try {
                            if (!parentSample.getStringVal("AltId", this.user).equals("")){
                                ss.addBaseId(alt2base.get(parentSample.getStringVal("AltId", this.user)));
                                log.info(parentSample.getStringVal("AltId", this.user));
                                log.info(alt2base.get(parentSample.getStringVal("AltId", this.user)));
                            } else{
                                DataRecord searchSample = parentSample;
                                boolean canSearch = true;
                                while (searchSample.getParentsOfType("Request", this.user).size() == 0 && canSearch){
                                    List<DataRecord> searchParents = searchSample.getParentsOfType("Sample", this.user);
                                    if(searchParents.size() == 0){
                                        canSearch = false;
                                    } else{
                                        searchSample = searchParents.get(0);
                                    }
                                }
                                ss.addBaseId(searchSample.getStringVal("SampleId", this.user));
                            }
                        } catch (Exception e){
                            log.info("Problem trying to access base id mapping");
                        }
                        DataRecord[] childSamples = parentSample.getChildrenOfType("Sample", this.user);
                        if (childSamples.length > 0){
                            ss.setInitialPool(childSamples[0].getStringVal("SampleId", this.user));
                        }
                        DataRecord[] qcData = parentSample.getChildrenOfType("QCDatum", this.user);
                        long created = -1;
                        for (DataRecord datum : qcData){
                            Map<String, Object> qcFields = datum.getFields(this.user);
                            if (qcFields.containsKey("MapToSample") && (boolean)qcFields.get("MapToSample") && (long)qcFields.get("DateCreated") > created){
                                qcSummary.setQcControl((Double)qcFields.get("CalculatedConcentration"));
                                qcSummary.setQcUnits((String)qcFields.get("ConcentrationUnits"));
                                created = (Long)qcFields.get("DateCreated");
                            }
                            if (qcFields.containsKey("DatumType") && qcFields.get("DatumType").equals("Quant-it")){
                                qcSummary.setQuantIt((Double)qcFields.get("CalculatedConcentration"));
                                qcSummary.setQuantUnits((String)qcFields.get("ConcentrationUnits"));
                            }
                        }
                        DataRecord[] requirements = parentSample.getChildrenOfType("SeqRequirement", this.user);
                        List<DataRecord> searchParents = parentSample.getAncestorsOfType("Sample", this.user);
                        created = -1;
                        double mass = 0.00000;
                        for (DataRecord protoParent : searchParents){
                            DataRecord[] kProtocols = protoParent.getChildrenOfType("KAPALibPlateSetupProtocol1", this.user);
                            DataRecord[] rProtocols = protoParent.getChildrenOfType("TruSeqRNAProtocol", this.user);
                            DataRecord[] depProtocols = protoParent.getChildrenOfType("TruSeqRiboDepleteProtocol", this.user);
                            for (DataRecord protocol : kProtocols){
                                long protoCreate = protocol.getDateVal("DateCreated", this.user);
                                if (protoCreate  > created){
                                    mass = protocol.getDoubleVal("TargetMassAliq1", this.user);
                                    created = protoCreate;
                                }
                            }
                            for (DataRecord protocol : rProtocols){
                                long protoCreate = protocol.getDateVal("DateCreated", this.user);
                                if (protoCreate  > created){
                                    mass = protocol.getDoubleVal("Aliq1TargetMass", this.user);
                                    created = protoCreate;
                                }
                            }
                            for (DataRecord protocol : depProtocols){
                                long protoCreate = protocol.getDateVal("DateCreated", this.user);
                                if (protoCreate  > created){
                                    mass = protocol.getDoubleVal("Aliq1TargetMass", this.user);
                                    created = protoCreate;
                                }
                            }
                        }
                        if (mass > 0.000001){
                            qcSummary.setStartingAmount(mass);
                        }
                        ListIterator<DataRecord> ancestorIter = searchParents.listIterator();
                        Deque<DataRecord> queue = new LinkedList<>();
                        queue.addLast(parentSample);
                        Set<DataRecord> visited = new HashSet<>();
                        while ((requirements.length == 0) &&  !queue.isEmpty()) {
                            DataRecord current = queue.removeFirst();
                            requirements = current.getChildrenOfType("SeqRequirement", this.user);
                            List<DataRecord> parents = current.getParentsOfType("Sample", this.user);
                            for (DataRecord parent : parents){
                                if (!visited.contains(parent)){
                                    visited.add(parent);
                                    queue.addLast(parent);
                                }
                            }
                        }
                        if (requirements.length > 0){
                            try {
                                ss.setRequestedReadNumber((long)requirements[0].getDoubleVal("RequestedReads", this.user));
                            } catch(NullPointerException npe){
                                ss.setRequestedReadNumber(0);
                            }
                            try {
                                ss.setCoverageTarget((int)requirements[0].getIntegerVal("CoverageTarget", this.user));
                            } catch (NullPointerException npe){
                                ss.setCoverageTarget(0);
                            }
                        }

                        // calculate yield trying to find a Protocol Record to get elution volume and use corresponding sample's concentration to multiply for yield
                        // Jira IGOWEB-1250 - concentration calculation is not 100% correct for all cases but nobody has explained what needs to change
                        try {
                            DataRecord[] protocols = parentSample.getChildrenOfType("DNALibraryPrepProtocol2", this.user);
                            if (protocols.length > 0){
                                ss.setYield(protocols[0].getDoubleVal("ElutionVol", this.user) * parentSample.getDoubleVal("Concentration", this.user));
                            } else{
                                DataRecord[] assignments = parentSample.getChildrenOfType("MolarConcentrationAssignment", this.user);
                                if(assignments.length > 0){
                                    ss.setYield(assignments[0].getDoubleVal("Concentration", this.user));
                                }
                            }
                        } catch (NullPointerException e){
                            ss.setYield(0);
                        }
                    }
                    ss.setQc(qcSummary);
                    rs.addSample(ss);
                }
                rss.add(rs);
            }
        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.info(e.getMessage());
            log.info(sw.toString());
            RequestSummary rs = RequestSummary.errorMessage(e.getMessage());
            rss.add(rs);
        }

        return rss;
    }
}