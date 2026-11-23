package com.sweatnation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spiller SweatNation sitt rally-lydklipp (rally.wav) med RuneLite sin AudioPlayer.
 * Over 100 % forsterkes selve lydbølgen med en myk limiter før avspilling, så det blir høyere uten skurring.
 */
final class RallySound
{
	private static final Logger log = LoggerFactory.getLogger(RallySound.class);
	private static Wav wav;

	private RallySound()
	{
	}

	/** Spilles på RuneLite sin delte executor, så klienttråden aldri venter på lyd. */
	static void play(Executor executor, AudioPlayer player, int volumePercent)
	{
		if (volumePercent <= 0)
		{
			return;
		}
		executor.execute(() ->
		{
			try
			{
				Wav w = load();
				if (w == null)
				{
					return;
				}
				byte[] bytes = volumePercent > 100 && w.bits == 16 ? w.boosted(volumePercent / 100.0) : w.file;
				// Under 100 %: demp med gain i dB. Over 100 % er forsterkningen allerede gjort i boosted().
				float db = (float) (20.0 * Math.log10(Math.min(100, volumePercent) / 100.0));
				player.play(new ByteArrayInputStream(bytes), db);
			}
			catch (Exception e)
			{
				log.warn("SWEAT NATION: kunne ikke spille rally-lyd", e);
			}
		});
	}

	/** Lengden på rally-klippet i millisekunder. */
	static long durationMs()
	{
		try
		{
			Wav w = load();
			return w == null || w.byteRate <= 0 ? 1500 : w.dataLength * 1000L / w.byteRate;
		}
		catch (IOException e)
		{
			return 1500;
		}
	}

	private static synchronized Wav load() throws IOException
	{
		if (wav == null)
		{
			try (InputStream in = RallySound.class.getResourceAsStream("rally.wav"))
			{
				if (in == null)
				{
					return null;
				}
				wav = Wav.parse(in.readAllBytes());
			}
		}
		return wav;
	}

	/** En enkel PCM-WAV lest direkte fra bytene (RIFF-header, «fmt »- og «data»-del). */
	private static final class Wav
	{
		final byte[] file;
		int bits;
		int byteRate;
		int dataOffset = -1;
		int dataLength;

		private Wav(byte[] file)
		{
			this.file = file;
		}

		static Wav parse(byte[] b)
		{
			Wav w = new Wav(b);
			if (b.length < 12 || b[0] != 'R' || b[1] != 'I' || b[2] != 'F' || b[3] != 'F')
			{
				return w;
			}
			int p = 12;
			while (p + 8 <= b.length)
			{
				String id = new String(b, p, 4, java.nio.charset.StandardCharsets.US_ASCII);
				int len = le32(b, p + 4);
				int body = p + 8;
				if (id.equals("fmt ") && body + 16 <= b.length)
				{
					w.byteRate = le32(b, body + 8);
					w.bits = le16(b, body + 14);
				}
				else if (id.equals("data"))
				{
					w.dataOffset = body;
					w.dataLength = Math.min(len, b.length - body);
					break;
				}
				p = body + len + (len & 1);
			}
			return w;
		}

		/** Kopi av fila der 16-bits lyden er forsterket med en myk limiter (tanh over knekkpunktet). */
		byte[] boosted(double gain)
		{
			if (dataOffset < 0)
			{
				return file;
			}
			byte[] out = file.clone();
			final double knee = 0.6; // under dette er lyden urørt
			int end = dataOffset + dataLength - 1;
			for (int i = dataOffset; i < end; i += 2)
			{
				double x = (short) ((out[i + 1] << 8) | (out[i] & 0xFF)) / 32768.0 * gain;
				double a = Math.abs(x);
				if (a > knee)
				{
					a = knee + (1 - knee) * Math.tanh((a - knee) / (1 - knee));
					x = Math.copySign(a, x);
				}
				int v = (int) Math.round(Math.max(-1, Math.min(1, x)) * 32767);
				out[i] = (byte) v;
				out[i + 1] = (byte) (v >> 8);
			}
			return out;
		}

		private static int le16(byte[] b, int i)
		{
			return (b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8;
		}

		private static int le32(byte[] b, int i)
		{
			return (b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8 | (b[i + 2] & 0xFF) << 16 | (b[i + 3] & 0xFF) << 24;
		}
	}
}
