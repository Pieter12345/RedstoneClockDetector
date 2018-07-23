package me.hwei.bukkit.redstoneClockDetector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import me.hwei.bukkit.redstoneClockDetector.commands.BreakCommand;
import me.hwei.bukkit.redstoneClockDetector.commands.ListCommand;
import me.hwei.bukkit.redstoneClockDetector.commands.StartCommand;
import me.hwei.bukkit.redstoneClockDetector.commands.StatusCommand;
import me.hwei.bukkit.redstoneClockDetector.commands.StopCommand;
import me.hwei.bukkit.redstoneClockDetector.commands.TeleportCommand;
import me.hwei.bukkit.redstoneClockDetector.util.AbstractCommand;
import me.hwei.bukkit.redstoneClockDetector.util.IOutput;
import me.hwei.bukkit.redstoneClockDetector.util.OutputManager;
import me.hwei.bukkit.redstoneClockDetector.util.PermissionsException;
import me.hwei.bukkit.redstoneClockDetector.util.UsageException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RCDPlugin extends JavaPlugin implements CommandExecutor {
	
	// Variables & Constants.
	private RedstoneUpdateListener redstoneUpdateListener = null;
	private ArrayList<Entry<Location, Integer>> redstoneActivityList = null;
	private Worker worker = null;
	private CommandSender sender = null;
	private int taskId = Integer.MIN_VALUE;
	private IOutput toConsole = null;
	private AbstractCommand topCommand = null;

	@Override
	public void onDisable() {
		this.stop();
		this.redstoneUpdateListener = null;
		this.redstoneActivityList = null;
		this.toConsole.output("Disabled.");
	}

	@Override
	public void onEnable() {
		IOutput toConsole = new IOutput() {
			@Override
			public void output(String message) {
				getServer().getConsoleSender().sendMessage(message);
			}
		};
		IOutput toAll = new IOutput() {
			@Override
			public void output(String message) {
				getServer().broadcastMessage(message);
			}
		};
		OutputManager.IPlayerGetter playerGetter = new OutputManager.IPlayerGetter() {
			@Override
			public Player get(String name) {
				return getPlayer(name);
			}
		};
		String pluginName = this.getDescription().getName();
		OutputManager.Setup(
				"[" + ChatColor.YELLOW + pluginName + ChatColor.WHITE + "] ",
				toConsole, toAll, playerGetter);
		this.toConsole = OutputManager.GetInstance().prefix(toConsole);
		this.toConsole.output("Enabled.");
		
		this.redstoneUpdateListener = new RedstoneUpdateListener(this);
		this.redstoneActivityList = new ArrayList<Entry<Location, Integer>>();
		this.stop();
		
		this.setupCommands();
	}
	
	protected boolean setupCommands() {
		try {
			ListCommand listCommand = new ListCommand(
					"list [page]  List locations of redstone activities.",
					"redstoneclockdetector.list",
					null, this);
			AbstractCommand[] childCommands = new AbstractCommand[] {
					new StartCommand(
							"<sec>  Start scan for <sec> seconds.",
							"redstoneclockdetector.start",
							null, this, listCommand),
					new StopCommand(
							"stop  Stop scan.",
							"redstoneclockdetector.stop",
							null, this),
					listCommand,
					new TeleportCommand(
							"tp [player] [num]  Teleport player [player] to place of number [num] in list.",
							"redstoneclockdetector.tp",
							null, this),
					new BreakCommand(
							"break <num>  Break the block at place of number <num> in list.",
							"redstoneclockdetector.break",
							null, this),
			};
			
			this.topCommand = new StatusCommand(
					"  Status of plugin.",
					"redstoneclockdetector",
					childCommands, this);
			
		} catch (Exception e) {
			this.toConsole.output("Can not setup commands!");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public List<Entry<Location, Integer>> getRedstoneActivityList() {
		return this.redstoneActivityList;
	}
	public CommandSender getUser() {
		return this.sender;
	}
	public int getSecondsRemain() {
		if(this.taskId == Integer.MIN_VALUE)
			return -1;
		return this.worker.getSecondsRemain();
	}
	public interface IProgressReporter {
		public void onProgress(int secondsRemain);
	}
	protected class Worker implements Runnable {
		public Worker(int seconds, IProgressReporter progressReporter) {
			this.progressReporter = progressReporter;
			this.secondsRemain = seconds;
		}
		@Override
		public void run() {
			if(this.secondsRemain <= 0)
			{
				if(RCDPlugin.this.stop() && this.progressReporter != null)
					this.progressReporter.onProgress(secondsRemain);
			} else {
				if(this.progressReporter != null)
					this.progressReporter.onProgress(secondsRemain);
				this.secondsRemain--;
			}

		}
		public int getSecondsRemain() {
			return this.secondsRemain;
		}
		protected IProgressReporter progressReporter;
		protected int secondsRemain;
	}
	public boolean start(CommandSender sender, int seconds, IProgressReporter progressReporter ) {
		if(this.taskId != Integer.MIN_VALUE) {
			return false;
		}
		this.redstoneUpdateListener.startListener();
		this.sender = sender;
		this.worker = new Worker(seconds, progressReporter);
		this.taskId = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, this.worker, 0L, 20L);
		return true;
	}
	public boolean stop() {
		if(this.taskId != Integer.MIN_VALUE) {
			this.getServer().getScheduler().cancelTask(this.taskId);
			this.taskId = Integer.MIN_VALUE;
			this.sender = null;
			this.worker = null;
			this.sortList();
			this.redstoneUpdateListener.getRedstoneActivityTable().clear();
			this.redstoneUpdateListener.stopListener();
			return true;
		}
		return false;
	}
	
	private void sortList() {
		this.redstoneActivityList.clear();
		this.redstoneActivityList.addAll(this.redstoneUpdateListener.getRedstoneActivityTable().entrySet());
		Collections.sort(this.redstoneActivityList, new Comparator<Entry<Location, Integer>>() {
			@Override
			public int compare(Entry<Location, Integer> a, Entry<Location, Integer> b) {
				if(a.getValue().intValue() < b.getValue().intValue()) {
					return 1;
				} else if(a.getValue().intValue() == b.getValue().intValue()) {
					// Values are equal, sort by coordinates instead.
					int[] diffs = new int[] {
							a.getKey().getBlockX() - b.getKey().getBlockX(),
							a.getKey().getBlockY() - b.getKey().getBlockY(),
							a.getKey().getBlockZ() - b.getKey().getBlockZ(),
							a.getKey().getWorld().getName().compareTo(b.getKey().getWorld().getName())
						};
					for(int diff : diffs) {
						if(diff != 0) {
							return (diff > 0 ? 1 : -1);
						}
					}
					return 0;
				} else {
					return -1;
				}
			}
		});
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if(!topCommand.execute(sender, args)) {
				topCommand.showUsage(sender, command.getName());
			}
		} catch (PermissionsException e) {
			sender.sendMessage(String.format(ChatColor.RED.toString() + "You do not have permission of %s", e.getPerms()));
		} catch (UsageException e) {
			sender.sendMessage("Usage: " + ChatColor.YELLOW + command.getName() + " " + e.getUsage());
			sender.sendMessage(String.format(ChatColor.RED.toString() + e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public Player getPlayer(String name) {
		Player player = null;
		for(Player p : this.getServer().getOnlinePlayers()) {
			if(p.getName().equalsIgnoreCase(name)) {
				player = p;
				if(p.getName().equals(name)) { // Only break on a case-sensitive match since names might not be unique anymore.
					break;
				}
			}
		}
		return player;
	}
}
