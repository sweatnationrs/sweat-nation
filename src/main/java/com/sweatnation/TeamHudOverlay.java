package com.sweatnation;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Lag-HUD: liv, bønn og spec-energi for laget ditt. Når noen bruker spesialangrep,
 * pulserer våpenikonet til høyre for barene 5 ganger (6 sekunder). Størrelsen styres i innstillingene.
 */
public class TeamHudOverlay extends Overlay
{
	/** Ikonet pulserer 5 ganger à 1,2 sekunder = 6 sekunder. */
	private static final double BLINK_PERIOD_MS = 1200.0;
	private static final int BLINK_CYCLES = 5;
	private static final long SPEC_FLASH_MS = (long) (BLINK_PERIOD_MS * BLINK_CYCLES);
	private static final Color BG = new Color(20, 20, 20, 190);
	private static final Color HP = new Color(0xC8, 0x28, 0x28);
	private static final Color HP_BG = new Color(0x3C, 0x14, 0x14);
	private static final Color PRAYER = new Color(0x3C, 0x8C, 0xE6);
	private static final Color PRAYER_BG = new Color(0x19, 0x23, 0x37);
	private static final Color SPEC = new Color(0xE6, 0xC8, 0x28);
	private static final Color SPEC_BG = new Color(0x32, 0x32, 0x14);

	private final SweatNationPlugin plugin;
	private final ItemManager itemManager;

	@Inject
	TeamHudOverlay(SweatNationPlugin plugin, ItemManager itemManager)
	{
		super(plugin);
		this.plugin = plugin;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		SweatNationPlugin.HudView view = plugin.hudView();
		if (view == null || view.members.size() < 2)
		{
			return null;
		}
		double s = plugin.hudScale();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		return draw(g, view.title, view.color, view.members, s, System.currentTimeMillis(), this::icon,
			plugin.showBuffs() ? plugin.buffTracker.icons::get : null);
	}

	private BufferedImage icon(int itemId)
	{
		return itemManager.getImage(itemId);
	}

	interface IconSource
	{
		BufferedImage get(int itemId);
	}

	/** Ikoner for boosts og buffs (null = vis ikke boosts og buffs). */
	interface BuffIconSource
	{
		BufferedImage get(String id);
	}

	/** Selve tegningen (statisk så den også kan brukes til forhåndsvisning). */
	static Dimension draw(Graphics2D g, String heading, Color color, List<TeamMember> members, double s, long now, IconSource icons,
		BuffIconSource buffIcons)
	{
		int pad = (int) Math.round(6 * s);
		int barW = (int) Math.round(120 * s);
		int iconW = (int) Math.round(30 * s);
		int width = pad * 3 + barW + iconW;
		Font title = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, (int) Math.round(11 * s)));
		Font name = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, (int) Math.round(11 * s)));
		Font small = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(7, (int) Math.round(9 * s)));
		FontMetrics fmT = g.getFontMetrics(title);
		FontMetrics fmN = g.getFontMetrics(name);
		FontMetrics fmS = g.getFontMetrics(small);

		int hpH = Math.max(fmS.getHeight(), (int) Math.round(11 * s));
		int prH = Math.max(2, (int) Math.round(4 * s));
		int spH = Math.max(2, (int) Math.round(3 * s));
		int gap = Math.max(1, (int) Math.round(2 * s));
		int rowH = fmN.getHeight() + gap + hpH + gap + prH + gap + spH + pad;
		int titleH = fmT.getHeight() + gap;
		// Boosts og buffs: små ikoner under barene, så mange per linje som får plass
		int buffSz = Math.max(12, (int) Math.round(20 * s));
		Font tiny = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(7, (int) Math.round(8 * s)));
		FontMetrics fmTiny = g.getFontMetrics(tiny);
		int buffGap = Math.max(1, (int) Math.round(2 * s));
		int perLine = Math.max(1, (barW + iconW + buffGap) / (buffSz + buffGap));
		int[] buffLines = new int[members.size()];
		int height = pad + titleH;
		for (int i = 0; i < members.size(); i++)
		{
			int n = buffIcons == null ? 0 : visibleBuffs(members.get(i), now, buffIcons).size();
			buffLines[i] = (n + perLine - 1) / perLine;
			height += rowH + buffLines[i] * (buffSz + buffGap);
		}

		g.setColor(BG);
		g.fillRoundRect(0, 0, width, height, (int) (8 * s), (int) (8 * s));
		g.setColor(color);
		g.fillRect(0, (int) (4 * s), Math.max(2, (int) Math.round(3 * s)), height - (int) (8 * s));

		g.setFont(title);
		g.setColor(color);
		g.drawString(heading, pad, pad + fmT.getAscent());

		int y = pad + titleH;
		for (int mi = 0; mi < members.size(); mi++)
		{
			TeamMember m = members.get(mi);
			int x = pad;
			g.setFont(name);
			g.setColor(Color.WHITE);
			g.drawString(m.name, x, y + fmN.getAscent());
			int by = y + fmN.getHeight() + gap;

			// Liv med tall
			double hpFrac = m.maxHp > 0 ? Math.max(0, Math.min(1, m.hp / (double) m.maxHp)) : 0;
			g.setColor(HP_BG);
			g.fillRect(x, by, barW, hpH);
			g.setColor(hpFrac < 0.3 ? new Color(0xFF, 0x30, 0x30) : HP);
			g.fillRect(x, by, (int) Math.round(barW * hpFrac), hpH);
			g.setFont(small);
			String hpText = m.hp + "/" + m.maxHp;
			g.setColor(Color.BLACK);
			g.drawString(hpText, x + (barW - fmS.stringWidth(hpText)) / 2 + 1, by + (hpH - fmS.getHeight()) / 2 + fmS.getAscent() + 1);
			g.setColor(Color.WHITE);
			g.drawString(hpText, x + (barW - fmS.stringWidth(hpText)) / 2, by + (hpH - fmS.getHeight()) / 2 + fmS.getAscent());
			by += hpH + gap;

			// Bønn (uten tekst)
			double prFrac = m.maxPrayer > 0 ? Math.max(0, Math.min(1, m.prayer / (double) m.maxPrayer)) : 0;
			g.setColor(PRAYER_BG);
			g.fillRect(x, by, barW, prH);
			g.setColor(PRAYER);
			g.fillRect(x, by, (int) Math.round(barW * prFrac), prH);
			by += prH + gap;

			// Spec-energi
			double spFrac = Math.max(0, Math.min(1, m.spec / 100.0));
			g.setColor(SPEC_BG);
			g.fillRect(x, by, barW, spH);
			g.setColor(SPEC);
			g.fillRect(x, by, (int) Math.round(barW * spFrac), spH);

			// Våpenikon som pulserer 5 ganger etter et spesialangrep
			long t = now - m.specAt;
			if (m.specWeapon > 0 && t >= 0 && t < SPEC_FLASH_MS && icons != null)
			{
				BufferedImage img = icons.get(m.specWeapon);
				if (img != null)
				{
					// Pulsen går mellom svak og full styrke – ikonet forsvinner aldri helt mellom pulsene
					float p = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * t / BLINK_PERIOD_MS));
					int size = iconW;
					int ix = pad * 2 + barW;
					int iy = y + (rowH - pad - size) / 2;
					float cx = ix + size / 2f;
					float cy = iy + size / 2f;
					// Myk gul glød bak ikonet, sterkest på toppen av pulsen
					float r = size * 0.85f;
					int glowA = (int) (70 + 120 * p);
					java.awt.Paint oldPaint = g.getPaint();
					g.setPaint(new java.awt.RadialGradientPaint(cx, cy, r, new float[]{0f, 0.55f, 1f},
						new Color[]{new Color(0xFF, 0xE0, 0x50, glowA), new Color(0xFF, 0xC8, 0x28, glowA / 2), new Color(0xFF, 0xC8, 0x28, 0)}));
					g.fillOval(Math.round(cx - r), Math.round(cy - r), Math.round(2 * r), Math.round(2 * r));
					g.setPaint(oldPaint);
					Composite old = g.getComposite();
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f + 0.55f * p));
					g.drawImage(img, ix, iy, size, size, null);
					g.setComposite(old);
				}
			}
			y += rowH;

			// Boosts og buffs med klokke
			if (buffLines[mi] > 0)
			{
				List<com.sweatnation.messages.BuffEntry> list = visibleBuffs(m, now, buffIcons);
				for (int k = 0; k < list.size(); k++)
				{
					int bx = pad + (k % perLine) * (buffSz + buffGap);
					int byy = y - pad + gap + (k / perLine) * (buffSz + buffGap);
					drawBuff(g, list.get(k), buffIcons.get(list.get(k).icon), bx, byy, buffSz,
						BuffTracker.fracNow(list.get(k), m.buffsAt, now), tiny, fmTiny);
				}
				y += buffLines[mi] * (buffSz + buffGap);
			}
		}
		return new Dimension(width, height);
	}

	private static List<com.sweatnation.messages.BuffEntry> visibleBuffs(TeamMember m, long now, BuffIconSource icons)
	{
		List<com.sweatnation.messages.BuffEntry> out = new java.util.ArrayList<>();
		List<com.sweatnation.messages.BuffEntry> all = m.buffs;
		if (all == null)
		{
			return out;
		}
		for (com.sweatnation.messages.BuffEntry e : all)
		{
			if (e != null && e.icon != null && icons.get(e.icon) != null && !BuffTracker.expired(e, m.buffsAt, now))
			{
				out.add(e);
			}
		}
		return out;
	}

	/**
	 * Ett buff-ikon. Klokken: det som har passert, er mørkt; tiden som er igjen, er lys.
	 * Den mørke delen vokser med klokka fra kl. 12.
	 */
	private static void drawBuff(Graphics2D g, com.sweatnation.messages.BuffEntry e, BufferedImage img, int x, int y, int size,
		float frac, Font font, FontMetrics fm)
	{
		g.setColor(new Color(0, 0, 0, 120));
		g.fillRoundRect(x, y, size, size, 4, 4);
		// Skarp nedskalering, med samme form som originalen
		double k = Math.min(size / (double) img.getWidth(), size / (double) img.getHeight());
		int w = Math.max(1, (int) Math.round(img.getWidth() * k));
		int h = Math.max(1, (int) Math.round(img.getHeight() * k));
		Object oldInterp = g.getRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(img, x + (size - w) / 2, y + (size - h) / 2, w, h, null);
		if (oldInterp != null)
		{
			g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, oldInterp);
		}
		if (frac >= 0 && frac < 1)
		{
			// Lys over tiden som er igjen, mørk over tiden som har passert (fra kl. 12, med klokka)
			int passed = Math.round(360 * (1 - frac));
			java.awt.Shape clip = g.getClip();
			g.clipRect(x, y, size, size);
			int r = (int) Math.ceil(size * 0.75);
			int cx = x + size / 2;
			int cy = y + size / 2;
			g.setColor(new Color(255, 255, 255, 45));
			g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 90 - passed, -(360 - passed));
			g.setColor(new Color(0, 0, 0, 175));
			g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 90, -passed);
			g.setClip(clip);
		}
		if (e.text != null && !e.text.isEmpty())
		{
			g.setFont(font);
			int tw = fm.stringWidth(e.text);
			int tx = x + size - tw - 1;
			int ty = y + size - 1;
			g.setColor(Color.BLACK);
			g.drawString(e.text, tx + 1, ty + 1);
			g.setColor(e.text.startsWith("+") ? new Color(0x50, 0xFF, 0x50) : Color.WHITE);
			g.drawString(e.text, tx, ty);
		}
	}
}
