package server;

import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

public class Server {
	// Server configuration
	public static final int PORT = 4321;
	public static final String PRIVATE_KEY_FILE = "privateServer.der";
	public static final String SERVER_CERT_FILE = "server.crt";
	public static final String UPLOAD_DIR = "upload/";
	public static final boolean LOCAL_SERVER = true;
	public static final String WELCOME_MESSAGE = "Hello, this is SecStore!";
	
	// RSA constants
	public static final long MAX_KEY_LENGTH = 8192L;
	public static final String SHA1_WITH_RSA = "SHA1withRSA";
	public static final String SUN_JSSE = "SunJSSE";
	public static final String RSA = "RSA";
	
	// RSA data objects
	public static File keyFile;
	public static byte [] keyFileBytes;
	public static Signature dsa;
	public static KeyFactory keyFactory;
	public static PrivateKey privateKey;
	
	/*
	 * init(): Ensures the server environment is correct and loads in necessary data
	 */
	public static void init() {
		try {
			// Check for upload directory
			System.out.println("Checking for upload directory");
			File uploadDir = new File(UPLOAD_DIR);
			if(!uploadDir.exists()) {
				System.out.println("Creating upload directory");
				uploadDir.mkdir();
				System.out.println("Created upload directory");
			}
			else {
				System.out.println("Upload directory exists");
			}
			
			// Load private/public key pair
			System.out.println("Loading key pair from " + PRIVATE_KEY_FILE);
			keyFile = new File(PRIVATE_KEY_FILE);
			dsa = Signature.getInstance(SHA1_WITH_RSA, SUN_JSSE);
			keyFactory = KeyFactory.getInstance(RSA, SUN_JSSE);

			if (!keyFile.exists()) {
				System.err.println("Key file not found!");
				System.exit(-1);
			}
			
			if(keyFile.length() > MAX_KEY_LENGTH) {
				System.err.println("Key file is too big!");
				System.exit(-1);
			}
			
			FileInputStream is = new FileInputStream(keyFile);

			int offset = 0;
			int read = 0;
			keyFileBytes = new byte[(int) keyFile.length()];
			
			while (offset < keyFileBytes.length && (read = is.read(keyFileBytes, offset, keyFileBytes.length - offset)) >= 0 ) {
				offset += read;
			}
			
			is.close();
			
			System.out.println("Key file loaded");
			
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(keyFileBytes);
			privateKey = keyFactory.generatePrivate(privKeySpec);
			dsa.initSign(privateKey);
			
			System.out.println("Initialization complete\n\n");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		init();
		
		ServerSocket welcomeSocket = null;
		Socket connectionSocket = null;
		
		HeartbeatThread heartbeat = new HeartbeatThread((LOCAL_SERVER) ? Util.getLocalIP() : Util.getPublicIP(), PORT);
		heartbeat.start();
		
		try {
			System.out.println("Server starting on port " + PORT);
			welcomeSocket = new ServerSocket(PORT);
			System.out.println("Server started! Listening...\n\n");
			
			while(true) {
				// Accept a client and fire off a new thread to handler them
				connectionSocket = welcomeSocket.accept();
				new ServerThread(connectionSocket).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
