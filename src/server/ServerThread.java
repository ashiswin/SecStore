package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.SignatureException;

import common.Packet;

public class ServerThread extends Thread {
	Socket connectionSocket;
	FileOutputStream fileOutputStream;
	BufferedOutputStream bufferedFileOutputStream;
	byte[] signedWelcome;
	
	public ServerThread(Socket connectionSocket) {
		this.connectionSocket = connectionSocket;
		try {
			signedWelcome = sign(Server.WELCOME_MESSAGE.getBytes("UTF-8"));
		} catch (SignatureException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] sign(byte[] message) throws SignatureException {
		Server.dsa.update(message);
		return Server.dsa.sign();
	}
	
	public void run() {
		try {
			DataInputStream fromClient = new DataInputStream(connectionSocket.getInputStream());
			DataOutputStream toClient = new DataOutputStream(connectionSocket.getOutputStream());
			boolean established = false;
			int count = 0;
			
			while(!connectionSocket.isClosed()) {
				int packetType = fromClient.readInt();
				
				if (established && packetType == Packet.FILENAME.getValue()) { // If the packet is for transferring the filename
					System.out.println("Receiving file...");
	
					int numBytes = fromClient.readInt();
					byte[] filename = new byte[numBytes];
					fromClient.read(filename);
					String filenameString = new String(filename, 0, numBytes);
					
					System.out.println("Saving file to " + Server.UPLOAD_DIR + filenameString);
					fileOutputStream = new FileOutputStream(Server.UPLOAD_DIR + filenameString);
					bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);
				}
				else if (established && packetType == Packet.FILE.getValue()) { // If the packet is for transferring a chunk of the file
					int numBytes = fromClient.readInt();
					if (numBytes > 0) {
						byte[] block = new byte[numBytes];
						fromClient.read(block);
						bufferedFileOutputStream.write(block, 0, numBytes);
						count++;
					}
				} else if (established && packetType == Packet.EOS.getValue()) { // If packet is for session termination
					System.out.println("Received " + count + " blocks");
					System.out.println("Closing connection...");
	
					if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
					if (bufferedFileOutputStream != null) fileOutputStream.close();
					fromClient.close();
					toClient.close();
					connectionSocket.close();
				}
				else if(packetType == Packet.HELO.getValue()) { // If packet is for initiation of communication
					System.out.println("Receiving hello");
					int numBytes = fromClient.readInt();
					byte[] helo = new byte[numBytes];
					fromClient.read(helo);
					
					String heloString = new String(helo, 0, numBytes);
					if(!heloString.equals("HELO")) {
						System.err.println("Invalid HELO received: " + heloString);
					}
					else {
						established = true;
						System.out.println("Sending welcome message...");
						toClient.writeInt(signedWelcome.length);
						toClient.write(signedWelcome);
						toClient.flush();
						System.out.println("Sent welcome message");
					}
				}
				else if(established && packetType == Packet.CERT.getValue()) { // If packet is for requesting server certificate
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
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
