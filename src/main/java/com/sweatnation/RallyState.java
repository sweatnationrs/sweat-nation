package com.sweatnation;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/** Et aktivt rally, med hvem som har svart. */
public class RallyState
{
	public enum Type { TO_ME, POINT }

	private final String id;
	private final long callerId;
	private final String callerName;
	private final WorldPoint target;
	private final int world;
	private final Type type;
	private final String note;
	private final long expiresAt;
	private final Map<String, Boolean> responses = new LinkedHashMap<>();

	public RallyState(String id, long callerId, String callerName, WorldPoint target, int world,
		Type type, String note, long expiresAt)
	{
		this.id = id;
		this.callerId = callerId;
		this.callerName = callerName;
		this.target = target;
		this.world = world;
		this.type = type;
		this.note = note;
		this.expiresAt = expiresAt;
	}

	public String getId() { return id; }
	public long getCallerId() { return callerId; }
	public String getCallerName() { return callerName; }
	public WorldPoint getTarget() { return target; }
	public int getWorld() { return world; }
	public Type getType() { return type; }
	public String getNote() { return note; }
	public long getExpiresAt() { return expiresAt; }
	public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }

	public synchronized void setResponse(String name, boolean coming) { responses.put(name, coming); }
	public synchronized Map<String, Boolean> getResponses() { return new LinkedHashMap<>(responses); }

	public synchronized int comingCount()
	{
		int n = 0;
		for (Boolean b : responses.values())
		{
			if (b) n++;
		}
		return n;
	}
}
