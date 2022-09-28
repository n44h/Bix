package com.cookiecrumbs19212.bix.BixDB.DatabaseExceptions;

public class RowNotFoundException extends Exception {
    public RowNotFoundException(String id) {
        super("The row with ID: \"" + id + "\" is not found.");
    }
}