package com.sweatnation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Tegner lagkameratene dine i lagfargen på minikartet. */
public class TeamMinimapOverlay extends Overlay
{
	private final SweatNationPlugin plugin;
	private final Client client;

	@Inject
	TeamMinimapOverlay(SweatNationPlugin plugin, Client client)
	{
		super(plugin);
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Color c = plugin.teamColor();
		if (c == null || !plugin.teamMinimapEnabled())
		{
			return null;
		}
		Map<String, WorldPoint> spots = plugin.teamLocations();
		if (spots.isEmpty())
		{
			return null;
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (WorldPoint wp : spots.values())
		{
			LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
			if (lp == null)
			{
				continue;
			}
			Point mp = Perspective.localToMinimap(client, lp);
			if (mp == null)
			{
				continue;
			}
			int r = 4;
			g.setColor(Color.BLACK);
			g.fillOval(mp.getX() - r - 1, mp.getY() - r - 1, (r + 1) * 2, (r + 1) * 2);
			g.setColor(c);
			g.fillOval(mp.getX() - r, mp.getY() - r, r * 2, r * 2);
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(1f));
			g.drawOval(mp.getX() - r, mp.getY() - r, r * 2, r * 2);
		}
		return null;
	}
}
