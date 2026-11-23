package com.sweatnation;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starter RuneLite med SweatNation lastet: ./gradlew run */
public class SweatNationPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SweatNationPlugin.class);
		RuneLite.main(args);
	}
}
