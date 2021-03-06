package server.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserConnector {
	private static UserConnector instance = null;
	
	private static final String TABLE_NAME = "users";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_USERNAME = "username";
	public static final String COLUMN_PASSWORD = "passwordHash";
	public static final String COLUMN_SALT = "salt";
	public static final String COLUMN_SESSIONKEY = "sessionKey";
	public static final String COLUMN_VALID = "valid";
	
	private Connection connect;
	private PreparedStatement registerStatement;
	private PreparedStatement selectByUsernameStatement;
	private PreparedStatement select;
	
	private UserConnector() {
		try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager.getConnection("jdbc:mysql://devostrum.no-ip.info/secstore?user=secstore&password=secstore");
            
            registerStatement = connect.prepareStatement("INSERT INTO " + TABLE_NAME + "(`" + COLUMN_NAME + "`, `" + COLUMN_USERNAME + "`, `" + COLUMN_PASSWORD + "`, `" + COLUMN_SALT + "`"
            		+ " VALUES(?, ?, ?, ?)");
            select = connect.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE `" + COLUMN_ID + "`=?");
            selectByUsernameStatement = connect.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE `" + COLUMN_USERNAME + "` = ?");
		} catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static UserConnector getInstance() {
		if(instance == null) {
			instance = new UserConnector();
		}
		
		return instance;
	}
	
	public void register(String name, String username, String passwordHash, String salt) throws SQLException {
		registerStatement.setString(1, name);
		registerStatement.setString(2, username);
		registerStatement.setString(3, passwordHash);
		registerStatement.setString(4, salt);
		
		registerStatement.executeUpdate();
	}
	
	public ResultSet selectByUsername(String username) throws SQLException {
		selectByUsernameStatement.setString(1, username);
        return selectByUsernameStatement.executeQuery();
	}
	
	public ResultSet select(int id) throws SQLException {
		select.setInt(1, id);
		return select.executeQuery();
	}
}
