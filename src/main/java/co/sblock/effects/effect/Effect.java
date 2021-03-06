package co.sblock.effects.effect;

import co.sblock.Sblock;

/**
 * Base class defining properties for an Effect.
 * <p>
 * No behaviors are defined, the Effect must implement BehaviorActive, BehaviorPassive, or
 * BehaviorReactive.
 * 
 * @author Jikoo
 */
public abstract class Effect {

	// The Sblock instance loading the Effect
	private final Sblock plugin;
	// The user-friendly name of the Effect
	private final String name;
	// Cost of the Effect during alchemy
	private final int cost;
	// Maximum level per individual item
	private final int maximumLevel;
	// Maximum level with any effect on play
	private final int maximumCombinedLevel;

	public Effect(Sblock plugin, int cost, int maximumLevel, int maximumCombinedLevel, String name) {
		this.plugin = plugin;
		this.name = name;
		this.cost = cost;
		this.maximumLevel = maximumLevel;
		this.maximumCombinedLevel = maximumCombinedLevel;
	}

	/**
	 * Gets the name this Effect goes by.
	 * 
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the cost of the Effect for use in alchemy.
	 * 
	 * @return the cost
	 */
	public int getCost() {
		return this.cost;
	}

	/**
	 * Gets the maximum level of an Effect per gear part.
	 * 
	 * @return the maximum level
	 */
	public int getMaxLevel() {
		return this.maximumLevel;
	}

	/**
	 * Gets the maximum level of an Effect for all gear parts combined.
	 * 
	 * @return the maximum level
	 */
	public int getMaxTotalLevel() {
		return this.maximumCombinedLevel;
	}

	/**
	 * Gets the Sblock instance loading this Effect.
	 * 
	 * @return the Sblock instance
	 */
	public Sblock getPlugin() {
		return this.plugin;
	}

}
