package com.bix.database.exceptions;

public class IllegalFileTypeException extends Exception {
    public IllegalFileTypeException() {
        super("Illegal file type. File should be of type \".ivry\".");
    }
}