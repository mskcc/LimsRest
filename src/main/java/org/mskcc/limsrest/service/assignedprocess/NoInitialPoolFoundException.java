package org.mskcc.limsrest.service.assignedprocess;

public class NoInitialPoolFoundException extends Exception {
    public NoInitialPoolFoundException(String message) {
        super(message);
    }
}
