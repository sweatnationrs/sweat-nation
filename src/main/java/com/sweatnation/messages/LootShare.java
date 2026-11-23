package com.sweatnation.messages;

import java.util.ArrayList;
import java.util.List;

/** Loot en deltaker fikk i en split-økt (fra RuneLite sin Loot Tracker). */
public class LootShare extends SnMessage
{
	public String sessionId;
	public String player;
	public String source;
	public List<Item> items = new ArrayList<>();

	public static class Item
	{
		public String name;
		public int quantity;
		public long value;

		public Item()
		{
		}

		public Item(String name, int quantity, long value)
		{
			this.name = name;
			this.quantity = quantity;
			this.value = value;
		}
	}

	public LootShare()
	{
	}
}
