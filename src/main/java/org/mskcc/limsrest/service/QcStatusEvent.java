package org.mskcc.limsrest.service;

public class QcStatusEvent extends RestDescriptor  implements Comparable{
    private Long timestamp;
    private String event;

    public QcStatusEvent(Long timestamp, String event){
        this.timestamp = timestamp;
        this.event = event;
    }

    public Long getTimestamp(){
        return timestamp;
    }

    public String getEvent(){
        return event;
    }

    @Override 
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null) return false;
        if(this.getClass() != o.getClass()) return false;
        return equals((QcStatusEvent) o);
    }

    @Override
    public int hashCode(){
        Long h = timestamp + event.hashCode();
        return h.hashCode();
    } 

    public boolean equals(QcStatusEvent qc){
        return (getEvent().equals(qc.getEvent()) && getTimestamp().equals(qc.getTimestamp()));
    }

    @Override
     public int compareTo(Object o){
        if(this == o) return 0;
        final QcStatusEvent event = (QcStatusEvent) o;
        
        return timestamp.compareTo(event.getTimestamp());    
    }
}
