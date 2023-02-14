package bix.enums;

/**
 * Enum to represent the status of Bix.
 * This includes safe terminations, runtime exceptions, timeouts, authentication status, misc. errors.
 */

public enum StatusCode {

    // Safe Termination.
    SAFE_TERMINATION(0, "Session safely terminated."),

    // Resource Not Found.
    CONFIG_FILE_NOT_FOUND(1, "The config.properties file could not be found."),
    VAULT_FILE_NOT_FOUND(2, "The vault.db file could not be found."),
    CONSOLE_NOT_FOUND(3, "Unable to find a system console associated with the current JVM."),

    // Opening GitHub Page Error.
    ERROR_OPENING_GITHUB_PAGE(4, "An error occurred while accessing the config.properties file."),
    INVALID_GITHUB_URL_SYNTAX(5, "An error occurred while accessing the vault.db file."),

    // Setup Failure.
    MASTER_PASSWORD_SETUP_FAILED(6, "Master Password setup failed."),

    // Authentication Failure.
    AUTHENTICATION_FAILED(7, "User Authentication Failed."),

    // Idle Session Timeout.
    IDLE_SESSION_TIMEOUT(8, "Bix session terminated due to inactivity."),

    // Unknown Errors.
    UNKNOWN_ERROR(127, "An unknown error occurred.");
    //--------------------------------------------------------------------------------------------

    public final int code;
    public final String message;

    StatusCode(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

} // enum StatusCode