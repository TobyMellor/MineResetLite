package us.myles.Sengine.message;

import org.bukkit.ChatColor;

import java.util.*;

public class Replacer {
	private final String target;
	private final Map<String, String> binding = new HashMap<String, String>();

	public Replacer(String target) {
		this.target = target;
	}

	public Replacer bind(String toReplace, String replacement) {
		this.binding.put(toReplace, replacement);
		return this;
	}

	public String build() {
		String s = target;
		List<String> words = new ArrayList<String>(binding.keySet());
		Collections.sort(words, new LengthComparator());
		Collections.reverse(words);
		for (String w : words)
			s = s.replace(w, binding.get(w));
		return ChatColor.translateAlternateColorCodes('&', s);
	}

}
