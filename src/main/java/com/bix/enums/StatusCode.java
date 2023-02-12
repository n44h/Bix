package com.bix.enums;

/**
 * Enum to represent the status of Bix.
 * This includes safe terminations, runtime exceptions, timeouts, authentication status, misc. errors.
 */

public enum StatusCode {

    // Safe Termination.
    SAFE_TERMINATION(0, "Safe Termination.", "info"),

    // Resource Not Found.
    CONFIG_FILE_NOT_FOUND(1, "The config.properties file could not be found.", "error"),
    VAULT_FILE_NOT_FOUND(2, "The vault.db file could not be found.", "error"),
    CONSOLE_NOT_FOUND(3, "Unable to find a system console associated with the current JVM.", "error"),

    // Opening GitHub Page Error.
    ERROR_OPENING_GITHUB_PAGE(4, "An error occurred while accessing the config.properties file.", "warn"),
    INVALID_GITHUB_URL_SYNTAX(5, "An error occurred while accessing the vault.db file.", "warn"),

    // Setup Failure.
    MASTER_PASSWORD_SETUP_FAILED(6, "Master Password setup failed.", "error"),

    // Authentication Failure.
    AUTHENTICATION_FAILED(7, "User Authentication Failed.", "error"),

    // Idle Session Timeout.
    IDLE_SESSION_TIMEOUT(8, "Bix session terminated due to inactivity.", "warn"),

    // Unknown Errors.
    UNKNOWN_ERROR(127, "An unknown error occurred.", "error");
    //--------------------------------------------------------------------------------------------

    public final int code;
    public final String message;
    public final String logLevel;

    StatusCode(final int code, final String message, final String logLevel) {
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

} // enum StatusCode