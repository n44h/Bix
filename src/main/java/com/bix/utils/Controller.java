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


/**
 * This class handles all the backend operations of Bix.
 */
public final class Controller {
    private static final String CONFIG_FILE = "config.properties";
    private static final String VAULT_FILE = "vault.db";
    private static Properties BIX_PROPERTIES = new Properties();
    private static final Console CONSOLE = System.console();
    private static int AES_FLAVOR = AESFlavor.AES_128.toInteger();
    private static char[] MASTER_PASSWORD = null;
    private static String MASTER_PASSWORD_HASH; // stores the SHA256 hash of the master key.
    private static int CREDENTIAL_DISPLAY_DURATION;
    private static URI GITHUB_URL;

    private static boolean authSuccess = false; // FALSE by default, turns TRUE if the user authentication is successful.
    private static int vaultSize;
    private static String[] accountNames; // Stores the stored account names.


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

            // Get Bix GitHub page URL from config file.
            GITHUB_URL = new URI(BIX_PROPERTIES.getProperty("github_page"));
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

        // If the console object is null, terminate session.
        if (CONSOLE == null)
            terminateSession(StatusCode.CONSOLE_NOT_FOUND);
    }

    /**
     * Runs initial setup processes.
     */
    public static void setup() {
        // Set the master password for Bix.
        setMasterPassword();
    }

    /**
     * Sets the Master Password for Bix. Standard Password setting process.
     */
    private static void setMasterPassword() {
        String firstInput,secondInput; // stores the user's input

        // Get the new Master Password from user.
        firstInput = readString("\n > Enter your new Master Password (1st time) : ");
        secondInput = readString("\n > Enter your new Master Password (2nd time) : ");

        // Clearing the interface.
        clearScreen();

        // Checking that the first and second inputs match.
        if (firstInput.equals(secondInput)) {
            MASTER_PASSWORD_HASH = getSHA256Hash(firstInput);
        }
        else {
            System.out.println("\nThe two passwords you entered did not match. Failed to set Master Password.");
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
    public static void authenticateUser(char[] masterPassword) {
        // Authenticating Master Password input.
        if (getSHA256Hash(new String(masterPassword)).equals(MASTER_PASSWORD_HASH)) { // comparing hash values
            // Clear the screen and display the appropriate message.
            clearScreen();
            System.out.println("\nAuthentication successful.");

            // Copy the input char[] to the class variable MASTER_PASSWORD.
            MASTER_PASSWORD = Arrays.copyOf(masterPassword, masterPassword.length);
            authSuccess = true; // set the user authentication flag to true.
        }
        // Failed authentication.
        else {
            terminateSession(StatusCode.AUTHENTICATION_FAILED);
        }

        // Clear the input char[] from the memory.
        clearCharArrayFromMemory(masterPassword);

        /* NOTE:
         * Don't clear MASTER_PASSWORD from the memory after user authentication because
         * you need it for encrypting and decrypting the vault and its contents.
         *
         * The MASTER_PASSWORD variable will automatically get cleared from the memory by the
         * terminateSession() method.
         */
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
        for(String account : accountNames){
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

    public static boolean accountExists (String accountName){ // returns true if the account exists in credentials.csv
        if (authSuccess) {
            accountName = accountName.toUpperCase(); // since the account names are stored in uppercase in csv file
            try (BufferedReader br = new BufferedReader(new FileReader(VAULT_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(","); // split up values in the line and store in String array
                    if (values[0].contains(accountName)) {
                        br.close();
                        return true;
                    }
                } // while
            } // try
            catch (IOException e) { e.printStackTrace(); }
        } // auth_success
        System.out.println("\nERROR: Account does not exist.");
        return false;
    } // accountExists()


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
        if (!authenticateSecretKey(new String(MASTER_PASSWORD), salt, AES_FLAVOR, values[4])) {
            terminateSession(StatusCode.AUTHENTICATION_FAILED);
        }

        // Decrypting ciphertext.
        String plaintext = decrypt(ciphertext, new String(MASTER_PASSWORD), salt, iv, AES_FLAVOR);


        // Print credentials to Terminal.
        System.out.println("\nUsername: " + plaintext.substring(0, plaintext.indexOf(" ")));
        System.out.println("Password: " + plaintext.substring(plaintext.indexOf(" ") + 1));
        sleep(); // Sleeps for a preset duration.
        clearScreen(); // Clear credentials from the screen.

    } // printCredentialsFor()

    /*-----------------------------------------------------------------------------------------*/

    public static void addAccountLogin() {
        // get account information
        String accountName = getInput("Account Name").toUpperCase();
        String username = getInput("Account Username");
        String password = getInput("Account Password");
        clearScreen(); // clears all sensitive information from the screen

        // Verifying the master key
        // Comparing hash values
        if (getSHA256Hash(new String(MASTER_PASSWORD)).equals(MASTER_PASSWORD_HASH)) {
            System.out.println("\nAuthentication successful.");
        }
        else {
            terminateSession(StatusCode.AUTHENTICATION_FAILED);
        }

        // creating new csv line entry
        String plaintext = username + " " + password;
        String newCsvEntry = accountName + "," + encrypt(plaintext, new String(MASTER_PASSWORD), AES_FLAVOR);

        // writing to csv file
        FileWriter csvWriter;
        try {
            csvWriter = new FileWriter(VAULT_FILE, true);
            csvWriter.append(newCsvEntry);
            csvWriter.append("\n");

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) { e.printStackTrace(); }

        // Print success message.
        System.out.println("Securely stored account credentials.");
        //terminateSession(StatusCode.SAFE_TERMINATION);

    } // createAccountLogin()

    private static String getInput(String dataName) {
        /* The following do while loop is an infinite loop.
         *
         * This is so that if the user does not give a positive response for their input confirmation, the loop will
         * start over again which allows the input to be provided again.
         *
         * Once a positive response for the input confirmation is received from the user, the return statement will
         * effectively terminate the loop and the function call.
         */
        do {
            // If the input is a password.
            if (dataName.toUpperCase(Locale.ROOT).contains("PASSWORD")) {

                // Get Password first time.
                System.out.print("\n > Enter " + dataName + " (1st time): ");
                char[] password1 = CONSOLE.readPassword();

                // Get Password second time.
                System.out.print("\n > Enter " + dataName + " (2nd time): ");
                char[] password2 = CONSOLE.readPassword();

                // Ensure the first and second password entries match.
                if (password1 == password2)
                    return new String(password1); // if they match, then return password1 as a String.
                else // if they do not match.
                    System.out.printf("\nThe %s inputs do not match. Please try again.\n", dataName);
            }
            // If input type is not password.
            else {
                // reading user input
                String userInput = readString(String.format("> Enter %s: ", dataName));

                // Confirming user's input.
                if (readString("\n *> Confirm this " + dataName + "? [Y]/[n]: "
                                        ).toLowerCase().charAt(0) == 'y')
                    return userInput; // return the value if user confirms.
            }
        } while (true); // endless loop. return statements will take care of exit.
    } // getInput()

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
            desktop.browse(GITHUB_URL);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

} // class Controller
