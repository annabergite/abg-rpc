package rpc.abg.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FutureUtils {

	public static <T> CompletableFuture<T> withFailover(CompletableFuture<T> future,
			Supplier<CompletableFuture<T>> failover) {

		CompletableFuture<T> futureWithFailover = future.newIncompleteFuture();

		future.whenComplete((result, throwable) -> {
			if (throwable != null) {
				failover.get().whenComplete((r, t) -> {
					if (t != null) {
						futureWithFailover.completeExceptionally(t);
					} else {
						futureWithFailover.complete(r);
					}
				});
			} else {
				futureWithFailover.complete(result);
			}
		});

		return futureWithFailover;
	}

}
