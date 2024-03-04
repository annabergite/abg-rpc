package rpc.abg.benchmark.serialization.manual;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import rpc.abg.util.UnsafeStringUtils;
import rpc.abg.util.concurrent.ThreadLocalBytes;

public class StringSerializer implements Serializer<String> {

	@Override
	public void write(ByteBuf byteBuf, String str) {
		byte[] bytes = UnsafeStringUtils.getUTF8Bytes(str); // str.getBytes(StandardCharsets.UTF_8);
		byteBuf.writeInt(bytes.length);
		byteBuf.writeBytes(bytes);
	}

	@Override
	public String read(ByteBuf byteBuf) {
		int length = byteBuf.readInt();
		byte[] bytes = ThreadLocalBytes.current();
		byteBuf.readBytes(bytes, 0, length);

		return new String(bytes, 0, length, StandardCharsets.UTF_8);
	}

	@Override
	public Class<String> typeClass() {
		return String.class;
	}

}
