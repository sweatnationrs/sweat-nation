package com.sweatnation;

import net.runelite.api.coords.WorldPoint;

/** Siste kjente posisjon/info for et gruppemedlem. */
public class MemberState
{
	private final long memberId;
	private volatile String name;
	private volatile WorldPoint location;
	private volatile int world;
	private volatile int combatLevel;
	private volatile long lastSeen;
	private volatile SweatNationConfig.MarkerLogo logo = SweatNationConfig.MarkerLogo.ROSE;

	public MemberState(long memberId, String name)
	{
		this.memberId = memberId;
		this.name = name;
		this.lastSeen = System.currentTimeMillis();
	}

	public long getMemberId() { return memberId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public WorldPoint getLocation() { return location; }
	public void setLocation(WorldPoint location) { this.location = location; }
	public int getWorld() { return world; }
	public void setWorld(int world) { this.world = world; }
	public int getCombatLevel() { return combatLevel; }
	public void setCombatLevel(int combatLevel) { this.combatLevel = combatLevel; }
	public SweatNationConfig.MarkerLogo getLogo() { return logo; }
	public void setLogo(SweatNationConfig.MarkerLogo logo) { this.logo = logo; }
	public long getLastSeen() { return lastSeen; }
	public void touch() { this.lastSeen = System.currentTimeMillis(); }
}
