package rpc.abg.util.concurrent;

import java.util.function.Supplier;

/**
 * very carefull
 * 
 * @author Annabergite
 *
 */
public class ThreadLocalStringBuilder {
	private static final int ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();
	private static final Supplier<StringBuilder> supplier = () -> new StringBuilder();

	public static StringBuilder current() {
		StringBuilder builder = AttachmentThreadUtils.getOrUpdate(ATTACHMENT_INDEX, supplier);

		// 防止内存溢出
		if (builder.capacity() > 1024 * 1024 * 2) {
			builder = new StringBuilder();
			AttachmentThreadUtils.put(ATTACHMENT_INDEX, builder);
		}

		builder.setLength(0);
		return builder;
	}
}
