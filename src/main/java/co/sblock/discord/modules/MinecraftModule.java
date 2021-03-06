package co.sblock.discord.modules;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.sblock.chat.ChannelManager;
import co.sblock.chat.Chat;
import co.sblock.chat.Language;
import co.sblock.chat.message.MessageBuilder;
import co.sblock.discord.Discord;
import co.sblock.discord.DiscordPlayer;
import co.sblock.discord.abstraction.CallPriority;
import co.sblock.discord.abstraction.DiscordModule;
import co.sblock.events.event.SblockAsyncChatEvent;
import co.sblock.users.Users;
import co.sblock.utilities.PermissionUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.TextComponent;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

/**
 * DiscordModule for interacting with the Minecraft server from Discord.
 * 
 * @author Jikoo
 */
public class MinecraftModule extends DiscordModule {

	private final Users users;
	private final ChannelManager manager;
	private final MessageBuilder builder;
	private final Pattern mention = Pattern.compile("<([@#])(.+?)>");

	public MinecraftModule(Discord discord) {
		super(discord);

		this.users = discord.getPlugin().getModule(Users.class);
		this.manager = discord.getPlugin().getModule(Chat.class).getChannelManager();

		// Permission to bypass Discord message filtering (can truly be horrific)
		PermissionUtils.addParent("sblock.discord.unfiltered", "sblock.horrorterror");

		// future modify MessageBuilder to allow custom name clicks (OPEN_URL www.sblock.co/discord)
		this.builder = new MessageBuilder(discord.getPlugin()).setNameClick("@# ")
				.setChannelClick("@# ").setChannel(this.manager.getChannel("#discord"))
				.setNameHover(TextComponent.fromLegacyText(discord.getPlugin()
						.getModule(Language.class).getValue("chat.user.discordHover")));
	}

	@Override
	public void doSetup() { }

	@Override
	public void doHeartbeat() { }

	public void handleCommand(DiscordPlayer player, String command, IChannel channel) {
		if (player.hasPendingCommand()) {
			getDiscord().postMessage(getDiscord().getBotName(), "You already have a pending command. Please be patient.", channel.getID());
			return;
		}
		Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(getDiscord().getPlugin(),
				new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						player.startMessages();
						PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, command);
						Bukkit.getPluginManager().callEvent(event);
						getDiscord().getLogger().info(event.getPlayer().getName() + " issued server command: " + event.getMessage());
						return !event.isCancelled() && Bukkit.dispatchCommand(player, event.getMessage().substring(1));
					}
				});

		new BukkitRunnable() {
			@Override
			public void run() {
				int count = 0;
				while (!future.isDone() && count < 20) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						future.cancel(false);
						break;
					}
					count++;
				}
				if (future.isCancelled() || !future.isDone()) {
					getDiscord().postMessage(getDiscord().getBotName(), "Command " + command + " from " + player.getName() + " timed out.", channel.getID());
					player.stopMessages();
					return;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) { }
				String message = player.stopMessages();
				if (message.isEmpty()) {
					return;
				}
				getDiscord().postMessage(getDiscord().getBotName(), message, channel.getID());
			}
		}.runTaskAsynchronously(getDiscord().getPlugin());
	}

	public void handleChat(IMessage message, Player player) {
		String content = message.getContent();
		if (!player.hasPermission("sblock.discord.unfiltered")) {
			int newline = content.indexOf('\n');
			boolean delete = false;
			if (newline > 0) {
				getDiscord().postMessage(getDiscord().getBotName(), "Newlines are not allowed in messages to Minecraft, "
						+ message.getAuthor().mention(), message.getChannel().getID());
				delete = true;
			} else if (content.length() > 255) {
				getDiscord().postMessage(getDiscord().getBotName(), "Messages from Discord may not be over 255 characters, "
						+ message.getAuthor().mention(), message.getChannel().getID());
				delete = true;
			}
			if (delete) {
				if (!message.getChannel().isPrivate()) {
					getDiscord().queueMessageDeletion(CallPriority.LOW, message);
				}
				return;
			}
		}
		builder.setSender(users.getUser(player.getUniqueId()))
				.setMessage(sanitizeForMinecraft(content)).setChannel(manager.getChannel("#discord"));
		if (!builder.canBuild(false)) {
			if (!message.getChannel().isPrivate()) {
				getDiscord().queueMessageDeletion(CallPriority.LOW, message);
			}
			return;
		}
		Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
		players.removeIf(p -> users.getUser(p.getUniqueId()).getSuppression());
		Bukkit.getPluginManager().callEvent(new SblockAsyncChatEvent(true, player, players, builder.toMessage()));
	}

	private String sanitizeForMinecraft(String message) {
		Matcher matcher = mention.matcher(message);
		StringBuilder sb = new StringBuilder();
		int lastMatch = 0;
		while (matcher.find()) {
			sb.append(message.substring(lastMatch, matcher.start())).append(matcher.group(1));
			String id = matcher.group(2);
			IUser user = getDiscord().getClient().getUserByID(id);
			if (user == null) {
				IChannel channel = getDiscord().getClient().getChannelByID(id);
				if (channel == null) {
					sb.append(id);
				} else {
					sb.append(channel.getName());
				}
			} else {
				sb.append(user.getName());
			}
			lastMatch = matcher.end();
		}
		sb.append(message.substring(lastMatch));
		return sb.toString();
	}

}
