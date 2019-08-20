package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A queued task that takes a request id and returns the hiseq run info 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetHiseq  extends LimsTask {
   private static Log log = LogFactory.getLog(GetHiseq.class);
  protected String run; 
  protected String[] projects;

  public void init(String run){
    this.run = run;
  }
  
  public void init(String[] projects){
     if(projects != null)
        this.projects = projects.clone();
    
  }


@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){

  HashSet<String> projectsSearchable = new HashSet<>();
// RUN Name, Project#, sample, Barcode, Lane #, Lab Head, Investigator, Date (start run), Date (end run), Run type, read length, SampleRef, # reads request_sample, Status of project 
// 151028_MOMO_0100_BC7MG0ANXX 6255 shLUC-DMSO_1 TS2 1,2 S Armstrong Chun-Wei Chen 10.28.2015 RNASeq 51/7/51 Mouse 40-50M Done 
  List<RunSummary> runResults = new LinkedList<>();

  try { 
    if(run == null  && projects == null){
      throw new Exception("Unable to get hiseq run information with no hisq run or projects specified");
    }
     List<DataRecord> runList = null;
    if(projects != null && projects.length > 0){
       runList = new LinkedList<DataRecord>();
       for(int i = 0; i < projects.length; i++){
          List<DataRecord> queriedRequests =  dataRecordManager.queryDataRecords("Request", "RequestId = '" + projects[i] + "'", user);
          projectsSearchable.add(projects[i]);
          for(DataRecord req : queriedRequests){ 
            log.info("Getting a record " + projects[i]);
            List<DataRecord> reqLanes = req.getDescendantsOfType("FlowCellLane", user);
            for(DataRecord lane : reqLanes){
               log.info("Getting a flow cell lane");
               List<DataRecord> flowcell = lane.getParentsOfType("FlowCell", user);
               if(flowcell.size() > 0){
                 log.info("Getting a flow cell");
                 List<DataRecord> possibleRun = flowcell.get(0).getParentsOfType("IlluminaSeqExperiment", user);
                 if(possibleRun.size() > 0){
                    log.info("Getting a run");
                    if(!runList.contains(possibleRun.get(0))){
                       runList.add(possibleRun.get(0));
                    }
                 }
               }
            }
          }
        }
    }
    else if(run.equals("")){
       runList = dataRecordManager.queryDataRecords("IlluminaSeqExperiment", null, user);
    }
    else{
       runList = dataRecordManager.queryDataRecords("IlluminaSeqExperiment", "SequencerRunFolder like '%" + run  +"%'", user);
    }
    for(DataRecord r: runList){
      String runType = r.getPickListVal("SequencingRunType", user);
      if(projects != null){
         try{ 
           run = r.getStringVal("SequencerRunFolder", user);
         }  catch(NullPointerException npe){
             run = "MISSING";
         }

      }
      if(r.getChildrenOfType("FlowCell", user).length == 0){
          continue;
      }
      DataRecord flowcell = r.getChildrenOfType("FlowCell", user)[0];
      long dateCreated = flowcell.getDateVal("DateCreated", user);
      if(run.equals("")){
         String runId = "ALLRUNS";
         try{
           String[] runFolderElements = r.getStringVal("SequencerRunFolder", user).split("/");
           runId = runFolderElements[runFolderElements.length -1];
         }  catch(NullPointerException npe){ }
          RunSummary runSum = new RunSummary(runId, "");
          runSum.setStartDate(dateCreated);
          runResults.add(runSum);
       } else{
        HashMap<String, RunSummary> sampleId2Summary = new HashMap<>();
        DataRecord[] lanes = flowcell.getChildrenOfType("FlowCellLane", user);
        for(int i = 0; i < lanes.length; i++){
          long laneNum = lanes[i].getLongVal("LaneNum", user);

          //dfs of all samples to in lane to find sample name, barcode, number of requested reads, and request info
          LinkedList<DataRecord> fringe = new LinkedList<>();
          for(DataRecord sample : lanes[i].getParentsOfType("Sample", user)){
           fringe.push(sample);
          }
          DataRecord current = null;
          String barcodeId = "";
          String barcodeSeq = "";
          String recipe = "";
          String sampleId = "";
          String species = "";
          //request level info
          String requestId = "";
          String labHead = "";
          String investigator = "";
          Boolean fastqOnly = null;
          Short numberSamples = null;
          
          RunSummary summary = null;
          double numberRequestedReads = 0.0;
          while(fringe.size() > 0){
             current = fringe.pop();
             Map<String, Object> currentFields = current.getFields(user);
             DataRecord[] barcode = current.getChildrenOfType("IndexBarcode", user);
             if(barcode.length > 0){
                try{
                  sampleId = currentFields.get("OtherSampleId") + "_IGO_" +  currentFields.get("SampleId");
                  species = (String)currentFields.get("Species");
                  barcodeId = barcode[0].getStringVal("IndexId", user);
                  barcodeSeq = barcode[0].getStringVal("IndexTag", user);
                } catch(NullPointerException npe){}
             }
             if(requestId.equals("")){
               recipe = (String)currentFields.get("Recipe");
               List<DataRecord> requestParents = current.getParentsOfType("Request", user);
               if(requestParents.size() > 0){
                 try{
                    Map<String, Object> requestFields = requestParents.get(0).getFields(user);
                    requestId = (String)requestFields.get("RequestId");
                    labHead = (String)requestFields.get("LaboratoryHead");
                    investigator = (String)requestFields.get("Investigator");
                    if( requestFields.containsKey("BICAnalysis") && requestFields.get("BICAnalysis") != null && (Boolean)requestFields.get("BICAnalysis")){
                        fastqOnly = Boolean.FALSE;
                    } else {
                       fastqOnly = Boolean.TRUE;
                    }
                    if(requestFields.containsKey("SampleNumber")){
                       numberSamples = (Short)requestFields.get("SampleNumber");
                    }
                 } catch(NullPointerException npe){}

                 if(projectsSearchable.size() > 0 && !projectsSearchable.contains(requestId)){
                    sampleId = "";
                    barcodeId = "";
                    barcodeSeq = "";
                    recipe = "";
                    species = "";
                    requestId = "";
                    labHead ="";
                    investigator = "";
                    numberRequestedReads = 0.0;   
                    fastqOnly = null;
                    numberSamples = null;
                    continue;
                 }
               }
             }
             DataRecord[] seqRequirement = current.getChildrenOfType("SeqRequirement", user);
             if(seqRequirement.length > 0){
                 try{
                      numberRequestedReads = seqRequirement[0].getDoubleVal("RequestedReads", user);
                 } 
                 catch(NullPointerException npe){}
                 catch(ClassCastException e){
                    numberRequestedReads = (double)seqRequirement[0].getIntegerVal("RequestedReads", user);

                  }
             }
             List<DataRecord> parentSamples = current.getParentsOfType("Sample", user);
             if(requestId.equals("")){
                List<DataRecord> parentPlates =  current.getParentsOfType("Plate", user);
                if(parentPlates.size() > 0){
                  List<DataRecord> requestParents = parentPlates.get(0).getParentsOfType("Request", user);
                  if(requestParents.size() > 0){
                    try{
                       Map<String, Object> requestFields = requestParents.get(0).getFields(user);
                       requestId = (String)requestFields.get("RequestId");
                        labHead = (String)requestFields.get("LaboratoryHead");
                        investigator = (String)requestFields.get("Investigator"); 
                       if( requestFields.containsKey("BICAnalysis") && requestFields.get("BICAnalysis") != null && (Boolean)requestFields.get("BICAnalysis")){
                          fastqOnly = Boolean.FALSE;
                       } else {
                          fastqOnly = Boolean.TRUE;
                        }
                       if(requestFields.containsKey("SampleNumber")){
                         numberSamples = (Short)requestFields.get("SampleNumber");
                       } 
                    } catch(NullPointerException npe){} 
                    if(projectsSearchable.size() > 0 && !projectsSearchable.contains(requestId)){
                       sampleId = "";
                       barcodeId = "";
                       barcodeSeq = "";
                       recipe = "";
                       species = "";
                       requestId = "";
                       labHead ="";
                       investigator = "";
                       numberRequestedReads = 0.0;
                       fastqOnly = null;
                       numberSamples = null;
                       continue;
                    }
                  }
                }
             }
             //if we have found our sample-related info we can just grab the record info and populate a summary
             if((!barcodeSeq.equals("") && !requestId.equals("") && numberRequestedReads > 0.00001) || parentSamples.size() == 0){
               if(!sampleId2Summary.containsKey(sampleId + "_" + run)){
                   sampleId2Summary.put(sampleId + "_" + run, new RunSummary(run, sampleId));
                }
                summary = sampleId2Summary.get(sampleId + "_" + run);
                summary.setBarcodeId(barcodeId);
                summary.setBarcodeSeq(barcodeSeq);
                summary.setNumberRequestedReads((int)numberRequestedReads);
                summary.setSpecies(species);
                summary.setRecipe(recipe);
                summary.addLane(laneNum);
                summary.setStartDate(dateCreated);
                summary.setRequestId(requestId);
                summary.setLabHead(labHead);
                summary.setInvestigator(investigator);
                summary.setFastqOnly(fastqOnly);
                summary.setNumberRequestSamples(numberSamples);
                summary.setRunType(runType);
                sampleId = "";
                barcodeId = "";
                barcodeSeq = "";
                recipe = "";
                species = "";
                requestId = "";
                labHead ="";
                investigator = "";
                fastqOnly = null;
                numberRequestedReads = 0.0;
             }
             else{
               //we can revist records because plates can have multiple sample children
                for(DataRecord parentSample : parentSamples){
                    fringe.push(parentSample);
                }
             }
          }
        }
      
        runResults.addAll(sampleId2Summary.values());
      }
   }
  } catch (Throwable e) {
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   e.printStackTrace(pw);
   log.info(e.getMessage() + " TRACE: " + sw.toString());
   runResults.add(RunSummary.errorMessage(e.getMessage(), sw.toString()));
 
  //rs = RequestSummary.errorMessage(e.getMessage()); 
  }

  return runResults; 
 }
}