package com.sweatnation;

/**
 * Flerspiller-aktiviteter man kan melde seg på.
 * minPlayers = antall påmeldte før en gruppe regnes som "klar" (anbefalt lagstørrelse),
 * maxPlayers = største lag aktiviteten tillater (0 = ingen grense i praksis).
 */
public enum Activity
{
	// Raids
	COX("Chambers of Xeric", Category.RAIDS, 3, 100, 1234, 3573, 0),
	COX_CM("Chambers of Xeric (CM)", Category.RAIDS, 3, 100, 1234, 3573, 0),
	TOB("Theatre of Blood", Category.RAIDS, 4, 5, 3646, 3219, 0),
	TOB_HM("Theatre of Blood (HM)", Category.RAIDS, 4, 5, 3646, 3219, 0),
	TOA("Tombs of Amascut", Category.RAIDS, 3, 8, 3357, 2713, 0),
	TOA_EXPERT("Tombs of Amascut (Expert)", Category.RAIDS, 3, 8, 3357, 2713, 0),

	// Bosser
	NEX("Nex", Category.BOSSES, 4, 0, 2916, 3746, 0),
	CORP("Corporeal Beast", Category.BOSSES, 3, 0, 3206, 3681, 0),
	NIGHTMARE("The Nightmare", Category.BOSSES, 5, 80, 3808, 9700, 0),
	ROYAL_TITANS("Royal Titans", Category.BOSSES, 2, 0, 2952, 9574, 0),
	YAMA("Yama", Category.BOSSES, 2, 2, 1435, 3669, 0),
	HUEYCOATL("The Hueycoatl", Category.BOSSES, 3, 0, 1509, 3290, 0),
	BANDOS("General Graardor", Category.BOSSES, 3, 0, 2916, 3746, 0),
	ARMADYL("Kree'arra", Category.BOSSES, 2, 0, 2916, 3746, 0),
	SARADOMIN("Commander Zilyana", Category.BOSSES, 2, 0, 2916, 3746, 0),
	ZAMORAK("K'ril Tsutsaroth", Category.BOSSES, 3, 0, 2916, 3746, 0),
	DKS("Dagannoth Kings", Category.BOSSES, 2, 0, 2521, 3740, 0),
	KQ("Kalphite Queen", Category.BOSSES, 2, 0, 3226, 3108, 0),
	KBD("King Black Dragon", Category.BOSSES, 2, 0, 3067, 10253, 0),
	GROTESQUE("Grotesque Guardians", Category.BOSSES, 2, 2, 3427, 3541, 0),
	SARACHNIS("Sarachnis", Category.BOSSES, 2, 0, 1650, 9987, 0),
	SCURRIUS("Scurrius", Category.BOSSES, 2, 0, 3237, 3458, 0),
	ZALCANO("Zalcano", Category.BOSSES, 3, 0, 3286, 6059, 0),

	// Wilderness
	CALLISTO("Callisto / Artio", Category.WILDERNESS, 2, 0, 3291, 3849, 0),
	VENENATIS("Venenatis / Spindel", Category.WILDERNESS, 2, 0, 3319, 3798, 0),
	VETION("Vet'ion / Calvar'ion", Category.WILDERNESS, 2, 0, 3219, 3788, 0),
	CHAOS_ELE("Chaos Elemental", Category.WILDERNESS, 2, 0, 3261, 3927, 0),
	REVENANTS("Revenant Caves", Category.WILDERNESS, 3, 0, 3069, 3740, 0),
	PK_TRIP("PK-tur", Category.WILDERNESS, 3, 0, 3087, 3521, 0),

	// Minispill / skilling
	WINTERTODT("Wintertodt", Category.MINIGAMES, 3, 0, 1630, 3963, 0),
	TEMPOROSS("Tempoross", Category.MINIGAMES, 3, 0, 3148, 2840, 0),
	GOTR("Guardians of the Rift", Category.MINIGAMES, 3, 0, 3614, 9477, 0),
	BA("Barbarian Assault", Category.MINIGAMES, 5, 5, 2531, 3577, 0),
	PEST_CONTROL("Pest Control", Category.MINIGAMES, 3, 25, 2658, 2663, 0),
	VOLCANIC_MINE("Volcanic Mine", Category.MINIGAMES, 4, 4, 3811, 3813, 0),
	CASTLE_WARS("Castle Wars", Category.MINIGAMES, 4, 0, 2442, 3093, 0),
	SOUL_WARS("Soul Wars", Category.MINIGAMES, 4, 0, 2018, 5929, 0),

	// Hjelp / sosialt
	QUEST_HELP("Quest-hjelp", Category.HELP, 2, 0),
	DIARY_HELP("Achievement diary", Category.HELP, 2, 0),
	CLUE_HUNT("Clue-jakt", Category.HELP, 2, 0),
	SKILLING("Skilling sammen", Category.HELP, 2, 0),
	CLAN_EVENT("Klan-event", Category.HELP, 5, 0),

	/** Egendefinert gruppe med eget navn og egen størrelse. */
	CUSTOM("Egendefinert", Category.HELP, 2, 0);

	public enum Category
	{
		ALL("Alle", "All"),
		RAIDS("Raids", "Raids"),
		BOSSES("Bosser", "Bosses"),
		WILDERNESS("Wildy", "Wildy"),
		MINIGAMES("Minispill", "Minigames"),
		HELP("Hjelp", "Help");

		private final String label;
		private final String labelEn;

		Category(String label, String labelEn)
		{
			this.label = label;
			this.labelEn = labelEn;
		}

		public String getLabel()
		{
			return L.t(label, labelEn);
		}
	}

	private final String displayName;
	private final Category category;
	private final int minPlayers;
	private final int maxPlayers;
	/** Inngangen der laget møtes. null = ingen fast inngang (Rohirrim! kaller da til deg). */
	private final net.runelite.api.coords.WorldPoint entrance;

	Activity(String displayName, Category category, int minPlayers, int maxPlayers)
	{
		this(displayName, category, minPlayers, maxPlayers, -1, -1, 0);
	}

	Activity(String displayName, Category category, int minPlayers, int maxPlayers, int x, int y, int plane)
	{
		this.displayName = displayName;
		this.category = category;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
		this.entrance = x < 0 ? null : new net.runelite.api.coords.WorldPoint(x, y, plane);
	}

	public net.runelite.api.coords.WorldPoint getEntrance()
	{
		return entrance;
	}

	/** Lagfarge, brukt på minikartet og i lag-HUD-en. */
	public java.awt.Color teamColor()
	{
		java.awt.Color[] palette = {
			new java.awt.Color(0xE6, 0x46, 0x3C), new java.awt.Color(0x46, 0x96, 0xFF), new java.awt.Color(0x37, 0xC8, 0x5A),
			new java.awt.Color(0xF0, 0xC8, 0x28), new java.awt.Color(0x28, 0xD2, 0xD2), new java.awt.Color(0xFF, 0x6E, 0xC8),
			new java.awt.Color(0xFF, 0x96, 0x28), new java.awt.Color(0xB4, 0x78, 0xFF)
		};
		return palette[ordinal() % palette.length];
	}

	/** Engelske navn der det norske er annerledes. */
	private static final java.util.Map<String, String> EN = java.util.Map.of(
		"PK-tur", "PK trip",
		"Quest-hjelp", "Quest help",
		"Clue-jakt", "Clue hunting",
		"Skilling sammen", "Skilling together",
		"Klan-event", "Clan event",
		"Egendefinert", "Custom");

	/** Vanlige forkortelser og kallenavn, så søket finner f.eks. «cox» og «graardor». */
	private static final java.util.Map<String, String> ALIASES = java.util.Map.ofEntries(
		java.util.Map.entry("COX", "cox cm raids 1 olm"),
		java.util.Map.entry("COX_CM", "cox cm raids 1 olm challenge mode"),
		java.util.Map.entry("TOB", "tob raids 2 verzik"),
		java.util.Map.entry("TOB_HM", "tob hm hmt raids 2 verzik hard mode"),
		java.util.Map.entry("TOA", "toa raids 3 wardens"),
		java.util.Map.entry("TOA_EXPERT", "toa raids 3 wardens expert"),
		java.util.Map.entry("CORP", "corp"),
		java.util.Map.entry("NIGHTMARE", "nm pnm phosani"),
		java.util.Map.entry("BANDOS", "bandos gwd god wars"),
		java.util.Map.entry("ARMADYL", "arma armadyl gwd god wars"),
		java.util.Map.entry("SARADOMIN", "sara saradomin gwd god wars"),
		java.util.Map.entry("ZAMORAK", "zammy zamorak gwd god wars"),
		java.util.Map.entry("NEX", "gwd god wars"),
		java.util.Map.entry("DKS", "dks rex prime supreme"),
		java.util.Map.entry("KQ", "kq"),
		java.util.Map.entry("KBD", "kbd"),
		java.util.Map.entry("GROTESQUE", "gg dusk dawn"),
		java.util.Map.entry("CHAOS_ELE", "chaos ele"),
		java.util.Map.entry("REVENANTS", "revs"),
		java.util.Map.entry("PK_TRIP", "pk pking wildy"),
		java.util.Map.entry("WINTERTODT", "wt todt"),
		java.util.Map.entry("GOTR", "gotr rc runecraft"),
		java.util.Map.entry("BA", "ba"),
		java.util.Map.entry("PEST_CONTROL", "pc"),
		java.util.Map.entry("VOLCANIC_MINE", "vm"),
		java.util.Map.entry("CASTLE_WARS", "cw"),
		java.util.Map.entry("SOUL_WARS", "sw"),
		java.util.Map.entry("DIARY_HELP", "diary dagbok"),
		java.util.Map.entry("CLUE_HUNT", "clue scroll"));

	/** Passer aktiviteten søket? Sjekker norsk og engelsk navn, kategori og vanlige forkortelser. */
	public boolean matches(String query)
	{
		String q = query == null ? "" : query.trim().toLowerCase();
		if (q.isEmpty())
		{
			return true;
		}
		String all = (displayName + " " + EN.getOrDefault(displayName, "") + " " + category.getLabel() + " "
			+ ALIASES.getOrDefault(name(), "")).toLowerCase();
		for (String word : q.split("\\s+"))
		{
			if (!all.contains(word))
			{
				return false;
			}
		}
		return true;
	}

	public String getDisplayName()
	{
		return L.isEnglish() ? EN.getOrDefault(displayName, displayName) : displayName;
	}

	public Category getCategory()
	{
		return category;
	}

	public int getMinPlayers()
	{
		return minPlayers;
	}

	public int getMaxPlayers()
	{
		return maxPlayers;
	}

	public String teamSizeText()
	{
		if (maxPlayers == 0 || maxPlayers > 10)
		{
			return minPlayers + "+";
		}
		if (maxPlayers == minPlayers)
		{
			return String.valueOf(minPlayers);
		}
		return minPlayers + "–" + maxPlayers;
	}

	/** Kortnavn brukt i Discord-kanalnavn, f.eks. "nex" eller "theatre-of-blood-hm". */
	public String slug()
	{
		return displayName.toLowerCase()
			.replace("'", "")
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-|-$)", "");
	}

	public static Activity fromId(String id)
	{
		try
		{
			return Activity.valueOf(id);
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return null;
		}
	}
}
