package org.mskcc.limsrest.limsapi;

import com.velox.api.datarecord.DataRecord;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;


/**
 * A queued task that takes a run name and returns a combination of a sample and request info
 * 
 * @author Aaron Gabow
 * 
 */
public class GetProjectInfo  extends LimsTask 
{

  private Log log = LogFactory.getLog(GetProjectInfo.class);

  protected String project;


  public void init(String project){
    this.project = project;
  }

 //execute the velox call
 @Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
  RequestSummary rs = new RequestSummary(project);

  try { 
    List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId = '" + project  +"'", user);
    for(DataRecord r: requestList){
      annotateRequestSummary(rs, r);
      List<DataRecord> qcs = r.getDescendantsOfType("SeqAnalysisSampleQC", user);
         for(DataRecord qc : qcs){
            SampleSummary ss = new SampleSummary();
            SampleQcSummary qcSummary = new SampleQcSummary();
            DataRecord parentSample = qc.getParentsOfType("Sample", user).get(0); //will only have one parent sample
            annotateQcSummary(qcSummary, qc); 
            if(parentSample != null){
              annotateSampleSummary(ss, parentSample);
              //try and get the requested number of reads
              DataRecord[] requirements = parentSample.getChildrenOfType("SeqRequirement", user);
              DataRecord ancestorSample = parentSample;
              while(requirements.length == 0 && ancestorSample != null){
                List<DataRecord> searchParents = ancestorSample.getParentsOfType("Sample", user);
                if(searchParents.size() > 0){
                    ancestorSample = searchParents.get(0);
                    requirements = ancestorSample.getChildrenOfType("SeqRequirement", user);
                    if(requirements.length > 0){
                        ss.setRequestedReadNumber((long)requirements[0].getDoubleVal("RequestedReads", user));
                    }
                } else{
                    ancestorSample = null;
                }
              }
            }
            ss.setQc(qcSummary);
            rs.addSample(ss);

        }
    }
  } catch (Throwable e) {
   log.info(e.getMessage());
   rs = RequestSummary.errorMessage(e.getMessage()); 
  }

  return rs;
 }

}
