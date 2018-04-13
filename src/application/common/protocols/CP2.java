package common.protocols;

import java.security.InvalidAlgorithmParameterException;

/**
 * Created by Oon Tong on 4/10/2018.
 */

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import common.Protocol;

public class CP2 extends BaseProtocol {
    Key key; //AES key
    public CP2(Key key) {
        super(Protocol.CP2);
        this.key = key;
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Cipher getDecryptCipher() {
    	Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES");
	        cipher.init(Cipher.DECRYPT_MODE, key);
	        return cipher;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
    
    public Cipher getEncryptCipher() {
    	Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES");
	        cipher.init(Cipher.ENCRYPT_MODE, key);
	        return cipher;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
}
