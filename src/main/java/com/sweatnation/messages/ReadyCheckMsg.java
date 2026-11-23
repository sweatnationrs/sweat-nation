package com.sweatnation.messages;

/** Klar-sjekk i en lobby. action = START eller ANSWER. */
public class ReadyCheckMsg extends SnMessage
{
	public String activity;
	public String checkId;
	public String action;
	public String player;
	public boolean ready;

	public ReadyCheckMsg()
	{
	}
}
