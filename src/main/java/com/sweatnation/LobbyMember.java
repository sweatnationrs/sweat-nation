package com.sweatnation;

/** En påmeldt i lobbyen: navn, rolle og svar på klar-sjekken (null = ikke svart). */
public class LobbyMember
{
	public final String name;
	public final TeamRole role;
	public final Boolean ready;
	/** Minutter til en utlogget spiller fjernes fra gruppa (-1 = pålogget). */
	public int offlineMinutesLeft = -1;

	public LobbyMember(String name, TeamRole role, Boolean ready)
	{
		this.name = name;
		this.role = role;
		this.ready = ready;
	}
}
