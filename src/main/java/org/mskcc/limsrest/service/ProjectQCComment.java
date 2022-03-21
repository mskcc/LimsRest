package org.mskcc.limsrest.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Setter @Getter @AllArgsConstructor
public class ProjectQCComment {

    String requestId;
    String comment;
    Date dateCreated;
    String createdBy;

    public Map<String, Object> getQcComments() {
        Map<String, Object> qcComments = new HashMap<>();
        qcComments.put("RequestId", this.requestId);
        qcComments.put("Comment", this.comment);
        qcComments.put("DateCreated", this.dateCreated);
        qcComments.put("CreatedBy", this.createdBy);

        return qcComments;
    }

}
