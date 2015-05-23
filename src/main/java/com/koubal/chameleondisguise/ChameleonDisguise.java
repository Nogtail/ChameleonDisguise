package com.koubal.chameleondisguise;

import com.google.common.collect.Multimap;
import com.koubal.chameleon.AsyncPlayerTagEvent;
import com.koubal.chameleon.Chameleon;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChameleonDisguise extends JavaPlugin implements Listener {
	private final Map<UUID, Disguise> disguises = new ConcurrentHashMap<UUID, Disguise>(); // To store who is disguised, UUIDs as key as this will allow players to keep their disguise after rejoin

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this); // Register this as a listener
		getLogger().info("Enabled!");
	}

	@Override
	public void onDisable() {
		disguises.clear();
		getLogger().info("Disabled!");
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, String[] arguments) {
		if (!label.equalsIgnoreCase("disguise")) {
			return true;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage("This command may only be used by a player!");
			return true;
		}

		if (!sender.hasPermission("disguise.use")) {
			sender.sendMessage("You do not have permission!");
			return true;
		}

		final Player player = (Player) sender;
		UUID uuid = player.getUniqueId();

		if (arguments.length == 0) { // Remove disguise when no arguments are given
			disguises.remove(uuid);
			Chameleon.getInstance().updateViewers(player); // This will cause an AsyncPlayerTagEvent for all viewers of this player
			Chameleon.getInstance().update(player); // This will cause an AsyncPlayerTagEvent for the player as well as refreshing their skin
			player.sendMessage("Removed disguise!");
			return true;
		}

		if (arguments.length > 2) {
			sender.sendMessage("Wrong number of arguments!");
			return true;
		}

		final String tag = arguments[0];
		final String textures;

		if (arguments.length == 2) {
			textures = arguments[1];
		} else {
			textures = tag;
		}

		disguises.put(uuid, new Disguise(tag, textures));
		player.setPlayerListName(tag);

		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() { // Run task async so we can load the players skin without any lag on the main thread
			@Override
			public void run() {
				if (!Chameleon.getInstance().isCached(textures)) // This checks to see if the skin is cached, by default cached skins will expire in 1 hour
					sender.sendMessage("Skin loading!");

				Chameleon.getInstance().loadTextures(textures); // This causes the textures for a specified player to be loaded and cached

				Bukkit.getScheduler().runTask(ChameleonDisguise.this, new Runnable() {
					@Override
					public void run() {
						Chameleon.getInstance().updateViewers(player);
						Chameleon.getInstance().update(player);
						sender.sendMessage("You have disguised as " + tag + " with the skin of " + textures + "!");
					}
				});
			}
		});
		return true;
	}

	@EventHandler
	public void onPlayerTag(AsyncPlayerTagEvent event) { // Received whenever a player can receive a new tag
		UUID uuid = event.getTarget().getUniqueId();

		if (disguises.containsKey(uuid)) {
			Disguise disguise = disguises.get(uuid);
			event.setTag(disguise.getTag()); // Sets the tag of this player
			event.setTextures(disguise.getTextures()); // Sets the skin, cape and any other textures this player may have
		}
	}
}
