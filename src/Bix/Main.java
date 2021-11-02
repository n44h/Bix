package Bix;

import java.util.Scanner;

class Main{
    private static final Scanner input_scanner = new Scanner(System.in); // creating scanner object

    public static void main(String[] args){
        // Creating a Handler object. Handlers take care of background processes.
        Handler handler = new Handler();

        // Asking the Handler to run the initial Bix setup if necessary.
        handler.setup();

        // Authenticate User.
        handler.authenticate();

        // Clear the Interface.
        clearScreen();

        // Main Menu loop.
        while(true) {
            // Getting menu option from the user.
            System.out.printf("""

                     Hello, %s!
                     This is Bix, your Account Manager!

                     Choose a menu option:

                    \t[1] Find an Account

                    \t[2] Add a new Account

                    \t[3] Manage Saved Accounts (Edit/Delete)

                    \t[4] Reset Master Password

                    \t[5] Import Accounts from CSV file

                    \t[6] Export Accounts as CSV file

                    \t[X] Exit Bix

                     > Enter Menu option:\s""", getUsername());

            // Reading user's menu choice.
            char user_choice = input_scanner.nextLine().trim().toUpperCase().charAt(0);

            // Evaluating based on the menu option entered by the user.
            switch (user_choice) {
                case '1': // retrieve an account login credentials
                    String account_name; // declaring relevant to account info

                    System.out.println("\nRetrieve Account Login Credential");
                    handler.printAccountNamesList(); // printing list of all account names

                    System.out.print("\n > Enter Account name (partial name is also accepted): ");
                    account_name = input_scanner.nextLine().trim().toUpperCase(); // reading account name

                    if (handler.accountExists(account_name)) { //checking if account exists
                        handler.getCredentialsFor(account_name); // get credentials for the account
                    }
                    break;

                case '2': // create a new account login entry
                    handler.createAccountLogin();
                    break;

                case '3':
                    break;

                case '4':
                    break;

                case '5':
                    break;

                case '6':
                    break;

                case 'X':
                    terminateSession();
                    break;

                default:
                    System.out.println("Invalid menu option entered. Try again.");
            } // switch

        } // while

    } // main()

    private static void clearScreen(){ // clears terminal and console
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception e) {e.printStackTrace();}
    } // clearScreen()

    private static String getUsername(){
        String username;
        try {
            if (System.getProperty("os.name").contains("Windows"))
                username = System.getenv("USERNAME");
            else
                username = System.getenv("USER");

            // Capitalizing the first letter of the username and returning.
            return Character.toUpperCase(username.charAt(0)) + username.substring(1);
        } catch (Exception e) {return "User";}
    }// getUsername()

    private static void terminateSession(){ // method to terminate the program
        System.out.println("\nTerminating Bix session.");
        input_scanner.close(); // closing scanner
        System.exit(0); // terminating program

    } // terminate()

} // class Main
