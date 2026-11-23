package com.sweatnation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aktivitetsloggen for gruppa. Lagres i .runelite/sweatnation/ per gruppe,
 * så historikken er der neste gang du logger inn.
 */
public class ActivityLog
{
	private static final Logger log = LoggerFactory.getLogger(ActivityLog.class);
	private static final int MAX = 500;
	private static final Type LIST_TYPE = new TypeToken<List<ActivityEvent>>() { }.getType();

	private final Gson gson;
	private final File dir;
	private final Map<String, ActivityEvent> events = new LinkedHashMap<>();
	private File file;
	private volatile boolean dirty;

	public ActivityLog(Gson gson, File dir)
	{
		this.gson = gson;
		this.dir = dir;
	}

	/** Bytt til loggen for en gruppe (eller ingen). */
	public synchronized void open(String groupCode)
	{
		events.clear();
		dirty = false;
		if (groupCode == null)
		{
			file = null;
			return;
		}
		String safe = groupCode.toLowerCase().replaceAll("[^a-z0-9-]+", "_");
		file = new File(dir, "historikk-" + safe + ".json");
		if (!file.exists())
		{
			return;
		}
		try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			List<ActivityEvent> list = gson.fromJson(r, LIST_TYPE);
			if (list != null)
			{
				for (ActivityEvent e : list)
				{
					if (e != null && e.id != null)
					{
						events.put(e.id, e);
					}
				}
			}
		}
		catch (Exception e)
		{
			log.warn("SWEAT NATION: kunne ikke lese historikk", e);
		}
	}

	/** Legger til en hendelse. Returnerer false hvis den allerede finnes. */
	public synchronized boolean add(ActivityEvent e)
	{
		if (e == null || e.id == null || events.containsKey(e.id))
		{
			return false;
		}
		events.put(e.id, e);
		while (events.size() > MAX)
		{
			String oldest = null;
			long t = Long.MAX_VALUE;
			for (ActivityEvent x : events.values())
			{
				if (x.time < t)
				{
					t = x.time;
					oldest = x.id;
				}
			}
			events.remove(oldest);
		}
		dirty = true;
		return true;
	}

	/** Nyeste først. */
	public synchronized List<ActivityEvent> list()
	{
		List<ActivityEvent> l = new ArrayList<>(events.values());
		l.sort((a, b) -> Long.compare(b.time, a.time));
		return l;
	}

	/** Mine siste hendelser, for å synkronisere nykommere. */
	public synchronized List<ActivityEvent> latestBy(String player, int max)
	{
		List<ActivityEvent> out = new ArrayList<>();
		for (ActivityEvent e : list())
		{
			if (player.equals(e.player))
			{
				out.add(e);
				if (out.size() >= max)
				{
					break;
				}
			}
		}
		Collections.reverse(out);
		return out;
	}

	public synchronized void clear()
	{
		events.clear();
		dirty = true;
	}

	/** Skriver til disk hvis noe er endret. Kalles fra en bakgrunnstråd. */
	public void saveIfDirty()
	{
		File f;
		String json;
		synchronized (this)
		{
			if (!dirty || file == null)
			{
				return;
			}
			dirty = false;
			f = file;
			json = gson.toJson(new ArrayList<>(events.values()), LIST_TYPE);
		}
		try
		{
			dir.mkdirs();
			File tmp = new File(dir, f.getName() + ".tmp");
			try (Writer w = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8))
			{
				w.write(json);
			}
			Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (Exception e)
		{
			log.warn("SWEAT NATION: kunne ikke lagre historikk", e);
		}
	}
}
