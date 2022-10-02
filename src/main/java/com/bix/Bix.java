package com.bix;

import java.io.Console;
import java.util.ArrayList;
import java.util.Locale;

import com.bix.enums.StatusCode;
import com.bix.utils.Handler;

import static com.bix.utils.Handler.*;
import static com.bix.utils.Reader.*;

public class Bix {
    // Creating console object.
    private static final Console CONSOLE = System.console();

    public static void main(String[] args) {
        // Adding a JVM shutdown hook. This thread will be executed when the JVM is shutting down.
        // This Shutdown Hook is for clearing the Master Password from memory when the session is terminated.
        // Carrying out shutdown procedure: clears sensitive information from the terminal and memory.
        Runtime.getRuntime().addShutdownHook(new Thread(Handler::executeShutdownProcedure));

        // Running the Bix setup.
        setup();

        // Greet user.
        clearScreen();
        System.out.printf("""
                           Hello, %s!
                           This is Bix, your Password Manager.
                           """, getUsernameFromSystem()); // getUsernameFromSystem() gets the name of the current user.

        // Authenticate User.
        char[] master_password = getMasterPasswordFromUser(); // get the master password securely.
        authenticateUser(master_password);

        // Bix Menu loop.
        char user_menu_choice;
        do {
            // Printing menu options.
            System.out.print("""
                    Bix Menu:

                    \t[1] Find an Account

                    \t[2] Add a new Account

                    \t[3] Manage Saved Accounts (Edit/Delete)

                    \t[4] Reset Master Password

                    \t[5] Import/Export a CSV vault file
                    
                    \t[6] Bix Settings
                    
                    \t[7] Open GitHub Page

                    \t[X] Exit Bix
                    
                    """);

            // Reading user's menu choice.
            user_menu_choice = readChar("> Enter Menu option: ");

            // Evaluating based on the menu option entered by the user.
            switch (user_menu_choice) {

                // retrieve login information for an account.
                case '1':
                    System.out.println("\nRetrieve Account Login Credentials");

                    // Boolean to indicate if the account has been found.
                    boolean account_retrieved = false;

                    // ArrayList that stores all the accounts that contain the keyword entered by the user.
                    ArrayList<String> search_results;

                    // Displaying all stored account names.
                    printAccountNames(null);

                    // Loop will keep running till an account is found.
                    do{
                        String keyword = readString("\n > Enter Account Name: ").toUpperCase(Locale.ROOT);
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

                            // Two or more account names contain the keyword, display them and ask the user to choose one.
                            default:
                                try{
                                    // Printing the account names along with an index number.
                                    for(int index = 0 ; index < search_results.size() ; index++){
                                        System.out.printf("[%d] %s \n", index, search_results.get(index));
                                    }
                                    // Asking the user to choose one of the displayed Accounts.
                                    int user_choice = readInt(
                                                    "\nChoose an Account to view (enter the number inside [ ]): ");

                                    // Printing the credentials.
                                    printCredentialsFor(search_results.get(user_choice));
                                    account_retrieved = true;
                                }
                                // If the user enters an invalid choice.
                                catch(Exception e){
                                    clearScreen();
                                    System.out.println("The option you entered is invalid. Try again.");
                                }
                                break;
                        } // switch
                    }while(!account_retrieved);
                    break;

                // create a new account login entry
                case '2':
                    addAccountLogin();
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
        terminateSession(StatusCode.SAFE_TERMINATION);

    } // main()

    /**
     * Retrieves the username of the current user from the system environment.
     * @return User's name if one is found, otherwise, returns "User".
     */
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
    }// getUsernameFromSystem()

    /**
     * Gets the Master Password from the user in a secure manner.
     * Precautions are taken to prevent the Master Password from being leaked.
     */
    private static char[] getMasterPasswordFromUser(){
        System.out.print("\n > Enter Master Password: ");
        return CONSOLE.readPassword(); // Getting the password from user.
    } // getMasterPasswordFromUser()

} // class Bix
