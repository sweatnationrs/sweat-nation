package com.sweatnation.messages;


/** Sendes fortløpende mens live-kartet er på. hidden=true betyr "jeg skrudde av live-kartet". */
public class LocationUpdate extends SnMessage
{
	public String name;
	public int x;
	public int y;
	public int plane;
	public int world;
	public int combatLevel;
	public boolean hidden;
	/** Ikonet avsenderen har valgt for seg selv: "ROSE" eller "FLAME". */
	public String logo;

	public LocationUpdate()
	{
	}

	public LocationUpdate(String name, int x, int y, int plane, int world, int combatLevel, boolean hidden)
	{
		this.name = name;
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.world = world;
		this.combatLevel = combatLevel;
		this.hidden = hidden;
	}
}
