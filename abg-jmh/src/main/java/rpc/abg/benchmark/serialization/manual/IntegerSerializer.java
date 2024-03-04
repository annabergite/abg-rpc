package rpc.abg.benchmark.serialization.manual;

import io.netty.buffer.ByteBuf;

public class IntegerSerializer implements Serializer<Integer> {

	@Override
	public void write(ByteBuf byteBuf, Integer value) {
		byteBuf.writeInt(value);
	}

	@Override
	public Integer read(ByteBuf byteBuf) {
		return byteBuf.readInt();
	}

	@Override
	public Class<Integer> typeClass() {
		return Integer.class;
	}

}
