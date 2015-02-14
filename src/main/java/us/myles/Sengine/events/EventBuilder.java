package us.myles.Sengine.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class EventBuilder<T extends org.bukkit.event.Event> implements Listener {
	private final JavaPlugin plugin;
	private Class<T> currentEvent;
	private EventPriority priority = EventPriority.NORMAL;

	public EventBuilder(JavaPlugin p) {
		this.plugin = p;
	}

	public EventBuilder<T> when(Class<T> e) {
		this.currentEvent = e;
		return this;
	}

	public EventBuilder<T> priority(EventPriority p) {
		this.priority = p;
		return this;
	}

	public EventBuilder<T> then(EventEngine<T> e) {
		if (this.currentEvent == null) throw new RuntimeException("Failed to register null event to handler");
		registerHandler(currentEvent, e, priority);
		this.currentEvent = null;
		this.priority = EventPriority.NORMAL;
		return this;
	}

	private void registerHandler(final Class<T> currentEvent, final EventEngine<T> e, EventPriority priority) {
		EventExecutor ee = new EventExecutor() {

			@Override
			public void execute(Listener listener, Event event) throws EventException {
				if (event.getClass().equals(currentEvent))
					e.handle(currentEvent.cast(event));
			}
		};
		Bukkit.getPluginManager().registerEvent(currentEvent, this, priority, ee, plugin);
	}
}
