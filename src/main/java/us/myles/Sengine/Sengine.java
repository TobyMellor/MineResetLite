package us.myles.Sengine;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import us.myles.Sengine.message.PastebinReporter;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Sengine {
	public static double VERSION = 0.1;
	public static PastebinReporter REPORTER = new PastebinReporter("b4baefd01da242b02880e879e46008e2");

	public static void dump(Object... debug) {
		dump(new Throwable(), debug);
	}

	public static void dump(Throwable t, String... debug) {
		PastebinReporter.Paste p = new PastebinReporter.Paste();
		p.appendLine("Stacktrace: ");
		p.appendLine(getStack(t));
		p.appendLine("");
		p.appendLine("");
		int i = 0;
		for (Object o : debug) {
			p.appendLine("INFO[" + i + "] " + o.toString());
			i++;
		}
		p.appendLine("");
		p.appendLine("");
		p.appendLine("Enabled Plugins: " + getEnabledPlugins());
		p.appendLine("Disabled Plugins: " + getDisabledPlugins());
		p.appendLine("-----");
		p.appendLine("Online Players: " + Bukkit.getOnlinePlayers().size());
		String pp = REPORTER.post("Pasted via Sengine", p, PastebinReporter.ExpireDate.ONE_MONTH);
		Bukkit.getLogger().warning("ERROR FOUND FOR PLUGIN, REPORT TO DEVELOPED --> " + pp);
	}

	public static String getEnabledPlugins() {
		StringBuilder sb = new StringBuilder();
		for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
			if (p.isEnabled()) {
				sb.append(sb.length() == 0 ? "" : " ");
				sb.append(p.getName());
			}
		}
		return sb.toString();
	}

	public static String getDisabledPlugins() {
		StringBuilder sb = new StringBuilder();
		for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
			if (!p.isEnabled()) {
				sb.append(sb.length() == 0 ? "" : " ");
				sb.append(p.getName());
			}
		}
		return sb.toString();
	}


	public static String paste(String... s) {
		return REPORTER.post("Pasted via Sengine", new PastebinReporter.Paste(s), PastebinReporter.ExpireDate.ONE_MONTH);
	}

	public static String getStack(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
}
