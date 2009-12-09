package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;

public class LocalDialog extends JDialog implements MouseListener
{
	private static final long serialVersionUID = 4616634940742071923L;
	private static final int DIALOG_WIDTH = 200;
	private JTextField rootField = new JTextField();
	private JTextField backupField = new JTextField();
	private JCheckBox backup00INDEXBox = new JCheckBox();
	private JCheckBox backupCopyOvers = new JCheckBox();
	private JButton ok = new JButton("Ok");
	private JButton cancel = new JButton("Cancel");
	private Hashtable<JComponent, String> cache = new Hashtable<JComponent, String>();
	private boolean cancelled = false;

	public LocalDialog(Frame f)
	{
		super(f, "Enter local directory information");
		setLocationRelativeTo(this); // center on screen
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		getContentPane().setLayout(new GridBagLayout());
		getContentPane().add(new JLabel("Root Dir:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		rootField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		rootField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(rootField, c);

		c.gridy = 1;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Backup Dir:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		backupField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		backupField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(backupField, c);

		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 3;
		backup00INDEXBox.setText("Backup 00INDEX files");
		backup00INDEXBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		backup00INDEXBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(backup00INDEXBox, c);

		c.gridy = 3;
		c.gridx = 0;
		c.gridwidth = 3;
		backupCopyOvers.setText("Backup Copy-Replaces");
		backupCopyOvers.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		backupCopyOvers.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(backupCopyOvers, c);

		ok.addMouseListener(this);
		c.gridy = 4;
		c.gridx = 1;
		c.gridwidth = 1;
		c.insets.set(10, 10, 10, 10);
		getContentPane().add(ok, c);
		cancel.addMouseListener(this);
		c.gridx = 2;
		getContentPane().add(cancel, c);
		this.pack();

		cache.put(rootField, "");
		cache.put(backupField, "");
		cache.put(backup00INDEXBox, String.valueOf(true));
		cache.put(backupCopyOvers, String.valueOf(true));
	}

	public String getBackupDir()
	{
		return cache.get(backupField);
	}

	public String getRootDir()
	{
		return cache.get(rootField);
	}

	public boolean getBackupIndexes()
	{
		return Boolean.valueOf(cache.get(backup00INDEXBox));
	}

	public boolean getBackupCopyOvers()
	{
		return Boolean.valueOf(cache.get(backupCopyOvers));
	}

	public boolean wasCancelled()
	{
		return cancelled;
	}

	public void fill(String rootDir, String backupDir, boolean backupIndexes, boolean backupCopies)
	{
		rootField.setText(rootDir);
		backupField.setText(backupDir);
		backup00INDEXBox.setSelected(backupIndexes);
		backupCopyOvers.setSelected(backupCopies);
		cache.put(rootField, rootDir);
		cache.put(backupField, backupDir);
		cache.put(backup00INDEXBox, String.valueOf(backupIndexes));
		cache.put(backupCopyOvers, String.valueOf(backupCopies));
	}

	public void mouseClicked(MouseEvent arg0)
	{
		if (arg0.getComponent() == cancel)
		{
			rootField.setText(cache.get(rootField));
			backupField.setText(cache.get(backupField));
			backup00INDEXBox.setSelected(Boolean.valueOf(cache.get(backup00INDEXBox)));
			backupCopyOvers.setSelected(Boolean.valueOf(cache.get(backupCopyOvers)));
			cancelled = true;
			this.setVisible(false);
		}
		else if (arg0.getComponent() == ok)
		{
			if (rootField.getText().trim().length() == 0)
				rootField.requestFocus();
			else
			{
				cache.put(rootField, rootField.getText());
				cache.put(backupField, backupField.getText());
				cache.put(backup00INDEXBox, String.valueOf(backup00INDEXBox.isSelected()));
				cache.put(backupCopyOvers, String.valueOf(backupCopyOvers.isSelected()));
				cancelled = false;
				this.setVisible(false);
			}
		}
	}

	public void setVisible(boolean truefalse)
	{
		if (truefalse)
			cancelled = true;
		super.setVisible(truefalse);
	}

	public void mouseEntered(MouseEvent arg0)
	{
	}

	public void mouseExited(MouseEvent arg0)
	{
	}

	public void mousePressed(MouseEvent arg0)
	{
	}

	public void mouseReleased(MouseEvent arg0)
	{
	}
}
