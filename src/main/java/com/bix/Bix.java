package com.bix;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import com.bix.enums.StatusCode;

import static com.bix.utils.Reader.*;
import static com.bix.Controller.*;
import static com.bix.utils.Utils.clearScreen;


public final class Bix {
    // Config file name.
    private static final String CONFIG_FILE = "config.properties";

    // Main menu options String.
    private static final String MAIN_MENU_OPTIONS = """
            Bix Main Menu:
            
            \t[0] Display Stored Accounts

            \t[1] Retrieve Account

            \t[2] Add Account

            \t[3] Update Account
            
            \t[4] Delete Account

            \t[M] More Options
            
            \t[H] Help

            \t[X] Exit Bix
            
            """;

    // Extended menu options String.
    private static final String EXT_MENU_OPTIONS = """
            Bix Extended Menu:
            
            \t[5] Reset Master Password
            
            \t[6] Change Credential Display Duration
            
            \t[7] Change Idle Timeout Duration
            
            \t[8] Import Vault
            
            \t[9] Export Vault
            
            \t[G] Open Bix GitHub Page
            
            +---------------------+
            |   DANGER ZONE:      |
            |---------------------|
            |   [P] Purge Vault   |
            |                     |
            |   [R] Reset Bix     |
            +---------------------+
            
            \t[B] Back to Main Menu
            
            \t[X] Exit Bix
            
            """;

    private static final String HELP_STRING = """
            Bix Help
            
            - Account Actions:
            
            \t[0] Display Stored Accounts - Prints the names of all the stored accounts
            
            \t[1] Retrieve Account - Retrieve a stored account's credentials
            
            \t[2] Add Account - Add a new account

            \t[3] Update Account - Update an existing account's credentials
            
            \t[4] Delete Account - Delete a stored account
            
            
            - Bix Settings:
            
            \t[5] Reset Master Password - Reset the Bix master password
                  
            \t[6] Change Credential Display Duration - Change the duration the credentials are displayed on the screen
            
            \t[7] Change Idle Timeout Duration - Change how long Bix can stay idle before terminating the session
            
            
            - Vault Actions:
            
            \t[8] Import Vault - Import a Bix vault
            
            \t[9] Export Vault - Export the Bix vault
            
            \t[P] Purge Vault - Destroy the contents of the Bix vault. Use this option if you no longer intend to use Bix
            
            
            - Reset Bix:
            
            \t[R] Reset Bix - This action will purge the Bix vault and remove the master password.
            
            
            - GitHub Page:
            
            \t[G] Open Bix GitHub Page - Open the GitHub page for Bix in the default browser
            
            """;

    // Variable to indicate whether to print the main menu or extended menu.
    private static boolean printMainMenu = true;

    public static void main(String[] args) {
        // Adding a JVM shutdown hook. This thread will be executed when the JVM is shutting down.
        // This Shutdown Hook is for clearing the Master Password from memory when the session is terminated.
        // Carrying out shutdown procedure: clears sensitive information from the terminal and memory.
        Runtime.getRuntime().addShutdownHook(new Thread(Controller::incinerate));

        // Perform Bix setup if this is the first time running Bix.
        if (!isBixSetupComplete()) {
            setup();
        }

        // Greet user.
        clearScreen();
        System.out.printf("""
                           Hello, %s!
                           This is Bix, your Account Manager.
                           """, getUsernameFromSystem());

        // Authenticate User.
        authenticateUser();

        // Bix Menu loop.
        String userMenuChoice;

        do {
            if (printMainMenu)
                // Printing main menu options.
                System.out.print(MAIN_MENU_OPTIONS);
            else {
                // Printing extended menu options.
                System.out.print(EXT_MENU_OPTIONS);
            }

            // Reading user's menu choice.
            userMenuChoice = readString("> Enter Menu option: ").toUpperCase(Locale.ROOT);

            // Evaluating based on the menu option entered by the user.
            switch (userMenuChoice) {

                // Display all saved accounts.
                case "0":
                    System.out.println("\nStored Accounts: ");

                    // Print all the account names.
                    printAccountNames();
                    break;

                // Retrieve Account.
                case "1":
                    System.out.println("\nRetrieve Account");

                    // Boolean to indicate if the account has been found.
                    var foundAccount = false;

                    // ArrayList that stores all the accounts that contain the keyword entered by the user.
                    ArrayList<String> searchResults;

                    // Loop will keep running till an account is found.
                    String keyword;
                    do{
                        clearScreen();

                        // Get keyword from user.
                        keyword = readString("> Enter Account Name: ").toUpperCase(Locale.ROOT);

                        // Finding all account names containing the keyword.
                        searchResults = getAccountNamesContaining(keyword);

                        switch (searchResults.size()) {
                            // No account name contains the keyword.
                            case 0 ->
                                    System.out.printf(
                                            "\nBix could not find an Account Name containing \"%s\".\n", keyword);

                            // Only one account name contains the keyword, retrieve the information for that account.
                            case 1 -> {
                                printCredentials(searchResults.get(0));
                                foundAccount = true;
                            }

                            // Two or more account names contain the keyword, ask the user to choose one.
                            default -> {
                                try {
                                    // Printing the account names along with an index number.
                                    for (var index = 0; index < searchResults.size(); index++) {
                                        System.out.printf("[%d] %s \n", index, searchResults.get(index));
                                    }

                                    // Asking the user to choose one of the displayed accounts.
                                    var userChoice = readInt(
                                            "\nChoose an Account to view (enter the number in [ ]): ");

                                    // Printing the credentials.
                                    printCredentials(searchResults.get(userChoice));
                                    foundAccount = true;
                                }
                                // If the user enters an invalid choice.
                                catch (Exception e) {
                                    clearScreen();
                                    System.out.println("The option you entered is invalid. Try again.");
                                }
                            }
                        } // switch
                    } while (!foundAccount);
                    break;

                // Add Account.
                case "2":
                    System.out.println();
                    break;

                // Update Account.
                case "3":
                    break;

                // Delete Account.
                case "4":
                    break;

                // Reset Master Password.
                case "5":
                    break;

                // Change Credential Display Duration.
                case "6":
                    break;

                // Change Idle Session Timeout.
                case "7":
                    break;

                // Import Vault.
                case "8":
                    break;

                // Export Vault.
                case "9":
                    break;

                // Open GitHub page.
                case "G":
                    // Open the Bix Repository GitHub page in the default browser.
                    openGitHubPage();

                    // Terminate the session.
                    terminateSession(StatusCode.SAFE_TERMINATION);
                    break;

                case "P":
                    System.out.println("""
                                WARNING: All the saved accounts from the Bix vault will be permanently deleted.
                                         This action is irreversible.
                                         """);

                    // Call Controller.purgeVault().
                    purgeVault();
                    break;

                // Reset Bix to initial state.
                case "R":
                    System.out.println("""
                                WARNING: All the saved accounts from the Bix vault will be permanently deleted.
                                         The Bix master password will be removed.
                                         This action is irreversible.
                                         """);

                    resetBix();
                    break;

                // Show extended menu options.
                case "M":
                    // Setting to false, so the extended menu is printed in the next iteration of the loop.
                    printMainMenu = false;
                    break;

                // Go back to main menu.
                case "B":
                    // Setting to false, so the extended menu is printed in the next iteration of the loop.
                    printMainMenu = true;
                    break;

                case "H":
                    // Print the help string.
                    System.out.print(HELP_STRING);
                    break;

                default:
                    System.out.println("Invalid menu option entered. Try again.");
            } // switch

            clearScreen();

        } while(!userMenuChoice.equals("X"));

        // Terminating the Bix session.
        terminateSession(StatusCode.SAFE_TERMINATION);
    }

    /**
     * Retrieves the username of the current user from the system environment.
     * @return User's name if one is found, otherwise, returns "User".
     */
    private static String getUsernameFromSystem() {
        String username;
        try {
            if (System.getProperty("os.name").contains("Windows"))
                username = System.getenv("USERNAME");
            else
                username = System.getenv("USER");

            // Capitalizing the first letter of the username and returning.
            return Character.toUpperCase(username.charAt(0)) +
                    (username.length() > 1 ? username.substring(1) : "");
        } catch (Exception e) { return "User"; } // return "User" if username could not be retrieved.
    }

    /**
     * Opens the GitHub page for Bix in the default browser.
     */
    private static void openGitHubPage() {
        // Create an input stream from config file (config file is in the resource folder).
        var configFile = Bix.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

        try {
            // Loading the properties from the config.properties file.
            var bix_properties = new Properties();
            bix_properties.load(configFile);

            // Get the URL for the Bix GitHub page.
            var github_page = new URI(bix_properties.getProperty("github_url"));

            // Open the URL.
            var desktop = Desktop.getDesktop();
            desktop.browse(github_page);
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
    }

} // class Bix
