package rpc.abg.util.tuple;

public final class Tuple3<T1, T2, T3> {
	public final T1 _1;
	public final T2 _2;
	public final T3 _3;

	public Tuple3(T1 _1, T2 _2, T3 _3) {
		this._1 = _1;
		this._2 = _2;
		this._3 = _3;
	}

	@Override
	public String toString() {
		return "Tuple3{" + //
				"_1=" + _1 + //
				", _2=" + _2 + //
				", _3=" + _3 + //
				'}';
	}
}
