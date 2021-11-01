package Bix;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
/**
 * This is an object class that handles reading and writing in .csv files
 * This class also encrypts and decrypts data from .csv files
 */
class Handler extends Krypto {

    String CSV_FILE; // stores the path to the location where the csv file is stored
    String HASH_FILE; // stores the path to the location where the master key hash file is stored

    String master_key_hash; // stores the SHA256 hash of the master key
    boolean auth_success; // FALSE by default, turns TRUE if the user authentication is successful

    int AES_ALGORITHM;

    static Scanner sc;

    Handler(int algorithm) {
        // finding credentials.csv location
        try {
            // first line finds the location of .jar file, second line attaches
            // "credentials.csv" string to the parent path of .jar file
            // NOTE: it is assumed that the credentials.csv file is in the same folder as the .jar file
            File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            CSV_FILE = jarFile.getParent() + File.separator + "credentials.csv";
        } catch (Exception e) {e.printStackTrace();}

        HASH_FILE = "/mkhash.txt";
        AES_ALGORITHM = algorithm;
        auth_success = false;
        sc = new Scanner(System.in);
        verifyFileExists();

        // retrieve the SHA256 hash of the master key stored in the credentials.csv file
        try {
            InputStreamReader isReader = new InputStreamReader(this.getClass().getResourceAsStream(HASH_FILE));
            BufferedReader br = new BufferedReader(isReader);
            master_key_hash = br.readLine();
            br.close();
        } catch (Exception e) { e.printStackTrace(); }
    } //constructor

    void verifyFileExists() { // method to check if the required csv and hash files exist
        boolean file_located = true; // turns false if one of the files is not found

        File csv_file = new File(CSV_FILE);
        if (csv_file.exists() == false) { // checking if csv file exists
            System.out.println("\nERROR: Failed to locate credentials.csv in filepath.");
            file_located = false;
        }
        URL hash_location = Handler.class.getResource(HASH_FILE);
        if (hash_location == null) {
            System.out.println("\nERROR: Failed to locate mkhash.txt in filepath.");
            file_located = false;
        }
        if (!file_located) { terminate(); }

    } //verifyFile()

    void authenticate () { // method to authenticate user
        String input_key = getMasterKeyFromUser(false);

        try {
            if (getSHA256(input_key).equals(master_key_hash)) { // comparing hash values
                clearScreen();
                System.out.println("\nAuthentication successful.");
                auth_success = true;
            } else {
                System.out.println("\nAuthentication failed. Incorrect key entered.");
                terminate();
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

    } // authenticate()

    private String getMasterKeyFromUser (boolean newMK){ // gets the master key from the user
        String user_input; // stores the user's input

        // get the master key from user
        if (newMK) { System.out.print("\n > Enter new Master Key: "); }
        else { System.out.print("\n > Enter Master Key: "); } // if setting a new master key
        user_input = sc.nextLine().trim(); // reading user input

        clearScreen(); // clears all sensitive information from the screen
        return user_input;

    } // getMasterKeyFromUser()

    void printAccountNamesList () { // prints out a list of names of all accounts stored in credentials.csv
        if (auth_success) {
            try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
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

    boolean accountExists (String account_name){ // returns true if the account exists in credentials.csv
        if (auth_success) {
            account_name = account_name.toUpperCase(); // since the account names are stored in uppercase in csv file
            try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
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

    private static void sleep (int seconds){ // pauses code execution
        try { Thread.sleep(seconds * 1000); }
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

    public void closeScanner () {
        sc.close(); // closing scanner
    } // closeScanner()

    private void terminate () { // method to terminate the program
        System.out.println("\nTerminating Bix session.");
        closeScanner(); // closing scanner
        System.exit(0); // terminating program
    } // terminate()

    /*-----------------------------------------------------------------------------------------*/

    void getCredentialsFor (String account_name){
        /** credentials.csv file format:
         *  account_name ,  ciphertext  ,     salt     ,     iv      , secret_key hash
         *    values[0]      values[1]      values[2]     values[3]       values[4]
         */

        String[] values = null; // to store each value of a comma separated value line
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                values = line.split(","); // split up values in the line and store in String array

                if (values[0].contains(account_name)) {
                    break;
                } // checking if account is found
            } // while
        } // try
        catch (IOException e) { e.printStackTrace(); }

        // getting master key from user
        String master_key = getMasterKeyFromUser(false);

        assert values != null;
        String ciphertext = values[1];
        String salt = values[2];
        String iv = values[3];

        // comparing hash values of user entered password and hash stored in in csv file
        try {
            if (!generateKeyAndGetHash(master_key, salt, AES_ALGORITHM).equals(values[4])) { // checking if hash values match
                System.out.println("\nERROR: Password entered is incorrect.");
                terminate(); // terminating Bix session
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // decrypting ciphertext
        String plaintext = decrypt(ciphertext, master_key, salt, iv, AES_ALGORITHM);

        // displaying credentials
        System.out.println("\nUsername: " + plaintext.substring(0, plaintext.indexOf(" ")));
        System.out.println("Password: " + plaintext.substring(plaintext.indexOf(" ") + 1));
        sleep(7); // sleep 7 seconds
        clearScreen(); // clear credentials from the screen

    } // getLoginCredentialsFor()

    /*-----------------------------------------------------------------------------------------*/

    void createAccountLogin () {
        // get account information
        String account_name = getInput("account name").toUpperCase();
        String username = getInput("account username");
        String password = getInput("account password");
        clearScreen(); // clears all sensitive information from the screen

        // getting master_key from user
        String input_key = getMasterKeyFromUser(false);

        // verifying master_key
        try {
            if (getSHA256(input_key).equals(master_key_hash)) { // comparing hash values
                System.out.println("\nAuthentication successful.");
            } else {
                System.out.println("\nERROR: Authentication failed. Incorrect key entered");
                terminate();
            }
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // creating new csv line entry
        String plaintext = username + " " + password;
        String new_csv_entry = account_name + "," + encrypt(plaintext, input_key, AES_ALGORITHM);

        // writing to csv file
        FileWriter csvWriter;
        try {
            csvWriter = new FileWriter(CSV_FILE, true);
            csvWriter.append(new_csv_entry);
            csvWriter.append("\n");

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) { e.printStackTrace(); }

        // print success message and terminate
        System.out.println("Securely stored account credentials.");
        terminate();

    } // createAccountLogin()

    private String getInput (String data_name){
        String user_input; // stores the user's input
        boolean confirm = false; // while loop control variable

        // get the data from user
        do {
            System.out.print("\n > Enter " + data_name + ": ");
            user_input = sc.nextLine().trim(); // reading user input

            System.out.print("\n *> Confirm this " + data_name + "? [Y]/[n]: ");
            if (sc.nextLine().trim().toLowerCase().charAt(0) == 'y') {
                confirm = true;
            }
        } while (!confirm);

        return user_input;

    } // getData()
} // class
