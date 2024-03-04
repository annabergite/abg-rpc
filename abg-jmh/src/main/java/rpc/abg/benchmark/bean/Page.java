package rpc.abg.benchmark.bean;

import java.io.Serializable;
import java.util.List;

public class Page<T> implements Serializable {
	private static final long serialVersionUID = -7529237188686406553L;

	private static final Page<Object> EMPTY = new Page<>();

	static {
		EMPTY.setResult(List.of());
	}

	@SuppressWarnings("unchecked")
	public static <T> Page<T> empty() {
		return (Page<T>) EMPTY;
	}

	private int pageNo;
	private int total;
	private List<T> result;

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<T> getResult() {
		return result;
	}

	public void setResult(List<T> result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "Page [pageNo=" + pageNo + ", total=" + total + ", result=" + result + "]";
	}
}
