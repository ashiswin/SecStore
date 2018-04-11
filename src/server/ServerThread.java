package server;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.SignatureException;
import java.security.cert.CertificateException;

import common.Packet;
import common.protocols.BaseProtocol;

public class ServerThread extends Thread {
	// Client connection variables
	Socket connectionSocket;
	DataInputStream fromClient;
	DataOutputStream toClient;
	
	// Data variables
	byte[] signedWelcome;
	boolean established = false;
	BaseProtocol protocol;
	long startTime = -1;
	
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
				Packet packet = Packet.fromInt(packetType);
				
				switch(packet) {
					case HELO:
						established = Handler.handleHelo(fromClient, toClient, signedWelcome);
						break;
					case CERT:
						if(!established) {
							sendEstablishError();
							break;
						}
						Handler.handleCert(fromClient, toClient);
						break;
					case PROTOCOL:
						protocol = Handler.handleProtocol(fromClient, toClient);
						break;
					case FILENAME:
						if(!established) {
							sendEstablishError();
							break;
						}
						bufferedFileOutputStream = Handler.handleFilename(fromClient, toClient);
						startTime = System.currentTimeMillis();
						break;
					case FILE:
						if(!established) {
							sendEstablishError();
							break;
						}
						if(Handler.handleFile(fromClient, toClient, bufferedFileOutputStream, protocol)) {
							count++;
						}
						break;
					case EOF:
						long endTime = System.currentTimeMillis() - startTime;
						startTime = -1;
						
						System.out.println("File transfer too " + endTime + "ms");
						toClient.writeLong(endTime);
						break;
					case EOS:
						System.out.println("Received " + count + " blocks");
						System.out.println("Closing connection...");
		
						if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
						fromClient.close();
						toClient.close();
						connectionSocket.close();
						break;
					case PING:
						Handler.handlePing(fromClient, toClient);
						break;
					default:
						sendInvalidPacketError();
				}
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		}
	}
	
	public void sendEstablishError() {
		// TODO: Implement error sending
	}
	
	public void sendInvalidPacketError() {
		// TODO: Implement error sending
	}
}
