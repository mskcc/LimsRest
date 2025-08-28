/**
 * Copyright (C) 2005 - 2019 Sapio Sciences <support@sapiosciences.com>
 *
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package org.mskcc.limsrest.util.illuminaseq;

import com.velox.api.plugin.PluginPoint;
import com.velox.api.plugin.PluginResult;
import com.velox.api.workflow.ActiveTask;
import com.velox.sapioutils.server.plugin.DefaultGenericPlugin;
import com.velox.sapioutils.shared.enums.PluginOrder;
import com.velox.sapioutils.shared.managers.TaskUtilManager;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;

/**
 *  <p><b>Tracked Item:</b> CR-14837 </p>
 *
 * The CompleteTaskOnEntry plugin will auto complete the first task of an workflow only when the status of the task is not 'Complete'
 * Task completion will only be happening at the very end of all plugin execution
 *
 * @author Zifo
 * August 11, 2015
 */

public class CompleteTaskOnEntry extends DefaultGenericPlugin {

	public CompleteTaskOnEntry() {
		setTaskEntry(true);
		setOrder(PluginOrder.LAST.getOrder() + 5);
	}

	@Override
	protected boolean shouldRun() throws Throwable {
	    return new SloanCMOUtils(managerContext).shouldRunPlugin()  && activeTask.getStatus() != ActiveTask.COMPLETE;
	}

	@Override
	protected PluginResult run() throws Throwable {
		logInfo("Running Plugin: " + "CompleteTaskOnEntry");
		
		PluginResult executeTaskPlugins = SloanCMOUtils.executeTaskPlugins(serverPluginContext,activeWorkflow,activeTask,PluginPoint.TASKSUBMIT);
		if(executeTaskPlugins.isPassed()){
		    activeTask.setStatus(ActiveTask.COMPLETE);
		    new TaskUtilManager(managerContext).removeTaskFromDependencyLists(activeTask);
		    activeTask.commitChanges(user);
		}
		
		return executeTaskPlugins;
	}
}