package com.sweatnation.messages;

import net.runelite.client.party.messages.PartyMessage;

/** Felles base for SweatNation-meldinger. sender = avsenderens party-medlems-id (settes ved sending). */
public abstract class SnMessage extends PartyMessage
{
	public long sender;
}
