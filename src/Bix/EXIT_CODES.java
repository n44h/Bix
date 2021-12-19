package Bix;

public enum EXIT_CODES {
    SAFE_TERMINATION(0, "Safe Termination."),

    MISSING_CONFIG_FILE(100, "The config.properties file is missing."),
    MISSING_HASH_FILE(101, "The hash file is missing."),
    MISSING_VAULT(102, "The vault.csv file is missing."),

    AUTHENTICATION_FAILED(200, "User Authentication Failed."),
    INCORRECT_MASTER_PASSWORD(201, "Authentication Failed. Incorrect Master Password."),
    CONSOLE_NOT_FOUND(202, "Could not find system console.");

    private final int exit_code;
    private final String message;

    EXIT_CODES(int value, String message) {
        this.exit_code = value;
        this.message = message;
    }

    public int getExitCode() {
        return exit_code;
    }

    public String getMessage() { return message; }
}
