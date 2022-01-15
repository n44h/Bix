package com.cookiecrumbs19212.bix.BixDB.DatabaseExceptions;

public class DirectoryNotFoundException extends Exception {
    public DirectoryNotFoundException(String directory_path) {
        super("Directory \"" + directory_path + "\" does not exist.");
    }
}