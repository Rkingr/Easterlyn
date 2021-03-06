package co.sblock.commands.admin;

import java.util.List;

import co.sblock.Sblock;
import co.sblock.commands.SblockCommand;
import co.sblock.users.User;
import co.sblock.users.Users;
import co.sblock.utilities.TextUtils;

import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

/**
 * SblockCommand for a dual ip and UUID ban.
 * 
 * @author Jikoo
 */
public class BanCommand extends SblockCommand {

	private final Users users;

	public BanCommand(Sblock plugin) {
		super(plugin, "ban");
		this.users = plugin.getModule(Users.class);
		this.setAliases("sban", "banip");
		this.setPermissionLevel("denizen");
	}

	@Override
	protected boolean onCommand(final CommandSender sender, final String label, final String[] args) {
		if (args == null || args.length == 0) {
			return false;
		}
		final String target = args[0];
		final StringBuilder reason = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			reason.append(ChatColor.translateAlternateColorCodes('&', args[i])).append(' ');
		}
		if (args.length == 1) {
			reason.append("Git wrekt m8.");
		} else {
			// Remove trailing space
			reason.deleteCharAt(reason.length() - 1);
		}
		if (TextUtils.IP_PATTERN.matcher(target).find()) { // IPs probably shouldn't be announced.
			Bukkit.getBanList(org.bukkit.BanList.Type.IP).addBan(target, reason.toString(), null, sender.getName());
		} else {
			Bukkit.broadcastMessage(getLang().getValue("command.ban.announce")
					.replace("{PLAYER}", target).replace("{REASON}", reason.toString()));
			banByName(sender, target, reason.toString());
		}
		return true;
	}

	private void banByName(final CommandSender sender, final String target, final String reason) {
		new BukkitRunnable() {
			@Override
			public void run() {
				@SuppressWarnings("deprecation")
				final OfflinePlayer player = Bukkit.getOfflinePlayer(target);
				new BukkitRunnable() {
					@Override
					public void run() {
						User victim = users.getUser(player.getUniqueId());
						if (victim.getUserIP().matches("([0-9]{1,3}.){3}[0-9]{1,3}")) {
							Bukkit.getBanList(Type.NAME).addBan(victim.getPlayerName(),
									"<ip=" + victim.getUserIP() + ">" + reason, null, sender.getName());
							Bukkit.getBanList(Type.IP).addBan(victim.getUserIP(),
									"<uuid=" + victim.getUUID() + ">" + reason, null, sender.getName());
						} else {
							Bukkit.getBanList(Type.NAME).addBan(victim.getPlayerName(), reason,
									null, sender.getName());
						}
						if (victim.isOnline()) {
							victim.getPlayer().kickPlayer(reason);
						}
					}
				}.runTask(getPlugin());
			}
		}.runTaskAsynchronously(getPlugin());
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		if (args.length != 1) {
			return com.google.common.collect.ImmutableList.of();
		}
		return super.tabComplete(sender, alias, args);
	}
}
