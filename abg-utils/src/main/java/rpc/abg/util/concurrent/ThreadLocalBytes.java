package rpc.abg.util.concurrent;

import java.util.function.Supplier;

/**
 * very carefull
 * 
 * @author Annabergite
 *
 */
public final class ThreadLocalBytes {
	private static final int ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();
	private static final Supplier<byte[]> supplier = () -> new byte[1024 * 1024 * 2];

	public static byte[] current() {
		return AttachmentThreadUtils.getOrUpdate(ATTACHMENT_INDEX, supplier);
	}
}
