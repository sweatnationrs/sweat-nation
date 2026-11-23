package com.sweatnation.messages;

/** Sendes når du logger inn eller ut av spillet, så de andre vet om du faktisk er i Gielinor. */
public class Presence extends SnMessage
{
	public String name;
	public boolean online;
}
