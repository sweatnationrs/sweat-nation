package com.sweatnation;

/**
 * Språk: norsk (standard) eller engelsk. Bruk L.t("norsk tekst", "English text").
 */
public final class L
{
	private static volatile boolean english;

	private L()
	{
	}

	public static void setEnglish(boolean on)
	{
		english = on;
	}

	public static boolean isEnglish()
	{
		return english;
	}

	/** Velger teksten for språket som er valgt. */
	public static String t(String norwegian, String englishText)
	{
		return english ? englishText : norwegian;
	}
}
