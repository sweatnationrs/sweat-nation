package com.sweatnation.messages;

/** Liv, bønn og spec-energi til lag-HUD-en. */
public class TeamStatus extends SnMessage
{
	public String name;
	public int hp;
	public int maxHp;
	public int prayer;
	public int maxPrayer;
	public int spec;
	public int world;

	public TeamStatus()
	{
	}
}
