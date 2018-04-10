package common;

public enum Protocol {
	CP1,
	CP2;
	
	private static Protocol[] values = null;
    public static Protocol fromInt(int i) {
        if(Protocol.values == null) {
        	Protocol.values = Protocol.values();
        }
        return Protocol.values[i];
    }
}
