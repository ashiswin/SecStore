package common;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Created by Oon Tong on 4/10/2018.
 */
public class AES {
    public static SecretKey generateKey(){
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
            SecureRandom random = new SecureRandom();
            keyGen.init(random);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encryptAESKey(SecretKey aes, Key publicKey){
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(aes.getEncoded());

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SecretKey decryptAESKey(byte[] aesEncrypted, Key privateKey){
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new SecretKeySpec(cipher.doFinal(aesEncrypted), "AES");

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }
}
