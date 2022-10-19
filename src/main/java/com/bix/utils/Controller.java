package com.bix.utils;

import com.bix.enums.AESFlavor;
import com.bix.enums.StatusCode;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import static com.bix.utils.Crypto.*;
import static com.bix.utils.Reader.*;
import static com.bix.utils.VaultController.*;


/**
 * This class handles all the backend operations of Bix.
 */
public final class Controller {
    // Manually set values.
    private static final String CONFIG_FILE = "config.properties";
    private static final String VAULT_FILE = "vault.db";
    private static final String GITHUB_URL = "https://github.com/CookieCrumbs19212/Bix";

    // Class constants.
    private static Properties BIX_PROPERTIES = new Properties();
    private static int AES_FLAVOR = AESFlavor.AES_128.toInteger();
    private static char[] MASTER_PASSWORD = null;
    private static int CREDENTIAL_DISPLAY_DURATION;
    private static URI GITHUB_PAGE;
    private static final int FAILED_LOGIN_ATTEMPT_LIMIT = 3;

    // Global variables.
    private static int failedLoginAttempts = getIntMetadata("failed_login_attempts");

    // Static initializer.
    static {
        try {
            // Input stream from config file (config file is in the resource folder).
            InputStream configFileInputStream = Controller.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

            // Loading the properties into BIX_PROPERTIES.
            BIX_PROPERTIES.load(configFileInputStream);

            // Get the credentials display duration from config file.
            // For security: maximum allowed duration is 10 minutes.
            CREDENTIAL_DISPLAY_DURATION = Math.min(10 * 60,
                    Integer.parseInt(BIX_PROPERTIES.getProperty("credential_display_duration")));

            // Set the idle session timeout in the Reader class.
            int idleSessionTimeout = Integer.parseInt(BIX_PROPERTIES.getProperty("idle_session_timeout"));
            setIdleTimeout(idleSessionTimeout);

            // Create the GitHub page URI object.
            GITHUB_PAGE = new URI(GITHUB_URL);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            terminateSession(StatusCode.ERROR_ACCESSING_CONFIG_FILE);
        }
        catch (NullPointerException ne) {
            ne.printStackTrace();
            terminateSession(StatusCode.CONFIG_FILE_NOT_FOUND);
        }
        catch (Exception e) {
            e.printStackTrace();
            terminateSession(StatusCode.UNKNOWN_RESOURCE_ERROR);
        }
    }

    /**
     * Check if the initial Bix setup is complete.
     *
     * @return true if the initial Bix setup is complete.
     */
    public static boolean isBixSetupComplete() {
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
    public static void setup() {
        boolean passwordSetupComplete = false;

        // Set up the vault.
        VaultController.setupVault();

        // Set the master password for Bix.
        while (!passwordSetupComplete) {
            passwordSetupComplete = setMasterPassword();
        }

        // Once setup is completed successfully, update bix_metadata table.
        updateMetadata("setup_complete", "true");
    }

    /**
     * Reset Bix to its initial state.
     */
    public static void resetBix() {
        // Purge vault.
        purgeVault();

        // Clear Master Password.
        clearCharArrayFromMemory(MASTER_PASSWORD);
    }

    /**
     * Sets the Master Password for Bix. Standard Password setting process.
     */
    private static boolean setMasterPassword() {
        char[] firstInput, secondInput; // stores the user's input

        clearScreen();

        // Get the new Master Password from user.
        firstInput = readPassword("> Enter your new Master Password (1st time) : ");
        secondInput = readPassword("> Enter your new Master Password (2nd time) : ");

        // Clearing the interface.
        clearScreen();

        // Checking that the first and second inputs match.
        if (firstInput == secondInput) {
            // Store the newly created Master Password's hash in the bix_metadata table.
            updateMetadata("master_password_hash", getSHA256Hash(firstInput));
            return true;
        }
        else {
            System.out.println("\nFailed to set Master Password: password inputs did not match. ");
            return false;
        }
    }

    /**
     * Set the AES flavor (128-bit, 192-bit or 256-bit).
     * @param flavor The desired AES flavor to be set as the working flavor.
     */
    public static void setAESFlavor(AESFlavor flavor){
        AES_FLAVOR = flavor.toInteger();
    }

    /**
     * Method to authenticate the user.
     */
    public static boolean authenticateUser() {
        // Get the Master Password hash.
        String masterPasswordHash = getStrMetadata("master_password_hash");

        clearScreen();

        do {
            // Get the Master Password from the user.
            MASTER_PASSWORD = readPassword("Enter Master Password: ");

            // Authenticating Master Password input.
            if (getSHA256Hash(MASTER_PASSWORD).equals(masterPasswordHash)) {
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
    public static ArrayList<String> getAccountNamesContaining(String keyword) {
        return VaultController.getAccountNamesContaining(keyword);
    }

    /**
     * Method to print stored account names.
     * Prints the account names containing the String {@code prompt}.
     *
     * @param prompt Prompt to find account names containing a specific String.
     *               Prints all account names when set to {@code null}.
     */
    public static void printAccountNames(String prompt) {
        // If prompt is null, print all account names.
        if (prompt==null) {
            try (BufferedReader br = new BufferedReader(new FileReader(VAULT_FILE))) {
                String line;
                System.out.println("\nStored Accounts: ");
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(","); // split up values in the line and store in String array
                    System.out.println(values[0]); // printing account names

                } // while

            } // try
            catch (IOException e) { e.printStackTrace(); }
        }
    } // printAccountNamesList()



    /*-----------------------------------------------------------------------------------------*/

    public static void printCredentials(String accountName){
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

        // Authenticating the key generated with the master password and salt to the secretKeyHash.
        if (!authenticateSecretKey(MASTER_PASSWORD, salt, AES_FLAVOR, secretKeyHash)) {
            terminateSession(StatusCode.AUTHENTICATION_FAILED);
        }

        // Decrypting ciphertext.
        char[] username = decrypt(MASTER_PASSWORD, ciphertextUsername, salt, iv, AES_FLAVOR);
        char[] password = decrypt(MASTER_PASSWORD, ciphertextPassword, salt, iv, AES_FLAVOR);

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

        // Sleeps for a preset duration then clear the credentials from the screen.
        sleep();
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
            System.out.print("NOTE: The password inputs will not be visible on the screen as you type.\n" +
                             "      Your keyboard inputs will be directly registered by Bix.\n\n");

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
        char userChoice = readChar("> Do you want to add an associated email for this account? [Y/n]: ");
        if (Character.toUpperCase(userChoice) == 'Y') {
            clearScreen();

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


    /**
     * Performs the shutdown procedure. Clears master password from memory and clears the terminal.
     */
    public static void executeShutdownProcedure() {
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

    /**
     * Terminates the current Bix session.
     * @param status The appropriate status code from enum {@code StatusCode}.
     */
    public static void terminateSession(StatusCode status) {
        // Clears screen and clear the master password from memory.
        executeShutdownProcedure();

        // Terminating the session.
        System.out.print("\nTerminating Bix session.");
        System.exit(status.getStatusCode());
    }

    // Utility Functions.
    /**
     * Clears the terminal.
     */
    public static void clearScreen() {
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

    /**
     * Clear character arrays from memory by setting its elements to null characters
     */
    private static void clearCharArrayFromMemory(char[] char_array){
        // Setting every character in the array to null character('\0') using Arrays.fill().
        Arrays.fill(char_array,'\0');
    }

    /**
     * Function to pause code execution for set amount of time.
     */
    private static void sleep() {
        try { Thread.sleep(CREDENTIAL_DISPLAY_DURATION * 1000L); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    /**
     * Function to pause code execution for set amount of time.
     *
     * @param duration the duration to sleep in seconds
     */
    private static void sleep(int duration) {
        try { Thread.sleep(duration * 1000L); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    /**
     * Opens the GitHub page for Bix in the default browser.
     */
    public static void openGitHubPage() {
        try {
            Desktop desktop = Desktop.getDesktop();

            // Open the URL.
            desktop.browse(GITHUB_PAGE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

} // class Controller
