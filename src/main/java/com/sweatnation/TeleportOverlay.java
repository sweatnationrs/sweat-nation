package com.sweatnation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Lyser opp den raskeste teleport-gjenstanden i inventory, utstyr og bank. */
public class TeleportOverlay extends WidgetItemOverlay
{
	private static final Color GLOW = new Color(0xFF, 0x70, 0x40);
	private final SweatNationPlugin plugin;
	private final ItemManager itemManager;

	@Inject
	TeleportOverlay(SweatNationPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		showOnInventory();
		showOnBank();
		showOnEquipment();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Set<Integer> wanted = plugin.teleportHighlights();
		if (!wanted.contains(itemId))
		{
			return;
		}
		Rectangle b = widgetItem.getCanvasBounds();
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), GLOW);
		graphics.drawImage(outline, (int) b.getX(), (int) b.getY(), null);
	}
}
