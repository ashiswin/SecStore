package common;

public enum Packet {
	// Packets from client to server
	HELO(1), // Initiate communication
	HELO_ACK(2), // Send signed welcome message
	CERT(3), // Request server certificate
	AUTH(4), // Upgrade connection to authenticated communication
	FILENAME(5), // Transmit filename for new file transfer
	FILE(6), // Transmit file chunk
	EOF(7), // Indicate file completion
	EOS(8) // End of session, de-authenticate session token
	
	// Packets from server to client
	
	;
	
	private int value;
	
	private Packet(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
	
	public static Packet fromId(int id) {
        for (Packet type : Packet.values()) {
            if (type.getValue() == id) {
                return type;
            }
        }
        return null;
    }
}
