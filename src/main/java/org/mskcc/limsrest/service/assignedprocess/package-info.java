package org.mskcc.limsrest.service.assignedprocess;

/**
 Assigned Process records are part of Exemplar’s Process Tracking feature.
 <BR>
 - When a Sample is received, it can be assigned to one or more processes. A process is a series of one or more workflows.
   For each process that a sample is assigned to when it is received, an Assigned Process record is created and assigned as a parent of that sample to represent that sample’s involvement in the process.
   As the sample moves through workflows, the Assigned Process status will be updated to reflect its progression through its respective process. The status value determines where it will display in the Work Queue.
 - Assigned Process records typically have a one-to-one relationship with samples – a single Assigned Process record should not have multiple Sample children.
 - The creation and assignment of Assigned Process records should be handled for you through out-of-the-box plugins and tag configurations.
 */