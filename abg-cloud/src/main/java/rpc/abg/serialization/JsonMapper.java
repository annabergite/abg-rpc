package rpc.abg.serialization;

import java.io.IOException;

import io.netty.buffer.ByteBuf;

public interface JsonMapper {

	/**
	 * 读取对象
	 * 
	 * @param buffer
	 * @param type
	 * @return
	 */
	<T> T read(ByteBuf buffer, Class<T> type) throws IOException;

	/**
	 * 写入对象
	 * 
	 * @param buffer
	 * @param value
	 */
	void write(ByteBuf buffer, Object value) throws IOException;
}
