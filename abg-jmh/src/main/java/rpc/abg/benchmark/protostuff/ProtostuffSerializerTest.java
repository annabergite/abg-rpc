package rpc.abg.benchmark.protostuff;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.serialization.protostuff.ProtostuffSerializer;

public class ProtostuffSerializerTest {
	public static void main(String[] args) throws IOException {
		ProtostuffSerializer serializer = new ProtostuffSerializer();

		UserService userService = new UserServiceServerImpl();

		ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
		ByteBuf byteBuf = allocator.directBuffer(16, 1024 * 1024 * 8);

		Request request = new Request();
		request.setRequestId(123);
		request.setServiceId(8);
		//request.setParams(new Object[] { Integer.valueOf(1), LocalDate.now(), userService.getUser(999).join() });

		serializer.writeRequest(byteBuf, request);

		byteBuf.readerIndex(4);
		System.out.println(serializer.readRequest(byteBuf));

		byteBuf.clear();

		Response response = new Response();
		response.setRequestId(321);
		response.setStatusCode((byte) 1);
		response.setResult(userService.listUser(0).join());

		serializer.writeResponse(byteBuf, response);

		byteBuf.readerIndex(4);
		System.out.println(serializer.readResponse(byteBuf));

	}
}
