package es.jaf.mfa_authenticator;

import android.content.Context;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Arrays;

import static es.jaf.mfa_authenticator.Utils.readFully;
import static es.jaf.mfa_authenticator.Utils.writeFully;

public class EncryptionHelper {
    private final static String ALGORITHM = "AES/GCM/NoPadding";
    //private final static int KEY_LENGTH = 16;
    private final static int IV_LENGTH = 12;

    public static byte[] encrypt(SecretKey secretKey, IvParameterSpec iv, byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        return cipher.doFinal(plainText);
    }

    public static byte[] decrypt(SecretKey secretKey, IvParameterSpec iv, byte[] cipherText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

        return cipher.doFinal(cipherText);
    }


    public static byte[] encrypt(SecretKey secretKey, byte[] plaintext) throws NoSuchPaddingException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        final byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        byte[] cipherText = encrypt(secretKey, new IvParameterSpec(iv), plaintext);


        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return combined;
    }

    public static byte[] decrypt(SecretKey secretKey, byte[] cipherText) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] iv = Arrays.copyOfRange(cipherText, 0, IV_LENGTH);
        byte[] cipher = Arrays.copyOfRange(cipherText, IV_LENGTH,cipherText.length);

        return decrypt(secretKey,new IvParameterSpec(iv), cipher );
    }

    /**
     * Load our symmetric secret key.
     * The symmetric secret key is stored securely on disk by wrapping
     * it with a public/private key pair, possibly backed by hardware.
     */
    public static SecretKey loadOrGenerateKeys(Context context, String password, File keyFile) throws GeneralSecurityException, IOException {
        final SecretKeyWrapper wrapper = new SecretKeyWrapper(context, "settings");

        try {
            // Generate secret key if none exists
            if (!keyFile.exists()) {
                final SecretKey key = getSecretKey(password);
                final byte[] wrapped = wrapper.wrap(key);

                writeFully(keyFile, wrapped);
            }
        } catch (Exception e) {
            Utils.saveException("Generating keys of file " + keyFile, e);
        }
        // Even if we just generated the key, always read it back to ensure we
        // can read it successfully.
        final byte[] wrapped = readFully(keyFile);

        return wrapper.unwrap(wrapped);
    }

    public static SecretKey getSecretKey(String password) throws IOException {
        byte[] salt = "PPRMYSDAEWUAQLISH3GK".getBytes(StandardCharsets.UTF_8); //generateSecretBase32(20);

        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        } catch (Exception e) {
            Utils.saveException("Getting secret key", e);
            throw new IOException(e);
        }
    }
}
