package org.mskcc.limsrest.limsapi;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.velox.api.workflow.Workflow;
import com.velox.api.workflow.ActiveWorkflow;
import com.velox.api.workflow.ActiveWorkflowData;
import com.velox.api.workflowmanager.WorkflowManager;
import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandalone;
import com.velox.sapioutils.client.standalone.VeloxStandaloneException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.client.standalone.VeloxExecutable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import java.sql.DriverManager;

/**
 * A queued task that takes shows all samples that need planned for illumina runs 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetReadyForIllumina extends LimsTask 
{
   private Log log = LogFactory.getLog(GetReadyForIllumina.class);
   private HashMap<String, List<String>> request2OutstandingSamples; 

  public void init(){
    request2OutstandingSamples  = new HashMap<>();
  }

 //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){
  List<RunSummary> results = new LinkedList<>(); 
 try{ 
 //   WorkflowManager workflowManager = dataMgmtServer.getWorkflowManager(user);
 //   List<ActiveWorkflowData> activeWorkflows = workflowManager.getActiveWorkflowDataList(user);
/*    List<Long> activeWfIds = new LinkedList<>();
 try{
     WorkflowManager wfMngr = dataMgmtServer.getWorkflowManager(user);
    List<ActiveWorkflowData> workflowDataList = wfMngr.getActiveWorkflowDataList(user);

    for(ActiveWorkflowData active : workflowDataList){
       if(active.getStatus() == ActiveWorkflow.PAUSED && active.getWorkflowName().equals("Normalization of Pooled Libraries")){
          activeWfIds.add(activeG.getActiveWorkflowId());
       }

    }
  } catch(Exception e){
       log.info(e.getMessage());

   }*/
    List<DataRecord> unpooledSamples = dataRecordManager.queryDataRecords("Sample", "ExemplarSampleStatus = 'Ready for - Pooling of Sample Libraries for Sequencing' AND ExemplarSampleType != 'Pooled Library'", user);
    for(DataRecord sample: unpooledSamples){
          double concentration = -1;
          try{
          concentration = sample.getDoubleVal("Concentration", user);
          } catch (NullPointerException npe){}
          DataRecord current = null;
          String barcodeId = "";
          String barcodeSeq = "";
          String sampleId = "";
          String otherId = "";
          try{
             sampleId =  sample.getStringVal("SampleId", user); //sample.getStringVal("SampleId", user);
          } catch(Exception e){}
          try{
             otherId = sample.getStringVal("OtherSampleId", user); //sample.getStringVal("SampleId", user);
          } catch(Exception e){}
          //request level info
          String requestId = "";
          try{
             requestId = sample.getStringVal("RequestId", user);
          } catch(Exception e){}
          String tumorOrNormal = "";

          //info added on Riddhika request 3/10/17
          String plannedSequencer = "";
          String plannedWeek = ""; // from batch protocol
          String plateId = ""; // from sample
          try{
             plateId = sample.getStringVal("RelatedRecord23", user);
          } catch(Exception e){}
          String wellPosition = ""; 
          try{
            wellPosition = sample.getStringVal("ColPosition", user) + sample.getStringVal("RowPosition", user);
          } catch(Exception e){}
          String sampleVol = "";
          try{
             sampleVol = Double.toString(sample.getDoubleVal("Volume", user));


          } catch(Exception e){}
          String concentrationUnits = "";
          try{
             concentrationUnits =  sample.getStringVal("ConcentrationUnits", user);
          } catch(Exception e){}
          String runLength = ""; //from seq request
          String readDepth = ""; // from seq request
          String tubeBarcode = "";
          try{
             tubeBarcode = sample.getStringVal("MicronicTubeBarcode", user);
          } catch(Exception e){}
          LinkedList<DataRecord> fringe = new LinkedList<>(); 
          fringe.push(sample);
          while(fringe.size() > 0 && (barcodeId.equals("")  || plannedSequencer.equals("") || requestId.equals("") || readDepth.equals(""))){
              current = fringe.pop();
              DataRecord[] barcode = current.getChildrenOfType("IndexBarcode", user);
              if(barcode.length > 0){
                try{
                  barcodeId = barcode[0].getStringVal("IndexId", user);
                  barcodeSeq = barcode[0].getStringVal("IndexTag", user);
                  tumorOrNormal = current.getPickListVal("TumorOrNormal", user);
                } catch(NullPointerException npe){}
              }
              DataRecord[] batchPlanning = current.getChildrenOfType("BatchPlanningProtocol", user);
              if(batchPlanning.length > 0){
                 try{
                    plannedSequencer = batchPlanning[0].getStringVal("RunPlan", user);
                    plannedWeek = batchPlanning[0].getStringVal("WeekPlan", user);
                 } catch(NullPointerException npe){}
              }
              DataRecord[] seqReq = current.getChildrenOfType("SeqRequirement", user);
              if(seqReq.length > 0){
                 try{
                    runLength = seqReq[0].getPickListVal("SequencingRunType", user);
                    readDepth = Double.toString(seqReq[0].getDoubleVal("RequestedReads", user));
                 } catch(NullPointerException npe){}
              }
              if(barcodeId.equals("") || plannedSequencer.equals("") || requestId.equals("") || readDepth.equals("")){
                 List<DataRecord> parentSamples = current.getParentsOfType("Sample",  user);
                 for(DataRecord parent : parentSamples){
                     fringe.push(parent);
                 }
              }
          }
          RunSummary summary = new RunSummary("", sampleId);
          summary.setOtherSampleId(otherId);
          summary.setBarcodeId(barcodeId);
          summary.setBarcodeSeq(barcodeSeq);
          summary.setRequestId(requestId);
          summary.setAltConcentration(concentration);
          summary.setConcentrationUnits(concentrationUnits);
          summary.setVolume(sampleVol);
          summary.setTumor(tumorOrNormal);
          summary.setSequencer(plannedSequencer);
          summary.setBatch(plannedWeek);
          summary.setPlateId(plateId);
          summary.setWellPos(wellPosition);
          summary.setRunType(runLength);
          summary.setReadNum(readDepth);
          summary.setTubeBarcode(tubeBarcode);
          summary.setStatus("Ready for - Pooling of Sample Libraries for Sequencing");

          results.add(summary);
    }
    List<DataRecord> pooledSamples = dataRecordManager.queryDataRecords("Sample", "ExemplarSampleStatus = 'Ready for - Pooling of Sample Libraries for Sequencing' AND ExemplarSampleType = 'Pooled Library'", user);
    for(DataRecord sample: pooledSamples){
          double poolConcentration = -1;
          double sampleConcentration = -1;

          try{
             poolConcentration = sample.getDoubleVal("Concentration", user);
          } catch (NullPointerException npe){}
          DataRecord current = null;
          String poolId = "";
          try{
             poolId = sample.getStringVal("SampleId", user); //sample.getStringVal("SampleId", user);
          } catch(Exception e){}


          RunSummary summary = null;
          List<DataRecord> parentSamples = sample.getParentsOfType("Sample", user);
          while(parentSamples != null && parentSamples.size() == 1){
             current = parentSamples.get(0);
             parentSamples = current.getParentsOfType("Sample", user);
          }
          String sampleVol = "";
          String status = "";
          try{
             sampleVol = Double.toString(sample.getDoubleVal("Volume", user));
          } catch(Exception e){}
          status = sample.getStringVal("ExemplarSampleStatus", user);
          for(DataRecord unpooledSample : parentSamples){
             //info added on Riddhika request 3/10/17
             String plannedSequencer = "";
             String plannedWeek = ""; // from batch protocol
             String plateId = ""; // from sample
             String barcodeId = "";
             String barcodeSeq = "";
             String sampleId = "";
             String otherId = "";
             
             //request level info
             String requestId = "";

             Map<String, Object> unpooledFields = unpooledSample.getFields(user);
             requestId = (String)unpooledFields.get("RequestId");
             String tumorOrNormal = "";
      
             try{
                  sampleId = (String)unpooledFields.get("SampleId");
             } catch(Exception e){}
             try{
                plateId = (String)unpooledFields.get("RelatedRecord23");
             } catch(Exception e){}
             String wellPosition = "";
             try{
               wellPosition = (String)unpooledFields.get("ColPosition") + (String)unpooledFields.get("RowPosition");
             } catch(Exception e){}
             String concentrationUnits = "";
             try{
               sampleConcentration = (Double)unpooledFields.get("Concentration");
               concentrationUnits =  (String)unpooledFields.get("ConcentrationUnits");
             } catch(Exception e){}
             LinkedList<DataRecord> fringe = new LinkedList<>();
             fringe.push(unpooledSample); 
             String runLength = ""; //from seq request
             String readDepth = ""; // from seq request
             String tubeBarcode = "";
             tumorOrNormal = (String)unpooledFields.get("TumorOrNormal");
             otherId = (String)unpooledFields.get("OtherSampleId");
             try{
                tubeBarcode = (String)unpooledFields.get("MicronicTubeBarcode");
            } catch(Exception e){}
             while(fringe.size() != 0 && (barcodeId.equals("") || plannedSequencer.equals("") &&  requestId.equals("") || readDepth.equals(""))){
                current  = fringe.pop();
                DataRecord[] barcode = current.getChildrenOfType("IndexBarcode", user);
                if(barcode.length > 0){
                   try{
                     Map<String, Object> barcodeFields = barcode[0].getFields(user);
                     barcodeId = (String)barcodeFields.get("IndexId");
                     barcodeSeq = (String)barcodeFields.get("IndexTag");
                     }  catch(NullPointerException npe){}
                }
                DataRecord[] batchPlanning = current.getChildrenOfType("BatchPlanningProtocol", user);
               if(batchPlanning.length > 0){
                  try{
                     plannedSequencer = batchPlanning[0].getStringVal("RunPlan", user);
                     plannedWeek = batchPlanning[0].getStringVal("WeekPlan", user);
                  } catch(NullPointerException npe){}
                }
               DataRecord[] seqReq = current.getChildrenOfType("SeqRequirement", user);
               if(seqReq.length > 0){
                 try{
                    runLength = seqReq[0].getPickListVal("SequencingRunType", user);
                    readDepth = Double.toString(seqReq[0].getDoubleVal("RequestedReads", user));
                 } catch(NullPointerException npe){}
               }
               if(barcodeId.equals("") || plannedSequencer.equals("") || requestId.equals("") || readDepth.equals("")){
                  List<DataRecord> currentParents = current.getParentsOfType("Sample",  user);
                  for(DataRecord parent : currentParents){
                     fringe.push(parent);
                 }
               }
            }
            summary = new RunSummary("", sampleId);
            summary.setOtherSampleId(otherId);
            summary.setBarcodeId(barcodeId);
            summary.setBarcodeSeq(barcodeSeq);
            summary.setRequestId(requestId);
            summary.setConcentration(poolConcentration);
            summary.setAltConcentration(sampleConcentration);
            summary.setConcentrationUnits(concentrationUnits);
            summary.setVolume(sampleVol);
            summary.setTumor(tumorOrNormal);
            summary.setSequencer(plannedSequencer);
            summary.setBatch(plannedWeek);
            summary.setPlateId(plateId);
            summary.setPool(poolId);
            summary.setWellPos(wellPosition);
            summary.setRunType(runLength);
            summary.setReadNum(readDepth);
            summary.setTubeBarcode(tubeBarcode);
            summary.setStatus(status);
            results.add(summary);
    }
    }
    
    List<DataRecord> samples = dataRecordManager.queryDataRecords("Sample", "ExemplarSampleStatus = 'Ready For - Normalization of Pooled Libraries' or ExemplarSampleStatus = 'Ready For - Illumina Sequencing Planning/Denaturing'", user);
    HashMap<String, List<String>> requestId2Awaiting = new HashMap<String, List<String>>();
    for(DataRecord sample: samples){
          String poolId = "";
          double concentration = -1;
          try{
          poolId = sample.getStringVal("SampleId", user);
          } catch (NullPointerException npe){}
          try{
          concentration = sample.getDoubleVal("Concentration", user);
          } catch (NullPointerException npe){}

          //dfs of all samples to in lane to find sample name, barcode, number of requested reads, and request info
          LinkedList<DataRecord> fringe = new LinkedList<>();
          for(DataRecord sampleParent : sample.getParentsOfType("Sample", user)){
           fringe.push(sampleParent);
          }
          DataRecord current = null;
          String barcodeId = "";
          String barcodeSeq = "";
          String sampleId = "";
          String otherId = "";
           //request level info
          String requestId = "";
          String tumorOrNormal = "";
          String runLength = "";
          String readDepth = "";
          String plannedSequencer = "";
          String plannedWeek = "";
          RunSummary summary = null;
          while(fringe.size() > 0){
             current = fringe.pop();
             DataRecord[] barcode = current.getChildrenOfType("IndexBarcode", user);
             if(barcode.length > 0){
                try{
                  sampleId =  current.getStringVal("SampleId", user);
                  otherId = current.getStringVal("OtherSampleId", user);  
                  barcodeId = barcode[0].getStringVal("IndexId", user);
                  barcodeSeq = barcode[0].getStringVal("IndexTag", user);
                  tumorOrNormal = current.getPickListVal("TumorOrNormal", user);
                } catch(NullPointerException npe){}
             }
             if(requestId.equals("")){
               List<DataRecord> requestParents = current.getParentsOfType("Request", user);
               if(requestParents.size() > 0){
                 try{
                    requestId = requestParents.get(0).getStringVal("RequestId", user);
                    if(!requestId2Awaiting.containsKey(requestId)){
                        addAwaitingSamples(requestParents.get(0), requestId2Awaiting);
                    }
                 } catch(NullPointerException npe){}
               }
             }
             DataRecord[] batchPlanning = current.getChildrenOfType("BatchPlanningProtocol", user);
             if(batchPlanning.length > 0){
                try{
                    plannedSequencer = batchPlanning[0].getStringVal("RunPlan", user);
                    plannedWeek = batchPlanning[0].getStringVal("WeekPlan", user);
                 } catch(NullPointerException npe){}
               }
             DataRecord[] seqReq = current.getChildrenOfType("SeqRequirement", user);
             if(seqReq.length > 0){
               try{
                  runLength = seqReq[0].getPickListVal("SequencingRunType", user);
                  readDepth = Double.toString(seqReq[0].getDoubleVal("RequestedReads", user));
               } catch(NullPointerException npe){}
             } 
             List<DataRecord> parentSamples = current.getParentsOfType("Sample", user);
             if(requestId.equals("")){
                List<DataRecord> parentPlates =  current.getParentsOfType("Plate", user);
                if(parentPlates.size() > 0){
                  List<DataRecord> requestParents = parentPlates.get(0).getParentsOfType("Request", user);
                  if(requestParents.size() > 0){
                    try{
                       requestId = requestParents.get(0).getStringVal("RequestId", user);
                    } catch(NullPointerException npe){} 
                  }
                }
             }
             //if we have found our sample-related info we can just grab the record info and populate a summary
             if((!barcodeSeq.equals("") && !requestId.equals("") && !plannedSequencer.equals("") && !runLength.equals("")) || parentSamples.size() == 0){
                summary = new RunSummary("", sampleId);
                summary.setOtherSampleId(otherId);
                summary.setBarcodeId(barcodeId);
                summary.setBarcodeSeq(barcodeSeq);
                summary.setRequestId(requestId);
                summary.setPool(poolId);
                summary.setConcentration(concentration);
                summary.setTumor(tumorOrNormal);
                summary.setSequencer(plannedSequencer);
                summary.setBatch(plannedWeek);
                summary.setRunType(runLength);
                summary.setReadNum(readDepth);
                summary.setAwaitingSamples(requestId2Awaiting.get(requestId));
                
                results.add(summary);
                sampleId = "";
                barcodeId = "";
                barcodeSeq = "";
                requestId = "";
             }
             else{
               //we can revist records because plates can have multiple sample children
                for(DataRecord parentSample : parentSamples){
                    fringe.push(parentSample);
                }
             }
          }
      }
      
    } 
  catch (Throwable e) {
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   e.printStackTrace(pw);
  log.info(e.getMessage());
   log.info(sw.toString());
 
  //rs = RequestSummary.errorMessage(e.getMessage()); 
  }

  return results; 
 }
  public void addAwaitingSamples(DataRecord req, HashMap<String, List<String>> requestId2Awaiting){
        LinkedList<String> sampleIds = new LinkedList<>();
        LinkedList<DataRecord> reqs = new LinkedList();
        reqs.add(req);
        String request = "";
        try{
            request = req.getStringVal("RequestId", user);
            List<List<Map<String, Object>>> allSampleFields = dataRecordManager.getFieldsForDescendantsOfType(reqs, "Sample", user);
            if(allSampleFields.size() == 0){
                requestId2Awaiting.put(request, sampleIds);
            } else{
                for(Map<String, Object> sampleFields : allSampleFields.get(0)){
                    String status = (String)sampleFields.get("ExemplarSampleStatus");
                    if((status.startsWith("Ready For Processing") || status.equals("Awaiting Processing")  || status.startsWith("Ready for") || status.startsWith("In Processing")  || status.startsWith("In Process ")) && !(status.endsWith("Normalization of Pooled Libraries") ||  status.endsWith("Illumina Sequencing Planning/Denaturing") || status.contains("Illumina"))){
                        sampleIds.add((String) sampleFields.get("SampleId") + ":" + status);
                    }
                }
                requestId2Awaiting.put(request, sampleIds);
            }
        } catch(ServerException | RemoteException|  NotFound e){
            requestId2Awaiting.put(request, sampleIds);
        }
  }
}
