package com.sweatnation;

import net.runelite.api.coords.WorldPoint;

/** Avstand og kompassretning mellom to punkter, på norsk. */
final class Geo
{
	private static final String[] DIRS = {"Ø", "NØ", "N", "NV", "V", "SV", "S", "SØ"};
	private static final String[] DIRS_EN = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};

	private Geo()
	{
	}

	static String direction(WorldPoint from, WorldPoint to)
	{
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		if (dx == 0 && dy == 0)
		{
			return "";
		}
		double angle = Math.toDegrees(Math.atan2(dy, dx));
		int idx = (int) Math.round(((angle + 360) % 360) / 45.0) % 8;
		return L.isEnglish() ? DIRS_EN[idx] : DIRS[idx];
	}

	static String describe(WorldPoint from, WorldPoint to)
	{
		if (from == null || to == null)
		{
			return "?";
		}
		if (from.getPlane() != to.getPlane() && from.distanceTo2D(to) < 20)
		{
			return L.t("annen etasje", "other floor");
		}
		int d = from.distanceTo2D(to);
		if (d <= 1)
		{
			return L.t("her", "here");
		}
		if (d > 4000)
		{
			return L.t("langt unna", "far away");
		}
		return d + L.t(" ruter ", " tiles ") + direction(from, to);
	}
}
