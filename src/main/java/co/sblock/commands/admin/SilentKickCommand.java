package co.sblock.commands.admin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import co.sblock.Sblock;
import co.sblock.commands.SblockCommand;

/**
 * SblockCommand for silently kicking a player.
 * 
 * @author Jikoo
 */
public class SilentKickCommand extends SblockCommand {

	public SilentKickCommand(Sblock plugin) {
		super(plugin, "silentkick");
		this.setPermissionLevel("denizen");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (args.length == 0) {
			return false;
		}
		List<Player> players = Bukkit.matchPlayer(args[0]);
		if (players.isEmpty()) {
			return false;
		}
		players.get(0).kickPlayer(StringUtils.join(args, ' ', 1, args.length));
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		if (!sender.hasPermission(this.getPermission()) || args.length != 1) {
			return com.google.common.collect.ImmutableList.of();
		}
		return super.tabComplete(sender, alias, args);
	}
}
