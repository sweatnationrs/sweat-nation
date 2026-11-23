package com.sweatnation;

import net.runelite.api.coords.WorldPoint;

/** Gjør koordinater om til stedsnavn i Gielinor, f.eks. «ved Grand Exchange» eller «Wilderness (lvl 23)». */
final class Places
{
	private static final class Place
	{
		final String name;
		final int x;
		final int y;

		Place(String name, int x, int y)
		{
			this.name = name;
			this.x = x;
			this.y = y;
		}
	}

	// Omtrentlige midtpunkter for kjente steder
	private static final Place[] PLACES = {
		new Place("Lumbridge", 3222, 3218), new Place("Lumbridge Swamp", 3200, 3170),
		new Place("Draynor Village", 3093, 3244), new Place("Draynor Manor", 3109, 3350),
		new Place("Wizards' Tower", 3109, 3162), new Place("Port Sarim", 3023, 3208),
		new Place("Rimmington", 2957, 3214), new Place("Falador", 2965, 3380),
		new Place("Falador Park", 2996, 3375), new Place("Taverley", 2895, 3455),
		new Place("Burthorpe", 2899, 3544), new Place("Warriors' Guild", 2865, 3546),
		new Place("Death Plateau", 2860, 3590), new Place("Trollheim", 2891, 3680),
		new Place("God Wars Dungeon", 2916, 3746), new Place("Varrock", 3213, 3424),
		new Place("Grand Exchange", 3164, 3486), new Place("Barbarian Village", 3082, 3420),
		new Place("Edgeville", 3093, 3493), new Place("Champions' Guild", 3191, 3363),
		new Place("Lumber Yard", 3307, 3491), new Place("Digsite", 3341, 3445),
		new Place("Al Kharid", 3293, 3174), new Place("Emir's Arena", 3315, 3235),
		new Place("Shantay Pass", 3304, 3117), new Place("Pollnivneach", 3359, 2972),
		new Place("Nardah", 3428, 2892), new Place("Sophanem", 3296, 2780),
		new Place("Kalphite Lair", 3226, 3108), new Place("Bandit Camp (desert)", 3175, 2985),
		new Place("Canifis", 3494, 3483), new Place("Slayer Tower", 3428, 3537),
		new Place("Port Phasmatys", 3687, 3502), new Place("Burgh de Rott", 3497, 3230),
		new Place("Ver Sinhaza", 3649, 3230), new Place("Darkmeyer", 3592, 3337),
		new Place("Fossil Island", 3724, 3808), new Place("Seers' Village", 2726, 3485),
		new Place("Camelot", 2757, 3478), new Place("Catherby", 2809, 3435),
		new Place("Fishing Guild", 2611, 3390), new Place("Ardougne", 2662, 3305),
		new Place("Ardougne Monastery", 2606, 3222), new Place("Witchaven", 2721, 3283),
		new Place("Yanille", 2606, 3093), new Place("Castle Wars", 2442, 3089),
		new Place("Tree Gnome Stronghold", 2461, 3444), new Place("Tree Gnome Village", 2526, 3167),
		new Place("Barbarian Assault", 2520, 3570), new Place("Rellekka", 2660, 3657),
		new Place("Waterbirth Island", 2525, 3740), new Place("Miscellania", 2535, 3862),
		new Place("Neitiznot", 2335, 3800), new Place("Lunar Isle", 2100, 3914),
		new Place("Karamja", 2918, 3176), new Place("Brimhaven", 2760, 3178),
		new Place("Shilo Village", 2852, 2955), new Place("TzHaar", 2480, 5170),
		new Place("Ape Atoll", 2755, 2784), new Place("Pest Control", 2659, 2660),
		new Place("Tempoross", 3135, 2840), new Place("Corsair Cove", 2570, 2860),
		new Place("Wintertodt", 1630, 3940), new Place("Kourend Castle", 1639, 3673),
		new Place("Hosidius", 1744, 3517), new Place("Shayzien", 1504, 3622),
		new Place("Lovakengj", 1504, 3817), new Place("Arceuus", 1696, 3758),
		new Place("Port Piscarilius", 1803, 3748), new Place("Mount Karuulm", 1311, 3807),
		new Place("Farming Guild", 1249, 3718), new Place("Kourend Woodland", 1550, 3449),
		new Place("Civitas illa Fortis", 1680, 3133), new Place("Fortis Colosseum", 1796, 3107),
		new Place("Varlamore", 1600, 3000), new Place("Prifddinas", 3264, 6079),
		new Place("Zanaris", 2412, 4434), new Place("Ferox Enclave", 3130, 3631),
		new Place("Corporeal Beast", 3206, 3681),
		new Place("Zul-Andra", 2200, 3056), new Place("Lletya", 2330, 3170),
		new Place("Port Khazard", 2660, 3160), new Place("Feldip Hills", 2560, 2960),
		new Place("Castle Wars", 2442, 3089), new Place("Mos Le'Harmless", 3680, 2970),
		new Place("Keldagrim", 2870, 10200), new Place("Mort'ton", 3490, 3290),
		new Place("Ectofuntus", 3660, 3522), new Place("Barrows", 3565, 3288),
		new Place("Kebos Lowlands", 1300, 3640), new Place("Chambers of Xeric", 1233, 3567),
		new Place("Theatre of Blood", 3650, 3219), new Place("Tombs of Amascut", 3355, 2733),
		new Place("Nightmare Zone", 2609, 3115), new Place("Wizards' Guild", 2590, 3087),
		new Place("Gnome Glider (Karamja)", 2971, 2970), new Place("Tai Bwo Wannai", 2789, 3065),
		new Place("Musa Point", 2918, 3176), new Place("Entrana", 2834, 3335),
		new Place("Crafting Guild", 2933, 3290), new Place("Mining Guild", 3046, 3337),
		new Place("Ice Mountain", 3007, 3477), new Place("Goblin Village", 2956, 3505),
		new Place("Eagles' Peak", 2330, 3500), new Place("Piscatoris", 2340, 3650),
		new Place("Jatizso", 2410, 3800), new Place("Weiss", 2860, 3930),
		new Place("Ghorrock", 2970, 3870), new Place("Darkfrost", 1450, 3260),
		new Place("Aldarin", 1390, 2930), new Place("Sunset Coast", 1530, 2980),
		new Place("Avium Savannah", 1580, 3100), new Place("Cam Torum", 1440, 3120),
		new Place("Hunter Guild", 1560, 3045), new Place("Tal Teklan", 1240, 3110),
	};

	private Places()
	{
	}

	static String describe(WorldPoint p)
	{
		return describe(p, L.isEnglish());
	}

	static String describe(WorldPoint p, boolean en)
	{
		if (p == null)
		{
			return en ? "unknown place" : "ukjent sted";
		}
		int x = p.getX();
		int y = p.getY();
		// Wilderness: x 2944–3391, y 3520–3967. Nivå = (y - 3520) / 8 + 1
		if (x >= 2944 && x <= 3391 && y >= 3520 && y <= 3967)
		{
			Place n = nearest(x, y, 25);
			String near = n != null ? (en ? " at " : " ved ") + n.name : "";
			return "Wilderness (lvl " + ((y - 3520) / 8 + 1) + ")" + near;
		}
		// Grotter og dungeons ligger 6400 ruter "nord" for stedet over bakken
		boolean underground = y >= 9000 && y < 12800;
		int sy = underground ? y - 6400 : y;
		Place n = nearest(x, sy, 160);
		if (n == null)
		{
			return underground ? (en ? "in a dungeon" : "i en dungeon") : (en ? "somewhere in Gielinor" : "et sted i Gielinor");
		}
		int d = Math.max(Math.abs(n.x - x), Math.abs(n.y - sy));
		if (underground)
		{
			return (en ? "beneath " : "under ") + n.name;
		}
		return (d <= 45 ? (en ? "at " : "ved ") : (en ? "near " : "nær ")) + n.name;
	}

	private static Place nearest(int x, int y, int max)
	{
		Place best = null;
		int bd = Integer.MAX_VALUE;
		for (Place pl : PLACES)
		{
			int d = Math.max(Math.abs(pl.x - x), Math.abs(pl.y - y));
			if (d < bd)
			{
				bd = d;
				best = pl;
			}
		}
		return bd <= max ? best : null;
	}
}
