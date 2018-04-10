package client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;

import common.protocols.CP2;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import common.Packet;
import common.Protocol;
import common.protocols.CP1;
import common.AES;

import javax.crypto.SecretKey;

public class ClientWithSecurity {
	private static final String CA_CERT_PATH = "CA.crt";
	private static final String HELO = "HELO";
	private static final String WELCOME_MESSAGE = "Hello, this is SecStore!";
	private static final String SHA1_WITH_RSA = "SHA1withRSA";
	private static final String SUN_JSSE = "SunJSSE";
	private static final String SERVER_LIST = "http://www.secstore.stream/Servers.php";
	
	private static X509Certificate serverCert;
	public static long ping(String ipAddress) {
		try {
			InetAddress inet = InetAddress.getByName(ipAddress);
		 
			System.out.println("Sending Ping Request to " + ipAddress);
	 
			long finish = 0;
			long start = new GregorianCalendar().getTimeInMillis();
	 
			if (inet.isReachable(5000)){
				finish = new GregorianCalendar().getTimeInMillis();
				System.out.println("Ping RTT: " + (finish - start + "ms"));
				
				return finish;
			} else {
				System.out.println(ipAddress + " NOT reachable.");
			}
	    } catch ( Exception e ) {
	    	System.out.println("Exception:" + e.getMessage());
	    }
		
		return -1;
	}
	/*
	 * Download list of servers from main server. Attempt to connect to one with best ping.
	 */
	public static Socket findServer() throws UnknownHostException, IOException, JSONException {
		URL obj = new URL(SERVER_LIST);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			JSONArray servers = new JSONArray(response.toString());
			if(servers.length() == 0) {
				System.err.println("No servers available");
				return null;
			}
			long min = 10000000;
			int minServer = 0;
			
			for(int i = 0; i < servers.length(); i++) {
				JSONObject server = servers.getJSONObject(i);
				long rtt = ping(server.getString("ip"));
				if(rtt > 0 && rtt < min ) {
					min = rtt;
					minServer = i;
				}
			}
			
			JSONObject server = servers.getJSONObject(minServer);
			System.out.println("Using server " + server.getString("ip") + ":" + server.getInt("port") + "\n\n");
			
			return new Socket(server.getString("ip"), server.getInt("port"));
		} else {
			System.out.println("Unable to get server list");
		}

		return null;
	}

	public static void main(String[] args) {
		String filename = "./rr.txt";

		long timeStarted = System.nanoTime();

		runCP1(filename);

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
	
	public static void sendWithCP1(String filename, DataOutputStream toServer, DataInputStream fromServer) {
		try {
			// Send the filename
			toServer.writeInt(Packet.FILENAME.getValue());
			toServer.writeInt(filename.substring(filename.lastIndexOf("/") + 1).getBytes().length);
			toServer.write(filename.substring(filename.lastIndexOf("/") + 1).getBytes());
			toServer.flush();

			// Open the file
			File file = new File(filename);
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
			CP1 protocol = new CP1(serverCert.getPublicKey());
			byte [] fromFileBuffer = new byte[117];
			int numBytes = 0;
			
			// Send the file
			int count = 0;
			for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < fromFileBuffer.length;

				byte[] encryptedBytes = protocol.encrypt(fromFileBuffer);
				
				toServer.writeInt(Packet.FILE.getValue());
				toServer.writeInt(numBytes);
				toServer.writeInt(encryptedBytes.length);
				toServer.write(encryptedBytes, 0, encryptedBytes.length);
				
				toServer.flush();
				count++;
			}
			System.out.println("Sent " + count + " blocks");
			bufferedFileInputStream.close();
			fileInputStream.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void runCP1(String filename){
		try {
			Socket clientSocket = null;

			DataOutputStream toServer = null;
			DataInputStream fromServer = null;
			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = findServer();
			if(clientSocket == null) {
				System.exit(-1);
			}
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());
			System.out.println("Sending HELO...");
			toServer.writeInt(Packet.HELO.getValue());
			toServer.writeInt(HELO.getBytes().length);
			toServer.write(HELO.getBytes());
			toServer.flush();

			int welcomeLength = fromServer.readInt();
			byte[] welcome = new byte[welcomeLength];
			fromServer.read(welcome);

			System.out.println("Received welcome!");
			System.out.println("Requesting server certificate...");

			toServer.writeInt(Packet.CERT.getValue());
			toServer.flush();

			int certLength = fromServer.readInt();
			byte[] cert = new byte[certLength];
			fromServer.read(cert);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));

			System.out.println("Received server certificate!");
			System.out.println("Verifying server certificate with " + CA_CERT_PATH + "...");
			X509Certificate CAcert = (X509Certificate) cf.generateCertificate(ClientWithSecurity.class.getResourceAsStream(CA_CERT_PATH));
			PublicKey key = CAcert.getPublicKey();

			serverCert.checkValidity();
			serverCert.verify(key);

			System.out.println("Verified server certificate!");
			System.out.println("Verifying welcome message...");

			Signature dsa = Signature.getInstance(SHA1_WITH_RSA, SUN_JSSE);
			dsa.initVerify(serverCert.getPublicKey());
			dsa.update(WELCOME_MESSAGE.getBytes("UTF-8"));
			if(!dsa.verify(welcome)) {
				System.err.println("Verification failed! Terminating file transfer");
				System.exit(-1);
			}

			System.out.println("Verified welcome message!");

			System.out.println("Establishing protocol");
			toServer.writeInt(Packet.PROTOCOL.getValue());
			toServer.writeInt(Protocol.CP1.ordinal());
			System.out.println("Established protocol CP1");

			System.out.println("Sending file...");

			sendWithCP1(filename, toServer, fromServer);

			System.out.println("Closing connection...");
			toServer.writeInt(Packet.EOS.getValue());
			toServer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void sendWithCP2(String filename, DataOutputStream toServer, DataInputStream fromServer, SecretKey aes) {
		// TODO: Implement CP2 (exchange AES key and encrypt with AES)
		try {
			// Send the filename
			toServer.writeInt(Packet.FILENAME.getValue());
			toServer.writeInt(filename.substring(filename.lastIndexOf("/") + 1).getBytes().length);
			toServer.write(filename.substring(filename.lastIndexOf("/") + 1).getBytes());
			toServer.flush();

			// Open the file
			File file = new File(filename);
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
			CP2 protocol = new CP2(aes);
			byte [] fromFileBuffer = new byte[117];
			int numBytes = 0;

			// Send the file
			int count = 0;
			for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < fromFileBuffer.length;

				byte[] encryptedBytes = protocol.encrypt(fromFileBuffer);

				toServer.writeInt(Packet.FILE.getValue());
				toServer.writeInt(numBytes);
				toServer.writeInt(encryptedBytes.length);
				toServer.write(encryptedBytes, 0, encryptedBytes.length);

				toServer.flush();
				count++;
			}
			System.out.println("Sent " + count + " blocks");
			bufferedFileInputStream.close();
			fileInputStream.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	public static void runCP2(String filename){
		try {
			Socket clientSocket = null;

			DataOutputStream toServer = null;
			DataInputStream fromServer = null;
			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = findServer();
			if(clientSocket == null) {
				System.exit(-1);
			}
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());
			System.out.println("Sending HELO...");
			toServer.writeInt(Packet.HELO.getValue());
			toServer.writeInt(HELO.getBytes().length);
			toServer.write(HELO.getBytes());
			toServer.flush();

			int welcomeLength = fromServer.readInt();
			byte[] welcome = new byte[welcomeLength];
			fromServer.read(welcome);

			System.out.println("Received welcome!");
			System.out.println("Requesting server certificate...");

			toServer.writeInt(Packet.CERT.getValue());
			toServer.flush();

			int certLength = fromServer.readInt();
			byte[] cert = new byte[certLength];
			fromServer.read(cert);

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			serverCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));

			System.out.println("Received server certificate!");
			System.out.println("Verifying server certificate with " + CA_CERT_PATH + "...");
			X509Certificate CAcert = (X509Certificate) cf.generateCertificate(ClientWithSecurity.class.getResourceAsStream(CA_CERT_PATH));
			PublicKey key = CAcert.getPublicKey();

			serverCert.checkValidity();
			serverCert.verify(key);

			System.out.println("Verified server certificate!");
			System.out.println("Verifying welcome message...");

			Signature dsa = Signature.getInstance(SHA1_WITH_RSA, SUN_JSSE);
			dsa.initVerify(serverCert.getPublicKey());
			dsa.update(WELCOME_MESSAGE.getBytes("UTF-8"));
			if(!dsa.verify(welcome)) {
				System.err.println("Verification failed! Terminating file transfer");
				System.exit(-1);
			}

			System.out.println("Verified welcome message!");

			//Generate AES key
			SecretKey aes = AES.generateKey();
			//Encrypt AES key
			byte[] aesEncrypted = AES.encryptAESKey(aes,serverCert.getPublicKey());

			System.out.println("Establishing protocol");
			toServer.writeInt(Packet.PROTOCOL.getValue());
			toServer.writeInt(Protocol.CP2.ordinal());
			//Send the number of bytes of  encrypted aes key
			toServer.writeInt(aesEncrypted.length);
			//Send encrypted aes key
			toServer.write(aesEncrypted);



			System.out.println("Established protocol CP2");

			System.out.println("Sending file...");

			sendWithCP2(filename, toServer, fromServer, aes);

			System.out.println("Closing connection...");
			toServer.writeInt(Packet.EOS.getValue());
			toServer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
