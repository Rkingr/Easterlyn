package co.sblock.discord;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import co.sblock.Sblock;
import co.sblock.chat.Language;
import co.sblock.chat.message.Message;
import co.sblock.discord.abstraction.CallPriority;
import co.sblock.discord.abstraction.DiscordCallable;
import co.sblock.discord.abstraction.DiscordCommand;
import co.sblock.discord.abstraction.DiscordModule;
import co.sblock.module.Module;
import co.sblock.utilities.PermissiblePlayer;
import co.sblock.utilities.PlayerLoader;
import co.sblock.utilities.TextUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import org.reflections.Reflections;

import net.md_5.bungee.api.ChatColor;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * A Module for managing messaging to and from Discord.
 * 
 * @author Jikoo
 */
public class Discord extends Module {

	private final String chars = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private final Pattern toEscape = Pattern.compile("([\\_~*])"),
			spaceword = Pattern.compile("(\\s*)(\\S*)");
	private final Map<Class<? extends DiscordModule>, DiscordModule> modules;
	private final Map<String, DiscordCommand> commands;
	private final LoadingCache<Object, Object> authentications;
	private final YamlConfiguration discordData; // TODO access is not synchronized, dangerous

	private Language lang;
	private String botName, token, channelGeneral, channelMain, channelLog, channelReports;
	private IDiscordClient client;
	private BukkitTask heartbeatTask;
	private QueueDrainThread drainQueueThread;
	private boolean ready = false;

	private boolean lock = false;

	public Discord(Sblock plugin) {
		super(plugin);

		modules = new HashMap<>();
		commands = new HashMap<>();

		authentications = CacheBuilder.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).build(
				new CacheLoader<Object, Object>() {
					@Override
					public Object load(Object key) throws Exception {
						if (!(key instanceof UUID)) {
							throw new IllegalArgumentException("Key must be a UUID");
						}
						String code = generateUniqueCode();
						authentications.put(code, key);
						return code;
					};
				});

		File file = new File(plugin.getDataFolder(), "DiscordData.yml");
		if (file.exists()) {
			discordData = YamlConfiguration.loadConfiguration(file);
		} else {
			discordData = new YamlConfiguration();
		}
	}

	@SuppressWarnings("rawtypes") // I don't like doing this, but type erasure forces my hand.
	@Override
	protected void onEnable() {
		this.lang = getPlugin().getModule(Language.class);
		botName = getConfig().getString("discord.username", "Sbot");
		token = getConfig().getString("discord.token");
		channelGeneral = getConfig().getString("discord.chat.general");
		channelMain = getConfig().getString("discord.chat.main");
		channelLog = getConfig().getString("discord.chat.log");
		channelReports = getConfig().getString("discord.chat.reports");

		if (token == null) {
			getLogger().severe("Unable to connect to Discord, no connection details provided!");
			this.disable();
			return;
		}

		try {
			this.client = new ClientBuilder().withToken(token).build();
		} catch (DiscordException e) {
			e.printStackTrace();
			this.disable();
			return;
		}

		Reflections reflections = new Reflections("co.sblock.discord.listeners");
		Set<Class<? extends IListener>> listenerClazzes = reflections.getSubTypesOf(IListener.class);
		EventDispatcher dispatcher = this.client.getDispatcher();
		for (Class<? extends IListener> clazz : listenerClazzes) {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!Sblock.areDependenciesPresent(clazz)) {
				getLogger().warning(clazz.getSimpleName() + " is missing dependencies, skipping.");
				continue;
			}
			try {
				Constructor<? extends IListener> constructor = clazz.getConstructor(getClass());
				IListener listener = constructor.newInstance(this);
				dispatcher.registerListener(listener);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					client.login();
				} catch (DiscordException e) {
					client = null;
				} catch (RateLimitException e) {
					try {
						Thread.sleep(e.getRetryDelay() + 1000L);
						client.login();
					} catch (InterruptedException | DiscordException | RateLimitException e1) {
						// Retry failed, ignore
					}
				}
			}
		}, "Sblock-DiscordLogin").start();

		// Only load modules and whatnot once, no matter how many times we re-enable
		if (lock) {
			return;
		}

		lock = true;

		reflections = new Reflections("co.sblock.discord.modules");
		Set<Class<? extends DiscordModule>> moduleClazzes = reflections.getSubTypesOf(DiscordModule.class);
		for (Class<? extends DiscordModule> clazz : moduleClazzes) {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!Sblock.areDependenciesPresent(clazz)) {
				getLogger().warning(clazz.getSimpleName() + " is missing dependencies, skipping.");
				continue;
			}
			try {
				Constructor<? extends DiscordModule> constructor = clazz.getConstructor(getClass());
				DiscordModule module = constructor.newInstance(this);
				modules.put(clazz, module);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		reflections = new Reflections("co.sblock.discord.commands");
		Set<Class<? extends DiscordCommand>> commandClazzes = reflections.getSubTypesOf(DiscordCommand.class);
		for (Class<? extends DiscordCommand> clazz : commandClazzes) {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!Sblock.areDependenciesPresent(clazz)) {
				getLogger().warning(clazz.getSimpleName() + " is missing dependencies, skipping.");
				continue;
			}
			try {
				Constructor<? extends DiscordCommand> constructor = clazz.getConstructor(getClass());
				DiscordCommand command = constructor.newInstance(this);
				commands.put('/' + command.getName(), command);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDisable() {
		this.ready = false;
		try {
			discordData.save(new File(getPlugin().getDataFolder(), "DiscordData.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (client != null) {
			/*
			 * Discord4J calls Thread#interrupt when finishing DiscordClientImpl#logout. Spawning
			 * new threads when shutting down is a bad idea, but we can't do much else. In case
			 * we're not stopping and are instead reloading the Discord module, we need to keep a
			 * reference to the current client to be able to log it out.
			 */
			final IDiscordClient oldClient = client;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						oldClient.logout();
					} catch (DiscordException e) {
						e.printStackTrace();
					}
				}
			}, "Sblock-DiscordLogout").start();
		}
	}

	@Override
	public YamlConfiguration loadConfig() {
		super.loadConfig();

		botName = getConfig().getString("discord.username", "Sbot");
		token = getConfig().getString("discord.token");
		channelGeneral = getConfig().getString("discord.chat.general");
		channelMain = getConfig().getString("discord.chat.main");
		channelLog = getConfig().getString("discord.chat.log");
		channelReports = getConfig().getString("discord.chat.reports");

		return getConfig();
	}

	public String getBotName() {
		return botName;
	}

	private String generateUniqueCode() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
		}
		String code = sb.toString();
		if (authentications.getIfPresent(code) != null) {
			return generateUniqueCode();
		}
		return code;
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && this.client != null;
	}

	public void setReady(boolean ready) {
		if (ready) {
			for (DiscordModule module : modules.values()) {
				module.doSetup();
			}
			startQueueDrain();
			startHeartbeat();
		}
		this.ready = ready;
	}

	public boolean isReady() {
		return this.ready;
	}

	public YamlConfiguration getDatastore() {
		return discordData;
	}

	private void startHeartbeat() {
		if (heartbeatTask != null && heartbeatTask.getTaskId() != -1) {
			return;
		}
		heartbeatTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!isEnabled()) {
					cancel();
					return;
				}
				// In case the queue has encountered an error, attempt to restart
				startQueueDrain();

				for (DiscordModule module : modules.values()) {
					try {
						module.doHeartbeat();
					} catch (Exception e) {
						// Catch any exception so everything else doesn't break
						e.printStackTrace();
					}
				}
			}
		}.runTaskTimerAsynchronously(getPlugin(), 100L, 6000L); // 5 minutes between checks
	}

	private void startQueueDrain() {
		if (drainQueueThread == null || !drainQueueThread.isAlive()) {
			drainQueueThread = new QueueDrainThread(this, 350, "Sblock-DiscordQueue");
			drainQueueThread.start();
		}
	}

	public void queue(DiscordCallable call) {
		startQueueDrain();
		drainQueueThread.queue(call);
	}

	public void queueMessageDeletion(CallPriority priority, IMessage... messages) {
		if (messages.length == 0) {
			return;
		}

		startQueueDrain();

		if (messages.length == 1) {
			queueSingleDelete(priority, messages[0]);
			return;
		}

		queueMessageDeletion(priority, Arrays.asList(messages));
	}

	public void queueMessageDeletion(CallPriority priority, Collection<IMessage> messages) {
		// Bulk delete requires messages to be within the last 14 days. To be safe, we pad our check by an hour.
		LocalDateTime bulkDeleteableBefore = LocalDateTime.now().plusDays(13).plusHours(12);
		// Collect messages by channel to ensure bulk delete will work.
		Map<Pair<IChannel, Boolean>, List<IMessage>> messagesByChannel = messages.stream()
				.collect(Collectors.groupingBy(
						message -> new ImmutablePair<IChannel, Boolean>(message.getChannel(),
								// FIXME Discord is throwing its 14 days or less on most bulk deletes
								false && message.getTimestamp().isBefore(bulkDeleteableBefore))));

		for (Map.Entry<Pair<IChannel, Boolean>, List<IMessage>> entry : messagesByChannel.entrySet()) {

			if (entry.getValue().size() == 1) {
				// Single message, single delete.
				queueSingleDelete(priority, entry.getValue().get(0));
				continue;
			}

			if (!entry.getKey().getRight()) {
				// Messages older than 14 days, single delete all
				for (IMessage message : entry.getValue()) {
					queueSingleDelete(priority, message);
				}
				continue;
			}

			while (entry.getValue().size() > 0) {
				List<IMessage> subList = entry.getValue().subList(0, Math.min(100, entry.getValue().size()));
				List<IMessage> messageList = new ArrayList<>();
				messageList.addAll(subList);
				subList.clear();
				this.queue(new DiscordCallable(priority) {
					@Override
					public void call() throws DiscordException, RateLimitException, MissingPermissionsException {
						entry.getKey().getLeft().getMessages().bulkDelete(messageList);
					}
				});
			}
		}
	}

	private void queueSingleDelete(CallPriority priority, IMessage message) {
		drainQueueThread.queue(new DiscordCallable(priority, 1) {
			@Override
			public void call() throws MissingPermissionsException, RateLimitException, DiscordException {
				message.delete();
			}
		});
	}

	public IDiscordClient getClient() {
		return this.client;
	}

	public String getGeneralChannel() {
		return this.channelGeneral;
	}

	public String getMainChannel() {
		return this.channelMain;
	}

	public String getLogChannel() {
		return this.channelLog;
	}

	public String getReportChannel() {
		return this.channelReports;
	}

	@SuppressWarnings("unchecked")
	public <T extends DiscordModule> T getModule(Class<T> clazz) throws IllegalArgumentException {
		Validate.isTrue(DiscordModule.class.isAssignableFrom(clazz), clazz.getName() + " is not a DiscordModule.");
		Validate.isTrue(modules.containsKey(clazz), "Module not enabled!");
		Object object = modules.get(clazz);
		Validate.isTrue(clazz.isAssignableFrom(object.getClass()));
		return (T) object;
	}

	public void log(String message) {
		postMessage(this.getBotName(), message, getLogChannel());
	}

	public void postMessage(Message message, boolean global) {
		if (global) {
			postMessage((message.isThirdPerson() ? "* " : "") + message.getSenderName(),
					message.getDiscordMessage(), getMainChannel());
		}
		postMessage(this.getBotName(), message.getConsoleMessage(), getLogChannel());
	}

	public void postMessage(String name, String message, boolean global) {
		if (global) {
			postMessage(name, message, getMainChannel(), getLogChannel());
		} else {
			postMessage(name, message, getLogChannel());
		}
	}

	public void postMessage(String name, String message, String... channels) {
		if (!isEnabled()) {
			return;
		}
		name = ChatColor.stripColor(name);
		// TODO allow formatting codes in any chat? Could support markdown rather than &codes.
		message = ChatColor.stripColor(message);
		if (message.trim().isEmpty()) {
			return;
		}
		// Discord is case-sensitive. This prevents an @everyone alert without altering content.
		message = message.replace("@everyone", "@Everyone").replace("@here", "@Here");
		StringBuilder builder = new StringBuilder(message.length());
		Matcher matcher = spaceword.matcher(message);
		while (matcher.find()) {
			builder.append(matcher.group(1));
			String word = matcher.group(2);
			if (!TextUtils.URL_PATTERN.matcher(word).find()) {
				word = toEscape.matcher(word).replaceAll("\\\\$1");
			}
			builder.append(word);
		}
		for (int index = 0, nextIndex = 2000; index < builder.length(); index = nextIndex, nextIndex += 2000) {
			// TODO: split at logical areas - spaces, dashes, etc. rather than a hard limit
			if (nextIndex > builder.length()) {
				nextIndex = builder.length();
			}
			message = builder.substring(index, nextIndex);
			for (String channel : channels) {
				addMessageToQueue(channel, name, message);
			}
		}
	}

	private void addMessageToQueue(final String channel, final String name, final String message) {
		if (drainQueueThread == null) {
			// Bot has not finished logging in. Forget it, it's just some chat.
			return;
		}
		drainQueueThread.queue(new DiscordCallable(CallPriority.HIGH) {
			@Override
			public void call() throws MissingPermissionsException, RateLimitException, DiscordException {
				IChannel group = client.getChannelByID(channel);
				if (group == null) {
					IUser user = client.getUserByID(channel);
					if (user == null) {
						return;
					}
					try {
						group = client.getOrCreatePMChannel(user);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}
				StringBuilder builder = new StringBuilder();
				if (channel.equals(getMainChannel()) && !name.equals(getBotName())) {
					builder.append("**").append(toEscape.matcher(name).replaceAll("\\\\$1"));
					if (!name.startsWith("* ")) {
						builder.append(':');
					}
					builder.append("** ");
				}
				builder.append(message);
				try {
					group.sendMessage(builder.toString());
				} catch (NoSuchElementException e) {
					// Internal Discord fault, don't log.
					return;
				}
			}
		});
	}

	public void postReport(String message) {
		postMessage(this.getBotName(), message, getReportChannel());
	}

	/**
	 * Update a Discord user's status. Handles new users (alerts them they must link, then kicks after a day if they have not)
	 * 
	 * @param user the IUser
	 */
	public void updateUser(IUser user) {
		UUID uuid = this.getUUIDOf(user);

		if (uuid == null) {
			this.updateUnlinkedUser(user);
			return;
		}

		this.updateLinkedUser(user, uuid);
	}

	private void updateUnlinkedUser(IUser user) {
		if (this.isLinked(user) || this.getClient().getOurUser().equals(user)) {
			return;
		}

		long now = System.currentTimeMillis();

		this.getClient().getGuilds().forEach(guild -> {

			// Check if a user has roles - if they have roles, they're recognized.
			// N.B.: Discord4J explicitly declares users to have the @everyone role
			for (IRole role : guild.getRolesForUser(user)) {
				if (!role.equals(guild.getEveryoneRole())) {
					return;
				}
			}

			String path = "unlinked." + guild.getID() + '.'  + user.getID();

			if (!discordData.isSet(path)) {
				// 1 day grace period
				discordData.set(path, now + 86400000);
				this.postMessage(this.getBotName(),
						lang.getValue("discord.link.mandate").replace("{USER}", user.mention()),
						this.getGeneralChannel());
				return;
			}

			long kickTime = discordData.getLong(path);

			if (kickTime <= now) {
				discordData.set(path, null);
				queue(new DiscordCallable(CallPriority.LOW) {
					@Override
					public void call() throws DiscordException, RateLimitException, MissingPermissionsException {
						guild.kickUser(user);
					}
				});
			}
		});
	}

	/**
	 * Update a linked Minecraft player's Discord account. Includes nicknaming and automatic role
	 * management.
	 * 
	 * @param user the user, or null if it is to be searched for
	 * @param uuid the UUID of the player
	 */
	public void updateLinkedUser(@Nullable IUser user, UUID uuid) {

		if (user == null) {
			// Given player object, find user ID from UUID
			String uuidString = uuid.toString();
			for (String path : discordData.getConfigurationSection("users").getKeys(false)) {
				if (uuidString.equals(discordData.getString("users." + path))) {
					user = getClient().getUserByID(path);
					break;
				}
			}
		}

		if (user == null) {
			return;
		}

		// Yes, yes, this is final now, shut up compiler.
		final IUser iuser = user;

		// Load a permissible player
		Player loadedPlayer = PlayerLoader.getPlayer(getPlugin(), uuid);
		if (loadedPlayer == null) {
			return;
		}
		if (!loadedPlayer.isOnline() && !(loadedPlayer instanceof PermissiblePlayer)) {
			loadedPlayer = new PermissiblePlayer(loadedPlayer);
		}

		// Finalize for callable
		final Player player = loadedPlayer;

		for (IGuild guild : this.getClient().getGuilds()) {
			ConfigurationSection guildRoles = this.getConfig().getConfigurationSection("roles." + guild.getID());
			if (guildRoles == null) {
				continue;
			}
			final List<IRole> roles = new ArrayList<>();
			for (String roleName : guildRoles.getKeys(false)) {
				if (!player.hasPermission("sblock.group." + roleName)) {
					continue;
				}

				String roleID = guildRoles.getString(roleName);
				IRole role = guild.getRoleByID(roleID);
				if (role == null) {
					continue;
				}
				roles.add(role);
			}

			List<IRole> currentRoles = iuser.getRolesForGuild(guild);
			if (!currentRoles.containsAll(roles)) {
				for (IRole role : currentRoles) {
					if (!roles.contains(role)) {
						roles.add(role);
					}
				}
	
				IRole[] roleArray = roles.toArray(new IRole[roles.size()]);
				this.queue(new DiscordCallable() {
					@Override
					public void call() throws DiscordException, RateLimitException, MissingPermissionsException {
						guild.editUserRoles(iuser, roleArray);
					}
				});
			}

			if (!this.getConfig().getBoolean("linkName")) {
				continue;
			}
	
			if (player.getName() != null) {
				Optional<String> name = iuser.getNicknameForGuild(guild);
				if (!name.isPresent() || !player.getName().equals(name.get())) {
					this.queue(new DiscordCallable(CallPriority.LOW) {
						@Override
						public void call() throws DiscordException, RateLimitException, MissingPermissionsException {
							guild.setUserNickname(iuser, player.getName());
						}
					});
				}
			}
		}
	}

	public LoadingCache<Object, Object> getAuthCodes() {
		return authentications;
	}

	public boolean isLinked(IUser user) {
		return discordData.isString("users." + user.getID());
	}

	public UUID getUUIDOf(IUser user) {
		String uuidString = discordData.getString("users." + user.getID());
		if (uuidString == null) {
			return null;
		}
		return UUID.fromString(uuidString);
	}

	public void addLink(UUID uuid, IUser user) {
		discordData.set("users." + user.getID(), uuid.toString());

		updateLinkedUser(user, uuid);
	}

	public DiscordPlayer getDiscordPlayerFor(IUser user) {
		UUID uuid = getUUIDOf(user);
		if (uuid == null) {
			return null;
		}
		Player player = PlayerLoader.getPlayer(this.getPlugin(), uuid);
		if (player instanceof DiscordPlayer) {
			return (DiscordPlayer) player;
		}
		// PlayerLoader loads a PermissiblePlayer, wrapping a wrapper would be silly.
		DiscordPlayer dplayer = new DiscordPlayer(this, user, player.getPlayer());
		PlayerLoader.modifyCachedPlayer(dplayer);
		return dplayer;
	}

	public boolean handleDiscordCommand(String command, IUser user, IChannel channel) {
		String[] split = command.split("\\s");
		if (!commands.containsKey(split[0])) {
			return false;
		}
		String[] args = new String[split.length - 1];
		if (args.length > 0) {
			System.arraycopy(split, 1, args, 0, args.length);
		}
		commands.get(split[0]).execute(user, channel, command, args);
		return true;
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public String getName() {
		return "Discord";
	}

}
