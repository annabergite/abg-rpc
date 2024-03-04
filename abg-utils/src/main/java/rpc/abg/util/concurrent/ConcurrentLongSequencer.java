package rpc.abg.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 高性能序号生成器，是AtomicLong性能的10x，<br>
 * 实现原理为每个线程持有100个数字，线程内简单递增，<br>
 * 该生成器生成数字整体上并不是简单递增的，仅适用于特定场景
 * 
 * @author Annabergite
 *
 */
public final class ConcurrentLongSequencer {
	private static final int BATCH_INCREMENT = 100;

	private final AtomicLong rootCounter;
	private final Supplier<CellCounter> supplier;
	private final int attachmentIndex = AttachmentThreadUtils.nextVarIndex();

	public ConcurrentLongSequencer() {
		this(0);
	}

	public ConcurrentLongSequencer(long initialValue) {
		this(initialValue, false);
	}

	public ConcurrentLongSequencer(long initialValue, boolean nonNegative) {
		this.rootCounter = new AtomicLong(initialValue);
		this.supplier = () -> new CellCounter(rootCounter, nonNegative);
	}

	public long next() {// 性能受制于AttachmentThreadUtils.getOrUpdate
		CellCounter cellCounter = AttachmentThreadUtils.getOrUpdate(attachmentIndex, supplier);
		return cellCounter.next();
	}

	private static class CellCounter {
		private final AtomicLong rootCounter;
		private final boolean nonNegative;

		private long base = 0;
		private long counter = 0;

		CellCounter(AtomicLong rootCounter, boolean nonNegative) {
			this.rootCounter = rootCounter;
			this.base = rootCounter.getAndAdd(BATCH_INCREMENT);
			this.nonNegative = nonNegative;
		}

		long next() {
			long value = base + counter++;

			if (counter == BATCH_INCREMENT) {
				base = rootCounter.getAndAdd(BATCH_INCREMENT);
				counter = 0;

				if (nonNegative) {
					long max = base + BATCH_INCREMENT;
					if (base < 0 || max < 0) {
						rootCounter.compareAndSet(max, 0);

						base = rootCounter.getAndAdd(BATCH_INCREMENT);
						counter = 0;
					}
				}
			}

			return value;
		}
	}
}
