package com.sweatnation.messages;

/** Et lite ikon (PNG, base64) for en buff. Sendes én gang per ikon, så BuffState kan vise til det med id. */
public class BuffIcon extends SnMessage
{
	public String id;
	public String png;
}
