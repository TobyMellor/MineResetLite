package us.myles.Sengine;

import us.myles.Sengine.cache.CacheCollector;
import us.myles.Sengine.cache.CacheProvider;

import java.util.concurrent.TimeUnit;

public class Main {
	public static void main(String[] args) {
		CacheProvider<String> s = CacheProvider.create(String.class, new CacheCollector<String>() {
			@Override
			public String get() {
				if (!getProvider().lastCacheWithin(10, TimeUnit.SECONDS)) {
					System.out.println("Last cache for was 30s out, caching method: : " + getProvider().lastCacheTime(TimeUnit.SECONDS) + "s");
					getProvider().updatedCache();
				}
				return "Hello";
			}
		});
		while(true){
			System.out.println(s.get());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
