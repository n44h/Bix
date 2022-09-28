package com.bix;

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

// Java program to calculate SHA hash value

class Krypto {

    // SecureRandom object for SALT and IV generation
    private static final SecureRandom RANDOM = new SecureRandom();

    // SHA256 Hashing
    /*-----------------------------------------------------------------------------------------*/
    protected static String getSHA256(String input) throws NoSuchAlgorithmException
    {
        // Static getInstance method is called with hashing SHA
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // digest() method called
        // to calculate message digest of an input
        // and return array of byte
        return toHexString(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    } // getSHA256()

    protected static String toHexString(byte[] hash)
    {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 64) { hexString.insert(0, '0'); }

        return hexString.toString();
    } // toHexString()
    /*-----------------------------------------------------------------------------------------*/

    // AES Encryption
    /*-----------------------------------------------------------------------------------------*/
    protected static String encrypt(String plaintext, String password, int algorithm){
        // returns the output as comma separated values
        // like this: "CIPHERTEXT,SALT,IV,KEYHASH"

        try{
            // generate salt
            byte[] salt = generateSalt();

            // generate secret_key
            SecretKey secret_key = getSecretKey(password, salt, algorithm);

            // generate IV
            IvParameterSpec iv = generateIV();

            // initializing cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secret_key, iv);

            // encrypting plaintext and then encoding to Base64 String
            String ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));

            // returning "ciphertext(base64), salt(base64), iv(base64), hash of secret_key"
            return ( ciphertext + "," + Base64.getEncoder().encodeToString(salt) + "," +
                    Base64.getEncoder().encodeToString(iv.getIV()) + "," + getKeyHash(secret_key));
        }
        catch(Exception e){
            System.out.println("\n\nEncryption Process Failure");
            e.printStackTrace();
        }
        return "0";

    } // encryptAES()

    private static byte[] generateSalt(){
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;

    } // getSalt()

    private static SecretKey getSecretKey(String password, byte[] salt, int algorithm)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, algorithm);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

    } // getSecretKey()

    private static IvParameterSpec generateIV() {
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        return new IvParameterSpec(iv);
    } // generateIV()

    /**
     * this method converts a SecretKey object -> byte[] -> hexadecimal string -> sha256 hash
     */
    protected static String getKeyHash(SecretKey secret_key) throws NoSuchAlgorithmException {
        byte[] array = secret_key.getEncoded(); // converting from SecretKey object to byte[]
        StringBuilder hex = new StringBuilder();
        for (byte b : array)
            hex.append(String.format("%02X", b));

        return getSHA256(hex.toString());

    }// ByteArrayToHexadecimal()

    protected static String generateKeyAndGetHash(String master_key, String salt, int algorithm) throws NoSuchAlgorithmException{
        SecretKey secret_key = null;
        try { // generate secret_key
            secret_key = getSecretKey(master_key, Base64.getDecoder().decode(salt), algorithm);
        } catch (Exception e) {e.printStackTrace();}

        assert secret_key != null;
        return getKeyHash(secret_key);
    } // generateKeyAndGetHash()
    /*-----------------------------------------------------------------------------------------*/

    // AES Decryption
    /*-----------------------------------------------------------------------------------------*/
    protected static String decrypt(String ciphertext, String password, String salt, String iv, int algorithm){
        try{
            // generate secret_key
            SecretKey secret_key = getSecretKey(password, Base64.getDecoder().decode(salt), algorithm);

            // initialize iv object
            IvParameterSpec iv_object = new IvParameterSpec(Base64.getDecoder().decode(iv));

            // initialize cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secret_key, iv_object);
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(ciphertext));

            return new String(plainText);
        }
        catch(Exception e) {
            System.out.println("\nDecryption Failed.");
            e.printStackTrace();
        }
        return "";
    } // decrypt()
    /*-----------------------------------------------------------------------------------------*/

} // class Krypto
