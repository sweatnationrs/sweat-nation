package com.sweatnation;

import net.runelite.api.coords.WorldPoint;

/** Et dødsfall som vises som rødt dødningshode på kartet. */
public class DeathRecord
{
	public final String name;
	public final WorldPoint location;
	public final int world;
	public final long time = System.currentTimeMillis();
	/** Hvem som drepte: monsternavn, eller "P:navn" for spillere. null = ukjent. */
	public final String killer;

	public DeathRecord(String name, WorldPoint location, int world, String killer)
	{
		this.name = name;
		this.location = location;
		this.world = world;
		this.killer = killer == null || killer.isEmpty() ? null : killer;
	}

	/** «drept av Vet'ion» / «killed by Vet'ion», eller «døde» / «died» når vi ikke vet hvem. */
	public String cause()
	{
		return cause(L.isEnglish());
	}

	public String cause(boolean english)
	{
		if (killer == null)
		{
			return english ? "died" : "døde";
		}
		return (english ? "killed by " : "drept av ") + killerName(killer, english);
	}

	/** "P:Zezima" → «spilleren Zezima» / «the player Zezima». */
	public static String killerName(String killer, boolean english)
	{
		if (killer.startsWith("P:"))
		{
			return (english ? "the player " : "spilleren ") + killer.substring(2);
		}
		return killer;
	}

	public long minutesAgo()
	{
		return (System.currentTimeMillis() - time) / 60_000L;
	}
}
