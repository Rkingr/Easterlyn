package co.sblock.commands.info;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;

import co.sblock.Sblock;
import co.sblock.chat.Chat;
import co.sblock.commands.SblockCommand;

/**
 * SblockCommand for using the halculate function just for yourself.
 * 
 * @author Jikoo
 */
public class HalculatorCommand extends SblockCommand {

	private final Chat chat;

	public HalculatorCommand(Sblock plugin) {
		super(plugin, "halculate");
		this.setAliases("halc", "evhal", "evhaluate");
		this.chat = plugin.getModule(Chat.class);
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (args.length == 0) {
			return false;
		} else {
			UUID uuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
			sender.sendMessage(getLang().getValue("command.halculate.success")
					.replace("{VALUE}", chat.getHalculator().evhaluate(uuid, StringUtils.join(args, ' '))));
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return ImmutableList.of();
	}
}
