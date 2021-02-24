package gg.mc;

import gg.mc.exceptions.NoAvailableEIDException;
import gg.mc.network.packets.Packet;
import gg.mc.network.packets.Packet12DespawnPlayer;
import gg.mc.network.packets.Packet6SetBlock;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class World {
	private String name;
	private short length;
	private short depth;
	private short height;
	private byte[] data;
	private boolean[] availableEids = new boolean[256];
	private Position spawn;

	public World(String name, short length, short depth, short height) {
		//players become invisible avec 1024
		if (length < 32) {
			length = 32;
		}
		if (depth < 32) {
			depth = 32;
		}
		if (height < 32) {
			height = 32;
		}
		this.name = name;
		this.length = length;
		this.depth = depth;
		this.height = height;
		data = new byte[length * depth * height];
		Arrays.fill(data, (byte) 0);
		for (int i = 0; i < (length * depth); i++) {
			data[i] = Block.Grass;
		}
		Arrays.fill(availableEids, true);
		spawn = new Position((length / 2) * 32, ((height / 2) + 3) * 32, (depth / 2) * 32, (byte) 0, (byte) 0);
	}

	public World(String name) {
		this.name = name;
		Arrays.fill(availableEids, true);
		load();
	}

	public static boolean exists(String name) {
		return new File(PowerBlock.WORLDS_DIR + name + ".world").exists();
	}

	public void load() {
		File file = new File(PowerBlock.WORLDS_DIR + name + ".world");
		try {
			GZIPInputStream gis = new GZIPInputStream(new FileInputStream(file));
			DataInputStream dis = new DataInputStream(gis);
			length = dis.readShort();
			depth = dis.readShort();
			height = dis.readShort();
			short x = dis.readShort();
			short y = dis.readShort();
			short z = dis.readShort();
			byte yaw = dis.readByte();
			byte pitch = dis.readByte();
			spawn = new Position(x, y, z, yaw, pitch);
			data = new byte[length * depth * height];
			dis.read(data);
			dis.close();
			gis.close();
		} catch (Exception ex) {
			Logger.getGlobal().warning("Failed to load map '" + name + "'!");
		}
	}

	public void save() {
		File file = new File(PowerBlock.WORLDS_DIR + name + ".world");
		try {
			GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(file));
			DataOutputStream dos = new DataOutputStream(gos);
			dos.writeShort(length);
			dos.writeShort(depth);
			dos.writeShort(height);
			dos.writeShort(spawn.getX());
			dos.writeShort(spawn.getY());
			dos.writeShort(spawn.getZ());
			dos.writeByte(spawn.getYaw());
			dos.writeByte(spawn.getPitch());
			dos.write(data);
			dos.flush();
			dos.close();
			gos.flush();
			gos.close();
		} catch (Exception ex) {
			Logger.getGlobal().warning("Failed to save map '" + name + "'!");
			ex.printStackTrace();
		}
	}

	public void broadcastWorldPacket(Packet packet) {
		broadcastWorldPacket(packet, null);
	}

	public void broadcastWorldPacket(Packet packet, Player exclude) {
		Player[] players = PowerBlock.getServer().getPlayers();
		for (int i = 0; i < players.length; i++) {
			if (players[i] != exclude && this.equals(players[i].getWorld())) {
				players[i].push(packet);
			}
		}
	}

	private int getDataPosition(short x, short y, short z) {
		return y * (length * depth) + (z * length) + x;
	}

	public byte requestEntityId() throws NoAvailableEIDException {
		for (int i = 0; i < availableEids.length; i++) {
			if (availableEids[i]) {
				availableEids[i] = false;
				return (byte) i;
			}
		}
		throw new NoAvailableEIDException(this);
	}

	public void reclaimEid(byte id) {
		broadcastWorldPacket(new Packet12DespawnPlayer(id));
		availableEids[id] = true;
	}

	public byte getBlockAt(short x, short y, short z) {
		return data[getDataPosition(x, y, z)];
	}

	public void setBlockAt(int x, int y, int z, byte block) {
		setBlockAt((short) x, (short) y, (short) z, block);
	}

	public void setBlockAt(short x, short y, short z, byte block) {
		data[getDataPosition(x, y, z)] = block;
		Packet6SetBlock update = new Packet6SetBlock(x, y, z, block);
		broadcastWorldPacket(update);
	}

	public byte getBlockAt(Position p) {
		return getBlockAt(p.getX(), p.getY(), p.getZ());
	}

	public void setBlockAt(Position p, byte block) {
		setBlockAt(p.getX(), p.getY(), p.getZ(), block);
	}

	public String getName() {
		return name;
	}

	public short getLength() {
		return length;
	}

	public short getDepth() {
		return depth;
	}

	public short getHeight() {
		return height;
	}

	public byte[] getWorldData() {
		return data;
	}

	public Position getSpawn() {
		return spawn;
	}

	public void setSpawn(Position p) {
		spawn = p;
	}
}
