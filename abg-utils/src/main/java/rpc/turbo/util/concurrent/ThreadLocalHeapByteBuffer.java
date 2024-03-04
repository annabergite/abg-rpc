package rpc.abg.util.concurrent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * very carefull
 * 
 * @author Annabergite
 *
 */
public class ThreadLocalHeapByteBuffer {
	private static final int ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();
	private static final Supplier<ByteBuffer> supplier = () -> ByteBuffer.allocate(1024 * 1024 * 2);

	public static ByteBuffer current() {
		ByteBuffer byteBuffer = AttachmentThreadUtils.getOrUpdate(ATTACHMENT_INDEX, supplier);

		byteBuffer.clear();
		return byteBuffer;
	}
}
