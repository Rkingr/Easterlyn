package co.sblock.effects.effect.passive;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import co.sblock.Sblock;
import co.sblock.effects.effect.BehaviorCooldown;
import co.sblock.effects.effect.BehaviorPassive;
import co.sblock.effects.effect.Effect;
import co.sblock.utilities.Potions;

/**
 * Effect for passively granting the speed PotionEffect.
 * 
 * @author Jikoo
 */
public class EffectSpeed extends Effect implements BehaviorPassive, BehaviorCooldown {

	public EffectSpeed(Sblock plugin) {
		super(plugin, 500, 2, 10, "Speed");
	}

	@Override
	public String getCooldownName() {
		return "Effect:Speed";
	}

	@Override
	public long getCooldownDuration() {
		return 5000;
	}

	@Override
	public void applyEffect(LivingEntity entity, int level) {
		if (level < 1) {
			level = 1;
		}
		int duration = entity instanceof Player ? 200 : Integer.MAX_VALUE;
		Potions.applyIfBetter(entity, new PotionEffect(PotionEffectType.SPEED, duration, level - 1));
	}

}
