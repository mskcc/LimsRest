package org.mskcc.limsrest.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeSet;

/**
 * A queued task that takes a request id and returns all the start and end date for task based on the creation and last modified date of every sample 
 * 
 * @author Aaron Gabow
 */
@Service
public class GetProjectHistory extends LimsTask {
    private static Log log = LogFactory.getLog(GetProjectHistory.class);

    protected String[] projects;

    public void init(String[] projects) {
        if (projects != null)
            this.projects = projects.clone();
    }

    @PreAuthorize("hasRole('READ')")
    @Override
    public Object execute(VeloxConnection conn) {
        TreeSet<HistoricalEvent> known = new TreeSet<>();

        log.info("Accessing lims");

        try {
            if (projects == null) {
                throw new Exception("Unable to get project information with no project specified");
            }
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < projects.length - 1; i++) {
                sb.append("'");
                sb.append(projects[i]);
                sb.append("',");
            }
            sb.append("'");
            sb.append(projects[projects.length - 1]);
            sb.append("'");
            List<DataRecord> requestList = dataRecordManager.queryDataRecords("Request", "RequestId in (" + sb.toString() + ")", user);

            for (DataRecord r : requestList) {
                long requestStart = r.getLongVal("DateCreated", user);
                long requestComplete = 0;
                try {
                    requestComplete = r.getLongVal("CompletedDate", user);
                } catch (NullPointerException npe) {
                }
                String project = "";
                try {
                    project = r.getStringVal("RequestId", user);
                } catch (NullPointerException npe) {
                }

                known.add(new HistoricalEvent(requestStart, requestComplete, project, "Initiated"));

                List<DataRecord> samples = r.getDescendantsOfType("Sample", user);
                List<DataRecord> plates = r.getDescendantsOfType("Plate", user);
                samples.addAll(plates);
                for (DataRecord sample : samples) {
                    log.info("processing");
                    long start = sample.getLongVal("DateCreated", user);
                    long end = 0;
                    try {
                        end = sample.getLongVal("DateModified", user);
                    } catch (NotFound nf) {
                        end = start; //plates do not have date modified
                    } catch (NullPointerException npe) {
                        log.info("Null end for sample");
                    }
                    String status = null;
                    try {
                        status = sample.getStringVal("ExemplarSampleStatus", user);
                    } catch (NotFound nf) {
                        try {
                            status = sample.getStringVal("ExemplarPlateStatus", user);
                        } catch (Exception e) {
                        }
                    } catch (NullPointerException npe) {
                    }
                    if (status == null) {
                        log.info("Skipping a null status sample");
                        continue;
                    }
                    String requestId = project;
                    try {
                        requestId = sample.getStringVal("RequestId", user);
                    } catch (NullPointerException | NotFound e) {
                    }
                    String[] statusElements = status.split(" - ");
                    if (statusElements[0].equals("Received")) {
                        end = start;
                    } else if (!statusElements[0].equals("Completed")) {
                        log.info("Status not completed:" + status);
                        end = 0;
                    }
                    log.info("End: " + end);
                    if (statusElements.length > 1) {
                        HistoricalEvent event = new HistoricalEvent(start, end, requestId, statusElements[1]);
                        known.add(event);
                    } else {
                        HistoricalEvent event = new HistoricalEvent(start, end, requestId, status);
                        known.add(event);
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

        return known;
    }

    public static class HistoricalEvent extends RestDescriptor implements Comparable {
        public GregorianCalendar start;
        public GregorianCalendar end;
        String taskName;
        String requestId;

        public HistoricalEvent(long startInMS, long endInMS, String requestId, String taskName) {
            this.start = new GregorianCalendar();
            this.end = new GregorianCalendar();
            start.setTimeInMillis(startInMS);
            end.setTimeInMillis(endInMS);
            this.requestId = requestId;
            this.taskName = taskName;

        }

        public int compCal(Calendar c1, Calendar c2) {
            if (c1 == c2) return 0;
            if (c1.get(Calendar.YEAR) > c2.get(Calendar.YEAR)) {
                return 1;
            } else if (c1.get(Calendar.YEAR) < c2.get(Calendar.YEAR)) {
                return -1;
            } else {
                if (c1.get(Calendar.MONTH) > c2.get(Calendar.MONTH)) {
                    return 1;
                } else if (c1.get(Calendar.MONTH) < c2.get(Calendar.MONTH)) {
                    return -1;
                } else {
                    if (c1.get(Calendar.DAY_OF_MONTH) > c2.get(Calendar.DAY_OF_MONTH)) {
                        return 1;
                    } else if (c1.get(Calendar.DAY_OF_MONTH) < c2.get(Calendar.DAY_OF_MONTH)) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }

        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getRequestId() {
            return requestId;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getStartDate() {
            StringBuffer sb = new StringBuffer();
            sb.append(start.get(Calendar.YEAR));
            sb.append("-");
            sb.append(start.get(Calendar.MONTH) + 1);
            sb.append("-");
            sb.append(start.get(Calendar.DAY_OF_MONTH));
            return sb.toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getEndDate() {
            if (end.get(Calendar.YEAR) < 1980) {
                return "";
            }
            StringBuffer sb = new StringBuffer();
            sb.append(end.get(Calendar.YEAR));
            sb.append("-");
            sb.append(end.get(Calendar.MONTH) + 1);
            sb.append("-");
            sb.append(end.get(Calendar.DAY_OF_MONTH));
            return sb.toString();
        }

        @JsonIgnore
        public GregorianCalendar getStart() {
            return start;
        }

        @JsonIgnore
        public GregorianCalendar getEnd() {
            return end;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String getTaskName() {
            return taskName;
        }

        @Override
        public int compareTo(Object o) {
            if (this == o) return 0;
            final HistoricalEvent h = (HistoricalEvent) o;

            if (compCal(start, h.getStart()) == 1) {
                return 1;
            } else if (compCal(start, h.getStart()) == -1) {
                return -1;
            } else if (compCal(end, h.getEnd()) == 1) {
                return 1;
            } else if (compCal(end, h.getEnd()) == -1) {
                return -1;
            } else {
                return taskName.compareTo(h.getTaskName());
            }

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;
            return equals((HistoricalEvent) o);
        }

        public boolean equals(HistoricalEvent h) {
            return (compCal(start, h.getStart()) == 0 && compCal(end, h.getEnd()) == 0 && taskName.equals(h.getTaskName()));
        }
    }

}