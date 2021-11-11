package com.planet_ink.zimmerscp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class FileInfoDialog extends JDialog implements MouseListener
{
	private static final long	serialVersionUID	= 4616634940742071922L;
	private final JButton		ok					= new JButton("Ok");
	private static final int	DEFAULT_WIDTH		= 200;

	public FileInfoDialog(final Frame frame, final FileNode node)
	{
		super(frame, "Local file: " + node.getFile().getName());
		JLabel label;
		JTextField field;
		setLocationRelativeTo(this); // center on screen
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.insets.set(3, 3, 3, 3);
		getContentPane().setLayout(new GridBagLayout());
		getContentPane().add(new JLabel("Path:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		field = new JTextField(node.getFile().getAbsolutePath());
		field.setEditable(false);
		field.setOpaque(true);
		field.setFont(field.getFont().deriveFont(Font.BOLD));
		field.setBackground(Color.white);
		field.setMinimumSize(new Dimension(DEFAULT_WIDTH, 21));
		field.setPreferredSize(new Dimension(DEFAULT_WIDTH, 21));
		field.select(0, 0);
		getContentPane().add(field, c);

		c.gridy = 1;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Size:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		label = new JLabel("" + node.getFile().length());
		label.setOpaque(true);
		label.setBackground(Color.white);
		label.setMinimumSize(new Dimension(DEFAULT_WIDTH, 21));
		label.setPreferredSize(new Dimension(DEFAULT_WIDTH, 21));
		getContentPane().add(label, c);

		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Modified:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSSZ");
		label = new JLabel(format.format(Long.valueOf(node.getFile().lastModified())));
		label.setOpaque(true);
		label.setBackground(Color.white);
		label.setMinimumSize(new Dimension(DEFAULT_WIDTH, 21));
		label.setPreferredSize(new Dimension(DEFAULT_WIDTH, 21));
		getContentPane().add(label, c);

		c.gridy = 3;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Type"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		label = new JLabel(node.getFile().isDirectory() ? "Directory" : "File");
		label.setOpaque(true);
		label.setBackground(Color.white);
		label.setMinimumSize(new Dimension(DEFAULT_WIDTH, 21));
		label.setPreferredSize(new Dimension(DEFAULT_WIDTH, 21));
		getContentPane().add(label, c);

		ok.addMouseListener(this);
		c.gridy = 4;
		c.gridx = 1;
		c.gridwidth = 1;
		c.insets.set(10, 10, 10, 10);
		getContentPane().add(ok, c);

		this.pack();
		setLocationRelativeTo(frame);
	}

	public void mouseClicked(final MouseEvent arg0)
	{
		if (arg0.getComponent() == ok)
		{
			this.setVisible(false);
		}
	}

	public void mouseEntered(final MouseEvent arg0)
	{
	}

	public void mouseExited(final MouseEvent arg0)
	{
	}

	public void mousePressed(final MouseEvent arg0)
	{
	}

	public void mouseReleased(final MouseEvent arg0)
	{
	}
}
