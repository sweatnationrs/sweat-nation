package com.sweatnation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.client.util.ImageUtil;

/**
 * Kartmarkører: et lilla merke med rosen eller dråpen fra SweatNation-banneret,
 * og navneskilt over. Størrelsen følger zoomen på verdenskartet.
 */
final class MapMarkers
{
	/** Banner-lilla, samme farge som på SweatNation-banneret. */
	static final Color BANNER_PURPLE = new Color(0x88, 0x63, 0x9B);
	private static final Color BANNER_PURPLE_DARK = new Color(0x5A, 0x3E, 0x68);
	private static final Color OTHER_WORLD = new Color(0x6E, 0x6E, 0x6E);
	private static final int BASE_BADGE = 24;

	private static BufferedImage rose;
	private static BufferedImage flame;
	private static BufferedImage skullImg;
	private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

	private MapMarkers()
	{
	}

	private static BufferedImage logo(SweatNationConfig.MarkerLogo which)
	{
		if (rose == null)
		{
			rose = ImageUtil.loadImageResource(MapMarkers.class, "logo_rose.png");
			flame = ImageUtil.loadImageResource(MapMarkers.class, "logo_flame.png");
		}
		return which == SweatNationConfig.MarkerLogo.FLAME ? flame : rose;
	}

	/** Høyden fra bunnen av bildet til midten av merket (der spilleren står). */
	static int anchorFromBottom(double scale)
	{
		return badgeSize(scale) / 2 + 1;
	}

	private static int badgeSize(double scale)
	{
		return Math.max(14, (int) Math.round(BASE_BADGE * scale));
	}

	static BufferedImage member(String name, int world, boolean sameWorld, SweatNationConfig.MarkerLogo logoType, double scale)
	{
		String key = name + "|" + world + "|" + sameWorld + "|" + logoType + "|" + Math.round(scale * 20);
		return CACHE.computeIfAbsent(key, k -> drawMember(name, world, sameWorld, logoType, scale));
	}

	private static BufferedImage drawMember(String name, int world, boolean sameWorld, SweatNationConfig.MarkerLogo logoType, double scale)
	{
		int badge = badgeSize(scale);
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, (int) Math.round(11 * scale)));
		String label = (name == null ? "?" : name) + (sameWorld ? "" : "  W" + world);

		BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tg = tmp.createGraphics();
		tg.setFont(font);
		FontMetrics fm = tg.getFontMetrics();
		tg.dispose();

		int padX = 5;
		int plateW = fm.stringWidth(label) + padX * 2;
		int plateH = fm.getAscent() + fm.getDescent() + 2;
		int gap = 2;
		int w = Math.max(plateW, badge + 4) + 2;
		int h = plateH + gap + badge + 3;

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// Navneskilt med mørk bakgrunn og lilla kant, så det synes på alle kartfarger
		int px = (w - plateW) / 2;
		g.setColor(new Color(0, 0, 0, 200));
		g.fillRoundRect(px, 0, plateW, plateH, plateH, plateH);
		g.setColor(sameWorld ? BANNER_PURPLE : OTHER_WORLD);
		g.setStroke(new BasicStroke(1.2f));
		g.drawRoundRect(px, 0, plateW - 1, plateH - 1, plateH, plateH);
		g.setFont(font);
		g.setColor(sameWorld ? Color.WHITE : new Color(0xBB, 0xBB, 0xBB));
		g.drawString(label, px + padX, fm.getAscent() + 1);

		// Merket: hvit ytre ring, lilla skive, logoen fra banneret i midten
		int bx = (w - badge) / 2;
		int by = plateH + gap;
		g.setColor(Color.BLACK);
		g.fillOval(bx - 1, by - 1, badge + 2, badge + 2);
		g.setColor(Color.WHITE);
		g.fillOval(bx, by, badge, badge);
		int ring = Math.max(2, badge / 10);
		g.setColor(sameWorld ? BANNER_PURPLE : OTHER_WORLD);
		g.fillOval(bx + ring, by + ring, badge - ring * 2, badge - ring * 2);
		g.setColor(sameWorld ? BANNER_PURPLE_DARK : OTHER_WORLD.darker());
		g.setStroke(new BasicStroke(1f));
		g.drawOval(bx + ring, by + ring, badge - ring * 2 - 1, badge - ring * 2 - 1);

		BufferedImage lg = logo(logoType);
		if (lg != null)
		{
			int inner = badge - ring * 2;
			double s = Math.min(inner * 0.78 / lg.getWidth(), inner * 0.78 / lg.getHeight());
			int lw = Math.max(1, (int) Math.round(lg.getWidth() * s));
			int lh = Math.max(1, (int) Math.round(lg.getHeight() * s));
			g.drawImage(lg, bx + (badge - lw) / 2, by + (badge - lh) / 2, lw, lh, null);
		}
		g.dispose();
		return img;
	}

	/** Rødt dødningshode med navnet over, for der et medlem døde. */
	static BufferedImage skull(String name)
	{
		if (skullImg == null)
		{
			skullImg = ImageUtil.loadImageResource(MapMarkers.class, "skull.png");
		}
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 11);
		BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tg = tmp.createGraphics();
		tg.setFont(font);
		FontMetrics fm = tg.getFontMetrics();
		tg.dispose();
		String label = name == null ? "?" : name;
		int sk = skullImg.getWidth() * 2;
		int plateW = fm.stringWidth(label) + 10;
		int plateH = fm.getAscent() + fm.getDescent() + 2;
		int w = Math.max(plateW, sk) + 2;
		int h = plateH + 2 + sk;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int px = (w - plateW) / 2;
		g.setColor(new Color(0, 0, 0, 200));
		g.fillRoundRect(px, 0, plateW, plateH, plateH, plateH);
		g.setColor(new Color(0xD0, 0x10, 0x10));
		g.drawRoundRect(px, 0, plateW - 1, plateH - 1, plateH, plateH);
		g.setFont(font);
		g.setColor(new Color(0xFF, 0x50, 0x50));
		g.drawString(label, px + 5, fm.getAscent() + 1);
		// skallen er pikselkunst: skaler med "nearest" så den holder seg skarp
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(skullImg, (w - sk) / 2, plateH + 2, sk, sk, null);
		g.dispose();
		return img;
	}
}
