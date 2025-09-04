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

import com.velox.api.report.CustomReport;
import com.velox.api.report.ReportTerm;
import com.velox.sapio.commons.exemplar.recordmodel.record.RecordModel;
import com.velox.sapio.commons.exemplar.recordmodel.relationship.Parents;
import com.velox.sloan.cmo.extensions.WorkQueueDataTypeRetrievalPluginExtension;
import com.velox.sloan.cmo.recmodels.BatchModel;
import com.velox.sloan.cmo.recmodels.IlluminaSeqExperimentModel;

public class WorkQueueRetrieval_IlluminaSequencingExperiment extends WorkQueueDataTypeRetrievalPluginExtension {

	@Override
	protected List<RecordModel> retrieveRecordsToDisplay() throws Exception {
		CustomReport report = reportMan.addAllDataFields(BatchModel.DATA_TYPE_NAME, new CustomReport());
		report.addTerm(BatchModel.DATA_TYPE_NAME, BatchModel.WORKFLOW_NAME, ReportTerm.EQUAL_TO_OPERATOR, "Illumina Sequencing Experiment");
		List<BatchModel> batches = instanceMan.addExistingRecordsOfType(reportMan.runCustomReportForRecords(report, user), BatchModel.class);
		relationMan.loadParents(batches, IlluminaSeqExperimentModel.class);
		
		List<RecordModel> batchesToReturn = new ArrayList<RecordModel>();
		
		for (BatchModel batch : batches) {
			if (batch.get(Parents.ofType(IlluminaSeqExperimentModel.class)).isEmpty()) {
				batchesToReturn.add(batch);
			}
		}
		
		return batchesToReturn;
	}
}
