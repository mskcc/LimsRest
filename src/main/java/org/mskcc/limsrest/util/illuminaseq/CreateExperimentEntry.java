/**
 * Copyright (C) 2005 - 2019 Sapio Sciences <support@sapiosciences.com>
 *
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package org.mskcc.limsrest.util.illuminaseq;

import java.util.ArrayList;
import java.util.List;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.plugin.PluginResult;
import com.velox.sapioutils.server.plugin.DefaultGenericPlugin;
import com.velox.sapioutils.shared.enums.PluginOrder;
import com.velox.sapioutils.shared.managers.DataRecordUtilManager;
import com.velox.sapioutils.shared.utilities.ListUtils;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Batch;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_SampleReceipt;
import com.velox.sloan.cmo.staticstrings.workflows.WF_IlluminaSequencing;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;

/**
 * <p><b>Tracked Item:</b> FR-9424 </p>
 *
 * The CreateExperimentEntry plugin will auto populate a Sequence Run Type field on the
 * Illumina Sequencing Experiment data record that is associated with the current workflow.
 * 
 * The Sequence Run Type will only get filled in if all samples selected in the Work Queue
 * have a matching Sequence Run Type, that was selected in the Sample Receiving workflow.
 *
 * @author Dmitri Petanov
 * May 28, 2014
 */

public class CreateExperimentEntry extends DefaultGenericPlugin {

	public CreateExperimentEntry() {
		setTaskEntry(true);
		setOrder(PluginOrder.MIDDLE.getOrder());
    }

	@Override
	protected boolean shouldRun() throws Throwable {
		return isCorrectTask(
					WF_IlluminaSequencing.WORKFLOW_NAME, 
					WF_IlluminaSequencing.TASK_CREATE_EXPERIMENT) ||
				new SloanCMOUtils(managerContext).shouldRunPlugin();
	}

	@Override
	protected PluginResult run() throws Throwable {
		logInfo("Running Plugin: " + "CreateExperimentEntry");
		
		// A list of all sample records entering this workflow. These may be Sample or PlateWell records.
		// This list is then used to extract the Sequencing run type to set on the experiment.
		List<DataRecord> sampleRecordList = new ArrayList<DataRecord>();
		
		List<DataRecord> batchRecordList = activeTask.getAttachedDataRecords(DT_Batch.DATA_TYPE, user);
		
		if (!batchRecordList.isEmpty()) {
			sampleRecordList.addAll(ListUtils.flattenListRemoveDuplicates(
					dataRecordManager.getChildrenOfType(batchRecordList, DT_Sample.DATA_TYPE, user)));
			
			// [CR-13797] Update project specific code to a LIMS version 7.0.0 API and to remove references to PlateWell data types.
			/*sampleRecordList.addAll(ListUtils.flattenListRemoveDuplicates(
					dataRecordManager.getChildrenOfType(batchRecordList, DT_PlateWell.DATA_TYPE, user)));*/
			
			ListUtils.removeNullValues(sampleRecordList);
		}
		
		if (sampleRecordList.isEmpty())
			new SloanCMOUtils(managerContext).getAllAttachedSamples(activeTask);
		
		// This may be a first call to plugins by the work queue launcher. At that point, neither batches
		// nor samples are attached to the task yet.
		if (sampleRecordList.isEmpty())
			return new PluginResult(true);
		
		// Get all of the ancestors from the located sample records. The top most ancestors will contain 
		// sample receipts which in turn specify the sequence run type. 
		List<DataRecord> ancestorSampleRecordList = ListUtils.removeNullValues(ListUtils.flattenListRemoveDuplicates(
				dataRecordManager.getAncestorsOfType(sampleRecordList, DT_Sample.DATA_TYPE, user)));
		
		if (ancestorSampleRecordList.isEmpty())
			return new PluginResult();
		
		// Get the sample receipts
		List<DataRecord> sampleReceiptRecordList = ListUtils.removeNullValues(ListUtils.flattenListRemoveDuplicates(
				dataRecordManager.getChildrenOfType(ancestorSampleRecordList, DT_SampleReceipt.DATA_TYPE, user)));
		
		if (sampleReceiptRecordList.isEmpty())
			return new PluginResult();
		
		List<Object> sequenceRunTypes = ListUtils.removeDuplicates(
				dataRecordManager.getValueList(sampleReceiptRecordList, DT_SampleReceipt.SEQUENCING_RUN_TYPE, user));
		
		// If the sequence run type is not specified or there are more than just one value, then do not set
		// a value on the Illumina Sequencing experiment
		if (sequenceRunTypes.isEmpty() || sequenceRunTypes.size() > 1)
			return new PluginResult();
		
		// Set the value of a sequencing run type on this experiment record
		new DataRecordUtilManager(managerContext).getExperiment().setDataField(DT_SampleReceipt.SEQUENCING_RUN_TYPE, sequenceRunTypes.get(0), user);

		return new PluginResult(true);
	}
}