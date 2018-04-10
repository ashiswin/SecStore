package server;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.SignatureException;

import common.Packet;

public class ServerThread extends Thread {
	// Client connection variables
	Socket connectionSocket;
	DataInputStream fromClient;
	DataOutputStream toClient;
	
	// Data variables
	byte[] signedWelcome;
	boolean established = false;
	
	
	BufferedOutputStream bufferedFileOutputStream;
	
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
			fromClient = new DataInputStream(connectionSocket.getInputStream());
			toClient = new DataOutputStream(connectionSocket.getOutputStream());
			int count = 0;
			
			while(!connectionSocket.isClosed()) {
				int packetType = fromClient.readInt();
				
				if (established && packetType == Packet.FILENAME.getValue()) { // If the packet is for transferring the filename
					bufferedFileOutputStream = Handler.handleFilename(fromClient, toClient);
				}
				else if (established && packetType == Packet.FILE.getValue()) { // If the packet is for transferring a chunk of the file
					if(Handler.handleFile(fromClient, toClient, bufferedFileOutputStream)) {
						count++;
					}
				} else if (packetType == Packet.EOS.getValue()) { // If packet is for session termination
					System.out.println("Received " + count + " blocks");
					System.out.println("Closing connection...");
	
					if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
					fromClient.close();
					toClient.close();
					connectionSocket.close();
				}
				else if(packetType == Packet.HELO.getValue()) { // If packet is for initiation of communication
					established = Handler.handleHelo(fromClient, toClient, signedWelcome);
				}
				else if(established && packetType == Packet.CERT.getValue()) { // If packet is for requesting server certificate
					Handler.handleCert(fromClient, toClient);
				}
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
