package library.server;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class MessageEncryption {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_SIZE = 128; // Use 128-bit key size
    private static final int IV_SIZE = 16; // 16 bytes for AES

    // Define a static secret key for encryption and decryption
    // Ensure the key is exactly 16 bytes long for AES-128
    private static final byte[] SECRET_KEY_BYTES = "1234567890123456".getBytes();

    // Create a SecretKeySpec from the static key bytes
    private static final SecretKeySpec SECRET_KEY = new SecretKeySpec(SECRET_KEY_BYTES, "AES");

    // Encrypt a plaintext message using AES
    public static String encryptMessage(String plaintext) throws Exception {
        // Create a random IV
        byte[] iv = new byte[IV_SIZE];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Create a Cipher object for AES/CBC/PKCS5Padding
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, ivSpec);

        // Encrypt the plaintext
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());

        // Combine the IV and the encrypted message
        byte[] combined = new byte[IV_SIZE + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, IV_SIZE);
        System.arraycopy(encryptedBytes, 0, combined, IV_SIZE, encryptedBytes.length);

        // Encode the combined IV and encrypted message using Base64
        return Base64.getEncoder().encodeToString(combined);
    }

    // Decrypt an encrypted message using AES
    public static String decryptMessage(String encryptedBase64) throws Exception {
        // Decode the Base64 encoded message
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract the IV and the encrypted message
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(combined, 0, iv, 0, IV_SIZE);

        byte[] encryptedMessage = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, IV_SIZE, encryptedMessage, 0, encryptedMessage.length);

        // Create an IvParameterSpec from the IV
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Create a Cipher object for AES/CBC/PKCS5Padding
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, ivSpec);

        // Decrypt the message
        byte[] decryptedBytes = cipher.doFinal(encryptedMessage);
        return new String(decryptedBytes);
    }
}
