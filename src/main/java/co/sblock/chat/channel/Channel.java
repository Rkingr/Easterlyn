package co.sblock.chat.channel;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import co.sblock.Sblock;
import co.sblock.chat.ChannelManager;
import co.sblock.chat.Chat;
import co.sblock.users.User;
import co.sblock.users.Users;

/**
 * Defines default channel behavior
 *
 * @author Dublek, Jikoo
 */
public abstract class Channel {

	private final Sblock plugin;
	private final Users users;
	private final ChannelManager manager;
	/* Immutable Data regarding the channel */
	protected final String name;
	protected final Set<UUID> listening;
	protected final UUID owner;

	/**
	 * @param name the name of the channel
	 * @param creator the owner of the channel
	 */
	public Channel(Sblock plugin, String name, UUID creator) {
		this.plugin = plugin;
		this.users = plugin.getModule(Users.class);
		this.manager = plugin.getModule(Chat.class).getChannelManager();
		this.name = name;
		this.owner = creator;
		listening = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
	}

	/**
	 * @return the channel's name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return this channel's access level
	 */
	public abstract AccessLevel getAccess();

	/**
	 * @return all UUID's of users listening to this channel
	 */
	public Set<UUID> getListening() {
		return this.listening;
	}

	/**
	 * @return the UUID of the channel owner
	 */
	public UUID getOwner() {
		return this.owner;
	}

	/**
	 * @param user a user
	 * @return if this user is an owner
	 */
	public abstract boolean isOwner(User user);

	/**
	 * @param user a user
	 * @return whether this user has permission to moderate the channel
	 */
	public abstract boolean isModerator(User user);


	/**
	 * Check if the user is in the banlist AND not a denizen.
	 *
	 * @param user a user
	 * @return whether this user is banned
	 */
	public abstract boolean isApproved(User user);

	/**
	 * Check if the given OfflineUser is banned.
	 *
	 * @param user the OfflineUser
	 * @return true if the OfflineUser is banned
	 */
	public abstract boolean isBanned(User user);

	/**
	 * Check if the channel has been recently accessed and should not be deleted.
	 * 
	 * @return true if the channel should not be deleted
	 */
	public abstract boolean isRecentlyAccessed();

	/**
	 * Update the last access time.
	 */
	public abstract void updateLastAccess();

	/**
	 * For sending a channel message, not for chat! Chat should be sent by constructing a Message with a MessageBuilder.
	 *
	 * @param message the message to send the channel.
	 */
	public abstract void sendMessage(String message);

	/**
	 * Gets the ChannelManager this Channel is registered in.
	 * 
	 * @return the ChannelManager
	 */
	public ChannelManager getChannelManager() {
		return this.manager;
	}

	/**
	 * Gets the Sblock instance creating this Channel.
	 * 
	 * @return the Sblock
	 */
	public Sblock getPlugin() {
		return this.plugin;
	}

	/**
	 * Gets the Users instance used to fetch Users.
	 * 
	 * @return the Users
	 */
	protected Users getUsers() {
		return users;
	}

	@Override
	public String toString() {
		return this.getName() + ": Access: " + this.getAccess() + " Type: "
				+ this.getClass().getSimpleName() + "\nOwner: "
				+ (this.owner != null ? Bukkit.getOfflinePlayer(this.getOwner()).getName() : "default");
	}
}
