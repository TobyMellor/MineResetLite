package com.koletar.jj.mineresetlite;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.placeholder.PlaceholderReplacer;
import com.gmail.filoghost.holographicdisplays.object.NamedHologramManager;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.primesoft.asyncworldedit.AsyncWorldEditMain;
import org.primesoft.asyncworldedit.PlayerEntry;
import org.primesoft.asyncworldedit.blockPlacer.BlockPlacerEntry;
import org.primesoft.asyncworldedit.blockPlacer.BlockPlacerPlayer;
import org.primesoft.asyncworldedit.blockPlacer.IBlockPlacerListener;
import org.primesoft.asyncworldedit.blockPlacer.IJobEntryListener;
import org.primesoft.asyncworldedit.blockPlacer.entries.JobEntry;
import org.primesoft.asyncworldedit.utils.FuncParamEx;
import org.primesoft.asyncworldedit.worldedit.AsyncEditSessionFactory;
import org.primesoft.asyncworldedit.worldedit.CancelabeEditSession;
import org.primesoft.asyncworldedit.worldedit.ThreadSafeEditSession;
import us.myles.Sengine.Sengine;
import us.myles.Sengine.cache.CacheCollector;
import us.myles.Sengine.cache.CacheProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author jjkoletar
 */
public class Mine implements ConfigurationSerializable {
	public int minX;
	public int minY;
	public int minZ;
	public int maxX;
	public int maxY;
	public int maxZ;
	private World world;
	public Map<SerializableBlock, Double> composition;
	private int resetDelay;
	private List<Integer> resetWarnings;
	private String name;
	public SerializableBlock surface;
	private boolean fillMode;
	private int resetClock;
	private boolean isSilent;
	public List<ResetCriterion> criteria;
	private int checkOffset;
	private int air = 0;
	private int cooldown = 300;
	private Long lasttried = 0L;
	private String schematic;
	private List<String> signs;
	private int seconds = 0;
	public String origin;
	public String holo;
	public String teleport = "";
	private String last = "";
	public boolean everyone = false;
	public UUID id = UUID.randomUUID();
	private boolean done = true;
	private PlayerEntry entry;
	private int total = 0;
	private int jobID;
	private List<Double> speed;
	public Hologram hologram;
	public Hologram replace;
	private int secondsLeft;
	private CacheProvider<Map> stats;
	private boolean pending = false;
//	private List<String> holograms;

	public Mine(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String name, World world) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.name = name;
		this.world = world;
		composition = new HashMap<SerializableBlock, Double>();
		resetWarnings = new LinkedList<Integer>();
		this.resetWarnings = new LinkedList();
		this.signs = new LinkedList();
		this.cooldown = 300;
		this.origin = "";
		this.holo = "";
		this.criteria = new LinkedList<ResetCriterion>();

//		this.holograms = new LinkedList();
		setup();
	}

	public Mine(Map<String, Object> me) {
		try {
			minX = (Integer) me.get("minX");
			minY = (Integer) me.get("minY");
			minZ = (Integer) me.get("minZ");
			maxX = (Integer) me.get("maxX");
			maxY = (Integer) me.get("maxY");
			maxZ = (Integer) me.get("maxZ");
		} catch (Throwable t) {
			throw new IllegalArgumentException("Error deserializing coordinate pairs");
		}
		try {
			world = Bukkit.getServer().getWorld((String) me.get("world"));
		} catch (Throwable t) {
			throw new IllegalArgumentException("Error finding world");
		}
		if (world == null) {
			Logger l = Bukkit.getLogger();
			l.severe("[MineResetLite] Unable to find a world! Please include these logger lines along with the stack trace when reporting this bug!");
			l.severe("[MineResetLite] Attempted to load world named: " + me.get("world"));
			l.severe("[MineResetLite] Worlds listed: " + StringTools.buildList(Bukkit.getWorlds(), "", ", "));
			throw new IllegalArgumentException("World was null!");
		}
		try {
			Map<String, Double> sComposition = (Map<String, Double>) me.get("composition");
			composition = new HashMap<SerializableBlock, Double>();
			for (Map.Entry<String, Double> entry : sComposition.entrySet()) {
				composition.put(new SerializableBlock(entry.getKey()), entry.getValue());
			}
		} catch (Throwable t) {
			throw new IllegalArgumentException("Error deserializing composition");
		}
		name = (String) me.get("name");
		resetDelay = (Integer) me.get("resetDelay");
		List<String> warnings = (List<String>) me.get("resetWarnings");
		resetWarnings = new LinkedList<Integer>();
		for (String warning : warnings) {
			try {
				resetWarnings.add(Integer.valueOf(warning));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Non-numeric reset warnings supplied");
			}
		}
		if (me.containsKey("surface")) {
			if (!me.get("surface").equals("")) {
				surface = new SerializableBlock((String) me.get("surface"));
			}
		}
		if (me.containsKey("fillMode")) {
			fillMode = (Boolean) me.get("fillMode");
		}
		if (me.containsKey("resetClock")) {
			resetClock = (Integer) me.get("resetClock");
		}
		//Compat for the clock
		if (resetDelay > 0 && resetClock == 0) {
			resetClock = resetDelay;
		}
		if (me.containsKey("isSilent")) {
			isSilent = (Boolean) me.get("isSilent");
		}
		this.criteria = new LinkedList();
		if (me.containsKey("criteria")) {
			List<String> sCriteria = (List) me.get("criteria");
			for (String sci : sCriteria)
				this.criteria.add(new ResetCriterion(sci));
		}
		if (me.containsKey("schematic")) {
			this.schematic = (String) me.get("schematic");
		}
		if (me.containsKey("origin")) {
			this.origin = (String) me.get("origin");
		}
		if (me.containsKey("holo")) {
			this.holo = (String) me.get("holo");
		}
		if (me.containsKey("teleport")) {
			this.teleport = (String) me.get("teleport");
		}
		if (me.containsKey("signs")) {
			List<String> signs = (List) me.get("signs");
			if (this.signs == null)
				this.signs = new LinkedList<String>();
			if (signs != null)
				for (String sci : signs)
					if (sci != null)
						this.signs.add(sci);
		}
//		if (me.containsKey("holograms")) {
//			List<String> holograms = (List) me.get("holograms");
//			for (String sci : holograms)
//				this.holograms.add(sci);
//		}
		if (me.containsKey("cooldown"))
			this.cooldown = (Integer) me.get("cooldown");
		if (me.containsKey("everyone"))
			this.everyone = (Boolean) me.get("everyone");
		setup();
	}

	private void setup() {
		HologramsAPI.registerPlaceholder(MineResetLite.instance, "{" + this.name + ":" + "name}", 60.0, new PlaceholderReplacer() {

			@Override
			public String update() {
				return name;
			}
		});
		HologramsAPI.registerPlaceholder(MineResetLite.instance, "{" + this.name + ":" + "time}", 100.0, new PlaceholderReplacer() {

			@Override
			public String update() {
				if (!isDone()) return 0 + "";
				return (getTimeUntilReset() - 1) == 0 ? "~1" : getTimeUntilReset() - 1 + "";
			}
		});
		HologramsAPI.registerPlaceholder(MineResetLite.instance, "{" + this.name + ":" + "time2}", 100.0, new PlaceholderReplacer() {

			@Override
			public String update() {
				if (!isDone()) return "Resetting!";
				return getTimeUntilReset() <= 1 ? ((getTimeUntilReset()) == 0 ? "~1" : getTimeUntilReset()) + " minute" : getTimeUntilReset() + " minutes";
			}
		});
		HologramsAPI.registerPlaceholder(MineResetLite.instance, "{" + this.name + ":" + "time3}", 100.0, new PlaceholderReplacer() {

			@Override
			public String update() {
				return getTimeUntilResetS() <= 1 ? getTimeUntilResetS() + " second" : getTimeUntilResetS() + " seconds";
			}
		});
		System.out.println("Registering reset hologram " + "{" + this.name + ":" + "reset}");
		HologramsAPI.registerPlaceholder(MineResetLite.instance, "{" + this.name + ":" + "reset}", 60.0, new PlaceholderReplacer() {

			@Override
			public String update() {
				return getResetDelay() + "";
			}
		});
		for (final Material m : Material.values()) {
			if (m.name().toLowerCase().contains("ore") || m == Material.STONE || m == Material.DIRT || m == Material.LOG || m == Material.LOG_2 || m == Material.AIR || m.name().contains("IRON") ||
					m.name().toUpperCase().contains("BLOCK"))
				HologramsAPI.registerPlaceholder(MineResetLite.instance, "{" + this.name + ":" + m.getId() + "}", 30.0, new PlaceholderReplacer() {

					@Override
					public String update() {
						Map<Integer, Integer> stats = getStats();
						if (stats.containsKey(m.getId())) {
							return stats.get(m.getId()) + "%";
						}
						return "0%";
					}
				});
		}
	}

	public int getCooldown() {
		return cooldown;
	}

	public void setCooldown(int cooldown) {
		this.cooldown = cooldown;
	}

	public Map<String, Object> serialize() {
		Map<String, Object> me = new HashMap<String, Object>();
		me.put("minX", minX);
		me.put("minY", minY);
		me.put("minZ", minZ);
		me.put("maxX", maxX);
		me.put("maxY", maxY);
		me.put("maxZ", maxZ);
		me.put("cooldown", cooldown);
		me.put("world", world.getName());
		//Make string form of composition
		Map<String, Double> sComposition = new HashMap<String, Double>();
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			sComposition.put(entry.getKey().toString(), entry.getValue());
		}
		me.put("composition", sComposition);
		me.put("name", name);
		me.put("resetDelay", resetDelay);
		List<String> warnings = new LinkedList<String>();
		for (Integer warning : resetWarnings) {
			warnings.add(warning.toString());
		}
		me.put("resetWarnings", warnings);
		if (surface != null) {
			me.put("surface", surface.toString());
		} else {
			me.put("surface", "");
		}
		me.put("fillMode", fillMode);
		me.put("resetClock", resetClock);
		me.put("isSilent", isSilent);
		me.put("everyone", everyone);
		List sCriteria = new LinkedList();
		for (ResetCriterion rc : this.criteria) {
			sCriteria.add(rc.toString());
		}
		me.put("criteria", sCriteria);
		List signs = new LinkedList();
		for (String rc : this.signs) {
			signs.add(rc.toString());
		}
		if (signs != null)
			me.put("signs", signs);
//		List holograms = new LinkedList();
//		for (String rc : this.holograms) {
//			holograms.add(rc.toString());
//		}
////		me.put("holograms", holograms);
		if (this.schematic != null)
			me.put("schematic", schematic);
		if (this.origin != null)
			me.put("origin", origin);
		if (this.holo != null)
			me.put("holo", origin);
		if (this.teleport != null)
			me.put("teleport", teleport);
		return me;
	}

	public boolean getFillMode() {
		return fillMode;
	}

	public void setFillMode(boolean fillMode) {
		this.fillMode = fillMode;
	}

	public void setResetDelay(int minutes) {
		resetDelay = minutes;
		resetClock = minutes;
	}

	public void setResetWarnings(List<Integer> warnings) {
		resetWarnings = warnings;
	}

	public List<Integer> getResetWarnings() {
		return resetWarnings;
	}

	public int getResetDelay() {
		return resetDelay;
	}

	/**
	 * Return the length of time until the next automatic reset.
	 * The actual length of time is anywhere between n and n-1 minutes.
	 *
	 * @return clock ticks left until reset
	 */
	public int getTimeUntilReset() {
		return resetClock;
	}

	public int getTimeUntilResetS() {
		return seconds;
	}

	public SerializableBlock getSurface() {
		return surface;
	}

	public void setSurface(SerializableBlock surface) {
		this.surface = surface;
	}

	public World getWorld() {
		return world;
	}

	public String getName() {
		return name;
	}

	public Map<SerializableBlock, Double> getComposition() {
		return composition;
	}

	public boolean isSilent() {
		return isSilent;
	}

	public void setSilence(boolean isSilent) {
		this.isSilent = isSilent;
	}

	public double getCompositionTotal() {
		double total = 0;
		for (Double d : composition.values()) {
			total += d;
		}
		return total;
	}

	public boolean isInside(Player p) {
		Location l = p.getLocation();
		return l.getWorld().equals(world)
				&& (l.getX() >= minX && l.getX() <= maxX)
				&& (l.getY() >= minY && l.getY() <= maxY)
				&& (l.getZ() >= minZ && l.getZ() <= maxZ);
	}

	public Map<Location, String[]> getSigns() {
		Map<Location, String[]> m = new HashMap<Location, String[]>();
		if (signs == null) {
			signs = new ArrayList<String>();
		}
		for (String s : signs) {
			String loc = s.split("#")[0];
			Location l = new Location(world, Integer.parseInt(loc.split(",")[0]), Integer.parseInt(loc.split(",")[1]), Integer.parseInt(loc.split(",")[2]));
			String data = s.split("#")[1];
			String[] lines = data.split("\n");
			m.put(l, lines);
		}
		return m;
	}

//	public Map<Location, String[]> getHolograms() {
//		Map<Location, String[]> m = new HashMap<Location, String[]>();
//		for (String s : holograms) {
//			String loc = s.split("#")[0];
//			Location l = new Location(world, Integer.parseInt(loc.split(",")[0]), Integer.parseInt(loc.split(",")[1]), Integer.parseInt(loc.split(",")[2]));
//			String data = s.split("#")[1];
//			String[] lines = data.split("\n");
//			m.put(l, lines);
//		}
//		return m;
//	}

	public void addSign(Sign s) {
		StringBuffer sb = new StringBuffer();
		for (String x : s.getLines()) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(x);
		}
		signs.add(s.getX() + "," + s.getY() + "," + s.getZ() + "#" + sb.toString());
	}

	public String formatString(String s) {
		if (!isDone()) {
			s = s.replace("%mn", "Resetting!");
		} else {
			s = s.replace("%mn", getTimeUntilReset() - 1 <= 1 ? getTimeUntilReset() - 1 + " minute" : getTimeUntilReset() - 1 + " minutes");
		}

		s = s.replace("%n", name);
		s = s.replace("%r", getResetDelay() + "");

		s = s.replace("%m", getTimeUntilReset() - 1 + "");
		Map<Integer, Integer> stat = getStats();
		for (final Material m : Material.values()) {
			if (m.name().toLowerCase().contains("ore") || m == Material.STONE || m == Material.DIRT || m == Material.LOG || m == Material.LOG_2 || m == Material.AIR)
				s = s.replace("%" + m.getId() + "c", stat.containsKey(m.getId()) ? stat.get(m.getId()) + "%" : "0%");
		}
		return s;
	}

	public void renderSigns() {
		for (Map.Entry<Location, String[]> sign : getSigns().entrySet()) {
			if (sign.getKey().getBlock() != null) {
				if (sign.getKey().getBlock().getState() instanceof Sign) {
					Sign s = (Sign) sign.getKey().getBlock().getState();
					int z = 0;
					for (String x : sign.getValue()) {
						s.setLine(z, formatString(x));
						z++;
					}
					s.update();
				}
			}
		}
	}

	public void http() throws IOException {
		if (!Config.isApi()) return;
		String url = Config.getLink() + "?mine=" + this.name + "&secret=" + Config.getSecret() + "&resetDelay=" + getResetDelay();
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "MineResetLite");
		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		while (in.readLine() != null) {
		}
		in.close();
	}

	public synchronized void reset() {
//		if (MineResetLite.instance.resetting) return; // Disabled for now
		MineResetLite.instance.resetting = true;
		//System.out.println("Request to reset, " + isDone() + " : " + isPending() + " for " + this.name);
		if (!isDone() || isPending()) return;
		// HTTP YAY
		setPending(true);
		try {
			http();
		} catch (IOException e) {
			System.out.println("HTTP Request Failed.. :( " + e.getMessage());
		}
		//Get probability map


		//Actually reset
		air = 0;
		this.speed = new ArrayList<Double>();
		final String jobName = UUID.randomUUID().toString();

		final ThreadSafeEditSession es = ((AsyncEditSessionFactory) WorldEdit.getInstance().getEditSessionFactory()).getThreadSafeEditSession(new BukkitWorld(world), 99999999);
		final IJobEntryListener stateListener = new IJobEntryListener() {
			@Override
			public void jobStateChanged(JobEntry job) {
				jobID = job.getJobId();
				if (job.getStatus() == JobEntry.JobStatus.PlacingBlocks) {
					System.out.println("Placing blocks for, " + name + ".");
					setPending(false);
					setDone(false);
					//Pull players out
					for (Player p : Bukkit.getServer().getOnlinePlayers()) {
						Location l = p.getLocation();
						if (isInside(p)) {
							if (teleport != null) {
								if (!teleport.equals("")) {
									p.teleport(new Location(world, Integer.parseInt(teleport.split(",")[0]), Integer.parseInt(teleport.split(",")[1]), Integer.parseInt(teleport.split(",")[2]),
											Float.parseFloat(teleport.split(",")[3]), Float.parseFloat(teleport.split(",")[4])));
								} else {
									p.teleport(new Location(world, l.getX(), maxY + 2D, l.getZ()));
								}
							} else {
								p.teleport(new Location(world, l.getX(), maxY + 2D, l.getZ()));
							}

						}
					}
				}
				if (job.getStatus() == JobEntry.JobStatus.Done) {
					for (int x = minX; x <= maxX; x++) {
						for (int y = minY; y <= maxY; y++) {
							for (int z = minZ; z <= maxZ; z++) {
								if (world.getBlockAt(x, y, z).getTypeId() == 0) {
									air++;
								}
							}
						}
					}
					setDone(true);
					setPending(false);
					MineResetLite.instance.resetting = false;
					System.out.println("Finished mine, " + name + ".");
				}
			}
		};
		final IBlockPlacerListener listener = new IBlockPlacerListener() {
			@Override
			public void jobAdded(JobEntry job) {
				if (job.getName().equals(jobName)
						&& job.getPlayer().equals(PlayerEntry.UNKNOWN)) {
					job.addStateChangedListener(stateListener);
					final IBlockPlacerListener listener = this;

					new Thread(new Runnable() {

						@Override
						public void run() {
							AsyncWorldEditMain.getInstance().getBlockPlacer().removeListener(listener);
						}
					}).start();
				}
			}

			@Override
			public void jobRemoved(JobEntry job) {
			}
		};

		AsyncWorldEditMain.getInstance().getBlockPlacer().addListener(listener);
		total = 0;
		if (schematic != null) {
			String sch = schematic;
			if (schematic.contains(",")) {
				String[] s = schematic.split(",");
				if (s.length >= 2) {
					boolean done = false;
					while (!done) {
						SecureRandom sr = new SecureRandom();
						sch = s[sr.nextInt(s.length)];
						if (!sch.equals(last))
							done = true;
					}
				}

			}
			last = sch;

			final File f = new File("plugins/WorldEdit/schematics/" + sch + ".schematic");
			if (!f.exists()) {
				MineResetLite.broadcast("Error Schematic not found... " + sch, this);
			} else {
				MineResetLite.broadcast(Phrases.phrase("schematicBc", this, sch), this);

				AsyncWorldEditMain.getInstance()
						.getBlockPlacer().PerformAsAsyncJob((ThreadSafeEditSession) es,
						PlayerEntry.UNKNOWN,
						jobName, new FuncParamEx<Integer, CancelabeEditSession, MaxChangedBlocksException>() {
							@Override
							public Integer Execute(CancelabeEditSession cancelabeEditSession) throws MaxChangedBlocksException {
								try {
									CuboidClipboard cc = CuboidClipboard.loadSchematic(f);
									if (origin == null) {
										cc.paste(cancelabeEditSession, new com.sk89q.worldedit.Vector(minX, maxY, minZ), false);
									} else {
										cc.paste(cancelabeEditSession, new com.sk89q.worldedit.Vector(Integer.parseInt(origin.split(",")[0]), Integer.parseInt(origin.split(",")[1]), Integer.parseInt(origin.split(",")[2])), false);
									}
									for (JobEntry e : AsyncWorldEditMain.getInstance().getBlockPlacer().getPlayerEvents(PlayerEntry.UNKNOWN).getJobs()) {
										if (e.getName().equalsIgnoreCase(jobName)) {
											jobID = e.getJobId();
										}
									}
									int b = 0;
									for (BlockPlacerEntry e : AsyncWorldEditMain.getInstance().getBlockPlacer().getPlayerEvents(PlayerEntry.UNKNOWN).getQueue()) {
										if (e.getJobId() == jobID) {
											b++;
										}
									}
									total = b;
								} catch (IOException e) {
									System.out.println("IO Exception");
									e.printStackTrace();
								} catch (MaxChangedBlocksException e) {
									System.out.println("Max Block Change Exception");
									e.printStackTrace();
								} catch (com.sk89q.worldedit.world.DataException e) {
									e.printStackTrace();
								}
								return 0;
							}
						}
				);
			}
			return;
		} else {
			// Manually :(
			final Mine t = this;
			AsyncWorldEditMain.getInstance()
					.getBlockPlacer().PerformAsAsyncJob((ThreadSafeEditSession) es,
					PlayerEntry.UNKNOWN,
					jobName, new FuncParamEx<Integer, CancelabeEditSession, MaxChangedBlocksException>() {
						@Override
						public Integer Execute(CancelabeEditSession cancelabeEditSession) throws MaxChangedBlocksException {

							/* Brum. */
							try {
								CuboidRegion region = new CuboidRegion(new BukkitWorld(world), new com.sk89q.worldedit.Vector(minX, minY, minZ), new com.sk89q.worldedit.Vector(maxX, maxY, maxZ));
								int i = cancelabeEditSession.setBlocks(region, new MinePattern(t));
								for (JobEntry e : AsyncWorldEditMain.getInstance().getBlockPlacer().getPlayerEvents(PlayerEntry.UNKNOWN).getJobs()) {
									if (e.getName().equalsIgnoreCase(jobName)) {
										jobID = e.getJobId();
									}
								}
								total = i;
							} catch (Exception e) {
								Sengine.dump(e, "Mine: " + name, "ID: " + jobID);
							}
//							Bukkit.getScheduler().scheduleSyncDelayedTask(MineResetLite.instance, new Runnable() {
//
//								@Override
//								public void run() {
//
//								}
//							}, 2L);

							return 0;
						}
					}
			);
			return;
		}
	}

	public Map<Integer, Integer> getStats() {
		/* lelel */
		if (this.stats == null) {
			this.stats = CacheProvider.create(Map.class, new CacheCollector<Map>() {
				public Map<Integer, Integer> comp = new HashMap<Integer, Integer>();
				int last = 1;
				Map<Integer, Integer> a = new HashMap<Integer, Integer>();
				Map<Integer, Integer> b = new HashMap<Integer, Integer>();

				@Override
				public Map get() {
					if (!getProvider().lastCacheWithin(30, TimeUnit.SECONDS)) {
						getProvider().updatedCache();
						int diff = maxX - minX;
						if (last == 1) {
							a.clear();
							for (int x = minX; x <= minX + (diff / 2); x++) {
								for (int y = minY; y <= maxY; y++) {
									for (int z = minZ; z <= maxZ; z++) {
										if (a.containsKey(Integer.valueOf(world.getBlockAt(x, y, z).getTypeId())))
											a.put(Integer.valueOf(world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(((Integer) a.get(Integer.valueOf(world.getBlockAt(x, y, z)
													.getTypeId()))).intValue() + 1));
										else {
											a.put(Integer.valueOf(world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(1));
										}
									}
								}
							}
							last = 0;
						} else {
							b.clear();
							for (int x = minX + (diff / 2); x <= maxX; x++) {
								for (int y = minY; y <= maxY; y++) {
									for (int z = minZ; z <= maxZ; z++) {
										if (b.containsKey(Integer.valueOf(world.getBlockAt(x, y, z).getTypeId())))
											b.put(Integer.valueOf(world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(((Integer) b.get(Integer.valueOf(world.getBlockAt(x, y, z)
													.getTypeId()))).intValue() + 1));
										else {
											b.put(Integer.valueOf(world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(1));
										}
									}
								}
							}
							last = 1;
						}
						int blocksSeen = 0;
						Map<Integer, Integer> merged = new HashMap<Integer, Integer>();
						for (Map.Entry<Integer, Integer> entry : a.entrySet()) {
							blocksSeen += entry.getValue();
							if (merged.containsKey(entry.getKey()))
								merged.put(entry.getKey(), merged.get(entry.getKey()) + entry.getValue());
							else
								merged.put(entry.getKey(), entry.getValue());
						}
						for (Map.Entry<Integer, Integer> entry : b.entrySet()) {
							blocksSeen += entry.getValue();
							if (merged.containsKey(entry.getKey()))
								merged.put(entry.getKey(), merged.get(entry.getKey()) + entry.getValue());
							else
								merged.put(entry.getKey(), entry.getValue());
						}
						if (merged.containsKey(0)) {
							if (air != 0) {
								merged.put(0, merged.get(0) - air);
							}
						}
						Map<Integer, Integer> percentageComposition = new HashMap<Integer, Integer>();
						for (Map.Entry<Integer, Integer> entry : merged.entrySet()) {
							percentageComposition.put(entry.getKey(), (int) (Double.valueOf(entry.getValue().doubleValue() / blocksSeen) * 100D));
						}
						this.comp = percentageComposition;
					}
					return this.comp;
				}
			});
		}
		return this.stats.get();
	}

	//	Map<Integer, Integer> blocks = new HashMap<Integer, Integer>();
//	double blocksSeen = 0.0D;
//	for (int x = this.minX; x <= this.maxX; x++) {
//		for (int y = this.minY; y <= this.maxY; y++) {
//			for (int z = this.minZ; z <= this.maxZ; z++) {
//				blocksSeen += 1.0D;
//				if (blocks.containsKey(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId())))
//					blocks.put(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(((Integer) blocks.get(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()))).intValue() + 1));
//				else if (!blocks.containsKey(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()))) {
//					blocks.put(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(1));
//				}
//			}
//		}
//	}
//	// Because some armadillo says so, we're adding the functionality to negate air blocks already in it. isn't that great.
//	if (blocks.containsKey(0)) {
//		if (air != 0) {
//			blocks.put(0, blocks.get(0) - air);
//		}
//	}
//	Map<Integer, Integer> percentageComposition = new HashMap<Integer, Integer>();
//	for (Map.Entry<Integer, Integer> entry : blocks.entrySet()) {
//		percentageComposition.put(entry.getKey(), (int) (Double.valueOf(entry.getValue().intValue() / blocksSeen) * 100D));
//	}
//	return percentageComposition;
	public boolean shouldMineReset(CommandSender sender) {
		Map<Integer, Integer> blocks = new HashMap<Integer, Integer>();
		double blocksSeen = 0.0D;
		for (int x = this.minX; x <= this.maxX; x++) {
			for (int y = this.minY; y <= this.maxY; y++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					blocksSeen += 1.0D;
					if (blocks.containsKey(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId())))
						blocks.put(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(((Integer) blocks.get(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()))).intValue() + 1));
					else if (!blocks.containsKey(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()))) {
						blocks.put(Integer.valueOf(this.world.getBlockAt(x, y, z).getTypeId()), Integer.valueOf(1));
					}
				}
			}
		}
		// Because some armadillo says so, we're adding the functionality to negate air blocks already in it. isn't that great.
		if (blocks.containsKey(0)) {
			if (air != 0) {
				blocks.put(0, blocks.get(0) - air);
			}
		}
		Map<Integer, Double> percentageComposition = new HashMap<Integer, Double>();
		for (Map.Entry<Integer, Integer> entry : blocks.entrySet()) {
			percentageComposition.put(entry.getKey(), Double.valueOf(entry.getValue().intValue() / blocksSeen));
		}

		boolean resettable = false;
		for (ResetCriterion m : this.criteria) {
			if (percentageComposition.containsKey(Integer.valueOf(m.getBlock().getBlockId()))) {
				if (m.isCriteriaMatched((percentageComposition.get(Integer.valueOf(m.getBlock().getBlockId()))).doubleValue())) {
					resettable = true;
				}
			} else {
				resettable = m.getOperator() == '<';
			}
		}
		if (sender != null) {
			for (Map.Entry entry : percentageComposition.entrySet()) {
				sender.sendMessage(Phrases.phrase("criteriaResp", new Object[]{Material.getMaterial(((Integer) entry.getKey()).intValue()), Double.valueOf(((Double) entry.getValue()).doubleValue() * 100.0D)}));
			}
		}
		return resettable;
	}

	public void cron2() {
		if (seconds <= 1) {
			cron();
			seconds = 60;
		} else {
			seconds--;
		}
		/* Hologram updates & Time */
		if (!isDone()) {
			AsyncWorldEditMain async = AsyncWorldEditMain.getInstance();
			BlockPlacerPlayer p = async.getBlockPlacer().getPlayerEvents(PlayerEntry.UNKNOWN);
			if (total != 0) {
				int left = 0;
				if (p != null)
					if (p.getQueue() != null) {
						synchronized (p.getJobs()) {
							for (BlockPlacerEntry e : p.getQueue()) {
								if (e.getJobId() == this.jobID) {
									left++;
								}
							}
							speed.add(p.getSpeed());
							if (seconds % 10 == 0) {
								this.secondsLeft = (int) ((double) left / averageSpeed());
//							System.out.println(secondsLeft + " <- Predicted left");
//							System.out.println(left + " blocks left.");
							/* Hologram */
								if (holo != null)
									if (!holo.equals("")) {
										if (this.hologram == null) {
											Location l = new Location(world, Integer.parseInt(holo.split(",")[0]), Integer.parseInt(holo.split(",")[1]), Integer
													.parseInt(holo
															.split(",")[2])).add(0, 0.5, 0);
											replace = getNearestHolo(l);
											if (replace != null) {
												System.out.println("Found existing holo and replacing");
												replace.getVisibilityManager().setVisibleByDefault(false);
											}
											hologram = HologramsAPI.createHologram(MineResetLite.instance, replace != null ? replace.getLocation() : l);
										}
										double percent = (double) ((double) left / (double) total);
										percent = percent * 100D;
										int blocksLeft = (int) (percent / 10D);
										//int done = 5 - blocksLeft;
										this.hologram.clearLines();
										StringBuilder sb = new StringBuilder();

										for (int i = 1; i <= 10; i++) {

											// 1 Left
											// 10 - 1 = 9
											// if i >= total
											// 10 - 3 = 7
											// 6 >= 7
											// 10 - 3 = 7
											// Green 7
											// 8, 9, 10 Black
											if (i >= ((10 - blocksLeft))) {
												sb.append(Phrases.phrase("progressPrimary") + "█");
											} else {
												sb.append(Phrases.phrase("progressSecondary") + "█");
											}
										}
										String d = Phrases.phrase("progressLayout");
										String[] s = d.split("\\|");
										for (String x : s) {
											x = x.replace("%progressbar", sb.toString());
											x = x.replace("%percent", ((int) percent) + "");
											x = x.replace("%seconds", secondsLeft + "");
											this.hologram.appendTextLine(ChatColor.translateAlternateColorCodes('&', x));
										}
									}
							}
						}

					}
			}

		} else {
			if (hologram != null) {
				if (replace != null) {
					replace.getVisibilityManager().setVisibleByDefault(true);
				}
				hologram.delete();
				hologram = null;
			}
		}
		if (!isDone())
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				Location l = p.getLocation();
				if (isInside(p) && !p.isOp()) {
					p.sendMessage(Phrases.phrase("mineResetting", new Object[]{this, secondsLeft}));
					if (this.teleport != null) {
						if (!this.teleport.equals("")) {
							p.teleport(new Location(world, Integer.parseInt(this.teleport.split(",")[0]), Integer.parseInt(this.teleport.split(",")[1]), Integer.parseInt(this.teleport.split(",")[2]),
									Float.parseFloat(this.teleport.split(",")[3]), Float.parseFloat(this.teleport.split(",")[4])));
						} else {
							p.teleport(new Location(world, l.getX(), maxY + 2D, l.getZ()));
						}
					} else {
						p.teleport(new Location(world, l.getX(), maxY + 2D, l.getZ()));
					}

				}
			}
	}

	private Hologram getNearestHolo(Location l) {
		for (Hologram h : NamedHologramManager.getHolograms()) {
			if (h.getLocation().getWorld().equals(l.getWorld()))
				if (h.getLocation().distance(l) < 6) {
					return h;
				}
		}
		return null;
	}

	private double averageSpeed() {
		double x = 0;
		for (Double d : this.speed) {
			x += d;
		}
		return x / this.speed.size();
	}

	private void broadcastNearby(String mineAutoResetBroadcast) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (isNear(p)) {
				p.sendMessage(mineAutoResetBroadcast);
			}
		}
	}

	private boolean isNear(Player p) {
		Location l = p.getLocation();
		return l.getWorld().equals(world)
				&& (l.getX() >= minX - 10 && l.getX() <= maxX + 10)
				&& (l.getY() >= minY - 10 && l.getY() <= maxY + 10)
				&& (l.getZ() >= minZ - 10 && l.getZ() <= maxZ + 10);
	}

	public void cron() {
		if (this.checkOffset > 0) {
			this.checkOffset -= 1;
		}
		if ((this.checkOffset == 0) && (this.criteria.size() > 0) && (shouldMineReset(null))) {
			this.checkOffset = 2;
			if (!this.isSilent) {
				MineResetLite.broadcast(Phrases.phrase("mineWarningBroadcast", new Object[]{this, Integer.valueOf(1)}), this);
			}
			final boolean isS = this.isSilent;
			final Mine self = this;
			Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MineResetLite"), new Runnable() {
				public void run() {
					self.reset();
					if (!isS)
						MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", new Object[]{self}), self);
				}
			}
					, 1200L);
		}
		renderSigns();
		if (resetDelay == 0) {
			return;
		}
		if (resetClock > 0) {
			resetClock--; //Tick down to the reset
		}
		if (resetClock == 0) {
			if (!isSilent) {
				MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", this), this);
			}
			reset();
			resetClock = resetDelay;
			return;
		}
		for (Integer warning : resetWarnings) {
			if (warning == resetClock) {
				MineResetLite.broadcast(Phrases.phrase("mineWarningBroadcast", this, warning), this);
			}
		}

	}

	public int attemptReset(Player p) {
		Long d = System.currentTimeMillis() - lasttried;
		if (d <= (1000 * cooldown)) {
			return (int) (cooldown - (d / 1000));
		} else {
			lasttried = System.currentTimeMillis();
			if (!isSilent) {
				MineResetLite.broadcast(Phrases.phrase("mineAutoResetBroadcast", this), this);
			}
			reset();
		}
		return 0;
	}

	public void setSchematic(String schematic) {
		this.schematic = schematic;
	}

	public String getSchematic() {
		return schematic;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isPending() {
		return pending;
	}

	public void setPending(boolean pending) {
		this.pending = pending;
	}

	public static class CompositionEntry {
		private SerializableBlock block;
		private double chance;

		public CompositionEntry(SerializableBlock block, double chance) {
			this.block = block;
			this.chance = chance;
		}

		public SerializableBlock getBlock() {
			return block;
		}

		public double getChance() {
			return chance;
		}
	}

	public static ArrayList<CompositionEntry> mapComposition(Map<SerializableBlock, Double> compositionIn) {
		ArrayList<CompositionEntry> probabilityMap = new ArrayList<CompositionEntry>();
		Map<SerializableBlock, Double> composition = new HashMap<SerializableBlock, Double>(compositionIn);
		double max = 0;
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			max += entry.getValue();
		}
		//Pad the remaining percentages with air
		if (max < 1) {
			composition.put(new SerializableBlock(0), 1 - max);
			max = 1;
		}
		double i = 0;
		for (Map.Entry<SerializableBlock, Double> entry : composition.entrySet()) {
			double v = entry.getValue() / max;
			i += v;
			probabilityMap.add(new CompositionEntry(entry.getKey(), i));
		}
		return probabilityMap;
	}

	public static class ResetCriterion {
		private SerializableBlock block;
		private char operator;
		private double reference;

		public ResetCriterion(SerializableBlock block, char operator, double reference) {
			this.block = block;
			this.operator = operator;
			this.reference = reference;
		}

		public ResetCriterion(String me) {
			String[] bits = me.split("\\|");
			this.block = new SerializableBlock(bits[0]);
			this.operator = bits[1].charAt(0);
			this.reference = Double.valueOf(bits[2]).doubleValue();
		}

		public SerializableBlock getBlock() {
			return this.block;
		}

		public char getOperator() {
			return this.operator;
		}

		public double getReference() {
			return this.reference;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(this.block.toString());
			sb.append("|");
			sb.append(this.operator);
			sb.append("|");
			sb.append(this.reference);
			return sb.toString();
		}

		public boolean isCriteriaMatched(double comparison) {
			if (this.operator == '>')
				return comparison > this.reference;
			if (this.operator == '<') {
				return comparison < this.reference;
			}
			return false;
		}
	}
}
