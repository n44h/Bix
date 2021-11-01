package Bix;

import java.util.Scanner;

class Main{
    static int AES_algorithm = 128; // can pass AES-128, AES-192 or AES-256

    static Scanner sc = new Scanner(System.in); // creating scanner object

    public static void main(String[] args){
        clearScreen();

        /* Create a Handler Object to read and write in credentials.csv */
        Handler handler = new Handler(AES_algorithm);

        /* Getting menu option from the user */
        System.out.print("\n Hello! This is Bix" +
                "\n\n Choose a menu option:" +
                "\n\n\t[1] Retrieve an account login credentials" +
                "\n\n\t[2] Store a new account login credential" +
                "\n\n\t[X] Exit Bix" +
                "\n\n > Enter Menu option: ");

        char user_choice = sc.nextLine().trim().toUpperCase().charAt(0); //reading user's menu choice

        /* Checking if user wishes to terminate the program */
        if(user_choice != '1' && user_choice != '2'){
            terminate();
        }

        /* Authenticate User */
        handler.authenticate();

        /* Evaluating based on the menu option entered by the user */
        switch(user_choice){

            case '1': // retrieve an account login credentials
                String account_name; // declaring relevant to account info

                System.out.println("\nRetrieve Account Login Credential");
                handler.printAccountNamesList(); // printing list of all account names

                System.out.print("\n > Enter Account name (partial name is also accepted): ");
                account_name = sc.nextLine().trim().toUpperCase(); // reading account name

                if(handler.accountExists(account_name)){ //checking if account exists
                    handler.getCredentialsFor(account_name); // get credentials for the account
                }
                break;

            /*--------------------------------------------------------------------------------*/

            case '2': // create a new account login entry
                handler.createAccountLogin();
                break;

        } //switch case

        // terminating program
        handler.closeScanner();
        terminate();

    } // main()

    private static void clearScreen(){ // clears terminal and console
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception e) {e.printStackTrace();}
    } // clearScreen()

    private static void terminate(){ // method to terminate the program
        System.out.println("\nTerminating Bix session.");
        sc.close(); // closing scanner
        System.exit(0); // terminating program

    } // terminate()

} // class Main
