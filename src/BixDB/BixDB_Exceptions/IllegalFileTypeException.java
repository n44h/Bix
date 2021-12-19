package BixDB.BixDB_Exceptions;

public class IllegalFileTypeException extends Exception {
    public IllegalFileTypeException() {
        super("Illegal file type. File should be of type \".ivry\".");
    }
}