package com.sweatnation;

import com.google.inject.Provides;
import com.sweatnation.messages.ActivityShare;
import com.sweatnation.messages.DeathNotice;
import com.sweatnation.messages.BuffEntry;
import com.sweatnation.messages.BuffIcon;
import com.sweatnation.messages.BuffState;
import com.sweatnation.messages.DiscordShare;
import com.sweatnation.messages.LocationUpdate;
import com.sweatnation.messages.LootShare;
import com.sweatnation.messages.Presence;
import com.sweatnation.messages.RallyCall;
import com.sweatnation.messages.RallyResponse;
import com.sweatnation.messages.SignupUpdate;
import com.sweatnation.messages.GroupEntry;
import com.sweatnation.messages.SplitControl;
import com.sweatnation.messages.ReadyCheckMsg;
import com.sweatnation.messages.SpecUsed;
import com.sweatnation.messages.TeamStatus;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.World;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Actor;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.Notifier;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.WorldService;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldResult;

@PluginDescriptor(
	name = "SWEAT NATION",
	description = "Clan tools: live member map, rally calls, group lobbies with ready check, team HUD, loot split and activity log",
	tags = {"clan", "party", "rally", "group", "lobby", "boss", "hud", "map", "teleport", "split", "loot", "discord", "norwegian", "sweatnation"}
)
public class SweatNationPlugin extends Plugin implements SweatNationController
{
	static final String CONFIG_GROUP = "sweatnation";

	/** Verdenskartets kartvisning (interface 595, komponent 7). */
	private static final int WORLD_MAP_GROUP = 595;
	private static final int WORLD_MAP_VIEW_CHILD = 7;
	private static final String RALLY_OPTION = L.t("Rohirrim! her", "Rohirrim! here");
	private static final String HOP_OPTION = L.t("Hopp til", "Hop to");
	private static final long MEMBER_TIMEOUT_MS = 3 * 60_000L;
	/** Send posisjon hver 3. tick (≈1,8 s) når du beveger deg – sanntid uten å spamme party-serveren. */
	private static final int SEND_EVERY_TICKS = 3;

	// Spillmeldinger som blir til hendelser i aktivitetsloggen
	private static final Pattern KC = Pattern.compile("Your (?:completed )?(.+?) (?:kill |success |harvest |completion |completed )?count is: ([\\d,]+)");
	private static final Pattern CLUE = Pattern.compile("You have completed ([\\d,]+) (\\w+) Treasure Trails?");
	private static final Pattern QUEST = Pattern.compile("Congratulations, you've completed an? (?:\\w+ )?quest: (.+)");
	private static final Pattern COLLECTION = Pattern.compile("New item added to your collection log: (.+)");
	private static final String[] PET_MESSAGES = {
		"You have a funny feeling like you're being followed",
		"You feel something weird sneaking into your backpack",
		"You have a funny feeling like you would have been followed"
	};

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private SweatNationConfig config;
	@Inject private ConfigManager configManager;
	@Inject private PartyService partyService;
	@Inject private WSClient wsClient;
	@Inject private WorldMapPointManager worldMapPointManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private OverlayManager overlayManager;
	@Inject private RallyOverlay rallyOverlay;
	@Inject private TeleportOverlay teleportOverlay;
	@Inject private TeamHudOverlay teamHudOverlay;
	@Inject private TeamMinimapOverlay teamMinimapOverlay;

	// ---- lobby, klar-sjekk og lag-HUD
	/** Gruppene du er med i: nøkkel → din oppføring (med din rolle). */
	private final Map<String, GroupEntry> myGroups = new ConcurrentHashMap<>();
	/** Gruppene de andre er med i. */
	private final Map<Long, List<GroupEntry>> remoteGroups = new ConcurrentHashMap<>();
	private final Map<String, ReadyCheck> readyChecks = new ConcurrentHashMap<>();
	private final Map<String, TeamMember> teamStatus = new ConcurrentHashMap<>();
	private int lastSpec = -1;
	private volatile int weaponId = -1;
	private int lastSentHp = -1;
	private int lastSentPrayer = -1;
	private int lastSentSpec = -1;
	private int ticksSinceStatus;

	/** Hva du har av gjenstander: beholder (inventory/utstyr/banken) → gjenstands-IDer. */
	private final Map<String, Set<Integer>> ownedItems = new ConcurrentHashMap<>();
	/** Antall av hver gjenstand per beholder (for runer til teleport-trylleformler). */
	private final Map<String, Map<Integer, Integer>> ownedCounts = new ConcurrentHashMap<>();
	/** Runer i rune pouch og hvilken spellbook du står på. Leses fra varbits på klienttråden. */
	private volatile Map<Integer, Integer> pouchRunes = Map.of();
	private volatile String spellbook;
	/** Elementrunene staven/tomen du har på deg gir uendelig av. */
	private volatile Set<String> staffRunes = Set.of();
	private final Map<Integer, String> itemNames = new ConcurrentHashMap<>();
	private volatile Teleports.Advice teleportAdvice;
	@Inject private EventBus eventBus;
	@Inject private PluginManager pluginManager;
	@Inject private MouseManager mouseManager;

	private volatile double markerScale = 1.0;
	private volatile WorldPoint pendingFocus;
	private boolean shortestPathActive;
	private final net.runelite.client.input.MouseAdapter mapMouse = new net.runelite.client.input.MouseAdapter()
	{
		@Override
		public java.awt.event.MouseEvent mouseClicked(java.awt.event.MouseEvent e)
		{
			if (e.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e))
			{
				int x = e.getX();
				int y = e.getY();
				clientThread.invokeLater(() ->
				{
					MemberState m = memberAt(x, y);
					if (m != null && m.getWorld() != client.getWorld())
					{
						hopToWorld(m.getWorld());
					}
				});
			}
			return e;
		}
	};
	@Inject private Notifier notifier;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private WorldService worldService;
	@Inject private ItemManager itemManager;
	@Inject private net.runelite.client.game.SkillIconManager skillIconManager;
	@Inject private net.runelite.client.ui.overlay.infobox.InfoBoxManager infoBoxManager;
	@Inject private Gson gson;
	@Inject private ScheduledExecutorService executor;

	private ActivityLog activityLog;
	private ScheduledFuture<?> saveTask;
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private volatile String clanDisplayName;
	/** Navnet på klanen du er medlem av akkurat nå (null = ingen / ikke logget inn). */
	private volatile String currentClan;

	/** Discord-lenken et klanmedlem har delt med gruppa (ligger bare i minnet, aldri i koden). */
	private volatile String sharedDiscordLink;

	private final Map<Long, MemberState> members = new ConcurrentHashMap<>();
	private final Map<Long, WorldMapPoint> memberMapPoints = new ConcurrentHashMap<>();
	private final Map<Long, String> memberMarkerKey = new ConcurrentHashMap<>();
	private final Map<Long, String> signupNames = new ConcurrentHashMap<>();
	private final Set<String> announcedMatches = ConcurrentHashMap.newKeySet();
	private final List<DeathRecord> deaths = new CopyOnWriteArrayList<>();
	private final Map<DeathRecord, WorldMapPoint> deathPoints = new ConcurrentHashMap<>();

	private volatile RallyState activeRally;
	private WorldMapPoint rallyMapPoint;
	private volatile SplitSession split;

	private SweatNationPanel panel;
	private NavigationButton navButton;
	private BufferedImage rallyBanner;

	private volatile WorldPoint localLocation;
	private volatile int localWorld;
	private WorldPoint lastSentLocation;
	private int lastSentWorld;
	private int ticksSinceSend;
	private int tickCounter;
	private String autoJoinedCode;
	private World pendingHop;

	@Provides
	SweatNationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SweatNationConfig.class);
	}

	@Override
	protected void startUp()
	{
		wsClient.registerMessage(LocationUpdate.class);
		wsClient.registerMessage(RallyCall.class);
		wsClient.registerMessage(RallyResponse.class);
		wsClient.registerMessage(SignupUpdate.class);
		wsClient.registerMessage(DeathNotice.class);
		wsClient.registerMessage(SplitControl.class);
		wsClient.registerMessage(LootShare.class);
		wsClient.registerMessage(ActivityShare.class);
		wsClient.registerMessage(DiscordShare.class);
		wsClient.registerMessage(TeamStatus.class);
		wsClient.registerMessage(SpecUsed.class);
		wsClient.registerMessage(ReadyCheckMsg.class);
		wsClient.registerMessage(Presence.class);
		wsClient.registerMessage(BuffState.class);
		wsClient.registerMessage(BuffIcon.class);

		activityLog = new ActivityLog(gson, new File(RuneLite.RUNELITE_DIR, "sweatnation"));
		activityLog.open(partyService.isInParty() ? partyService.getPartyPassphrase() : null);
		saveTask = executor.scheduleWithFixedDelay(activityLog::saveIfDirty, 30, 30, TimeUnit.SECONDS);
		loggedOutAt = client.getGameState() == GameState.LOGGED_IN ? 0 : System.currentTimeMillis();
		offlineTask = executor.scheduleWithFixedDelay(this::checkLoggedOut, 30, 30, TimeUnit.SECONDS);

		L.setEnglish(config.language() == SweatNationConfig.Language.ENGLISH);
		rallyBanner = ImageUtil.loadImageResource(SweatNationPlugin.class, "rally_banner.png");
		BufferedImage navIcon = ImageUtil.loadImageResource(SweatNationPlugin.class, "icon.png");
		BufferedImage logo = ImageUtil.loadImageResource(SweatNationPlugin.class, "logo.png");

		panel = new SweatNationPanel(this, logo);
		navButton = NavigationButton.builder()
			.tooltip("SWEAT NATION")
			.icon(navIcon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(rallyOverlay);
		overlayManager.add(teleportOverlay);
		overlayManager.add(teamHudOverlay);
		overlayManager.add(teamMinimapOverlay);
		mouseManager.registerMouseListener(mapMouse);

		lastSentLocation = null;
		// Allerede i klanchatten når pluginen slås på?
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN && client.getClanChannel() != null)
			{
				currentClan = client.getClanChannel().getName();
				autoJoin(client.getClanChannel());
				refreshPanel();
			}
		});
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(LocationUpdate.class);
		wsClient.unregisterMessage(RallyCall.class);
		wsClient.unregisterMessage(RallyResponse.class);
		wsClient.unregisterMessage(SignupUpdate.class);
		wsClient.unregisterMessage(DeathNotice.class);
		wsClient.unregisterMessage(SplitControl.class);
		wsClient.unregisterMessage(LootShare.class);
		wsClient.unregisterMessage(ActivityShare.class);
		wsClient.unregisterMessage(DiscordShare.class);
		wsClient.unregisterMessage(TeamStatus.class);
		wsClient.unregisterMessage(SpecUsed.class);
		wsClient.unregisterMessage(ReadyCheckMsg.class);
		wsClient.unregisterMessage(Presence.class);
		wsClient.unregisterMessage(BuffState.class);
		wsClient.unregisterMessage(BuffIcon.class);

		if (saveTask != null)
		{
			saveTask.cancel(false);
		}
		if (offlineTask != null)
		{
			offlineTask.cancel(false);
			offlineTask = null;
		}
		ActivityLog logToSave = activityLog;
		executor.execute(logToSave::saveIfDirty);
		levels.clear();
		duckGeneration.incrementAndGet();
		clientThread.invokeLater(this::restoreMusic);

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(rallyOverlay);
		overlayManager.remove(teleportOverlay);
		overlayManager.remove(teamHudOverlay);
		overlayManager.remove(teamMinimapOverlay);
		mouseManager.unregisterMouseListener(mapMouse);
		clearRally();
		clearAllMembers();
		clearDeaths();
		myGroups.clear();
		announcedMatches.clear();
		split = null;
		panel = null;
	}

	// ------------------------------------------------------------------ auto-join via klanchatten

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged e)
	{
		if (e.isGuest())
		{
			return;
		}
		currentClan = e.getClanChannel() == null ? null : e.getClanChannel().getName();
		if (currentClan == null)
		{
			sharedDiscordLink = null; // ute av klanen: ingen klan-lenke
		}
		shareDiscordLink();
		refreshPanel();
		if (e.getClanChannel() != null)
		{
			autoJoin(e.getClanChannel());
		}
		else if (autoJoinedCode != null && autoJoinedCode.equals(partyService.getPartyPassphrase()))
		{
			// Klanchatten forsvant. Skjer det fordi du logger ut, blir du i gruppa (så gruppene dine står i 10 minutter).
			// Er du fortsatt logget inn etter et par sekunder, har du faktisk forlatt klanchatten: da forlater du gruppa.
			String code = autoJoinedCode;
			executor.schedule(() -> clientThread.invokeLater(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN && client.getClanChannel() == null
					&& code.equals(autoJoinedCode) && code.equals(partyService.getPartyPassphrase()))
				{
					partyService.changeParty(null);
					autoJoinedCode = null;
					chat(L.t("Koblet fra klanchatten – forlot SWEAT NATION-gruppa.", "Left the clan chat – left the SWEAT NATION group."));
				}
			}), 3, TimeUnit.SECONDS);
		}
	}

	private void autoJoin(ClanChannel clan)
	{
		if (!config.autoJoinClan() || clan == null || clan.getName() == null)
		{
			return;
		}
		String code = clanCode(clan.getName());
		if (code.equals(partyService.getPartyPassphrase()))
		{
			return; // allerede med
		}
		if (partyService.isInParty() && !partyService.getPartyPassphrase().equals(autoJoinedCode))
		{
			return; // brukeren er i en egen gruppe – ikke overstyr
		}
		autoJoinedCode = code;
		clanDisplayName = clan.getName();
		partyService.changeParty(code);
		chat(L.t("Koblet til klanchatten – du er nå med i SWEAT NATION-gruppa for ", "Joined the clan chat – you are now in the SWEAT NATION group for ") + clan.getName() + ".");
	}

	private String clanCode(String clanName)
	{
		String secret = config.clanSecret().trim();
		return "sweatnation-" + clanName.toLowerCase().replaceAll("[^a-z0-9]+", "-")
			+ (secret.isEmpty() ? "" : "-" + secret.toLowerCase());
	}

	// ------------------------------------------------------------------ spill-hendelser

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			lastSentLocation = null; // send posisjon på nytt etter innlogging / world hop
			if (loggedOutAt != 0)
			{
				loggedOutAt = 0;
				sendPresence(true);
			}
		}
		else if (e.getGameState() == GameState.LOGIN_SCREEN)
		{
			if (loggedOutAt == 0)
			{
				loggedOutAt = System.currentTimeMillis();
				sendPresence(false);
			}
			levels.clear(); // ny innlogging sender alle nivåer på nytt – det er ikke level-ups
			recentAttackers.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		tickCounter++;
		if (tickCounter % 2 == 0 && activeRally != null)
		{
			readMagicState();
		}

		if (pendingHop != null)
		{
			client.hopToWorld(pendingHop);
			pendingHop = null;
		}

		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return;
		}
		if (lp.getName() != null)
		{
			lastLocalName = lp.getName();
		}
		WorldPoint wp = WorldPoint.fromLocalInstance(client, lp.getLocalLocation());
		localLocation = wp;
		int world = client.getWorld();
		boolean worldChanged = world != localWorld;
		localWorld = world;
		if (worldChanged)
		{
			members.values().forEach(this::updateMemberMapPoint); // farge for samme/annen verden
		}

		// Live-kart: send posisjonen i sanntid mens det står på
		if (config.liveMap() && partyService.isInParty() && partyService.getLocalMember() != null)
		{
			ticksSinceSend++;
			boolean moved = !wp.equals(lastSentLocation) || world != lastSentWorld;
			if ((moved && ticksSinceSend >= SEND_EVERY_TICKS) || ticksSinceSend >= 50)
			{
				LocationUpdate lu = new LocationUpdate(lp.getName(), wp.getX(), wp.getY(), wp.getPlane(),
					world, lp.getCombatLevel(), false);
				lu.logo = config.markerLogo().name();
				send(lu);
				lastSentLocation = wp;
				lastSentWorld = world;
				ticksSinceSend = 0;
			}
		}

		RallyState rally = activeRally;
		if (rally != null)
		{
			if (rally.isExpired())
			{
				clearRally();
				chat(L.t("Rohirrim! fra ", "Rohirrim! from ") + rally.getCallerName() + L.t(" er utløpt.", " has expired."));
			}
			else if (config.rallyHintArrow() && rally.getWorld() == world
				&& rally.getTarget().getPlane() == wp.getPlane()
				&& rally.getTarget().distanceTo2D(wp) < 64)
			{
				if (rally.getTarget().distanceTo2D(wp) <= 1)
				{
					client.clearHintArrow();
				}
				else
				{
					client.setHintArrow(rally.getTarget());
				}
			}
		}

		updateOwnStatus(lp.getName());

		if (tickCounter % 10 == 0)
		{
			readyChecks.entrySet().removeIf(en -> en.getValue().isExpired());
			pruneStaleMembers();
			pruneDeaths();
			refreshPanel();
		}
	}

	/** Følger zoomen på verdenskartet, og hopper til et ventende punkt når kartet åpnes. */
	@Subscribe
	public void onClientTick(ClientTick t)
	{
		Widget view = client.getWidget(WORLD_MAP_GROUP, WORLD_MAP_VIEW_CHILD);
		WorldMap map = client.getWorldMap();
		if (view == null || view.isHidden() || map == null)
		{
			return;
		}
		WorldPoint focus = pendingFocus;
		if (focus != null)
		{
			pendingFocus = null;
			map.setWorldMapPositionTarget(focus);
		}
		double scale = scaleFor(map.getWorldMapZoom());
		if (Math.abs(scale - markerScale) >= 0.05)
		{
			markerScale = scale;
			members.values().forEach(this::updateMemberMapPoint);
		}
	}

	/** Større merker når du zoomer ut, så klanen er lett å finne; normal størrelse når du zoomer inn. */
	private double scaleFor(float zoom)
	{
		double z = Math.max(0.25, zoom);
		double f = 1.0 + 0.35 * (Math.log(4.0 / z) / Math.log(2));
		f = Math.max(1.0, Math.min(2.0, f));
		return Math.round(f * config.markerSize() / 100.0 * 20) / 20.0;
	}

	/** Finner medlemmet hvis merke er under musepekeren på verdenskartet. */
	private MemberState memberAt(int mouseX, int mouseY)
	{
		Widget view = client.getWidget(WORLD_MAP_GROUP, WORLD_MAP_VIEW_CHILD);
		WorldMap map = client.getWorldMap();
		if (view == null || view.isHidden() || map == null || !config.liveMap())
		{
			return null;
		}
		java.awt.Rectangle b = view.getBounds();
		if (!b.contains(mouseX, mouseY))
		{
			return null;
		}
		float zoom = map.getWorldMapZoom();
		Point center = map.getWorldMapPosition();
		int radius = (int) Math.round(18 * markerScale);
		MemberState best = null;
		double bestD = Double.MAX_VALUE;
		for (MemberState m : members.values())
		{
			WorldPoint wp = m.getLocation();
			if (wp == null)
			{
				continue;
			}
			double sx = b.getCenterX() + (wp.getX() - center.getX()) * zoom;
			double sy = b.getCenterY() - (wp.getY() - center.getY()) * zoom;
			// merket står litt over selve punktet; godta klikk på både merke og navneskilt
			double dx = mouseX - sx;
			double dy = mouseY - (sy - radius * 0.6);
			double d = Math.sqrt(dx * dx + dy * dy * 0.6);
			if (d < radius * 1.6 && d < bestD)
			{
				bestD = d;
				best = m;
			}
		}
		return best;
	}

	/** Hvem som har angrepet deg den siste tiden (for «drept av …»). */
	private final Map<Actor, Long> recentAttackers = new java.util.WeakHashMap<>();
	private static final long ATTACKER_MEMORY_MS = 15_000;

	@Subscribe
	public void onInteractingChanged(InteractingChanged e)
	{
		Player lp = client.getLocalPlayer();
		if (lp != null && e.getSource() != null && e.getTarget() == lp)
		{
			recentAttackers.put(e.getSource(), System.currentTimeMillis());
		}
	}

	/** Finner hvem som drepte deg: den som fortsatt angriper deg, ellers den som angrep sist. */
	private String findKiller(Player lp)
	{
		long now = System.currentTimeMillis();
		Actor best = null;
		long bestTime = 0;
		boolean bestActive = false;
		for (Map.Entry<Actor, Long> en : recentAttackers.entrySet())
		{
			Actor a = en.getKey();
			long t = en.getValue();
			if (a == null || now - t > ATTACKER_MEMORY_MS || a.getName() == null)
			{
				continue;
			}
			boolean active = a.getInteracting() == lp;
			if (best == null || (active && !bestActive) || (active == bestActive && t > bestTime))
			{
				best = a;
				bestTime = t;
				bestActive = active;
			}
		}
		if (best == null)
		{
			return null;
		}
		String name = Text.removeTags(best.getName()).replace('\u00A0', ' ').trim();
		return best instanceof Player ? "P:" + name : name;
	}

	/** Du døde: legg et dødningshode på kartet for hele gruppa. */
	// ------------------------------------------------------------------ PK-er

	/** Spillere du selv har truffet nylig: navn -> tidspunkt for siste treff. */
	private final Map<String, Long> recentPkHits = new java.util.HashMap<>();
	/** Navn på spillere du nylig er logget for å ha drept, så samme PK ikke logges to ganger. */
	private final Map<String, Long> loggedPks = new java.util.HashMap<>();
	private static final long PK_HIT_WINDOW_MS = 15_000;

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		Player lp = client.getLocalPlayer();
		if (lp != null && e.getActor() instanceof Player && e.getActor() != lp
			&& e.getHitsplat() != null && e.getHitsplat().isMine() && e.getActor().getName() != null)
		{
			long now = System.currentTimeMillis();
			recentPkHits.values().removeIf(t -> now - t > PK_HIT_WINDOW_MS);
			recentPkHits.put(e.getActor().getName(), now);
		}
	}

	/** En annen spiller døde. Har du truffet dem de siste sekundene i Wilderness eller på en PvP-verden, er det din PK. */
	private void checkPk(Player victim)
	{
		String name = victim.getName();
		Long hit = name == null ? null : recentPkHits.remove(name);
		long now = System.currentTimeMillis();
		if (hit == null || now - hit > PK_HIT_WINDOW_MS)
		{
			return;
		}
		WorldPoint wp = WorldPoint.fromLocalInstance(client, victim.getLocalLocation());
		boolean pvpWorld = client.getWorldType() != null
			&& (client.getWorldType().contains(WorldType.PVP) || client.getWorldType().contains(WorldType.DEADMAN));
		if (!pvpWorld && !inWilderness(wp))
		{
			return; // LMS, PvP Arena, duell osv. regnes ikke som PK
		}
		loggedPks.values().removeIf(t -> now - t > 60_000);
		if (loggedPks.putIfAbsent(name, now) != null)
		{
			return;
		}
		int w = client.getWorld();
		String lvl = victim.getCombatLevel() > 0 ? " (lvl " + victim.getCombatLevel() + ")" : "";
		record(ActivityEvent.Type.KILL,
			"PK: drepte " + name + lvl + " – " + Places.describe(wp, false) + " (verden " + w + ")",
			"PK: killed " + name + lvl + " – " + Places.describe(wp, true) + " (world " + w + ")", 0);
	}

	private static boolean inWilderness(WorldPoint wp)
	{
		if (wp == null || wp.getX() < 2944 || wp.getX() > 3391)
		{
			return false;
		}
		int y = wp.getY();
		return (y >= 3520 && y <= 3967) || (y >= 9920 && y <= 10431); // overflaten og Wilderness-dungeons
	}

	@Subscribe
	public void onActorDeath(ActorDeath e)
	{
		Player lp = client.getLocalPlayer();
		if (lp != null && e.getActor() instanceof Player && e.getActor() != lp)
		{
			checkPk((Player) e.getActor());
			return;
		}
		if (lp == null || e.getActor() != lp)
		{
			return;
		}
		recentPkHits.clear();
		WorldPoint wp = WorldPoint.fromLocalInstance(client, lp.getLocalLocation());
		String killer = findKiller(lp);
		recentAttackers.clear();
		DeathRecord death = new DeathRecord(lp.getName(), wp, client.getWorld(), killer);
		addDeath(death);
		int w = client.getWorld();
		record(ActivityEvent.Type.DEATH,
			(killer == null ? "døde" : "ble drept av " + DeathRecord.killerName(killer, false))
				+ " – " + Places.describe(wp, false) + " (verden " + w + ")",
			(killer == null ? "died" : "was killed by " + DeathRecord.killerName(killer, true))
				+ " – " + Places.describe(wp, true) + " (world " + w + ")", 0);
		if (config.liveMap() && partyService.isInParty() && partyService.getLocalMember() != null)
		{
			DeathNotice m = new DeathNotice();
			m.name = lp.getName();
			m.x = wp.getX();
			m.y = wp.getY();
			m.plane = wp.getPlane();
			m.world = client.getWorld();
			m.killer = killer;
			send(m);
		}
	}

	/** Loot fra RuneLite sin Loot Tracker → split-økten. */
	@Subscribe
	public void onLootReceived(LootReceived e)
	{
		logLoot(e);
		SplitSession s = split;
		String me = getLocalName();
		if (s == null || !s.isActive() || !s.hasParticipant(me) || !partyService.isInParty())
		{
			return;
		}
		LootShare m = new LootShare();
		m.sessionId = s.getId();
		m.player = me;
		m.source = e.getName();
		for (ItemStack stack : e.getItems())
		{
			int id = itemManager.canonicalize(stack.getId());
			ItemComposition comp = itemManager.getItemComposition(id);
			long value = (long) itemManager.getItemPrice(id) * stack.getQuantity();
			m.items.add(new LootShare.Item(comp.getName(), stack.getQuantity(), value));
		}
		if (m.items.isEmpty())
		{
			return;
		}
		applyLoot(m);
		send(m);
	}

	// ------------------------------------------------------------------ aktivitetslogg

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		// Fjern både <col>-tagger og spillets @farge@-koder (f.eks. "@q_mes@")
		String msg = Text.removeTags(e.getMessage()).replaceAll("@[a-zA-Z_]+@", "").trim();
		Matcher m = KC.matcher(msg);
		if (m.find())
		{
			long n = parseNum(m.group(2));
			record(ActivityEvent.Type.KILL, "drepte " + m.group(1) + " (KC " + m.group(2) + ")",
				"killed " + m.group(1) + " (KC " + m.group(2) + ")", n);
			return;
		}
		m = CLUE.matcher(msg);
		if (m.find())
		{
			record(ActivityEvent.Type.CLUE, "fullførte en " + m.group(2) + " clue (" + m.group(1) + " totalt)",
				"completed a " + m.group(2) + " clue (" + m.group(1) + " total)", parseNum(m.group(1)));
			return;
		}
		m = QUEST.matcher(msg);
		if (m.find())
		{
			String quest = m.group(1).replaceAll("[.!]+$", "");
			record(ActivityEvent.Type.QUEST, "fullførte questen " + quest, "completed the quest " + quest, 0);
			return;
		}
		m = COLLECTION.matcher(msg);
		if (m.find())
		{
			String item = m.group(1).replaceAll("[.!]+$", "");
			record(ActivityEvent.Type.COLLECTION, "ny i collection log: " + item, "new collection log item: " + item, 0);
			return;
		}
		for (String pet : PET_MESSAGES)
		{
			if (msg.startsWith(pet))
			{
				record(ActivityEvent.Type.PET, "fikk en PET!", "got a PET!", 0);
				return;
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		Skill skill = e.getSkill();
		int level = e.getLevel();
		Integer prev = levels.put(skill, level);
		if (prev != null && level > prev)
		{
			record(ActivityEvent.Type.LEVEL, "nådde level " + level + " i " + skill.getName(),
				"reached level " + level + " in " + skill.getName(), level);
		}
	}

	private void logLoot(LootReceived e)
	{
		long total = 0;
		String best = null;
		long bestValue = -1;
		int count = 0;
		for (ItemStack stack : e.getItems())
		{
			int id = itemManager.canonicalize(stack.getId());
			long v = (long) itemManager.getItemPrice(id) * stack.getQuantity();
			total += v;
			count++;
			if (v > bestValue)
			{
				bestValue = v;
				ItemComposition comp = itemManager.getItemComposition(id);
				best = (stack.getQuantity() > 1 ? stack.getQuantity() + " x " : "") + comp.getName();
			}
		}
		if (best == null || total < getMinLootLog())
		{
			return;
		}
		String more = count > 1 ? " (+" + (count - 1) + " til)" : "";
		String moreEn = count > 1 ? " (+" + (count - 1) + " more)" : "";
		record(ActivityEvent.Type.LOOT, "fikk " + best + more + " fra " + e.getName() + " – " + Gp.format(total) + " gp",
			"got " + best + moreEn + " from " + e.getName() + " – " + Gp.format(total) + " gp", total);
	}

	/** Legger en hendelse om deg selv i loggen og deler den med gruppa. */
	/** Legger en hendelse i loggen – på begge språk, så hver mottaker ser den på sitt eget språk. */
	private void record(ActivityEvent.Type type, String text, String textEn, long value)
	{
		if (activityLog == null)
		{
			return;
		}
		ActivityEvent ev = new ActivityEvent(UUID.randomUUID().toString(), getLocalName(), type, text, value,
			System.currentTimeMillis());
		ev.textEn = textEn;
		activityLog.add(ev);
		if (config.shareActivity() && partyService.isInParty())
		{
			ActivityShare m = new ActivityShare();
			m.events.add(ev);
			send(m);
		}
		refreshPanel();
	}

	@Subscribe
	public void onActivityShare(ActivityShare m)
	{
		if (isLocal(m.sender) || activityLog == null || m.events == null)
		{
			return;
		}
		boolean changed = false;
		for (ActivityEvent ev : m.events)
		{
			changed |= activityLog.add(ev);
		}
		if (changed)
		{
			refreshPanel();
		}
	}

	private static long parseNum(String s)
	{
		try
		{
			return Long.parseLong(s.replace(",", ""));
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	@Override
	public int getOnlineCount()
	{
		if (!partyService.isInParty())
		{
			return 0;
		}
		// Bare de som faktisk er logget inn, teller som «i Gielinor»
		int n = 0;
		PartyMember me = partyService.getLocalMember();
		for (PartyMember pm : partyService.getMembers())
		{
			boolean self = me != null && pm.getMemberId() == me.getMemberId();
			if (self ? loggedOutAt == 0 : !offlineSince.containsKey(pm.getMemberId()))
			{
				n++;
			}
		}
		return n;
	}

	@Override
	public long getMinLootLog()
	{
		return Math.max(500_000, config.historyMinLoot());
	}

	@Override
	public List<ActivityEvent> getActivity()
	{
		return activityLog == null ? List.of() : activityLog.list();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!CONFIG_GROUP.equals(e.getGroup()))
		{
			return;
		}
		if ("language".equals(e.getKey()))
		{
			L.setEnglish(config.language() == SweatNationConfig.Language.ENGLISH);
			SweatNationPanel p = panel;
			if (p != null)
			{
				SwingUtilities.invokeLater(p::rebuildForLanguage);
			}
			return;
		}
		if ("discordLink".equals(e.getKey()))
		{
			shareDiscordLink();
		}
		if ("liveMap".equals(e.getKey()))
		{
			onLiveMapChanged(config.liveMap());
		}
		if ("markerLogo".equals(e.getKey()))
		{
			lastSentLocation = null; // send det nye ikonet til de andre med en gang
		}
		if ("showOtherWorlds".equals(e.getKey()) || "markerSize".equals(e.getKey()))
		{
			clientThread.invokeLater(() -> members.values().forEach(this::updateMemberMapPoint));
		}
		if ("markerSize".equals(e.getKey()))
		{
			markerScale = -1; // tving ny beregning på neste ClientTick
		}
		if ("rallyTeleport".equals(e.getKey()))
		{
			clientThread.invokeLater(this::updateTeleportAdvice);
		}
		if (("rallyShortestPath".equals(e.getKey())) && activeRally != null)
		{
			updateShortestPath(activeRally);
		}
		if ("deathMarkers".equals(e.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				deathPoints.values().forEach(worldMapPointManager::remove);
				deathPoints.clear();
				if (config.deathMarkers())
				{
					deaths.forEach(this::addDeathPoint);
				}
			});
		}
		refreshPanel();
	}

	/** Høyreklikk på verdenskartet → "Rally her". */
	@Subscribe
	public void onMenuOpened(MenuOpened e)
	{
		if (!isMouseOverWorldMap())
		{
			return;
		}
		WorldPoint target = mouseWorldMapPoint();
		if (target == null)
		{
			return;
		}
		Point mouse = client.getMouseCanvasPosition();
		MemberState member = memberAt(mouse.getX(), mouse.getY());
		if (member != null && member.getWorld() != client.getWorld())
		{
			int w = member.getWorld();
			client.createMenuEntry(-1)
				.setOption(HOP_OPTION)
				.setTarget("<col=b07cff>" + member.getName() + "</col> (W" + w + ")")
				.setType(MenuAction.RUNELITE)
				.onClick(me -> hopToWorld(w));
		}
		client.createMenuEntry(-1)
			.setOption(RALLY_OPTION)
			.setTarget("<col=b05cff>SWEAT NATION</col>")
			.setType(MenuAction.RUNELITE)
			.onClick(me -> sendRally(target, RallyState.Type.POINT, "", true));
	}

	/** Shift + høyreklikk på en rute i spillet → "Rally her". */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (e.getMenuEntry().getType() != MenuAction.WALK || !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}
		Tile tile = client.getTopLevelWorldView().getSelectedSceneTile();
		if (tile == null)
		{
			return;
		}
		WorldPoint target = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
		client.createMenuEntry(-1)
			.setOption(RALLY_OPTION)
			.setTarget("<col=b05cff>SWEAT NATION</col>")
			.setType(MenuAction.RUNELITE)
			.onClick(me -> sendRally(target, RallyState.Type.POINT, "", true));
	}

	private boolean isMouseOverWorldMap()
	{
		Widget view = client.getWidget(WORLD_MAP_GROUP, WORLD_MAP_VIEW_CHILD);
		if (view == null || view.isHidden())
		{
			return false;
		}
		Point mouse = client.getMouseCanvasPosition();
		return view.getBounds().contains(mouse.getX(), mouse.getY());
	}

	private WorldPoint mouseWorldMapPoint()
	{
		Widget view = client.getWidget(WORLD_MAP_GROUP, WORLD_MAP_VIEW_CHILD);
		WorldMap map = client.getWorldMap();
		if (view == null || map == null)
		{
			return null;
		}
		float zoom = map.getWorldMapZoom();
		Point center = map.getWorldMapPosition();
		Point mouse = client.getMouseCanvasPosition();
		java.awt.Rectangle b = view.getBounds();
		int dx = (int) ((mouse.getX() - b.getCenterX()) / zoom);
		int dy = (int) ((b.getCenterY() - mouse.getY()) / zoom);
		return new WorldPoint(center.getX() + dx, center.getY() + dy, 0);
	}

	// ------------------------------------------------------------------ party-hendelser

	@Subscribe
	public void onPartyChanged(PartyChanged e)
	{
		sentBuffIcons.clear();
		lastBuffSig = null;
		clearAllMembers();
		clearRally();
		announcedMatches.clear();
		split = null;
		lastSentLocation = null;
		sharedDiscordLink = null;
		ActivityLog al = activityLog;
		if (al != null)
		{
			al.saveIfDirty();
			al.open(partyService.isInParty() ? partyService.getPartyPassphrase() : null);
		}
		if (!partyService.isInParty())
		{
			autoJoinedCode = null;
		}
		offlineSince.clear();
		if (partyService.isInParty())
		{
			broadcastSignups();
			if (loggedOutAt != 0)
			{
				sendPresence(false);
			}
		}
		refreshPanel();
	}

	@Subscribe
	public void onUserJoin(UserJoin e)
	{
		lastSentLocation = null; // nye medlemmer ser oss med en gang
		sentBuffIcons.clear(); // nykommeren trenger ikonene våre
		lastBuffSig = null;
		broadcastSignups();
		if (loggedOutAt != 0)
		{
			sendPresence(false); // nykommeren skal ikke telle oss som pålogget
		}
		shareDiscordLink();
		if (activityLog != null && config.shareActivity())
		{
			List<ActivityEvent> mine = activityLog.latestBy(getLocalName(), 25);
			if (!mine.isEmpty())
			{
				ActivityShare sync = new ActivityShare();
				sync.events = mine;
				send(sync);
			}
		}
		SplitSession s = split;
		if (s != null && s.isActive() && s.getHost().equals(getLocalName()))
		{
			// Fortell nykommeren om split-økten som pågår
			SplitControl m = new SplitControl();
			m.sessionId = s.getId();
			m.action = "START";
			m.player = s.getHost();
			m.title = s.getTitle();
			m.participants = s.getParticipants();
			send(m);
		}
	}

	@Subscribe
	public void onUserPart(UserPart e)
	{
		removeMember(e.getMemberId());
		offlineSince.remove(e.getMemberId());
		remoteGroups.remove(e.getMemberId());
		signupNames.remove(e.getMemberId());
		checkMatches();
		refreshPanel();
	}

	@Subscribe
	public void onLocationUpdate(LocationUpdate m)
	{
		if (isLocal(m.sender))
		{
			return;
		}
		if (m.hidden)
		{
			removeMember(m.sender);
			refreshPanel();
			return;
		}
		MemberState s = members.computeIfAbsent(m.sender, id -> new MemberState(id, m.name));
		s.setName(m.name);
		s.setLocation(new WorldPoint(m.x, m.y, m.plane));
		s.setWorld(m.world);
		s.setCombatLevel(m.combatLevel);
		s.setLogo("FLAME".equals(m.logo) ? SweatNationConfig.MarkerLogo.FLAME : SweatNationConfig.MarkerLogo.ROSE);
		s.touch();
		clientThread.invokeLater(() -> updateMemberMapPoint(s));
		refreshPanel();
	}

	@Subscribe
	public void onDeathNotice(DeathNotice m)
	{
		if (isLocal(m.sender))
		{
			return;
		}
		DeathRecord d = new DeathRecord(m.name, new WorldPoint(m.x, m.y, m.plane), m.world, m.killer);
		addDeath(d);
		chat(m.name + " " + (d.killer == null ? L.t("døde", "died") : d.cause()) + L.t("! Dødningshodet viser hvor på kartet.", "! The skull shows where on the map."));
	}

	@Subscribe
	public void onRallyCall(RallyCall m)
	{
		RallyState current = activeRally;
		if (m.cancel)
		{
			if (current != null && current.getId().equals(m.rallyId))
			{
				clearRally();
				chat(m.callerName + L.t(" avsluttet Rohirrim!.", " ended the Rohirrim!."));
				refreshPanel();
			}
			return;
		}
		if (current != null && current.getId().equals(m.rallyId))
		{
			return; // vi har det allerede (vårt eget)
		}
		RallyState.Type type = "TO_ME".equals(m.type) ? RallyState.Type.TO_ME : RallyState.Type.POINT;
		long expires = System.currentTimeMillis() + Math.max(1, m.durationMinutes) * 60_000L;
		RallyState rally = new RallyState(m.rallyId, m.sender, m.callerName,
			new WorldPoint(m.x, m.y, m.plane), m.world, type, m.note, expires);
		setRally(rally);
		if (m.glow && !isLocal(m.sender))
		{
			flashRohirrim();
		}

		if (!isLocal(m.sender))
		{
			playRallySound();
			String where = type == RallyState.Type.TO_ME ? L.t("kaller deg til seg", "calls you to them") : L.t("kaller til samling", "calls everyone to a point");
			String msg = m.callerName + " " + where + L.t(" (verden ", " (world ") + m.world + ")"
				+ (m.note == null || m.note.isEmpty() ? "" : ": " + m.note);
			chat("ROHIRRIM! " + msg + L.t(" – se SWEAT NATION-panelet.", " – see the SWEAT NATION panel."));
			if (config.rallyNotify())
			{
				notifier.notify("Rohirrim! " + msg);
			}
		}
		refreshPanel();
	}

	@Subscribe
	public void onRallyResponse(RallyResponse m)
	{
		RallyState rally = activeRally;
		if (rally != null && rally.getId().equals(m.rallyId))
		{
			rally.setResponse(m.name, m.coming);
			if (m.coming && !isLocal(m.sender))
			{
				playRallySound(); // svaret er også et Rohirrim!
			}
			if (isMyRally() && !isLocal(m.sender))
			{
				chat(m.name + (m.coming ? ": Rohirrim!" : L.t(" kan ikke komme.", " can't make it.")));
			}
			refreshPanel();
		}
	}

	@Subscribe
	public void onSignupUpdate(SignupUpdate m)
	{
		if (isLocal(m.sender))
		{
			return;
		}
		List<GroupEntry> groups = m.groups == null ? List.of() : new ArrayList<>(m.groups);
		remoteGroups.put(m.sender, groups);
		signupNames.put(m.sender, m.name);
		// Nyeste endring av gruppestørrelsen vinner – ta den inn i vår egen oppføring også
		boolean adopted = false;
		for (GroupEntry g : groups)
		{
			GroupEntry mine = g.key == null ? null : myGroups.get(g.key);
			if (mine != null && g.sizeAt > mine.sizeAt)
			{
				mine.size = g.size;
				mine.sizeAt = g.sizeAt;
				adopted = true;
			}
		}
		if (adopted)
		{
			announcedMatches.clear();
		}
		checkMatches();
		refreshPanel();
	}

	@Subscribe
	public void onSplitControl(SplitControl m)
	{
		if (isLocal(m.sender))
		{
			return;
		}
		SplitSession s = split;
		switch (m.action == null ? "" : m.action)
		{
			case "START":
				// En split fra lobbyen (flere deltakere satt på forhånd) gjelder bare deltakerne
				if (m.participants != null && m.participants.size() > 1 && !m.participants.contains(getLocalName()))
				{
					return;
				}
				if (s == null || !s.getId().equals(m.sessionId))
				{
					s = new SplitSession(m.sessionId, m.title, m.player);
					split = s;
					chat(m.player + L.t(" startet Vipps-oppgjør for ", " started a Vipps split for ") + m.title + L.t(". Trykk «Bli med» i Vipps-fanen.", ". Press “Join” in the Vipps tab."));
				}
				for (String p : m.participants)
				{
					s.addParticipant(p);
				}
				break;
			case "JOIN":
				if (s != null && s.getId().equals(m.sessionId))
				{
					s.addParticipant(m.player);
				}
				break;
			case "LEAVE":
				if (s != null && s.getId().equals(m.sessionId))
				{
					s.removeParticipant(m.player);
				}
				break;
			case "STOP":
				if (s != null && s.getId().equals(m.sessionId))
				{
					s.end();
					chat(L.t("Vipps-oppgjøret for ", "The Vipps split for ") + s.getTitle() + L.t(" er avsluttet. Se oppgjøret i Vipps-fanen.", " has ended. See the settlement in the Vipps tab."));
				}
				break;
			default:
				break;
		}
		refreshPanel();
	}

	@Subscribe
	public void onLootShare(LootShare m)
	{
		if (isLocal(m.sender))
		{
			return;
		}
		applyLoot(m);
	}

	private void applyLoot(LootShare m)
	{
		SplitSession s = split;
		if (s == null || !s.isActive() || !s.getId().equals(m.sessionId))
		{
			return;
		}
		for (LootShare.Item it : m.items)
		{
			s.addLoot(m.player, it.name, it.quantity, it.value, m.source);
		}
		refreshPanel();
	}

	// ------------------------------------------------------------------ gruppe

	@Override
	public boolean isInGroup()
	{
		return partyService.isInParty();
	}

	@Override
	public String getGroupLabel()
	{
		if (!partyService.isInParty())
		{
			return null;
		}
		String pass = partyService.getPartyPassphrase();
		if (pass != null && pass.equals(autoJoinedCode) && clanDisplayName != null)
		{
			return L.t("Klan: ", "Clan: ") + clanDisplayName;
		}
		return L.t("Gruppe: ", "Group: ") + (pass == null ? "?" : pass);
	}

	@Override
	public void createGroup()
	{
		autoJoinedCode = null;
		String code = "sn-" + partyService.generatePassphrase();
		partyService.changeParty(code);
		copyToClipboard(code);
		chat(L.t("Ny SWEAT NATION-gruppe laget. Koden \"", "New SWEAT NATION group created. The code \"") + code + L.t("\" er kopiert – del den med vennene dine.", "\" has been copied – share it with your friends."));
	}

	@Override
	public void copyGroupCode()
	{
		String pass = partyService.getPartyPassphrase();
		if (pass != null)
		{
			copyToClipboard(pass);
			chat(L.t("Gruppekoden er kopiert: ", "Group code copied: ") + pass);
		}
	}

	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	@Override
	public void joinGroup(String code)
	{
		if (code == null || code.trim().isEmpty())
		{
			return;
		}
		autoJoinedCode = null;
		partyService.changeParty(code.trim().toLowerCase());
		chat(L.t("Koblet til gruppa \"", "Joined the group \"") + code.trim().toLowerCase() + "\".");
	}

	@Override
	public void joinClanGroup()
	{
		clientThread.invokeLater(() ->
		{
			ClanChannel clan = client.getClanChannel();
			if (clan == null || clan.getName() == null)
			{
				warn(L.t("Du er ikke koblet til klanchatten. Skriv inn en gruppekode i stedet.", "You're not in the clan chat. Enter a group code instead."));
				return;
			}
			String code = clanCode(clan.getName());
			autoJoinedCode = code;
			clanDisplayName = clan.getName();
			partyService.changeParty(code);
			chat(L.t("Koblet til SWEAT NATION-gruppa for ", "Joined the SWEAT NATION group for ") + clan.getName() + ".");
		});
	}

	@Override
	public void leaveGroup()
	{
		autoJoinedCode = null;
		partyService.changeParty(null);
	}

	@Override
	public String getLocalName()
	{
		Player lp = client.getLocalPlayer();
		if (lp != null && lp.getName() != null)
		{
			return lp.getName();
		}
		return lastLocalName != null ? lastLocalName : L.t("Deg", "You");
	}

	// ------------------------------------------------------------------ live-kart

	@Override
	public boolean isLiveMap()
	{
		return config.liveMap();
	}

	@Override
	public void setLiveMap(boolean on)
	{
		configManager.setConfiguration(CONFIG_GROUP, "liveMap", on); // fyrer ConfigChanged → onLiveMapChanged
	}

	private void onLiveMapChanged(boolean on)
	{
		if (on)
		{
			lastSentLocation = null; // send posisjon med en gang
			chat(L.t("Live-kart er PÅ – du deler posisjonen din og ser klanen på kartet.", "Live map is ON – you share your location and see the clan on the map."));
		}
		else
		{
			// Si fra til de andre så markøren din forsvinner med en gang
			if (partyService.isInParty() && partyService.getLocalMember() != null)
			{
				send(new LocationUpdate(getLocalName(), 0, 0, 0, 0, 0, true));
			}
			clearAllMemberMarkers();
			members.clear();
			chat(L.t("Live-kart er AV – du er skjult og ser ikke de andre.", "Live map is OFF – you're hidden and can't see the others."));
		}
		refreshPanel();
	}

	@Override
	public List<MemberState> getMembers()
	{
		List<MemberState> list = new ArrayList<>(members.values());
		list.sort(Comparator.comparing(MemberState::getName, String.CASE_INSENSITIVE_ORDER));
		return list;
	}

	@Override
	public List<DeathRecord> getDeaths()
	{
		List<DeathRecord> l = new ArrayList<>(deaths);
		l.sort((a, b) -> Long.compare(b.time, a.time));
		return l;
	}

	@Override
	public WorldPoint getLocalLocation()
	{
		return localLocation;
	}

	@Override
	public int getLocalWorld()
	{
		return localWorld;
	}

	@Override
	public void focusOnMap(WorldPoint point)
	{
		if (point == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			WorldMap map = client.getWorldMap();
			Widget view = client.getWidget(WORLD_MAP_GROUP, WORLD_MAP_VIEW_CHILD);
			if (map == null || view == null || view.isHidden())
			{
				// Kartet hopper hit med en gang det åpnes
				pendingFocus = point;
				warn(L.t("Åpne verdenskartet – det hopper rett dit.", "Open the world map – it will jump straight there."));
				return;
			}
			map.setWorldMapPositionTarget(point);
		});
	}

	@Override
	public void hopToWorld(int worldId)
	{
		clientThread.invokeLater(() ->
		{
			if (worldId == client.getWorld())
			{
				chat(L.t("Du er allerede på verden ", "You're already on world ") + worldId + ".");
				return;
			}
			WorldResult result = worldService.getWorlds();
			net.runelite.http.api.worlds.World w = result == null ? null : result.findWorld(worldId);
			if (w == null)
			{
				warn(L.t("Fant ikke verden ", "Could not find world ") + worldId + ".");
				return;
			}
			World rsWorld = client.createWorld();
			rsWorld.setActivity(w.getActivity());
			rsWorld.setAddress(w.getAddress());
			rsWorld.setId(w.getId());
			rsWorld.setPlayerCount(w.getPlayers());
			rsWorld.setTypes(WorldUtil.toWorldTypes(w.getTypes()));

			if (client.getGameState() == GameState.LOGIN_SCREEN)
			{
				client.changeWorld(rsWorld);
				return;
			}
			chat(L.t("Hopper til verden ", "Hopping to world ") + worldId + "...");
			client.openWorldHopper();
			pendingHop = rsWorld; // hopper på neste tick når world hopper er åpen
		});
	}

	/** Oppdaterer markøren til et medlem. Flytter eksisterende markør i stedet for å lage ny (glatt sanntid). */
	private void updateMemberMapPoint(MemberState s)
	{
		if (!config.liveMap() || s.getLocation() == null)
		{
			return;
		}
		boolean sameWorld = s.getWorld() == localWorld;
		if (!sameWorld && !config.showOtherWorlds())
		{
			WorldMapPoint old = memberMapPoints.remove(s.getMemberId());
			if (old != null)
			{
				worldMapPointManager.remove(old);
			}
			return;
		}
		SweatNationConfig.MarkerLogo logo = s.getLogo(); // ikonet medlemmet selv har valgt
		double scale = markerScale;
		String key = s.getName() + "|" + sameWorld + "|" + s.getWorld() + "|" + logo + "|" + scale;
		WorldMapPoint p = memberMapPoints.get(s.getMemberId());
		if (p != null && key.equals(memberMarkerKey.get(s.getMemberId())))
		{
			p.setWorldPoint(s.getLocation());
		}
		else
		{
			if (p != null)
			{
				worldMapPointManager.remove(p);
			}
			BufferedImage img = MapMarkers.member(s.getName(), s.getWorld(), sameWorld, logo, scale);
			p = new WorldMapPoint(s.getLocation(), img);
			p.setName(s.getName());
			p.setSnapToEdge(true);
			p.setJumpOnClick(true);
			p.setImagePoint(new Point(img.getWidth() / 2, img.getHeight() - MapMarkers.anchorFromBottom(scale)));
			memberMapPoints.put(s.getMemberId(), p);
			memberMarkerKey.put(s.getMemberId(), key);
			worldMapPointManager.add(p);
		}
		p.setTooltip(s.getName() + L.t(" – verden ", " – world ") + s.getWorld() + " – lvl " + s.getCombatLevel()
			+ (sameWorld ? "" : L.t(" – dobbeltklikk for å hoppe dit", " – double-click to hop there")));
	}

	private void removeMember(long id)
	{
		members.remove(id);
		clientThread.invokeLater(() ->
		{
			WorldMapPoint p = memberMapPoints.remove(id);
			memberMarkerKey.remove(id);
			if (p != null)
			{
				worldMapPointManager.remove(p);
			}
		});
	}

	private void clearAllMemberMarkers()
	{
		clientThread.invokeLater(() ->
		{
			memberMapPoints.values().forEach(worldMapPointManager::remove);
			memberMapPoints.clear();
			memberMarkerKey.clear();
		});
	}

	private void clearAllMembers()
	{
		members.clear();
		remoteGroups.clear();
		signupNames.clear();
		clearAllMemberMarkers();
	}

	private void pruneStaleMembers()
	{
		long now = System.currentTimeMillis();
		for (MemberState s : new ArrayList<>(members.values()))
		{
			PartyMember pm = partyService.getMemberById(s.getMemberId());
			if (pm == null || now - s.getLastSeen() > MEMBER_TIMEOUT_MS)
			{
				removeMember(s.getMemberId());
			}
		}
	}

	// ------------------------------------------------------------------ dødsfall

	private void addDeath(DeathRecord d)
	{
		deaths.add(d);
		clientThread.invokeLater(() -> addDeathPoint(d));
		refreshPanel();
	}

	private void addDeathPoint(DeathRecord d)
	{
		if (!config.deathMarkers() || !config.liveMap())
		{
			return;
		}
		BufferedImage img = MapMarkers.skull(d.name);
		WorldMapPoint p = new WorldMapPoint(d.location, img);
		p.setName(d.name + " " + d.cause());
		p.setTooltip(d.name + " " + d.cause() + L.t(" her (verden ", " here (world ") + d.world + ")");
		p.setSnapToEdge(false);
		p.setJumpOnClick(true);
		p.setImagePoint(new Point(img.getWidth() / 2, img.getHeight() - 8));
		deathPoints.put(d, p);
		worldMapPointManager.add(p);
	}

	private void pruneDeaths()
	{
		long max = config.deathMinutes();
		for (DeathRecord d : deaths)
		{
			if (d.minutesAgo() >= max)
			{
				deaths.remove(d);
				WorldMapPoint p = deathPoints.remove(d);
				if (p != null)
				{
					worldMapPointManager.remove(p);
				}
			}
		}
	}

	private void clearDeaths()
	{
		deaths.clear();
		clientThread.invokeLater(() ->
		{
			deathPoints.values().forEach(worldMapPointManager::remove);
			deathPoints.clear();
		});
	}

	// ------------------------------------------------------------------ rally

	@Override
	public RallyState getActiveRally()
	{
		return activeRally;
	}

	@Override
	public boolean isMyRally()
	{
		RallyState r = activeRally;
		return r != null && isLocal(r.getCallerId());
	}

	@Override
	public void rallyToMe(String note)
	{
		clientThread.invokeLater(() ->
		{
			WorldPoint here = localLocation;
			if (here == null)
			{
				warn(L.t("Du må være logget inn for å rope Rohirrim!.", "You must be logged in to call Rohirrim!."));
				return;
			}
			sendRally(here, RallyState.Type.TO_ME, note, true);
		});
	}

	private void sendRally(WorldPoint target, RallyState.Type type, String note, boolean glow)
	{
		if (!partyService.isInParty() || partyService.getLocalMember() == null)
		{
			warn(L.t("Du er ikke koblet til en gruppe. Koble til klanchatten eller bli med i panelet.", "You're not in a group. Join the clan chat or join from the panel."));
			return;
		}
		RallyCall m = new RallyCall();
		m.rallyId = UUID.randomUUID().toString();
		m.callerName = getLocalName();
		m.x = target.getX();
		m.y = target.getY();
		m.plane = target.getPlane();
		m.world = client.getWorld();
		m.type = type.name();
		m.note = note == null ? "" : note.trim();
		m.durationMinutes = config.rallyMinutes();
		m.glow = glow;
		if (glow)
		{
			flashRohirrim();
		}

		long expires = System.currentTimeMillis() + m.durationMinutes * 60_000L;
		setRally(new RallyState(m.rallyId, partyService.getLocalMember().getMemberId(), m.callerName,
			target, m.world, type, m.note, expires));
		send(m);
		playRallySound(); // du hører den samme lyden som klanen
		chat(L.t("Rohirrim! sendt til klanen", "Rohirrim! sent to the clan") + (type == RallyState.Type.TO_ME ? L.t(" – de kalles til deg.", " – they are called to you.") : L.t(" – banneret er satt på kartet.", " – the banner is placed on the map.")));

		refreshPanel();
	}

	@Override
	public void respondToRally(boolean coming)
	{
		RallyState r = activeRally;
		if (r == null || !partyService.isInParty())
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			String name = getLocalName();
			r.setResponse(name, coming);
			send(new RallyResponse(r.getId(), name, coming));
			if (coming)
			{
				playRallySound();
			}
			if (coming && r.getWorld() != client.getWorld())
			{
				chat(L.t("Rohirrim! er på verden ", "Rohirrim! is on world ") + r.getWorld() + L.t(". Trykk «Hopp» i panelet for å bytte.", ". Press “Hop” in the panel to switch."));
			}
			refreshPanel();
		});
	}

	@Override
	public void cancelRally()
	{
		RallyState r = activeRally;
		if (r == null)
		{
			return;
		}
		if (isMyRally() && partyService.isInParty())
		{
			RallyCall m = new RallyCall();
			m.rallyId = r.getId();
			m.callerName = r.getCallerName();
			m.cancel = true;
			send(m);
		}
		clearRally();
		refreshPanel();
	}

	/** Hvor lenge musikken tones ned før kallet, og opp igjen etter (millisekunder). */
	private static final int DUCK_FADE_DOWN_MS = 200;
	private static final int DUCK_FADE_UP_MS = 200;
	private static final int DUCK_STEP_MS = 20;

	private volatile int savedMusicVolume = -1;
	private volatile int currentMusicVolume = -1;
	private final java.util.concurrent.atomic.AtomicInteger duckGeneration = new java.util.concurrent.atomic.AtomicInteger();

	/**
	 * Spiller kallet til krig. Musikken tones først ned, så spilles lyden,
	 * og til slutt tones musikken gradvis opp igjen.
	 */
	private void playRallySound()
	{
		if (!config.rallySound())
		{
			return;
		}
		if (!config.rallyDuckMusic())
		{
			RallySound.play(executor, config.rallyVolume());
			return;
		}
		clientThread.invokeLater(() ->
		{
			int gen = duckGeneration.incrementAndGet();
			if (savedMusicVolume < 0)
			{
				int vol = client.getMusicVolume();
				if (vol <= 0)
				{
					RallySound.play(executor, config.rallyVolume()); // musikken er allerede av
					return;
				}
				savedMusicVolume = vol;
				currentMusicVolume = vol;
			}
			int from = currentMusicVolume;
			int low = savedMusicVolume / 6;
			int volume = config.rallyVolume();
			long clipMs = RallySound.durationMs();

			// 1) ton ned musikken
			fadeMusic(gen, from, low, DUCK_FADE_DOWN_MS, 0);
			// 2) spill kallet når musikken er nede
			executor.schedule(() ->
			{
				if (duckGeneration.get() == gen)
				{
					RallySound.play(executor, volume);
				}
			}, DUCK_FADE_DOWN_MS, TimeUnit.MILLISECONDS);
			// 3) ton musikken opp igjen når kallet er ferdig
			fadeMusic(gen, low, savedMusicVolume, DUCK_FADE_UP_MS, DUCK_FADE_DOWN_MS + clipMs);
			executor.schedule(() -> clientThread.invokeLater(() ->
			{
				if (duckGeneration.get() == gen)
				{
					restoreMusic();
				}
			}), DUCK_FADE_DOWN_MS + clipMs + DUCK_FADE_UP_MS + DUCK_STEP_MS, TimeUnit.MILLISECONDS);
		});
	}

	/** Endrer musikkvolumet gradvis fra -> to over durationMs, med start etter delayMs. */
	private void fadeMusic(int gen, int from, int to, int durationMs, long delayMs)
	{
		int steps = Math.max(1, durationMs / DUCK_STEP_MS);
		for (int i = 1; i <= steps; i++)
		{
			double t = (double) i / steps;
			// myk kurve (ease in/out) så overgangen ikke føles brå
			double eased = t * t * (3 - 2 * t);
			int v = (int) Math.round(from + (to - from) * eased);
			executor.schedule(() -> clientThread.invokeLater(() ->
			{
				if (duckGeneration.get() == gen && savedMusicVolume >= 0)
				{
					client.setMusicVolume(v);
					currentMusicVolume = v;
				}
			}), delayMs + (long) i * DUCK_STEP_MS, TimeUnit.MILLISECONDS);
		}
	}

	private void restoreMusic()
	{
		if (savedMusicVolume >= 0)
		{
			client.setMusicVolume(savedMusicVolume);
			savedMusicVolume = -1;
			currentMusicVolume = -1;
		}
	}

	private void setRally(RallyState rally)
	{
		activeRally = rally;
		clientThread.invokeLater(() ->
		{
			updateShortestPath(rally);
			updateTeleportAdvice();
		});
		clientThread.invokeLater(() ->
		{
			if (rallyMapPoint != null)
			{
				worldMapPointManager.remove(rallyMapPoint);
			}
			// SweatNation-banneret markerer rallypunktet
			rallyMapPoint = new WorldMapPoint(rally.getTarget(), rallyBanner);
			rallyMapPoint.setName("Rohirrim!");
			rallyMapPoint.setTooltip("Rohirrim!: " + rally.getCallerName() + L.t(" (verden ", " (world ") + rally.getWorld() + ")");
			rallyMapPoint.setSnapToEdge(true);
			rallyMapPoint.setJumpOnClick(true);
			rallyMapPoint.setImagePoint(new Point(2, rallyBanner.getHeight() - 1)); // foten av flaggstanga
			worldMapPointManager.add(rallyMapPoint);
		});
	}

	/** Ber Shortest Path-pluginen tegne raskeste vei (med teleporter) til punktet. */
	private void updateShortestPath(RallyState rally)
	{
		boolean want = rally != null && config.rallyShortestPath() && !isLocal(rally.getCallerId());
		if (want)
		{
			Map<String, Object> data = new java.util.HashMap<>();
			data.put("target", rally.getTarget());
			eventBus.post(new PluginMessage("shortestpath", "path", data));
			shortestPathActive = true;
		}
		else if (shortestPathActive)
		{
			eventBus.post(new PluginMessage("shortestpath", "clear"));
			shortestPathActive = false;
		}
	}

	@Override
	public boolean isShortestPathAvailable()
	{
		for (Plugin pl : pluginManager.getPlugins())
		{
			PluginDescriptor d = pl.getClass().getAnnotation(PluginDescriptor.class);
			if (d != null && "Shortest Path".equals(d.name()) && pluginManager.isPluginEnabled(pl))
			{
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ raskeste teleport

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		String where;
		switch (e.getContainerId())
		{
			case 93: where = "inventory"; break;
			case 94: where = "utstyr"; break;
			case 95: where = "banken"; break;
			default: return;
		}
		ItemContainer c = e.getItemContainer();
		Set<Integer> ids = new HashSet<>();
		Map<Integer, Integer> counts = new java.util.HashMap<>();
		if (c != null)
		{
			for (Item it : c.getItems())
			{
				if (it.getId() > 0 && it.getQuantity() > 0)
				{
					ids.add(it.getId());
					counts.merge(it.getId(), it.getQuantity(), Integer::sum);
				}
			}
		}
		ownedItems.put(where, ids);
		ownedCounts.put(where, counts);
		if (e.getContainerId() == 94 && c != null)
		{
			Item[] eq = c.getItems();
			weaponId = eq.length > 3 ? eq[3].getId() : -1; // plass 3 = våpen
			Set<String> staff = new HashSet<>();
			for (int slot : new int[]{3, 5}) // våpen og skjold (tome)
			{
				String[] gives = eq.length > slot ? Teleports.STAVES.get(eq[slot].getId()) : null;
				if (gives != null)
				{
					staff.addAll(java.util.Arrays.asList(gives));
				}
			}
			staffRunes = staff;
		}
		updateTeleportAdvice();
	}

	/** Navnet på en gjenstand med små bokstaver (hurtigbufret). Kalles på klienttråden. */
	String itemName(int itemId)
	{
		String n = itemNames.get(itemId);
		if (n == null)
		{
			ItemComposition comp = itemManager.getItemComposition(itemManager.canonicalize(itemId));
			n = comp == null || comp.getName() == null ? "" : comp.getName().toLowerCase();
			itemNames.put(itemId, n);
		}
		return n.isEmpty() ? null : n;
	}

	private static final int[] POUCH_RUNE_VARBITS = {29, 1622, 1623, 14285, 15373, 15374};
	private static final int[] POUCH_AMOUNT_VARBITS = {1624, 1625, 1626, 14286, 15375, 15376};
	private static final int RUNEPOUCH_RUNE_ENUM = 982;
	private static final String[] SPELLBOOKS = {"standard", "ancient", "lunar", "arceuus"};

	/** Leser rune pouch og spellbook. Kalles på klienttråden; oppdaterer rådet hvis noe er endret. */
	private void readMagicState()
	{
		Map<Integer, Integer> pouch = new java.util.HashMap<>();
		net.runelite.api.EnumComposition runeEnum = client.getEnum(RUNEPOUCH_RUNE_ENUM);
		for (int i = 0; i < POUCH_RUNE_VARBITS.length && runeEnum != null; i++)
		{
			int rune = client.getVarbitValue(POUCH_RUNE_VARBITS[i]);
			int amount = client.getVarbitValue(POUCH_AMOUNT_VARBITS[i]);
			if (rune > 0 && amount > 0)
			{
				int id = runeEnum.getIntValue(rune);
				if (id > 0)
				{
					pouch.merge(id, amount, Integer::sum);
				}
			}
		}
		// Runene i posen teller bare når posen er i inventory
		Set<Integer> inv = ownedItems.getOrDefault("inventory", Set.of());
		boolean hasPouch = false;
		for (int id : Teleports.RUNE_POUCHES)
		{
			hasPouch |= inv.contains(id);
		}
		if (!hasPouch)
		{
			pouch.clear();
		}
		int book = client.getVarbitValue(4070);
		String sb = book >= 0 && book < SPELLBOOKS.length ? SPELLBOOKS[book] : null;
		if (!pouch.equals(pouchRunes) || !java.util.Objects.equals(sb, spellbook))
		{
			pouchRunes = pouch;
			spellbook = sb;
			updateTeleportAdvice();
		}
	}

	private void updateTeleportAdvice()
	{
		RallyState r = activeRally;
		if (r == null || !config.rallyTeleport() || isLocal(r.getCallerId()))
		{
			teleportAdvice = null;
			return;
		}
		int magic = client.getGameState() == GameState.LOGGED_IN ? client.getRealSkillLevel(Skill.MAGIC) : 0;
		Teleports.Stock stock = new Teleports.Stock(ownedCounts, pouchRunes, staffRunes, spellbook);
		teleportAdvice = Teleports.advise(r.getTarget(), ownedItems, stock, magic);
		refreshPanel();
	}

	Set<Integer> teleportHighlights()
	{
		Teleports.Advice a = teleportAdvice;
		return a == null ? Set.of() : a.highlightIds;
	}

	@Override
	public List<String> getTeleportAdvice()
	{
		Teleports.Advice a = teleportAdvice;
		return a == null ? List.of() : Teleports.describe(a);
	}

	@Override
	public String describePlace(WorldPoint p)
	{
		return Places.describe(p);
	}

	private void clearRally()
	{
		activeRally = null;
		teleportAdvice = null;
		clientThread.invokeLater(() -> updateShortestPath(null));
		clientThread.invokeLater(() ->
		{
			if (rallyMapPoint != null)
			{
				worldMapPointManager.remove(rallyMapPoint);
				rallyMapPoint = null;
			}
			client.clearHintArrow();
		});
	}

	// ------------------------------------------------------------------ grupper (påmelding)

	private static final int MAX_GROUP_SIZE = 100;

	/** Alle grupper som finnes i SWEAT NATION-gruppa akkurat nå, dine først. */
	@Override
	public List<GroupView> getGroups()
	{
		Map<String, List<GroupEntry>> entries = new java.util.LinkedHashMap<>();
		Map<String, List<String>> owners = new java.util.HashMap<>();
		String me = getLocalName();
		for (GroupEntry g : myGroups.values())
		{
			entries.computeIfAbsent(g.key, k -> new ArrayList<>()).add(g);
			owners.computeIfAbsent(g.key, k -> new ArrayList<>()).add(me);
		}
		for (Map.Entry<Long, List<GroupEntry>> e : remoteGroups.entrySet())
		{
			String n = signupNames.getOrDefault(e.getKey(), L.t("Ukjent", "Unknown"));
			for (GroupEntry g : e.getValue())
			{
				if (g.key == null || Activity.fromId(g.activity) == null)
				{
					continue;
				}
				entries.computeIfAbsent(g.key, k -> new ArrayList<>()).add(g);
				owners.computeIfAbsent(g.key, k -> new ArrayList<>()).add(n);
			}
		}
		List<GroupView> out = new ArrayList<>();
		for (Map.Entry<String, List<GroupEntry>> e : entries.entrySet())
		{
			String key = e.getKey();
			List<GroupEntry> list = e.getValue();
			List<String> names = owners.get(key);
			GroupEntry newest = list.get(0);
			for (GroupEntry g : list)
			{
				if (g.sizeAt > newest.sizeAt)
				{
					newest = g;
				}
			}
			Activity a = Activity.fromId(newest.activity);
			ReadyCheck rc = getReadyCheck(key);
			List<LobbyMember> lm = new ArrayList<>();
			for (int i = 0; i < list.size(); i++)
			{
				String n = names.get(i);
				LobbyMember member = new LobbyMember(n, TeamRole.of(list.get(i).role), rc == null ? null : rc.answerOf(n));
				member.offlineMinutesLeft = offlineMinutesLeft(n);
				lm.add(member);
			}
			String title = newest.name != null && !newest.name.isEmpty() ? newest.name : a.getDisplayName();
			out.add(new GroupView(key, a, title, Math.max(1, newest.size), newest.leader, lm, myGroups.containsKey(key)));
		}
		out.sort((x, y) -> x.mine != y.mine ? (x.mine ? -1 : 1) : Integer.compare(y.members.size(), x.members.size()));
		return out;
	}

	private GroupView groupView(String key)
	{
		for (GroupView g : getGroups())
		{
			if (g.key.equals(key))
			{
				return g;
			}
		}
		return null;
	}

	/** Lag en ny gruppe for en aktivitet – også når det finnes grupper for den fra før. */
	@Override
	public void createTeam(Activity a)
	{
		GroupEntry g = new GroupEntry();
		g.key = a.name() + "#" + UUID.randomUUID().toString().substring(0, 8);
		g.activity = a.name();
		g.size = a.getMinPlayers();
		g.sizeAt = System.currentTimeMillis();
		g.leader = getLocalName();
		g.role = TeamRole.ANY.name();
		addMyGroup(g);
	}

	@Override
	public void createCustomTeam(String name, int size)
	{
		String n = name == null ? "" : name.trim();
		if (n.isEmpty())
		{
			warn(L.t("Gi gruppa et navn først.", "Give the group a name first."));
			return;
		}
		GroupEntry g = new GroupEntry();
		g.key = Activity.CUSTOM.name() + "#" + UUID.randomUUID().toString().substring(0, 8);
		g.activity = Activity.CUSTOM.name();
		g.name = n.length() > 40 ? n.substring(0, 40) : n;
		g.size = Math.max(2, Math.min(MAX_GROUP_SIZE, size));
		g.sizeAt = System.currentTimeMillis();
		g.leader = getLocalName();
		g.role = TeamRole.ANY.name();
		addMyGroup(g);
	}

	@Override
	public void joinTeam(String key)
	{
		GroupView v = groupView(key);
		if (v == null || myGroups.containsKey(key))
		{
			return;
		}
		GroupEntry g = new GroupEntry();
		g.key = key;
		g.activity = v.activity.name();
		g.name = v.activity == Activity.CUSTOM ? v.title : "";
		g.size = v.size;
		g.sizeAt = 0; // vi har ikke endret størrelsen selv
		g.leader = v.leader;
		g.role = TeamRole.ANY.name();
		addMyGroup(g);
	}

	private void addMyGroup(GroupEntry g)
	{
		myGroups.put(g.key, g);
		if (!partyService.isInParty())
		{
			chat(L.t("Gruppa er laget. Bli med i SWEAT NATION-gruppa så andre ser den.", "Group created. Join the SWEAT NATION group so others can see it."));
		}
		broadcastSignups();
		checkMatches();
		refreshPanel();
	}

	@Override
	public void leaveTeam(String key)
	{
		if (myGroups.remove(key) != null)
		{
			announcedMatches.remove(key);
			readyChecks.remove(key);
			broadcastSignups();
			refreshPanel();
		}
	}

	/** Endre ønsket gruppestørrelse med pil opp/ned. Nyeste endring gjelder for alle i gruppa. */
	@Override
	public void changeTeamSize(String key, int delta)
	{
		GroupEntry g = myGroups.get(key);
		GroupView v = groupView(key);
		if (g == null || v == null)
		{
			return;
		}
		int max = v.activity.getMaxPlayers() > 0 ? v.activity.getMaxPlayers() : MAX_GROUP_SIZE;
		int size = Math.max(1, Math.min(max, v.size + delta));
		if (size == v.size)
		{
			return;
		}
		g.size = size;
		g.sizeAt = System.currentTimeMillis();
		announcedMatches.remove(key);
		broadcastSignups();
		checkMatches();
		refreshPanel();
	}

	private void broadcastSignups()
	{
		if (!partyService.isInParty() || partyService.getLocalMember() == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			Player lp = client.getLocalPlayer();
			SignupUpdate m = new SignupUpdate();
			m.name = getLocalName();
			m.combatLevel = lp != null ? lp.getCombatLevel() : 0;
			m.world = client.getWorld();
			m.groups = new ArrayList<>();
			for (GroupEntry g : myGroups.values())
			{
				m.groups.add(g.copy());
			}
			send(m);
		});
	}

	/** Sier fra når en av gruppene dine er full. */
	private void checkMatches()
	{
		PartyMember me = partyService.getLocalMember();
		for (GroupView v : getGroups())
		{
			if (!v.mine)
			{
				continue;
			}
			if (v.members.size() < v.size)
			{
				announcedMatches.remove(v.key);
				continue;
			}
			if (!announcedMatches.add(v.key))
			{
				continue;
			}
			List<String> names = v.names();
			chat(L.t("Gruppe klar for ", "Group ready for ") + v.title + "! (" + String.join(", ", names) + ")");
			notifier.notify(L.t("SWEAT NATION: Gruppe klar for ", "SWEAT NATION: Group ready for ") + v.title + " – " + names.size() + L.t(" med", " joined"));
		}
	}

	// ------------------------------------------------------------------ split loot

	@Override
	public SplitSession getSplit()
	{
		return split;
	}

	@Override
	public boolean isInSplit()
	{
		SplitSession s = split;
		return s != null && s.hasParticipant(getLocalName());
	}

	@Override
	public void startSplit(String title)
	{
		startSplit(title, List.of());
	}

	@Override
	public void startSplit(String title, List<String> participants)
	{
		if (!partyService.isInParty())
		{
			warn(L.t("Bli med i gruppa først, så kan klanen se splitten.", "Join the group first so the clan can see the split."));
			return;
		}
		clientThread.invokeLater(() ->
		{
			String t = title == null || title.trim().isEmpty() ? L.t("Gruppetur", "Group trip") : title.trim();
			String me = getLocalName();
			SplitSession s = new SplitSession(UUID.randomUUID().toString(), t, me);
			s.addParticipant(me);
			for (String p : participants)
			{
				s.addParticipant(p);
			}
			split = s;
			SplitControl m = new SplitControl();
			m.sessionId = s.getId();
			m.action = "START";
			m.player = me;
			m.title = t;
			m.participants = s.getParticipants();
			send(m);
			chat(L.t("Vipps-oppgjør startet for ", "Vipps split started for ") + t + L.t(". Loot du får telles automatisk.", ". Loot you get is counted automatically."));
			refreshPanel();
		});
	}

	@Override
	public void joinSplit()
	{
		sendSplitAction("JOIN");
	}

	@Override
	public void leaveSplit()
	{
		sendSplitAction("LEAVE");
	}

	@Override
	public void stopSplit()
	{
		SplitSession s = split;
		if (s == null)
		{
			return;
		}
		sendSplitAction("STOP");
	}

	@Override
	public void closeSplit()
	{
		split = null;
		refreshPanel();
	}

	@Override
	public void copySplitSummary()
	{
		SplitSession s = split;
		if (s != null)
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s.summary()), null);
			chat(L.t("Oppgjøret er kopiert – lim det inn i Discord.", "Settlement copied – paste it into Discord."));
		}
	}

	private void sendSplitAction(String action)
	{
		SplitSession s = split;
		if (s == null || !partyService.isInParty())
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			String me = getLocalName();
			switch (action)
			{
				case "JOIN": s.addParticipant(me); break;
				case "LEAVE": s.removeParticipant(me); break;
				case "STOP": s.end(); break;
				default: break;
			}
			SplitControl m = new SplitControl();
			m.sessionId = s.getId();
			m.action = action;
			m.player = me;
			m.title = s.getTitle();
			send(m);
			refreshPanel();
		});
	}

	// ------------------------------------------------------------------ lobby og klar-sjekk

	@Override
	public void cycleRole(String key)
	{
		GroupEntry g = myGroups.get(key);
		if (g == null)
		{
			return;
		}
		g.role = TeamRole.of(g.role).next().name();
		broadcastSignups();
		refreshPanel();
	}

	@Override
	public ReadyCheck getReadyCheck(String key)
	{
		ReadyCheck rc = readyChecks.get(key);
		return rc == null || rc.isExpired() ? null : rc; // utløper etter 30 s, så kan en ny startes
	}

	@Override
	public void startReadyCheck(String key)
	{
		ReadyCheck running = getReadyCheck(key);
		if (running != null && !running.isCompleted())
		{
			return; // en klar-sjekk pågår allerede
		}
		if (!partyService.isInParty())
		{
			warn(L.t("Bli med i SWEAT NATION-gruppa først.", "Join the SWEAT NATION group first."));
			return;
		}
		clientThread.invokeLater(() ->
		{
			String me = getLocalName();
			ReadyCheck rc = new ReadyCheck(UUID.randomUUID().toString(), me);
			rc.answer(me, true); // den som starter er klar
			readyChecks.put(key, rc);
			ReadyCheckMsg m = new ReadyCheckMsg();
			m.activity = key;
			m.checkId = rc.getId();
			m.action = "START";
			m.player = me;
			m.ready = true;
			send(m);
			checkAllReady(key);
			refreshPanel();
		});
	}

	@Override
	public void answerReadyCheck(String key, boolean ready)
	{
		ReadyCheck rc = readyChecks.get(key);
		if (rc == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			String me = getLocalName();
			rc.answer(me, ready);
			ReadyCheckMsg m = new ReadyCheckMsg();
			m.activity = key;
			m.checkId = rc.getId();
			m.action = "ANSWER";
			m.player = me;
			m.ready = ready;
			send(m);
			checkAllReady(key);
			refreshPanel();
		});
	}

	@Subscribe
	public void onReadyCheckMsg(ReadyCheckMsg m)
	{
		if (isLocal(m.sender) || m.activity == null)
		{
			return;
		}
		String key = m.activity;
		if ("START".equals(m.action))
		{
			ReadyCheck rc = new ReadyCheck(m.checkId, m.player);
			rc.answer(m.player, true);
			readyChecks.put(key, rc);
			if (myGroups.containsKey(key))
			{
				GroupView v = groupView(key);
				String title = v != null ? v.title : "gruppa";
				notifier.notify(L.t("SWEAT NATION: ready check for ", "SWEAT NATION: ready check for ") + title + L.t(" – er du klar?", " – are you ready?"));
				chat(m.player + L.t(" startet ready check for ", " started a ready check for ") + title + L.t(". Svar i Gruppe-fanen.", ". Answer in the Group tab."));
			}
		}
		else
		{
			ReadyCheck rc = readyChecks.get(key);
			if (rc == null || !rc.getId().equals(m.checkId))
			{
				return;
			}
			rc.answer(m.player, m.ready);
		}
		checkAllReady(key);
		refreshPanel();
	}

	/** Når alle i gruppa har svart «Klar», spilles Rohirrim!-lyden for gruppa (ingen markør på kartet). */
	private void checkAllReady(String key)
	{
		ReadyCheck rc = readyChecks.get(key);
		GroupView v = groupView(key);
		if (rc == null || rc.isCompleted() || v == null || v.members.size() < 2)
		{
			return;
		}
		for (LobbyMember lm : v.members)
		{
			if (!Boolean.TRUE.equals(lm.ready))
			{
				return;
			}
		}
		rc.complete();
		chat(L.t("Alle er klare for ", "Everyone is ready for ") + v.title + "!");
		if (myGroups.containsKey(key))
		{
			playRallySound();
		}
	}

	// ------------------------------------------------------------------ lag-HUD

	private void updateOwnStatus(String name)
	{
		if (name == null)
		{
			return;
		}
		int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
		int spec = client.getVarpValue(300) / 10; // VarPlayer SPECIAL_ATTACK_PERCENT (0–1000)

		TeamMember self = teamStatus.computeIfAbsent(name, TeamMember::new);
		self.hp = hp;
		self.maxHp = maxHp;
		self.prayer = prayer;
		self.maxPrayer = maxPrayer;
		self.spec = spec;
		self.world = client.getWorld();
		self.lastUpdate = System.currentTimeMillis();

		// Status deles med hele gruppa (ikke bare laget), så HUD-en også virker for klanmedlemmer i nærheten
		boolean inTeam = partyService.isInParty() && partyService.getLocalMember() != null;
		if (lastSpec >= 0 && spec < lastSpec && weaponId > 0)
		{
			// Spec-energien falt: et spesialangrep ble brukt med våpenet du holder
			self.specWeapon = weaponId;
			self.specAt = System.currentTimeMillis();
			if (inTeam)
			{
				SpecUsed su = new SpecUsed();
				su.name = name;
				su.weaponId = weaponId;
				send(su);
			}
		}
		lastSpec = spec;
		updateBuffs(self, name, inTeam);

		ticksSinceStatus++;
		boolean changed = hp != lastSentHp || prayer != lastSentPrayer || spec != lastSentSpec;
		if (inTeam && ((changed && ticksSinceStatus >= 3) || ticksSinceStatus >= 30))
		{
			TeamStatus ts = new TeamStatus();
			ts.name = name;
			ts.hp = hp;
			ts.maxHp = maxHp;
			ts.prayer = prayer;
			ts.maxPrayer = maxPrayer;
			ts.spec = spec;
			ts.world = self.world;
			send(ts);
			lastSentHp = hp;
			lastSentPrayer = prayer;
			lastSentSpec = spec;
			ticksSinceStatus = 0;
		}
	}

	@Subscribe
	public void onTeamStatus(TeamStatus m)
	{
		if (isLocal(m.sender) || m.name == null)
		{
			return;
		}
		TeamMember t = teamStatus.computeIfAbsent(m.name, TeamMember::new);
		t.hp = m.hp;
		t.maxHp = m.maxHp;
		t.prayer = m.prayer;
		t.maxPrayer = m.maxPrayer;
		t.spec = m.spec;
		t.world = m.world;
		t.lastUpdate = System.currentTimeMillis();
	}

	@Subscribe
	public void onSpecUsed(SpecUsed m)
	{
		if (isLocal(m.sender) || m.name == null)
		{
			return;
		}
		TeamMember t = teamStatus.computeIfAbsent(m.name, TeamMember::new);
		t.specWeapon = m.weaponId;
		t.specAt = System.currentTimeMillis();
	}

	/** Gruppa di i HUD-en: den første gruppa du er med i som har minst ett annet medlem. */
	GroupView teamGroup()
	{
		for (GroupView v : getGroups())
		{
			if (v.mine && v.members.size() >= 2)
			{
				return v;
			}
		}
		return null;
	}

	/** Lagfargen til gruppa di (minikartet), eller null når du ikke er i en gruppe. */
	java.awt.Color teamColor()
	{
		GroupView v = teamGroup();
		return v == null ? null : v.activity.teamColor();
	}

	/** Det HUD-en skal vise: gruppa di, eller klanmedlemmer i nærheten når du ikke er i en gruppe. */
	static final class HudView
	{
		final String title;
		final java.awt.Color color;
		final List<TeamMember> members;

		HudView(String title, java.awt.Color color, List<TeamMember> members)
		{
			this.title = title;
			this.color = color;
			this.members = members;
		}
	}

	HudView hudView()
	{
		GroupView team = teamGroup();
		if (team != null)
		{
			if (!config.teamHud())
			{
				return null;
			}
			// 10000 = ingen grense: hele gruppa, uansett hvor de er
			List<TeamMember> out = statusWithin(new HashSet<>(team.names()), tileLimit(config.hudTeamTiles()));
			return new HudView(team.title.toUpperCase(), team.activity.teamColor(), out);
		}
		if (!config.hudNearby())
		{
			return null;
		}
		List<TeamMember> out = statusWithin(null, tileLimit(config.hudNearbyTiles()));
		return new HudView(L.t("Sweats nær deg", "Sweats near you"), MapMarkers.BANNER_PURPLE.brighter(), out);
	}

	/** 10000 = alltid (ingen avstandsgrense, og live-kart trengs ikke). Ellers 0–9999 ruter (0 = samme rute). */
	private static int tileLimit(int setting)
	{
		return setting >= 10000 ? -1 : Math.max(0, setting);
	}

	/**
	 * Deg selv + medlemmer med fersk status. names = bare disse (null = alle i SWEAT NATION-gruppa).
	 * maxTiles < 0 = ingen avstandsgrense; ellers må de være på samme verden og etasje, innen så mange ruter
	 * (det krever at de har live-kartet på).
	 */
	private List<TeamMember> statusWithin(Set<String> names, int maxTiles)
	{
		long now = System.currentTimeMillis();
		String myName = getLocalName();
		WorldPoint me = localLocation;
		List<TeamMember> out = new ArrayList<>();
		TeamMember self = teamStatus.get(myName);
		if (self != null && self.maxHp > 0)
		{
			out.add(self);
		}
		Map<String, MemberState> byName = new java.util.HashMap<>();
		for (MemberState m : members.values())
		{
			byName.put(m.getName(), m);
		}
		List<TeamMember> others = new ArrayList<>();
		for (TeamMember t : teamStatus.values())
		{
			if (t.name.equals(myName) || t.maxHp <= 0 || now - t.lastUpdate > 120_000)
			{
				continue;
			}
			if (names != null && !names.contains(t.name))
			{
				continue;
			}
			if (maxTiles >= 0)
			{
				MemberState ms = byName.get(t.name);
				WorldPoint wp = ms == null ? null : ms.getLocation();
				if (me == null || wp == null || ms.getWorld() != localWorld || wp.getPlane() != me.getPlane()
					|| wp.distanceTo2D(me) > maxTiles)
				{
					continue;
				}
			}
			others.add(t);
		}
		others.sort(Comparator.comparing(t -> t.name, String.CASE_INSENSITIVE_ORDER));
		out.addAll(others);
		return out;
	}

	double hudScale()
	{
		return config.hudScale() / 100.0;
	}

	boolean teamMinimapEnabled()
	{
		return config.teamMinimap();
	}

	/** Hvor lagkameratene er (fra live-kartet), bare de på samme verden og etasje som deg. */
	Map<String, WorldPoint> teamLocations()
	{
		Map<String, WorldPoint> out = new java.util.HashMap<>();
		GroupView team = teamGroup();
		WorldPoint me = localLocation;
		if (team == null || me == null)
		{
			return out;
		}
		Set<String> names = new HashSet<>(team.names());
		for (MemberState m : members.values())
		{
			WorldPoint wp = m.getLocation();
			if (wp != null && names.contains(m.getName()) && m.getWorld() == localWorld
				&& wp.getPlane() == me.getPlane() && wp.distanceTo2D(me) < 30)
			{
				out.put(m.getName(), wp);
			}
		}
		return out;
	}

	@Override
	public boolean isTeamHud()
	{
		return config.teamHud();
	}

	@Override
	public void setTeamHud(boolean on)
	{
		configManager.setConfiguration(CONFIG_GROUP, "teamHud", on);
	}

	@Override
	public int getHudScalePercent()
	{
		return config.hudScale();
	}

	@Override
	public void setHudScalePercent(int pct)
	{
		configManager.setConfiguration(CONFIG_GROUP, "hudScale", Math.max(50, Math.min(150, pct)));
	}

	private void flashRohirrim()
	{
		SweatNationPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(p::flashRohirrim);
		}
	}

	// ------------------------------------------------------------------ Discord

	/** Er du medlem av SWEAT NATION-klanen? Navnet sammenlignes uten mellomrom og store/små bokstaver. */
	private boolean inSweatNationClan()
	{
		String c = currentClan;
		return c != null && c.replace('\u00A0', ' ').replaceAll("[\\s_-]+", "").equalsIgnoreCase("sweatnation");
	}

	/**
	 * Discord-lenken fra innstillingene, gjort om til en full https-adresse.
	 * Bare lenker til Discord godtas (discord.gg, discord.com), så knappen aldri åpner noe annet.
	 */
	@Override
	public String getDiscordLink()
	{
		String link = config.discordLink().trim();
		if (link.isEmpty())
		{
			// Ingen egen lenke: medlemmer av SWEAT NATION-klanen får lenken et annet klanmedlem har delt
			String shared = sharedDiscordLink;
			return inSweatNationClan() && shared != null ? shared : "";
		}
		if (!link.startsWith("http://") && !link.startsWith("https://"))
		{
			link = "https://" + link;
		}
		link = link.replaceFirst("^http://", "https://");
		String host = link.substring("https://".length()).split("[/?#]", 2)[0].toLowerCase();
		boolean discord = host.equals("discord.gg") || host.equals("discord.com") || host.endsWith(".discord.com")
			|| host.equals("discordapp.com");
		return discord ? link : "";
	}

	/** Del din Discord-lenke med SWEAT NATION-gruppa – bare når du selv er medlem av klanen. */
	private void shareDiscordLink()
	{
		String own = config.discordLink().trim();
		if (own.isEmpty() || !inSweatNationClan() || !partyService.isInParty() || partyService.getLocalMember() == null)
		{
			return;
		}
		String link = getDiscordLink();
		if (link.isEmpty())
		{
			return; // ikke en gyldig Discord-lenke
		}
		DiscordShare m = new DiscordShare();
		m.link = link;
		send(m);
	}

	@Subscribe
	public void onDiscordShare(DiscordShare m)
	{
		if (isLocal(m.sender) || m.link == null || !inSweatNationClan())
		{
			return; // bare klanmedlemmer tar imot lenken
		}
		if (isDiscordUrl(m.link))
		{
			sharedDiscordLink = m.link;
			refreshPanel();
		}
	}

	private static boolean isDiscordUrl(String link)
	{
		if (!link.startsWith("https://"))
		{
			return false;
		}
		String host = link.substring("https://".length()).split("[/?#]", 2)[0].toLowerCase();
		return host.equals("discord.gg") || host.equals("discord.com") || host.endsWith(".discord.com")
			|| host.equals("discordapp.com");
	}

	@Override
	public boolean hasInvalidDiscordLink()
	{
		return !config.discordLink().trim().isEmpty() && getDiscordLink().isEmpty();
	}

	@Override
	public void openDiscord()
	{
		String link = getDiscordLink();
		if (!link.isEmpty())
		{
			LinkBrowser.browse(link);
		}
		else
		{
			warn(L.t("Legg inn en Discord-lenke under SWEAT NATION-innstillingene (Discord) først.", "Add a Discord link in the SWEAT NATION settings (Discord) first."));
		}
	}

	// ------------------------------------------------------------------ utlogging

	/** Etter så lang tid utlogget fjernes en spiller fra gruppene sine. */
	private static final long LOGGED_OUT_LIMIT_MS = 10 * 60_000L;
	/** Når du selv logget ut (0 = pålogget). */
	private volatile long loggedOutAt;
	/** Andre i gruppa som er logget ut: medlems-id -> når de logget ut. */
	private final Map<Long, Long> offlineSince = new ConcurrentHashMap<>();
	private final Map<Long, String> offlineNames = new ConcurrentHashMap<>();
	private ScheduledFuture<?> offlineTask;
	/** Navnet ditt fra sist du var logget inn (brukes når du står på innloggingsskjermen). */
	private volatile String lastLocalName;

	private void sendPresence(boolean online)
	{
		if (!partyService.isInParty() || partyService.getLocalMember() == null)
		{
			return;
		}
		Presence p = new Presence();
		p.online = online;
		Player lp = client.getLocalPlayer();
		p.name = lp != null && lp.getName() != null ? lp.getName() : lastLocalName;
		send(p);
	}

	@Subscribe
	public void onPresence(Presence m)
	{
		if (isLocal(m.sender))
		{
			return;
		}
		if (m.online)
		{
			offlineSince.remove(m.sender);
			offlineNames.remove(m.sender);
		}
		else
		{
			offlineSince.putIfAbsent(m.sender, System.currentTimeMillis());
			if (m.name != null)
			{
				offlineNames.put(m.sender, m.name);
			}
			removeMember(m.sender); // ikke lenger på kartet
		}
		refreshPanel();
	}

	/** Navnet til et annet medlem i gruppa, slik vi kjenner det fra påmeldinger eller kartet. */
	private String nameOf(long memberId)
	{
		String n = signupNames.get(memberId);
		if (n == null)
		{
			MemberState ms = members.get(memberId);
			n = ms != null ? ms.getName() : null;
		}
		if (n == null)
		{
			n = offlineNames.get(memberId);
		}
		return n;
	}

	private static String plainName(String name)
	{
		return name == null ? "" : Text.removeTags(name).replace('\u00A0', ' ').trim().toLowerCase();
	}

	/** Finn medlems-id-en i gruppa for et spillernavn (eller -1). */
	private long memberIdByName(String name)
	{
		String want = plainName(name);
		if (want.isEmpty() || !partyService.isInParty())
		{
			return -1;
		}
		PartyMember me = partyService.getLocalMember();
		for (PartyMember pm : partyService.getMembers())
		{
			if (me != null && pm.getMemberId() == me.getMemberId())
			{
				continue;
			}
			if (want.equals(plainName(nameOf(pm.getMemberId()))))
			{
				return pm.getMemberId();
			}
		}
		return -1;
	}

	/** Hvor lenge noen kan være ute av klanchatten før de regnes som utlogget (dekker world-hopping). */
	private static final long CLAN_LEAVE_GRACE_MS = 10_000;
	/** Klanmedlemmer som nettopp forlot klanchatten: medlems-id -> når. */
	private final Map<Long, Long> pendingLeave = new ConcurrentHashMap<>();

	/**
	 * Et klanmedlem forlot klanchatten. Er de ikke tilbake innen 10 sekunder (som ved world-hopping),
	 * regnes de som utlogget: de telles ikke lenger, men beholder gruppene sine i 10 minutter.
	 */
	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft e)
	{
		if (e.getClanMember() == null)
		{
			return;
		}
		String name = e.getClanMember().getName();
		long id = memberIdByName(name);
		if (id == -1 || offlineSince.containsKey(id))
		{
			return;
		}
		long at = System.currentTimeMillis();
		pendingLeave.put(id, at);
		executor.schedule(() ->
		{
			if (pendingLeave.remove(id, at) && offlineSince.putIfAbsent(id, at) == null)
			{
				offlineNames.put(id, name);
				removeMember(id); // ikke lenger på kartet
				refreshPanel();
			}
		}, CLAN_LEAVE_GRACE_MS, TimeUnit.MILLISECONDS);
	}

	/** Et klanmedlem kom tilbake til klanchatten (logget inn eller ferdig med world-hop). Alt fortsetter som før. */
	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined e)
	{
		if (e.getClanMember() == null)
		{
			return;
		}
		long id = memberIdByName(e.getClanMember().getName());
		if (id == -1)
		{
			return;
		}
		pendingLeave.remove(id); // innenfor grace-tiden: ingenting skjer
		if (offlineSince.remove(id) != null)
		{
			offlineNames.remove(id);
			lastSentLocation = null; // de ser oss på kartet igjen med en gang
			refreshPanel();
		}
	}

	/** Hvor mange minutter en utlogget spiller har igjen før de fjernes fra gruppa (-1 = pålogget). */
	private int offlineMinutesLeft(String name)
	{
		long since;
		if (plainName(name).equals(plainName(getLocalName())))
		{
			since = loggedOutAt;
		}
		else
		{
			long id = memberIdByName(name);
			Long t = id == -1 ? null : offlineSince.get(id);
			since = t == null ? 0 : t;
		}
		if (since == 0)
		{
			return -1;
		}
		long left = LOGGED_OUT_LIMIT_MS - (System.currentTimeMillis() - since);
		return (int) Math.max(1, (left + 59_999) / 60_000);
	}

	/** Kjøres hvert 30. sekund, også når du står på innloggingsskjermen. */
	private void checkLoggedOut()
	{
		long now = System.currentTimeMillis();
		// Selv: utlogget i over 10 minutter → meld deg ut av alle grupper
		if (loggedOutAt != 0 && now - loggedOutAt > LOGGED_OUT_LIMIT_MS && !myGroups.isEmpty())
		{
			myGroups.clear();
			announcedMatches.clear();
			readyChecks.clear();
			broadcastSignups();
			refreshPanel();
		}
		if (!offlineSince.isEmpty() || loggedOutAt != 0)
		{
			refreshPanel(); // minuttene i lobbyen teller ned
		}
		// Andre: utlogget i over 10 minutter → fjern dem fra gruppene hos oss (i tilfelle klienten deres ikke gjorde det)
		boolean changed = false;
		for (Map.Entry<Long, Long> e : offlineSince.entrySet())
		{
			if (now - e.getValue() <= LOGGED_OUT_LIMIT_MS)
			{
				continue;
			}
			long id = e.getKey();
			if (remoteGroups.remove(id) != null)
			{
				changed = true;
			}
			String name = offlineNames.get(id);
			if (name == null)
			{
				name = signupNames.get(id);
			}
			signupNames.remove(id);
			if (name != null && teamStatus.remove(name) != null)
			{
				changed = true;
			}
		}
		if (changed)
		{
			checkMatches();
			refreshPanel();
		}
	}

	// ------------------------------------------------------------------ boosts og buffs i HUD

	final BuffTracker buffTracker = new BuffTracker();
	private final Set<String> sentBuffIcons = ConcurrentHashMap.newKeySet();
	private String lastBuffSig;
	private int ticksSinceBuffs;

	/** Leser dine boosts og buffs, viser dem på deg selv og deler dem med gruppa når de endrer seg. */
	private void updateBuffs(TeamMember self, String name, boolean inTeam)
	{
		ticksSinceBuffs++;
		if (ticksSinceBuffs % 2 != 0)
		{
			return;
		}
		long now = System.currentTimeMillis();
		List<BuffEntry> mine = buffTracker.collect(client, skillIconManager, infoBoxManager);
		self.buffs = mine;
		self.buffsAt = now;
		if (!inTeam)
		{
			return;
		}
		// Send bare når noe faktisk endrer seg: nye/borte buffs, ny boost-verdi eller en timer som ble fornyet
		StringBuilder sig = new StringBuilder();
		for (BuffEntry e : mine)
		{
			sig.append(e.icon).append('|').append(e.text).append('|')
				.append(e.totalMs > 0 ? (now + e.remainingMs) / 3000 : 0).append(';');
		}
		String signature = sig.toString();
		if (signature.equals(lastBuffSig) && ticksSinceBuffs < 100)
		{
			return;
		}
		for (BuffEntry e : mine)
		{
			if (sentBuffIcons.add(e.icon))
			{
				BufferedImage img = buffTracker.icons.get(e.icon);
				String png = img == null ? null : BuffTracker.encode(img);
				if (png == null)
				{
					sentBuffIcons.remove(e.icon);
					continue;
				}
				BuffIcon bi = new BuffIcon();
				bi.id = e.icon;
				bi.png = png;
				send(bi);
			}
		}
		BuffState st = new BuffState();
		st.name = name;
		st.buffs = new ArrayList<>(mine);
		send(st);
		lastBuffSig = signature;
		ticksSinceBuffs = 0;
	}

	@Subscribe
	public void onBuffIcon(BuffIcon m)
	{
		if (isLocal(m.sender) || m.id == null || buffTracker.icons.containsKey(m.id) || buffTracker.icons.size() > 400)
		{
			return;
		}
		BufferedImage img = BuffTracker.decode(m.png);
		if (img != null)
		{
			buffTracker.icons.put(m.id, img);
		}
	}

	@Subscribe
	public void onBuffState(BuffState m)
	{
		if (isLocal(m.sender) || m.name == null)
		{
			return;
		}
		TeamMember t = teamStatus.computeIfAbsent(m.name, TeamMember::new);
		List<BuffEntry> list = m.buffs == null ? List.of() : m.buffs;
		t.buffs = list.size() > 24 ? new ArrayList<>(list.subList(0, 24)) : list;
		t.buffsAt = System.currentTimeMillis();
	}

	boolean showBuffs()
	{
		return config.hudBuffs();
	}

	// ------------------------------------------------------------------ hjelpere

	/** Sender en melding til gruppa med vår medlems-id som avsender. */
	private void send(com.sweatnation.messages.SnMessage m)
	{
		PartyMember me = partyService.getLocalMember();
		if (me == null)
		{
			return;
		}
		m.sender = me.getMemberId();
		partyService.send(m);
	}

	private boolean isLocal(long memberId)
	{
		PartyMember me = partyService.getLocalMember();
		return me != null && me.getMemberId() == memberId;
	}

	RallyState rallyForOverlay()
	{
		return config.rallyOverlay() ? activeRally : null;
	}

	/** Varsel i chatten. Vises bare hvis chatvarsler er slått på i innstillingene. */
	private void chat(String text)
	{
		if (config.chatMessages())
		{
			say(text);
		}
	}

	/** Beskjed når noe du trykket på ikke kunne gjøres. Vises alltid, ellers skjer det bare ingenting. */
	private void warn(String text)
	{
		say(text);
	}

	private void say(String text)
	{
		String msg = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("[SWEAT NATION] ")
			.append(ChatColorType.NORMAL)
			.append(text)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(msg)
			.build());
	}

	private void refreshPanel()
	{
		SweatNationPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(p::refresh);
		}
	}
}
