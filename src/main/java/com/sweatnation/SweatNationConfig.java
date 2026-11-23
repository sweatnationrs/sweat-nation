package com.sweatnation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(SweatNationPlugin.CONFIG_GROUP)
public interface SweatNationConfig extends Config
{
	enum Language
	{
		NORSK("Norsk"),
		ENGLISH("English");

		private final String label;

		Language(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(keyName = "language", name = "Språk / Language",
		description = "Språket i panelet, på kartet og i chatten. / Language of the panel, map and chat.",
		position = -1)
	default Language language()
	{
		return Language.NORSK;
	}

	enum MarkerLogo
	{
		ROSE("Rose"),
		FLAME("Dråpe / Drop");

		private final String label;

		MarkerLogo(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(name = "Tilkobling / Connection", description = "Hvordan du kobles sammen med klanen / How you connect with the clan", position = 0)
	String groupSection = "group";

	@ConfigSection(name = "Live-kart / Live map", description = "Sanntidskartet med klanmedlemmer / The real-time map of clan members", position = 1)
	String mapSection = "map";

	@ConfigSection(name = "Rohirrim!", description = "Kall klanen til deg eller til et punkt / Call the clan to you or to a point", position = 2)
	String rallySection = "rally";

	@ConfigSection(name = "Gruppe / Group", description = "Lobby, HUD og gruppefarge på minikartet / Lobby, HUD and group colour on the minimap", position = 3)
	String teamSection = "team";

	@ConfigItem(keyName = "teamHud", name = "HUD i gruppe / HUD in group",
		description = "Vis liv, bønn og spec for gruppa du er med i"
			+ " / Show HP, prayer and spec for the group you are in",
		position = 1, section = teamSection)
	default boolean teamHud()
	{
		return true;
	}

	@Range(min = 0, max = 10000)
	@ConfigItem(keyName = "hudTeamTiles", name = "Avstand i gruppe (ruter) / Distance in group (tiles)",
		description = "Hvor nær et gruppemedlem må være for å vises i HUD-en (0–9999). 10000 = alltid, uansett hvor de er i spillet (standard)."
			+ " / How close a group member must be to show in the HUD (0–9999). 10000 = always, wherever they are in the game (default).",
		position = 2, section = teamSection)
	default int hudTeamTiles()
	{
		return 10000;
	}

	@ConfigItem(keyName = "hudNearby", name = "HUD utenfor gruppe / HUD outside group",
		description = "Vis klanmedlemmer i nærheten i HUD-en når du ikke er i en gruppe (de må ha live-kartet på)"
			+ " / Show nearby clan members in the HUD when you are not in a group (they need the live map on)",
		position = 3, section = teamSection)
	default boolean hudNearby()
	{
		return true;
	}

	@Range(min = 0, max = 10000)
	@ConfigItem(keyName = "hudNearbyTiles2", name = "Avstand utenfor gruppe (ruter) / Distance outside group (tiles)",
		description = "Hvor nær et klanmedlem må være for å vises i HUD-en utenfor gruppe (0–9999). 10000 = alltid, uansett hvor de er i spillet."
			+ " / How close a clan member must be to show in the HUD outside a group (0–9999). 10000 = always, wherever they are in the game.",
		position = 4, section = teamSection)
	default int hudNearbyTiles()
	{
		return 100;
	}

	@Range(min = 50, max = 150)
	@ConfigItem(keyName = "hudScale", name = "Størrelse på HUD (%) / HUD size (%)",
		description = "Gjør HUD-en mindre eller større så den ikke tar for mye plass"
			+ " / Make the HUD smaller or bigger so it does not take up too much space",
		position = 5, section = teamSection)
	default int hudScale()
	{
		return 105;
	}

	@ConfigItem(keyName = "teamMinimap", name = "Lagfarge på minikartet / Group colour on minimap",
		description = "Vis lagkameratene dine som prikker i lagfargen på minikartet"
			+ " / Show your group mates as dots in the group colour on the minimap",
		position = 6, section = teamSection)
	default boolean teamMinimap()
	{
		return true;
	}

	@ConfigItem(keyName = "hudBuffs", name = "Boosts og buffs i HUD / Boosts and buffs in HUD",
		description = "Viser stat boosts og tidsbegrensede buffs som små ikoner under hver spiller i HUD-en, med en klokke som viser tiden igjen. "
			+ "Buffs med tid hentes fra RuneLite sin Timers & Buffs-plugin, så den må være på."
			+ " / Shows stat boosts and timed buffs as small icons under each player in the HUD, with a clock showing the time left. "
			+ "Timed buffs come from RuneLite's Timers & Buffs plugin, so it must be on.",
		position = 7, section = teamSection)
	default boolean hudBuffs()
	{
		return false;
	}

	@ConfigSection(name = "Aktivitetslogg / Activity log", description = "Historikk over drap, loot, clues, quests og levels / History of kills, loot, clues, quests and levels", position = 3)
	String logSection = "log";

	@ConfigSection(name = "Discord", description = "Kobling til Discord-serveren / Link to the Discord server", position = 4)
	String discordSection = "discord";

	@ConfigItem(keyName = "shareActivity", name = "Del min aktivitet / Share my activity",
		description = "Del drap, loot, clues, quests, levels og dødsfall med gruppa sin aktivitetslogg"
			+ " / Share kills, loot, clues, quests, levels and deaths with the group activity log",
		position = 1, section = logSection)
	default boolean shareActivity()
	{
		return true;
	}

	@Range(min = 500000, max = 2000000000)
	@ConfigItem(keyName = "historyMinLoot500k", name = "Minste loot i loggen (gp) / Minimum loot in log (gp)",
		description = "Loot under denne verdien vises ikke i loggen (minst 500 000). Split loot teller alt uansett."
			+ " / Loot below this value is not shown in the log (at least 500,000). The Vipps split counts everything regardless.",
		position = 2, section = logSection)
	default int historyMinLoot()
	{
		return 500000;
	}
	@ConfigItem(keyName = "autoJoinClan", name = "Bli med når klanchatten kobles til / Join when clan chat connects",
		description = "Blir automatisk med i klanens SWEAT NATION-gruppe når du kobler til klanchatten i spillet"
			+ " / Automatically join the clan's SWEAT NATION group when you join the clan chat in game",
		position = 1, section = groupSection)
	default boolean autoJoinClan()
	{
		return true;
	}

	@ConfigItem(keyName = "chatMessages", name = "Chatvarsler fra SWEAT NATION / Chat messages from SWEAT NATION",
		description = "Skriv varsler i spillchatten (rally, dødsfall, split, grupper klare osv.). "
			+ "Feilmeldinger når en knapp ikke kan brukes, vises alltid."
			+ " / Write notifications in the game chat (Rohirrim!, deaths, splits, groups ready etc.). Error messages when a button cannot be used are always shown.",
		position = 3, section = groupSection)
	default boolean chatMessages()
	{
		return false;
	}

	@ConfigItem(keyName = "clanSecret", name = "Klan-hemmelighet / Clan secret",
		description = "Valgfritt ord hele klanen avtaler, så bare de som kjenner det havner i gruppa"
			+ " / Optional word the whole clan agrees on, so only those who know it end up in the group",
		secret = true, position = 2, section = groupSection)
	default String clanSecret()
	{
		return "";
	}

	@ConfigItem(keyName = "liveMap", name = "Live-kart på / Live map on",
		description = "Når det står på, deler du posisjonen din i sanntid og ser de andre på verdenskartet. Kan også skrus av/på i panelet."
			+ " / When on, you share your location in real time and see the others on the world map. Can also be toggled in the panel.",
		position = 1, section = mapSection)
	default boolean liveMap()
	{
		return true;
	}

	@ConfigItem(keyName = "showOtherWorlds", name = "Vis medlemmer på andre verdener / Show members on other worlds",
		description = "Vis også medlemmer på en annen verden enn deg (tegnes nedtonet)"
			+ " / Also show members on a different world than you (drawn faded)",
		position = 2, section = mapSection)
	default boolean showOtherWorlds()
	{
		return true;
	}

	@ConfigItem(keyName = "markerLogo", name = "Mitt ikon på kartet / My map icon",
		description = "Velg rosen eller dråpen fra SWEAT NATION-banneret. Dette er ditt eget merke – slik de andre ser deg på kartet."
			+ " / Choose the rose or the drop from the SWEAT NATION banner. This is your own marker – how the others see you on the map.",
		position = 3, section = mapSection)
	default MarkerLogo markerLogo()
	{
		return MarkerLogo.ROSE;
	}

	@Range(min = 50, max = 250)
	@ConfigItem(keyName = "markerSize", name = "Størrelse på medlemsmerker (%) / Member marker size (%)",
		description = "Grunnstørrelse på merkene. De blir i tillegg større når du zoomer ut, så de er lette å finne."
			+ " / Base size of the markers. They also get bigger when you zoom out, so they are easy to find.",
		position = 4, section = mapSection)
	default int markerSize()
	{
		return 100;
	}

	@ConfigItem(keyName = "deathMarkers", name = "Dødningshode der medlemmer dør / Skull where members die",
		description = "Vis et rødt dødningshode på kartet der et medlem døde"
			+ " / Show a red skull on the map where a member died",
		position = 5, section = mapSection)
	default boolean deathMarkers()
	{
		return true;
	}

	@Range(min = 1, max = 240)
	@ConfigItem(keyName = "deathMinutes15", name = "Vis dødsfall i (min) / Show deaths for (min)",
		description = "Hvor lenge dødningshodet blir liggende på kartet"
			+ " / How long the skull stays on the map",
		position = 6, section = mapSection)
	default int deathMinutes()
	{
		return 15;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(keyName = "rallyMinutes5", name = "Varighet (min) / Duration (min)",
		description = "Hvor lenge et Rohirrim! er aktivt"
			+ " / How long a Rohirrim! stays active",
		position = 1, section = rallySection)
	default int rallyMinutes()
	{
		return 5;
	}

	@ConfigItem(keyName = "rallyShortestPath", name = "Raskeste vei (Shortest Path) / Fastest route (Shortest Path)",
		description = "Tegn raskeste vei til Rohirrim!-punktet, med teleporter, via Shortest Path-pluginen fra Plugin Hub"
			+ " / Draw the fastest route to the Rohirrim! point, with teleports, using the Shortest Path plugin from the Plugin Hub",
		position = 7, section = rallySection)
	default boolean rallyShortestPath()
	{
		return true;
	}

	@ConfigItem(keyName = "rallyTeleport", name = "Raskeste teleport / Fastest teleport",
		description = "Vis hvilken teleport-gjenstand som tar deg nærmest Rohirrim!-punktet, og lys den opp i bank, utstyr og inventory"
			+ " / Show which teleport item gets you closest to the Rohirrim! point, and highlight it in bank, equipment and inventory",
		position = 8, section = rallySection)
	default boolean rallyTeleport()
	{
		return true;
	}

	@ConfigItem(keyName = "rallySound", name = "Spill Rohirrim!-lyden / Play Rohirrim! sound",
		description = "Spill klanens kall når noen roper Rohirrim!"
			+ " / Play the clan call when someone calls Rohirrim!",
		position = 2, section = rallySection)
	default boolean rallySound()
	{
		return true;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(keyName = "rallyVolume100", name = "Volum (%) / Volume (%)",
		description = "Volum på Rohirrim!-lyden. 100 er standard. Over 100 forsterkes lyden med en myk limiter, så den blir høyere uten å skurre."
			+ " / Volume of the Rohirrim! sound. 100 is default. Above 100 the sound is boosted with a soft limiter, so it gets louder without distorting.",
		position = 3, section = rallySection)
	default int rallyVolume()
	{
		return 100;
	}

	@ConfigItem(keyName = "rallyDuckMusic", name = "Demp musikken under kallet / Lower music during the call",
		description = "Senker spillmusikken et par sekunder mens rallylyden spilles, så kallet høres tydelig"
			+ " / Lowers the game music for a few seconds while the call plays, so it is heard clearly",
		position = 4, section = rallySection)
	default boolean rallyDuckMusic()
	{
		return true;
	}

	@ConfigItem(keyName = "rallyNotify", name = "Varsle ved Rohirrim! / Notify on Rohirrim!",
		description = "Send et RuneLite-varsel (blink i oppgavelinja) når noen roper rally"
			+ " / Send a RuneLite notification (taskbar flash) when someone calls Rohirrim!",
		position = 4, section = rallySection)
	default boolean rallyNotify()
	{
		return true;
	}

	@ConfigItem(keyName = "rallyHintArrow", name = "Pil til punktet / Arrow to the point",
		description = "Vis den gule spillpilen mot rallypunktet når det er i nærheten"
			+ " / Show the yellow game arrow towards the Rohirrim! point when it is nearby",
		position = 5, section = rallySection)
	default boolean rallyHintArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "rallyOverlay", name = "Rohirrim!-boks i spillet / Rohirrim! box in game",
		description = "Vis en boks med avstand og retning til rallypunktet"
			+ " / Show a box with distance and direction to the Rohirrim! point",
		position = 6, section = rallySection)
	default boolean rallyOverlay()
	{
		return true;
	}

	@ConfigItem(keyName = "discordLink", name = "Discord-lenke / Discord link",
		description = "Invitasjon eller kanal-lenke til klanens Discord, f.eks. https://discord.gg/abc123. "
			+ "Knappen i Discord-fanen åpner den i nettleseren. Er du medlem av SWEAT NATION-klanen, deles lenken "
			+ "med de andre klanmedlemmene i gruppa, så de får den automatisk uten å skrive den inn selv."
			+ " / Invite or channel link to the clan Discord, e.g. https://discord.gg/abc123. The button in the Discord tab opens it in your browser. If you are a member of the SWEAT NATION clan, the link is shared with the other clan members in the group so they get it automatically.",
		position = 1, section = discordSection)
	default String discordLink()
	{
		return "";
	}
}
