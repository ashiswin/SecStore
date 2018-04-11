package server.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ServerConnector {
	private static ServerConnector instance = null;
	
	private static final String TABLE_NAME = "servers";
	private static final String COLUMN_IP = "ip";
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_SCRATCH = "scratch";
	@SuppressWarnings("unused")
	private static final String COLUMN_HEARTBEAT = "heartbeat";
	
	private Connection connect;
	private PreparedStatement heartbeatStatement;
	
	private ServerConnector() {
		try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager.getConnection("jdbc:mysql://devostrum.no-ip.info/secstore?user=secstore&password=secstore");
            
            heartbeatStatement = connect.prepareStatement("INSERT INTO " + TABLE_NAME + "(`" + COLUMN_IP + "`, `" + COLUMN_NAME + "`) VALUES(?, ?)"
            		+ "ON DUPLICATE KEY UPDATE `" + COLUMN_SCRATCH + "` = `" + COLUMN_SCRATCH + "` + 1, `" + COLUMN_NAME + "` = ?");
		} catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static ServerConnector getInstance() {
		if(instance == null) {
			instance = new ServerConnector();
		}
		
		return instance;
	}
	
	public void heartbeat(String ip, int port, String name) throws SQLException {
		heartbeatStatement.setString(1, ip + ":" + port);
		heartbeatStatement.setString(2, name);
		heartbeatStatement.setString(3, name);
		
		heartbeatStatement.executeUpdate();
	}
}
