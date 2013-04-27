package gg.mc;

import java.io.File;
import java.util.Scanner;

import gg.mc.exceptions.ServerRunningException;
import gg.mc.heartbeat.HeartbeatThread;
import gg.mc.network.ConnectionThread;
import gg.mc.plugin.PluginManager;

public class PowerBlock {

	private static PowerBlock instance;
	
	public static PowerBlock getServer() {
		return instance;
	}
	
	public static void main(String[] args) throws ServerRunningException {
		if (instance != null) {
			throw new ServerRunningException();
		}
		File dirWorlds = new File(System.getProperty("user.dir") + File.separator + "worlds");
		File dirPlugins = new File(System.getProperty("user.dir") + File.separator + "plugins");
		if (!dirWorlds.exists()) {
			System.out.println("Creating directory /worlds");
			dirWorlds.mkdir();
		}
		if (!dirPlugins.exists()) {
			System.out.println("Creating directory /plugins");
			dirPlugins.mkdir();
		}
		instance = new PowerBlock();
		instance.startServer();
		
		Scanner s = new Scanner(System.in);
		s.useDelimiter(System.getProperty("line.separator"));
		while (s.hasNext()) {
			String[] cmdRaw = s.next().split(" ");
			String cmd = cmdRaw[0];
			String[] cmdArgs = new String[cmdRaw.length - 1];
			System.arraycopy(cmdRaw, 1, cmdArgs, 0, cmdArgs.length);
			if (cmd.equalsIgnoreCase("stop")) {
				if (cmdArgs.length == 0) {
					PowerBlock.getServer().stop();
					break;
				}
				else {
					System.out.println("Command 'stop' takes no arguments. Type 'stop' to stop the server.");
				}
			}
			PowerBlock.getServer().getPluginManager().callConsoleCommand(cmd, cmdArgs);
		}
		s.close();
	}
	
	private Thread connectionThread = new ConnectionThread();
	private Thread serverThread = new ServerThread((ConnectionThread) connectionThread);
	private Thread heartbeatThread = new HeartbeatThread((ConnectionThread) connectionThread);
	private Configuration configuration = new Configuration();
	private WorldManager worldManager;
	private PluginManager pluginManager;
	
	private void startServer() {
		connectionThread.start();
		serverThread.start();
		try {
			worldManager = new WorldManager();
		}
		catch (ServerRunningException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		if (worldManager.getTotalWorlds() == 0) {
			System.out.println("Generating default world...");
			worldManager.createWorld("world", 256, 256, 256);
		}
		pluginManager = new PluginManager();
		pluginManager.load();
		
		// Start heartbeat thread after plugin manager to call events
		heartbeatThread.start();
	}
	
	public void broadcastMessage(String message) {
		Player[] players = getOnlinePlayers();
		for (int i = 0; i < players.length; i++) {
			players[i].sendMessage(message);
		}
		System.out.println("[Chat] " + message);
	}
	
	public void stop() {
		System.out.println("Server shutting down...");
		pluginManager.unload();
		connectionThread.interrupt();
		serverThread.interrupt();
		heartbeatThread.interrupt();
		System.exit(0);
	}
	
	public Player getOnlinePlayer(String name) {
		return ((ConnectionThread) connectionThread).getPlayer(name);
	}
	
	public Player[] getOnlinePlayers() {
		return ((ConnectionThread) connectionThread).getOnlinePlayers();
	}
	
	public Configuration getConfiguration() {
		return configuration;
	}
	
	public WorldManager getWorldManager() {
		return worldManager;
	}
	
	public PluginManager getPluginManager() {
		return pluginManager;
	}
}
