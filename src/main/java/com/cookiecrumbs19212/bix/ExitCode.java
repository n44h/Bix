package com.cookiecrumbs19212.bix;

public enum ExitCode {
    // Safe Termination.
    SAFE_TERMINATION(0, "Safe Termination."),

    // Missing files or resources.
    MISSING_CONFIG_FILE(100, "The config.properties file is missing."),
    MISSING_VAULT_FILE(102, "The vault.bxdb file is missing."),
    MISSING_CONFIG_PROPERTY(103, "Property Missing in Config File."),
    CONSOLE_NOT_FOUND(202, "Could not find system console."),

    // Setup failures.
    SETUP_FAILED_UNKNOWN(103,"Bix Setup Failed For Unknown Reason."),
    MASTER_PASSWORD_SETUP_FAILED(104, "Master Password Setup Failed."),

    // Authentication failures.
    AUTHENTICATION_FAILED(200, "User Authentication Failed."),
    INCORRECT_MASTER_PASSWORD(201, "Authentication Failed. Incorrect Master Password.");


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
