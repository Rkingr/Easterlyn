package co.sblock.events.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import co.sblock.fx.FXManager;
import co.sblock.users.OnlineUser;
import co.sblock.users.UserManager;
import co.sblock.utilities.vote.SleepVote;

/**
 * Listener for PlayerChangedWorldEvents.
 * 
 * @author Jikoo
 */
public class PlayerChangedWorldListener implements Listener {

	/**
	 * The event handler for PlayerChangedWorldEvents.
	 * 
	 * @param event the PlayerChangedWorldEvent
	 */
	@EventHandler
	public void onPlayerChangedWorlds(PlayerChangedWorldEvent event) {

		SleepVote.getInstance().updateVoteCount(event.getFrom().getName(), event.getPlayer().getName());

		if (event.getFrom().getName().equals("Derspit")) {
			event.getPlayer().resetPlayerTime();
		}

		OnlineUser user = UserManager.getGuaranteedUser(event.getPlayer().getUniqueId()).getOnlineUser();

		user.removeAllEffects();
		FXManager.fullEffectsScan(user);
	}
}
