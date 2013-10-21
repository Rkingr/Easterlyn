package co.sblock.Sblock.Chat.Channel;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import co.sblock.Sblock.DatabaseManager;
import co.sblock.Sblock.Chat2.ChatModule;
import co.sblock.Sblock.Chat2.ChatMsgs;
import co.sblock.Sblock.Chat2.ChatUser;
import co.sblock.Sblock.Chat2.ChatUserManager;
import co.sblock.Sblock.Chat2.Channel.Channel;
import co.sblock.Sblock.Chat2.Channel.AccessLevel;
import co.sblock.Sblock.Chat2.Channel.ChannelType;
import co.sblock.Sblock.Utilities.Sblogger;

public class NormalChannel extends Channel {

	protected String name;
	protected AccessLevel access;
	protected String owner;

	protected Set<String> approvedList = new HashSet<String>();
	protected Set<String> modList = new HashSet<String>();
	protected Set<String> muteList = new HashSet<String>();
	protected Set<String> banList = new HashSet<String>();

	protected Set<String> listening = new HashSet<String>();

	public NormalChannel(String name, AccessLevel a, String creator) {
		super(name, a, creator);
		this.name = name;
		this.access = a;
		this.owner = creator;
		this.modList.add(creator);
		DatabaseManager.getDatabaseManager().saveChannelData(this);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override

	public AccessLevel getAccess() {
		return this.access;
	}

	@Override
	public Set<String> getListening() {
		return this.listening;
	}

	@Override
	public ChannelType getType() {
		return ChannelType.NORMAL;
	}

	@Override
	public void addListening(String user) {
		this.listening.add(user);
	}

	@Override
	public void removeListening(String user) {
		this.listening.remove(user);
	}

	@Override
	public void setNick(ChatUser sender, String nick) {
		sender.sendMessage(ChatMsgs.unsupportedOperation(this));
	}

	@Override
	public void removeNick(ChatUser sender) {
		sender.sendMessage(ChatMsgs.unsupportedOperation(this));
	}

	public String getNick(ChatUser sender) {
		return ChatMsgs.unsupportedOperation(this);
	}
	
	@Override
	public boolean hasNick(ChatUser sender)	{
		return false;
	}

	public void setOwner(String newO, ChatUser sender) {
		if (sender.equals(this.owner)) {
			this.owner = newO;
		}
	}

	@Override
	public String getOwner() {
		return this.owner;
	}

	@Override
	public boolean isOwner(ChatUser user) {
		return user.getPlayerName().equalsIgnoreCase(owner);
	}

	@Override
	public void loadMod(String user) {
		this.modList.add(user);
	}

	public void addMod(String username, ChatUser sender) {
		if (!ChatUser.isValidUser(username)) {
			sender.sendMessage(ChatMsgs.errorInvalidUser(username));
			return;
		}
		if (this.isChannelMod(sender) && !modList.contains(username)) {
			this.modList.add(username);
			this.sendToAll(sender,
					ChatMsgs.onUserModAnnounce(username, this), "channel");
			Player targetUser = Bukkit.getPlayerExact(username);
			if (targetUser != null) {
				targetUser.sendMessage(ChatMsgs.onUserMod(this));
			}
		} else if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onUserModFail(this));
		} else {
			sender.sendMessage(ChatMsgs.onUserModAlready(username, this));
		}

	}

	public void removeMod(String target, ChatUser sender) {
		// SburbChat code. Handle with care

		if (modList.contains(sender.getPlayerName())
				&& this.modList.contains(target)) {
			this.modList.remove(target);
			this.sendToAll(sender, ChatColor.YELLOW + target
					+ " is no longer a mod in " + ChatColor.GOLD + this.name
					+ ChatColor.YELLOW + "!", "channel");
			Player targetUser = Bukkit.getPlayerExact(target);
			if (targetUser != null) {
				targetUser.sendMessage(ChatColor.RED
						+ "You are no longer a mod in " + ChatColor.GOLD
						+ this.name + ChatColor.RED + "!");
			}
		} else if (!sender.getPlayerName().equals(this.owner)) {
			sender.sendMessage(ChatColor.RED
					+ "You do not have permission to demod people in "
					+ ChatColor.GOLD + this.name + ChatColor.RED + "!");
		} else {
			sender.sendMessage(ChatColor.YELLOW + target + ChatColor.RED
					+ " is not a mod in " + ChatColor.GOLD + this.name
					+ ChatColor.RED + "!");
		}
	}

	@Override
	public Set<String> getModList() {
		return this.modList;
	}

	@Override
	public boolean isChannelMod(ChatUser user) {
		if (modList.contains(user.getPlayerName())
				|| user.getPlayer().hasPermission("group.denizen")
				|| user.getPlayer().hasPermission("group.horrorterror")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isMod(ChatUser user) {
		if (user.getPlayer().hasPermission("group.denizen")
				|| user.getPlayer().hasPermission("group.horrorterror")) {
			return true;
		}
		return false;
	}

	@Override
	public void kickUser(ChatUser user, ChatUser sender) {
		if (this.isChannelMod(sender) && listening.contains(user.getPlayerName())) {
			this.listening.remove(user);
			user.sendMessage(ChatMsgs.onUserKick(this));
			user.removeListening(this.getName());
			this.sendToAll(sender, ChatMsgs.onUserKickAnnounce(user, this), "channel");
		} else if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onUserKickFail(this));
		} else {
			sender.sendMessage(ChatMsgs.onUserKickedAlready(user, this));
		}

	}

	@Override
	public void loadBan(String user) {
		this.banList.add(user);
	}

	@Override
	public void banUser(String username, ChatUser sender) {
		if (this.isChannelMod(sender)
				&& !banList.contains(username)) {
			if (modList.contains(username)) {
				modList.remove(username);
			}
			if (listening.contains(username)) {
				ChatUser user = ChatUser.getUser(username);
				if (user != null) {
					user.removeListening(this.getName());
					user.sendMessage(ChatMsgs.onUserBan(this));
				}
			}
			this.banList.add(username);
			this.sendToAll(sender, ChatMsgs.onUserBanAnnounce(username, this),
					"channel");
		} else if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onUserBanFail(this));
		} else {
			sender.sendMessage(ChatMsgs.onUserBannedAlready(username, this));
		}
	}

	@Override
	public void unbanUser(String username, ChatUser sender) {
		if (sender.getPlayerName().equalsIgnoreCase(this.owner)
				&& banList.contains(username)) {
			this.banList.remove(username);
			Player user = Bukkit.getPlayerExact(username);
			if (user != null) {
				user.sendMessage(ChatMsgs.onUserUnban(this));
			}
			this.sendToAll(sender, ChatMsgs.onUserUnbanAnnounce(username, this),
					"channel");
		} else if (!sender.getPlayerName().equalsIgnoreCase(owner)) {
			sender.sendMessage(ChatMsgs.onUserUnbanFail(this));
		} else {
			sender.sendMessage(ChatMsgs.onUserUnbannedAlready(username, this));
		}
	}

	@Override
	public Set<String> getBanList() {
		return banList;
	}

	@Override
	public boolean isBanned(ChatUser user) {
		return banList.contains(user.getPlayerName());
	}

	@Override
	public void loadApproval(String user) {
		this.approvedList.add(user);
	}

	@Override
	public void approveUser(ChatUser user, ChatUser sender) {
		if (this.getAccess().equals(AccessLevel.PUBLIC)) {
			sender.sendMessage(ChatColor.GOLD + this.name + ChatColor.RED
					+ " is a public channel!");
			return;
		} else {
			approvedList.add(user.getPlayerName());
		}
	}

	@Override
	public void deapproveUser(ChatUser user, ChatUser sender) {
		if (this.getAccess().equals(AccessLevel.PUBLIC)) {
			sender.sendMessage(ChatColor.GOLD + this.name + ChatColor.RED
					+ " is a public channel!");
			return;
		} else {
			approvedList.remove(user.getPlayerName());
		}
	}

	public Set<String> getApprovedUsers() {
		return approvedList;
	}

	@Override
	public boolean isApproved(ChatUser user) {
		return approvedList.contains(user.getPlayerName())
				|| isChannelMod(user);
	}

	@Override
	public void disband(ChatUser sender) {
		this.sendToAll(sender, ChatMsgs.onChannelDisband(this.getName()), "channel");
		for (String s : this.listening) {
			ChatUserManager.getUserManager().getUser(s).removeListening(this.getName());
		}
		ChatModule.getChatModule().getChannelManager().dropChannel(this.name);
	}

	@Override
	public void sendToAll(ChatUser sender, String s, String type) {
		Set<String> failures = new HashSet<String>();
		for (String name : this.listening) {
			ChatUser u = ChatUserManager.getUserManager().getUser(name);
			if (u != null) {
				u.sendMessageFromChannel(s, this, type);
			} else {
				failures.add(name);
			}
		}
		for (String failure : failures) {
			this.listening.remove(failure);
		}
		Sblogger.infoNoLogName(s);
	}

}
