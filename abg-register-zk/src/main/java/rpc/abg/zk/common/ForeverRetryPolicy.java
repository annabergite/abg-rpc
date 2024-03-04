package rpc.abg.zk.common;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.TimeUnit;

import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;

public class ForeverRetryPolicy implements RetryPolicy {
	private final int baseSleepTimeMs;
	private final int maxSleepMs;

	public ForeverRetryPolicy(int baseSleepTimeMs, int maxSleepMs) {
		checkArgument(baseSleepTimeMs > 0);
		checkArgument(maxSleepMs > 0);
		checkArgument(maxSleepMs >= baseSleepTimeMs);

		this.baseSleepTimeMs = baseSleepTimeMs;
		this.maxSleepMs = maxSleepMs;
	}

	@Override
	public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
		try {
			sleeper.sleepFor(getSleepTimeMs(retryCount, elapsedTimeMs), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}

		return true;
	}

	private long getSleepTimeMs(int retryCount, long elapsedTimeMs) {
		if (retryCount < 0) {
			return maxSleepMs;
		}

		long sleepMs = baseSleepTimeMs * (retryCount + 1);

		if (sleepMs > maxSleepMs || sleepMs <= 0) {
			sleepMs = maxSleepMs;
		}

		return sleepMs;
	}

}
