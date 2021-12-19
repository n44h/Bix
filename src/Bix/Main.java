package Bix;

import java.util.Scanner;

class Main{
    private static final Scanner SCANNER = new Scanner(System.in); // creating scanner object

    public static void main(String[] args){
        // Running the initial Bix setup if necessary.
        Handler.setup();

        // Authenticate User.
        Handler.authenticateUser();

        // Main Menu loop.
        char user_menu_choice;
        do {
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

                     > Enter Menu option:\s""", getUsernameFromSystem()); // getUsernameFromSystem() gets the name of the current user.

            // Reading user's menu choice.
            user_menu_choice = SCANNER.nextLine().trim().toUpperCase().charAt(0);

            // Evaluating based on the menu option entered by the user.
            switch (user_menu_choice) {
                case '1': // retrieve login information for an account.
                    Handler.retrieveAccountLogin();
                    break;

                case '2': // create a new account login entry
                    Handler.addAccountLogin();
                    break;

                case '3':
                    break;

                case '4':
                    break;

                case '5':
                    break;

                case '6':
                    break;

                default:
                    System.out.println("Invalid menu option entered. Try again.");
            } // switch

        }while(user_menu_choice != 'X');

        // Terminating the Bix session.
        terminateSession();

    } // main()

    private static void clearScreen(){ // clears terminal and console
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception e) {e.printStackTrace();}
    } // clearScreen()

    private static String getUsernameFromSystem(){
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
        System.out.println("\nTerminating Bix session...");
        SCANNER.close(); // closing scanner
        System.exit(ExitCode.SAFE_TERMINATION.getExitCode()); // terminating program

    } // terminate()

} // class Main
