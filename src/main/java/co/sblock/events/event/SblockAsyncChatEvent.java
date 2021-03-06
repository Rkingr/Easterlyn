package co.sblock.events.event;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import co.sblock.chat.message.Message;
import co.sblock.utilities.CollectionConversions;

/**
 * Event wrapper allowing us to more easily manage our channeled chat system.
 * 
 * @author Jikoo
 */
public class SblockAsyncChatEvent extends AsyncPlayerChatEvent {

	private final Message message;
	private boolean globalCancelled;
	private final boolean checkSpam;

	public SblockAsyncChatEvent(boolean async, Player who, Message message) {
		this(async, who, message, true);
	}

	public SblockAsyncChatEvent(boolean async, Player who, Message message, boolean checkSpam) {
		this(async, who, CollectionConversions.toSet(message.getChannel().getListening(),
				uuid -> Bukkit.getPlayer(uuid)), message, checkSpam);
	}

	public SblockAsyncChatEvent(boolean async, Player who, Set<Player> players, Message message) {
		this(async, who, players, message, true);
	}

	public SblockAsyncChatEvent(boolean async, Player who, Set<Player> players, Message message, boolean checkSpam) {
		super(async, who, message.getMessage(), players);
		setFormat(message.getConsoleFormat());
		this.message = message;
		this.checkSpam = checkSpam;
		this.globalCancelled = false;
	}

	public boolean checkSpam() {
		return checkSpam;
	}

	public Message getSblockMessage() {
		return message;
	}

	@Override
	public void setCancelled(boolean cancel) {
		super.setCancelled(cancel);
		globalCancelled = false;
	}

	public void setGlobalCancelled(boolean cancelled) {
		if (cancelled && !isCancelled()) {
			globalCancelled = true;
			super.setCancelled(true);
			return;
		}
		globalCancelled = false;
	}

	public boolean isGlobalCancelled() {
		return globalCancelled;
	}
}
