package Bix;

public enum AES_FLAVOR{
    AES_128(128),
    AES_192(192),
    AES_256(256);

    private final int value;

    AES_FLAVOR(int value) {
        this.value = value;
    }

    public int toInteger() {
        return value;
    }
} // end of enum
