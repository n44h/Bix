package bix;

import bix.enums.AESFlavor;
import bix.enums.StatusCode;
import bix.utils.Crypto;
import bix.utils.TransientPrinter;
import bix.utils.VaultController;

import java.util.ArrayList;
import java.util.Arrays;

import static bix.utils.Utils.*;
import static bix.utils.Reader.*;
import static bix.utils.VaultController.*;

import static bix.utils.Constants.AES_FLAVOR_HELP_STRING;
import static bix.utils.Constants.PURGE_VAULT_WARNING_MSG;
import static bix.utils.Constants.RESET_BIX_WARNING_MSG;
import static bix.utils.Constants.FAILED_LOGIN_ATTEMPT_LIMIT;


/**
 * This class handles all the backend operations of Bix.
 */
public class Controller {
    private Controller(){} // Enforce non-instantiability of this class.


    // Class constants.
    private static Crypto CRYPTO;
    private static TransientPrinter TRANSIENT_PRINTER;
    private static char[] MASTER_PASSWORD = null;
    private static int failedLoginAttempts = getIntMetadata("failed_login_attempts");


    /**
     * Check if the initial Bix setup is complete.
     *
     * @return true if the initial Bix setup is complete.
     */
    static boolean isInitialSetupComplete() {
        try {
            return Boolean.parseBoolean(getStrMetadata("setup_complete"));
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Runs initial setup processes.
     */
    static void setup() {
        // Perform initial setup, if it is not complete.
        if(!isInitialSetupComplete()) {
            // Set up the vault.
            setupVault();

            // Set the AES flavor.
            setAESFlavor();

            // Set the master password for Bix.
            setMasterPassword();

            // If Master Password setup failed.
            if(MASTER_PASSWORD == null) {
                // Reset Bix.
                resetBix();

                // Terminate session.
                terminateSession(StatusCode.MASTER_PASSWORD_SETUP_FAILED);
            }

            // Once setup is completed successfully, update bix_metadata table.
            updateMetadata("setup_complete", "true");
        }

        // Load Idle Session Timeout Duration.
        int idleSessionTimeout = getIntMetadata("idle_session_timeout");
        // Set the idle session timeout for the Reader.
        setIdleTimeout(idleSessionTimeout);

        // Load Credential Display Duration.
        var credentialDisplayDuration = getIntMetadata("credential_display_duration");
        // Initialize a TransientPrinter instance with the credential display duration.
        TRANSIENT_PRINTER = new TransientPrinter(credentialDisplayDuration);

        // Load AES flavor.
        var aesFlavor = AESFlavor.fromString(getStrMetadata("aes_flavor"));
        // Initialize a Crypto instance with the AES flavor.
        CRYPTO = new Crypto(aesFlavor.toInteger());

    }

    /**
     * Sets the Master Password for Bix. Standard Password setting process.
     */
    private static void setMasterPassword() {
        clearScreen();

        // Get the new Master Password from user. Get password twice for validation.
        var firstInput = readPassword("> Enter your new Master Password (1st time) : ");
        var secondInput = readPassword("> Enter your new Master Password (2nd time) : ");

        // Clearing the interface.
        clearScreen();

        // Checking that the first and second inputs match.
        if (firstInput == secondInput) {
            // Store the newly created Master Password's hash in the bix_metadata table.
            updateMetadata("master_password_hash", CRYPTO.getSHA256Hash(firstInput));
        }
        else {
            System.out.println("\nFailed to set Master Password: password inputs did not match. ");
        }
    }

    /**
     * Set up the AES flavor (128-bit, 192-bit or 256-bit).
     */
    static void setAESFlavor() {
        AESFlavor aesFlavor;
        char userChoice;
        do {
            clearScreen();

            // Print the help string explaining the AES flavors.
            System.out.println(AES_FLAVOR_HELP_STRING);

            // Get the user's choice of AES flavor.
            userChoice = readChar("> Choose an AES flavor: ");

            switch (userChoice) {
                case '1' -> aesFlavor = AESFlavor.AES_128;
                case '2' -> aesFlavor = AESFlavor.AES_192;
                default -> aesFlavor = AESFlavor.AES_256;
            }

        } while(!getConfirmation(
                String.format("""
                \nWARNING: You cannot change the AES flavor once it has been set.
                > Confirm your choice (AES-%d) [N/y]:\040""", aesFlavor.toInteger()
                ), true)
        );

        // Save the AES flavor to the metadata table.
        updateMetadata("aes_flavor", aesFlavor.toString());
    }

    /**
     * Method to authenticate the user.
     */
    static boolean authenticateUser() {
        // Get the Master Password hash.
        var masterPasswordHash = getStrMetadata("master_password_hash");

        clearScreen();

        do {
            // Get the Master Password from the user.
            MASTER_PASSWORD = readPassword("Enter Master Password: ");

            // Authenticating Master Password input.
            if (CRYPTO.getSHA256Hash(MASTER_PASSWORD).equals(masterPasswordHash)) {
                clearScreen();
                System.out.println("\nAuthentication successful.");

                // Reset failedLoginAttempts to 0.
                failedLoginAttempts = 0;

                return true;
            }
            // Failed authentication.
            else {
                clearScreen();
                System.out.println("\nERROR: Incorrect Master Password.");
                failedLoginAttempts++;
            }
        } while(failedLoginAttempts < FAILED_LOGIN_ATTEMPT_LIMIT);

        return false;
    }

    /**
     * Finds account names that contain or match the keyword provided.
     * @param keyword find accounts containing this keyword
     * @return an {@code ArrayList<String>} containing all the Account names that contain the keyword
     */
    static ArrayList<String> getAccountNamesContaining(String keyword) {
        return getAccountNamesContaining(keyword);
    }

    /**
     * Print all the account names stored in Bix.
     */
    static void printAccountNames() {
        for(var accountName : getAccountNames()) {
            System.out.println(accountName);
        }
    }

    /**
     * Print account names containing a specified keyword.
     *
     * @param keyword keyword to look for in account names
     */
    static void printAccountNames(String keyword) {
        for(var accountName : getAccountNamesContaining(keyword)) {
            System.out.println(accountName);
        }
    }

    /**
     * Prints the username and password of an account
     * @param accountName the account to print the credentials for
     */
    static void printCredentials(String accountName){
        /* Contents of the String[] returned by retrieveAccount():
         * +--------------+--------------+--------------+--------------+--------------+--------------+--------------+
         * | values[0]    | values[1]    | values[2]    | values[3]    | values[4]    | values[5]    | values[6]    |
         * |--------------|--------------|--------------|--------------|--------------|--------------|--------------|
         * | account_name | email        | ciphertext_u | ciphertext_p | salt         | iv           | secret_hash  |
         * +--------------+--------------+--------------+--------------+--------------+--------------+--------------+
         */

        String associatedEmail, ciphertextUsername, ciphertextPassword, salt, iv, secretKeyHash;

        try {
            String[] values = retrieveAccount(accountName);

            // Store values in appropriate variables.
            accountName = values[0];
            associatedEmail = values[1];
            ciphertextUsername = values[2];
            ciphertextPassword= values[3];
            salt = values[4];
            iv = values[5];
            secretKeyHash = values[6];
        }
        catch (Exception ae) {
            System.out.println(ae.getMessage());
            return;
        }

        // Authenticating the account's secret key generated with the master password and salt to the secretKeyHash.
        if (!CRYPTO.authenticateSecretKey(MASTER_PASSWORD, salt, secretKeyHash)) {
            terminateSession(StatusCode.AUTHENTICATION_FAILED);
        }

        // Decrypting ciphertext.
        char[] username = CRYPTO.decrypt(MASTER_PASSWORD, ciphertextUsername, salt, iv);
        char[] password = CRYPTO.decrypt(MASTER_PASSWORD, ciphertextPassword, salt, iv);

        // Clear Screen and display account name.
        clearScreen();
        System.out.println(accountName);

        // Print credentials to Terminal.
        System.out.printf("\nUsername         : %s", Arrays.toString(username));
        System.out.printf("\nPassword         : %s", Arrays.toString(password));
        System.out.printf("\nAssociated email : %s", associatedEmail == null ? "nil" : associatedEmail);

        // Clear credentials from memory.
        clearCharArrayFromMemory(username);
        clearCharArrayFromMemory(password);

        clearScreen();
    }

    private static ArrayList<char[]> getCredentialsFromUser(String accountName) {
        /* The following do while loop is an infinite loop.
         *
         * This is so that if the user does not give a positive response for their input confirmation, the loop will
         * start over again which allows the input to be provided again.
         *
         * Once a positive response for the input confirmation is received from the user, the return statement will
         * effectively terminate the loop and the function call.
         */

        ArrayList<char[]> credentials = new ArrayList<>();
        char[] username, password1, password2, email;


        // Get username.
        username = readString(String.format("> Enter %s username: ", accountName)).toCharArray();

        // Add the Username to credentials list.
        credentials.add(username);

        clearScreen();

        // Run loop to get the password.
        do {
            clearScreen();
            System.out.print("""
                    NOTE: The password inputs will not be visible on the screen as you type.
                          Your keyboard inputs will be directly registered by Bix.
                    """);

            // Get Password first time.
            password1 = readPassword(String.format("> Enter %s password (1st time): ", accountName));

            // Get Password second time.
            password2 = readPassword(String.format("> Enter %s password (2nd time): ", accountName));

            // Check the first and second password entries match.
            if (password1 != password2) {
                System.out.print("\nThe password inputs do not match. Try again in 5 seconds.\n");
                sleep(5);
            }

        } while (password1 != password2);

        // Add the Password to credentials list.
        credentials.add(password1);

        clearScreen();

        // Add associated email.
        if (getConfirmation(
                "> Do you want to add an associated email for this account? [Y/n]: ", false)) {
            clearScreen();

            // Get email from user.
            email = readString(String.format("> Enter associated email for %s: ", accountName)).toCharArray();
            credentials.add(email);
        }
        else {
            // represents null value for email.
            credentials.add(null);
        }

        clearScreen();

        return credentials;
    }


    /* Caution Zone */

    /**
     * Clears all stored accounts from the Bix vault.
     */
    static void purgeVault() {
        // Print warning.
        System.out.println(PURGE_VAULT_WARNING_MSG);

        // Get confirmation to purge vault.
        if (getConfirmation("> Confirm purge vault [N/y]: ", true)) {
            // Authenticate the user.
            if (authenticateUser()) {
                // Purge Vault.
                VaultController.purgeVault();
                System.out.println("\nPurged Bix vault. All stored account details have been cleared.");
            }
            else {
                System.out.println("\nVault purge operation aborted.");
            }
        }
    }

    /**
     * Reset Bix to its initial state.
     */
    static void resetBix() {
        // Print warning.
        System.out.println(RESET_BIX_WARNING_MSG);

        // Get confirmation to reset Bix.
        if (getConfirmation("Confirm Bix reset [N/y]: ", true)) {
            // Authenticate the user before resetting Bix.
            if (authenticateUser()) {
                // Purge vault.
                VaultController.purgeVault();

                // Clear Master Password.
                clearCharArrayFromMemory(MASTER_PASSWORD);

                System.out.println("\nBix reset complete.");
            }
            else {
                System.out.println("\nBix reset command aborted.");
            }
        }
    }


    /* Danger Zone */

    /**
     * Terminates the current Bix session.
     * @param status The appropriate status code from enum {@code StatusCode}.
     */
    public static void terminateSession(StatusCode status) {
        // Clears screen and removes the master password from memory.
        incinerate();

        // Print status message and terminate the session.
        System.out.printf("\n%s \nTerminating Bix session.", status.message);
        System.exit(status.code);
    }

    /**
     * Performs the shutdown procedure.
     * Clears the master password from memory and clears the terminal.
     */
    static void incinerate() {
        // Clearing Master Password from memory.
        if (MASTER_PASSWORD != null) {
            // Setting every character in the char array to null character('\0') using Arrays.fill().
            Arrays.fill(MASTER_PASSWORD, '\0');
        }

        // Clearing screen.
        try {
            // For Windows systems.
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();

            // For Unix-based systems.
            else
                System.out.print("\033\143");
        }
        catch (Exception e) {
            System.out.println("\nError: Unable to clear terminal");
            e.printStackTrace();
        }
    }

} // class Controller
