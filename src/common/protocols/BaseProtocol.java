package common.protocols;

import common.Protocol;

public abstract class BaseProtocol {
	Protocol protocol;
	
	public BaseProtocol(Protocol protocol) {
		this.protocol = protocol;
	}
	
	public abstract byte[] encrypt(byte[] plaintext);
	public abstract byte[] decrypt(byte[] ciphertext);
}
