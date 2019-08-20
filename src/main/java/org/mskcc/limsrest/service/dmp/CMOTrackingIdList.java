package org.mskcc.limsrest.service.dmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CMOTrackingIdList {
    private String runDate;
    private String result;
    private Content content;

    public CMOTrackingIdList() {
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public List<String> getTrackingIds() {
        return content.getTrackingIds();
    }

    @Override
    public String toString() {
        return "CmoTrackingIdList{" +
                "runDate='" + runDate + '\'' +
                ", result='" + result + '\'' +
                ", content=" + content +
                '}';
    }

    private class Content {
        @JsonProperty("TrackingId List")
        private List<String> trackingIds;

        public Content() {
        }

        public List<String> getTrackingIds() {
            return trackingIds;
        }

        public void setTrackingIds(List<String> trackingIds) {
            this.trackingIds = trackingIds;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "trackingIds=" + trackingIds +
                    '}';
        }
    }
}
