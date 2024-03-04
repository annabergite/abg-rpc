package rpc.abg.config.server;

import static rpc.abg.config.ConfigUtils.getStringOrElse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import com.typesafe.config.Config;

import rpc.abg.config.HostPort;
import rpc.abg.registry.DirectConnectRegister;
import rpc.abg.registry.Register;

public class RegisterConfig {
	private Protocol protocol;
	private HostPort serverAddress;
	private int serverWeight;
	private Register register;

	public Register getRegister() {
		return register;
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public HostPort getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(HostPort serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getServerWeight() {
		return serverWeight;
	}

	public void setServerWeight(int serverWeight) {
		this.serverWeight = serverWeight;
	}

	@Override
	public String toString() {
		return "RegisterConfig{" + //
				"register=" + register.getClass().getName() + //
				", protocol=" + protocol + //
				", serverAddress=" + serverAddress + //
				", serverWeight=" + serverWeight + //
				'}';
	}

	static RegisterConfig parse(Config config)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException {

		String serverAddressStr = config.getString("server.address");
		HostPort serverAddress = new HostPort(serverAddressStr);
		int serverWeight = config.getInt("server.weight");

		String protocolStr = getStringOrElse(config, "server.protocol", Protocol.RPC.name());
		Protocol protocol = Protocol.valueOf(protocolStr);

		String registerClass = getStringOrElse(config, "register.class", DirectConnectRegister.class.getName());
		List<String> registerAddressList = config.getStringList("register.address");

		Register register = (Register) Class//
				.forName(registerClass)//
				.getDeclaredConstructor()//
				.newInstance();

		List<HostPort> hostPorts = registerAddressList//
				.stream()//
				.map(str -> new HostPort(str))//
				.collect(Collectors.toList());

		register.init(hostPorts);

		RegisterConfig registerConfig = new RegisterConfig();
		registerConfig.setRegister(register);
		registerConfig.setProtocol(protocol);
		registerConfig.setServerAddress(serverAddress);
		registerConfig.setServerWeight(serverWeight);

		return registerConfig;
	}

}
