package us.myles.Sengine.cache;

import java.util.concurrent.TimeUnit;

public class CacheProvider<T> {
	private final CacheCollector<T> collector;
	private Long lastRetrieved = 0L;
	private Long lastCache = 0L;

	private CacheProvider(CacheCollector<T> cc) {
		this.collector = cc;
	}

	public Long getLastRetrieved() {
		return this.lastRetrieved;
	}

	public Long getLastCache() {
		return this.lastCache;
	}


	public boolean lastRequestWithin(int u, TimeUnit t) {
		Long x = TimeUnit.MILLISECONDS.convert(u, t);
		Long diff = System.currentTimeMillis() - getLastRetrieved();
		return (diff <= x);
	}

	public Long lastCacheTime(TimeUnit t) {
		Long diff = System.currentTimeMillis() - getLastCache();
		return t.convert(diff, TimeUnit.MILLISECONDS);
	}

	public boolean lastCacheWithin(int u, TimeUnit t) {
		Long x = TimeUnit.MILLISECONDS.convert(u, t);
		Long diff = System.currentTimeMillis() - getLastCache();
		return (diff <= x);
	}

	public void updatedCache() {
		this.lastCache = System.currentTimeMillis();
	}

	public Long lastRequestTime(TimeUnit t) {
		Long diff = System.currentTimeMillis() - getLastRetrieved();
		return t.convert(diff, TimeUnit.MILLISECONDS);
	}

	public T get() {
		T value = collector.get();
		this.lastRetrieved = System.currentTimeMillis();
		return value;
	}


	public static <Z> CacheProvider<Z> create(Class<Z> x, CacheCollector<Z> cc) {
		CacheProvider cp = new CacheProvider<Z>(cc);
		cc.setProvider(cp);
		return cp;
	}
}
