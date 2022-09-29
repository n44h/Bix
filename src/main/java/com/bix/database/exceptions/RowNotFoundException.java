package com.bix.database.exceptions;

public class RowNotFoundException extends Exception {
    public RowNotFoundException(String id) {
        super("The row with ID: \"" + id + "\" is not found.");
    }
}