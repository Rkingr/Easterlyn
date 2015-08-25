package co.sblock.utilities;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import com.mojang.authlib.GameProfile;

import co.sblock.Sblock;

/**
 * Dummy player for usage in commands.
 * 
 * @author Jikoo
 */
public class WrappedSenderPlayer extends DummyPlayer {

	private final CommandSender sender;
	private final GameProfile profile;
	private final String name;

	public WrappedSenderPlayer (CommandSender sender) {
		this(sender, sender.getName());
	}

	public WrappedSenderPlayer(CommandSender sender, String name) {
		this.sender = sender;
		this.profile = Sblock.getInstance().getFakeGameProfile(sender.getName());
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getUniqueId() {
		return profile.getId();
	}

	@Override
	public void sendMessage(String message) {
		sender.sendMessage(message);
	}

	@Override
	public void sendMessage(String[] messages) {
		sender.sendMessage(messages);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return sender.addAttachment(plugin);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return sender.addAttachment(plugin, ticks);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return sender.addAttachment(plugin, name, value);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return sender.addAttachment(plugin, name, value, ticks);
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return sender.getEffectivePermissions();
	}

	@Override
	public boolean hasPermission(String name) {
		return sender.hasPermission(name);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return sender.hasPermission(perm);
	}

	@Override
	public boolean isPermissionSet(String name) {
		return sender.isPermissionSet(name);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return sender.isPermissionSet(perm);
	}

	@Override
	public void recalculatePermissions() {
		sender.recalculatePermissions();
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		sender.removeAttachment(attachment);
	}

	@Override
	public boolean isOp() {
		return sender.isOp();
	}

	@Override
	public void setOp(boolean arg0) {
		sender.setOp(arg0);
	}

	@Override
	public String getDisplayName() {
		return name;
	}

	@Override
	public String getPlayerListName() {
		return name;
	}

	@Override
	public boolean performCommand(String command) {
		return Bukkit.dispatchCommand(sender, command);
	}

}
