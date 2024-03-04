package rpc.abg.util.tuple;

public final class Tuple {
	private Tuple() {}
	
	public static <T1> Tuple1<T1> tuple(T1 _1) {
		return new Tuple1<T1>(_1);
	}

	public static <T1, T2> Tuple2<T1, T2> tuple(T1 _1, T2 _2) {
		return new Tuple2<T1, T2>(_1, _2);
	}

	public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 _1, T2 _2, T3 _3) {
		return new Tuple3<T1, T2, T3>(_1, _2, _3);
	}

	public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(T1 _1, T2 _2, T3 _3, T4 _4) {
		return new Tuple4<T1, T2, T3, T4>(_1, _2, _3, _4);
	}

	public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> tuple(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
		return new Tuple5<T1, T2, T3, T4, T5>(_1, _2, _3, _4, _5);
	}

	public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> tuple(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
		return new Tuple6<T1, T2, T3, T4, T5, T6>(_1, _2, _3, _4, _5, _6);
	}
}
