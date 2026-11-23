package com.sweatnation.messages;


/** Et rally-anrop: "kom til meg" eller "møt her" (punkt på kartet). cancel=true avslutter et rally. */
public class RallyCall extends SnMessage
{
	public String rallyId;
	public String callerName;
	public int x;
	public int y;
	public int plane;
	public int world;
	/** "TO_ME" eller "POINT" */
	public String type;
	public String note;
	public int durationMinutes;
	public boolean cancel;
	/** true = ropt manuelt (fanen/kartet) – Rohirrim!-knappen lyser. false = fra lobby/klar-sjekk. */
	public boolean glow;

	public RallyCall()
	{
	}
}
