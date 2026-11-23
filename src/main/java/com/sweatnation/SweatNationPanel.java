package com.sweatnation;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.PluginPanel;

/** Sidepanelet: gruppe, medlemmer på kartet, rally, boss-påmelding og Discord. */
public class SweatNationPanel extends PluginPanel
{
	private enum Tab
	{
		MAP("Kart", "Map"), RALLY("Rohirrim!", "Rohirrim!"), BOSSES("Gruppe", "Group"), SPLIT("Vipps", "Vipps"),
		LOG("Logg", "Log"), DISCORD("Discord", "Discord");

		private final String no;
		private final String en;

		Tab(String no, String en)
		{
			this.no = no;
			this.en = en;
		}

		String label()
		{
			return L.t(no, en);
		}
	}

	private final SweatNationController ctl;

	private final JPanel groupBox = new JPanel();
	private final JTextField codeField = OsrsUi.textField(L.t("Gruppekode – alle som skriver samme kode havner i samme gruppe", "Group code – everyone who enters the same code ends up in the same group"));
	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);
	private final List<OsrsUi.StoneButton> tabButtons = new ArrayList<>();

	private final JPanel membersList = vertical();
	private final JPanel rallyStatus = vertical();
	private final JTextField rallyNote = OsrsUi.textField(L.t("Valgfri beskjed, f.eks. \"Nex om 5 min, ta med tank-gear\"", "Optional message, e.g. \"Nex in 5 min, bring tank gear\""));
	private final JPanel activityList = vertical();
	private final List<OsrsUi.StoneButton> categoryButtons = new ArrayList<>();
	private final JPanel discordStatus = vertical();
	private final JPanel liveBox = vertical();
	private final JPanel deathsList = vertical();
	private final JPanel splitBox = vertical();
	private final JPanel logList = vertical();
	private final JPanel logFilters = new JPanel(new GridLayout(0, 3, 2, 2));
	private final List<OsrsUi.StoneButton> logFilterButtons = new ArrayList<>();
	private ActivityEvent.Type logFilter;
	private OsrsUi.StoneButton rohirrimTab;
	private OsrsUi.StoneButton rohirrimReply;
	private float rohirrimGlow;
	private javax.swing.Timer glowTimer;
	private final JTextField splitTitle = OsrsUi.textField(L.t("F.eks. \"Nex\" eller \"ToB-kveld\"", "E.g. \"Nex\" or \"ToB night\""));
	private final java.awt.image.BufferedImage logo;
	private Activity.Category category = Activity.Category.ALL;
	/** Søkefeltet i gruppefinneren. Søker i valgt kategori («Alle» søker i alle). */
	private JTextField activitySearch = OsrsUi.searchField();

	public SweatNationPanel(SweatNationController ctl, java.awt.image.BufferedImage logo)
	{
		super();
		this.ctl = ctl;
		this.logo = logo;
		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		setBackground(OsrsUi.BG);

		JPanel north = vertical();
		north.add(buildTitle());
		north.add(Box.createVerticalStrut(6));
		groupBox.setLayout(new BoxLayout(groupBox, BoxLayout.Y_AXIS));
		groupBox.setOpaque(false);
		north.add(groupBox);
		north.add(Box.createVerticalStrut(6));
		north.add(buildTabs());
		add(north, BorderLayout.NORTH);

		cardPanel.setOpaque(false);
		cardPanel.add(buildMapCard(), Tab.MAP.name());
		cardPanel.add(buildRallyCard(), Tab.RALLY.name());
		cardPanel.add(buildBossCard(), Tab.BOSSES.name());
		cardPanel.add(buildSplitCard(), Tab.SPLIT.name());
		cardPanel.add(buildLogCard(), Tab.LOG.name());
		cardPanel.add(buildDiscordCard(), Tab.DISCORD.name());
		add(cardPanel, BorderLayout.CENTER);

		selectTab(Tab.MAP);
		refresh();
	}

	private Tab currentTab = Tab.MAP;

	/**
	 * Bygger hele panelet på nytt etter språkbytte, og blir stående på samme fane.
	 */
	public void rebuildForLanguage()
	{
		Tab keep = currentTab;
		removeAll();
		cardPanel.removeAll();
		tabButtons.clear();
		categoryButtons.clear();
		logFilterButtons.clear();
		logFilters.removeAll();
		codeField.setToolTipText(L.t("Gruppekode – alle som skriver samme kode havner i samme gruppe", "Group code – everyone who enters the same code ends up in the same group"));
		rallyNote.setToolTipText(L.t("Valgfri beskjed, f.eks. \"Nex om 5 min, ta med tank-gear\"", "Optional message, e.g. \"Nex in 5 min, bring tank gear\""));
		splitTitle.setToolTipText(L.t("F.eks. \"Nex\" eller \"ToB-kveld\"", "E.g. \"Nex\" or \"ToB night\""));
		customName.setToolTipText(L.t("F.eks. \"Barbarian Fishing\" eller \"Fight Caves-hjelp\"", "E.g. \"Barbarian Fishing\" or \"Fight Caves help\""));

		JPanel north = vertical();
		north.add(buildTitle());
		north.add(Box.createVerticalStrut(6));
		north.add(groupBox);
		north.add(Box.createVerticalStrut(6));
		north.add(buildTabs());
		add(north, BorderLayout.NORTH);
		cardPanel.add(buildMapCard(), Tab.MAP.name());
		cardPanel.add(buildRallyCard(), Tab.RALLY.name());
		cardPanel.add(buildBossCard(), Tab.BOSSES.name());
		cardPanel.add(buildSplitCard(), Tab.SPLIT.name());
		cardPanel.add(buildLogCard(), Tab.LOG.name());
		cardPanel.add(buildDiscordCard(), Tab.DISCORD.name());
		add(cardPanel, BorderLayout.CENTER);
		selectTab(keep);
		refresh();
	}

	// ------------------------------------------------------------------ oppbygging

	private JComponent buildTitle()
	{
		JPanel title = new JPanel(new BorderLayout(8, 0));
		title.setBackground(OsrsUi.BG_DARK);
		title.setBorder(BorderFactory.createCompoundBorder(OsrsUi.bevel(true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		if (logo != null)
		{
			title.add(new JLabel(new javax.swing.ImageIcon(logo)), BorderLayout.WEST);
		}
		OsrsUi.ShadowLabel name = OsrsUi.label("SWEAT NATION", OsrsUi.bold(), OsrsUi.PURPLE);
		name.setFont(OsrsUi.bold().deriveFont(18f));
		title.add(name, BorderLayout.CENTER);
		return full(title);
	}

	private JComponent buildTabs()
	{
		// To rader med tre like brede faner, så alle boksene ligger på linje:
		// Kart | Rohirrim! | Gruppe
		// Split | Logg | Chat
		JPanel tabs = new JPanel(new GridLayout(2, 3, 2, 2));
		tabs.setOpaque(false);
		Tab[] order = {Tab.MAP, Tab.RALLY, Tab.BOSSES, Tab.SPLIT, Tab.LOG, Tab.DISCORD};
		OsrsUi.StoneButton[] byTab = new OsrsUi.StoneButton[Tab.values().length];
		for (Tab t : order)
		{
			OsrsUi.StoneButton b = new OsrsUi.StoneButton(t.label());
			b.setMargin(new java.awt.Insets(6, 2, 6, 2));
			if (t == Tab.RALLY)
			{
				b.setFont(OsrsUi.bold());
				b.textColor(new Color(0xFF, 0x70, 0x40));
				rohirrimTab = b;
			}
			b.addActionListener(e -> selectTab(t));
			byTab[t.ordinal()] = b;
			tabs.add(b);
		}
		for (OsrsUi.StoneButton b : byTab)
		{
			tabButtons.add(b);
		}
		return full(tabs);
	}

	private JComponent buildMapCard()
	{
		JPanel card = vertical();
		card.add(liveBox);
		card.add(Box.createVerticalStrut(6));
		card.add(OsrsUi.header(L.t("Sweats på kartet", "Sweats on the map")));
		card.add(Box.createVerticalStrut(4));
		card.add(membersList);
		card.add(Box.createVerticalStrut(6));
		card.add(OsrsUi.header(L.t("Siste dødsfall", "Recent deaths")));
		card.add(Box.createVerticalStrut(4));
		card.add(deathsList);
		return top(card);
	}

	private static final ActivityEvent.Type[] LOG_FILTERS = {
		null, ActivityEvent.Type.KILL, ActivityEvent.Type.LOOT,
		ActivityEvent.Type.CLUE, ActivityEvent.Type.QUEST, ActivityEvent.Type.LEVEL
	};

	private JComponent buildLogCard()
	{
		JPanel card = vertical();
		card.add(OsrsUi.header(L.t("Rikets tilstand", "State of the realm")));
		card.add(Box.createVerticalStrut(4));
		logFilters.setOpaque(false);
		for (ActivityEvent.Type t : LOG_FILTERS)
		{
			OsrsUi.StoneButton b = new OsrsUi.StoneButton(t == null ? L.t("Alle", "All") : t.getLabel()).small();
			b.addActionListener(e ->
			{
				logFilter = t;
				refresh();
			});
			logFilterButtons.add(b);
			logFilters.add(b);
		}
		card.add(full(logFilters));
		card.add(Box.createVerticalStrut(6));
		card.add(logList);
		return top(card);
	}

	private static Color typeColor(ActivityEvent.Type t)
	{
		switch (t)
		{
			case KILL: return OsrsUi.ORANGE;
			case LOOT: return ColorSchemeGe.GE;
			case CLUE: return OsrsUi.CYAN;
			case QUEST: return OsrsUi.PURPLE;
			case LEVEL: return OsrsUi.GREEN;
			case PET: return new Color(0xFF, 0x7F, 0xD0);
			case COLLECTION: return new Color(0xFF, 0xD7, 0x40);
			case DEATH: return OsrsUi.RED;
			default: return OsrsUi.TEXT;
		}
	}

	/** GE-grønn som RuneLite bruker for priser. */
	private static final class ColorSchemeGe
	{
		static final Color GE = net.runelite.client.ui.ColorScheme.GRAND_EXCHANGE_PRICE;
	}

	/** Spillere med utvidet logg (klikk på navnet). */
	private final java.util.Set<String> expandedPlayers = new java.util.HashSet<>();
	private static final int EXPANDED_EVENTS = 10;

	private void refreshLog()
	{
		for (int i = 0; i < logFilterButtons.size(); i++)
		{
			logFilterButtons.get(i).setActive(LOG_FILTERS[i] == logFilter);
		}
		logList.removeAll();
		long minLoot = ctl.getMinLootLog();

		// Grupper per spiller (nyeste først), med samme filter som er valgt
		java.util.LinkedHashMap<String, List<ActivityEvent>> byPlayer = new java.util.LinkedHashMap<>();
		for (ActivityEvent e : ctl.getActivity())
		{
			ActivityEvent.Type t = e.getType();
			if (logFilter != null && t != logFilter)
			{
				continue;
			}
			if (t == ActivityEvent.Type.LOOT && e.value < minLoot)
			{
				continue; // også eldre oppføringer under grensen skjules
			}
			byPlayer.computeIfAbsent(e.player == null ? "?" : e.player, k -> new ArrayList<>()).add(e);
		}

		java.text.SimpleDateFormat today = new java.text.SimpleDateFormat("HH:mm");
		java.text.SimpleDateFormat older = new java.text.SimpleDateFormat("dd.MM HH:mm");
		long startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

		for (java.util.Map.Entry<String, List<ActivityEvent>> en : byPlayer.entrySet())
		{
			String player = en.getKey();
			List<ActivityEvent> events = en.getValue();
			ActivityEvent latest = events.get(0);
			boolean open = expandedPlayers.contains(player);
			int extra = Math.min(events.size(), EXPANDED_EVENTS) - 1;

			JPanel row = OsrsUi.inset(new BorderLayout(0, 1));
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, typeColor(latest.getType())),
				BorderFactory.createEmptyBorder(4, 7, 4, 6)));

			// Spillernavnet: klikk for å vise de siste hendelsene
			JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			String arrow = extra > 0 ? (open ? "▾ " : "▸ ") : "";
			String count = extra > 0 && !open ? "  (+" + extra + ")" : "";
			top.add(OsrsUi.label(arrow + player + count, OsrsUi.bold(), Color.WHITE), BorderLayout.WEST);
			top.add(OsrsUi.label(fmtTime(latest.time, startOfDay, today, older), OsrsUi.small(), OsrsUi.TEXT_DIM), BorderLayout.EAST);
			if (extra > 0)
			{
				top.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
				top.setToolTipText(open ? L.t("Skjul", "Hide") : L.t("Vis de siste ", "Show the last ") + (extra + 1) + L.t(" hendelsene til ", " events for ") + player);
				top.addMouseListener(new java.awt.event.MouseAdapter()
				{
					@Override
					public void mouseClicked(java.awt.event.MouseEvent ev)
					{
						if (!expandedPlayers.remove(player))
						{
							expandedPlayers.add(player);
						}
						refresh();
					}
				});
			}
			row.add(top, BorderLayout.NORTH);

			JPanel body = vertical();
			body.add(eventLine(latest, null));
			if (open)
			{
				for (int i = 1; i <= extra; i++)
				{
					ActivityEvent e = events.get(i);
					body.add(Box.createVerticalStrut(3));
					body.add(eventLine(e, fmtTime(e.time, startOfDay, today, older)));
				}
			}
			row.add(body, BorderLayout.CENTER);
			logList.add(full(row));
			logList.add(Box.createVerticalStrut(2));
		}
		if (byPlayer.isEmpty())
		{
			logList.add(hint(L.t("Ingen sweat enda", "No sweat yet")));
		}
	}

	private static String fmtTime(long t, long startOfDay, java.text.SimpleDateFormat today, java.text.SimpleDateFormat older)
	{
		return (t >= startOfDay ? today : older).format(new java.util.Date(t));
	}

	/** Én hendelse i loggen; eldre hendelser får tidspunktet foran. */
	private static JComponent eventLine(ActivityEvent e, String time)
	{
		String clean = e.displayText() == null ? "" : e.displayText().replaceAll("@[a-zA-Z_]+@", "");
		String prefix = time == null ? "" : "<span style='color:#a5a5a5'>" + time + "</span>&nbsp; ";
		JLabel l = new JLabel("<html><div style='width:165px'>" + prefix + escape(clean) + "</div></html>");
		l.setFont(OsrsUi.small());
		l.setForeground(typeColor(e.getType()));
		return full(l);
	}

	private JComponent buildSplitCard()
	{
		JPanel card = vertical();
		card.add(OsrsUi.header(L.t("Vipps-oppgjør", "Vipps split")));
		card.add(Box.createVerticalStrut(4));
		card.add(splitBox);
		return top(card);
	}

	private JComponent buildRallyCard()
	{
		JPanel card = vertical();
		card.add(OsrsUi.header(L.t("Kall på nasjonen", "Call the nation")));
		card.add(Box.createVerticalStrut(4));
		card.add(OsrsUi.label(L.t("Beskjed (valgfri):", "Message (optional):"), OsrsUi.small(), OsrsUi.TEXT_DIM));
		card.add(Box.createVerticalStrut(2));
		card.add(full(rallyNote));
		card.add(Box.createVerticalStrut(5));
		OsrsUi.StoneButton toMe = new OsrsUi.StoneButton("Rohirrim!").textColor(new Color(0xFF, 0x70, 0x40));
		toMe.setFont(OsrsUi.bold().deriveFont(16f));
		toMe.setMargin(new java.awt.Insets(10, 8, 10, 8));
		toMe.addActionListener(e ->
		{
			ctl.rallyToMe(rallyNote.getText());
			rallyNote.setText("");
		});
		card.add(full(toMe));
		card.add(Box.createVerticalStrut(6));
		card.add(OsrsUi.header(L.t("Aktivt Rohirrim!", "Active Rohirrim!")));
		card.add(Box.createVerticalStrut(4));
		card.add(rallyStatus);
		return top(card);
	}

	private JComponent buildBossCard()
	{
		JPanel card = vertical();
		card.add(OsrsUi.header(L.t("Finn Sweats", "Find Sweats")));
		card.add(Box.createVerticalStrut(4));
		String keep = activitySearch.getText();
		activitySearch = OsrsUi.searchField();
		activitySearch.setText(keep);
		activitySearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			private void changed()
			{
				refreshActivities();
				activityList.revalidate();
				activityList.repaint();
			}

			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
				changed();
			}
		});
		JPanel cats = new JPanel(new GridLayout(2, 3, 2, 2));
		cats.setOpaque(false);
		for (Activity.Category c : Activity.Category.values())
		{
			OsrsUi.StoneButton b = new OsrsUi.StoneButton(c.getLabel()).small();
			b.addActionListener(e ->
			{
				category = c;
				refresh();
			});
			categoryButtons.add(b);
			cats.add(b);
		}
		card.add(full(cats));
		card.add(Box.createVerticalStrut(4));
		card.add(full(activitySearch));
		card.add(Box.createVerticalStrut(6));
		card.add(activityList);
		return top(card);
	}

	private JComponent buildDiscordCard()
	{
		JPanel card = vertical();
		card.add(OsrsUi.header("Discord"));
		card.add(Box.createVerticalStrut(4));
		card.add(discordStatus);
		return top(card);
	}

	// ------------------------------------------------------------------ oppdatering

	private void selectTab(Tab t)
	{
		currentTab = t;
		cards.show(cardPanel, t.name());
		for (int i = 0; i < tabButtons.size(); i++)
		{
			tabButtons.get(i).setActive(Tab.values()[i] == t);
		}
	}

	/**
	 * Får Rohirrim!-fanen og svarknappen til å pulsere 6 ganger à 0,8 sekunder når noen
	 * roper Rohirrim! fra fanen eller kartet. Knappen for å rope et nytt Rohirrim! lyser ikke.
	 */
	public void flashRohirrim()
	{
		final long start = System.currentTimeMillis();
		final double period = 800.0;            // én puls = 0,8 sekunder
		final long duration = (long) (period * 6); // 6 pulser = 4,8 sekunder
		if (glowTimer != null)
		{
			glowTimer.stop();
		}
		glowTimer = new javax.swing.Timer(40, e ->
		{
			long t = System.currentTimeMillis() - start;
			float g = 0f;
			if (t < duration)
			{
				// hver puls går fra mørk til full glød og tilbake, så syklusene går opp i varigheten
				g = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * t / period));
			}
			else
			{
				((javax.swing.Timer) e.getSource()).stop();
			}
			rohirrimGlow = g;
			if (rohirrimTab != null)
			{
				rohirrimTab.setGlow(g);
			}
			// Svarknappen lyser også – knappen for å rope et nytt Rohirrim! gjør det ikke
			if (rohirrimReply != null)
			{
				rohirrimReply.setGlow(g);
			}
		});
		glowTimer.start();
	}

	/** Bygger dynamiske deler på nytt. Kalles på Swing-tråden. */
	public void refresh()
	{
		refreshGroup();
		refreshMembers();
		refreshRally();
		refreshActivities();
		refreshSplit();
		refreshLog();
		refreshDeaths();
		refreshDiscord();
		revalidate();
		repaint();
	}

	private void refreshGroup()
	{
		groupBox.removeAll();
		JPanel box = OsrsUi.inset(new BorderLayout(0, 4));
		if (ctl.isInGroup())
		{
			JPanel line = new JPanel(new BorderLayout());
			line.setOpaque(false);
			line.add(OsrsUi.label(L.t("Tilkoblet", "Connected"), OsrsUi.font(), OsrsUi.GREEN), BorderLayout.WEST);
			line.add(OsrsUi.label(Math.max(1, ctl.getOnlineCount()) + L.t(" i Gielinor", " in Gielinor"), OsrsUi.small(), OsrsUi.TEXT), BorderLayout.EAST);
			box.add(line, BorderLayout.NORTH);
			OsrsUi.ShadowLabel code = OsrsUi.label(shorten(ctl.getGroupLabel(), 28), OsrsUi.small(), OsrsUi.TEXT_DIM);
			code.setToolTipText(ctl.getGroupLabel());
			box.add(code, BorderLayout.CENTER);
			OsrsUi.StoneButton copy = new OsrsUi.StoneButton(L.t("Kopier kode", "Copy code")).small();
			copy.setToolTipText(L.t("Kopier gruppekoden så venner utenfor klanen kan bli med", "Copy the group code so friends outside the clan can join"));
			copy.addActionListener(e -> ctl.copyGroupCode());
			OsrsUi.StoneButton leave = new OsrsUi.StoneButton(L.t("Forlat", "Leave")).small().textColor(OsrsUi.RED);
			leave.addActionListener(e -> ctl.leaveGroup());
			JPanel btn = new JPanel(new GridLayout(1, 2, 3, 0));
			btn.setOpaque(false);
			btn.add(copy);
			btn.add(leave);
			box.add(btn, BorderLayout.SOUTH);
		}
		else
		{
			box.add(OsrsUi.label(L.t("Ikke tilkoblet", "Not connected"), OsrsUi.font(), OsrsUi.RED), BorderLayout.NORTH);
			JPanel join = vertical();
			OsrsUi.StoneButton clan = new OsrsUi.StoneButton(L.t("Bli med i klangruppa", "Join the clan group")).textColor(OsrsUi.ORANGE);
			clan.setToolTipText(L.t("Skjer automatisk når du kobler til klanchatten i spillet", "Happens automatically when you join the clan chat in game"));
			clan.addActionListener(e -> ctl.joinClanGroup());
			join.add(full(clan));
			join.add(Box.createVerticalStrut(4));
			join.add(OsrsUi.label(L.t("…eller bruk en gruppekode:", "…or use a group code:"), OsrsUi.small(), OsrsUi.TEXT_DIM));
			join.add(Box.createVerticalStrut(2));
			JPanel row = new JPanel(new BorderLayout(3, 0));
			row.setOpaque(false);
			row.add(codeField, BorderLayout.CENTER);
			OsrsUi.StoneButton go = new OsrsUi.StoneButton(L.t("Bli med", "Join")).small();
			go.addActionListener(e -> ctl.joinGroup(codeField.getText()));
			row.add(go, BorderLayout.EAST);
			join.add(full(row));
			join.add(Box.createVerticalStrut(4));
			OsrsUi.StoneButton create = new OsrsUi.StoneButton(L.t("Lag ny gruppe", "Create new group")).small();
			create.setToolTipText(L.t("Lag din egen SWEAT NATION-gruppe og få en kode du kan dele", "Create your own SWEAT NATION group and get a code you can share"));
			create.addActionListener(e -> ctl.createGroup());
			join.add(full(create));
			box.add(join, BorderLayout.CENTER);
		}
		groupBox.add(full(box));
	}

	private void refreshLive()
	{
		liveBox.removeAll();
		boolean on = ctl.isLiveMap();
		JPanel box = OsrsUi.inset(new BorderLayout(6, 2));
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(on ? OsrsUi.GREEN.darker() : OsrsUi.EDGE, 1),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		JPanel text = vertical();
		text.add(OsrsUi.label(on ? L.t("Live-kart: PÅ", "Live map: ON") : L.t("Live-kart: AV", "Live map: OFF"), OsrsUi.bold(), on ? OsrsUi.GREEN : OsrsUi.RED));
		text.add(OsrsUi.label(on ? L.t("Du deles i sanntid", "You're shared in real time") : L.t("Du er skjult", "You're hidden"), OsrsUi.small(), OsrsUi.TEXT_DIM));
		box.add(text, BorderLayout.CENTER);
		OsrsUi.StoneButton toggle = new OsrsUi.StoneButton(on ? L.t("Skru av", "Turn off") : L.t("Skru på", "Turn on")).small()
			.textColor(on ? OsrsUi.RED : OsrsUi.GREEN);
		toggle.addActionListener(e -> ctl.setLiveMap(!on));
		JPanel right = new JPanel(new java.awt.GridBagLayout());
		right.setOpaque(false);
		right.add(toggle);
		box.add(right, BorderLayout.EAST);
		liveBox.add(full(box));
	}

	private void refreshMembers()
	{
		refreshLive();
		membersList.removeAll();
		List<MemberState> list = ctl.getMembers();
		if (!ctl.isInGroup())
		{
			membersList.add(hint(L.t("Koble til klanchatten i spillet, så blir du automatisk med.", "Join the clan chat in game and you join automatically.")));
			return;
		}
		if (!ctl.isLiveMap())
		{
			membersList.add(hint(L.t("Skru på live-kartet for å se klanen og bli sett, akkurat som Snap Map.", "Turn on the live map to see the clan and be seen, just like Snap Map.")));
			return;
		}
		if (list.isEmpty())
		{
			membersList.add(hint(L.t("Ingen andre på live-kartet akkurat nå.", "Nobody else on the live map right now.")));
			return;
		}
		WorldPoint me = ctl.getLocalLocation();
		int myWorld = ctl.getLocalWorld();
		for (MemberState m : list)
		{
			membersList.add(memberRow(m, me, myWorld));
			membersList.add(Box.createVerticalStrut(3));
		}
	}

	private JComponent memberRow(MemberState m, WorldPoint me, int myWorld)
	{
		boolean sameWorld = m.getWorld() == myWorld;
		JPanel row = OsrsUi.inset(new BorderLayout(4, 2));

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(OsrsUi.label(m.getName(), OsrsUi.font(), OsrsUi.YELLOW), BorderLayout.WEST);
		top.add(OsrsUi.label("W" + m.getWorld(), OsrsUi.small(), sameWorld ? OsrsUi.GREEN : OsrsUi.ORANGE), BorderLayout.EAST);
		row.add(top, BorderLayout.NORTH);

		String where = ctl.describePlace(m.getLocation());
		JLabel wl = new JLabel("<html><div style='width:165px'>Lvl " + m.getCombatLevel() + "  ·  " + escape(where) + "</div></html>");
		wl.setFont(OsrsUi.small());
		wl.setForeground(OsrsUi.TEXT);
		row.add(wl, BorderLayout.CENTER);

		JPanel btns = new JPanel(new GridLayout(1, 2, 3, 0));
		btns.setOpaque(false);
		OsrsUi.StoneButton map = new OsrsUi.StoneButton(L.t("Vis på kart", "Show on map")).small();
		map.addActionListener(e -> ctl.focusOnMap(m.getLocation()));
		OsrsUi.StoneButton hop = new OsrsUi.StoneButton(sameWorld ? L.t("Samme verden", "Same world") : L.t("Hopp til W", "Hop to W") + m.getWorld()).small();
		hop.setEnabled(!sameWorld);
		hop.addActionListener(e -> ctl.hopToWorld(m.getWorld()));
		btns.add(map);
		btns.add(hop);
		row.add(btns, BorderLayout.SOUTH);
		return full(row);
	}

	private void refreshRally()
	{
		rallyStatus.removeAll();
		rohirrimReply = null;
		RallyState r = ctl.getActiveRally();
		if (r == null)
		{
			rallyStatus.add(hint(L.t("Ingen Rohirrim!", "No Rohirrim!")));
			return;
		}
		JPanel box = OsrsUi.inset(new BorderLayout(0, 3));
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(OsrsUi.ORANGE, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		JPanel info = vertical();
		info.add(OsrsUi.label(r.getCallerName() + "'s Rohirrim!", OsrsUi.bold(), new Color(0xFF, 0x70, 0x40)));
		String where = ctl.describePlace(r.getTarget());
		JLabel wl = new JLabel("<html><div style='width:165px'>" + L.t("Verden ", "World ") + r.getWorld()
			+ (r.getWorld() != ctl.getLocalWorld() ? L.t(" (bytt verden)", " (switch world)") : "") + "  ·  " + escape(where) + "</div></html>");
		wl.setFont(OsrsUi.small());
		wl.setForeground(OsrsUi.TEXT);
		info.add(wl);
		if (r.getNote() != null && !r.getNote().isEmpty())
		{
			info.add(OsrsUi.label("\"" + r.getNote() + "\"", OsrsUi.small(), OsrsUi.CYAN));
		}
		long secs = Math.max(0, (r.getExpiresAt() - System.currentTimeMillis()) / 1000);
		info.add(OsrsUi.label(L.t("Utløper om ", "Expires in ") + secs / 60 + " min", OsrsUi.small(), OsrsUi.TEXT_DIM));
		Map<String, Boolean> resp = r.getResponses();
		if (!resp.isEmpty())
		{
			info.add(Box.createVerticalStrut(3));
			for (Map.Entry<String, Boolean> e : resp.entrySet())
			{
				info.add(OsrsUi.label(e.getKey() + (e.getValue() ? " – Rohirrim!" : L.t(" – kan ikke", " – can't")), OsrsUi.small(),
					e.getValue() ? OsrsUi.GREEN : OsrsUi.RED));
			}
		}
		box.add(info, BorderLayout.CENTER);

		JPanel btns = new JPanel(new GridLayout(0, 2, 3, 3));
		btns.setOpaque(false);
		if (!ctl.isMyRally())
		{
			OsrsUi.StoneButton yes = new OsrsUi.StoneButton("Rohirrim!").small().textColor(new Color(0xFF, 0x70, 0x40));
			yes.setFont(OsrsUi.bold());
			yes.setGlow(rohirrimGlow);
			rohirrimReply = yes;
			yes.addActionListener(e -> ctl.respondToRally(true));
			OsrsUi.StoneButton no = new OsrsUi.StoneButton(L.t("Kan ikke", "Can't")).small().textColor(OsrsUi.RED);
			no.addActionListener(e -> ctl.respondToRally(false));
			btns.add(yes);
			btns.add(no);
		}
		OsrsUi.StoneButton show = new OsrsUi.StoneButton(L.t("Vis på kart", "Show on map")).small();
		show.addActionListener(e -> ctl.focusOnMap(r.getTarget()));
		btns.add(show);
		if (r.getWorld() != ctl.getLocalWorld())
		{
			OsrsUi.StoneButton hop = new OsrsUi.StoneButton(L.t("Hopp til W", "Hop to W") + r.getWorld()).small();
			hop.addActionListener(e -> ctl.hopToWorld(r.getWorld()));
			btns.add(hop);
		}
		OsrsUi.StoneButton end = new OsrsUi.StoneButton(ctl.isMyRally() ? L.t("Avslutt Rohirrim!", "End Rohirrim!") : L.t("Skjul", "Hide")).small();
		end.addActionListener(e -> ctl.cancelRally());
		btns.add(end);
		box.add(btns, BorderLayout.SOUTH);
		rallyStatus.add(full(box));

		List<String> tele = ctl.getTeleportAdvice();
		if (!tele.isEmpty())
		{
			rallyStatus.add(Box.createVerticalStrut(6));
			rallyStatus.add(OsrsUi.header(L.t("Raskeste teleport", "Fastest teleport")));
			rallyStatus.add(Box.createVerticalStrut(3));
			JPanel tb = OsrsUi.inset(null);
			tb.setLayout(new BoxLayout(tb, BoxLayout.Y_AXIS));
			tb.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, OsrsUi.ORANGE),
				BorderFactory.createEmptyBorder(4, 7, 4, 6)));
			boolean first = true;
			for (String line : tele)
			{
				JLabel l = new JLabel("<html><div style='width:165px'>" + escape(line) + "</div></html>");
				l.setFont(first ? OsrsUi.bold() : OsrsUi.small());
				l.setForeground(first ? Color.WHITE : OsrsUi.CYAN);
				if (!first)
				{
					tb.add(Box.createVerticalStrut(4));
				}
				tb.add(l);
				first = false;
			}
			rallyStatus.add(full(tb));
			rallyStatus.add(Box.createVerticalStrut(3));
			rallyStatus.add(hint(L.t("Gjenstander og runer du har, lyser oransje i bank, utstyr og inventory.", "Items and runes you own glow orange in the bank, equipment and inventory.")));
		}
	}

	/** Tekster som oppdateres hvert sekund (nedtellingen i ready check). Tømmes når lista bygges på nytt. */
	private final List<Runnable> liveLabels = new java.util.concurrent.CopyOnWriteArrayList<>();
	private final javax.swing.Timer liveTimer = new javax.swing.Timer(250, e -> liveLabels.forEach(Runnable::run));

	private void refreshActivities()
	{
		liveLabels.clear();
		if (!liveTimer.isRunning())
		{
			liveTimer.start();
		}
		for (int k = 0; k < categoryButtons.size(); k++)
		{
			categoryButtons.get(k).setActive(Activity.Category.values()[k] == category);
		}
		activityList.removeAll();
		List<GroupView> groups = ctl.getGroups();

		// 1) Gruppene dine øverst, i fokus
		boolean any = false;
		for (GroupView g : groups)
		{
			if (g.mine)
			{
				activityList.add(lobbyCard(g));
				activityList.add(Box.createVerticalStrut(6));
				any = true;
			}
		}
		if (any)
		{
			activityList.add(lagHudControls());
			activityList.add(Box.createVerticalStrut(4));
			activityList.add(OsrsUi.header(L.t("Flere grupper", "More groups")));
			activityList.add(Box.createVerticalStrut(4));
		}

		String query = activitySearch.getText().trim();
		boolean searching = !query.isEmpty();

		// 2) Egendefinert gruppe under Hjelp
		if (category == Activity.Category.HELP && !searching)
		{
			activityList.add(customCreator());
			activityList.add(Box.createVerticalStrut(6));
		}

		// 3) Aktiviteter med grupper (som du ikke er med i) – lett å finne nye medlemmer
		java.util.Map<Activity, List<GroupView>> open = new java.util.LinkedHashMap<>();
		for (GroupView g : groups)
		{
			boolean inCategory = category == Activity.Category.ALL || g.activity.getCategory() == category;
			boolean hit = inCategory && (!searching || g.activity.matches(query)
				|| (g.title != null && g.title.toLowerCase().contains(query.toLowerCase())));
			if (!g.mine && hit)
			{
				open.computeIfAbsent(g.activity, x -> new ArrayList<>()).add(g);
			}
		}
		for (java.util.Map.Entry<Activity, List<GroupView>> e : open.entrySet())
		{
			activityList.add(activityRow(e.getKey(), e.getValue()));
			activityList.add(Box.createVerticalStrut(3));
		}

		// 4) Resten av aktivitetene – alle kan få en ny gruppe
		int shown = open.size();
		for (Activity a : Activity.values())
		{
			if (a == Activity.CUSTOM || open.containsKey(a))
			{
				continue;
			}
			if ((category != Activity.Category.ALL && a.getCategory() != category) || (searching && !a.matches(query)))
			{
				continue;
			}
			activityList.add(activityRow(a, List.of()));
			shown++;
			activityList.add(Box.createVerticalStrut(3));
		}
		if (searching && shown == 0)
		{
			String where = category == Activity.Category.ALL ? "" : L.t(" i ", " in ") + category.getLabel();
			activityList.add(hint(L.t("Ingen aktiviteter", "No activities") + where + L.t(" passer «", " match \"") + query
				+ L.t("».", "\".")));
		}
	}

	/** Lag en egendefinert gruppe med navn og antall medlemmer. */
	private final JTextField customName = OsrsUi.textField(L.t("F.eks. \"Barbarian Fishing\" eller \"Fight Caves-hjelp\"", "E.g. \"Barbarian Fishing\" or \"Fight Caves help\""));
	private int customSize = 4;

	private JComponent customCreator()
	{
		JPanel box = OsrsUi.inset(null);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, OsrsUi.PURPLE),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)));
		box.add(full(OsrsUi.label(L.t("Egendefinert gruppe", "Custom group"), OsrsUi.bold(), Color.WHITE)));
		box.add(Box.createVerticalStrut(4));
		box.add(full(OsrsUi.label(L.t("Navn", "Name"), OsrsUi.small(), OsrsUi.TEXT_DIM)));
		box.add(full(customName));
		box.add(Box.createVerticalStrut(4));
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.add(OsrsUi.label(L.t("Antall medlemmer", "Number of members"), OsrsUi.small(), OsrsUi.TEXT), BorderLayout.WEST);
		JPanel ctrl = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 2, 0));
		ctrl.setOpaque(false);
		JLabel n = OsrsUi.label(String.valueOf(customSize), OsrsUi.bold(), Color.WHITE);
		OsrsUi.StoneButton down = new OsrsUi.StoneButton("▼").small();
		down.setMargin(new java.awt.Insets(1, 4, 1, 4));
		down.addActionListener(e ->
		{
			customSize = Math.max(2, customSize - 1);
			n.setText(String.valueOf(customSize));
		});
		OsrsUi.StoneButton up = new OsrsUi.StoneButton("▲").small();
		up.setMargin(new java.awt.Insets(1, 4, 1, 4));
		up.addActionListener(e ->
		{
			customSize = Math.min(100, customSize + 1);
			n.setText(String.valueOf(customSize));
		});
		ctrl.add(down);
		ctrl.add(n);
		ctrl.add(up);
		row.add(ctrl, BorderLayout.EAST);
		box.add(full(row));
		box.add(Box.createVerticalStrut(6));
		OsrsUi.StoneButton create = new OsrsUi.StoneButton(L.t("Opprett gruppe", "Create group")).textColor(OsrsUi.GREEN);
		create.addActionListener(e ->
		{
			ctl.createCustomTeam(customName.getText(), customSize);
			customName.setText("");
		});
		box.add(full(create));
		return full(box);
	}

	/** Lobbyen for en gruppe du er med i: medlemmer med roller, størrelse med piler, klar-sjekk, Vipps og Discord. */
	private JComponent lobbyCard(GroupView gv)
	{
		Activity a = gv.activity;
		List<LobbyMember> lobby = gv.members;
		ReadyCheck rc = ctl.getReadyCheck(gv.key);
		Color team = a.teamColor();
		boolean ready = lobby.size() >= gv.size;

		JPanel card = OsrsUi.inset(new BorderLayout(0, 4));
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(2, 3, 2, 2, team),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)));

		JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		head.add(OsrsUi.label(shorten(gv.title, 20), OsrsUi.bold().deriveFont(14f), team), BorderLayout.WEST);
		// Antall påmeldte / ønsket størrelse, med pil ned og opp for å justere størrelsen
		JPanel count = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 2, 0));
		count.setOpaque(false);
		OsrsUi.StoneButton down = new OsrsUi.StoneButton("▼").small();
		down.setMargin(new java.awt.Insets(1, 4, 1, 4));
		down.setToolTipText(L.t("Mindre gruppe", "Smaller group"));
		down.setEnabled(gv.size > 1);
		down.addActionListener(e -> ctl.changeTeamSize(gv.key, -1));
		OsrsUi.StoneButton up = new OsrsUi.StoneButton("▲").small();
		up.setMargin(new java.awt.Insets(1, 4, 1, 4));
		up.setToolTipText(L.t("Større gruppe", "Bigger group"));
		up.addActionListener(e -> ctl.changeTeamSize(gv.key, 1));
		count.add(down);
		count.add(OsrsUi.label(lobby.size() + "/" + gv.size, OsrsUi.bold(), ready ? OsrsUi.GREEN : Color.WHITE));
		count.add(up);
		head.add(count, BorderLayout.EAST);
		card.add(head, BorderLayout.NORTH);

		JPanel body = vertical();
		String me = ctl.getLocalName();
		for (LobbyMember lm : lobby)
		{
			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setOpaque(false);
			row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
			boolean isMe = lm.name.equals(me);
			JComponent chip;
			if (isMe)
			{
				OsrsUi.StoneButton rb = new OsrsUi.StoneButton(lm.role.getLabel()).small().textColor(Color.BLACK);
				rb.setFill(lm.role.getColor());
				rb.setToolTipText(L.t("Trykk for å bytte rolle", "Click to change role"));
				rb.setPreferredSize(new Dimension(52, 18));
				rb.addActionListener(e -> ctl.cycleRole(gv.key));
				chip = rb;
			}
			else
			{
				OsrsUi.StoneButton rb = new OsrsUi.StoneButton(lm.role.getLabel()).small().textColor(Color.BLACK);
				rb.setFill(lm.role.getColor());
				rb.setEnabled(false);
				rb.setPreferredSize(new Dimension(52, 18));
				chip = rb;
			}
			row.add(chip, BorderLayout.WEST);
			boolean off = lm.offlineMinutesLeft >= 0;
			row.add(OsrsUi.label(lm.name, isMe ? OsrsUi.bold() : OsrsUi.font(), off ? OsrsUi.TEXT_DIM : Color.WHITE), BorderLayout.CENTER);
			String status = off ? L.t("Logget ut · ", "Logged out · ") + lm.offlineMinutesLeft + " min"
				: lm.ready == null ? (rc != null ? "…" : "") : lm.ready ? L.t("Klar", "Ready") : L.t("Ikke klar", "Not ready");
			Color sc = off ? OsrsUi.TEXT_DIM : lm.ready == null ? OsrsUi.TEXT_DIM : lm.ready ? OsrsUi.GREEN : OsrsUi.RED;
			row.add(OsrsUi.label(status, OsrsUi.small(), sc), BorderLayout.EAST);
			body.add(full(row));
		}
		for (int i = lobby.size(); i < Math.min(gv.size, 8); i++)
		{
			body.add(full(OsrsUi.label(L.t("— ledig plass —", "— open slot —"), OsrsUi.small(), new Color(0x70, 0x70, 0x70))));
		}

		if (rc != null)
		{
			body.add(Box.createVerticalStrut(4));
			int klare = 0;
			for (LobbyMember lm : lobby)
			{
				if (Boolean.TRUE.equals(lm.ready))
				{
					klare++;
				}
			}
			final int readyCount = klare;
			final int total = lobby.size();
			final ReadyCheck check = rc;
			java.util.function.Supplier<String> text = () -> check.isCompleted() ? L.t("Alle er klare!", "Everyone is ready!")
				: L.t("Ready check: ", "Ready check: ") + readyCount + "/" + total + L.t(" klare  ·  ", " ready  ·  ") + check.secondsLeft() + " s";
			OsrsUi.ShadowLabel rcLabel = OsrsUi.label(text.get(), OsrsUi.bold(), rc.isCompleted() ? OsrsUi.GREEN : OsrsUi.ORANGE);
			if (!rc.isCompleted())
			{
				liveLabels.add(() -> rcLabel.setText(text.get())); // teller ned hvert sekund
			}
			body.add(full(rcLabel));
			Boolean mine = rc.answerOf(me);
			if (!rc.isCompleted() && mine == null)
			{
				JPanel ans = new JPanel(new GridLayout(1, 2, 3, 0));
				ans.setOpaque(false);
				OsrsUi.StoneButton yes = new OsrsUi.StoneButton(L.t("Klar", "Ready")).textColor(OsrsUi.GREEN);
				yes.setFont(OsrsUi.bold());
				yes.addActionListener(e -> ctl.answerReadyCheck(gv.key, true));
				OsrsUi.StoneButton no = new OsrsUi.StoneButton(L.t("Ikke klar", "Not ready")).textColor(OsrsUi.RED);
				no.addActionListener(e -> ctl.answerReadyCheck(gv.key, false));
				ans.add(yes);
				ans.add(no);
				body.add(Box.createVerticalStrut(3));
				body.add(full(ans));
			}
		}
		card.add(body, BorderLayout.CENTER);

		JPanel btns = new JPanel(new GridLayout(0, 2, 3, 3));
		btns.setOpaque(false);
		OsrsUi.StoneButton check = new OsrsUi.StoneButton(L.t("Ready check", "Ready check")).small().textColor(OsrsUi.ORANGE);
		check.setEnabled(lobby.size() >= 2 && (rc == null || rc.isCompleted()));
		check.addActionListener(e -> ctl.startReadyCheck(gv.key));
		OsrsUi.StoneButton sp = new OsrsUi.StoneButton("Vipps").small().textColor(ColorSchemeGe.GE);
		sp.setEnabled(lobby.size() >= 2);
		sp.addActionListener(e ->
		{
			List<String> names = new ArrayList<>();
			for (LobbyMember lm : lobby)
			{
				names.add(lm.name);
			}
			ctl.startSplit(gv.title, names);
			selectTab(Tab.SPLIT);
		});
		OsrsUi.StoneButton dc = new OsrsUi.StoneButton("Discord").small().textColor(OsrsUi.PURPLE);
		dc.addActionListener(e -> ctl.openDiscord());
		OsrsUi.StoneButton leave = new OsrsUi.StoneButton(L.t("Meld av", "Leave group")).small().textColor(OsrsUi.RED);
		leave.addActionListener(e -> ctl.leaveTeam(gv.key));
		btns.add(check);
		btns.add(sp);
		btns.add(dc);
		btns.add(leave);
		JPanel south = vertical();
		south.add(full(btns));
		if (a.getEntrance() != null)
		{
			south.add(Box.createVerticalStrut(3));
			south.add(hint(L.t("Møtested: ", "Meeting point: ") + ctl.describePlace(a.getEntrance())));
		}
		card.add(south, BorderLayout.SOUTH);
		return full(card);
	}

	/** HUD på/av og størrelse (glidebryter). Avstander stilles i innstillingene. */
	private JComponent lagHudControls()
	{
		JPanel box = OsrsUi.inset(new BorderLayout(6, 2));
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(OsrsUi.label("HUD", OsrsUi.bold(), Color.WHITE), BorderLayout.WEST);
		boolean on = ctl.isTeamHud();
		OsrsUi.StoneButton tog = new OsrsUi.StoneButton(on ? L.t("På", "On") : L.t("Av", "Off")).small().textColor(on ? OsrsUi.GREEN : OsrsUi.RED);
		tog.addActionListener(e -> ctl.setTeamHud(!on));
		top.add(tog, BorderLayout.EAST);
		box.add(top, BorderLayout.NORTH);
		javax.swing.JSlider slider = new javax.swing.JSlider(50, 150, ctl.getHudScalePercent());
		slider.setOpaque(false);
		slider.setToolTipText(L.t("Størrelse på HUD-en i spillet", "Size of the HUD in game"));
		JLabel pct = OsrsUi.label(ctl.getHudScalePercent() + " %", OsrsUi.small(), OsrsUi.TEXT_DIM);
		slider.addChangeListener(e ->
		{
			pct.setText(slider.getValue() + " %");
			if (!slider.getValueIsAdjusting())
			{
				ctl.setHudScalePercent(slider.getValue());
			}
		});
		box.add(slider, BorderLayout.CENTER);
		box.add(pct, BorderLayout.EAST);
		return full(box);
	}

	/** En aktivitet: grupper du kan bli med i, og «Ny gruppe» (også når det finnes grupper fra før). */
	private JComponent activityRow(Activity a, List<GroupView> groups)
	{
		JPanel row = OsrsUi.inset(new BorderLayout(4, 2));
		if (!groups.isEmpty())
		{
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, a.teamColor()),
				BorderFactory.createEmptyBorder(4, 7, 4, 6)));
		}
		JPanel head = new JPanel(new BorderLayout(4, 0));
		head.setOpaque(false);
		JPanel text = vertical();
		boolean custom = a == Activity.CUSTOM;
		text.add(OsrsUi.label(custom ? L.t("Egendefinerte grupper", "Custom groups") : a.getDisplayName(), OsrsUi.font(),
			groups.isEmpty() ? OsrsUi.ORANGE : a.teamColor()));
		if (!custom)
		{
			text.add(OsrsUi.label(L.t("lag ", "team ") + a.teamSizeText() + (groups.isEmpty() ? "" : "  ·  " + groups.size()
				+ (groups.size() == 1 ? L.t(" gruppe", " group") : L.t(" grupper", " groups"))), OsrsUi.small(), OsrsUi.TEXT_DIM));
		}
		head.add(text, BorderLayout.CENTER);
		if (!custom)
		{
			OsrsUi.StoneButton create = new OsrsUi.StoneButton(L.t("Ny gruppe", "New group")).small().textColor(OsrsUi.GREEN);
			create.setToolTipText(L.t("Lag en ny gruppe for ", "Create a new group for ") + a.getDisplayName());
			create.addActionListener(e -> ctl.createTeam(a));
			JPanel right = new JPanel(new java.awt.GridBagLayout());
			right.setOpaque(false);
			right.add(create);
			head.add(right, BorderLayout.EAST);
		}
		row.add(head, BorderLayout.NORTH);

		if (!groups.isEmpty())
		{
			JPanel list = vertical();
			for (GroupView g : groups)
			{
				JPanel gr = new JPanel(new BorderLayout(4, 0));
				gr.setOpaque(false);
				gr.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
				JPanel info = vertical();
				info.add(OsrsUi.label(shorten(g.title, 22) + "  " + g.members.size() + "/" + g.size, OsrsUi.bold(),
					g.isFull() ? OsrsUi.TEXT_DIM : Color.WHITE));
				JLabel who = new JLabel("<html><div style='width:115px'>" + escape(String.join(", ", g.names())) + "</div></html>");
				who.setFont(OsrsUi.small());
				who.setForeground(OsrsUi.CYAN);
				info.add(who);
				gr.add(info, BorderLayout.CENTER);
				OsrsUi.StoneButton join = new OsrsUi.StoneButton(g.isFull() ? "Full" : L.t("Bli med", "Join")).small()
					.textColor(g.isFull() ? OsrsUi.TEXT_DIM : OsrsUi.GREEN);
				join.setEnabled(!g.isFull());
				join.addActionListener(e -> ctl.joinTeam(g.key));
				JPanel right = new JPanel(new java.awt.GridBagLayout());
				right.setOpaque(false);
				right.add(join);
				gr.add(right, BorderLayout.EAST);
				list.add(full(gr));
			}
			row.add(list, BorderLayout.CENTER);
		}
		return full(row);
	}

	private void refreshDeaths()
	{
		deathsList.removeAll();
		List<DeathRecord> list = ctl.getDeaths();
		if (list.isEmpty())
		{
			deathsList.add(hint(L.t("Ingen dødsfall. Hold det sånn!", "No deaths. Keep it that way!")));
			return;
		}
		int n = 0;
		for (DeathRecord d : list)
		{
			if (n++ >= 5)
			{
				break;
			}
			JPanel row = OsrsUi.inset(new BorderLayout(4, 0));
			JPanel t = vertical();
			t.add(OsrsUi.label(d.name, OsrsUi.bold(), OsrsUi.RED));
			JLabel cause = new JLabel("<html><div style='width:150px'>" + escape(d.cause()) + "</div></html>");
			cause.setFont(OsrsUi.small());
			cause.setForeground(new Color(0xFF, 0x8C, 0x8C));
			t.add(cause);
			long m = d.minutesAgo();
			t.add(OsrsUi.label((m == 0 ? L.t("nå nettopp", "just now") : m + L.t(" min siden", " min ago")) + "  ·  W" + d.world, OsrsUi.small(), OsrsUi.TEXT_DIM));
			row.add(t, BorderLayout.CENTER);
			OsrsUi.StoneButton show = new OsrsUi.StoneButton(L.t("Vis", "Show")).small();
			show.addActionListener(e -> ctl.focusOnMap(d.location));
			JPanel right = new JPanel(new java.awt.GridBagLayout());
			right.setOpaque(false);
			right.add(show);
			row.add(right, BorderLayout.EAST);
			deathsList.add(full(row));
			deathsList.add(Box.createVerticalStrut(3));
		}
	}

	private void refreshSplit()
	{
		splitBox.removeAll();
		SplitSession s = ctl.getSplit();
		if (s == null)
		{
			splitBox.add(OsrsUi.label(L.t("Gi navn til oppgjøret", "Name the split"), OsrsUi.small(), OsrsUi.TEXT_DIM));
			splitBox.add(Box.createVerticalStrut(2));
			splitBox.add(full(splitTitle));
			splitBox.add(Box.createVerticalStrut(5));
			OsrsUi.StoneButton start = new OsrsUi.StoneButton(L.t("Start oppgjør", "Start split")).textColor(OsrsUi.YELLOW);
			start.setFont(OsrsUi.bold());
			start.addActionListener(e -> ctl.startSplit(splitTitle.getText()));
			splitBox.add(full(start));
			splitBox.add(Box.createVerticalStrut(6));
			splitBox.add(hint(L.t("Alle i gruppa får beskjed og kan trykke «Bli med». Loot hver deltaker får, telles automatisk ", "Everyone in the group is notified and can press “Join”. Loot each participant gets is counted automatically ")
				+ L.t("med GE-pris fra RuneLite sin Loot Tracker. Til slutt regner SWEAT NATION ut hvem som skal betale hvem.", "at GE price from RuneLite's Loot Tracker. At the end SWEAT NATION works out who pays whom.")));
			return;
		}

		boolean in = ctl.isInSplit();
		boolean host = s.getHost().equals(ctl.getLocalName());

		JPanel head = OsrsUi.inset(new BorderLayout(0, 2));
		head.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(s.isActive() ? OsrsUi.PURPLE : OsrsUi.EDGE, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		JPanel ht = vertical();
		ht.add(OsrsUi.label(s.getTitle(), OsrsUi.bold(), OsrsUi.ORANGE));
		ht.add(OsrsUi.label((s.isActive() ? L.t("Pågår", "In progress") : L.t("Avsluttet", "Ended")) + L.t("  ·  leder: ", "  ·  leader: ") + s.getHost(), OsrsUi.small(),
			s.isActive() ? OsrsUi.GREEN : OsrsUi.TEXT_DIM));
		ht.add(Box.createVerticalStrut(3));
		JPanel totals = new JPanel(new BorderLayout());
		totals.setOpaque(false);
		totals.add(OsrsUi.label("Total", OsrsUi.small(), OsrsUi.TEXT), BorderLayout.WEST);
		totals.add(OsrsUi.label(Gp.format(s.getTotal()) + " gp", OsrsUi.font(), OsrsUi.YELLOW), BorderLayout.EAST);
		ht.add(full(totals));
		JPanel share = new JPanel(new BorderLayout());
		share.setOpaque(false);
		share.add(OsrsUi.label(L.t("Andel (", "Share (") + s.getParticipants().size() + L.t(" stk)", " players)"), OsrsUi.small(), OsrsUi.TEXT), BorderLayout.WEST);
		share.add(OsrsUi.label(Gp.format(s.getShare()) + " gp", OsrsUi.font(), OsrsUi.GREEN), BorderLayout.EAST);
		ht.add(full(share));
		head.add(ht, BorderLayout.CENTER);
		splitBox.add(full(head));
		splitBox.add(Box.createVerticalStrut(5));

		splitBox.add(OsrsUi.label(L.t("Deltakere – fikk", "Participants – received"), OsrsUi.small(), OsrsUi.TEXT_DIM));
		for (String p : s.getParticipants())
		{
			JPanel r = new JPanel(new BorderLayout());
			r.setOpaque(false);
			r.add(OsrsUi.label(p, OsrsUi.small(), OsrsUi.YELLOW), BorderLayout.WEST);
			r.add(OsrsUi.label(Gp.format(s.getReceived(p)), OsrsUi.small(), OsrsUi.TEXT), BorderLayout.EAST);
			splitBox.add(full(r));
		}

		List<SplitSession.Transfer> transfers = s.settle();
		if (!transfers.isEmpty())
		{
			splitBox.add(Box.createVerticalStrut(5));
			splitBox.add(OsrsUi.label(L.t("Oppgjør", "Settlement"), OsrsUi.small(), OsrsUi.TEXT_DIM));
			JPanel tbox = OsrsUi.inset(null);
			tbox.setLayout(new BoxLayout(tbox, BoxLayout.Y_AXIS));
			for (SplitSession.Transfer t : transfers)
			{
				JPanel r = new JPanel(new BorderLayout());
				r.setOpaque(false);
				r.add(OsrsUi.label(t.from + " → " + t.to, OsrsUi.small(), OsrsUi.CYAN), BorderLayout.WEST);
				r.add(OsrsUi.label(Gp.format(t.amount), OsrsUi.small(), OsrsUi.YELLOW), BorderLayout.EAST);
				tbox.add(full(r));
			}
			splitBox.add(full(tbox));
		}

		List<SplitSession.Entry> recent = s.getRecent(6);
		if (!recent.isEmpty())
		{
			splitBox.add(Box.createVerticalStrut(5));
			splitBox.add(OsrsUi.label(L.t("Siste loot", "Latest loot"), OsrsUi.small(), OsrsUi.TEXT_DIM));
			for (SplitSession.Entry e : recent)
			{
				JPanel r = new JPanel(new BorderLayout(4, 0));
				r.setOpaque(false);
				String item = (e.quantity > 1 ? e.quantity + " x " : "") + e.item;
				r.add(OsrsUi.label(e.player + ": " + shorten(item, 20), OsrsUi.small(), OsrsUi.TEXT), BorderLayout.WEST);
				r.add(OsrsUi.label(Gp.format(e.value), OsrsUi.small(), e.value >= 1_000_000 ? OsrsUi.GREEN : OsrsUi.TEXT_DIM), BorderLayout.EAST);
				splitBox.add(full(r));
			}
		}

		splitBox.add(Box.createVerticalStrut(6));
		JPanel btns = new JPanel(new GridLayout(0, 2, 3, 3));
		btns.setOpaque(false);
		if (s.isActive())
		{
			if (!in)
			{
				OsrsUi.StoneButton join = new OsrsUi.StoneButton(L.t("Bli med", "Join")).small().textColor(OsrsUi.GREEN);
				join.addActionListener(e -> ctl.joinSplit());
				btns.add(join);
			}
			else if (!host)
			{
				OsrsUi.StoneButton leave = new OsrsUi.StoneButton(L.t("Forlat", "Leave")).small().textColor(OsrsUi.RED);
				leave.addActionListener(e -> ctl.leaveSplit());
				btns.add(leave);
			}
			if (host)
			{
				OsrsUi.StoneButton stop = new OsrsUi.StoneButton(L.t("Avslutt oppgjør", "End split")).small().textColor(OsrsUi.RED);
				stop.addActionListener(e -> ctl.stopSplit());
				btns.add(stop);
			}
		}
		else
		{
			OsrsUi.StoneButton close = new OsrsUi.StoneButton(L.t("Lukk", "Close")).small();
			close.addActionListener(e -> ctl.closeSplit());
			btns.add(close);
		}
		OsrsUi.StoneButton copy = new OsrsUi.StoneButton(L.t("Kopier oppgjør", "Copy settlement")).small();
		copy.addActionListener(e -> ctl.copySplitSummary());
		btns.add(copy);
		splitBox.add(full(btns));
	}

	private void refreshDiscord()
	{
		discordStatus.removeAll();
		String link = ctl.getDiscordLink();
		OsrsUi.StoneButton open = new OsrsUi.StoneButton(L.t("Åpne Discord", "Open Discord")).textColor(OsrsUi.PURPLE);
		open.setFont(OsrsUi.bold().deriveFont(14f));
		open.setMargin(new java.awt.Insets(10, 8, 10, 8));
		open.setEnabled(!link.isEmpty());
		open.setToolTipText(link.isEmpty() ? L.t("Legg inn en Discord-lenke i innstillingene først", "Add a Discord link in the settings first") : link);
		open.addActionListener(e -> ctl.openDiscord());
		discordStatus.add(full(open));
		discordStatus.add(Box.createVerticalStrut(6));
		if (!link.isEmpty())
		{
			discordStatus.add(full(OsrsUi.label(shorten(link.replaceFirst("^https://", ""), 34), OsrsUi.small(), OsrsUi.TEXT_DIM)));
		}
		else if (ctl.hasInvalidDiscordLink())
		{
			discordStatus.add(hint(L.t("Lenken i innstillingene er ikke en Discord-lenke. Bruk en lenke som starter med discord.gg/ eller discord.com/.", "The link in the settings isn't a Discord link. Use a link starting with discord.gg/ or discord.com/.")));
		}
		else
		{
			discordStatus.add(hint(L.t("Legg inn klanens Discord-lenke under tannhjulet → SWEAT NATION → Discord, så åpner knappen den i nettleseren.", "Add the clan's Discord link under the cog → SWEAT NATION → Discord, and the button opens it in your browser.")));
		}
	}

	// ------------------------------------------------------------------ små hjelpere

	private static JPanel vertical()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setOpaque(false);
		return p;
	}

	/** Lar en komponent fylle hele bredden i en BoxLayout. */
	private static <T extends JComponent> T full(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
		return c;
	}

	private static JComponent top(JPanel card)
	{
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setOpaque(false);
		wrap.add(card, BorderLayout.NORTH);
		return wrap;
	}

	private static JComponent hint(String text)
	{
		JLabel l = new JLabel("<html><div style='width:170px'>" + escape(text) + "</div></html>");
		l.setFont(OsrsUi.safe(OsrsUi.small(), text));
		l.setForeground(OsrsUi.TEXT_DIM);
		return full(l);
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String shorten(String s, int max)
	{
		if (s == null)
		{
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}
}
