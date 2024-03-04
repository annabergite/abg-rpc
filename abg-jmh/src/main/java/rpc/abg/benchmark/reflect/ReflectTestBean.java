package rpc.abg.benchmark.reflect;

public class ReflectTestBean {

	public int directUserAge = 18;
	public volatile int volatileUserAge = 26;

	public int getDirectUserAge(long userId) {
		return directUserAge;
	}

	public void setDirectUserAge(long userId, int directUserAge) {
		this.directUserAge = directUserAge;
	}
}
