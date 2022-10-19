package com.bix.exceptions;

public class VaultConnectionFailedException extends Exception {
    public VaultConnectionFailedException() {
        super("ERROR: Vault connection failed. Try operation again.");
    }
}
