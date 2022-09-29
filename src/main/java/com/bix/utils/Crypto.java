package com.bix.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class serves 2 functions:
 * 1. Generating SHA256 hashes
 * 2. Encrypting and decrypting data (with choice of AES flavors 128, 192, or 256)
 */

class Crypto {

    // SecureRandom object for SALT and Initialization Vector (IV) generation.
    private static final SecureRandom RANDOM = new SecureRandom();


    // SHA256 Hashing
    /**
     * Generate SHA256 hash
     *
     * @param input String input to hash
     *
     * @return The {@code SHA256} hash of the input as a hexadecimal String
     */
    protected static String getSHA256Hash(String input) {
        // Generating MessageDigest object initialized with the SHA-256 algorithm.
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        /* md.digest() method generates the message digest of the input and returns a byte array.
         * The byte array is then converted into a hexadecimal String using toHexString(). */
        return toHexString(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    } // getSHA256Hash()

    /**
     * Converts byte arrays to hexadecimal Strings
     *
     * @param input as a byte array
     *
     * @return a hexadecimal String
     */
    protected static String toHexString(byte[] input) {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, input);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 64) { hexString.insert(0, '0'); }

        return hexString.toString();
    } // toHexString()


    // AES Encryption
    /**
     * Encrypts plaintext using AES algorithm
     *
     * @param plaintext the String to be encrypted
     *
     * @param password the password, along with a randomly generated salt, will be used to generate the secret key
     *                 for encrypting the plaintext
     *
     * @param flavor flavor of AES: 128, 192, or 256; Should be of type {@code int}
     *
     * @return a String containing comma separated values with the format: "CIPHERTEXT,SALT,IV,KEY_HASH"
     */
    protected static String encrypt(String plaintext, String password, int flavor) {
        try{
            // Generate random salt.
            byte[] salt = generateRandomSalt();

            // Generate random initialization vector (IV).
            IvParameterSpec iv = generateRandomIV();

            // Generate secret key.
            SecretKey secret_key = getSecretKey(password, salt, flavor);

            // Initializing cipher for AES in CBC mode using PKCS5 padding.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secret_key, iv);

            // Encrypting the plaintext and then encoding to Base64 String.
            String ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));

            // Return comma separated values: "ciphertext(base64), salt(base64), iv(base64), hash of secret_key".
            return ( ciphertext + "," + Base64.getEncoder().encodeToString(salt) + "," +
                    Base64.getEncoder().encodeToString(iv.getIV()) + "," + getKeyHash(secret_key));
        }
        catch(Exception e){
            System.out.println("\n\nEncryption Process Failure");
            e.printStackTrace();
        }
        return "0";
    } // encrypt()

    /**
     * Generates a random salt
     *
     * @return a byte array of size 16
     */
    private static byte[] generateRandomSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    } // generateRandomSalt()

    /**
     * Generates a random Initialization Vector
     *
     * @return an {@code IvParameterSpec} object
     */
    private static IvParameterSpec generateRandomIV() {
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        return new IvParameterSpec(iv);
    } // generateRandomIV()

    /**
     * Generates the Secret Key
     *
     * @param password the master password
     *
     * @param salt randomly generated salt
     *
     * @param algorithm AES flavor: 128, 192, or 256 as int
     *
     * @return {@code SecretKey} object
     */
    private static SecretKey getSecretKey(String password, byte[] salt, int algorithm) {
        SecretKey secret_key;
        try {
            // Create an instance of SecretKeyFactory with Password-Based Key Derivation Function 2 (PBKDF2).
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            // Create a Key Specifications object.
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, algorithm);

            // Generate the secret key
            secret_key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        return secret_key;
    } // getSecretKey()

    /**
     * Gets the hash of the Secret Key object
     *
     * @param secret_key the secret key to hash
     *
     * @return the SHA256 hash of the secret key as a hexadecimal String
     */
    protected static String getKeyHash(SecretKey secret_key) {
        // Convert SecretKey object to byte array.
        byte[] array = secret_key.getEncoded();

        // Initialize a StringBuilder to build the hex string.
        StringBuilder hex = new StringBuilder();

        // Convert each byte element in the array to hexadecimal and append it to StringBuilder object.
        for (byte b : array)
            hex.append(String.format("%02X", b));

        // Return the SHA256 hash of the hex string.
        return getSHA256Hash(hex.toString());
    }// getKeyHash()

    /**
     * Compares secret key hash generated from {@code masterKey} and {@code salt} to the {@code target hash}
     *
     * @param masterKey the master password
     *
     * @param salt the salt as a String
     *
     * @param algorithm AES flavor as an {@code int} 128, 192, or 256
     *
     * @return true if the hash of the generated secret key matches the target hash
     */
    protected static boolean authenticateSecretKey(String masterKey, String salt, int algorithm, String targetHash) {
        // Generate the secret key.
        SecretKey secretKey = getSecretKey(masterKey, Base64.getDecoder().decode(salt), algorithm);

        // Return true iff secret key is not null, and it is equal to the target hash.
        return secretKey != null && getKeyHash(secretKey).equals(targetHash);
    } // authenticateSecretKey()


    // AES Decryption
    /**
     * Decrypts the AES-encrypted ciphertext
     *
     * @param ciphertext the ciphertext to decrypt
     *
     * @param password the master password
     *
     * @param salt salt used during encryption
     *
     * @param iv initialization vector used during encryption
     *
     * @param algorithm the AES flavor as an {@code int}, 128, 192 or 256.
     *
     * @return the decrypted plaintext as a String
     */
    protected static String decrypt(String ciphertext, String password, String salt, String iv, int algorithm) {

        // Generate secret key object.
        SecretKey secretKey = getSecretKey(password, Base64.getDecoder().decode(salt), algorithm);

        // Initialize IvParameterSpec object.
        IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));

        // Decrypting ciphertext.
        String plaintext;
        try {
            // Initialize Cipher object.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Decrypting the ciphertext then converting the resulting byte array to a String.
            plaintext = new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return plaintext;
    } // decrypt()

} // class Crypto