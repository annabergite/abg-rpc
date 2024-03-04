package rpc.abg.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 高性能序号生成器，是AtomicInteger性能的10x，<br>
 * 实现原理为每个线程持有100个数字，线程内简单递增，<br>
 * 该生成器生成数字整体上并不是简单递增的，仅适用于特定场景
 * 
 * @author Annabergite
 *
 */
public final class ConcurrentIntegerSequencer {
	private static final int BATCH_INCREMENT = 100;

	private final AtomicInteger rootCounter;
	private final Supplier<CellCounter> supplier;
	private final int attachmentIndex = AttachmentThreadUtils.nextVarIndex();

	public ConcurrentIntegerSequencer() {
		this(0);
	}

	public ConcurrentIntegerSequencer(int initialValue) {
		this(initialValue, false);
	}

	public ConcurrentIntegerSequencer(int initialValue, boolean nonNegative) {
		this.rootCounter = new AtomicInteger(initialValue);
		this.supplier = () -> new CellCounter(rootCounter, nonNegative);
	}

	public int next() {// 性能受制于AttachmentThreadUtils.getOrUpdate
		CellCounter cellCounter = AttachmentThreadUtils.getOrUpdate(attachmentIndex, supplier);
		return cellCounter.next();
	}

	private static class CellCounter {
		private final AtomicInteger rootCounter;
		private final boolean nonNegative;

		private int base = 0;
		private int counter = 0;

		CellCounter(AtomicInteger rootCounter, boolean nonNegative) {
			this.rootCounter = rootCounter;
			this.base = rootCounter.getAndAdd(BATCH_INCREMENT);
			this.nonNegative = nonNegative;
		}

		int next() {
			int value = base + counter++;

			if (counter == BATCH_INCREMENT) {
				base = rootCounter.getAndAdd(BATCH_INCREMENT);
				counter = 0;

				if (nonNegative) {
					int max = base + BATCH_INCREMENT;
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
