package com.sweatnation.messages;

/**
 * Klanens Discord-lenke, delt fra et klanmedlem som har lagt den inn i innstillingene.
 * Lenken ligger aldri i koden – den sendes bare innad i SWEAT NATION-gruppa.
 */
public class DiscordShare extends SnMessage
{
	public String link;

	public DiscordShare()
	{
	}
}
