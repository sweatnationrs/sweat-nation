package com.sweatnation;

import java.awt.Color;

/** Roller i lobbyen. */
public enum TeamRole
{
	ANY("Fri", "Any", new Color(0x8C, 0x8C, 0x8C)),
	TANK("Tank", "Tank", new Color(0xA0, 0x64, 0xF0)),
	MELEE("Melee", "Melee", new Color(0xDC, 0x3C, 0x32)),
	RANGE("Range", "Range", new Color(0x37, 0xC8, 0x5A)),
	MAGE("Mage", "Mage", new Color(0x3C, 0x8C, 0xF0)),
	SUPPORT("Støtte", "Support", new Color(0xF0, 0xC8, 0x28));

	private final String label;
	private final String labelEn;
	private final Color color;

	TeamRole(String label, String labelEn, Color color)
	{
		this.label = label;
		this.labelEn = labelEn;
		this.color = color;
	}

	public String getLabel()
	{
		return L.t(label, labelEn);
	}

	public Color getColor()
	{
		return color;
	}

	public TeamRole next()
	{
		return values()[(ordinal() + 1) % values().length];
	}

	public static TeamRole of(String s)
	{
		try
		{
			return s == null ? ANY : valueOf(s);
		}
		catch (IllegalArgumentException e)
		{
			return ANY;
		}
	}
}
