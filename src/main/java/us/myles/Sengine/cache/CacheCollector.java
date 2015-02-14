package us.myles.Sengine.cache;

public abstract class CacheCollector<T> {
	private CacheProvider<T> provider;

	public abstract T get();

	public CacheProvider<T> getProvider() {
		return this.provider;
	}

	protected void setProvider(CacheProvider<T> p) {
		this.provider = p;
	}
}
