package com.sweatnation;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Spiller SweatNation sitt rally-lydklipp (rally.wav). */
final class RallySound
{
	private static final Logger log = LoggerFactory.getLogger(RallySound.class);
	private static byte[] data;

	private RallySound()
	{
	}

	/** Spilles på RuneLite sin delte executor, så klienttråden aldri venter på lyd. */
	static void play(java.util.concurrent.Executor executor, int volumePercent)
	{
		if (volumePercent <= 0)
		{
			return;
		}
		executor.execute(() ->
		{
			try
			{
				byte[] bytes = load();
				if (bytes == null)
				{
					return;
				}
				AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)));
				if (volumePercent > 100)
				{
					in = boosted(in, volumePercent / 100.0);
				}
				Clip clip = AudioSystem.getClip();
				clip.open(in);
				if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
				{
					FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
					// Under 100 %: demp. Over 100 % er forsterkningen allerede gjort i boosted(), så her står vi på 0 dB.
					float db = (float) (20.0 * Math.log10(Math.min(100, volumePercent) / 100.0));
					gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
				}
				clip.addLineListener(e ->
				{
					if (e.getType() == LineEvent.Type.STOP)
					{
						clip.close();
					}
				});
				clip.start();
			}
			catch (Exception e)
			{
				log.warn("SWEAT NATION: kunne ikke spille rally-lyd", e);
			}
		});
	}

	/**
	 * Forsterker 16-bits PCM med en myk limiter: lyden blir høyere, men toppene rundes
	 * av jevnt i stedet for å klippes hardt (det er hard klipping som høres skurrete ut).
	 */
	private static AudioInputStream boosted(AudioInputStream in, double gain) throws IOException
	{
		javax.sound.sampled.AudioFormat f = in.getFormat();
		if (f.getSampleSizeInBits() != 16 || f.getEncoding() != javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED)
		{
			return in; // ukjent format: spill som det er
		}
		byte[] pcm = in.readAllBytes();
		boolean big = f.isBigEndian();
		final double knee = 0.6; // under dette er lyden urørt
		for (int i = 0; i + 1 < pcm.length; i += 2)
		{
			int lo = big ? pcm[i + 1] : pcm[i];
			int hi = big ? pcm[i] : pcm[i + 1];
			double x = (short) ((hi << 8) | (lo & 0xFF)) / 32768.0 * gain;
			double a = Math.abs(x);
			if (a > knee)
			{
				// myk overgang fra knee mot 1.0 (tanh)
				a = knee + (1 - knee) * Math.tanh((a - knee) / (1 - knee));
				x = Math.copySign(a, x);
			}
			int v = (int) Math.round(Math.max(-1, Math.min(1, x)) * 32767);
			if (big)
			{
				pcm[i] = (byte) (v >> 8);
				pcm[i + 1] = (byte) v;
			}
			else
			{
				pcm[i] = (byte) v;
				pcm[i + 1] = (byte) (v >> 8);
			}
		}
		return new AudioInputStream(new ByteArrayInputStream(pcm), f, pcm.length / f.getFrameSize());
	}

	/** Lengden på rally-klippet i millisekunder. */
	static long durationMs()
	{
		try
		{
			byte[] bytes = load();
			if (bytes == null)
			{
				return 1500;
			}
			try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes))))
			{
				return (long) (in.getFrameLength() * 1000.0 / in.getFormat().getFrameRate());
			}
		}
		catch (Exception e)
		{
			return 1500;
		}
	}

	private static synchronized byte[] load() throws IOException
	{
		if (data == null)
		{
			try (InputStream in = RallySound.class.getResourceAsStream("rally.wav"))
			{
				if (in == null)
				{
					return null;
				}
				data = in.readAllBytes();
			}
		}
		return data;
	}
}
