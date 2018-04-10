package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import common.BaseProtocol;
import common.CP1;
import common.Protocol;

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
	public static void handleCert(DataInputStream fromClient, DataOutputStream toClient) throws IOException {
		System.out.println("Sending server certificate...");
		
		File cert = new File(Server.class.getResource(Server.SERVER_CERT_FILE).getFile());
		FileInputStream fileInputStream = new FileInputStream(cert);
		BufferedInputStream bis = new BufferedInputStream(fileInputStream);
		byte [] fromFileBuffer = new byte[(int) cert.length()];
		
		toClient.writeInt((int) cert.length());
		toClient.flush();
		
		int numBytes = 0;
		
		for (boolean fileEnded = false; !fileEnded;) {
			numBytes = bis.read(fromFileBuffer);
			fileEnded = numBytes < fromFileBuffer.length;
			
			toClient.writeInt(1);
			toClient.writeInt(numBytes);
			if(numBytes > 0) {
				toClient.write(fromFileBuffer);
			}
			toClient.flush();
		}
		
		bis.close();
		toClient.flush();
		
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
		int decryptedNumBytes = fromClient.readInt();
		int numBytes = fromClient.readInt();
		if (numBytes > 0) {
			byte[] block = new byte[numBytes];
			fromClient.read(block);
			
			byte[] decryptedBytes = protocol.decrypt(block);
			bufferedFileOutputStream.write(decryptedBytes, 0, decryptedNumBytes);
			
			return true;
		}
		
		return false;
	}
}
