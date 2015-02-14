package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class MineCommands {
	private MineResetLite plugin;
	private Map<Player, Location> point1;
	private Map<Player, Location> point2;

	public MineCommands(MineResetLite plugin) {
		this.plugin = plugin;
		point1 = new HashMap<Player, Location>();
		point2 = new HashMap<Player, Location>();
	}

	@Command(aliases = {"list", "l"},
			description = "List the names of all Mines",
			permissions = {"mineresetlite.mine.list"},
			help = {"List the names of all Mines currently created, across all worlds."},
			min = 0, max = 0, onlyPlayers = false)
	public void listMines(CommandSender sender, String[] args) {
		sender.sendMessage(phrase("mineList", StringTools.buildList(plugin.mines, "&c", "&d, ")));
	}

	@Command(aliases = {"pos1", "p1"},
			description = "Change your first selection point",
			help = {"Run this command to set your first selection point to the block you are looking at.",
					"Use /mrl pos1 -feet to set your first point to the location you are standing on."},
			usage = "(-feet)",
			permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
			min = 0, max = 1, onlyPlayers = true)
	public void setPoint1(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		if (args.length == 0) {
			//Use block being looked at
			point1.put(player, player.getTargetBlock(null, 100).getLocation());
			player.sendMessage(phrase("firstPointSet"));
			return;
		} else if (args[0].equalsIgnoreCase("-feet")) {
			//Use block being stood on
			point1.put(player, player.getLocation());
			player.sendMessage(phrase("firstPointSet"));
			return;
		}
		//Args weren't empty or -feet, bad args
		throw new InvalidCommandArgumentsException();
	}

	@Command(aliases = {"pos2", "p2"},
			description = "Change your first selection point",
			help = {"Run this command to set your second selection point to the block you are looking at.",
					"Use /mrl pos2 -feet to set your second point to the location you are standing on."},
			usage = "(-feet)",
			permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
			min = 0, max = 1, onlyPlayers = true)
	public void setPoint2(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		if (args.length == 0) {
			//Use block being looked at
			point2.put(player, player.getTargetBlock(null, 100).getLocation());
			player.sendMessage(phrase("secondPointSet"));
			return;
		} else if (args[0].equalsIgnoreCase("-feet")) {
			//Use block being stood on
			point2.put(player, player.getLocation());
			player.sendMessage(phrase("secondPointSet"));
			return;
		}
		//Args weren't empty or -feet, bad args
		throw new InvalidCommandArgumentsException();
	}

	@Command(aliases = {"create", "save"},
			description = "Create a mine from either your WorldEdit selection or by manually specifying the points",
			help = {"Provided you have a selection made via either WorldEdit or selecting the points using MRL,",
					"an empty mine will be created. This mine will have no composition and default settings."},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.create"},
			min = 1, max = -1, onlyPlayers = true)
	public void createMine(CommandSender sender, String[] args) {
		//Find out how they selected the region
		Player player = (Player) sender;
		World world = null;
		Vector p1 = null;
		Vector p2 = null;
		//Native selection techniques?
		if (point1.containsKey(player) && point2.containsKey(player)) {
			world = point1.get(player).getWorld();
			if (!world.equals(point2.get(player).getWorld())) {
				player.sendMessage(phrase("crossWorldSelection"));
				return;
			}
			p1 = point1.get(player).toVector();
			p2 = point2.get(player).toVector();
		}
		//WorldEdit?
		if (plugin.hasWorldEdit() && plugin.getWorldEdit().getSelection(player) != null) {
			WorldEditPlugin worldEdit = plugin.getWorldEdit();
			Selection selection = worldEdit.getSelection(player);
			world = selection.getWorld();
			p1 = selection.getMinimumPoint().toVector();
			p2 = selection.getMaximumPoint().toVector();
		}
		if (p1 == null) {
			player.sendMessage(phrase("emptySelection"));
			return;
		}
		//Construct mine name
		String name = StringTools.buildSpacedArgument(args);
		//Verify uniqueness of mine name
		Mine[] mines = plugin.matchMines(name);
		if (mines.length > 0) {
			player.sendMessage(phrase("nameInUse", name));
			return;
		}
		//Sort coordinates
		if (p1.getX() > p2.getX()) {
			//Swap
			double x = p1.getX();
			p1.setX(p2.getX());
			p2.setX(x);
		}
		if (p1.getY() > p2.getY()) {
			double y = p1.getY();
			p1.setY(p2.getY());
			p2.setY(y);
		}
		if (p1.getZ() > p2.getZ()) {
			double z = p1.getZ();
			p1.setZ(p2.getZ());
			p2.setZ(z);
		}
		//Create!
		Mine newMine = new Mine(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(), name, world);
		plugin.mines.add(newMine);
		player.sendMessage(phrase("mineCreated", newMine));
		plugin.buffSave();
	}

	@Command(aliases = {"info", "i"},
			description = "List information about a mine",
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.info"},
			min = 1, max = -1, onlyPlayers = false)
	public void mineInfo(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines"));
			return;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		sender.sendMessage(phrase("mineInfoName", mines[0]));
		sender.sendMessage(phrase("mineInfoWorld", mines[0].getWorld()));
		//Build composition list
		StringBuilder csb = new StringBuilder();
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			csb.append(entry.getValue() * 100);
			csb.append("% ");
			csb.append(Material.getMaterial(entry.getKey().getBlockId()).toString());
			if (entry.getKey().getData() != 0) {
				csb.append(":");
				csb.append(entry.getKey().getData());
			}
			csb.append(", ");
		}
		if (csb.length() > 2) {
			csb.delete(csb.length() - 2, csb.length() - 1);
		}
		sender.sendMessage(phrase("mineInfoComposition", csb));
		if (mines[0].getResetDelay() != 0) {
			sender.sendMessage(phrase("mineInfoResetDelay", mines[0].getResetDelay()));
			sender.sendMessage(phrase("mineInfoTimeUntilReset", mines[0].getTimeUntilReset()));
		}
		sender.sendMessage(phrase("mineInfoSilence", mines[0].isSilent()));
		if (mines[0].getResetWarnings().size() > 0) {
			sender.sendMessage(phrase("mineInfoWarningTimes", StringTools.buildList(mines[0].getResetWarnings(), "", ", ")));
		}
		if (mines[0].getSurface() != null) {
			sender.sendMessage(phrase("mineInfoSurface", mines[0].getSurface()));
		}
		if (mines[0].getFillMode()) {
			sender.sendMessage(phrase("mineInfoFillMode"));
		}
		if (mines[0].origin != null) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Create schematic at origin: " + ChatColor.WHITE + mines[0].origin);
		} else {
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Create schematic at origin: " + ChatColor.WHITE + mines[0].minX + ", " + mines[0].maxY + ", " + mines[0].minZ);
		}

		sender.sendMessage(mines[0].getSchematic() == null ? ChatColor.LIGHT_PURPLE + "Not using a schematic" : ChatColor.LIGHT_PURPLE + "Using schematic: " + ChatColor.WHITE + mines[0].getSchematic
				());
		if (mines[0].criteria.size() > 0) {
			sender.sendMessage("§dReset Criteria:");
			for (Mine.ResetCriterion rc : mines[0].criteria) {
				sender.sendMessage(new StringBuilder().append("§c").append(rc.getOperator() == '>' ? "greater than " : "less than ").append(rc.getReference() * 100.0D).append("% ").append(Material.getMaterial(rc.getBlock().getBlockId()).toString()).toString());
			}
			sender.sendMessage("§dCurrent Composition:");
			sender.sendMessage(new StringBuilder().append("§dShould mine reset: §c").append(mines[0].shouldMineReset(sender)).toString());
		}
	}

	@Command(aliases = {"set", "add", "+"},
			description = "Set the percentage of a block in the mine",
			help = {"This command will always overwrite the current percentage for the specified block,",
					"if a percentage has already been set. You cannot set the percentage of any specific",
					"block, such that the percentage would then total over 100%."},
			usage = "<mine name> <block>:(data) <percentage>%",
			permissions = {"mineresetlite.mine.composition"},
			min = 3, max = -1, onlyPlayers = false)
	public void setComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines"));
			return;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		//Match material
		String[] bits = args[args.length - 2].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (m == null) {
			sender.sendMessage(phrase("unknownBlock"));
			return;
		}
		if (!m.isBlock()) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		//Parse percentage
		String percentageS = args[args.length - 1];
		if (!percentageS.endsWith("%")) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		StringBuilder psb = new StringBuilder(percentageS);
		psb.deleteCharAt(psb.length() - 1);
		double percentage = 0;
		try {
			percentage = Double.valueOf(psb.toString());
		} catch (NumberFormatException nfe) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		if (percentage > 100 || percentage <= 0) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		percentage = percentage / 100; //Make it a programmatic percentage
		SerializableBlock block = new SerializableBlock(m.getId(), data);
		Double oldPercentage = mines[0].getComposition().get(block);
		double total = 0;
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			if (!entry.getKey().equals(block)) {
				total += entry.getValue();
			} else {
				block = entry.getKey();
			}
		}
		total += percentage;
		if (total > 1) {
			sender.sendMessage(phrase("insaneCompositionChange"));
			if (oldPercentage == null) {
				mines[0].getComposition().remove(block);
			} else {
				mines[0].getComposition().put(block, oldPercentage);
			}
			return;
		}
		mines[0].getComposition().put(block, percentage);
		sender.sendMessage(phrase("mineCompositionSet", mines[0], percentage * 100, block, (1 - mines[0].getCompositionTotal()) * 100));
		plugin.buffSave();
	}

	@Command(aliases = {"unset", "remove", "-"},
			description = "Remove a block from the composition of a mine",
			usage = "<mine name> <block>:(data)",
			permissions = {"mineresetlite.mine.composition"},
			min = 2, max = -1, onlyPlayers = false)
	public void unsetComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines"));
			return;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		//Match material
		String[] bits = args[args.length - 1].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (m == null) {
			sender.sendMessage(phrase("unknownBlock"));
			return;
		}
		if (!m.isBlock()) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		//Does the mine contain this block?
		SerializableBlock block = new SerializableBlock(m.getId(), data);
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			if (entry.getKey().equals(block)) {
				mines[0].getComposition().remove(entry.getKey());
				sender.sendMessage(phrase("blockRemovedFromMine", mines[0], block, (1 - mines[0].getCompositionTotal()) * 100));
				return;
			}
		}
		sender.sendMessage(phrase("blockNotInMine", mines[0], block));
		plugin.buffSave();
	}

	@Command(aliases = {"reset", "r"},
			description = "Reset a mine",
			help = {"If you supply the -s argument, the mine will silently reset. Resets triggered via",
					"this command will not show a 1 minute warning, unless this mine is flagged to always",
					"have a warning. If the mine's composition doesn't equal 100%, the composition will be",
					"padded with air until the total equals 100%."},
			usage = "<mine name> (-s)",
			permissions = {"mineresetlite.mine.reset"},
			min = 1, max = -1, onlyPlayers = false)
	public void resetMine(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args).replace(" -s", ""));
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines"));
			return;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		if (args[args.length - 1].equalsIgnoreCase("-s")) {
			//Silent reset
			mines[0].reset();
		} else {
			MineResetLite.broadcast(phrase("mineResetBroadcast", mines[0], sender), mines[0]);
			mines[0].reset();
		}
	}

	@Command(aliases = {"flag", "f"},
			description = "Set various properties of a mine, including automatic resets",
			help = {"Available flags:",
					"resetDelay: An integer number of minutes specifying the time between automatic resets. Set to 0 to disable automatic resets.",
					"resetWarnings: A comma separated list of integer minutes to warn before the automatic reset. Warnings must be less than the reset delay.",
					"surface: A block that will cover the entire top surface of the mine when reset, obscuring surface ores. Set surface to air to clear the value.",
					"fillMode: An alternate reset algorithm that will only \"reset\" air blocks inside your mine. Set to true or false.",
					"isSilent: A boolean (true or false) of whether or not this mine should broadcast a reset notification when it is reset *automatically*",
					"schematic: The name of a schematic in the worldedit/schematics folder for using on reset, do not include the .schematic extension. Put none for no schematic.",
					"addsign: Adds a sign to the sign list for the plugin.",
					"cooldown: The number of seconds between use of the RESET sign",
					"teleport: Set the teleport for the mine when it resets.",
					"hologram: Set the hologram location for reset.",
					"everyone: Should it broadcast to everyone, default no. Overrides config."},
			usage = "<mine name> <setting> <value>",
			permissions = {"mineresetlite.mine.flag"},
			min = 3, max = -1, onlyPlayers = false)
	public void flag(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines"));
			return;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		String setting = args[args.length - 2];
		String value = args[args.length - 1];
		if (setting.equalsIgnoreCase("resetEvery") || setting.equalsIgnoreCase("resetDelay") || setting.equalsIgnoreCase("cooldown")) {
			int delay;
			try {
				delay = Integer.valueOf(value);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("badResetDelay"));
				return;
			}
			if (setting.equalsIgnoreCase("cooldown")) {
				mines[0].setCooldown(delay);
				sender.sendMessage("Set sign cooldown for the mine.");
			} else {
				if (delay < 0) {
					sender.sendMessage(phrase("badResetDelay"));
					return;
				}
				mines[0].setResetDelay(delay);
				if (delay == 0) {
					sender.sendMessage(phrase("resetDelayCleared", mines[0]));
				} else {
					sender.sendMessage(phrase("resetDelaySet", mines[0], delay));
				}
			}
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("resetWarnings") || setting.equalsIgnoreCase("resetWarning")) {
			String[] bits = value.split(",");
			List<Integer> warnings = mines[0].getResetWarnings();
			List<Integer> oldList = new LinkedList<Integer>(warnings);
			warnings.clear();
			for (String bit : bits) {
				try {
					warnings.add(Integer.valueOf(bit));
				} catch (NumberFormatException nfe) {
					sender.sendMessage(phrase("badWarningList"));
					return;
				}
			}
			//Validate warnings
			for (Integer warning : warnings) {
				if (warning >= mines[0].getResetDelay()) {
					sender.sendMessage(phrase("badWarningList"));
					mines[0].setResetWarnings(oldList);
					return;
				}
			}
			if (warnings.contains(0) && warnings.size() == 1) {
				warnings.clear();
				sender.sendMessage(phrase("warningListCleared", mines[0]));
				return;
			} else if (warnings.contains(0)) {
				sender.sendMessage(phrase("badWarningList"));
				mines[0].setResetWarnings(oldList);
				return;
			}
			sender.sendMessage(phrase("warningListSet", mines[0]));
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("surface")) {
			//Match material
			String[] bits = value.split(":");
			Material m = plugin.matchMaterial(bits[0]);
			if (m == null) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
			if (!m.isBlock()) {
				sender.sendMessage(phrase("notABlock"));
				return;
			}
			byte data = 0;
			if (bits.length == 2) {
				try {
					data = Byte.valueOf(bits[1]);
				} catch (NumberFormatException nfe) {
					sender.sendMessage(phrase("unknownBlock"));
					return;
				}
			}
			if (m.equals(Material.AIR)) {
				mines[0].setSurface(null);
				sender.sendMessage(phrase("surfaceBlockCleared", mines[0]));
				plugin.buffSave();
				return;
			}
			SerializableBlock block = new SerializableBlock(m.getId(), data);
			mines[0].setSurface(block);
			sender.sendMessage(phrase("surfaceBlockSet", mines[0]));
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("fill") || setting.equalsIgnoreCase("fillMode")) {
			//Match true or false
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
				mines[0].setFillMode(true);
				sender.sendMessage(phrase("fillModeEnabled"));
				plugin.buffSave();
				return;
			} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
				mines[0].setFillMode(false);
				sender.sendMessage(phrase("fillModeDisabled"));
				plugin.buffSave();
				return;
			}
			sender.sendMessage(phrase("invalidFillMode"));
		} else if (setting.equalsIgnoreCase("isSilent") || setting.equalsIgnoreCase("silent") || setting.equalsIgnoreCase("silence")) {
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
				mines[0].setSilence(true);
				sender.sendMessage(phrase("mineIsNowSilent", mines[0]));
				plugin.buffSave();
				return;
			} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
				mines[0].setSilence(false);
				sender.sendMessage(phrase("mineIsNoLongerSilent", mines[0]));
				plugin.buffSave();
				return;
			}
			sender.sendMessage(phrase("badBoolean"));
		} else if (setting.equalsIgnoreCase("everyone")) {
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
				mines[0].everyone = true;
				sender.sendMessage(ChatColor.RED + "Mine will broadcast to everyone.");
				plugin.buffSave();
				return;
			} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
				mines[0].everyone = false;
				sender.sendMessage(ChatColor.RED + "Mine will no longer broadcast to everyone.");
				plugin.buffSave();
				return;
			}
			sender.sendMessage(phrase("badBoolean"));
		} else if (setting.equalsIgnoreCase("schematic")) {
			if (value.equalsIgnoreCase("none")) value = null;
			String schematic = value;
			if (schematic.contains(",")) {
				String[] s = schematic.split(",");
				for (String x : s) {
					File f = new File("plugins/WorldEdit/schematics/" + x + ".schematic");
					if (!f.exists()) {
						sender.sendMessage(ChatColor.RED + "Schematic not found: " + x + ". Put it in the WorldEdit Schematic folder.");
						return;
					}
				}
			} else {
				File f = new File("plugins/WorldEdit/schematics/" + schematic + ".schematic");
				if (!f.exists()) {
					sender.sendMessage(ChatColor.RED + "Schematic not found: " + value + ". Put it in the WorldEdit Schematic folder.");
					return;
				}
			}
			mines[0].setSchematic(value);
			if (sender instanceof Player) {
				Location l = ((Player) sender).getLocation();
				mines[0].origin = l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
			}
			sender.sendMessage(phrase("schematicSet", mines[0]));
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("addsign")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				Block b = p.getTargetBlock(null, 10);
				if (b != null) {
					if (b.getState() instanceof Sign) {
						mines[0].addSign((Sign) b.getState());
						sender.sendMessage(ChatColor.LIGHT_PURPLE + "Updated Sign");
						plugin.buffSave();
						return;
					}
				}
			}
			sender.sendMessage(ChatColor.RED + "You must look at a sign first...");
			return;
		} else if (setting.equalsIgnoreCase("teleport")) {
			if (sender instanceof Player) {
				Location l = ((Player) sender).getLocation();
				mines[0].teleport = l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + l.getYaw() + "," + l.getPitch();
			}
			sender.sendMessage(ChatColor.RED + "Teleport has been set");
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("hologram")) {
			if (sender instanceof Player) {
				Location l = ((Player) sender).getLocation();
				mines[0].holo = l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + l.getYaw() + "," + l.getPitch();
			}
			sender.sendMessage(ChatColor.RED + "Hologram has been set.");
			plugin.buffSave();
			return;
		}

		sender.sendMessage(phrase("unknownFlag"));
	}

	@Command(aliases = {"erase"},
			description = "Completely erase a mine",
			help = {"Like most erasures of data, be sure you don't need to recover anything from this mine before you delete it."},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.erase"},
			min = 1, max = -1, onlyPlayers = false)
	public void erase(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines"));
			return;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		plugin.eraseMine(mines[0]);
		sender.sendMessage(phrase("mineErased", mines[0]));
	}

	@Command(aliases = {"reschedule"},
			description = "Synchronize all automatic mine resets",
			help = {"This command will set the 'start time' of the mine resets to the same point."},
			usage = "",
			permissions = {"mineresetlite.mine.flag"},
			min = 0, max = 0, onlyPlayers = false)
	public void reschedule(CommandSender sender, String[] args) {
		for (Mine mine : plugin.mines) {
			mine.setResetDelay(mine.getResetDelay());
		}
		plugin.buffSave();
		sender.sendMessage(phrase("rescheduled"));
	}

	@Command(aliases = {"cset", "cadd", "c+"}, description = "Set a reset criteria for the mine", help = {"This command will always overwrite the current percentage for the specified block,", "if a percentage has already been set."}, usage = "<mine name> <block>:(data) <greater/less> <percentage>%", permissions = {"mineresetlite.mine.composition"}, min = 4, max = -1, onlyPlayers = false)
	public void addCriteria(CommandSender sender, String[] args) {
		Mine[] mines = this.plugin.matchMines(StringTools.buildSpacedArgument(args, 3));
		if (mines.length > 1) {
			sender.sendMessage(Phrases.phrase("tooManyMines", new Object[0]));
			return;
		}
		if (mines.length == 0) {
			sender.sendMessage(Phrases.phrase("noMinesMatched", new Object[0]));
			return;
		}

		String[] bits = args[(args.length - 3)].split(":");
		Material m = this.plugin.matchMaterial(bits[0]);
		if (m == null) {
			sender.sendMessage(Phrases.phrase("unknownBlock", new Object[0]));
			return;
		}
		if (!m.isBlock()) {
			sender.sendMessage(Phrases.phrase("notABlock", new Object[0]));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]).byteValue();
			} catch (NumberFormatException nfe) {
				sender.sendMessage(Phrases.phrase("unknownBlock", new Object[0]));
				return;
			}
		}

		String operatorS = args[(args.length - 2)];
		char operator;
		if (("greaterthan".equalsIgnoreCase(operatorS)) || ("greater".equalsIgnoreCase(operatorS)) || (">".equalsIgnoreCase(operatorS))) {
			operator = '>';
		} else {
			if (("lessthan".equalsIgnoreCase(operatorS)) || ("less".equalsIgnoreCase(operatorS)) || ("<".equalsIgnoreCase(operatorS))) {
				operator = '<';
			} else {
				sender.sendMessage("§4Unknown operator specified. Valid operators are: greaterthan, lessthan");
				return;
			}
		}
		String percentageS = args[(args.length - 1)];
		if (!percentageS.endsWith("%")) {
			sender.sendMessage(Phrases.phrase("badPercentage", new Object[0]));
			return;
		}
		StringBuilder psb = new StringBuilder(percentageS);
		psb.deleteCharAt(psb.length() - 1);
		double percentage = 0.0D;
		try {
			percentage = Double.valueOf(psb.toString()).doubleValue();
		} catch (NumberFormatException nfe) {
			sender.sendMessage(Phrases.phrase("badPercentage", new Object[0]));
			return;
		}
		if ((percentage > 100.0D) || (percentage <= 0.0D)) {
			sender.sendMessage(Phrases.phrase("badPercentage", new Object[0]));
			return;
		}
		percentage /= 100.0D;
		SerializableBlock block = new SerializableBlock(m.getId(), data);
		Mine.ResetCriterion rc = new Mine.ResetCriterion(block, operator, percentage);
		mines[0].criteria.add(rc);
		sender.sendMessage(Phrases.phrase("criterionAdded", new Object[]{mines[0], operator == '>' ? "greater than" : "less than", psb.toString(), m}));
		this.plugin.buffSave();
	}

	@Command(aliases = {"cunset", "cremove", "c-"}, description = "Remove a reset criteria for the mine", help = {"Removes a reset criterion for the block"}, usage = "<mine name> <block>:(data)", permissions = {"mineresetlite.mine.composition"}, min = 2, max = -1, onlyPlayers = false)
	public void rmCriteria(CommandSender sender, String[] args) {
		Mine[] mines = this.plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
		if (mines.length > 1) {
			sender.sendMessage(Phrases.phrase("tooManyMines", new Object[0]));
			return;
		}
		if (mines.length == 0) {
			sender.sendMessage(Phrases.phrase("noMinesMatched", new Object[0]));
			return;
		}

		String[] bits = args[(args.length - 1)].split(":");
		Material m = this.plugin.matchMaterial(bits[0]);
		if (m == null) {
			sender.sendMessage(Phrases.phrase("unknownBlock", new Object[0]));
			return;
		}
		if (!m.isBlock()) {
			sender.sendMessage(Phrases.phrase("notABlock", new Object[0]));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]).byteValue();
			} catch (NumberFormatException nfe) {
				sender.sendMessage(Phrases.phrase("unknownBlock", new Object[0]));
				return;
			}
		}
		SerializableBlock block = new SerializableBlock(m.getId(), data);

		List<Mine.ResetCriterion> pendingDel = new LinkedList();
		for (Mine.ResetCriterion rc : mines[0].criteria) {
			if (rc.getBlock().equals(block)) {
				pendingDel.add(rc);
			}
		}

		for (Mine.ResetCriterion rc : pendingDel) {
			mines[0].criteria.remove(rc);
		}
		sender.sendMessage(new StringBuilder().append("§dRemoved §c").append(pendingDel.size()).append("§d reset criteria.").toString());
	}
}
