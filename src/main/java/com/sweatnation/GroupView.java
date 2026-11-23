package com.sweatnation;

import java.util.List;

/** En gruppe slik panelet viser den: aktivitet, navn, størrelse og medlemmer. */
public class GroupView
{
	public final String key;
	public final Activity activity;
	public final String title;
	public final int size;
	public final String leader;
	public final List<LobbyMember> members;
	public final boolean mine;

	public GroupView(String key, Activity activity, String title, int size, String leader, List<LobbyMember> members, boolean mine)
	{
		this.key = key;
		this.activity = activity;
		this.title = title;
		this.size = size;
		this.leader = leader;
		this.members = members;
		this.mine = mine;
	}

	public boolean isFull()
	{
		return members.size() >= size;
	}

	public List<String> names()
	{
		java.util.List<String> n = new java.util.ArrayList<>();
		for (LobbyMember m : members)
		{
			n.add(m.name);
		}
		return n;
	}
}
