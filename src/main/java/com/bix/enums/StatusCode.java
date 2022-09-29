package com.bix.enums;

public enum StatusCode {
    /*
        How to Read Error Codes:
        The 1st digit of the error codes indicate the general group the error belongs to.
        e.g.: Error codes starting with '1' indicate a File/Resource error.

        The 2nd digit further categorizes the error into subgroups.
        e.g.: Error codes starting with '11' indicate that a File/Resource is not found.

        Unknown errors in each group end with a '00'.
        e.g.: The error code for an unknown File error is '100'.
     */

    // 0. Safe Termination.
    SAFE_TERMINATION(0, "Safe Termination."),

    // 1. File/Resource Errors.
    UNKNOWN_RESOURCE_ERROR(100, "An unknown file/resource error has occurred."),

    PROPERTY_FILE_NOT_FOUND(110, "The bix.properties file could not be found."),
    VAULT_FILE_NOT_FOUND(111, "The vault.db file could not be found."),
    CONSOLE_NOT_FOUND(112, "Unable to find a system console associated with the current JVM."),

    ERROR_ACCESSING_PROPERTY_FILE(120, "An error occurred while accessing the bix.properties file."),
    ERROR_ACCESSING_VAULT_FILE(121, "An error occurred while accessing the vault.db file."),

    // 2. Setup Errors.
    UNKNOWN_SETUP_ERROR(200,"Bix setup process failed for an unknown reason."),

    MASTER_PASSWORD_SETUP_FAILED(210, "Master Password setup failed."),

    // 3. Authentication Failure.
    UNKNOWN_AUTHENTICATION_ERROR(300, "An unknown error occurred during User Authentication."),

    AUTHENTICATION_FAILED(310, "User Authentication Failed."),

    // 4. Idle Session Timeout.
    TERMINATE_IDLE_SESSION(400, "Bix session terminated due to inactivity.");
    //--------------------------------------------------------------------------------------------

    private final int statusCode;
    private final String message;

    StatusCode(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() { return message; }

} // enum StatusCode
