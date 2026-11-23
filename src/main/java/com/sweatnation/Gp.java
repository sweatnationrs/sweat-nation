package com.sweatnation;

/** Formaterer gp som i OSRS: 950, 12.5K, 3.25M, 1.1B. */
final class Gp
{
	private Gp()
	{
	}

	static String format(long v)
	{
		long a = Math.abs(v);
		String s;
		if (a >= 1_000_000_000L) s = trim(a / 1_000_000_000.0) + "B";
		else if (a >= 1_000_000L) s = trim(a / 1_000_000.0) + "M";
		else if (a >= 10_000L) s = trim(a / 1_000.0) + "K";
		else s = String.valueOf(a);
		return v < 0 ? "-" + s : s;
	}

	private static String trim(double d)
	{
		String s = String.format(java.util.Locale.ROOT, d >= 100 ? "%.0f" : d >= 10 ? "%.1f" : "%.2f", d);
		if (s.contains("."))
		{
			s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return s;
	}
}
