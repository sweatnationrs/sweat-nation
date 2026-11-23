package com.sweatnation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** En split loot-økt: hvem som er med, hva de har fått, og hvem som skylder hvem. */
public class SplitSession
{
	public static class Entry
	{
		public final String player;
		public final String item;
		public final int quantity;
		public final long value;
		public final String source;

		Entry(String player, String item, int quantity, long value, String source)
		{
			this.player = player;
			this.item = item;
			this.quantity = quantity;
			this.value = value;
			this.source = source;
		}
	}

	/** Én overføring i oppgjøret: from gir amount til to. */
	public static class Transfer
	{
		public final String from;
		public final String to;
		public final long amount;

		Transfer(String from, String to, long amount)
		{
			this.from = from;
			this.to = to;
			this.amount = amount;
		}
	}

	private final String id;
	private final String title;
	private final String host;
	private final long startedAt = System.currentTimeMillis();
	private volatile boolean active = true;
	private final Set<String> participants = new LinkedHashSet<>();
	private final Map<String, Long> received = new LinkedHashMap<>();
	private final List<Entry> entries = new ArrayList<>();

	public SplitSession(String id, String title, String host)
	{
		this.id = id;
		this.title = title;
		this.host = host;
	}

	public String getId() { return id; }
	public String getTitle() { return title; }
	public String getHost() { return host; }
	public long getStartedAt() { return startedAt; }
	public boolean isActive() { return active; }
	public void end() { active = false; }

	public synchronized void addParticipant(String name)
	{
		participants.add(name);
		received.putIfAbsent(name, 0L);
	}

	public synchronized void removeParticipant(String name)
	{
		participants.remove(name);
	}

	public synchronized boolean hasParticipant(String name)
	{
		return participants.contains(name);
	}

	public synchronized List<String> getParticipants()
	{
		return new ArrayList<>(participants);
	}

	public synchronized void addLoot(String player, String item, int qty, long value, String source)
	{
		if (!participants.contains(player))
		{
			return;
		}
		entries.add(new Entry(player, item, qty, value, source));
		received.merge(player, value, Long::sum);
	}

	public synchronized long getReceived(String player)
	{
		return received.getOrDefault(player, 0L);
	}

	public synchronized long getTotal()
	{
		long t = 0;
		for (String p : participants)
		{
			t += received.getOrDefault(p, 0L);
		}
		return t;
	}

	public synchronized long getShare()
	{
		return participants.isEmpty() ? 0 : getTotal() / participants.size();
	}

	/** Siste loot først. */
	public synchronized List<Entry> getRecent(int max)
	{
		List<Entry> l = new ArrayList<>(entries);
		Collections.reverse(l);
		return l.size() > max ? l.subList(0, max) : l;
	}

	/**
	 * Minimalt oppgjør: de som fikk mer enn sin andel betaler ut til dem som fikk mindre.
	 */
	public synchronized List<Transfer> settle()
	{
		long share = getShare();
		List<String> payers = new ArrayList<>();
		List<String> takers = new ArrayList<>();
		Map<String, Long> bal = new LinkedHashMap<>();
		for (String p : participants)
		{
			long b = received.getOrDefault(p, 0L) - share;
			bal.put(p, b);
			if (b > 0) payers.add(p);
			else if (b < 0) takers.add(p);
		}
		payers.sort((a, b) -> Long.compare(bal.get(b), bal.get(a)));
		takers.sort((a, b) -> Long.compare(bal.get(a), bal.get(b)));
		List<Transfer> out = new ArrayList<>();
		int i = 0;
		int j = 0;
		while (i < payers.size() && j < takers.size())
		{
			String p = payers.get(i);
			String t = takers.get(j);
			long amount = Math.min(bal.get(p), -bal.get(t));
			if (amount > 0)
			{
				out.add(new Transfer(p, t, amount));
			}
			bal.put(p, bal.get(p) - amount);
			bal.put(t, bal.get(t) + amount);
			if (bal.get(p) == 0) i++;
			if (bal.get(t) == 0) j++;
		}
		return out;
	}

	public synchronized String summary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(L.t("SWEAT NATION Vipps-oppgjør – ", "SWEAT NATION Vipps split – ")).append(title).append('\n');
		sb.append("Total: ").append(Gp.format(getTotal())).append(" gp · ").append(participants.size())
			.append(L.t(" spillere · andel ", " players · share ")).append(Gp.format(getShare())).append(L.t(" gp hver\n", " gp each\n"));
		for (String p : participants)
		{
			sb.append("  ").append(p).append(L.t(": fikk ", ": received ")).append(Gp.format(getReceived(p))).append('\n');
		}
		List<Transfer> t = settle();
		if (!t.isEmpty())
		{
			sb.append(L.t("Oppgjør:\n", "Settlement:\n"));
			for (Transfer tr : t)
			{
				sb.append("  ").append(tr.from).append(" → ").append(tr.to).append(": ")
					.append(Gp.format(tr.amount)).append(" gp\n");
			}
		}
		return sb.toString();
	}
}
