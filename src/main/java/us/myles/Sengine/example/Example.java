package us.myles.Sengine.example;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.Sengine.cache.CacheCollector;
import us.myles.Sengine.cache.CacheProvider;
import us.myles.Sengine.events.EventBuilder;
import us.myles.Sengine.events.EventEngine;
import us.myles.Sengine.message.Replacer;

import java.util.concurrent.TimeUnit;

public class Example extends JavaPlugin {
	private EventBuilder eb;

	public void onEnable() {
		eb = new EventBuilder(this);
		System.out.println("Running a CacheProvider example");
		final CacheProvider<String> x = CacheProvider.create(String.class, new CacheCollector<String>() {

			@Override
			public String get() {
				if (getProvider().lastRequestWithin(10, TimeUnit.SECONDS)) {
					System.out.println("Hey this resource was fetched within last " + getProvider().lastRequestTime(TimeUnit.SECONDS));
				}
				return "Hello World";
			}
		});
		System.out.println("Added an event hook for PlayerJoinEvent");
		eb.when(PlayerJoinEvent.class).then(new EventEngine<PlayerJoinEvent>() {
			@Override
			public void handle(PlayerJoinEvent e) {
				System.out.println("Somebody joined the game called " + e.getPlayer().getName());
				System.out.println("String: " + x.get());
			}
		});
		System.out.println(new Replacer("Hello %player").bind("%pl", "Plop").bind("%player", "John").build());

	}
}
