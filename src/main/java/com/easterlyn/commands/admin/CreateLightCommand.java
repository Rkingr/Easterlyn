package com.easterlyn.commands.admin;

import java.util.List;
import java.util.Set;

import com.easterlyn.Easterlyn;
import com.easterlyn.commands.SblockCommand;
import com.easterlyn.users.UserRank;
import com.easterlyn.utilities.LightSource;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Basic SblockCommand for creating a light source.
 * 
 * @author Jikoo
 */
public class CreateLightCommand extends SblockCommand {

	public CreateLightCommand(Easterlyn plugin) {
		super(plugin, "createlight");
		this.setAliases("lettherebelight", "fakelight");
		this.setDescription("Create a fake light source at the block on your cursor.");
		this.setPermissionLevel(UserRank.DENIZEN);
		this.setUsage("Run /createlight while pointing at a block under 10 blocks away");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(getLang().getValue("command.general.noConsole"));
			return true;
		}

		Player player = (Player) sender;
		List<Block> blocks = player.getLineOfSight((Set<Material>) null, 10);
		if (blocks.isEmpty()) {
			return false;
		}

		LightSource.createLightSource(blocks.get(0).getLocation(), 15);
		player.sendMessage("Light created!");
		return true;
	}

}