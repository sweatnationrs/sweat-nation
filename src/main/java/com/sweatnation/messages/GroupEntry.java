package com.sweatnation.messages;

/** Én gruppe et medlem er med i, slik det meldes til de andre. */
public class GroupEntry
{
	/** Unik nøkkel: AKTIVITET#id, f.eks. "NEX#a1b2c3". Flere grupper kan ha samme aktivitet. */
	public String key;
	public String activity;
	/** Eget navn (egendefinerte grupper). Tomt = aktivitetens navn. */
	public String name = "";
	/** Ønsket gruppestørrelse, og når den sist ble endret (nyeste endring vinner). */
	public int size;
	public long sizeAt;
	public String leader;
	public String role;

	public GroupEntry()
	{
	}

	public GroupEntry copy()
	{
		GroupEntry g = new GroupEntry();
		g.key = key;
		g.activity = activity;
		g.name = name;
		g.size = size;
		g.sizeAt = sizeAt;
		g.leader = leader;
		g.role = role;
		return g;
	}
}
