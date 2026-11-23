package com.sweatnation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finner de raskeste måtene å komme seg til Rohirrim!-punktet på: teleport-gjenstander
 * du har (i inventory, utstyr eller banken), trylleformler, fairy rings, spirit trees
 * og minispill-teleporter.
 *
 * Transportdataene kommer fra Shortest Path-pluginen (github.com/Skretzo/shortest-path),
 * BSD 2-Clause-lisens. Se teleports.tsv.
 */
final class Teleports
{
	private static final Logger log = LoggerFactory.getLogger(Teleports.class);
	/** Ignorer muligheter som lander lenger unna enn dette. */
	private static final int MAX_DISTANCE = 200;
	private static final int MAX_LINES = 4;

	enum Type { ITEM, SPELL, MINIGAME, FAIRY, SPIRIT }

	static final class Option
	{
		final Type type;
		final WorldPoint point;
		final String label;
		final int[][] itemGroups;
		final int magicLevel;
		final int duration;
		final String extra;
		/** Runer for trylleformler: navn (f.eks. LAW_RUNE) -> antall, i rekkefølgen fra dataene. */
		final Map<String, Integer> runes;

		Option(Type type, WorldPoint point, String label, int[][] itemGroups, int magicLevel, int duration, String extra,
			Map<String, Integer> runes)
		{
			this.runes = runes;
			this.type = type;
			this.point = point;
			this.label = label;
			this.itemGroups = itemGroups;
			this.magicLevel = magicLevel;
			this.duration = duration;
			this.extra = extra;
		}
	}

	/** Én anbefaling som vises i panelet. */
	static final class Choice
	{
		final Option option;
		final int distance;
		final String where;
		final boolean owned;
		/** Trylleformler: runer du mangler (navn -> antall) og runer staven/tomen din gir. */
		Map<String, Integer> missing = Collections.emptyMap();
		Set<String> fromStaff = Collections.emptySet();
		Set<Integer> runeIds = Collections.emptySet();

		Choice(Option option, int distance, String where, boolean owned)
		{
			this.option = option;
			this.distance = distance;
			this.where = where;
			this.owned = owned;
		}
	}

	static final class Advice
	{
		final List<Choice> choices;
		final Set<Integer> highlightIds;

		Advice(List<Choice> choices, Set<Integer> highlightIds)
		{
			this.choices = choices;
			this.highlightIds = highlightIds;
		}
	}

	private static List<Option> options;

	private Teleports()
	{
	}

	private static synchronized List<Option> load()
	{
		if (options != null)
		{
			return options;
		}
		List<Option> list = new ArrayList<>();
		try (InputStream in = Teleports.class.getResourceAsStream("teleports.tsv"))
		{
			if (in != null)
			{
				BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String line;
				while ((line = r.readLine()) != null)
				{
					if (line.isEmpty() || line.startsWith("#"))
					{
						continue;
					}
					String[] c = line.split("\t", -1);
					if (c.length < 9)
					{
						continue;
					}
					try
					{
						list.add(new Option(Type.valueOf(c[0]),
							new WorldPoint(Integer.parseInt(c[1]), Integer.parseInt(c[2]), Integer.parseInt(c[3])),
							c[4], parseGroups(c[5]), Integer.parseInt(c[6]), Integer.parseInt(c[7]), c[8],
							parseRunes(c.length > 9 ? c[9] : "")));
					}
					catch (RuntimeException e)
					{
						// hopp over en ødelagt linje
					}
				}
			}
		}
		catch (Exception e)
		{
			log.warn("SWEAT NATION: kunne ikke lese teleports.tsv", e);
		}
		options = list;
		return list;
	}

	private static int[][] parseGroups(String s)
	{
		if (s == null || s.isEmpty())
		{
			return new int[0][];
		}
		String[] groups = s.split(",");
		int[][] out = new int[groups.length][];
		for (int g = 0; g < groups.length; g++)
		{
			String[] ids = groups[g].split("\\|");
			out[g] = new int[ids.length];
			for (int i = 0; i < ids.length; i++)
			{
				out[g][i] = Integer.parseInt(ids[i].trim());
			}
		}
		return out;
	}


	// ------------------------------------------------------------------ runer

	/** Det spilleren har til å kaste trylleformler med. */
	static final class Stock
	{
		/** Beholder ("inventory", "banken") -> gjenstands-ID -> antall. */
		final Map<String, Map<Integer, Integer>> counts;
		/** Runer i rune pouch: ID -> antall. */
		final Map<Integer, Integer> pouch;
		/** Runer staven eller tomen du har på deg gir uendelig av (f.eks. FIRE_RUNE). */
		final Set<String> infinite;
		/** Spellbook du står på: standard, ancient, lunar, arceuus (null = ukjent). */
		final String spellbook;

		Stock(Map<String, Map<Integer, Integer>> counts, Map<Integer, Integer> pouch, Set<String> infinite, String spellbook)
		{
			this.counts = counts;
			this.pouch = pouch;
			this.infinite = infinite;
			this.spellbook = spellbook;
		}
	}

	static final Map<String, Integer> RUNE_IDS = new java.util.HashMap<>();
	static final Map<String, String> RUNE_NAMES = new java.util.HashMap<>();
	/** Kombinasjonsruner: ID -> hvilke elementer de teller som. */
	static final Map<Integer, String[]> COMBO_RUNES = new java.util.HashMap<>();
	/** Staver og tomes: ID -> elementrunene de gir uendelig av. */
	static final Map<Integer, String[]> STAVES = new java.util.HashMap<>();
	static final int[] RUNE_POUCHES = {12791, 27281};

	static
	{
		rune("AIR_RUNE", 556, "Air");
		rune("WATER_RUNE", 555, "Water");
		rune("EARTH_RUNE", 557, "Earth");
		rune("FIRE_RUNE", 554, "Fire");
		rune("MIND_RUNE", 558, "Mind");
		rune("BODY_RUNE", 559, "Body");
		rune("DEATH_RUNE", 560, "Death");
		rune("NATURE_RUNE", 561, "Nature");
		rune("CHAOS_RUNE", 562, "Chaos");
		rune("LAW_RUNE", 563, "Law");
		rune("COSMIC_RUNE", 564, "Cosmic");
		rune("BLOOD_RUNE", 565, "Blood");
		rune("SOUL_RUNE", 566, "Soul");
		rune("ASTRAL_RUNE", 9075, "Astral");
		rune("WRATH_RUNE", 21880, "Wrath");
		rune("BANANA", 1963, "Banana");

		String a = "AIR_RUNE", w = "WATER_RUNE", e = "EARTH_RUNE", f = "FIRE_RUNE";
		COMBO_RUNES.put(4694, new String[]{w, f}); // steam
		COMBO_RUNES.put(4695, new String[]{a, w}); // mist
		COMBO_RUNES.put(4696, new String[]{a, e}); // dust
		COMBO_RUNES.put(4697, new String[]{a, f}); // smoke
		COMBO_RUNES.put(4698, new String[]{w, e}); // mud
		COMBO_RUNES.put(4699, new String[]{e, f}); // lava

		staff(new String[]{a}, 1381, 1397, 1405);
		staff(new String[]{w}, 1383, 1395, 1403, 21006, 11791, 12904, 22296, 25574);
		staff(new String[]{e}, 1385, 1399, 1407);
		staff(new String[]{f}, 1387, 1393, 1401, 20714);
		staff(new String[]{e, f}, 3053, 3054, 21198, 21200); // lava
		staff(new String[]{w, e}, 6562, 6563); // mud
		staff(new String[]{w, f}, 11787, 11789, 12795, 12796); // steam
		staff(new String[]{a, f}, 11998, 12000); // smoke
		staff(new String[]{a, w}, 20730, 20733); // mist
		staff(new String[]{a, e}, 20736, 20739); // dust
	}

	private static void rune(String key, int id, String name)
	{
		RUNE_IDS.put(key, id);
		RUNE_NAMES.put(key, name);
	}

	private static void staff(String[] gives, int... ids)
	{
		for (int id : ids)
		{
			STAVES.put(id, gives);
		}
	}

	private static Map<String, Integer> parseRunes(String s)
	{
		Map<String, Integer> out = new java.util.LinkedHashMap<>();
		if (s == null || s.isEmpty())
		{
			return out;
		}
		for (String part : s.split("&&"))
		{
			String[] kv = part.trim().split("=");
			if (kv.length == 2)
			{
				out.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
			}
		}
		return out;
	}

	/** Resultatet av å sjekke runene for én trylleformel mot det du har. */
	private static final class SpellCheck
	{
		final Map<String, Integer> missing = new java.util.LinkedHashMap<>();
		final Set<String> fromStaff = new java.util.LinkedHashSet<>();
		final Set<Integer> used = new HashSet<>();
	}

	/** Kan trylleformelen kastes med runene i pool (gjenstands-ID -> antall)? Bruker vanlige runer først, så kombinasjonsruner. */
	private static SpellCheck checkRunes(Option o, Map<Integer, Integer> pool, Set<String> infinite)
	{
		SpellCheck r = new SpellCheck();
		Map<Integer, Integer> left = new java.util.HashMap<>(pool);
		for (Map.Entry<String, Integer> need : o.runes.entrySet())
		{
			String rune = need.getKey();
			int q = need.getValue();
			if (infinite.contains(rune))
			{
				r.fromStaff.add(rune);
				continue;
			}
			Integer id = RUNE_IDS.get(rune);
			if (id == null)
			{
				continue; // ukjent krav – ignorer
			}
			q = take(left, id, q, r);
			for (Map.Entry<Integer, String[]> combo : COMBO_RUNES.entrySet())
			{
				if (q > 0 && java.util.Arrays.asList(combo.getValue()).contains(rune))
				{
					q = take(left, combo.getKey(), q, r);
				}
			}
			if (q > 0)
			{
				r.missing.put(rune, q);
			}
		}
		return r;
	}

	private static int take(Map<Integer, Integer> left, int id, int q, SpellCheck r)
	{
		int have = left.getOrDefault(id, 0);
		int use = Math.min(have, q);
		if (use > 0)
		{
			left.put(id, have - use);
			r.used.add(id);
		}
		return q - use;
	}

	private static Map<Integer, Integer> pool(Stock stock, boolean withBank)
	{
		Map<Integer, Integer> p = new java.util.HashMap<>();
		add(p, stock.counts.get("inventory"));
		add(p, stock.pouch);
		if (withBank)
		{
			add(p, stock.counts.get("banken"));
		}
		return p;
	}

	private static void add(Map<Integer, Integer> into, Map<Integer, Integer> from)
	{
		if (from != null)
		{
			from.forEach((id, n) -> into.merge(id, n, Integer::sum));
		}
	}

	/** Vurderer en trylleformel: kan kastes fra inventory, etter en tur i banken, eller mangler runer. */
	private static Choice spellChoice(Option o, int distance, Stock stock)
	{
		SpellCheck inv = checkRunes(o, pool(stock, false), stock.infinite);
		SpellCheck check = inv;
		String where = inv.missing.isEmpty() ? "inventory" : null;
		if (where == null)
		{
			SpellCheck bank = checkRunes(o, pool(stock, true), stock.infinite);
			check = bank;
			if (bank.missing.isEmpty())
			{
				where = "banken";
			}
		}
		Choice c = new Choice(o, distance, where, where != null);
		c.missing = check.missing;
		c.fromStaff = check.fromStaff;
		Set<Integer> ids = new HashSet<>(check.used);
		for (int id : check.used)
		{
			if (stock.pouch.containsKey(id))
			{
				for (int pouch : RUNE_POUCHES)
				{
					ids.add(pouch);
				}
				break;
			}
		}
		c.runeIds = ids;
		return c;
	}

	/** «2 Law, 1 Fire (stav)» */
	private static String runeText(Choice c)
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> e : c.option.runes.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			String name = RUNE_NAMES.getOrDefault(e.getKey(), e.getKey());
			if (c.fromStaff.contains(e.getKey()))
			{
				sb.append(name).append(L.t(" (stav)", " (staff)"));
			}
			else
			{
				sb.append(e.getValue()).append(' ').append(name);
			}
		}
		return sb.toString();
	}

	private static String missingText(Choice c)
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> e : c.missing.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(e.getValue()).append(' ').append(RUNE_NAMES.getOrDefault(e.getKey(), e.getKey()));
		}
		return sb.toString();
	}

	/**
	 * @param owned beholder ("inventory", "utstyr", "banken") → gjenstands-IDer du har der
	 * @param magicLevel ditt Magic-nivå (0 = ukjent)
	 */
	static Advice advise(WorldPoint target, Map<String, Set<Integer>> owned, Stock stock, int magicLevel)
	{
		if (target == null)
		{
			return null;
		}
		// Beste valg per kategori; gjenstander deles i «har» og «har ikke»
		Choice[] best = new Choice[Type.values().length + 1];
		double[] bestScore = new double[best.length];
		java.util.Arrays.fill(bestScore, Double.MAX_VALUE);

		for (Option o : load())
		{
			int d = o.point.distanceTo2D(target);
			if (o.point.getPlane() != target.getPlane())
			{
				d += 30;
			}
			if (d > MAX_DISTANCE)
			{
				continue;
			}
			if (o.type == Type.SPELL && magicLevel > 0 && o.magicLevel > magicLevel)
			{
				continue; // for lavt Magic-nivå
			}
			if (o.type == Type.SPELL && stock.spellbook != null && !stock.spellbook.equals(o.extra))
			{
				continue; // en annen spellbook enn den du står på
			}
			Choice choice;
			int slot = o.type.ordinal();
			if (o.type == Type.ITEM)
			{
				String where = whereOwned(o, owned);
				choice = new Choice(o, d, where, where != null);
			}
			else if (o.type == Type.SPELL)
			{
				choice = spellChoice(o, d, stock);
			}
			else
			{
				choice = new Choice(o, d, null, true);
			}
			if (!choice.owned)
			{
				slot = Type.values().length; // «har ikke»-plassen
			}
			double score = score(o, d);
			if (score < bestScore[slot])
			{
				bestScore[slot] = score;
				best[slot] = choice;
			}
		}

		List<double[]> order = new ArrayList<>();
		for (int i = 0; i < best.length; i++)
		{
			if (best[i] != null)
			{
				order.add(new double[]{bestScore[i], i});
			}
		}
		// Vis en gjenstand du ikke har bare hvis den slår alt du faktisk kan bruke
		int notOwned = Type.values().length;
		double bestUsable = Double.MAX_VALUE;
		for (double[] e : order)
		{
			if ((int) e[1] != notOwned)
			{
				bestUsable = Math.min(bestUsable, e[0]);
			}
		}
		order.sort((a, b) -> Double.compare(a[0], b[0]));
		List<Choice> choices = new ArrayList<>();
		Set<Integer> highlight = new HashSet<>();
		for (double[] e : order)
		{
			int slot = (int) e[1];
			if (slot == notOwned && e[0] >= bestUsable)
			{
				continue;
			}
			Choice c = best[slot];
			choices.add(c);
			highlight.addAll(c.runeIds); // runene trylleformelen trenger, også om noen mangler
			if (c.owned && c.option.type == Type.ITEM)
			{
				for (int[] g : c.option.itemGroups)
				{
					for (int id : g)
					{
						highlight.add(id);
					}
				}
			}
			if (choices.size() >= MAX_LINES)
			{
				break;
			}
		}
		return new Advice(choices, Collections.unmodifiableSet(highlight));
	}

	/** Omtrentlig tid i ticks: kastetid + gåing (løping ≈ 2 ruter per tick) + tid for å komme seg til ringen/treet. */
	private static double score(Option o, int distance)
	{
		double extra = 0;
		if (o.type == Type.FAIRY || o.type == Type.SPIRIT)
		{
			extra = 30;
		}
		return o.duration + distance / 2.0 + extra;
	}

	private static String whereOwned(Option o, Map<String, Set<Integer>> owned)
	{
		String result = null;
		for (String where : new String[]{"inventory", "utstyr", "banken"})
		{
			Set<Integer> ids = owned.get(where);
			if (ids == null || ids.isEmpty())
			{
				continue;
			}
			boolean all = o.itemGroups.length > 0;
			for (int[] g : o.itemGroups)
			{
				boolean any = false;
				for (int id : g)
				{
					if (ids.contains(id))
					{
						any = true;
						break;
					}
				}
				if (!any)
				{
					all = false;
					break;
				}
			}
			if (all)
			{
				return where;
			}
		}
		return result;
	}

	/** Teksten til én linje, f.eks. «Ghommal's hilt → God Wars Dungeon · 4 ruter · i banken». */
	static String describe(Choice c)
	{
		Option o = c.option;
		String name;
		switch (o.type)
		{
			case ITEM:
			{
				int i = o.label.indexOf(':');
				String item = i > 0 ? o.label.substring(0, i).trim() : o.label;
				String dest = i > 0 ? o.label.substring(i + 1).trim() : "";
				if (dest.isEmpty() || dest.equalsIgnoreCase("Teleport"))
				{
					dest = Places.describe(o.point);
				}
				name = item + " → " + dest;
				break;
			}
			case SPELL:
				name = o.label + (o.extra.isEmpty() ? "" : " (" + o.extra + ")");
				break;
			case FAIRY:
				name = "Fairy ring " + o.label + " → " + Places.describe(o.point);
				break;
			case SPIRIT:
				name = "Spirit tree → " + o.label;
				break;
			case MINIGAME:
				name = L.t("Minispill-teleport: ", "Minigame teleport: ") + o.label;
				break;
			default:
				name = o.label;
		}
		String tail = " · " + (c.distance <= 1 ? L.t("rett ved", "right there") : c.distance + L.t(" ruter", " tiles"));
		if (o.type == Type.ITEM)
		{
			tail += c.owned ? L.t(" · i ", " · in ") + whereLabel(c.where) : L.t(" · har ikke", " · not owned");
		}
		else if (o.type == Type.SPELL && !o.runes.isEmpty())
		{
			tail += " · " + runeText(c);
			tail += c.owned ? (c.where.equals("banken") ? L.t(" · runer i banken", " · runes in the bank") : "")
				: L.t(" · mangler ", " · missing ") + missingText(c);
		}
		return name + tail;
	}

	/** Beholdernavnene er norske nøkler internt; vis dem på valgt språk. */
	private static String whereLabel(String where)
	{
		switch (where)
		{
			case "utstyr": return L.t("utstyr", "equipment");
			case "banken": return L.t("banken", "the bank");
			default: return "inventory";
		}
	}

	static List<String> describe(Advice a)
	{
		List<String> lines = new ArrayList<>();
		if (a == null)
		{
			return lines;
		}
		if (a.choices.isEmpty())
		{
			lines.add(L.t("Ingen kjent teleport lander i nærheten – gå eller løp.", "No known teleport lands nearby – walk or run."));
			return lines;
		}
		for (Choice c : a.choices)
		{
			lines.add(describe(c));
		}
		return lines;
	}
}
