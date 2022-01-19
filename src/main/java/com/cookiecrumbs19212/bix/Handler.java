package com.cookiecrumbs19212.bix;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * This is an object class that handles reading and writing in .csv files
 * This class also encrypts and decrypts data from .csv files
 */
class Handler {
    private static Properties bix_properties;
    private static String IVRY_FILE; // stores the path to the location where the csv file is stored
    private static String HASH_FILE; // stores the path to the location where the master key hash file is stored
    private static String MASTER_PASSWORD_HASH; // stores the SHA256 hash of the master key
    private static boolean auth_success; // FALSE by default, turns TRUE if the user authentication is successful
    private static int AES_flavor; // can be set to 128-bit, 192-bit or 256-bit.
    private static int vault_size;
    private static String[] account_names; // stores the stored account names.
    private static Console console; // System console reference.
    private static char[] MASTER_PASSWORD; // global char[] to store and access Master Password; must be cleared from memory before session end.
    private static int CREDENTIAL_DISPLAY_TIMEOUT; // duration that credentials are displayed in seconds.

    Handler() {
        // Creating a Properties object to parse the config.properties file.
        bix_properties = new Properties();
        // Filename of the properties file.
        String properties_filename = "config.properties";
        try {
            // Creating an input stream object of the properties file which is in the Resource folder.
            InputStream resource_file_input_stream = getClass().getClassLoader().getResourceAsStream(properties_filename);
            // Loading the properties into bix_properties.
            bix_properties.load(resource_file_input_stream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            terminateSession(ExitCode.ERROR_ACCESSING_PROPERTY_FILE);
        } catch (NullPointerException ne){
            ne.printStackTrace();
            terminateSession(ExitCode.PROPERTY_FILE_NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            terminateSession(ExitCode.UNKNOWN_RESOURCE_ERROR);
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
        AES_flavor = AESFlavor.AES_128.toInteger(); // default AES flavor is 128 bit.
        auth_success = false;
        vault_size = Integer.parseInt(bix_properties.getProperty("vault_size"));
        account_names = new String[vault_size];
        console = System.console(); // attaching variable to the system console.
        if (console == null)
            terminateSession(ExitCode.CONSOLE_NOT_FOUND);
        CREDENTIAL_DISPLAY_TIMEOUT = 15; // Setting session timeout to 15 seconds.
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
    static void setup(){
        // Firstly, confirm that the config file exists.
        File config_file = new File("something idk yet");
        try {
            // If this is the first time opening Bix, do the initial setup.
            if (Boolean.parseBoolean(bix_properties.getProperty("initial_setup_required"))) {
                // Step 1: Set up the Master Password.
                if (setMasterPassword()) {
                /* If initial setup was successful, set the "initial_setup_property" as false
                   to prevent setup procedure from redundantly running again in the future. */
                    bix_properties.setProperty("initial_setup_required", "false");
                } else {
                    System.out.println("\nBix Setup Failed.\n");
                    terminateSession(ExitCode.MASTER_PASSWORD_SETUP_FAILED);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }// setup()

    /**
     * Sets the Master Password for Bix. Standard Password setting process.
     * @return {@code true} if new Master Password was set successfully.
     */
    private static boolean setMasterPassword(){
        String first_input,second_input; // stores the user's input

        // Get the new Master Password from user.
        first_input = Reader.getString("\n > Enter your new Master Password (1st time) : ");
        second_input = Reader.getString("\n > Enter your new Master Password (2nd time) : ");

        // Clearing the interface.
        clearScreen();

        // Checking that the first and second inputs match.
        if(first_input.equals(second_input)){
            try {MASTER_PASSWORD_HASH = Krypto.getSHA256(first_input); }
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return false;
            }
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
    static void setAESFlavor(AESFlavor flavor){
        AES_flavor = flavor.toInteger();
    }

    private void verifyFileExists() { // method to check if the required csv and hash files exist
        boolean file_located = true; // turns false if one of the files is not found

        File csv_file = new File(IVRY_FILE);
        if (!csv_file.exists()) { // checking if csv file exists
            System.out.println("\nERROR: Failed to locate credentials.csv in filepath.");
            file_located = false;
        }
        URL hash_location = Handler.class.getResource(HASH_FILE);
        if (hash_location == null) {
            System.out.println("\nERROR: Failed to locate mkhash.txt in filepath.");
            file_located = false;
        }
        if (!file_located) { terminateSession(ExitCode.VAULT_FILE_NOT_FOUND); }

    } //verifyFile()

    /**
     * Method to authenticate the user.
     */
    static void authenticateUser(char[] master_password) {
        try {
            // Successful authentication.
            if (Krypto.getSHA256(new String(master_password)).equals(MASTER_PASSWORD_HASH)) { // comparing hash values
                // Clear the screen and display the appropriate message.
                clearScreen();
                System.out.println("\nAuthentication successful.");

                // Copy the input char[] to the class variable MASTER_PASSWORD.
                MASTER_PASSWORD = Arrays.copyOf(master_password, master_password.length);
                auth_success = true; // set the user authentication flag to true.
            }
            // Failed authentication.
            else {
                terminateSession(ExitCode.AUTHENTICATION_FAILED);
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // Clear the input char[] from the memory.
        clearCharArrayFromMemory(master_password);

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
    static void clearCharArrayFromMemory(char[] char_array){
        // Setting every character in the array to null character('\0') using Arrays.fill().
        Arrays.fill(char_array,'\0');
    } // clearCharArrayFromMemory()

    /**
     * Finds account names that contain or match the keyword provided.
     * @param keyword Keyword for finding an account.
     * @return An {@code ArrayList<String>} containing all the Account names that contain the keyword.
     */
    static ArrayList<String> getAccountNamesContaining(String keyword){
        ArrayList<String> account_names_containing_keyword = new ArrayList<>();
        // Converting the keyword to lower case because the .contains() method is case sensitive.
        keyword = keyword.toUpperCase(Locale.ROOT);
        // Looping through the account_names array to find all account names that contain the keyword.
        for(String account : account_names){
            if(account.contains(keyword))
                account_names_containing_keyword.add(account);
        }
        return account_names_containing_keyword;
    }

    /**
     * Loads the account names from the credentials.csv file to memory for quicker access.
     */
    private static void loadAccountNames (){
        try (BufferedReader br = new BufferedReader(new FileReader(IVRY_FILE))) {
            int index = 0;
            String line;
            while ((line = br.readLine()) != null) {
                account_names[index++] = line.substring(0, line.indexOf(',')); // split up values in the line and store in String array

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
    static void printAccountNames (String prompt) {
        // If prompt is null, print all account names.
        if (prompt==null) {
            try (BufferedReader br = new BufferedReader(new FileReader(IVRY_FILE))) {
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

    static boolean accountExists (String account_name){ // returns true if the account exists in credentials.csv
        if (auth_success) {
            account_name = account_name.toUpperCase(); // since the account names are stored in uppercase in csv file
            try (BufferedReader br = new BufferedReader(new FileReader(IVRY_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(","); // split up values in the line and store in String array
                    if (values[0].contains(account_name)) {
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

    static void clearScreen() { // clears terminal and console
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception e) { e.printStackTrace(); }
    } // clearScreen()

    /**
     * Terminates the current Bix session.
     * @param exit_code The appropriate exit code from enum {@code ExitCode}.
     */
    static void terminateSession(ExitCode exit_code) {
        // Reassurance that the Master Password will always be cleared from the memory.
        clearCharArrayFromMemory(MASTER_PASSWORD);

        // Clearing the console.
        clearScreen();
        // Displaying the exit message depending on the exit code.
        System.out.printf("\n%s\nTerminating Bix session.",exit_code.getMessage());
        // Terminating the session.
        System.exit(exit_code.getExitCode());

    } // terminateSession()

    /*-----------------------------------------------------------------------------------------*/

    static void printCredentialsFor(String account_name){
        /* credentials.csv file format:
         *  account_name ,  ciphertext  ,     salt     ,     iv      , secret_key hash
         *    values[0]      values[1]      values[2]     values[3]       values[4]
         */

        String[] values = null; // to store each value of a comma separated value line
        try (BufferedReader br = new BufferedReader(new FileReader(IVRY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                values = line.split(","); // split up values in the line and store in String array

                if (values[0].contains(account_name)) {
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
        try {
            if (!Krypto.generateKeyAndGetHash(new String(MASTER_PASSWORD), salt, AES_flavor).equals(values[4])) { // checking if hash values match
                terminateSession(ExitCode.AUTHENTICATION_FAILED);
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // decrypting ciphertext
        String plaintext = Krypto.decrypt(ciphertext, new String(MASTER_PASSWORD), salt, iv, AES_flavor);

        // displaying credentials
        System.out.println("\nUsername: " + plaintext.substring(0, plaintext.indexOf(" ")));
        System.out.println("Password: " + plaintext.substring(plaintext.indexOf(" ") + 1));
        sleep(); // Sleeps for a preset duration.
        clearScreen(); // Clear credentials from the screen.

    } // printCredentialsFor()

    /*-----------------------------------------------------------------------------------------*/

    static void addAccountLogin() {
        // get account information
        String account_name = getInput("Account Name").toUpperCase();
        String username = getInput("Account Username");
        String password = getInput("Account Password");
        clearScreen(); // clears all sensitive information from the screen

        // verifying master_key
        try {
            if (Krypto.getSHA256(new String(MASTER_PASSWORD)).equals(MASTER_PASSWORD_HASH)) { // comparing hash values
                System.out.println("\nAuthentication successful.");
            } else {
                terminateSession(ExitCode.AUTHENTICATION_FAILED);
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // creating new csv line entry
        String plaintext = username + " " + password;
        String new_csv_entry = account_name + "," + Krypto.encrypt(plaintext, new String(MASTER_PASSWORD), AES_flavor);

        // writing to csv file
        FileWriter csvWriter;
        try {
            csvWriter = new FileWriter(IVRY_FILE, true);
            csvWriter.append(new_csv_entry);
            csvWriter.append("\n");

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) { e.printStackTrace(); }

        // Print success message.
        System.out.println("Securely stored account credentials.");
        //terminateSession(ExitCode.SAFE_TERMINATION);

    } // createAccountLogin()

    private static String getInput (String data_name){
        do {
            // If the input is a password.
            if(data_name.toUpperCase(Locale.ROOT).contains("PASSWORD")){
                System.out.print("\n > Enter " + data_name + " (1st time): ");
                char[] password_1 = console.readPassword();
                System.out.print("\n > Enter " + data_name + " (2nd time): ");
                char[] password_2 = console.readPassword();

                // Checking to see if the user entered the same password both times.
                if(password_1 == password_2)
                    return new String(password_1); // if they match, then return password_1 as a String.
                else // if they do not match.
                    System.out.printf("\nThe %s you entered the 2nd time did not match the 1st one. Please try again.\n", data_name);
            }
            // If not a password.
            else {
                // reading user input
                String user_input = Reader.getString("\n > Enter " + data_name + ": ");

                // Confirming user's input.
                if (Reader.getString("\n *> Confirm this " + data_name + "? [Y]/[n]: "
                                        ).toLowerCase().charAt(0) == 'y')
                    return user_input; // return the value if user confirms.
            }
        } while (true); // endless loop. return statements will take care of exit.

    } // getInput()
} // class
