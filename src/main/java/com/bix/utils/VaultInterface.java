package com.bix.utils;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.ArrayList;

/**
 * Class to communicate with the SQLite database "vault.db".
 * The database contains 2 tables: "accounts" and "bix_metadata".
 * accounts table stores the encrypted account credentials.
 * bix_metadata table stores information critical to Bix operations.
 */

public final class VaultInterface {
    /* Note:
     * The names "vault" and "database" are used interchangeably through the documentation in this class
     * depending on the appropriate naming for doc.
     *
     * Conceptually, the "vault" contains all the account entries. And so, when referring to the "vault", we are
     * essentially referring to the "accounts" table inside the "vault.db" database.
     */

    private static final String VAULT_RESOURCE_PATH = "vault.db";

    /* Note:
     * To connect to an embedded database in a regular Java project the url would look like:
     * "jdbc:sqlite:path/to/vault.db"
     *
     * To connect to an embedded database in the resource folder of a Gradle project:
     * "jdbc:sqlite::resource:vault.db"
     */
    private static final String URL = String.format("jdbc:sqlite::resource:%s", VAULT_RESOURCE_PATH);

    // Number of account entries in the vault.
    private static int numberOfEntries;

    // List of accounts stored in the vault.
    private static ArrayList<String> accountNames = new ArrayList<>();

    // Static initializer. Will be executed as soon as the program starts.
    static {
        // Get the number of entries in the vault and store it in num_of_entries.
        // Construct SQL statement to count entries in a table.
        String countStmt = "SELECT count(*) FROM accounts";
        String selectAccNamesStmt = "SELECT account_name FROM accounts";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Getting the number of entries.
            // Execute the SQL query to count the number of entries.
            ResultSet rs = stmt.executeQuery(countStmt);
            // rs will contain one int value, which is the number of entries in the vault.
            numberOfEntries = rs.getInt(1);

            // Getting the account names.
            // Execute the SQL query to select all the account names.
            rs = stmt.executeQuery(selectAccNamesStmt);
            // Loop through every value in the result set and get the account names.
            while (rs.next()) {
                // Add the account name to accountNames list.
                accountNames.add(rs.getString(1));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Connect to the vault.db database.
     *
     * @return the Connection object
     */
    private static Connection connect() {
        Connection conn;
        try {
            conn = DriverManager.getConnection(URL);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    /**
     * Checks if the vault file exists and is accessible.
     *
     * @return true if the vault.db file is accessible
     */
    public static boolean vaultExists() {
        // Connect to the database.
        try (Connection conn = DriverManager.getConnection(URL)) {
            // If the connection is successful, a non-null value is returned.
            if (conn != null) {
                return true;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Check if the "accounts" table in vault.db is empty.
     *
     * @return true if "accounts" table does not exist or is empty
     */
    public static boolean isVaultEmpty() {
        return numberOfEntries == 0;
    }

    /**
     * Get the account names of all the entries in the vault.
     *
     * @return a String array containing the account names
     */
    public static String[] getAccountNames() {
        // Converting ArrayList<String> to String[].
        return accountNames.toArray(new String[0]);
    }

    /**
     * Checks if a particular account name already exists in the vault.
     *
     * @param accountName the account name to check against the list of stored account names in the vault
     *
     * @return true if the account name exists in the vault
     */
    public static boolean accountExists(String accountName) {
        return accountNames.contains(accountName);
    }

    /**
     * Get the number of entries in the vault.
     *
     * @return an int value
     */
    public static int getVaultSize() {
        return numberOfEntries;
    }

    /**
     * Create a new table in the database. Used during initial Bix setup.
     *
     * @param tableName the name of the table to be created
     */
    public static void createTable(String tableName) {
        // Construct SQL Statement for creating a new table.
        String createTableStmt = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                	account_name TEXT PRIMARY KEY,
                	ciphertext TEXT NOT NULL,
                	salt TEXT NOT NULL,
                	iv TEXT NOT NULL,
                	secret_hash TEXT NOT NULL
                );""", tableName);

        // Open connection.
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Execute the SQL statement to create a new table.
            stmt.execute(createTableStmt);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a table from the SQLite database.
     *
     * @param tableName the name of the table to delete from the database
     */
    public static void deleteTable(String tableName) {
        // Construct the SQL statement to delete a table if it exists.
        String deleteTableStmt = String.format("DROP TABLE IF EXISTS %s", tableName);

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(deleteTableStmt);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Lists all the tables in the database.
     */
    public static ArrayList<String> getTables() {
        // ArrayList to store the tables.
        ArrayList<String> tables = new ArrayList<>();

        try (Connection conn = connect()) {
            ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
            while (rs.next()) {
                // Add the table to the list.
                tables.add(rs.getString("TABLE_NAME"));
            }

            return tables;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add an account entry to the vault.
     *
     * @param accountName the account name
     * @param ciphertext the ciphertext containing the encrypted username and password
     * @param salt the salt used for encrypting ciphertext
     * @param iv the initialization vector used for encrypting ciphertext
     * @param secretHash secret hash of the secret key
     */
    public static void addAccount(
            String accountName, String ciphertext, String salt, String iv, String secretHash) {
        // If the account name already exists in the vault, raise an error.
        if (accountNames.contains(accountName))
            throw new RuntimeException("An account with the name " + accountName + " already exists in the vault.");

        // Construct SQL statement for inserting a new entry.
        String insertStmt = "INSERT INTO accounts(account_name,ciphertext,salt,iv,secret_hash) VALUES(?,?,?,?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(insertStmt)) {
            // Set the corresponding values of the insert statement.
            pstmt.setString(1, accountName);
            pstmt.setString(2, ciphertext);
            pstmt.setString(3, salt);
            pstmt.setString(4, iv);
            pstmt.setString(5, secretHash);

            // Execute the prepared statement.
            pstmt.executeUpdate();

            // Update the class variables.
            accountNames.add(accountName);
            numberOfEntries++;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve an Account entry.
     *
     * @param accountName the account to retrieve from the vault
     *
     * @return a String[] containing the account information
     */
    public static String[] retrieveAccount(String accountName) {
        // Return null if the account does not exist in the vault.
        if (!accountNames.contains(accountName))
            return null;

        // Construct the SQL select statement.
        String selectStmt = "SELECT * FROM accounts WHERE account_name = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(selectStmt)) {
            // Set the accountName field.
            pstmt.setString(1, accountName);

            // Execute the select SQL statement and get the result set.
            ResultSet rs = pstmt.executeQuery();

            // Unpack the ResultSet into a String array.
            return new String[] {rs.getString("account_name"),
                    rs.getString("ciphertext"),
                    rs.getString("salt"),
                    rs.getString("iv"),
                    rs.getString("secret_hash")};
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update an existing account entry in the vault.
     *
     * @param accountName the account name (primary key in the database)
     * @param ciphertext the updated ciphertext containing the encrypted username and password
     * @param salt the new salt used for encrypting ciphertext
     * @param iv the new initialization vector used for encrypting ciphertext
     * @param secretHash the updated secret hash of the secret key
     */
    public static void updateAccount(
            String accountName, String ciphertext, String salt, String iv, String secretHash) {
        // Exit function if the account does not exist in the vault.
        if (!accountNames.contains(accountName))
            return;

        // Construct SQL statement to update an account entry.
        String updateStmt = """
                UPDATE accounts
                SET ciphertext = ?,
                salt = ?,
                iv = ?,
                secret_hash = ?
                WHERE account_name = ?;""";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(updateStmt)) {

            // Set the corresponding values of the update statement.
            pstmt.setString(1, ciphertext);
            pstmt.setString(2, salt);
            pstmt.setString(3, iv);
            pstmt.setString(4, secretHash);
            pstmt.setString(5, accountName);

            // Execute the update statement.
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete an account entry from the vault.
     *
     * @param accountName name of the account to delete from the vault
     */
    public static void deleteAccount(String accountName) {
        // Exit function if the account does not exist in the vault.
        if (!accountNames.contains(accountName))
            return;

        // Construct the SQL Statement to delete an account entry.
        String deleteStmt = "DELETE FROM accounts WHERE account_name = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(deleteStmt)) {

            // Set the corresponding value of the delete statement.
            pstmt.setString(1, accountName);

            // Execute the delete statement.
            pstmt.executeUpdate();

            // Update the class variables.
            accountNames.remove(accountName);
            numberOfEntries--;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Purges the vault. Deletes all the contents of the database.
     * This function is reserved for destroying the Bix vault at the user's request.
     * Unsurprisingly, this process is irreversible.
     */
    public static void purgeVault() {
        // Delete tables.
        for (String table : getTables()) {
            deleteTable(table);
        }

        // Set the class variables to null values.
        accountNames = null;
        numberOfEntries = 0;
    }

} // class VaultInterface
