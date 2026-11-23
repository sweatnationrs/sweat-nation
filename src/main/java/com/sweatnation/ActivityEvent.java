package com.sweatnation;

/** Én hendelse i aktivitetsloggen: et drap, loot, clue, quest, level osv. */
public class ActivityEvent
{
	public enum Type
	{
		KILL("Drap", "Kills"),
		LOOT("Loot", "Loot"),
		CLUE("Clue", "Clue"),
		QUEST("Quest", "Quest"),
		LEVEL("Level", "Level"),
		PET("Pet", "Pet"),
		COLLECTION("Collection log", "Collection log"),
		DEATH("Død", "Death");

		private final String label;
		private final String labelEn;

		Type(String label, String labelEn)
		{
			this.label = label;
			this.labelEn = labelEn;
		}

		public String getLabel()
		{
			return L.t(label, labelEn);
		}
	}

	public String id;
	public String player;
	public String type;
	public String text;
	/** Samme tekst på engelsk (eldre hendelser har bare norsk). */
	public String textEn;
	public long value;
	public long time;

	public ActivityEvent()
	{
	}

	public ActivityEvent(String id, String player, Type type, String text, long value, long time)
	{
		this.id = id;
		this.player = player;
		this.type = type.name();
		this.text = text;
		this.value = value;
		this.time = time;
	}

	/** Teksten på språket som er valgt. */
	public String displayText()
	{
		return L.isEnglish() && textEn != null ? textEn : text;
	}

	public Type getType()
	{
		try
		{
			return Type.valueOf(type);
		}
		catch (RuntimeException e)
		{
			return Type.LOOT;
		}
	}
}
