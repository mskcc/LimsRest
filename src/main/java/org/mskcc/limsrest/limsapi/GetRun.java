package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * A queued task that finds all runs that have undergone picard but are not archived 
 * 
 * @author Aaron Gabow
 * 
 */
@Service
public class GetRun  extends LimsTask 
{
   private Log log = LogFactory.getLog(GetRun.class);

    //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){

    //find all runs
    HashSet<String> archiveable = new HashSet<>();
    try {
        List<DataRecord> unarchivedLanes =  dataRecordManager.queryDataRecords("ArchiveInfo", "isArchived = 0", user);
        List<List<DataRecord>> lanes = dataRecordManager.getParentsOfType(unarchivedLanes, "FlowCellLane",  user);
        List<DataRecord> flattenedLanes = new LinkedList<>();
        for(List<DataRecord> singleLane : lanes){
            for(DataRecord actualLane : singleLane){
                flattenedLanes.add(actualLane);
            }
        }
        List<List<DataRecord>> parentSamples = dataRecordManager.getParentsOfType(flattenedLanes, "Sample", user);
        for(int i = 0; i < unarchivedLanes.size(); i++){
            boolean hasNeededQc = true;
            DataRecord archiveInfo = unarchivedLanes.get(i);
            String runtToCheck = archiveInfo.getStringVal("FinalRunId", user);
            List<DataRecord> parentSamplesOfLane = parentSamples.get(i);
            //need to dfs to confirm that every sample chain has a qc and that it is set to something other than Under Review
            Stack<DataRecord> next = new Stack<>(); 
            for(DataRecord source : parentSamplesOfLane){
                next.push(source);
            }
            while(!next.empty() && hasNeededQc){
                DataRecord sample = next.pop();
                DataRecord[] qcs = sample.getChildrenOfType("SeqAnalysisSampleQC", user);
                for(int j = 0; j < qcs.length; j++){
                    DataRecord qc = qcs[j];
                    if(true){}
                }

            }
        }
        archiveable.add("Truth");
  } catch (Throwable e) {
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   e.printStackTrace(pw);
   log.info(e.getMessage() + " TRACE: " + sw.toString());
    archiveable.add(e.getMessage());
  }

  return archiveable; 
 }

}