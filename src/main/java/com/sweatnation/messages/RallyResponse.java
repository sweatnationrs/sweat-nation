package com.sweatnation.messages;


/** Svar på et rally: kommer / kommer ikke. */
public class RallyResponse extends SnMessage
{
	public String rallyId;
	public String name;
	public boolean coming;

	public RallyResponse()
	{
	}

	public RallyResponse(String rallyId, String name, boolean coming)
	{
		this.rallyId = rallyId;
		this.name = name;
		this.coming = coming;
	}
}
