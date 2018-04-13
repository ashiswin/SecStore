package client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import common.AES;
import common.Packet;
import common.Protocol;
import common.protocols.SplitChunks;

public class SplitChunksClient {
	private static final String CA_CERT_PATH = "CA.crt";
	private static final String HELO = "HELO";
	private static final String WELCOME_MESSAGE = "Hello, this is SecStore!";
	private static final String SHA1_WITH_RSA = "SHA1withRSA";
	private static final String SUN_JSSE = "SunJSSE";
	private static final String SERVER_LIST = "http://www.secstore.stream/Servers.php";
	
	private static final long THRESHOLD = 200;
	private static Integer progress = 0;
	private static int servers = 0;
	
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
	public static ArrayList<Socket> findServer() throws UnknownHostException, IOException, JSONException {
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
			
			ArrayList<Socket> okServers = new ArrayList<>();
			
			for(int i = 0; i < servers.length(); i++) {
				JSONObject server = servers.getJSONObject(i);
				long rtt = ping(server.getString("ip"));
				if(rtt < THRESHOLD) {
					int colon = server.getString("ip").indexOf(":");
					okServers.add(new Socket(server.getString("ip").substring(0, colon), Integer.parseInt(server.getString("ip").substring(colon + 1))));
				}
			}
			return okServers;
		} else {
			System.out.println("Unable to get server list");
		}

		return null;
	}

	public static void main(String[] args) {
		String filename = "/home/ashiswin/randomdoc.pdf";

		long timeStarted = System.nanoTime();

		runSplitChunks(filename);

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
	
	public static JSONObject authRequest(String username,String password){
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("http://www.secstore.stream/Authenticate.php").openConnection();
            
            String urlParameters = "username=" + username + "&password=" + password;
            
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", urlParameters.getBytes().length + "");
            con.setRequestMethod("POST");

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(urlParameters.getBytes());

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject isAuth = new JSONObject(response.toString());
            return isAuth;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
	
	public static void sendWithSplitChunks(File file, ArrayList<Socket> clientSockets, int fileId, SecretKey aes, JSONObject auth) throws IOException {
		long size = file.length();
		byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
		ChunkThread[] threads = new ChunkThread[clientSockets.size()];
		
		servers = clientSockets.size();
		
		int step;
		if(size % clientSockets.size() == 0) {
			step = (int) (size / clientSockets.size());
		}
		else {
			step = (int) (size / (clientSockets.size() - 1));
		}
		for(int i = 0; i < clientSockets.size(); i++) {
			threads[i] = new ChunkThread(clientSockets.get(i), Arrays.copyOfRange(fileBytes, (int) i * step, (int) Math.min(size, (i + 1) * step)), fileId, i, aes, auth);
			threads[i].start();
		}
		
		for(int i = 0; i < clientSockets.size(); i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void updateProgress() {
		System.out.println("Progress: " + (progress * 100.0 / servers) + "%");
	}
	
	public static void runSplitChunks(String filename){
		try {
			ArrayList<Socket> clientSockets = null;
			
			DataOutputStream toServer = null;
			DataInputStream fromServer = null;
			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSockets = findServer();
			if(clientSockets == null) {
				System.exit(-1);
			}
			
			Socket clientSocket = clientSockets.get(0);
			
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());
			System.out.println("Sending HELO to " + clientSocket.getInetAddress() + "...");
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

			System.out.println("Authenticating");
			JSONObject auth = authRequest("ashiswin", "terror56");
			toServer.writeInt(Packet.AUTH.getValue());
			toServer.writeInt(auth.getInt("id"));
			toServer.writeInt(auth.getString("key").getBytes().length);
			toServer.write(auth.getString("key").getBytes());
			System.out.println("Authenticated");
			//Generate AES key
			SecretKey aes = AES.generateKey();
			//Encrypt AES key
			byte[] aesEncrypted = AES.encryptAESKey(aes,serverCert.getPublicKey());

			System.out.println("Establishing protocol");
			toServer.writeInt(Packet.PROTOCOL.getValue());
			toServer.writeInt(Protocol.SPLIT_CHUNKS.ordinal());
			//Send the number of bytes of  encrypted aes key
			toServer.writeInt(aesEncrypted.length);
			//Send encrypted aes key
			toServer.write(aesEncrypted);

			System.out.println("Established protocol SPLIT_CHUNKS");

			System.out.println("Sending file metadata...");

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
			
			// Send the filename
			toServer.writeInt(Packet.FILENAME.getValue());
			toServer.writeInt(filename.substring(filename.lastIndexOf("/") + 1).getBytes().length);
			toServer.write(filename.substring(filename.lastIndexOf("/") + 1).getBytes());
			toServer.flush();
			
			toServer.writeLong(file.length());
			MessageDigest md = MessageDigest.getInstance("MD5");
			try (InputStream is = new FileInputStream(file); DigestInputStream dis = new DigestInputStream(is, md)) 
			{
				dis.readAllBytes();
			}
			byte[] digest = md.digest();
			toServer.writeInt(digest.length);
			toServer.write(digest);
			toServer.flush();
			
			int fileId = fromServer.readInt();
			System.out.println("Allocated file id " + fileId);
			sendWithSplitChunks(file, clientSockets, fileId, aes, auth);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static class ChunkThread extends Thread {
		Socket socket;
		byte[] data;
		int fileId;
		int chunkId;
		SecretKey aes;
		JSONObject auth;
		
		public ChunkThread(Socket socket, byte[] data, int fileId, int chunkId, SecretKey aes, JSONObject auth) {
			this.socket = socket;
			this.data = data;
			this.fileId = fileId;
			this.chunkId = chunkId;
			this.aes = aes;
			this.auth = auth;
			
			System.out.println("Created new chunk thread handling " + data.length + " bytes");
		}
		
		public void run() {
			try {
				DataInputStream fromServer = new DataInputStream(socket.getInputStream());
				DataOutputStream toServer = new DataOutputStream(socket.getOutputStream());
				
				if(chunkId != 0) {
					System.out.println("Sending HELO to " + socket.getInetAddress() + "...");
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
					System.out.println("Authenticating");
					toServer.writeInt(Packet.AUTH.getValue());
					toServer.writeInt(auth.getInt("id"));
					toServer.writeInt(auth.getString("key").getBytes().length);
					toServer.write(auth.getString("key").getBytes());
					System.out.println("Authenticated");
					//Encrypt AES key
					byte[] aesEncrypted = AES.encryptAESKey(aes,serverCert.getPublicKey());

					System.out.println("Establishing protocol");
					toServer.writeInt(Packet.PROTOCOL.getValue());
					toServer.writeInt(Protocol.SPLIT_CHUNKS.ordinal());
					//Send the number of bytes of  encrypted aes key
					toServer.writeInt(aesEncrypted.length);
					//Send encrypted aes key
					toServer.write(aesEncrypted);

					System.out.println("Established protocol SPLIT_CHUNKS");
				}
				
				SplitChunks protocol = new SplitChunks(aes);
				toServer.writeInt(Packet.CHUNK.getValue());
				byte[] encrypted = protocol.encrypt(data);
				
				toServer.writeInt(fileId);
				toServer.writeInt(chunkId);
				toServer.writeInt(encrypted.length);
				toServer.write(encrypted, 0, encrypted.length);
				toServer.flush();
				
				int received = fromServer.readInt();
				synchronized(progress) {
					progress += 1;
					updateProgress();
				}
				toServer.writeInt(Packet.EOS.getValue());
				System.out.println("Closing connection");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
