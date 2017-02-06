package com.easterlyn.commands.chat;

import java.util.HashSet;
import java.util.Set;

import com.easterlyn.Easterlyn;
import com.easterlyn.chat.Chat;
import com.easterlyn.chat.channel.Channel;
import com.easterlyn.chat.message.Message;
import com.easterlyn.chat.message.MessageBuilder;
import com.easterlyn.commands.EasterlynAsynchronousCommand;
import com.easterlyn.discord.Discord;
import com.easterlyn.events.event.EasterlynAsyncChatEvent;
import com.easterlyn.users.UserRank;
import com.easterlyn.users.Users;
import com.easterlyn.utilities.WrappedSenderPlayer;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * EasterlynCommand for /aether, the command executed to make IRC chat mimic normal channels.
 * 
 * @author Jikoo
 */
public class AetherCommand extends EasterlynAsynchronousCommand {

	private final Discord discord;
	private final Users users;
	private final BaseComponent[] hover;
	private final Channel aether;

	public AetherCommand(Easterlyn plugin) {
		super(plugin, "aether");
		this.setAliases("aetherme");
		this.setDescription("For usage in console largely. Talks in #Aether.");
		this.setPermissionLevel(UserRank.HORRORTERROR);
		this.setPermissionMessage("The aetherial realm eludes your grasp once more.");
		this.setUsage("/aether <text>");
		this.discord = plugin.getModule(Discord.class);
		this.users = plugin.getModule(Users.class);

		hover = TextComponent.fromLegacyText(getLang().getValue("command.aether.hover"));
		aether = plugin.getModule(Chat.class).getChannelManager().getChannel("#Aether");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (args.length < 2) {
			sender.sendMessage("Hey Adam, stop faking empty IRC messages.");
			return true;
		}

		sendAether(sender, args[0], StringUtils.join(args, ' ', 1, args.length), label.equals("aetherme"));
		return true;
	}

	public void sendAether(CommandSender sender, String name, String msg, boolean thirdPerson) {

		// set channel before and after to prevent @channel changing while also stripping invalid characters
		MessageBuilder builder = new MessageBuilder((Easterlyn) getPlugin())
				.setSender(ChatColor.WHITE + name).setChannel(aether).setMessage(msg)
				.setChannel(aether).setChannelClick("@# ").setNameClick("@# ").setNameHover(hover)
				.setThirdPerson(thirdPerson);

		if (!builder.canBuild(false)) {
			return;
		}

		Message message = builder.toMessage();

		Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
		players.removeIf(p -> users.getUser(p.getUniqueId()).getSuppression());

		WrappedSenderPlayer senderPlayer = new WrappedSenderPlayer((Easterlyn) getPlugin(),
				sender == null ? Bukkit.getConsoleSender() : sender, name);

		EasterlynAsyncChatEvent event = new EasterlynAsyncChatEvent(true, senderPlayer, players, message);

		if (Bukkit.isPrimaryThread()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled() || event.isGlobalCancelled()) {
						discord.postMessage(senderPlayer.getDisplayName(), message.getDiscordMessage(), discord.getMainChannel());
					}
				}
			}.runTaskAsynchronously(getPlugin());
		} else {
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled() || event.isGlobalCancelled()) {
				discord.postMessage(senderPlayer.getDisplayName(), message.getDiscordMessage(), discord.getMainChannel());
			}
		}
	}

}
