/**
 * Copyright (C) 2005 - 2019 Sapio Sciences <support@sapiosciences.com>
 *
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package org.mskcc.limsrest.util.illuminaseq;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.plugin.PluginResult;
import com.velox.api.workflow.ActiveTask;
import com.velox.sapio.commons.exemplar.recordmodel.main.RecordModelManager;
import com.velox.sapio.commons.exemplar.recordmodel.record.RecordModelInstanceManager;
import com.velox.sapio.commons.exemplar.recordmodel.relationship.Parents;
import com.velox.sapio.commons.exemplar.recordmodel.relationship.RecordModelRelationshipManager;
import com.velox.sapio.commons.exemplar.recordmodel.relationship.Relationships;
import com.velox.sapioutils.server.plugin.DefaultGenericPlugin;
import com.velox.sapioutils.shared.enums.PluginOrder;
import com.velox.sapioutils.shared.managers.TaskUtilManager;
import com.velox.sloan.cmo.recmodels.BatchModel;
import com.velox.sloan.cmo.recmodels.SequencingRunSetupPlanExpModel;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;

/**
 * This plugin is used to attach the SequencingRunSetupPlanExp record that is associated with the 
 * Batch of the Sample that is being processed in this workflow, to this workflow.
 * 
 * @author Jude.J
 * 21-Nov-2017
 */
// [FR-26939] - <Create a plugin to retrieve the Experiment record associated with the Batch that the sample belongs to>
public class AttachExperimentOnEntry extends DefaultGenericPlugin {

	// The default constructor
	public AttachExperimentOnEntry() {
		
		this.setTaskEntry(true);
		this.setOrder(PluginOrder.EARLY.getOrder());
	}
	
	// The task must be run only once
	@Override
	protected boolean shouldRun() throws Throwable {

		return (new SloanCMOUtils(managerContext).shouldRunPlugin())  && (activeTask.getStatus() != ActiveTask.COMPLETE);
	}
	
	/*
	 * This method will work by first checking if a SequencingRunSetupPlanExp is already attached to the task. If yes, it will just let the user 
	 * into the task. In case a SequencingRunSetupPlanExp is not present, the batch associated with the samples in the workflow is obtained,
	 * from which the SequencingRunSetupPlanExp record is obtained. This is then attached to the task.
	 * 
	 * (non-Javadoc)
	 * @see com.velox.sapioutils.server.plugin.DefaultGenericPlugin#run()
	 */
	@Override
	protected PluginResult run() throws Throwable {
	
		// Get the SequencingRunSetupPlanExp attached to the task
		List<DataRecord> sequencingRunSetupPlanExpsInTask = activeTask.getAttachedDataRecords(SequencingRunSetupPlanExpModel.DATA_TYPE_NAME, user);
		
		if (CollectionUtils.isNotEmpty(sequencingRunSetupPlanExpsInTask)) {
			
			logInfo("A record of type SequencingRunSetupPlanExp is already attached to this task");
			return new PluginResult(true);
		}
		
		// Initialize the managers
		RecordModelManager recMan = getManager(RecordModelManager.class);
		RecordModelInstanceManager instMan = recMan.getInstanceManager();
		RecordModelRelationshipManager relationshipManager = recMan.getRelationshipManager();
		
		// Get the batch from the first task 
		List<BatchModel> batches = instMan.addExistingRecordsOfType(TaskUtilManager.getFirstActiveTask(activeWorkflow).
				getAttachedDataRecords(BatchModel.DATA_TYPE_NAME, user), BatchModel.class);
		
		if (CollectionUtils.isEmpty(batches) || batches.size() > 1) {
			
			return new PluginResult(true);
		}
		
		// Load and get the parent of type SequencingRunSetupPlanExp for the batch
		relationshipManager.loadParents(batches, SequencingRunSetupPlanExpModel.class);
		Relationships<SequencingRunSetupPlanExpModel> sequencingRunSetupPlanExps = batches.iterator().next().get(Parents.ofType(SequencingRunSetupPlanExpModel.class));
		
		if (CollectionUtils.isEmpty(sequencingRunSetupPlanExps) || sequencingRunSetupPlanExps.size() > 1) {
			
			return new PluginResult(true);
		}
		
		// Attach the SequencingRunSetupPlanExp record to the task
		TaskUtilManager.attachRecordToTask(activeTask, sequencingRunSetupPlanExps.iterator().next().getDataRecord());
		
		return new PluginResult(true);
	}
}
