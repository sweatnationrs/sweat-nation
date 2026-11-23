package com.sweatnation;

/** Siste kjente status for en lagkamerat i lag-HUD-en. */
public class TeamMember
{
	public final String name;
	public volatile int hp;
	public volatile int maxHp;
	public volatile int prayer;
	public volatile int maxPrayer;
	public volatile int spec;
	public volatile int world;
	public volatile long lastUpdate;
	/** Våpenet som sist ble brukt til spesialangrep, og når. */
	public volatile int specWeapon = -1;
	public volatile long specAt;
	/** Boosts og buffs, og når de ble mottatt (klokkene teller ned fra da). */
	public volatile java.util.List<com.sweatnation.messages.BuffEntry> buffs = java.util.List.of();
	public volatile long buffsAt;

	public TeamMember(String name)
	{
		this.name = name;
	}
}
