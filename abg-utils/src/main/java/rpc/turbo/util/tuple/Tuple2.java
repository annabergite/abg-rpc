package rpc.abg.util.tuple;

public final class Tuple2<T1, T2> {
	public final T1 _1;
	public final T2 _2;

	public Tuple2(T1 _1, T2 _2) {
		this._1 = _1;
		this._2 = _2;
	}

	@Override
	public String toString() {
		return "Tuple2{" + //
				"_1=" + _1 + //
				", _2=" + _2 + //
				'}';
	}
}
