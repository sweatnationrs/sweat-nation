package com.sweatnation;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

/** Det panelet trenger fra pluginen (gjør panelet testbart uten spillklient). */
public interface SweatNationController
{
	boolean isInGroup();
	String getGroupLabel();
	void joinGroup(String code);
	void joinClanGroup();
	void leaveGroup();
	void createGroup();
	void copyGroupCode();
	List<ActivityEvent> getActivity();
	long getMinLootLog();
	int getOnlineCount();
	boolean isShortestPathAvailable();
	List<String> getTeleportAdvice();
	String describePlace(WorldPoint p);
	String getLocalName();

	boolean isLiveMap();
	void setLiveMap(boolean on);
	List<MemberState> getMembers();
	List<DeathRecord> getDeaths();
	WorldPoint getLocalLocation();
	int getLocalWorld();
	void focusOnMap(WorldPoint point);
	void hopToWorld(int world);

	RallyState getActiveRally();
	boolean isMyRally();
	void rallyToMe(String note);
	void respondToRally(boolean coming);
	void cancelRally();


	SplitSession getSplit();
	boolean isInSplit();
	void startSplit(String title);
	void startSplit(String title, List<String> participants);
	void joinSplit();
	void leaveSplit();
	void stopSplit();
	void closeSplit();
	void copySplitSummary();

	List<GroupView> getGroups();
	void createTeam(Activity activity);
	void createCustomTeam(String name, int size);
	void joinTeam(String key);
	void leaveTeam(String key);
	void changeTeamSize(String key, int delta);
	void cycleRole(String key);
	ReadyCheck getReadyCheck(String key);
	void startReadyCheck(String key);
	void answerReadyCheck(String key, boolean ready);

	boolean isTeamHud();
	void setTeamHud(boolean on);
	int getHudScalePercent();
	void setHudScalePercent(int pct);

	String getDiscordLink();
	boolean hasInvalidDiscordLink();
	void openDiscord();
}
