package com.easterlyn.utilities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Enum managing worlds and resource packs.
 * 
 * @author Jikoo
 */
public class RegionUtils {

	public static boolean regionsMatch(String worldName, String otherWorldName) {
		worldName = worldName.toUpperCase().replace("_NETHER", "").replace("_THE_END", "");
		otherWorldName = otherWorldName.toUpperCase().replace("_NETHER", "").replace("_THE_END", "");
		return worldName.equals(otherWorldName);
	}

	public static Location calculatePortalDestination(Location from, Material portalType) {
		if (portalType != Material.PORTAL && portalType != Material.ENDER_PORTAL) {
			return null;
		}

		World world = null;
		double x, y, z;
		switch (from.getWorld().getName()) {
		case "Earth":
			if (portalType == Material.PORTAL) {
				world = Bukkit.getWorld("Earth_nether");
				x = from.getX() / 8;
				y = from.getY();
				z = from.getZ() / 8;
				break;
			}
		case "Earth_nether":
			if (portalType == Material.PORTAL) {
				world = Bukkit.getWorld("Earth");
				x = from.getX() * 8;
				y = from.getY();
				z = from.getZ() * 8;
				break;
			}
		default:
			switch (from.getWorld().getEnvironment()) {
			case NETHER:
				if (portalType == Material.ENDER_PORTAL) {
					return null;
				}
				world = Bukkit.getWorld(from.getWorld().getName().replaceAll("_.*", ""));
				if (world == null || world.equals(from.getWorld())) {
					return null;
				}
				x = from.getX() * 8;
				y = Math.min(2, Math.max(251, Math.floor(from.getY() * 2.05)));
				z = from.getZ() * 8;
			case NORMAL:
				if (portalType == Material.ENDER_PORTAL) {
					world = Bukkit.getWorld(from.getWorld().getName() + "_the_end");
					return world != null ? world.getSpawnLocation() : null;
				}
				world = Bukkit.getWorld(from.getWorld().getName() + "_nether");
				x = from.getX() / 8;
				y = Math.min(2, Math.max(123, Math.ceil(y = from.getY() / 2.05)));
				z = from.getZ() / 8;
				break;
			case THE_END:
			default:
				if (portalType == Material.PORTAL) {
					return null;
				}
				world = Bukkit.getWorld(from.getWorld().getName().replace("_the_end", "").replace("_nether", ""));
				return world != null ? world.getSpawnLocation() : null;
			}
		}
		if (world == null) {
			return null;
		}
		return new Location(world, x, y, z, from.getYaw(), from.getPitch());
	}

	public static Block getAdjacentPortalBlock(Block block) {
		// Player isn't standing inside the portal block, they're next to it.
		if (block.getType() == Material.PORTAL || block.getType() == Material.ENDER_PORTAL) {
			return block;
		}
		for (int dX = -1; dX < 2; dX++) {
			for (int dY = -1; dY < 4; dY++) {
				// Search higher in case of end portals, falling through at speed can lead to portal usage from a position well beyond
				for (int dZ = -1; dZ < 2; dZ++) {
					if (dX == 0 && dY == 0 && dZ == 0) {
						continue;
					}
					Block maybePortal = block.getRelative(dX, dY, dZ);
					if (maybePortal.getType() == Material.PORTAL || block.getType() == Material.ENDER_PORTAL) {
						return maybePortal;
					}
				}
			}
		}
		return null;
	}

	public static Location findNetherPortalCenter(Block portal) {
		if (portal == null) {
			return null;
		}
		double minX = 0;
		while (portal.getRelative((int) minX - 1, 0, 0).getType() == Material.PORTAL) {
			minX -= 1;
		}
		double maxX = 0;
		while (portal.getRelative((int) maxX + 1, 0, 0).getType() == Material.PORTAL) {
			maxX += 1;
		}
		double minY = 0;
		while (portal.getRelative(0, (int) minY - 1, 0).getType() == Material.PORTAL) {
			minY -= 1;
		}
		double minZ = 0;
		while (portal.getRelative(0, 0, (int) minZ - 1).getType() == Material.PORTAL) {
			minZ -= 1;
		}
		double maxZ = 0;
		while (portal.getRelative(0, 0, (int) maxZ + 1).getType() == Material.PORTAL) {
			maxZ += 1;
		}
		double x = portal.getX() + (maxX + 1 + minX) / 2.0;
		double y = portal.getY() + minY + 0.1;
		double z = portal.getZ() + (maxZ + 1 + minZ) / 2.0;
		return new Location(portal.getWorld(), x, y, z);
	}

}