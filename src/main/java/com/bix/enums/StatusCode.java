package com.bix.enums;

/**
 * Enum to represent the status of Bix.
 * This includes safe terminations, runtime exceptions, timeouts, authentication status, misc. errors.
 */

public enum StatusCode {

    // Safe Termination.
    SAFE_TERMINATION(0, "Safe Termination."),

    // Resource Not Found.
    CONFIG_FILE_NOT_FOUND(1, "The config.properties file could not be found."),
    VAULT_FILE_NOT_FOUND(2, "The vault.db file could not be found."),
    CONSOLE_NOT_FOUND(3, "Unable to find a system console associated with the current JVM."),

    // Resource Access Error.
    ERROR_ACCESSING_CONFIG_FILE(4, "An error occurred while accessing the config.properties file."),
    ERROR_ACCESSING_VAULT_FILE(5, "An error occurred while accessing the vault.db file."),

    // Setup Failure.
    MASTER_PASSWORD_SETUP_FAILED(6, "Master Password setup failed."),

    // Authentication Failure.
    AUTHENTICATION_FAILED(7, "User Authentication Failed."),

    // Idle Session Timeout.
    IDLE_SESSION_TIMEOUT(8, "Bix session terminated due to inactivity."),

    // Unknown Errors.
    UNKNOWN_RESOURCE_ERROR(126, "An unknown file/resource error has occurred."),
    UNKNOWN_ERROR(127, "An unknown error occurred.");
    //--------------------------------------------------------------------------------------------

    private final int statusCode;
    private final String message;

    StatusCode(final int statusCode, final String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() { return statusCode; }

    public String getMessage() { return message; }

} // enum StatusCode