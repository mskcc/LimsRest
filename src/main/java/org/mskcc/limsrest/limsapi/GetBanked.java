
package org.mskcc.limsrest.limsapi;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
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

import org.mskcc.limsrest.staticstrings.Messages;

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


/**
 * A queued task that takes a request id or a banked sample names and returns 
 * multiple banked samples 
 *
 * @author Aaron Gabow
 * 
 */
@Service
public class GetBanked  extends LimsTask 
{
  
  private Log log = LogFactory.getLog(GetBanked.class);

  protected String project;
  protected String[] sampleNames;
  protected String investigator;
  protected String serviceId;

  public void init(String project){
    this.project = project;
  }

  public void init(String[] names){
    if(names != null)
        this.sampleNames = names.clone();
  }

  public void initInvestigator(String name){
    this.investigator = name;
  }

  public void initServiceId(String id){
     this.serviceId = id;
  }

  

 //execute the velox call
@PreAuthorize("hasRole('READ')")
@Override
 public Object execute(VeloxConnection conn){
// private void runProgram(User apiUser, DataRecordManager dataRecordManager) {
 
  LinkedList<SampleSummary> sampleInfo = new LinkedList();  
  List<DataRecord> banked;
  
  

  try { 
    if(project == null && sampleNames == null && serviceId == null && investigator == null){
      throw new LimsException("Unable to get banked reads without specifying a request, a sample name, an investigator or a service id");
    }
    boolean isFirstCondition = true;
    StringBuffer sqlWhere = new StringBuffer();
    if(project != null){
       sqlWhere.append("RequestId = '");
       sqlWhere.append(project);
       sqlWhere.append("'");
       isFirstCondition = false;
    }
    if(sampleNames != null){
       if(!isFirstCondition){
          sqlWhere.append(" AND ");
       }
       sqlWhere.append("OtherSampleId in (");
       for(int i = 0; i < sampleNames.length - 1; i++){
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
    if(serviceId != null){
       if(!isFirstCondition){
          sqlWhere.append(" AND ");
       }
       sqlWhere.append("ServiceId = '");
       sqlWhere.append(serviceId);
       sqlWhere.append("'"); 
       isFirstCondition = false;
    }
    if(investigator != null){
       if(!isFirstCondition){
          sqlWhere.append(" AND ");
       }
       sqlWhere.append("Investigator = '");
       sqlWhere.append(investigator);
       sqlWhere.append("'");
    }
    banked = dataRecordManager.queryDataRecords("BankedSample",  sqlWhere.toString(), user);
    for(DataRecord sample: banked){
        SampleSummary ss = new SampleSummary();
        annotateSampleDetailed(ss, sample);
        try{ss.setInvestigator(sample.getStringVal("Investigator", user));} catch(NullPointerException npe){}
        try{ss.setPlateId(sample.getStringVal("PlateId", user));} catch(NullPointerException npe){}
        try{
           ss.setRowPosition(sample.getStringVal("RowPosition", user));
           ss.setColPosition(sample.getStringVal("ColPosition", user));
        } catch(NullPointerException npe){}
        try{ ss.setBarcodeId(sample.getStringVal("BarcodeId", user)); } catch(NullPointerException npe) {}
        try{ss.setRecipe(sample.getStringVal("Recipe", user)); } catch(NullPointerException npe) {}
        try{ss.setTumorType(sample.getStringVal("TumorType", user)); } catch(NullPointerException npe) {}
        try{ss.setSpecimenType(sample.getStringVal("SpecimenType", user)); } catch(NullPointerException npe) {}
        try{
            ss.setRequestedReadNumber(Long.parseLong(sample.getStringVal("RequestedReads", user))); 
        } catch(NullPointerException npe) {}
          catch(NumberFormatException nfe){
            log.info("Warning: " + sample.getStringVal("RequestedReads", user) + " incorrect format.");
            ss.setRequestedReadNumber(-1);
          }
        try{ss.setCellCount(sample.getStringVal("CellCount", user)); } catch(NullPointerException npe) {}
        try{ss.setMicronicTubeBarcode(sample.getStringVal("MicronicTubeBarcode", user)); } catch(NullPointerException npe) {}
        try{ss.setNaToExtract(sample.getPickListVal("NAtoExtract", user)); } catch(NullPointerException npe) {}
        try{ss.setNumTubes(sample.getStringVal("NumTubes", user)); } catch(NullPointerException npe) {}
        try{ss.setRunType(sample.getStringVal("RunType", user)); } catch(NullPointerException npe) {}
        try{ss.setSampleClass(sample.getStringVal("SampleClass", user)); } catch(NullPointerException npe) {}
        try{ss.setSampleType(sample.getStringVal("SampleType", user)); } catch(NullPointerException npe) {}
        if("ERROR".equals(ss.getBaseId())){
            ss.addBaseId("");
        }
        sampleInfo.add(ss);

    }
  } catch (Throwable e) {
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   e.printStackTrace(pw);
   log.info(e.getMessage() + " TRACE: " + sw.toString());
   SampleSummary ss = new SampleSummary();
   ss.addCmoId(Messages.ERROR_IN + " GetBanked: " + e.getMessage()); 
   sampleInfo.addFirst(ss);
   return sampleInfo;
  }

  return sampleInfo;
 }

}
