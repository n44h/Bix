package com.bix.utils;

import com.bix.exceptions.*;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * Class to communicate with the SQLite database "vault.db".
 * The database contains 2 tables: "accounts" and "bix_metadata".
 * - accounts table stores the encrypted account credentials.
 * - bix_metadata table stores information critical to Bix operations.
 */

/* Note:
 * The names "vault" and "database" are used interchangeably through the documentation in this class
 * depending on the appropriate naming for doc.
 *
 * Conceptually, the "vault" contains all the account entries. And so, when referring to the "vault", we are
 * essentially referring to the "accounts" table inside the "vault.db" database.
 */

/* Columns in the vault database's "accounts" table:
 * +--------------+--------------+--------------+--------------+--------------+--------------+--------------+
 * | [0]          | [1]          | [2]          | [3]          | [4]          | [5]          | [6]          |
 * |--------------|--------------|--------------|--------------|--------------|--------------|--------------|
 * | account_name | email        | ciphertext_u | ciphertext_p | salt         | iv           | secret_hash  |
 * +--------------+--------------+--------------+--------------+--------------+--------------+--------------+
 * ciphertext_u is the encrypted username
 * ciphertext_p is the encrypted password
 * secret_hash is the SHA256 hash of the secret key (which is generated using thr master password and the salt)
 */

final class VaultController {
    // Variable to indicate whether initial setup is required.
    private static final String VAULT_RESOURCE_PATH = "vault.db";

    /* Note:
     * To connect to an embedded database in a regular Java project the url would look like:
     * "jdbc:sqlite:path/to/vault.db"
     *
     * To connect to an embedded database in the resource folder of a Gradle project:
     * "jdbc:sqlite::resource:vault.db"
     */
    private static final String URL = String.format("jdbc:sqlite::resource:%s", VAULT_RESOURCE_PATH);


    //--------------------


    /**
     * Performs the initial vault setup. Used during initial Bix setup.
     */
    static void setupVault() {
        // Clean up any junk data in the vault database.
        purgeVault();

        // Create the bix_metadata table if it does not already exist.
        createMetadataTable();

        // Create the accounts table if it does not already exist.
        createAccountsTable();
    }


    //--------------------


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
    static boolean vaultExists() {
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
     * Get the number of entries in the vault.
     *
     * @return an int value
     */
    static int getVaultSize() {
        int numberOfEntries;

        // Construct SQL statement to count entries in the "accounts" table.
        String countStmt = "SELECT count(*) FROM accounts";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Execute the SQL query to count the number of entries.
            ResultSet rs = stmt.executeQuery(countStmt);

            // rs will contain one int value, which is the number of entries in the vault.
            numberOfEntries = rs.getInt(1);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return numberOfEntries;
    }


    //--------------------


    /**
     * Create the accounts table in the database. Used during initial Bix setup.
     */
    private static void createAccountsTable() {
        // Construct SQL Statement for creating the "accounts" table.
        String createTableStmt = """
                CREATE TABLE IF NOT EXISTS accounts (
                	account_name TEXT PRIMARY KEY,
                	associated_email TEXT,
                	ciphertext_u TEXT NOT NULL,
                	ciphertext_p TEXT NOT NULL,
                	salt TEXT NOT NULL,
                	iv TEXT NOT NULL,
                	secret_hash TEXT NOT NULL
                );""";

        // Open connection.
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Execute the SQL statement to create the "accounts" table.
            stmt.execute(createTableStmt);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the bix_metadata table in the database. Used during initial Bix setup.
     */
    private static void createMetadataTable() {
        // Construct SQL Statement for creating the "bix_metadata" table.
        String createTableStmt = """
                CREATE TABLE IF NOT EXISTS bix_metadata (
                	id TEXT PRIMARY KEY,
                	metadata_value TEXT NOT NULL
                );""";

        // Open connection.
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Execute the SQL statement to create the "bix_metadata" table.
            stmt.execute(createTableStmt);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Add the metadata fields with default values.
        addMetadata("setup_complete", "false");
        addMetadata("master_password_hash", "null");
        addMetadata("aes_flavor", "null");
        addMetadata("credential_display_duration", 30);
        addMetadata("failed_login_attempts", 0);
    }

    /**
     * Deletes a table from the SQLite database.
     *
     * @param tableName the name of the table to delete from the database
     */
    static void deleteTable(String tableName) {
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
     * Get a list of all the tables in the database.
     */
    static ArrayList<String> getTables() {
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


    //--------------------


    /**
     * Get the account names of all the entries in the vault.
     *
     * @return an ArrayList containing the account names
     */
    static ArrayList<String> getAccountNames() {
        // Construct SQL statement to select all the account names from the accounts table.
        String selectAccNamesStmt = "SELECT account_name FROM accounts";

        // ArrayList to store the account names.
        ArrayList<String> accountNames = new ArrayList<>();

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Execute the SQL query to select all the account names.
            ResultSet rs = stmt.executeQuery(selectAccNamesStmt);

            // Loop through every value in the result set and get the account names.
            while (rs.next()) {
                // Add the account name to the array.
                accountNames.add(rs.getString(1));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return accountNames;
    }

    /**
     * Get a list of all the Account Names containing a particular keyword.
     *
     * @param keyword the keyword to search for
     *
     * @return a list containing account names
     */
    static ArrayList<String> getAccountNamesContaining(String keyword) {
        // Convert the keyword to uppercase to perform a case-insensitive search.
        keyword = keyword.toUpperCase(Locale.ROOT);

        // Construct SQL statement to select all the account names from the accounts table.
        String selectAccNamesStmt = "SELECT account_name FROM accounts WHERE UPPER(account_name) LIKE '%?%'";

        // ArrayList to store the account names.
        ArrayList<String> accountNames = new ArrayList<>();

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(selectAccNamesStmt)) {

            // Set the corresponding values of the select statement.
            pstmt.setString(1, keyword);

            // Execute the select SQL statement and get the result set.
            ResultSet rs = pstmt.executeQuery();

            // Loop through every value in the result set and get the account names.
            while (rs.next()) {
                // Add the account name to the array.
                accountNames.add(rs.getString(1));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return accountNames;
    }

    /**
     * Checks if a particular account name already exists in the vault.
     *
     * @param accountName the account name to check against the list of stored account names in the vault
     *
     * @return true if the account name exists in the vault
     */
    static boolean accountExists(String accountName) {
        return getAccountNames().contains(accountName);
    }

    /**
     * Add an account entry to the vault.
     *
     * @param accountName the account name
     * @param associatedEmail the email associated with the account, can be null
     * @param ciphertextUsername the ciphertext containing the encrypted username
     * @param ciphertextPassword the ciphertext containing the encrypted password
     * @param salt the salt used for encrypting ciphertext
     * @param iv the initialization vector used for encrypting ciphertext
     * @param secretHash secret hash of the secret key
     */
    static void addAccount(
            String accountName, String associatedEmail, String ciphertextUsername, String ciphertextPassword,
            String salt, String iv, String secretHash)
            throws AccountAlreadyExistsException {

        // If the account name already exists in the vault, raise an error.
        if (accountExists(accountName))
            throw new AccountAlreadyExistsException(accountName);

        // Construct SQL statement for inserting a new entry.
        String insertStmt = "INSERT INTO " +
                "accounts(account_name,associated_email,ciphertext_u,ciphertext_p,salt,iv,secret_hash) " +
                "VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(insertStmt)) {
            // Set the corresponding values of the insert statement.
            pstmt.setString(1, accountName);
            pstmt.setString(2, Objects.requireNonNullElse(associatedEmail, "nil"));
            pstmt.setString(3, ciphertextUsername);
            pstmt.setString(4, ciphertextPassword);
            pstmt.setString(5, salt);
            pstmt.setString(6, iv);
            pstmt.setString(7, secretHash);

            // Execute the prepared statement.
            pstmt.executeUpdate();
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
    static String[] retrieveAccount(String accountName) throws AccountNotFoundException {
        // Return null if the account does not exist in the vault.
        if (!accountExists(accountName))
            throw new AccountNotFoundException(accountName);

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
                    rs.getString("associated_email"),
                    rs.getString("ciphertext_u"),
                    rs.getString("ciphertext_p"),
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
     * @param associatedEmail the email associated with the account, can be null
     * @param ciphertextUsername the ciphertext containing the encrypted username
     * @param ciphertextPassword the ciphertext containing the encrypted password
     * @param salt the new salt used for encrypting ciphertext
     * @param iv the new initialization vector used for encrypting ciphertext
     * @param secretHash the updated secret hash of the secret key
     */
    static void updateAccount(
            String accountName, String associatedEmail, String ciphertextUsername, String ciphertextPassword,
            String salt, String iv, String secretHash)
            throws AccountNotFoundException {

        // Exit function if the account does not exist in the vault.
        if (!accountExists(accountName))
            throw new AccountNotFoundException(accountName);

        // Construct SQL statement to update an account entry.
        String updateStmt = """
                UPDATE accounts
                SET associated_email = ?,
                ciphertext_u = ?,
                ciphertext_p = ?,
                salt = ?,
                iv = ?,
                secret_hash = ?
                WHERE account_name = ?;""";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(updateStmt)) {

            // Set the corresponding values of the update statement.
            pstmt.setString(1, Objects.requireNonNullElse(associatedEmail, "nil"));
            pstmt.setString(2, ciphertextUsername);
            pstmt.setString(3, ciphertextPassword);
            pstmt.setString(4, salt);
            pstmt.setString(5, iv);
            pstmt.setString(6, secretHash);
            pstmt.setString(7, accountName);

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
    static void deleteAccount(String accountName) {
        // Exit function if the account does not exist in the vault.
        if (!accountExists(accountName))
            return;

        // Construct the SQL Statement to delete an account entry.
        String deleteStmt = "DELETE FROM accounts WHERE account_name = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(deleteStmt)) {

            // Set the corresponding value of the delete statement.
            pstmt.setString(1, accountName);

            // Execute the delete statement.
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    //--------------------


    /**
     * Add a metadata field with a String value to the "bix_metadata" table in the database.
     *
     * @param id the metadata id, must be unique
     * @param value the String value of the metadata
     */
    private static void addMetadata(String id, String value) {
        // Construct SQL statement for inserting a new entry.
        String insertStmt = "INSERT INTO bix_metadata(id,metadata_value) VALUES(?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(insertStmt)) {
            // Set the corresponding values of the insert statement.
            pstmt.setString(1, id);
            pstmt.setString(2, value);

            // Execute the prepared statement.
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add a metadata field with integer value to the "bix_metadata" table in the database.
     *
     * @param id the metadata id, must be unique
     * @param value the integer value of the metadata
     */
    private static void addMetadata(String id, int value) {
        // Construct SQL statement for inserting a new entry.
        String insertStmt = "INSERT INTO bix_metadata(id,metadata_value) VALUES(?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(insertStmt)) {
            // Set the corresponding values of the insert statement.
            pstmt.setString(1, id);
            pstmt.setInt(2, value);

            // Execute the prepared statement.
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a String metadata value from the "bix_metadata" table.
     *
     * @param id the id of the metadata
     *
     * @return a String metadata value
     */
    static String getStrMetadata(String id) {
        // Construct the SQL select statement.
        String selectStmt = "SELECT value FROM bix_metadata WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(selectStmt)) {
            // Set the id field.
            pstmt.setString(1, id);

            // Execute the select SQL statement and get the result set.
            ResultSet rs = pstmt.executeQuery();

            // Return the metadata value from the ResultSet.
            return rs.getString("metadata_value");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an integer metadata value from the "bix_metadata" table.
     *
     * @param id the id of the metadata
     *
     * @return an integer metadata value
     */
    static int getIntMetadata(String id) {
        // Construct the SQL select statement.
        String selectStmt = "SELECT value FROM bix_metadata WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(selectStmt)) {
            // Set the id field.
            pstmt.setString(1, id);

            // Execute the select SQL statement and get the result set.
            ResultSet rs = pstmt.executeQuery();

            // Return the metadata value from the ResultSet.
            return rs.getInt("metadata_value");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update a String metadata value in the "bix_metadata" table.
     *
     * @param id the id of the metadata
     * @param value the new metadata value
     */
    static void updateMetadata(String id, String value) {
        // Construct SQL statement to update an entry.
        String updateStmt = "UPDATE bix_metadata SET value = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(updateStmt)) {

            // Set the corresponding values of the update statement.
            pstmt.setString(1, value);
            pstmt.setString(2, id);

            // Execute the update statement.
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update an integer metadata value in the "bix_metadata" table.
     *
     * @param id the id of the metadata
     * @param value the new metadata value
     */
    static void updateMetadata(String id, int value) {
        // Construct SQL statement to update an entry.
        String updateStmt = "UPDATE bix_metadata SET value = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(updateStmt)) {

            // Set the corresponding values of the update statement.
            pstmt.setInt(1, value);
            pstmt.setString(2, id);

            // Execute the update statement.
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    //--------------------


    /**
     * Purges the vault. Deletes all the contents of the database.
     * This function is reserved for destroying the Bix vault at the user's request.
     * Unsurprisingly, this process is irreversible.
     */
    static void purgeVault() {
        // Delete tables.
        for (String table : getTables()) {
            deleteTable(table);
        }
    }

} // class VaultController
