package com.easterlyn.commands.admin;

import com.easterlyn.Easterlyn;
import com.easterlyn.chat.Color;
import com.easterlyn.commands.EasterlynCommand;
import com.easterlyn.users.UserRank;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * EasterlynCommand for being Lord English.
 * 
 * @author Jikoo
 */
public class LordEnglishCommand extends EasterlynCommand {

	public LordEnglishCommand(Easterlyn plugin) {
		super(plugin, "le");
		this.setDescription("&4He's already here!");
		this.setPermissionLevel(UserRank.HORRORTERROR);
		this.setPermissionMessage("&0Le no. Le /le is reserved for le fancy people.");
		this.setUsage("/le <text>");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		StringBuilder msg = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			msg.append(args[i].toUpperCase()).append(' ');
		}
		StringBuilder leOut = new StringBuilder();
		for (int i = 0; i < msg.length();) {
			for (int j = 0; j < Color.RAINBOW.length; j++) {
				if (i >= msg.length())
					break;
				leOut.append(Color.RAINBOW[j]).append(msg.charAt(i));
				i++;
			}
		}
		Bukkit.broadcastMessage(leOut.substring(0, leOut.length() - 1 > 0 ? leOut.length() - 1 : 0));
		return true;
	}
}
