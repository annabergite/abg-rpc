package rpc.abg.config;

import java.util.ArrayList;

public final class HostPort {
	public final String host;
	public final int port;

	private int hash;

	public static ArrayList<HostPort> parse(String str) {

		String[] array = str.split(",");
		ArrayList<HostPort> hostPortList = new ArrayList<>(array.length);

		for (int i = 0; i < array.length; i++) {
			hostPortList.add(new HostPort(array[i]));
		}

		return hostPortList;

	}

	public HostPort(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public HostPort(String hostAndPort) {
		String[] array = hostAndPort.split(":");
		this.host = array[0].trim();
		this.port = Integer.parseInt(array[1].trim());
	}

	@Override
	public int hashCode() {
		if (hash != 0) {
			return hash;
		}

		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;

		hash = result;

		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HostPort other = (HostPort) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}

}
