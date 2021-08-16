package org.mskcc.limsrest.service.dmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
@Getter @Setter @ToString @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CMOTrackingIdList {
    private String runDate;
    private String result;
    private Content content;

    public List<String> getTrackingIds() {
        return content.getTrackingIds();
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
