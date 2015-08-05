package co.sblock.effects.effect.active;

import java.util.Arrays;
import java.util.Collection;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

import co.sblock.effects.effect.Effect;
import co.sblock.effects.effect.EffectBehaviorActive;
import co.sblock.effects.effect.EffectBehaviorCooldown;

/**
 * Try not to die!
 * 
 * @author Jikoo
 */
public class EffectPshoooot extends Effect implements EffectBehaviorActive, EffectBehaviorCooldown {

	public EffectPshoooot() {
		super(350, 5, 10, "Pshoooot");
	}

	@Override
	public String getCooldownName() {
		return "Effect:Pshoooot";
	}

	@Override
	public long getCooldownDuration() {
		return 500;
	}

	@Override
	public Collection<Class<? extends Event>> getApplicableEvents() {
		return Arrays.asList(PlayerInteractEvent.class);
	}

	@Override
	public void handleEvent(Event event, LivingEntity entity, int level) {
		entity.setFallDistance(0);
		entity.setVelocity(entity.getLocation().getDirection().multiply(level + 2));
	}

}
