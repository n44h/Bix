package bix;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

import bix.enums.StatusCode;

import static bix.Controller.*;
import static bix.utils.Reader.*;
import static bix.utils.Utils.clearScreen;

import static bix.utils.Constants.MAIN_MENU_OPTIONS;
import static bix.utils.Constants.EXT_MENU_OPTIONS;
import static bix.utils.Constants.HELP_STRING;
import static bix.utils.Constants.BIX_GITHUB_URL;


public final class Bix {
    // Variable to indicate whether to print the main menu or extended menu.
    private static boolean printMainMenu = true;

    public static void main(String[] args) {
        // Adding a JVM shutdown hook. This thread will be executed when the JVM is shutting down.
        // This Shutdown Hook is for clearing the Master Password from memory when the session is terminated.
        // Carrying out shutdown procedure: clears sensitive information from the terminal and memory.
        Runtime.getRuntime().addShutdownHook(new Thread(Controller::incinerate));

        // Set up Bix: loads variables, and initializes
        setup();

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
            // Print menu options.
            System.out.print(printMainMenu ? MAIN_MENU_OPTIONS : EXT_MENU_OPTIONS);

            // Reading user's menu choice.
            userMenuChoice = readString("> Enter Menu option: ").toUpperCase(Locale.ROOT);

            clearScreen();

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
                                            "Choose an Account to view (enter the number in [ ]): ");

                                    // Printing the credentials.
                                    printCredentials(searchResults.get(userChoice));
                                    foundAccount = true;
                                }
                                // If the user enters an invalid choice.
                                catch (Exception e) {
                                    clearScreen();
                                    System.out.println("The option you entered is invalid. Try again.");
                                } // try catch

                            } // default

                        } // switch

                    } while (!foundAccount);
                    break;

                // Add Account.
                case "2":
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
                    // Get new idle session timeout duration from user.
                    var newTimeout = readInt("Enter new idle session timeout duration in seconds: ");
                    newTimeout = setIdleTimeout(newTimeout);
                    System.out.println("");
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

                // Purge vault.
                case "P":
                    purgeVault(); // Controller.purgeVault()
                    break;

                // Reset Bix to factory state.
                case "R":
                    resetBix(); // Controller.resetBix()
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
        try {
            // Get the URL for the Bix GitHub page.
            var github_page = new URI(BIX_GITHUB_URL);

            // Open the URL.
            var desktop = Desktop.getDesktop();
            desktop.browse(github_page);
        }
        catch (IOException ioe) {
            terminateSession(StatusCode.UNKNOWN_ERROR);
        }
        catch (URISyntaxException ue) {
            terminateSession(StatusCode.INVALID_GITHUB_URL_SYNTAX);
        }
    }

} // class Bix
