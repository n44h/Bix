package bix.exceptions;

public class AccountAlreadyExistsException extends Exception {
    public AccountAlreadyExistsException(String accountName) {
        super(String.format("An account with the name \"%s\" already exists.", accountName));
    }
}
