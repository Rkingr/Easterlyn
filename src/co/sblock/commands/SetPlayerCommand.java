package co.sblock.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import co.sblock.users.ProgressionState;
import co.sblock.users.User;
import co.sblock.users.UserAspect;
import co.sblock.users.UserClass;
import co.sblock.users.UserManager;

import com.google.common.collect.ImmutableList;

/**
 * SblockCommand for setting User data.
 * 
 * @author Jikoo
 */
public class SetPlayerCommand extends SblockCommand {

	private final String[] primaryArgs;

	public SetPlayerCommand() {
		super("setplayer");
		this.setDescription("Set player data manually.");
		this.setUsage("/setplayer <playername> <class|aspect|land|dream|prevloc|progression> <value>");
		this.setPermission("group.horrorterror");
		primaryArgs = new String[] {"class", "aspect", "land", "dream", "prevloc", "progression"};
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (args == null || args.length < 3) {
			return false;
		}
		User user = UserManager.getUser(Bukkit.getPlayer(args[0]).getUniqueId());
		args[1] = args[1].toLowerCase();
		if(args[1].equals("class"))
			user.setPlayerClass(args[2]);
		else if(args[1].equals("aspect"))
			user.setAspect(args[2]);
		else if(args[1].replaceAll("m(edium_?)?planet", "land").equals("land"))
			user.setMediumPlanet(args[2]);
		else if(args[1].replaceAll("d(ream_?)?planet", "dream").equals("dream"))
			user.setDreamPlanet(args[2]);
		else if(args[1].equals("progression"))
			user.setProgression(ProgressionState.valueOf(args[2].toUpperCase()));
		else if (args[1].equals("prevloc")) {
			user.setPreviousLocation(user.getPlayer().getLocation());
		} else
			return false;
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		if (args.length < 2) {
			return super.tabComplete(sender, alias, args);
		}
		ArrayList<String> matches = new ArrayList<>();
		args[1] = args[1].toLowerCase();
		if (args.length == 2) {
			for (String argument : primaryArgs) {
				if (argument.startsWith(args[1])) {
					matches.add(args[1]);
				}
			}
			return matches;
		}
		if (args[1].equals("class") && args.length == 3) {
			args[2] = args[2].toUpperCase();
			for (UserClass userclass : UserClass.values()) {
				if (userclass.name().startsWith(args[2])) {
					matches.add(userclass.name());
				}
			}
			return matches;
		}
		if (args[1].equals("aspect") && args.length == 3) {
			args[2] = args[2].toUpperCase();
			for (UserAspect aspect : UserAspect.values()) {
				if (aspect.name().startsWith(args[2])) {
					matches.add(aspect.name());
				}
			}
			return matches;
		}
		if (args[1].equals("land") && args.length == 3) {
			args[2] = args[2].toUpperCase();
			for (String land : new String[]{"LOFAF", "LOHAC", "LOLAR", "LOWAS"}) {
				if (land.startsWith(args[2])) {
					matches.add(land);
				}
			}
			return matches;
		}
		if (args[1].equals("dream") && args.length == 3) {
			args[2] = args[2].toUpperCase();
			for (String dream : new String[]{"PROSPIT", "DERSE"}) {
				if (dream.startsWith(args[2])) {
					matches.add(dream);
				}
			}
			return matches;
		}
		if (args[1].equals("progression") && args.length == 3) {
			args[2] = args[2].toUpperCase();
			for (ProgressionState state : ProgressionState.values()) {
				if (state.name().startsWith(args[2])) {
					matches.add(state.name());
				}
			}
			return matches;
		}
		return ImmutableList.of();
	}
}