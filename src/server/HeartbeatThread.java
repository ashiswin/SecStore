package server;

import java.sql.SQLException;

import server.model.ServerConnector;

public class HeartbeatThread extends Thread {
	String ip;
	int port;
	
	boolean running = true;
	
	public HeartbeatThread(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public void run() {
		ServerConnector s = ServerConnector.getInstance();
		
		while(running) {
			try {
				System.out.println("Sending heartbeat...");
				s.heartbeat(ip, port);
				Thread.sleep(10000);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopRunning() {
		running = false;
	}
}
