package server.enums;

/**
 * Possible status of tasks requested to the system
 * Failed is necessary in case of a malformed directory that will keep the task forever unsaved
 */
public enum StatusEnum {
    PROCESSING,
    DONE,
    FAILED
}