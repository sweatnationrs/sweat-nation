package com.sweatnation;

import com.sweatnation.messages.BuffEntry;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;

/**
 * Samler dine egne stat boosts og buffs med tid, og holder styr på ikonene.
 * Boosts leses fra skill-nivåene. Buffs med tid leses fra infoboksene til RuneLite sin
 * Timers &amp; Buffs-plugin, så ikonene er de samme som i spillet.
 */
final class BuffTracker
{
	/** Ikonene lagres og sendes i opptil 32 px, så de er skarpe når HUD-en skalerer dem ned. */
	static final int ICON_PX = 32;

	/** Alle ikoner vi kjenner: id -> bilde (egne og mottatte). */
	final Map<String, BufferedImage> icons = new ConcurrentHashMap<>();
	/** Høyeste boost sett siden den startet, så klokken kan vise hvor mye som er igjen. */
	private final Map<Skill, Integer> peak = new EnumMap<>(Skill.class);
	/** For bokser uten egen klokke (bare tekst som «2:09»): lengste tid sett, brukt som full tid. */
	private final Map<String, Long> textTimerTotal = new ConcurrentHashMap<>();

	/** Les boosts og buffs nå. Kalles på klienttråden. */
	List<BuffEntry> collect(Client client, SkillIconManager skillIcons, InfoBoxManager infoBoxes)
	{
		List<BuffEntry> out = new ArrayList<>();
		for (Skill sk : Skill.values())
		{
			if (sk == Skill.HITPOINTS || sk == Skill.PRAYER || "OVERALL".equals(sk.name()))
			{
				continue; // liv og bønn vises allerede som barer
			}
			int boost = client.getBoostedSkillLevel(sk) - client.getRealSkillLevel(sk);
			if (boost <= 0)
			{
				peak.remove(sk);
				continue;
			}
			int top = Math.max(boost, peak.getOrDefault(sk, 0));
			peak.put(sk, top);
			String id = "skill:" + sk.name();
			if (!icons.containsKey(id))
			{
				BufferedImage img = skillIcons.getSkillImage(sk, false);
				if (img == null)
				{
					continue;
				}
				icons.put(id, scale(img));
			}
			BuffEntry e = new BuffEntry();
			e.icon = id;
			e.text = "+" + boost;
			e.frac = boost / (float) top;
			out.add(e);
		}

		Instant now = Instant.now();
		java.util.Set<String> seenText = new java.util.HashSet<>();
		for (InfoBox box : infoBoxes.getInfoBoxes())
		{
			if (!fromTimersPlugin(box))
			{
				continue; // bare Timers & Buffs (ikke boss-timere o.l. fra andre plugins)
			}
			BufferedImage img = box.getImage();
			if (img == null || img.getWidth() <= 1)
			{
				continue; // ikonet er ikke lastet ennå
			}
			BufferedImage big = scale(img);
			BuffEntry e = new BuffEntry();
			e.icon = iconId(big);
			if (box instanceof Timer && ((Timer) box).getEndTime() != null)
			{
				Timer t = (Timer) box;
				long left = Duration.between(now, t.getEndTime()).toMillis();
				long total = t.getDuration() == null ? 0 : t.getDuration().toMillis();
				if (left <= 0)
				{
					continue;
				}
				e.remainingMs = left;
				e.totalMs = Math.max(total, left);
			}
			else
			{
				// Mange buffs (f.eks. Mark of Darkness) er bokser med tiden som tekst: «2:09», «45s»
				String text = box.getText();
				long left = parseTime(text);
				if (left > 0)
				{
					long total = textTimerTotal.getOrDefault(e.icon, 0L);
					if (left > total + 2000 || total == 0)
					{
						total = left; // ny eller fornyet buff
					}
					textTimerTotal.put(e.icon, total);
					seenText.add(e.icon);
					e.remainingMs = left;
					e.totalMs = total;
				}
				else
				{
					e.text = text == null || text.isEmpty() ? null : text;
				}
			}
			icons.putIfAbsent(e.icon, big);
			out.add(e);
		}
		textTimerTotal.keySet().retainAll(seenText);
		return out;
	}

	/** Er infoboksen fra RuneLite sin Timers &amp; Buffs-plugin? Sjekker pakken til boksens klasse (og navnet som reserve). */
	static boolean fromTimersPlugin(InfoBox box)
	{
		String cls = box.getClass().getName().toLowerCase();
		if (cls.contains(".plugins.timers"))
		{
			return true; // net.runelite.client.plugins.timers / timersandbuffs
		}
		String name = box.getName();
		return name != null && name.startsWith("Timers");
	}

	/** «2:09» -> 129 000 ms, «45s» / «45» -> 45 000 ms, «1m» -> 60 000 ms. Ellers 0. */
	static long parseTime(String text)
	{
		if (text == null)
		{
			return 0;
		}
		String t = text.trim().toLowerCase();
		try
		{
			if (t.matches("\\d{1,2}:\\d{2}"))
			{
				String[] p = t.split(":");
				return (Integer.parseInt(p[0]) * 60L + Integer.parseInt(p[1])) * 1000L;
			}
			if (t.matches("\\d{1,3}s"))
			{
				return Integer.parseInt(t.substring(0, t.length() - 1)) * 1000L;
			}
			if (t.matches("\\d{1,2}m"))
			{
				return Integer.parseInt(t.substring(0, t.length() - 1)) * 60_000L;
			}
		}
		catch (NumberFormatException ignored)
		{
		}
		return 0;
	}

	/** Andel igjen (0–1) akkurat nå, eller -1 hvis buffen ikke har klokke. */
	static float fracNow(BuffEntry e, long receivedAt, long now)
	{
		if (e.totalMs > 0)
		{
			long left = e.remainingMs - (now - receivedAt);
			return Math.max(0f, Math.min(1f, left / (float) e.totalMs));
		}
		return e.frac;
	}

	/** Er buffen utløpt (bare buffs med tid)? */
	static boolean expired(BuffEntry e, long receivedAt, long now)
	{
		return e.totalMs > 0 && e.remainingMs - (now - receivedAt) <= 0;
	}

	/** Skalerer ned til maks 32 px (beholder formen). Mindre bilder beholdes som de er. */
	static BufferedImage scale(BufferedImage src)
	{
		double k = Math.min(1.0, Math.min(ICON_PX / (double) src.getWidth(), ICON_PX / (double) src.getHeight()));
		int w = Math.max(1, (int) Math.round(src.getWidth() * k));
		int h = Math.max(1, (int) Math.round(src.getHeight() * k));
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return out;
	}

	static String iconId(BufferedImage img)
	{
		int[] px = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
		return "img:" + Integer.toHexString(Arrays.hashCode(px));
	}

	static String encode(BufferedImage img)
	{
		try
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(img, "png", bos);
			return Base64.getEncoder().encodeToString(bos.toByteArray());
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	static BufferedImage decode(String png)
	{
		if (png == null || png.length() > 40_000)
		{
			return null; // ikonene er små – noe større er ikke et ikon
		}
		try
		{
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(png)));
			return img == null ? null : scale(img);
		}
		catch (Exception ex)
		{
			return null;
		}
	}
}
