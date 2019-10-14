/*
 * Copyright (C) 2012 Vex Software LLC
 * This file is part of Votifier.
 * 
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vexsoftware.votifier;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyPair;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.net.VoteReceiver;

/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class VotifierPlus extends JavaPlugin {

	/** The Votifier instance. */
	private static VotifierPlus instance;

	/** The current Votifier version. */
	private String version;

	/** The vote receiver. */
	private VoteReceiver voteReceiver;

	/** The RSA key pair. */
	private KeyPair keyPair;

	/** Debug mode flag */
	private boolean debug;

	@Override
	public void onEnable() {
		VotifierPlus.instance = this;

		// Set the plugin version.
		version = getDescription().getVersion();

		// Handle configuration.
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File config = new File(getDataFolder() + "/config.yml");
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);
		File rsaDirectory = new File(getDataFolder() + "/rsa");
		// Replace to remove a bug with Windows paths - SmilingDevil
		String listenerDirectory = getDataFolder().toString().replace("\\", "/") + "/listeners";

		/*
		 * Use IP address from server.properties as a default for
		 * configurations. Do not use InetAddress.getLocalHost() as it most
		 * likely will return the main server address instead of the address
		 * assigned to the server.
		 */
		String hostAddr = Bukkit.getServer().getIp();
		if (hostAddr == null || hostAddr.length() == 0)
			hostAddr = "0.0.0.0";

		/*
		 * Create configuration file if it does not exists; otherwise, load it
		 */
		if (!config.exists()) {
			int openPort = 8192;
			try {
				ServerSocket s = new ServerSocket();
				s.bind(new InetSocketAddress("0.0.0.0", 0));
				openPort = s.getLocalPort();
				s.close();
			} catch (Exception e) {

			}
			try {
				// First time run - do some initialization.
				getLogger().info("Configuring Votifier for the first time...");

				// Initialize the configuration file.
				config.createNewFile();

				cfg.set("host", hostAddr);

				cfg.set("port", openPort);
				cfg.set("debug", false);

				/*
				 * Remind hosted server admins to be sure they have the right
				 * port number.
				 */
				getLogger().info("------------------------------------------------------------------------------");
				getLogger().info(
						"Assigning Votifier to listen on port " + openPort + ". If you are hosting Craftbukkit on a");
				getLogger().info("shared server please check with your hosting provider to verify that this port");
				getLogger().info("is available for your use. Chances are that your hosting provider will assign");
				getLogger().info("a different port, which you need to specify in config.yml");
				getLogger().info("------------------------------------------------------------------------------");

				cfg.save(config);
			} catch (Exception ex) {
				getLogger().severe("Error creating configuration file");
				gracefulExit();
				return;
			}
		} else {
			// Load configuration.
			cfg = YamlConfiguration.loadConfiguration(config);
		}

		/*
		 * Create RSA directory and keys if it does not exist; otherwise, read
		 * keys.
		 */
		try {
			if (!rsaDirectory.exists()) {
				rsaDirectory.mkdir();
				new File(listenerDirectory).mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				keyPair = RSAIO.load(rsaDirectory);
			}
		} catch (Exception ex) {
			getLogger().severe("Error reading configuration file or RSA keys");
			gracefulExit();
			return;
		}

		// Initialize the receiver.
		String host = cfg.getString("host", hostAddr);
		int port = cfg.getInt("port", 8192);
		debug = cfg.getBoolean("debug", false);
		if (debug)
			getLogger().info("DEBUG mode enabled!");

		try {
			voteReceiver = new VoteReceiver(this, host, port);
			voteReceiver.start();

			getLogger().info("Votifier enabled.");
		} catch (Exception ex) {
			gracefulExit();
			return;
		}
	}

	@Override
	public void onDisable() {
		// Interrupt the vote receiver.
		if (voteReceiver != null) {
			voteReceiver.shutdown();
		}
		getLogger().info("Votifier disabled.");
	}

	private void gracefulExit() {
		getLogger().severe("Votifier did not initialize properly!");
	}

	/**
	 * Gets the instance.
	 * 
	 * @return The instance
	 */
	public static VotifierPlus getInstance() {
		return instance;
	}

	/**
	 * Gets the version.
	 * 
	 * @return The version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Gets the vote receiver.
	 * 
	 * @return The vote receiver
	 */
	public VoteReceiver getVoteReceiver() {
		return voteReceiver;
	}

	/**
	 * Gets the keyPair.
	 * 
	 * @return The keyPair
	 */
	public KeyPair getKeyPair() {
		return keyPair;
	}

	public boolean isDebug() {
		return debug;
	}

}