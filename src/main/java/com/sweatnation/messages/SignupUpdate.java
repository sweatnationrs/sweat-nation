package com.sweatnation.messages;

import java.util.ArrayList;
import java.util.List;

/** Alle gruppene spilleren er med i (sendes ved endring og når noen nye blir med). */
public class SignupUpdate extends SnMessage
{
	public String name;
	public int combatLevel;
	public int world;
	public List<GroupEntry> groups = new ArrayList<>();

	public SignupUpdate()
	{
	}
}
