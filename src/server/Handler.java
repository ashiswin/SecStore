package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;

import common.AES;
import common.Packet;
import common.Protocol;
import common.protocols.BaseProtocol;
import common.protocols.CP1;
import common.protocols.CP2;
import common.protocols.SplitChunks;
import server.model.FileConnector;
import server.model.UserConnector;

public class Handler {
	public static byte[] sign(byte[] message) throws SignatureException {
		Server.dsa.update(message);
		return Server.dsa.sign();
	}
	
	/*
	 * handleHelo(): Handle an initiation packet from the client and verify that it is valid
	 */
	public static boolean handleHelo(DataInputStream fromClient, DataOutputStream toClient, byte[] signedWelcome) throws IOException, SignatureException {
		System.out.println("Receiving hello");
		int numBytes = fromClient.readInt();
		byte[] helo = new byte[numBytes];
		fromClient.read(helo);
		int nonceBytes = fromClient.readInt();
		byte[] nonce = new byte[nonceBytes];
		fromClient.read(nonce);
		
		String heloString = new String(helo, 0, numBytes);
		String nonceString = new String(nonce, 0, nonceBytes);
		if(!heloString.equals("HELO")) {
			System.err.println("Invalid HELO received: " + heloString);
		}
		else {
			System.out.println("Sending welcome message...");
			byte[] signedNonce = sign(nonce);
			toClient.writeInt(signedNonce.length);
			toClient.write(signedNonce);
			//toClient.writeInt(signedWelcome.length);
			//toClient.write(signedWelcome);
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
	
	public static int[] handleAuth(DataInputStream fromClient, DataOutputStream toClient) throws IOException, SQLException {
		System.out.println("Received authentication request");
		int owner = fromClient.readInt();
		int numBytes = fromClient.readInt();
		byte[] sessionKey = new byte[numBytes];
		fromClient.read(sessionKey, 0, numBytes);
		String key = new String(sessionKey);
		
		UserConnector u = UserConnector.getInstance();
		ResultSet user = u.select(owner);
		user.next();
		
		String actualSessionKey = user.getString(UserConnector.COLUMN_SESSIONKEY);
		
		int[] response = new int[2];
		response[0] = (actualSessionKey.equals(key)) ? 1 : 0;
		response[1] = owner;
		if(response[0] == 1) {
			System.out.println("Upgraded connection to authenticated for user " + owner + " with session key " + key);
		}
		else {
			System.out.println("Authentication failed! " + actualSessionKey + " " + key);
		}
		return response;
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
			case SPLIT_CHUNKS:
				//Read Encrypted AES key
				numBytes = fromClient.readInt();
				encryptedAES = new byte[numBytes];
				fromClient.read(encryptedAES);
				//Decrypt AES key
				aesDecrypted = AES.decryptAESKey(encryptedAES,Server.privateKey);
				//Put into protocol
				protocol = new SplitChunks(aesDecrypted);
				System.out.println("Established protocol SplitChunks");
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
	public static BufferedOutputStream handleFilename(DataInputStream fromClient, DataOutputStream toClient, BaseProtocol protocol) throws IOException, SQLException {
		System.out.println("Receiving file...");
		
		int numBytes = fromClient.readInt();
		byte[] filename = new byte[numBytes];
		fromClient.read(filename);
		String filenameString = new String(filename, 0, numBytes);
		
		if(protocol.getProtocol() == Protocol.SPLIT_CHUNKS) {
			SplitChunks p = (SplitChunks) protocol;
			int owner = p.owner;
			long size = fromClient.readLong();
			int digestLength = fromClient.readInt();
			byte[] digest = new byte[digestLength];
			fromClient.read(digest);
			String md5 = Base64.getEncoder().encodeToString(digest);
			System.out.println("Creating file entry for " + owner + " of size " + size + " with checksum " + md5);
			
			FileConnector f = FileConnector.getInstance();
			int fileId = f.create(filenameString, owner, md5, size);
			toClient.writeInt(fileId);
			
			return null;
		}
		else {
			System.out.println("Saving file to " + Server.UPLOAD_DIR + filenameString);
			FileOutputStream fileOutputStream = new FileOutputStream(Server.UPLOAD_DIR + filenameString);
			return new BufferedOutputStream(fileOutputStream);
		}
	}
	
	/*
	 * handleFile(): Handle receiving a new chunk of a file
	 */
	public static boolean handleFile(DataInputStream fromClient, DataOutputStream toClient, BufferedOutputStream bufferedFileOutputStream, BaseProtocol protocol) throws IOException {
		if(protocol.getProtocol() == Protocol.CP2) {
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
				
				byte[] decryptedBytes = protocol.decrypt(block);
				bufferedFileOutputStream.write(decryptedBytes, 0, decryptedNumBytes);
				
				return true;
			}
		}
		
		return false;
	}
	
	public static Chunk handleChunk(DataInputStream fromClient, DataOutputStream toClient, BaseProtocol protocol) throws IOException {
		int fileId = fromClient.readInt();
		int chunkId = fromClient.readInt();
		int len = fromClient.readInt();
		byte[] ciphertext = new byte[len];
		
		int offset = 0;
		while(offset < len) {
			offset += fromClient.read(ciphertext, offset, len - offset);
		}
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(Server.UPLOAD_DIR + fileId + "." + chunkId)));
		byte[] decryptedBytes = protocol.decrypt(ciphertext);
		bos.write(decryptedBytes);
		bos.flush();
		bos.close();
		
		Chunk c = new Chunk();
		c.chunkId = chunkId;
		c.fileId = fileId;
		
		toClient.writeInt(Packet.CHUNK_RECV.getValue());
		return c;
	}
	
	public static void handleDownload(DataInputStream fromClient, DataOutputStream toClient, BaseProtocol protocol) throws IOException {
		int fileId = fromClient.readInt();
		
		// Open the file
		File file = new File(Server.UPLOAD_DIR + fileId);
		if(!file.exists()) {
			System.err.println("File has problem");
			System.exit(-1);
		}
		if(file.length() == 0) {
			System.err.println("Empty file");
			System.exit(-1);
		}
	    
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedInputStream bufferedFileInputStream = new BufferedInputStream(fileInputStream);
		
		byte[] fileBytes = Files.readAllBytes(Paths.get(Server.UPLOAD_DIR + fileId));
		byte[] encrypted = protocol.encrypt(fileBytes);
		
		toClient.writeInt(encrypted.length);
		toClient.write(encrypted, 0, encrypted.length);
		toClient.flush();
		
		bufferedFileInputStream.close();
		fileInputStream.close();
	}
	
	public static void handlePing(DataInputStream fromClient, DataOutputStream toClient) throws IOException {
		System.out.println("Received PING");
		toClient.writeInt(1);
		toClient.flush();
		System.out.println("Sent PONG");
	}
	
	public static void handleSendChunk(DataInputStream fromClient, DataOutputStream toClient) throws IOException, SQLException, NoSuchAlgorithmException {
		System.out.println("Received chunk from fellow server");
		int fileId = fromClient.readInt();
		
		if((new File(Server.UPLOAD_DIR + fileId).exists())) {
			System.out.println("Already have full file! Rejecting");
			return;
		}
		
		int chunkId = fromClient.readInt();
		int len = fromClient.readInt();
		byte[] chunk = new byte[len];
		
		int offset = 0;
		while(offset < len) {
			offset += fromClient.read(chunk, offset, len - offset);
		}
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(Server.UPLOAD_DIR + fileId + "." + chunkId)));
		bos.write(chunk);
		bos.flush();
		bos.close();
		
		File dir = new File(Server.UPLOAD_DIR);
		File[] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.startsWith(fileId + ".");
		    }
		});
		Arrays.sort(files);
		long totalLength = 0;
		for(File f : files) {
		    totalLength += f.length();
		}
		
		ResultSet r = FileConnector.getInstance().select(fileId);
		r.next();
		
		long actualLength = r.getLong(FileConnector.COLUMN_SIZE);
		String actualChecksum = r.getString(FileConnector.COLUMN_CHECKSUM);
		
		if(totalLength == actualLength) {
			System.out.println("Complete file received! Splicing...");
			bos = new BufferedOutputStream(new FileOutputStream(new File(Server.UPLOAD_DIR + fileId)));
			for(File f : files) {
				bos.write(Files.readAllBytes(Paths.get(f.getPath())));
			}
			bos.flush();
			bos.close();
			System.out.println("File spliced. Checking checksum...");
			MessageDigest md = MessageDigest.getInstance("MD5");
			try (InputStream is = new FileInputStream(new File(Server.UPLOAD_DIR + fileId)); DigestInputStream dis = new DigestInputStream(is, md)) 
			{
				dis.readAllBytes();
			}
			byte[] digest = md.digest();
			String checksum = Base64.getEncoder().encodeToString(digest);
			if(checksum.equals(actualChecksum)) {
				System.out.println("Checksum passed! Deleting chunk files");
				for(File f : files) {
					f.delete();
				}
				System.out.println("All chunk files cleaned! File completely received!");
			}
		}
	}
}
