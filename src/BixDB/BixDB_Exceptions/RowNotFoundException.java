package BixDB.BixDB_Exceptions;

public class RowNotFoundException extends Exception {
    public RowNotFoundException(String id) {
        super("The row with ID: \"" + id + "\" is not found.");
    }
}