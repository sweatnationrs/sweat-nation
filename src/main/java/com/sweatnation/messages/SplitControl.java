package com.sweatnation.messages;

import java.util.ArrayList;
import java.util.List;

/** Styrer en split loot-økt: START, JOIN, LEAVE, STOP. */
public class SplitControl extends SnMessage
{
	public String sessionId;
	public String action;
	public String player;
	public String title;
	public List<String> participants = new ArrayList<>();

	public SplitControl()
	{
	}
}
