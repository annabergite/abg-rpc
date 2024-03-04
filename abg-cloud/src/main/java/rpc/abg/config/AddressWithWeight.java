package rpc.abg.config;

import java.nio.charset.StandardCharsets;

import rpc.abg.config.HostPort;
import rpc.abg.util.UnsafeStringUtils;

public class AddressWithWeight {
	public final HostPort address;
	public final int weight;

	public AddressWithWeight(HostPort address, int weight) {
		this.address = address;
		this.weight = weight;
	}

	public AddressWithWeight(byte[] bytes) {
		String str = new String(bytes, StandardCharsets.UTF_8);
		String[] array = str.split("@");

		this.address = new HostPort(array[0]);
		this.weight = Integer.parseInt(array[1]);
	}

	public byte[] toBytes() {
		return UnsafeStringUtils.getUTF8Bytes(toString());
	}

	@Override
	public String toString() {
		return address + "@" + weight;
	}

}
