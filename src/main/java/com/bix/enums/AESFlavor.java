package com.bix.enums;

public enum AESFlavor {
    AES_128(128, "AES-128"),
    AES_192(192, "AES-192"),
    AES_256(256, "AES-256");

    private final int intValue;
    private final String strValue;

    AESFlavor(final int intValue, final String strValue) {
        this.intValue = intValue;
        this.strValue = strValue;
    }

    public int toInteger() {
        return intValue;
    }

    public String toString() {
        return strValue;
    }

    public static AESFlavor fromString(String value) {
        switch(value) {
            case "AES-128" -> {
                return AES_128;
            }
            case "AES-192" -> {
                return AES_192;
            }
            default -> {
                return AES_256;
            }
        }
    }

} // enum AESFlavor