package com.sweatnation;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;

/**
 * Stil som matcher RuneLite sine egne paneler: mørkegrå flater, oransje aksent
 * og en glatt, lesbar skrift. Lilla brukes bare som SweatNation-merkefarge.
 */
final class OsrsUi
{
	static final Color BG = ColorScheme.DARK_GRAY_COLOR;
	static final Color BG_DARK = ColorScheme.DARKER_GRAY_COLOR;
	static final Color BG_INSET = ColorScheme.DARKER_GRAY_COLOR;
	static final Color HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
	static final Color EDGE = ColorScheme.MEDIUM_GRAY_COLOR;

	static final Color ORANGE = ColorScheme.BRAND_ORANGE;
	static final Color YELLOW = Color.WHITE;
	static final Color GREEN = ColorScheme.PROGRESS_COMPLETE_COLOR;
	static final Color RED = ColorScheme.PROGRESS_ERROR_COLOR;
	static final Color CYAN = new Color(0x7F, 0xC4, 0xFF);
	static final Color PURPLE = new Color(0xB0, 0x7C, 0xFF);
	static final Color TEXT = new Color(0xDC, 0xDC, 0xDC);
	static final Color TEXT_DIM = ColorScheme.LIGHT_GRAY_COLOR;

	private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);

	private OsrsUi()
	{
	}

	static Font font()
	{
		return FONT;
	}

	static Font small()
	{
		return SMALL;
	}

	static Font bold()
	{
		return BOLD;
	}

	/** Beholdt for kompatibilitet: sans-serif viser alle tegn, så fonten brukes som den er. */
	static Font safe(Font f, String text)
	{
		return f;
	}

	static ShadowLabel label(String text, Font f, Color c)
	{
		ShadowLabel l = new ShadowLabel(text);
		l.setFont(f);
		l.setForeground(c);
		return l;
	}

	/** Tynn kant som RuneLite bruker rundt bokser. */
	static Border bevel(boolean raised)
	{
		return BorderFactory.createLineBorder(raised ? EDGE : BG_DARK.darker(), 1);
	}

	static JPanel inset(LayoutManager layout)
	{
		JPanel p = new JPanel(layout);
		p.setBackground(BG_INSET);
		p.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		return p;
	}

	/** Seksjonsoverskrift: hvit, fet tekst med en tynn linje under. */
	static JComponent header(String text)
	{
		ShadowLabel l = label(text, bold(), Color.WHITE);
		l.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, EDGE),
			BorderFactory.createEmptyBorder(8, 0, 4, 0)));
		return l;
	}

	static JTextField textField(String tooltip)
	{
		JTextField f = new JTextField();
		f.setFont(font());
		f.setForeground(Color.WHITE);
		f.setCaretColor(Color.WHITE);
		f.setBackground(BG_DARK);
		f.setToolTipText(tooltip);
		f.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(EDGE, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		return f;
	}

	/** Søkefelt: et forstørrelsesglass vises til høyre til man begynner å skrive. */
	static JTextField searchField()
	{
		JTextField f = new JTextField()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (!getText().isEmpty())
				{
					return;
				}
				Graphics2D g2 = (Graphics2D) g.create();
				smooth(g2);
				int r = 5;
				int cx = getWidth() - 16;
				int cy = getHeight() / 2 - 1;
				g2.setColor(TEXT_DIM);
				g2.setStroke(new java.awt.BasicStroke(1.6f));
				g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
				g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
				g2.drawLine(cx + 4, cy + 4, cx + 8, cy + 8);
				g2.dispose();
			}
		};
		f.setFont(font());
		f.setForeground(Color.WHITE);
		f.setCaretColor(Color.WHITE);
		f.setBackground(BG_DARK);
		f.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(EDGE, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 26)));
		return f;
	}

	private static void smooth(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	/** Vanlig etikett med glatt (antialiased) tekst. */
	static class ShadowLabel extends JLabel
	{
		ShadowLabel(String text)
		{
			super(text);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			smooth(g2);
			super.paintComponent(g2);
			g2.dispose();
		}
	}

	/** Flat knapp i RuneLite-stil. Aktiv fane får oransje strek under. */
	static class StoneButton extends JButton
	{
		private boolean hover;
		private boolean active;
		private Color textColor = Color.WHITE;
		private Color fill;
		private float glow;

		StoneButton(String text)
		{
			super(text);
			setFont(font());
			setFocusPainted(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setMargin(new Insets(5, 8, 5, 8));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hover = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hover = false;
					repaint();
				}
			});
		}

		StoneButton small()
		{
			setFont(OsrsUi.small());
			return this;
		}

		StoneButton textColor(Color c)
		{
			this.textColor = c;
			return this;
		}

		/** Fast bakgrunnsfarge (brukes for rolle-merker). */
		void setFill(Color c)
		{
			this.fill = c;
			repaint();
		}

		/** 0 = ingen glød, 1 = full oransjerød glød (Rohirrim!). */
		void setGlow(float g)
		{
			this.glow = Math.max(0f, Math.min(1f, g));
			repaint();
		}

		void setActive(boolean active)
		{
			this.active = active;
			repaint();
		}

		@Override
		public Dimension getPreferredSize()
		{
			FontMetrics fm = getFontMetrics(getFont());
			Insets m = getMargin();
			return new Dimension(fm.stringWidth(getText()) + m.left + m.right, fm.getHeight() + m.top + m.bottom);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			smooth(g2);
			int w = getWidth();
			int h = getHeight();
			boolean pressed = getModel().isArmed() && getModel().isPressed();
			Color bg = fill != null ? fill
				: active ? BG_DARK : (pressed ? BG_DARK : hover && isEnabled() ? HOVER : BG_DARK.brighter());
			g2.setColor(bg);
			g2.fillRect(0, 0, w, h);
			if (glow > 0)
			{
				g2.setColor(new Color(0xFF, 0x50, 0x20, (int) (200 * glow)));
				g2.fillRect(0, 0, w, h);
				g2.setColor(new Color(0xFF, 0xC0, 0x60, (int) (255 * glow)));
				g2.setStroke(new java.awt.BasicStroke(2f));
				g2.drawRect(1, 1, w - 3, h - 3);
			}
			if (active)
			{
				g2.setColor(ORANGE);
				g2.fillRect(0, h - 2, w, 2);
			}
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			String t = getText();
			int tx = (w - fm.stringWidth(t)) / 2;
			int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
			Color c = fill != null ? textColor
				: glow > 0.3f ? Color.WHITE
				: !isEnabled() ? new Color(0x77, 0x77, 0x77) : active ? ORANGE : textColor;
			g2.setColor(c);
			g2.drawString(t, tx, ty);
			g2.dispose();
		}
	}
}
