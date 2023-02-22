package bix.utils;

public final class Constants {
    private Constants() {} // Enforce non-instantiability for this class.

    // Cipher Algorithm: AES in CBC mode with PKCS5 padding.
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";

    // Resource path to config file.
    public static final String CONFIG_FILE_RESOURCE_PATH = "config.properties";

    // Resource path to vault file.
    public static final String VAULT_RESOURCE_PATH = "vault.db";

    // URL to Bix GitHub page.
    public static final String BIX_GITHUB_URL = "https://github.com/CookieCrumbs19212/Bix";

    // Number of failed login attempts before vault is purged.
    public static final int FAILED_LOGIN_ATTEMPT_LIMIT = 3;

    // Main menu options String.
    public static final String MAIN_MENU_OPTIONS = """
            Bix Main Menu:
            
            \t[0] Display Stored Accounts

            \t[1] Retrieve Account

            \t[2] Add Account

            \t[3] Update Account
            
            \t[4] Delete Account

            \t[M] More Options
            
            \t[H] Help

            \t[X] Exit Bix
            
            """;

    // Extended menu options String.
    public static final String EXT_MENU_OPTIONS = """
            Bix Extended Menu:
            
            \t[5] Reset Master Password
            
            \t[6] Change Credential Display Duration
            
            \t[7] Change Idle Timeout Duration
            
            \t[8] Import Vault
            
            \t[9] Export Vault
            
            \t[G] Open Bix GitHub Page
            
            +---------------------+
            |   DANGER ZONE:      |
            |---------------------|
            |   [P] Purge Vault   |
            |                     |
            |   [R] Reset Bix     |
            +---------------------+
            
            \t[B] Back to Main Menu
            
            \t[X] Exit Bix
            
            """;

    // Helpful descriptions for Bix actions.
    public static final String HELP_STRING = """
            Bix Help
            
            - Account Actions:
            
            \t[0] View Saved Accounts - Prints the names of all the stored accounts
            
            \t[1] Retrieve Account - Retrieve a stored account's credentials
            
            \t[2] Add Account - Add a new account

            \t[3] Update Account - Update an existing account's credentials
            
            \t[4] Delete Account - Delete a stored account
            
            
            - Bix Settings:
            
            \t[5] Reset Master Password - Reset the Bix master password
                  
            \t[6] Change Credential Display Duration - Change the duration the credentials are displayed on the screen
            
            \t[7] Change Idle Timeout Duration - Change how long Bix can stay idle before terminating the session
            
            
            - Vault Actions:
            
            \t[8] Import Vault - Import a Bix vault
            
            \t[9] Export Vault - Export the Bix vault
            
            \t[P] Purge Vault - Destroy the contents of the Bix vault. Use this option if you no longer intend to use Bix
            
            
            - Reset Bix:
            
            \t[R] Reset Bix - This action will purge the Bix vault and remove the master password.
            
            
            - GitHub Page:
            
            \t[G] Open Bix GitHub Page - Open the GitHub page for Bix in the default browser
            
            """;

    // Helpful descriptions for choosing an AES flavor during setup.
    public static final String AES_FLAVOR_HELP_STRING = """
            Pick an AES flavor. Bix will use this flavor of AES when encrypting credentials.
            
            AES-128 is the fastest.
            AES-256 provides the highest level of security. This is the default flavor.
            AES-192 is midway between AES-128 and AES-256 in terms of speed and security.
            
            Although AES-128 is technically considered "less secure" compared to the other flavors,
            it still provides a very high level of security.
            
            WARNING: You cannot change the AES flavor once it has been set.
            
            [1] AES-128
            [2] AES-192
            [3] AES-256 (default)
            """;

    // Warning message that is printed when purging the vault.
    public static final String PURGE_VAULT_WARNING_MSG = """
            WARNING: All the saved accounts from the Bix vault will be permanently deleted.
                     This action is irreversible. Please be sure before proceeding.
            """;

    // Warning message that is printed when resetting Bix.
    public static final String RESET_BIX_WARNING_MSG = """
            WARNING: All the saved accounts from the Bix vault will be permanently deleted.
                     The Bix master password will be removed.
                     This action is irreversible. Please be sure before proceeding.
            """;

    // Informational message for password input.
    public static final String PASSWORD_INPUT_MSG = """
            NOTE: The password inputs will not be visible on the screen as you type.
                  Your keyboard inputs will be directly registered by Bix.
            """;

    // Lower and Upper limits for the Idle Session Timeout Duration.
    public static final int IDLE_TIMEOUT_DURATION_LOWER_LIMIT = 10;
    public static final int IDLE_TIMEOUT_DURATION_UPPER_LIMIT = 10 * 60;
    public static final String SET_TIMEOUT_DURATION_MSG = String.format("""
            Idle Session Timeout Duration must be >= %d seconds and <= %d seconds.
            """, IDLE_TIMEOUT_DURATION_LOWER_LIMIT, IDLE_TIMEOUT_DURATION_UPPER_LIMIT);

    // Lower and Upper limits for the Timed Display Duration.
    public static final int TIMED_DISPLAY_DURATION_LOWER_LIMIT = 1;
    public static final int TIMED_DISPLAY_DURATION_UPPER_LIMIT = 10 * 60;
    public static final String SET_DISPLAY_DURATION_MSG = String.format("""
            Credential Display Duration must be >= %d seconds and <= %d seconds.
            """, TIMED_DISPLAY_DURATION_LOWER_LIMIT, TIMED_DISPLAY_DURATION_UPPER_LIMIT);

    // Regex for validating email addresses.
    public static final String EMAIL_VALIDATION_REGEX =
            "^[\\\\w!#$%&’*+/=?`{|}~^-]+(?:\\\\.[\\\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\\\.)+[a-zA-Z]{2,6}$";

} // class Constants
