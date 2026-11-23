package com.sweatnation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Liten boks i spillet med avstand og retning til et aktivt rally. */
public class RallyOverlay extends OverlayPanel
{
	private final SweatNationPlugin plugin;
	private final Client client;

	@Inject
	RallyOverlay(SweatNationPlugin plugin, Client client)
	{
		super(plugin);
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		RallyState rally = plugin.rallyForOverlay();
		if (rally == null)
		{
			return null;
		}
		panelComponent.setBackgroundColor(new Color(43, 37, 28, 225));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(rally.getCallerName() + "'s Rohirrim!")
			.color(OsrsUi.PURPLE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(Places.describe(rally.getTarget())).leftColor(Color.WHITE)
			.build());
		WorldPoint me = plugin.getLocalLocation();
		int myWorld = client.getWorld();
		if (rally.getWorld() != myWorld)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(L.t("Verden", "World")).leftColor(Color.WHITE)
				.right(rally.getWorld() + L.t(" (bytt!)", " (switch!)")).rightColor(OsrsUi.RED)
				.build());
		}
		else if (me != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(L.t("Avstand", "Distance")).leftColor(Color.WHITE)
				.right(Geo.describe(me, rally.getTarget())).rightColor(OsrsUi.YELLOW)
				.build());
		}
		if (rally.getNote() != null && !rally.getNote().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("\"" + rally.getNote() + "\"").leftColor(OsrsUi.CYAN)
				.build());
		}
		long secs = Math.max(0, (rally.getExpiresAt() - System.currentTimeMillis()) / 1000);
		panelComponent.getChildren().add(LineComponent.builder()
			.left(L.t("Kommer", "Coming")).leftColor(Color.WHITE)
			.right(rally.comingCount() + "  /  " + secs / 60 + ":" + String.format("%02d", secs % 60))
			.rightColor(OsrsUi.GREEN)
			.build());
		return super.render(graphics);
	}
}
