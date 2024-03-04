package rpc.abg.util.tuple;

public final class Tuple1<T1> {
	public final T1 _1;

	public Tuple1(T1 _1) {
		this._1 = _1;
	}

	@Override
	public String toString() {
		return "Tuple1{" + //
				"_1=" + _1 + //
				'}';
	}
}
