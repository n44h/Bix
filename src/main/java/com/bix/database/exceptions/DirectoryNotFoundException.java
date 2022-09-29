package com.bix.database.exceptions;

public class DirectoryNotFoundException extends Exception {
    public DirectoryNotFoundException(String directory_path) {
        super("Directory \"" + directory_path + "\" does not exist.");
    }
}