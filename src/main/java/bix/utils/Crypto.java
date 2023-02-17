package bix.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import java.security.*;
import java.security.spec.KeySpec;

import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// Get the cipher algorithm which contains the encryption system, encryption mode, and padding mode.
import static bix.utils.Constants.CIPHER_ALGORITHM;

/**
 * This class serves 2 functions:
 * 1. Generating SHA256 hashes
 * 2. Encrypting and decrypting data (with choice of AES flavors 128, 192, or 256)
 */

public final class Crypto {
    // SecureRandom object for salt and Initialization Vector (IV) generation.
    private final SecureRandom RANDOM;
    private final int AES_FLAVOR;

    public Crypto(int aesFlavor) {
        // Add the Bouncy Castle provider.
        Security.addProvider(new BouncyCastleProvider());

        // Create new SecureRandom instance, constructs a secure Random Number Generator.
        RANDOM = new SecureRandom();

        // Set the AES Flavor.
        AES_FLAVOR = aesFlavor;
    }


    // SHA256 Hashing
    /**
     * Generate SHA256 hash
     *
     * @param input String input to hash
     *
     * @return SHA256 hash as a hexadecimal String
     */
    public String getSHA256Hash(char[] input) {
        // Generating MessageDigest object initialized with the SHA-256 algorithm.
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256", "BC");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Converting char[] to byte[].
        byte[] inputAsBytes = charToByteArray(input);

        /* md.digest() method generates the message digest of the input and returns a byte array.
         * The byte array is then converted into a hexadecimal String using toHexString(). */
        return toHexString(md.digest(inputAsBytes));
    }

    /**
     * Gets the hash of the Secret Key object
     *
     * @param secretKey the secret key to hash
     *
     * @return the SHA256 hash of the secret key as a hexadecimal String
     */
    public String getKeyHash(SecretKey secretKey) {
        // Convert SecretKey object to byte array.
        byte[] byteArray = secretKey.getEncoded();

        // Converting byte[] to char[], then return the SHA256 hash of the char[].
        return getSHA256Hash(byteToCharArray(byteArray));
    }

    /**
     * Compares secret key hash generated from {@code masterPassword} and {@code salt} to the {@code target hash}
     *
     * @param masterPassword the master password
     * @param salt the salt as a String
     *
     * @return true if the hash of the generated secret key matches the target hash
     */
    public boolean authenticateSecretKey(char[] masterPassword, String salt, String targetHash) {
        // Generate the secret key.
        SecretKey secretKey = getSecretKey(masterPassword, decode(salt));

        // Return true iff secret key is not null, and it is equal to the target hash.
        return secretKey != null && getKeyHash(secretKey).equals(targetHash);
    }


    // AES Encryption/Decryption
    /**
     * Encrypts plaintext using AES algorithm
     *
     * @param masterPassword the masterPassword, along with a randomly generated salt, will be used to generate the
     *                       secret key for encrypting the plaintext
     * @param plaintext the String to be encrypted
     *
     * @return a String[] containing [CIPHERTEXT (base64), SALT (base64), IV (base64), SECRET_KEY_HASH]
     */
    public String[] encrypt(char[] masterPassword, char[] plaintext) {
        try{
            // Generate random salt.
            var salt = generateRandomSalt();

            // Generate random initialization vector (IV).
            var iv = generateRandomIV();

            // Generate secret key.
            var secretKey = getSecretKey(masterPassword, salt);

            // Initializing cipher for AES in CBC mode using PKCS5 padding.
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC");

            // Initialize the cipher in encrypt mode with the secret key and IV.
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            // Convert the plaintext from char[] to byte[].
            byte[] plaintextBytes = charToByteArray(plaintext);

            // Encrypting the plaintext.
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Return a String[] containing [ciphertext(base64), salt(base64), iv(base64), hash of secretKey].
            return new String[] {
                    encode(ciphertext),
                    encode(salt),
                    encode(iv.getIV()),
                    getKeyHash(secretKey)
            };
        }
        catch(Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * Decrypts the AES-encrypted ciphertext
     *
     * @param masterPassword the master password
     * @param ciphertext the ciphertext to decrypt
     * @param salt salt used during encryption
     * @param iv initialization vector used during encryption
     *
     * @return the decrypted plaintext as a String
     */
    public char[] decrypt(char[] masterPassword, String ciphertext, String salt, String iv) {

        // Generate secret key object.
        var secretKey = getSecretKey(masterPassword, decode(salt));

        // Initialize IvParameterSpec object.
        var ivSpec = new IvParameterSpec(decode(iv));

        // Decrypting ciphertext.
        char[] plaintext;
        try {
            // Initialize Cipher object.
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Decode the ciphertext from Base64 String to byte[] then decrypt the ciphertext.
            byte[] plaintextByteArray = cipher.doFinal(decode(ciphertext));

            // Converting plaintext from byte[] to char[].
            plaintext = byteToCharArray(plaintextByteArray);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return plaintext;
    }


    // Helper Functions
    /**
     * Generates a random salt
     *
     * @return a byte array of size 16
     */
    private byte[] generateRandomSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Generates a random Initialization Vector
     *
     * @return an {@code IvParameterSpec} object
     */
    private IvParameterSpec generateRandomIV() {
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    /**
     * Generates the Secret Key
     *
     * @param masterPassword the master password
     * @param salt randomly generated salt
     *
     * @return {@code SecretKey} object
     */
    private SecretKey getSecretKey(char[] masterPassword, byte[] salt) {
        SecretKey secretKey;
        try {
            // Create an instance of SecretKeyFactory with Password-Based Key Derivation Function 2 (PBKDF2).
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", "BC");

            // Create a Key Specifications object.
            KeySpec spec = new PBEKeySpec(masterPassword, salt, 65536, AES_FLAVOR);

            // Generate the secret key
            secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return secretKey;
    }

    /**
     * Converts byte arrays to hexadecimal Strings
     *
     * @param input a byte array
     *
     * @return a hexadecimal String
     */
    private static String toHexString(byte[] input) {
        // Convert byte array into signum representation.
        var number = new BigInteger(1, input);

        // Convert message digest into hex value.
        var hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros.
        while (hexString.length() < 64) { hexString.insert(0, '0'); }

        return hexString.toString();
    }

    /**
     * Converts byte array to char array.
     *
     * @param byteArray byte array to convert to char array
     *
     * @return char[]
     */
    private static char[] byteToCharArray(byte[] byteArray) {
        final CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(byteArray));
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit());
    }

    /**
     * Converts char array to byte array.
     *
     * @param charArray char array to convert to byte array
     *
     * @return byte[]
     */
    private static byte[] charToByteArray(char[] charArray) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(charArray));
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    /**
     * Encodes byte array to Base64 String.
     *
     * @param input byte[] input
     *
     * @return Base64 encoded String
     */
    private static String encode(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    /**
     * Decodes Base64 encoded String to byte array.
     *
     * @param input Base64 encoded String
     *
     * @return byte[]
     */
    private static byte[] decode(String input) {
        return Base64.getDecoder().decode(input);
    }

} // class Crypto