package com.sweatnation.messages;

import java.util.List;

/** Boostene og buffene til avsenderen akkurat nå. */
public class BuffState extends SnMessage
{
	public String name;
	public List<BuffEntry> buffs;
}
