package com.bix.utils;

import com.bix.enums.AESFlavor;
import com.bix.enums.StatusCode;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
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
    public static ArrayList<String> getAccountNamesContaining(String keyword){
        ArrayList<String> accountNamesContainingKeyword = new ArrayList<>();
        // Converting the keyword to lower case because the .contains() method is case sensitive.
        keyword = keyword.toUpperCase(Locale.ROOT);
        // Looping through the accountNames array to find all account names that contain the keyword.
        for(String account : getAccountNames()){
            if(account.contains(keyword))
                accountNamesContainingKeyword.add(account);
        }
        return accountNamesContainingKeyword;
    }

    /**
     * Method to print stored account names.
     * Prints the account names containing the String {@code prompt}.
     *
     * @param prompt Prompt to find account names containing a specific String.
     *               Prints all account names when set to {@code null}.
     */
    public static void printAccountNames (String prompt) {
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

    public static void printCredentialsFor(String accountName){
        /* credentials.csv file format:
         *  account_name ,  ciphertext  ,     salt     ,     iv      , secret_key hash
         *    values[0]      values[1]      values[2]     values[3]       values[4]
         */

        String[] values = null; // to store each value of a comma separated value line
        try (BufferedReader br = new BufferedReader(new FileReader(VAULT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                values = line.split(","); // split up values in the line and store in String array

                if (values[0].contains(accountName)) {
                    break;
                } // checking if account is found
            } // while
        } // try
        catch (IOException e) { e.printStackTrace(); }

        assert values != null;
        String ciphertext = values[1];
        String salt = values[2];
        String iv = values[3];

        // comparing hash values of user entered password and hash stored in csv file
        // Authenticating the key generated from the master password and salt to the target hash values[4].
        if (!authenticateSecretKey(MASTER_PASSWORD, salt, AES_FLAVOR, values[4])) {
            terminateSession(StatusCode.AUTHENTICATION_FAILED);
        }

        // Decrypting ciphertext.
        String plaintext = decrypt(MASTER_PASSWORD, ciphertext, salt, iv, AES_FLAVOR);


        // Print credentials to Terminal.
        System.out.println("\nUsername: " + plaintext.substring(0, plaintext.indexOf(" ")));
        System.out.println("Password: " + plaintext.substring(plaintext.indexOf(" ") + 1));
        sleep(); // Sleeps for a preset duration.
        clearScreen(); // Clear credentials from the screen.

    } // printCredentialsFor()

    /*-----------------------------------------------------------------------------------------*/


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
        char[] username, password1, password2;


        // Get username.
        username = readString(String.format("> Enter %s username: ", accountName)).toCharArray();


        // Run loop to get the password.
        do {
            // Get Password first time.
            password1 = readPassword(String.format("> Enter %s password (1st time): ", accountName));

            // Get Password second time.
            password2 = readPassword(String.format("> Enter %s password (2nd time): ", accountName));

            // Check the first and second password entries match.
            if (password1 != password2)
                System.out.print("\nThe password inputs do not match. Try again.\n");

        } while (password1 != password2);


        // Add the Username to credentials list.
        credentials.add(username);

        // Add the Password to credentials list.
        credentials.add(password1);

        return credentials;
    }

    public static void executeShutdownProcedure() {
        // Clear the screen so that any sensitive information is erased from the terminal.
        clearScreen();

        // If master password is not null, clear it from memory.
        if (MASTER_PASSWORD != null) {
            // Setting every character in the char array to null character('\0') using Arrays.fill().
            Arrays.fill(MASTER_PASSWORD, '\0');
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
