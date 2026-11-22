package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 409: assigning would exceed the technician's maximum workload. */
public class WorkloadExceededException extends ItsmException {
    public WorkloadExceededException(String message) { super("WORKLOAD_EXCEEDED", HttpStatus.CONFLICT, message); }
    public WorkloadExceededException() { this("This technician has reached the maximum workload."); }
}
