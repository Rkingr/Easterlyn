package co.sblock.commands.fun;

import java.util.List;

import co.sblock.Sblock;
import co.sblock.commands.SblockCommand;
import co.sblock.events.packets.ParticleEffectWrapper;
import co.sblock.micromodules.ParticleUtils;

import com.google.common.collect.ImmutableList;

import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * SblockCommand for riding a firework in style.
 * 
 * @author Jikoo
 */
public class CrotchRocketCommand extends SblockCommand {

	private final ParticleUtils particles;

	public CrotchRocketCommand(Sblock plugin) {
		super(plugin, "crotchrocket");
		this.setPermissionLevel("felt");
		this.particles = plugin.getModule(ParticleUtils.class);
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(getLang().getValue("command.general.noConsole"));
			return true;
		}
		return launch((LivingEntity) sender);
	}

	public boolean launch(LivingEntity entity) {
		entity.setFallDistance(0);
		entity.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, entity.getLocation(), 1);

		final Firework firework = (Firework) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.FIREWORK);
		FireworkMeta fm = firework.getFireworkMeta();
		fm.setPower(4);
		firework.setFireworkMeta(fm);
		firework.setPassenger(entity);

		particles.addEntity(firework, new ParticleEffectWrapper(Particle.FIREWORKS_SPARK, 5));

		new BukkitRunnable() {

			private int count = 0;

			@Override
			public void run() {
				if (count > 39 || firework.getLocation().getY() > 255) {
					particles.removeAllEffects(firework);
					firework.remove();
					cancel();
					return;
				}
				++count;
				firework.setVelocity(new Vector(0, 1, 0));
			}
		}.runTaskTimer(getPlugin(), 0L, 1L);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return ImmutableList.of();
	}
}
