package co.sblock.micromodules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import co.sblock.Sblock;
import co.sblock.chat.Language;
import co.sblock.module.Module;
import co.sblock.users.Users;
import co.sblock.utilities.PermissionUtils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Module for managing players in spectator mode. Designed to allow players
 * to explore without giving any gamebreaking advantages.
 * 
 * @author Jikoo
 */
public class Spectators extends Module {

	/* The List of Players in spectator mode */
	private final Map<UUID, Location> spectators;

	/* The Cooldowns instance used to manage whether someone has recently been spectating. */
	private Cooldowns cooldowns;
	private Language lang;
	/* The SleepVote instance used to discount spectators from sleeping. */
	private SleepVote sleep;
	private Users users;

	public Spectators(Sblock plugin) {
		super(plugin);
		this.spectators = new HashMap<>();

		PermissionUtils.addParent("sblock.spectators.unrestricted", "sblock.helper");
	}

	@Override
	protected void onEnable() {
		this.cooldowns = getPlugin().getModule(Cooldowns.class);
		this.lang = getPlugin().getModule(Language.class);
		this.sleep = getPlugin().getModule(SleepVote.class);
		this.users = getPlugin().getModule(Users.class);

		new BukkitRunnable() {
			@Override
			public void run() {
				nextSpectator: for (Player player : Bukkit.getOnlinePlayers()) {
					if (!isSpectator(player.getUniqueId())) {
						continue;
					}
					if (player.hasPermission("sblock.spectators.unrestricted")) {
						continue;
					}
					if (cooldowns.getRemainder(player, getName()) == 0) {
						removeSpectator(player, false);
						player.sendMessage(lang.getValue("spectators.return.time"));
						continue;
					}
					// 100 blocks from starting location
					Location start = spectators.get(player.getUniqueId());
					if (start.getWorld().equals(player.getWorld())
							&& start.distanceSquared(player.getLocation()) < 10000) {
						continue;
					}
					// 100 blocks from any player
					for (Player nearby : player.getWorld().getPlayers()) {
						if (nearby.equals(player)) {
							continue;
						}
						Location playerLoc;
						if (isSpectator(nearby.getUniqueId())) {
							playerLoc = spectators.get(nearby.getUniqueId());
						} else {
							playerLoc = nearby.getLocation();
						}
						if (playerLoc.getWorld().equals(player.getWorld())
								&& player.getLocation().distanceSquared(playerLoc) < 10000) {
							continue nextSpectator;
						}
					}
					removeSpectator(player, false);
					player.sendMessage(lang.getValue("spectators.return.distance"));
				}
			}
		}.runTaskTimer(getPlugin(), 100, 100);
	}

	@Override
	protected void onDisable() {
		for (UUID uuid : spectators.keySet().toArray(new UUID[0])) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				this.removeSpectator(player, false);
			}
		}
	}

	/**
	 * Puts a player into spectator mode.
	 * 
	 * @param player the player to add
	 */
	public void addSpectator(Player player) {
		if (!this.isEnabled()) {
			return;
		}
		player.closeInventory();
		player.setGameMode(GameMode.SPECTATOR);
		if (sleep.updateVoteCount(player.getWorld().getName(), player.getName())) {
			player.sendMessage(lang.getValue("sleep.interrupt"));
		}
		spectators.put(player.getUniqueId(), player.getLocation().add(0, .1, 0));
		if (!player.hasPermission("sblock.spectators.unrestricted")) {
			// Allow spectating for 30 minutes at a time
			cooldowns.addCooldown(player, getName(), 1800000);
		}
	}

	/**
	 * Check to see if a player is a spectator.
	 * 
	 * @param name the name of the player
	 * 
	 * @return true if the player is a spectator
	 */
	public boolean isSpectator(UUID userID) {
		return spectators.containsKey(userID);
	}

	/**
	 * Check to see if a Player is not on spectate ore mining cooldown.
	 * 
	 * @param player the Player
	 * 
	 * @return true if the Player is allowed to mine ore
	 */
	public boolean canMineOre(Player player) {
		return cooldowns.getRemainder(player, "spectatore") <= 0;
	}

	/**
	 * Removes a Player's spectator status.
	 * 
	 * @param player the Player
	 * @param logout whether or not the player is logging out
	 */
	public void removeSpectator(Player player, boolean logout) {
		if (!this.isEnabled() || !this.isSpectator(player.getUniqueId())) {
			return;
		}
		cooldowns.clearCooldown(player, getName());
		if (!player.hasPermission("sblock.spectators.unrestricted")) {
			// 8 minutes, 8 * 60 * 1000 ms
			cooldowns.addCooldown(player, "spectatore", 480000L);
		}
		Location teleport = spectators.remove(player.getUniqueId());
		if (logout) {
			users.getUser(player.getUniqueId()).setLoginLocation(teleport);
			return;
		}
		player.teleport(teleport);
		player.setGameMode(GameMode.SURVIVAL);
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public String getName() {
		return "Spectators";
	}
}
