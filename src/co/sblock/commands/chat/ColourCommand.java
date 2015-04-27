package co.sblock.commands.chat;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.google.common.collect.ImmutableList;

import co.sblock.chat.ColorDef;
import co.sblock.commands.SblockCommand;

/**
 * SblockCommand for /color
 * 
 * @author Jikoo
 */
public class ColourCommand extends SblockCommand {

	public ColourCommand() {
		super("colour");
		this.setAliases("color");
		this.setDescription("List all colours.");
		this.setUsage("&c/colour");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		sender.sendMessage(ColorDef.listColors());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return ImmutableList.of();
	}
}