package rpc.abg.util.concurrent;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

//注意：用自定义的ThreadFactory替换netty默认实现，会导致性能下降，原因没找出来
public class AttachmentThreadFactory implements ThreadFactory {

	private static final AtomicInteger poolId = new AtomicInteger();

	private final AtomicInteger nextId = new AtomicInteger();
	private final String prefix;
	private final boolean daemon;
	private final int priority;
	protected final ThreadGroup threadGroup;

	public AttachmentThreadFactory() {
		this("AttachmentThreadFactory", false, Thread.NORM_PRIORITY);
	}

	public AttachmentThreadFactory(Class<?> poolType) {
		this(poolType, false, Thread.NORM_PRIORITY);
	}

	public AttachmentThreadFactory(String poolName) {
		this(poolName, false, Thread.NORM_PRIORITY);
	}

	public AttachmentThreadFactory(Class<?> poolType, boolean daemon) {
		this(poolType, daemon, Thread.NORM_PRIORITY);
	}

	public AttachmentThreadFactory(String poolName, boolean daemon) {
		this(poolName, daemon, Thread.NORM_PRIORITY);
	}

	public AttachmentThreadFactory(Class<?> poolType, int priority) {
		this(poolType, false, priority);
	}

	public AttachmentThreadFactory(String poolName, int priority) {
		this(poolName, false, priority);
	}

	public AttachmentThreadFactory(Class<?> poolType, boolean daemon, int priority) {
		this(toPoolName(poolType), daemon, priority);
	}

	public static String toPoolName(Class<?> poolType) {
		if (poolType == null) {
			throw new NullPointerException("poolType");
		}

		String poolName = simpleClassName(poolType);
		switch (poolName.length()) {
		case 0:
			return "unknown";
		case 1:
			return poolName.toLowerCase(Locale.US);
		default:
			if (Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1))) {
				return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
			} else {
				return poolName;
			}
		}
	}

	private static String simpleClassName(Class<?> clazz) {
		String className = checkNotNull(clazz, "clazz").getName();
		final int lastDotIdx = className.lastIndexOf('.');
		if (lastDotIdx > -1) {
			return className.substring(lastDotIdx + 1);
		}
		return className;
	}

	public AttachmentThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
		if (poolName == null) {
			throw new NullPointerException("poolName");
		}
		if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException(
					"priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
		}

		prefix = poolName + '-' + poolId.incrementAndGet() + '-';
		this.daemon = daemon;
		this.priority = priority;
		this.threadGroup = threadGroup;
	}

	public AttachmentThreadFactory(String poolName, boolean daemon, int priority) {
		this(poolName, daemon, priority, System.getSecurityManager() == null ? Thread.currentThread().getThreadGroup()
				: System.getSecurityManager().getThreadGroup());
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = newThread(r, prefix + nextId.incrementAndGet());
		try {
			if (t.isDaemon() != daemon) {
				t.setDaemon(daemon);
			}

			if (t.getPriority() != priority) {
				t.setPriority(priority);
			}
		} catch (Exception ignored) {
			// Doesn't matter even if failed to set.
		}
		return t;
	}

	protected Thread newThread(Runnable r, String name) {
		return new AttachmentThread(threadGroup, r, name);
	}

}
