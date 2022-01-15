package com.cookiecrumbs19212.bix.BixDB.DatabaseExceptions;

public class ColumnNotFoundException extends Exception {
    public ColumnNotFoundException(String column_name) {
        super("The column \"" + column_name + "\" is not found.");
    }
}