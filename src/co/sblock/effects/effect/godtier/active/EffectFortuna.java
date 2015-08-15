package co.sblock.effects.effect.godtier.active;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import co.sblock.effects.effect.BehaviorActive;
import co.sblock.effects.effect.BehaviorGodtier;
import co.sblock.effects.effect.Effect;
import co.sblock.users.UserAspect;

import net.md_5.bungee.api.ChatColor;

/**
 * 
 * 
 * @author Jikoo
 */
public class EffectFortuna extends Effect implements BehaviorActive, BehaviorGodtier {

	public EffectFortuna() {
		super(1000, 5, 5, "Fortuna");
	}

	@Override
	public Collection<Class<? extends Event>> getApplicableEvents() {
		// TODO should these be separate? Likely yes.
		return Arrays.asList(BlockBreakEvent.class, PlayerFishEvent.class, EntityDeathEvent.class);
	}

	@Override
	public void handleEvent(Event event, LivingEntity entity, int level) {
		// TODO
		
	}

	@Override
	public Collection<UserAspect> getAspects() {
		return Arrays.asList(UserAspect.LIGHT);
	}

	@Override
	public List<String> getDescription(UserAspect aspect) {
		ArrayList<String> list = new ArrayList<>();
		switch (aspect) {
		case LIGHT:
			list.add(aspect.getColor() + "Fortuna");
			break;
		default:
			break;
		}
		list.add(ChatColor.WHITE + "Fortune just isn't enough.");
		return list;
	}

}