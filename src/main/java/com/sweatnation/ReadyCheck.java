package com.sweatnation;

import java.util.LinkedHashMap;
import java.util.Map;

/** En klar-sjekk i en lobby. */
public class ReadyCheck
{
	public static final long TIMEOUT_MS = 30_000L;

	private final String id;
	private final String initiator;
	private final long startedAt = System.currentTimeMillis();
	private final Map<String, Boolean> answers = new LinkedHashMap<>();
	private volatile boolean completed;

	public ReadyCheck(String id, String initiator)
	{
		this.id = id;
		this.initiator = initiator;
	}

	public String getId() { return id; }
	public String getInitiator() { return initiator; }
	public boolean isExpired() { return System.currentTimeMillis() - startedAt > TIMEOUT_MS; }
	public long secondsLeft() { return Math.max(0, (TIMEOUT_MS - (System.currentTimeMillis() - startedAt)) / 1000); }
	public boolean isCompleted() { return completed; }
	public void complete() { completed = true; }

	public synchronized void answer(String player, boolean ready) { answers.put(player, ready); }
	public synchronized Boolean answerOf(String player) { return answers.get(player); }
}
