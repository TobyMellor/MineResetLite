package us.myles.Sengine.events;

import org.bukkit.event.Event;

public interface EventEngine<T extends Event>{
	public void handle(T e);
}
