/*
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rpc.abg.util;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface SystemClock {

	/**
	 * 很快，但不是那么靠谱，要求高精度的地方不能用
	 * 
	 * @return
	 */
	static SystemClock fast() {
		return FastClock.clock;
	}

	/**
	 * 靠谱时间，要求高精度的地方用
	 * 
	 * @return
	 */
	static SystemClock realTime() {
		return RealTimeClock.clock;
	}

	/**
	 * 返回当前秒
	 * 
	 * @return
	 */
	long seconds();

	/**
	 * 返回当前毫秒
	 * 
	 * @return
	 */
	long mills();

	/**
	 * 返回当前微妙
	 * 
	 * @return
	 */
	long micros();
}

class RealTimeClock implements SystemClock {

	static final SystemClock clock = new RealTimeClock();

	@Override
	public long seconds() {
		return System.currentTimeMillis() / 1000;
	}

	@Override
	public long mills() {
		return System.currentTimeMillis();
	}

	@Override
	public long micros() {
		Instant instant = Instant.now();
		long micros = instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1000;
		return micros;
	}

}

class FastClockCacheLine0 {
	public volatile long p0, p1, p2, p3, p4, p5, p6, p7;
	volatile long mills;
}

class FastClockMills extends FastClockCacheLine0 {
	volatile long mills;
}

class FastClockCacheLine1 extends FastClockMills {
	public volatile long q0, q1, q2, q3, q4, q5, q6, q7;
}

class FastClockSeconds extends FastClockCacheLine1 {
	volatile long seconds;
}

class FastClockCacheLine2 extends FastClockSeconds {
	public volatile long r0, r1, r2, r3, r4, r5, r6, r7;
}

class FastClock extends FastClockCacheLine2 implements SystemClock {

	static final SystemClock clock = new FastClock(1);

	final long precision;

	FastClock(long precision) {
		this.precision = precision;
		mills = System.currentTimeMillis();
		seconds = mills / 1000;
		scheduleClockUpdating();
	}

	private void scheduleClockUpdating() {
		ScheduledExecutorService scheduler = Executors//
				.newSingleThreadScheduledExecutor(runnable -> {
					Thread t = new Thread(runnable, "system.clock");
					t.setDaemon(true);
					return t;
				});

		scheduler.scheduleAtFixedRate(() -> {
			long currentTimeMillis = System.currentTimeMillis();
			mills = currentTimeMillis;

			if (seconds != currentTimeMillis / 1000) {
				seconds = currentTimeMillis / 1000;
			}
		}, precision, precision, TimeUnit.MILLISECONDS);
	}

	public long seconds() {
		return seconds;
	}

	public long mills() {
		return mills;
	}

	public long micros() {
		return mills * 1000;
	}

}
