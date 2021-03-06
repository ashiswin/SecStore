package server.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FileConnector {
	private static FileConnector instance = null;
	
	private static final String TABLE_NAME = "files";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_FILENAME = "filename";
	public static final String COLUMN_OWNER = "owner";
	public static final String COLUMN_CHECKSUM = "checksum";
	public static final String COLUMN_SIZE = "size";
	
	private Connection connect;
	private PreparedStatement selectStatement;
	private PreparedStatement createStatement;
	
	private FileConnector() {
		try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager.getConnection("jdbc:mysql://devostrum.no-ip.info/secstore?user=secstore&password=secstore");
            
            selectStatement = connect.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE `" + COLUMN_ID + "` = ?");
            createStatement = connect.prepareStatement("INSERT INTO " + TABLE_NAME + "(`" + COLUMN_FILENAME + "`, `" + COLUMN_OWNER + "`, `" + COLUMN_CHECKSUM + "`, `" + COLUMN_SIZE + "`)"
            		+ " VALUES(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		} catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public static FileConnector getInstance() {
		if(instance == null) {
			instance = new FileConnector();
		}
		
		return instance;
	}
	
	public int create(String filename, int owner, String checksum, long size) throws SQLException {
		createStatement.setString(1, filename);
		createStatement.setInt(2, owner);
		createStatement.setString(3, checksum);
		createStatement.setLong(4, size);
		createStatement.executeUpdate();
		ResultSet r = createStatement.getGeneratedKeys();
		r.next();
		
		return r.getInt(1);
	}
	
	public ResultSet select(int id) throws SQLException {
		selectStatement.setInt(1, id);
		return selectStatement.executeQuery();
	}
}
