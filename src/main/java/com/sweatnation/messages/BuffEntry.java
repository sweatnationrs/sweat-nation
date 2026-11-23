package com.sweatnation.messages;

/**
 * Én boost eller buff i HUD-en.
 * icon = ikon-id (bilde sendt med BuffIcon), text = liten tekst (f.eks. «+19»),
 * frac = andel igjen (0–1) for boosts, remainingMs/totalMs for buffs med tid.
 */
public class BuffEntry
{
	public String icon;
	public String text;
	public float frac = -1f;
	public long remainingMs;
	public long totalMs;
}
