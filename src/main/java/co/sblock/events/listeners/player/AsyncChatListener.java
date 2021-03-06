package co.sblock.events.listeners.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import co.sblock.Sblock;
import co.sblock.chat.Chat;
import co.sblock.chat.Language;
import co.sblock.chat.ai.HalMessageHandler;
import co.sblock.chat.channel.Channel;
import co.sblock.chat.channel.RegionChannel;
import co.sblock.chat.message.Message;
import co.sblock.chat.message.MessageBuilder;
import co.sblock.discord.Discord;
import co.sblock.events.event.SblockAsyncChatEvent;
import co.sblock.events.listeners.SblockListener;
import co.sblock.micromodules.AwayFromKeyboard;
import co.sblock.micromodules.Cooldowns;
import co.sblock.users.User;
import co.sblock.users.Users;
import co.sblock.utilities.TextUtils;
import co.sblock.utilities.WrappedSenderPlayer;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;

/**
 * Listener for PlayerAsyncChatEvents.
 * 
 * @author Jikoo
 */
public class AsyncChatListener extends SblockListener {

	// TODO -> lang, events.chat.test
	private static final String[] TEST = new String[] {"It is certain.", "It is decidedly so.",
			"Without a doubt.", "Yes, definitely.", "You may rely on it.", "As I see, yes.",
			"Most likely.", "Outlook good.", "Yes.", "Signs point to yes.",
			"Reply hazy, try again.", "Ask again later.", "Better not tell you now.",
			"Cannot predict now.", "Concentrate and ask again.", "Don't count on it.",
			"My reply is no.", "My sources say no.", "Outlook not so good.", "Very doubtful.",
			"Testing complete. Proceeding with operation.", "A critical fault has been discovered while testing.",
			"Error: Test results contaminated.", "tset", "PONG."};

	private final AwayFromKeyboard afk;
	private final Chat chat;
	private final Cooldowns cooldowns;
	private final Discord discord;
	private final Language lang;
	private final Users users;
	private final List<HalMessageHandler> halFunctions;
	private final boolean handleGriefPrevention;
	private final Pattern claimPattern, trappedPattern, yoooooooooooooooooooooooooooooooooooooooo;
	private final List<Pattern> doNotSayThat;

	public AsyncChatListener(Sblock plugin) {
		super(plugin);
		this.afk = plugin.getModule(AwayFromKeyboard.class);
		this.chat = plugin.getModule(Chat.class);
		this.cooldowns = plugin.getModule(Cooldowns.class);
		this.discord = plugin.getModule(Discord.class);
		this.lang = plugin.getModule(Language.class);
		this.users = plugin.getModule(Users.class);

		halFunctions = new ArrayList<>();
		Chat chat = plugin.getModule(Chat.class);
		halFunctions.add(chat.getHalculator());
		// Hal AI function should be last as it (by design) handles any message passed to it.
		// Insert any additional functions above.
		halFunctions.add(chat.getHal());

		// If, say, the Hal AI fails to load, don't NPE whenever anyone talks.
		Iterator<HalMessageHandler> iterator = halFunctions.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == null) {
				iterator.remove();
			}
		}

		handleGriefPrevention = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention");
		if (handleGriefPrevention) {
			unregisterChatListeners();
			claimPattern = Pattern.compile("(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", Pattern.CASE_INSENSITIVE);
			trappedPattern = Pattern.compile("(^|\\s)(stuck|trapped(?! (chest|horse)))(\\W|\\s|$)", Pattern.CASE_INSENSITIVE);
		} else {
			claimPattern = null;
			trappedPattern = null;
		}

		yoooooooooooooooooooooooooooooooooooooooo = Pattern.compile("[Yy][Oo]+");

		doNotSayThat = new ArrayList<>();
		for (String pattern : lang.getValue("events.chat.filter").split("\n")) {
			doNotSayThat.add(Pattern.compile(pattern));
		}
	}

	/**
	 * Because we send JSON messages, we actually have to remove all recipients from the event and
	 * manually send each one the message.
	 * 
	 * To prevent IRC and other chat loggers from picking up chat sent to non-regional channels,
	 * non-regional chat must be cancelled.
	 * 
	 * @param event the SblockAsyncChatEvent
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
		afk.setActive(event.getPlayer());

		Message message;
		boolean checkSpam = true;
		if (event instanceof SblockAsyncChatEvent) {
			SblockAsyncChatEvent sblockEvent = (SblockAsyncChatEvent) event;
			message = sblockEvent.getSblockMessage();
			checkSpam = sblockEvent.checkSpam();
		} else {
			try {
				MessageBuilder mb = new MessageBuilder(getPlugin())
						.setSender(users.getUser(event.getPlayer().getUniqueId()))
						.setMessage(event.getMessage());
				// Ensure message can be sent
				if (!mb.canBuild(true) || !mb.isSenderInChannel(true)) {
					event.setCancelled(true);
					return;
				}
				message = mb.toMessage();

				event.getRecipients().removeIf(p -> !message.getChannel().getListening().contains(p.getUniqueId()));
			} catch (Exception e) {
				event.setCancelled(true);
				e.printStackTrace();
				return;
			}
		}

		final Player player = event.getPlayer();
		String cleaned = ChatColor.stripColor(message.getMessage());
		boolean publishGlobally = message.getChannel().getName().equals("#");

		if (checkSpam) {
			if (cleaned.equalsIgnoreCase("test")) {
				event.getPlayer().sendMessage(ChatColor.RED + test());
				event.setCancelled(true);
				return;
			}

			for (Player recipient : event.getRecipients()) {
				if (cleaned.equalsIgnoreCase(recipient.getName())) {
					this.messageLater(player, message.getChannel(),
							lang.getValue("events.chat.ping").replace("{PLAYER}",
									player.getDisplayName()));
					event.setCancelled(true);
					return;
				}
			}

			if (message.getChannel() instanceof RegionChannel && rpMatch(cleaned)) {
				this.messageLater(player, message.getChannel(), lang.getValue("events.chat.rp"));
				event.setCancelled(true);
				return;
			}
		}

		event.setFormat(message.getConsoleFormat());
		event.setMessage(cleaned);

		if (checkSpam && handleGriefPrevention) {
			if (handleGPChat(event, message)) {
				publishGlobally = false;
			}
			if (event.isCancelled()) {
				return;
			}
		}

		final User sender = message.getSender();

		// Spam detection and handling, woo!
		if (checkSpam && sender != null && !message.getChannel().getName().equals(lang.getValue("chat.spamChannel"))
				&& detectSpam(event, message)) {
			publishGlobally = false;
			event.getRecipients().clear();
			event.getRecipients().add(player);
			if (sender.getChatViolationLevel() > 8 && sender.getChatWarnStatus()) {
				messageLater(player, message.getChannel(), lang.getValue("events.chat.mute"));
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						String.format("mute %s 5m", player.getName()));
				discord.postReport("Automatically muted " + player.getName()
						+ " for spamming, violation level " + sender.getChatViolationLevel());
				event.setCancelled(true);
				return;
			}
			if (sender.getChatViolationLevel() > 3 && !sender.getChatWarnStatus()) {
				this.messageLater(player, message.getChannel(), lang.getValue("events.chat.spam"));
				sender.setChatWarnStatus(true);
			}
		}

		// Set message format in case of softmute or spam detection
		message.setConsoleFormat(event.getFormat());

		// Region channels are the only ones that should be appearing in certain plugins
		if (!(message.getChannel() instanceof RegionChannel)) {
			if (!event.isCancelled() && event instanceof SblockAsyncChatEvent) {
				((SblockAsyncChatEvent) event).setGlobalCancelled(true);
			} else {
				event.setCancelled(true);
			}
		}

		// Flag channel as having been used so it is not deleted.
		message.getChannel().updateLastAccess();

		// Don't actually display tests to anyone.
		if (message.getChannel().getName().equals("@test@")) {
			event.getRecipients().clear();
			return;
		}

		// Manually send messages to each player so we can wrap links, etc.
		message.send(event.getRecipients(), event instanceof SblockAsyncChatEvent, true);

		// Post messages to Discord
		discord.postMessage(message, publishGlobally);

		// Dummy player should not trigger Hal; he may become one.
		if (player instanceof WrappedSenderPlayer) {
			event.getRecipients().clear();
			return;
		}

		try {
			// Handle Hal functions
			for (HalMessageHandler handler : halFunctions) {
				if (handler.handleMessage(message, event.getRecipients())) {
					break;
				}
			}
		} catch (Exception e) {
			// Just in case, don't allow a Hal integration mistake to break chat.
			e.printStackTrace();
		}

		// No one should receive the final message if it is not cancelled.
		event.getRecipients().clear();
	}

	public boolean rpMatch(String message) {
		if (message.matches("([hH][oO][nN][kK] ?)+")) {
			return true;
		}
		return false;
	}

	/**
	 * Handles chat in a way similar to how GP's listener does, sans the extra checks we do not
	 * want.
	 * 
	 * @param event the AsyncPlayerChatEvent
	 * @param message the Message
	 * @return true if the sender is soft muted
	 */
	private boolean handleGPChat(final AsyncPlayerChatEvent event, final Message message) {
		if (message.getSender() == null || GriefPrevention.instance == null) {
			return false;
		}

		final DataStore dataStore = GriefPrevention.instance.dataStore;
		final Player player = event.getPlayer();
		final String world = player.getWorld().getName();

		if (!world.equals("Derspit") && !world.equals("DreamBubble")
				&& claimPattern.matcher(message.getMessage()).find()) {
			messageLater(player, message.getChannel(), lang.getValue("events.chat.gp.claims"));
		}
		if (trappedPattern.matcher(message.getMessage()).find()) {
			// Improvement over GP: Pattern ignores case and matches in substrings of words
			messageLater(player, message.getChannel(), lang.getValue("events.chat.gp.trapped"));
		}

		// Soft-muted chat
		boolean softMute = dataStore.isSoftMuted(player.getUniqueId());
		if (softMute) {
			event.setFormat("[SoftMute] " + event.getFormat());
			String soft = new StringBuilder().append(ChatColor.GRAY).append("[SoftMute] ")
					.append(ChatColor.stripColor(message.getConsoleMessage())).toString();
			Iterator<Player> iterator = event.getRecipients().iterator();
			while (iterator.hasNext()) {
				Player recipient = iterator.next();
				if (dataStore.isSoftMuted(recipient.getUniqueId())) {
					continue;
				}
				iterator.remove();
				if (recipient.hasPermission("griefprevention.eavesdrop")) {
					recipient.sendMessage(soft);
				}
			}
		}

		// Don't send messages to players ignoring sender or who the sender is ignoring
		Iterator<Player> iterator = event.getRecipients().iterator();
		PlayerData senderData = dataStore.getPlayerData(player.getUniqueId());
		boolean ignorable = !player.hasPermission("griefprevention.notignorable");
		while (iterator.hasNext()) {
			Player recipient = iterator.next();
			UUID uuid = recipient.getUniqueId();
			if (ignorable) {
				if (dataStore.getPlayerData(uuid).ignoredPlayers.containsKey(player.getUniqueId())) {
					iterator.remove();
					continue;
				}
				if (recipient.hasPermission("griefprevention.notignorable")) {
					continue;
				}
			}
			if (senderData.ignoredPlayers.containsKey(uuid)) {
				iterator.remove();
				continue;
			}
		}
		return softMute;
	}

	private boolean detectSpam(AsyncPlayerChatEvent event, Message message) {
		final Player player = event.getPlayer();
		final User sender = message.getSender();
		if (sender == null || player.hasPermission("sblock.chat.unfiltered")) {
			return false;
		}

		String msg = message.getMessage();
		String prefix = null;
		if (message.getChannel().getName().equals("#pm")) {
			// We want the space in the prefix, add 1 once rather than twice.
			int space = msg.indexOf(' ') + 1;
			if (space > 0) {
				prefix = msg.substring(0, space);
				msg = msg.substring(space);
			}
		}

		// Caps filter only belongs in regional channels.
		if (message.getChannel() instanceof RegionChannel && msg.length() > 3
				&& StringUtils.getLevenshteinDistance(msg, msg.toUpperCase()) < msg.length() * .25) {
			StringBuilder msgBuilder = new StringBuilder();
			if (prefix != null) {
				msgBuilder.append(prefix);
			}
			for (String word : msg.split(" ")) {
				if (TextUtils.URL_PATTERN.matcher(word).find()) {
					msgBuilder.append(word);
				} else {
					msgBuilder.append(word.toLowerCase());
				}
				msgBuilder.append(' ');
			}
			if (msgBuilder.length() > 0) {
				msgBuilder.deleteCharAt(msgBuilder.length() - 1);
			}
			message.setMessage(msgBuilder.toString());
		}

		msg = msg.toLowerCase();
		String lastMsg = sender.getLastChat();
		sender.setLastChat(msg);
		long lastChat = cooldowns.getRemainder(player, "chat");
		cooldowns.addCooldown(player, "chat", 30000);

		for (Pattern pattern : doNotSayThat) {
			if (pattern.matcher(msg).find()) {
				sender.setChatViolationLevel(sender.getChatViolationLevel() + 2);
				event.setFormat("[Scumbag] " + event.getFormat());
				return true;
			}
		}

		// Mute repeat messages within 30 seconds
		if (lastChat > 0 && msg.equals(lastMsg)) {
			// In event of exact duplicates, reach penalization levels at a much faster rate
			int spamCount = sender.getChatViolationLevel();
			if (spamCount == 0) {
				spamCount++;
			} else {
				spamCount *= 2;
			}
			sender.setChatViolationLevel(spamCount);
			event.setFormat("[RepeatChat] " + event.getFormat());
			return true;
		}

		// Cooldown of 1.5 seconds between messages, 3 seconds between short messages.
		if (lastChat > 28500 || msg.length() < 12 && lastChat > 27000) {
			sender.setChatViolationLevel(sender.getChatViolationLevel() + 2);
			event.setFormat("[FastChat] " + event.getFormat());
			return true;
		}

		// Sans links, messages should contain a good symbol/space to length ratio
		String[] words = msg.split(" ");
		int spaces = words.length - 1;
		int length = msg.length();
		int symbols = 0;
		boolean question = false;
		for (String word : words) {
			if (TextUtils.URL_PATTERN.matcher(word).find()) {
				length -= word.length();
				spaces--;
				continue;
			}
			for (char character : word.toCharArray()) {
				if (!Character.isLetterOrDigit(character)) {
					if (!question && character == '?') {
						// Discount one question mark from spam filtering in case of "?" or "<link> ?"
						question = true;
						length--;
						continue;
					}
					symbols++;
				}
			}
		}
		if (!yoooooooooooooooooooooooooooooooooooooooo.matcher(msg).matches()
				&& (symbols > length / 2 || length > 15 && spaces < length / 10)) {
			sender.setChatViolationLevel(sender.getChatViolationLevel() + 1);
			event.setFormat("[Gibberish] " + event.getFormat());
			return true;
		}

		// Must be more than 25% different from last message
		double lenPercent = msg.length() * .25;
		if (lastChat > 0 && StringUtils.getLevenshteinDistance(msg, lastMsg) < lenPercent) {
			sender.setChatViolationLevel(sender.getChatViolationLevel() + 2);
			event.setFormat("[SimilarChat] " + event.getFormat());
			return true;
		}

		sender.setChatViolationLevel(0);
		sender.setChatWarnStatus(false);
		return false;
	}

	private void messageLater(final Player player, final Channel channel, final String message) {
		if (player.spigot() == null) {
			return;
		}
		final UUID uuid = player.getUniqueId();
		new BukkitRunnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(uuid);
				if (player != null) {
					chat.getHalBase().setChannel(channel)
							.setMessage(Language.getColor("bot_text") + message).toMessage()
							.send(ImmutableList.of(player));
				}
			}
		}.runTaskLater(getPlugin(), 1L);
	}

	private void unregisterChatListeners() {
		for (RegisteredListener listener : AsyncPlayerChatEvent.getHandlerList().getRegisteredListeners()) {
			String plugin = listener.getPlugin().getName();
			if (plugin.equals("GriefPrevention")) {
				AsyncPlayerChatEvent.getHandlerList().unregister(listener);
				break;
			}
		}
	}

	public static String test() {
		return TEST[(int) (Math.random() * TEST.length)];
	}
}
