package server;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;

import common.AES;
import common.Protocol;
import common.protocols.BaseProtocol;
import common.protocols.CP1;
import common.protocols.CP2;

public class Handler {
	/*
	 * handleHelo(): Handle an initiation packet from the client and verify that it is valid
	 */
	public static boolean handleHelo(DataInputStream fromClient, DataOutputStream toClient, byte[] signedWelcome) throws IOException {
		System.out.println("Receiving hello");
		int numBytes = fromClient.readInt();
		byte[] helo = new byte[numBytes];
		fromClient.read(helo);
		
		String heloString = new String(helo, 0, numBytes);
		if(!heloString.equals("HELO")) {
			System.err.println("Invalid HELO received: " + heloString);
		}
		else {
			System.out.println("Sending welcome message...");
			toClient.writeInt(signedWelcome.length);
			toClient.write(signedWelcome);
			toClient.flush();
			System.out.println("Sent welcome message");
			return true;
		}
		
		return false;
	}
	
	/*
	 * handleCert(): Handle request for server's certificate
	 */
	public static void handleCert(DataInputStream fromClient, DataOutputStream toClient) throws IOException, CertificateException {
		System.out.println("Sending server certificate...");

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(new FileInputStream(new File(Server.SERVER_CERT_FILE)));
		
		byte[] encodedCert = cert.getEncoded();
		System.out.println("Cert length: " + encodedCert.length);
		toClient.writeInt(encodedCert.length);
		toClient.flush();
		toClient.write(encodedCert);
		
		System.out.println("Sent server certificate");
	}
	
	/*
	 * handleProtocl(): Handle the setting of the communication protocol
	 */
	public static BaseProtocol handleProtocol(DataInputStream fromClient, DataOutputStream toClient) throws IOException {
		System.out.println("Receiving protocol...");
		Protocol p = Protocol.fromInt(fromClient.readInt());
		BaseProtocol protocol;
		
		switch(p) {
			case CP1:
				protocol = new CP1(Server.privateKey);
				System.out.println("Established protocol CP1");
				break;
			case CP2:
				//Read Encrypted AES key
				int numBytes = fromClient.readInt();
				byte[] encryptedAES = new byte[numBytes];
				fromClient.read(encryptedAES);
				//Decrypt AES key
				SecretKey aesDecrypted = AES.decryptAESKey(encryptedAES,Server.privateKey);
				//Put into protocol
				protocol = new CP2(aesDecrypted);
				System.out.println("Established protocol CP2");
				break;

			default:
				System.err.println("Unknown protocol requested");
				protocol = null;
		}
		
		return protocol;
	}
	/*
	 * handleFilename(): Handle the initiation of a new file transfer
	 */
	public static BufferedOutputStream handleFilename(DataInputStream fromClient, DataOutputStream toClient) throws IOException {
		System.out.println("Receiving file...");
		
		int numBytes = fromClient.readInt();
		byte[] filename = new byte[numBytes];
		fromClient.read(filename);
		String filenameString = new String(filename, 0, numBytes);
		
		System.out.println("Saving file to " + Server.UPLOAD_DIR + filenameString);
		FileOutputStream fileOutputStream = new FileOutputStream(Server.UPLOAD_DIR + filenameString);
		
		return new BufferedOutputStream(fileOutputStream);
	}
	
	/*
	 * handleFile(): Handle receiving a new chunk of a file
	 */
	public static boolean handleFile(DataInputStream fromClient, DataOutputStream toClient, BufferedOutputStream bufferedFileOutputStream, BaseProtocol protocol) throws IOException {
		System.out.println("Receiving chunk with protocol " + protocol.getProtocol());
		if(protocol.getProtocol() == Protocol.CP2) {
			/*CP2 p = (CP2) protocol;
			CipherInputStream stream = new CipherInputStream(fromClient, p.getDecryptCipher());
		    int nextByte;
		    while ((nextByte = stream.read()) != -1) {
		    	bufferedFileOutputStream.write(nextByte);
		    }*/
			//stream.close();
			int len = fromClient.readInt();
			byte[] ciphertext = new byte[len];
			int offset = 0;
			while(offset < len) {
				offset += fromClient.read(ciphertext, offset, len - offset);
			}
			
			bufferedFileOutputStream.write(protocol.decrypt(ciphertext));
			return true;
		}
		else {
			int decryptedNumBytes = fromClient.readInt();
			int numBytes = fromClient.readInt();
			if (numBytes > 0) {
				byte[] block = new byte[numBytes];
				fromClient.read(block);
				
				System.out.println(Base64.getEncoder().encodeToString(block));
				byte[] decryptedBytes = protocol.decrypt(block);
				bufferedFileOutputStream.write(decryptedBytes, 0, decryptedNumBytes);
				
				return true;
			}
		}
		
		return false;
	}
}
