package com.bix.utils;

import com.bix.enums.AESFlavor;
import com.bix.enums.StatusCode;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import static com.bix.utils.Crypto.*;
import static com.bix.utils.Reader.*;


/**
 * This class handles all the backend operations of Bix.
 */
public final class Handler {
    private static Properties BIX_PROPERTIES;
    private static String VAULT_FILE; // stores the path to the location where the csv file is stored
    private static String HASH_FILE; // stores the path to the location where the master key hash file is stored
    private static String MASTER_PASSWORD_HASH; // stores the SHA256 hash of the master key.
    private static boolean authSuccess; // FALSE by default, turns TRUE if the user authentication is successful.
    private static int AES_FLAVOR; // Can represent AES-128, AES-192, or AES-256.
    private static int vaultSize;
    private static String[] accountNames; // Stores the stored account names.
    private static Console CONSOLE; // System console reference.
    private static char[] MASTER_PASSWORD; // global char[] to store and access Master Password; must be cleared from memory before session end.
    private static int CREDENTIAL_DISPLAY_TIMEOUT; // duration that credentials are displayed in seconds.

    public Handler() {
        // Creating a Properties object to parse the config.properties file.
        BIX_PROPERTIES = new Properties();

        // Filename of the properties file.
        String propertiesFilename = "config.properties";

        try {
            // Creating an input stream object of the properties file which is in the Resource folder.
            InputStream resourceFileInputStream = getClass().getClassLoader().getResourceAsStream(propertiesFilename);

            // Loading the properties into BIX_PROPERTIES.
            BIX_PROPERTIES.load(resourceFileInputStream);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            terminateSession(StatusCode.ERROR_ACCESSING_PROPERTY_FILE);
        }
        catch (NullPointerException ne){
            ne.printStackTrace();
            terminateSession(StatusCode.PROPERTY_FILE_NOT_FOUND);
        }
        catch (Exception e) {
            e.printStackTrace();
            terminateSession(StatusCode.UNKNOWN_RESOURCE_ERROR);
        }

        /*
        // finding credentials.csv location
        try {
            // first line finds the location of .jar file, second line attaches
            // "credentials.csv" string to the parent path of .jar file
            // NOTE: it is assumed that the credentials.csv file is in the same folder as the .jar file
            File jarFile = new File(Bix.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            IVRY_FILE = jarFile.getParent() + File.separator + "credentials.csv";
        } catch (Exception e) {e.printStackTrace();}
                */
        MASTER_PASSWORD = null; // will be null until the user has been successfully authenticated.
        HASH_FILE = "/mkhash.txt";
        AES_FLAVOR = AESFlavor.AES_128.toInteger(); // default AES flavor is 128 bit.
        authSuccess = false;
        vaultSize = Integer.parseInt(BIX_PROPERTIES.getProperty("vault_size"));
        accountNames = new String[vaultSize];
        CONSOLE = System.console(); // attaching variable to the system console.
        if (CONSOLE == null)
            terminateSession(StatusCode.CONSOLE_NOT_FOUND);
        //verifyFileExists();

        // retrieve the SHA256 hash of the master key stored in the credentials.csv file
        /*try {
            InputStreamReader isReader = new InputStreamReader(this.getClass().getResourceAsStream(HASH_FILE));
            BufferedReader br = new BufferedReader(isReader);
            MASTER_PASSWORD_HASH = br.readLine();
            br.close();
        } catch (Exception e) { e.printStackTrace(); }*/
    } //constructor

    /**
     * Runs initial setup processes.
     */
    public static void setup(){
        // Firstly, confirm that the config file exists.
        File configFile = new File("something idk yet");
        try {
            // If this is the first time opening Bix, do the initial setup.
            if (Boolean.parseBoolean(BIX_PROPERTIES.getProperty("initial_setup_required"))) {
                // Step 1: Set up the Master Password.
                if (setMasterPassword()) {
                /* If initial setup was successful, set the "initial_setup_property" as false
                   to prevent setup procedure from redundantly running again in the future. */
                    BIX_PROPERTIES.setProperty("initial_setup_required", "false");
                }
                else
                    terminateSession(StatusCode.MASTER_PASSWORD_SETUP_FAILED);
            }

            // Loading the CREDENTIAL_DISPLAY_TIMEOUT.
            CREDENTIAL_DISPLAY_TIMEOUT = Integer.parseInt(BIX_PROPERTIES.getProperty("credential_display_duration"));

            // Setting the idle_timeout in the Reader class.
            setIdleTimeout(Integer.parseInt(BIX_PROPERTIES.getProperty("idle_session_timeout")));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }// setup()

    /**
     * Sets the Master Password for Bix. Standard Password setting process.
     * @return {@code true} if new Master Password was set successfully.
     */
    private static boolean setMasterPassword(){
        String firstInput,secondInput; // stores the user's input

        // Get the new Master Password from user.
        firstInput = readString("\n > Enter your new Master Password (1st time) : ");
        secondInput = readString("\n > Enter your new Master Password (2nd time) : ");

        // Clearing the interface.
        clearScreen();

        // Checking that the first and second inputs match.
        if(firstInput.equals(secondInput)){
            MASTER_PASSWORD_HASH = getSHA256Hash(firstInput);
            return true;
        }
        else{
            System.out.println("\nThe two passwords you entered did not match. Failed to set Master Password.");
            return false;
        }
    }// setMasterPassword()

    /**
     * Set the AES flavor (128-bit, 192-bit or 256-bit).
     * @param flavor The desired AES flavor to be set as the working flavor.
     */
    public static void setAESFlavor(AESFlavor flavor){
        AES_FLAVOR = flavor.toInteger();
    }

    private void verifyFileExists() { // method to check if the required csv and hash files exist
        boolean fileLocated = true; // turns false if one of the files is not found

        File csvFile = new File(VAULT_FILE);
        if (!csvFile.exists()) { // checking if csv file exists
            System.out.println("\nERROR: Failed to locate credentials.csv in filepath.");
            fileLocated = false;
        }
        URL hashLocation = Handler.class.getResource(HASH_FILE);
        if (hashLocation == null) {
            System.out.println("\nERROR: Failed to locate mkhash.txt in filepath.");
            fileLocated = false;
        }
        if (!fileLocated) { terminateSession(StatusCode.VAULT_FILE_NOT_FOUND); }

    } //verifyFile()

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
    } // authenticateUser()

    /**
     * Clear character arrays from memory by setting its elements to null characters.
     */
    public static void clearCharArrayFromMemory(char[] charArray){
        // Setting every character in the array to null character('\0') using Arrays.fill().
        Arrays.fill(charArray,'\0');
    } // clearCharArrayFromMemory()

    /**
     * Finds account names that contain or match the keyword provided.
     * @param keyword Keyword for finding an account.
     * @return An {@code ArrayList<String>} containing all the Account names that contain the keyword.
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
     * Loads the account names from the credentials.csv file to memory for quicker access.
     */
    private static void loadAccountNames (){
        try (BufferedReader br = new BufferedReader(new FileReader(VAULT_FILE))) {
            int index = 0;
            String line;
            while ((line = br.readLine()) != null) {
                accountNames[index++] = line.substring(0, line.indexOf(',')); // split up values in the line and store in String array

            } // while

        } // try
        catch (IOException e) { e.printStackTrace(); }
    } // loadAccountNames()

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

    private static void sleep (){ // pauses code execution
        try { Thread.sleep(CREDENTIAL_DISPLAY_TIMEOUT * 1000L); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    } // sleep()

    public static void clearScreen() { // clears terminal and console
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception e) { e.printStackTrace(); }
    } // clearScreen()



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

    public static void openGitHubPage() {
        try {
            Desktop desktop = Desktop.getDesktop();

            // Get the GitHub page URL from config.properties.
            URI url = new URI(BIX_PROPERTIES.getProperty("github_page"));

            // Open the URL.
            desktop.browse(url);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        // Reassurance that the Master Password will always be cleared from the memory.
        clearCharArrayFromMemory(MASTER_PASSWORD);

        // Clearing the terminal.
        clearScreen();

        // Displaying the status message.
        System.out.print("\nTerminating Bix session.");

        // Terminating the session.
        System.exit(status.getStatusCode());
    } // terminateSession()

} // class Handler
