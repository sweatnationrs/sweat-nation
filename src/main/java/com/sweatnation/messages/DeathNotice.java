package com.sweatnation.messages;


/** Et medlem døde her – vises som rødt dødningshode på kartet. */
public class DeathNotice extends SnMessage
{
	public String name;
	public int x;
	public int y;
	public int plane;
	public int world;
	/** Monsternavn eller «spilleren X». Tom/null = ukjent. */
	public String killer;

	public DeathNotice()
	{
	}
}
