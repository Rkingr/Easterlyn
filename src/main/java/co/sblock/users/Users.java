package co.sblock.users;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import co.sblock.Sblock;
import co.sblock.chat.Color;
import co.sblock.chat.Language;
import co.sblock.module.Module;
import co.sblock.utilities.CollectionConversions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Class that keeps track of players currently logged on to the game.
 * 
 * @author FireNG, Jikoo
 */
public class Users extends Module {

	/* The Cache of Player UUID and relevant Users. */
	private final LoadingCache<UUID, User> userCache;

	public Users(Sblock plugin) {
		super(plugin);
		this.userCache = CacheBuilder.newBuilder()
				.expireAfterAccess(30L, TimeUnit.MINUTES)
				.removalListener(new RemovalListener<UUID, User>() {
					@Override
					public void onRemoval(RemovalNotification<UUID, User> notification) {
						User user = notification.getValue();
						user.save();
						unteam(user.getPlayerName());
					}
				}).build(new CacheLoader<UUID, User>() {
					@Override
					public User load(UUID uuid) {
						User user = User.load(getPlugin(), uuid);
						team(user.getPlayer(), null);
						return user;
					}
				});
	}

	@Override
	protected void onEnable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			getUser(player.getUniqueId());
			team(player, null);
		}
	}

	@Override
	protected void onDisable() {
		// Invalidating and cleaning up causes our removal listener to save all cached users.
		userCache.invalidateAll();
		userCache.cleanUp();
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@Override
	public String getName() {
		return "Users";
	}

	/**
	 * Fetch a User. A User is always returned, even if the Player by the given UUID is not online.
	 * 
	 * @param uuid
	 * 
	 * @return the User
	 */
	public User getUser(UUID uuid) {
		return userCache.getUnchecked(uuid);
	}

	public Set<User> getOnlineUsers() {
		return CollectionConversions.toSet(Bukkit.getOnlinePlayers(), player -> getUser(player.getUniqueId()));
	}

	/**
	 * Add a Player to a Team colored based on permissions.
	 * 
	 * @param player the Player
	 */
	public static void team(Player player, String prefix) {
		if (player == null) {
			return;
		}
		StringBuilder prefixBuilder = new StringBuilder();
		if (prefix != null && !prefix.isEmpty()) {
			prefixBuilder.append(prefix);
		}
		for (net.md_5.bungee.api.ChatColor color : Color.COLORS) {
			if (player.hasPermission("sblock.chat.color." + color.name().toLowerCase())) {
				prefixBuilder.append(color);
				break;
			}
		}
		if (prefixBuilder.length() > (prefix == null ? 0 : prefix.length())) {
			// Do nothing, we've got a fancy override going on
		} else if (player.hasPermission("sblock.chat.color.horrorterror")) {
			prefixBuilder.append(Language.getColor("rank.horrorterror"));
		} else if (player.hasPermission("sblock.chat.color.denizen")) {
			prefixBuilder.append(Language.getColor("rank.denizen"));
		} else if (player.hasPermission("sblock.chat.color.felt")) {
			prefixBuilder.append(Language.getColor("rank.felt"));
		} else if (player.hasPermission("sblock.chat.color.helper")) {
			prefixBuilder.append(Language.getColor("rank.helper"));
		} else if (player.hasPermission("sblock.chat.color.donator")) {
			prefixBuilder.append(Language.getColor("rank.donator"));
		} else if (player.hasPermission("sblock.chat.color.godtier")) {
			prefixBuilder.append(Language.getColor("rank.godtier"));
		} else {
			prefixBuilder.append(Language.getColor("rank.hero"));
		}
		for (net.md_5.bungee.api.ChatColor color : Color.FORMATS) {
			if (player.hasPermission("sblock.chat.color." + color.name().toLowerCase())) {
				prefixBuilder.append(color);
			}
		}
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		String teamName = player.getName();
		Team team = board.getTeam(teamName);
		if (team == null) {
			team = board.registerNewTeam(teamName);
		}
		prefix = prefixBuilder.length() <= 16 ? prefixBuilder.toString()
				: prefixBuilder.substring(prefixBuilder.length() - 16, prefixBuilder.length());
		team.setPrefix(prefix);
		team.addEntry(player.getName());
		team.addEntry(player.getPlayerListName());

		Objective objective = board.getObjective("deaths");
		if (objective == null) {
			objective = board.registerNewObjective("deaths", "deathCount");
			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}

		// Since Mojang doesn't, we'll force deathcount to persist - it's been a feature for ages
		Score score = objective.getScore(player.getName());
		score.setScore(player.getStatistic(Statistic.DEATHS));
	}

	public static void unteam(Player player) {
		unteam(player.getName());
	}

	private static void unteam(String teamName) {
		if (teamName == null) {
			return;
		}
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
		if (team != null) {
			team.unregister();
		}
	}

	public static Location getSpawnLocation() {
		return new Location(Bukkit.getWorld("Earth"), -3.5, 20, 6.5, 179.99F, 1F);
	}
}
