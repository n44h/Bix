package bix.exceptions;

public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String accountName) {
        super(String.format("Account \"%s\" does not exist.", accountName));
    }
}
