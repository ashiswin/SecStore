package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import common.Packet;

public class Broadcaster {
	public static void broadcastChunk(Chunk c) throws IOException, JSONException {
		URL obj = new URL(Server.SERVER_LIST);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		int responseCode = con.getResponseCode();
		System.out.println("Got server list from " + Server.SERVER_LIST);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			JSONArray servers = new JSONArray(response.toString());
			BroadcasterThread[] threads = new BroadcasterThread[servers.length()];
			
			byte[] data = Files.readAllBytes(Paths.get(Server.UPLOAD_DIR + c.fileId + "." + c.chunkId));
			
			for(int i = 0; i < servers.length(); i++) {
				JSONObject server = servers.getJSONObject(i);
				int colon = server.getString("ip").indexOf(":");
				Socket serverSocket = new Socket(server.getString("ip").substring(0, colon), Integer.parseInt(server.getString("ip").substring(colon + 1)));
				threads[i] = new BroadcasterThread(serverSocket, c, data);
				threads[i].start();
			}
			
			for(int i = 0; i < servers.length(); i++) {
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	static class BroadcasterThread extends Thread {
		Socket server;
		Chunk c;
		byte[] data;
		
		public BroadcasterThread(Socket server, Chunk c, byte[] data) {
			this.server = server;
			this.c = c;
			this.data = data;
		}
		
		public void run() {
			try {
				System.out.println("Broadcasting to " + server.getInetAddress());
				DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
				
				toServer.writeInt(Packet.SEND_CHUNK.getValue());
				toServer.writeInt(c.fileId);
				toServer.writeInt(c.chunkId);
				toServer.writeInt(data.length);
				toServer.write(data);
				toServer.flush();
				toServer.writeInt(Packet.EOS.getValue());
				System.out.println("Broadcasted to " + server.getInetAddress());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
