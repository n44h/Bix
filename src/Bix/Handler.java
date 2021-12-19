package Bix;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

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
    private static AES_FLAVOR AES_flavor; // can be initialized to 128-bit, 192-bit or 256-bit.
    private static int vault_size;
    private static String[] account_names; // stores the stored account names.
    private static Console console; // System console reference.
    private static char[] master_password; // global char[] to store and access Master Password; must be cleared after use.
    private static int CREDENTIAL_DISPLAY_TIMEOUT; // Session timeout in seconds.
    private static final Scanner SCANNER = new Scanner(System.in);

    Handler() {
        // Creating a Properties object to parse the config.properties file.
        bix_properties = new Properties();
        try {
            bix_properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.out.println("ERROR: Missing config.properties file.");
            e.printStackTrace();
            System.exit(EXIT_CODES.MISSING_CONFIG_FILE.getExitCode());
        }
        /*
        // finding credentials.csv location
        try {
            // first line finds the location of .jar file, second line attaches
            // "credentials.csv" string to the parent path of .jar file
            // NOTE: it is assumed that the credentials.csv file is in the same folder as the .jar file
            File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            IVRY_FILE = jarFile.getParent() + File.separator + "credentials.csv";
        } catch (Exception e) {e.printStackTrace();}
                */
        HASH_FILE = "/mkhash.txt";
        AES_flavor = AES_FLAVOR.AES_128; // default AES flavor is 128 bit.
        auth_success = false;
        vault_size = Integer.parseInt(bix_properties.getProperty("vault_size"));
        account_names = new String[vault_size];
        console = System.console(); // attaching variable to the system console.
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
        // If this is the first time opening Bix, do the initial setup.
        if(Boolean.parseBoolean(bix_properties.getProperty("initial_setup_required"))){
            if(setMasterPassword()) {
                /* If initial setup was successful, set the "initial_setup_property" as false
                   to prevent setup procedure from redundantly running again in the future. */
                bix_properties.setProperty("initial_setup_required", "false");
            }
            else {
                System.out.println("\nBix Setup Failed.\n");
                terminateSession(EXIT_CODES.MISSING_CONFIG_FILE);
            }
        }
    }// setup()

    /**
     * Sets the Master Password for Bix. Standard Password setting process.
     * @return {@code true} if new Master Password was set successfully.
     */
    private static boolean setMasterPassword(){
        String first_input,second_input; // stores the user's input

        // Get the new Master Password from user.
        System.out.print("\n > Enter your new Master Password (1st time) : ");
        first_input = SCANNER.next().trim();
        System.out.print("\n > Enter your new Master Password (2nd time) : ");
        second_input = SCANNER.next().trim();

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
    static void setAESFlavor(AES_FLAVOR flavor){
        AES_flavor = flavor;
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
        if (!file_located) { terminateSession(EXIT_CODES.MISSING_VAULT); }

    } //verifyFile()

    /**
     * Method to authenticate the user.
     */
    static void authenticateUser() {
        // First, get the master password from the user.
        getMasterPasswordFromUser();

        try {
            // Successful authentication.
            if (Krypto.getSHA256(new String(master_password)).equals(MASTER_PASSWORD_HASH)) { // comparing hash values
                clearScreen();
                System.out.println("\nAuthentication successful.");
                auth_success = true;
            }
            // Failed authentication.
            else {
                System.out.println("\nAuthentication failed. Incorrect Master Password.");
                terminateSession(EXIT_CODES.INCORRECT_MASTER_PASSWORD);
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        /* NOTE:
         * Don't clear the Master Password from the memory after user authentication because
         * you need it for encrypting and decrypting the vault and its contents.
         *
         * The Master Password will automatically get cleared from the memory by the
         * terminateSession() method.
         */
    } // authenticateUser()

    /**
     * Gets the Master Password from the user in a secure manner.
     * Precautions are taken to prevent the Master Password from being leaked.
     */
    private static void getMasterPasswordFromUser(){
        System.out.print("\n > Enter Master Password: ");
        master_password = console.readPassword(); // Getting the password from user.
    } // getMasterPasswordFromUser()

    /**
     * Clears the Master Password from memory.
     */
    private static void clearMasterPassword(){
        // loop to set every character in master_password to null character('\0').
        for(char ch : master_password)
            ch = '\0'; // setting each character to null character.
    } // clearMasterPassword()

    /**
     * Retrieves Account login credentials requested by the user.
     */
    static void retrieveAccountLogin(){
        System.out.println("\nRetrieve Account Login Credentials");

        // Boolean to indicate if the account has been found.
        boolean account_retrieved = false;

        // ArrayList that stores all the accounts that contain the keyword entered by the user.
        ArrayList<String> search_results;

        // Loop will keep running till an account is found.
        do{
            System.out.print("\n > Enter Account Name: ");
            String keyword = SCANNER.nextLine().trim().toUpperCase(Locale.ROOT);
            // Finding all account names containing the keyword.
            search_results = getAccountNamesContaining(keyword);

            switch(search_results.size()) {
                // No account name contains the keyword.
                case 0:
                    System.out.printf("\nBix could not find an Account Name containing \"%s\".\n", keyword);
                    break;

                // Only one account name contains the keyword, retrieve the information for that account.
                case 1:
                    printCredentialsFor(search_results.get(0));
                    account_retrieved = true;
                    break;

                // Two or more account names contain the keyword, display them and ask the user choose one.
                default:
                    try{
                        for(int index = 0 ; index < search_results.size() ; index++){
                            // Printing the account names along with an index number.
                            System.out.printf("[%d] %s \n", index, search_results.get(index));
                        }
                        System.out.print("Choose an Account to view (enter the number inside [ ]): ");
                        printCredentialsFor(search_results.get(Integer.parseInt(SCANNER.next().trim())));
                    }
                    catch(Exception e){
                        clearScreen();
                        System.out.println("The option you entered is invalid. Try again.");
                    }
                    break;
            } // switch
        }while(!account_retrieved);
    } // retrieveAccountLogin()

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
    static void printAccountNames (String prompt) { // prints out a list of names of all accounts stored in credentials.csv
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
        } // auth_success
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

    private static void clearScreen () { // clears terminal and console
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception e) { e.printStackTrace(); }
    } // clearScreen()

    /**
     * Terminates the current Bix session.
     * @param exit_code The appropriate exit code from enum {@code EXIT_CODES}.
     */
    private static void terminateSession(EXIT_CODES exit_code) {
        // Reassurance that the Master Password will always be cleared.
        clearMasterPassword();
        // Closing the Scanner stream.
        SCANNER.close();

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
            if (!Krypto.generateKeyAndGetHash(new String(master_password), salt, AES_flavor.toInteger()).equals(values[4])) { // checking if hash values match
                terminateSession(EXIT_CODES.INCORRECT_MASTER_PASSWORD);
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // decrypting ciphertext
        String plaintext = Krypto.decrypt(ciphertext, new String(master_password), salt, iv, AES_flavor.toInteger());

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
            if (Krypto.getSHA256(new String(master_password)).equals(MASTER_PASSWORD_HASH)) { // comparing hash values
                System.out.println("\nAuthentication successful.");
            } else {
                terminateSession(EXIT_CODES.INCORRECT_MASTER_PASSWORD);
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // creating new csv line entry
        String plaintext = username + " " + password;
        String new_csv_entry = account_name + "," + Krypto.encrypt(plaintext, new String(master_password), AES_flavor.toInteger());

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
        //terminateSession(EXIT_CODES.SAFE_TERMINATION);

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
                System.out.print("\n > Enter " + data_name + ": ");
                String user_input = SCANNER.nextLine().trim();

                // Confirming user's input.
                System.out.print("\n *> Confirm this " + data_name + "? [Y]/[n]: ");
                if (SCANNER.nextLine().trim().toLowerCase().charAt(0) == 'y')
                    return user_input; // return the value if user confirms.
            }
        } while (true); // endless loop. return statements will take care of exit.

    } // getInput()
} // class
