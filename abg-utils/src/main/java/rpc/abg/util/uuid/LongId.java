package rpc.abg.util.uuid;

import static java.time.ZoneOffset.UTC;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 每个jvm实例一个不重复的machineId，分配起来有点费劲
 * 
 * @author Annabergite
 *
 */
public final class LongId {

	private final static long NEW_EPOCH_MILLIS = LocalDateTime.parse("2017-01-01T00:00:00").toEpochSecond(UTC) * 1000L;

	private final int machineId;
	private final AtomicInteger counter = new AtomicInteger(0);
	private volatile int lastMinutes = 0;

	public LongId(int machineId) {
		if (machineId < 0 || machineId > 1 << 15) {
			throw new IllegalArgumentException("machineId must between 0, " + (1 << 15));
		}

		this.machineId = machineId;
	}

	private int getEpochMinutes() {
		long millis = System.currentTimeMillis() - NEW_EPOCH_MILLIS;
		int minutes = (int) (millis / 1000 / 60);
		return minutes;
	}

	/**
	 * 63年内有效
	 * 
	 * @return
	 */
	public long nextId() {
		int minutes = getEpochMinutes();

		if (lastMinutes != minutes) {
			synchronized (this) {
				minutes = getEpochMinutes();

				if (lastMinutes != minutes) {
					lastMinutes = minutes;
					counter.set(0);
				}
			}
		}

		int count = counter.getAndIncrement();
		if (count > (1 << 24)) {
			long millis = System.currentTimeMillis();

			try {
				long minuteMillis = TimeUnit.MINUTES.toMillis(1);

				Thread.sleep(millis / minuteMillis * minuteMillis - millis + minuteMillis);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			return nextId();
		}

		long id = (((long) minutes) << (64 - 25)) | (((long) machineId) << (64 - 25 - 15)) | count;

		return id;
	}

	public static void main(String[] args) {
		LongId idGenerator = new LongId(123);

		while (true) {
			long id = idGenerator.nextId();

			System.out.println(id);
		}
	}

}
