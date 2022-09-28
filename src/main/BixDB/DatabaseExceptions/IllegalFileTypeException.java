package com.cookiecrumbs19212.bix.BixDB.DatabaseExceptions;

public class IllegalFileTypeException extends Exception {
    public IllegalFileTypeException() {
        super("Illegal file type. File should be of type \".ivry\".");
    }
}