package application.client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.crypto.SecretKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import client.ClientWithSecurity;
import common.AES;
import common.Packet;
import common.Protocol;
import common.protocols.CP2;

public class DownloadClient {
	private static final String CA_CERT_PATH = "CA.crt";
	private static final String HELO = "HELO";
	private static final String WELCOME_MESSAGE = "Hello, this is SecStore!";
	private static final String SHA1_WITH_RSA = "SHA1withRSA";
	private static final String SUN_JSSE = "SunJSSE";
	private static final String SERVER_LIST = "http://www.secstore.stream/Servers.php";
	
	private static X509Certificate serverCert;
	public static long ping(String ipAddress) {
		try {
			System.out.print("Pinging " + ipAddress + "... ");
			int colon = ipAddress.indexOf(":");
			Socket ping = new Socket(ipAddress.substring(0, colon), Integer.parseInt(ipAddress.substring(colon + 1)));
			DataInputStream fromServer = new DataInputStream(ping.getInputStream());
			DataOutputStream toServer = new DataOutputStream(ping.getOutputStream());
			long start = System.currentTimeMillis();
			
			toServer.writeInt(Packet.PING.getValue());
			fromServer.readInt();
			long end = System.currentTimeMillis() - start;
			
			toServer.writeInt(Packet.EOS.getValue());
			
			fromServer.close();
			toServer.close();
			ping.close();
			
			System.out.println(end + "ms");
			return end;
			
	    } catch ( Exception e ) {
	    	//System.out.println("Exception:" + e.getMessage());
	    	e.printStackTrace();
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
				if(rtt >= 0 && rtt < min) {
					min = rtt;
					minServer = i;
				}
			}
			
			JSONObject server = servers.getJSONObject(minServer);
			System.out.println("Using server " + server.getString("ip") + "\n\n");
			int colon = server.getString("ip").indexOf(":");
			return new Socket(server.getString("ip").substring(0, colon), Integer.parseInt(server.getString("ip").substring(colon + 1)));
		} else {
			System.out.println("Unable to get server list");
		}

		return null;
	}
	
	public static void main(String[] args) {
		String filename = "/home/ashiswin/randomdoc2.pdf";

		long timeStarted = System.nanoTime();

		runDownload(60, filename);

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
	
	public static void download(int fileId, String localFile, DataOutputStream toServer, DataInputStream fromServer, SecretKey aes) {
		try {
			// Send the filename
			toServer.writeInt(Packet.DOWNLOAD.getValue());
			toServer.writeInt(fileId);
			toServer.flush();

			File file = new File(localFile);
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			BufferedOutputStream bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);
			CP2 protocol = new CP2(aes);
			
			int numBytes = fromServer.readInt();
			byte[] fileBytes = new byte[numBytes];
			int offset = 0;
			
			while(numBytes != offset) {
				offset += fromServer.read(fileBytes, offset, numBytes - offset);
			}
			byte[] decrypted = protocol.decrypt(fileBytes);
			
			bufferedFileOutputStream.write(decrypted);
			
			bufferedFileOutputStream.close();
			fileOutputStream.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	public static void runDownload(int fileId, String filename){
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
			System.out.println("Generating nonce...");
			Random random = new Random();
			String nonce = "";
			for(int i = 0; i < 10; i++) {
				nonce += random.nextInt(10);
			}
			
			System.out.println("Sending HELO...");
			toServer.writeInt(Packet.HELO.getValue());
			toServer.writeInt(HELO.getBytes().length);
			toServer.write(HELO.getBytes());
			toServer.writeInt(nonce.getBytes().length);
			toServer.write(nonce.getBytes());
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
			dsa.update(nonce.getBytes("UTF-8"));
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

			System.out.println("Receiving file...");

			download(fileId, filename, toServer, fromServer, aes);

			System.out.println("Closing connection...");
			toServer.writeInt(Packet.EOS.getValue());
			toServer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
