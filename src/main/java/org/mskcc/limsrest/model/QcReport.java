package org.mskcc.limsrest.model;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class QcReport implements Serializable {
    public enum QcReportType {
        DNA, RNA, LIBRARY;
    }

    private QcReportType qcReportType;
    private String igoRecommendation;
    private String comments;
    private String investigatorDecision;

    public QcReport(){}

    /**
     * QcReport constructor.
     * @param qcReportType
     * @param igoRecommendation
     * @param comments
     * @param investigatorDecision
     */
    public QcReport(QcReportType qcReportType, String igoRecommendation,
            String comments, String investigatorDecision) {
        this.qcReportType = qcReportType;
        this.igoRecommendation = igoRecommendation;
        this.comments = comments;
        this.investigatorDecision = investigatorDecision;
    }

    public QcReportType getQcReportType() {
        return qcReportType;
    }

    public void setQcReportType(QcReportType qcReportType) {
        this.qcReportType = qcReportType;
    }

    public String getIgoRecommendation() {
        return igoRecommendation;
    }

    public void setIgoRecommendation(String igoRecommendation) {
        this.igoRecommendation = igoRecommendation;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getInvestigatorDecision() {
        return investigatorDecision;
    }

    public void setInvestigatorDecision(String investigatorDecision) {
        this.investigatorDecision = investigatorDecision;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
