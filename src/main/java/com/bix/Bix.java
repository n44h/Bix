package com.bix;

import java.io.Console;
import java.util.ArrayList;
import java.util.Locale;

import com.bix.enums.StatusCode;
import com.bix.utils.Controller;

import static com.bix.utils.Reader.*;
import static com.bix.utils.Controller.*;


public final class Bix {
    // Creating console object.
    private static final Console CONSOLE = System.console();

    // Main menu options String.
    private static final String MAIN_MENU_OPTIONS = """
                    Bix Main Menu:

                    \t[1] Retrieve Account

                    \t[2] Add Account

                    \t[3] Update Account
                    
                    \t[4] Delete Account

                    \t[M] More Options

                    \t[X] Exit Bix
                    
                    """;

    // Extended menu options String.
    private static final String EXT_MENU_OPTIONS = """
                    Bix Extended Menu:
                    
                    \t[5] Reset Master Password
                    
                    \t[6] Import Vault
                    
                    \t[7] Export Vault
                    
                    \t[8] Purge Vault
                    
                    \t[9] Open Bix GitHub Page
                    
                    \t[X] Exit Bix
                    
                    """;

    // Variable to indicate whether to print the main menu or extended menu.
    private static boolean printMainMenu = true;

    public static void main(String[] args) {
        // Adding a JVM shutdown hook. This thread will be executed when the JVM is shutting down.
        // This Shutdown Hook is for clearing the Master Password from memory when the session is terminated.
        // Carrying out shutdown procedure: clears sensitive information from the terminal and memory.
        Runtime.getRuntime().addShutdownHook(new Thread(Controller::executeShutdownProcedure));

        // Running the Bix setup.
        setup();

        // Greet user.
        clearScreen();
        System.out.printf("""
                           Hello, %s!
                           This is Bix, your Password Manager.
                           """, getUsernameFromSystem());

        // Authenticate User.
        char[] masterPassword = getMasterPasswordFromUser(); // get the master password securely.
        authenticateUser(masterPassword);

        // Bix Menu loop.
        char userMenuChoice;
        do {
            if (printMainMenu)
                // Printing main menu options.
                System.out.print(MAIN_MENU_OPTIONS);
            else {
                // Printing extended menu options.
                System.out.print(EXT_MENU_OPTIONS);

                // Set the printMainMenu to true again.
                printMainMenu = true;
            }

            // Reading user's menu choice.
            userMenuChoice = readChar("> Enter Menu option: ");

            // Evaluating based on the menu option entered by the user.
            switch (userMenuChoice) {

                // Retrieve Account.
                case '1':
                    System.out.println("\nRetrieve Account");

                    // Boolean to indicate if the account has been found.
                    boolean retrievedAccount = false;

                    // ArrayList that stores all the accounts that contain the keyword entered by the user.
                    ArrayList<String> searchResults;

                    // Displaying all stored account names.
                    printAccountNames(null);

                    // Loop will keep running till an account is found.
                    do{
                        String keyword = readString("\n > Enter Account Name: ").toUpperCase(Locale.ROOT);
                        // Finding all account names containing the keyword.
                        searchResults = getAccountNamesContaining(keyword);

                        switch(searchResults.size()) {
                            // No account name contains the keyword.
                            case 0:
                                System.out.printf("\nBix could not find an Account Name containing \"%s\".\n", keyword);
                                break;

                            // Only one account name contains the keyword, retrieve the information for that account.
                            case 1:
                                printCredentialsFor(searchResults.get(0));
                                retrievedAccount = true;
                                break;

                            // Two or more account names contain the keyword, display them and ask the user to choose one.
                            default:
                                try{
                                    // Printing the account names along with an index number.
                                    for(int index = 0 ; index < searchResults.size() ; index++){
                                        System.out.printf("[%d] %s \n", index, searchResults.get(index));
                                    }
                                    // Asking the user to choose one of the displayed Accounts.
                                    int userChoice = readInt(
                                                    "\nChoose an Account to view (enter the number inside [ ]): ");

                                    // Printing the credentials.
                                    printCredentialsFor(searchResults.get(userChoice));
                                    retrievedAccount = true;
                                }
                                // If the user enters an invalid choice.
                                catch(Exception e){
                                    clearScreen();
                                    System.out.println("The option you entered is invalid. Try again.");
                                }
                                break;
                        } // switch
                    }while(!retrievedAccount);
                    break;

                // Add Account.
                case '2':
                    addAccountLogin();
                    break;

                // Update Account.
                case '3':
                    break;

                // Delete Account.
                case '4':
                    break;

                // Reset Master Password.
                case '5':
                    break;

                // Import Vault.
                case '6':
                    break;

                // Export Vault.
                case '7':
                    break;

                // Purge Vault.
                case '8':
                    break;

                // Open GitHub page.
                case '9':
                    // Open the Bix Repository GitHub page in the default browser.
                    openGitHubPage();

                    // Terminate the session.
                    terminateSession(StatusCode.SAFE_TERMINATION);
                    break;

                // View extended menu options.
                case 'M':
                    // Setting to false, so the extended menu is printed in the next iteration of the loop.
                    printMainMenu = false;
                    break;

                default:
                    System.out.println("Invalid menu option entered. Try again.");
            } // switch

        } while(userMenuChoice != 'X');

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
