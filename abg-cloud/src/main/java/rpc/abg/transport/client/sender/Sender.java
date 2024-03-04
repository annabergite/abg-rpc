package rpc.abg.transport.client.sender;

import java.io.Closeable;

import rpc.abg.transport.client.future.RequestWithFuture;

public interface Sender extends Closeable {

	public void send(RequestWithFuture request);
}
