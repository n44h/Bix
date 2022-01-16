package com.cookiecrumbs19212.bix;

public enum ExitCode {
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
    UNKNOWN_RESOURCE_ERROR(100, "An unknown resource error has occurred."),

    PROPERTY_FILE_NOT_FOUND(110, "The config.properties file could not be found."),
    VAULT_FILE_NOT_FOUND(111, "The vault.bxdb file could not be found."),
    CONSOLE_NOT_FOUND(112, "Unable to find a system console associated with the current JVM."),

    ERROR_ACCESSING_PROPERTY_FILE(120, "An error occurred while accessing the config.properties file."),
    ERROR_ACCESSING_VAULT_FILE(121, "An error occurred while accessing the vault.bxdb file."),

    // 2. Setup Errors.
    UNKNOWN_SETUP_ERROR(200,"Bix setup process failed for an unknown reason."),

    MASTER_PASSWORD_SETUP_FAILED(210, "Master Password setup failed."),

    // 3. Authentication Failure.
    UNKNOWN_AUTHENTICATION_ERROR(300, "An unknown error occurred during User Authentication."),

    AUTHENTICATION_FAILED(310, "User Authentication Failed.");
    //--------------------------------------------------------------------------------------------

    private final int exit_code;
    private final String message;

    ExitCode(int value, String message) {
        this.exit_code = value;
        this.message = message;
    }

    public int getExitCode() {
        return exit_code;
    }

    public String getMessage() { return message; }
}
