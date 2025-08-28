/**
 * Copyright (C) 2005 - 2019 Sapio Sciences <support@sapiosciences.com>
 *
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package org.mskcc.limsrest.util.illuminaseq;

import java.util.List;

import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.ServerPluginDescriptor;
import com.velox.api.plugin.ServerPluginManager;
import com.velox.plugin.workflows.maprectorec.MapFieldValuesFromRecToRecEntryMiddle;
import com.velox.sapioutils.server.plugin.DefaultGenericPlugin;
import com.velox.sapioutils.shared.enums.PluginOrder;
import com.velox.sloan.cmo.utilities.SloanCMOUtils;

/**
 * <p>Tracked Item: CR-14563</p>
 * 
 * <p>The RunMapFieldValuesLastOnEntry plugin will execute the code behind MAP FIELD VALUES FROM [dataTypeName] TO [dataTypeName] 
 * task option key at the LAST + 1 order on task entry. This is useful in cases when values need to be mapped between
 * records, where one or more of these records is being either updated or created after the default execution time
 * of the field value mapping tag.</p>
 *
 * @author Dmitri Petanov
 * Jun 29, 2015
 */

public class RunMapFieldValuesLastOnEntry extends DefaultGenericPlugin {

	public RunMapFieldValuesLastOnEntry() {
		setTaskEntry(true);
		setOrder(PluginOrder.LAST.getOrder() + 1);
    }

	@Override
	protected boolean shouldRun() throws Throwable {
		return new SloanCMOUtils(managerContext).shouldRunPlugin();
	}

	@Override
	protected PluginResult run() throws Throwable {

		logInfo("Beginning execution of the RunMapFieldValuesLastOnEntry.java plugin.");
		
		ServerPluginManager pluginManager = serverPluginContext.getDataMgmtServer().getServerPluginManager(user);
		List<ServerPluginDescriptor> descriptorList = pluginManager.getDescriptorList();
		
		ServerPluginDescriptor fieldMappingDescriptor = null;
		
		for (ServerPluginDescriptor descriptor : descriptorList) {
			if (MapFieldValuesFromRecToRecEntryMiddle.class.getName().equals(descriptor.getClassName())) {
				fieldMappingDescriptor = descriptor;
				break;
			}
		}
		
		if (fieldMappingDescriptor == null) {
			logInfo("Did not find a descriptor associated with the " + 
				MapFieldValuesFromRecToRecEntryMiddle.class.getName() + " plugin. Exiting.");
			return new PluginResult();
		}
		
		logInfo("Found a descriptor associated with the " + MapFieldValuesFromRecToRecEntryMiddle.class.getName() + 
				" plugin. Executing.");
		
		return pluginManager.runPlugin(
				user, activeWorkflow, activeTask, clientCallback.getClientCallbackRMI(), fieldMappingDescriptor);
	}
}