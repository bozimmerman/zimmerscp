package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.*;

public class LocalDialog extends JDialog implements MouseListener
{
	private static final long	serialVersionUID	= 4616634940742071923L;
	private static final int	DIALOG_WIDTH		= 200;
	private final JTextField	rootField			= new JTextField();
	private final JTextField	backupField			= new JTextField();
	private final JCheckBox		backup00INDEXBox	= new JCheckBox();
	private final JCheckBox		backupCopyOvers		= new JCheckBox();
	private final JCheckBox		blank00INDEXDesc	= new JCheckBox();
	private final JButton		ok					= new JButton("Ok");
	private final JButton		cancel				= new JButton("Cancel");
	private boolean				cancelled			= false;

	private final Map<JComponent, String> cache = new Hashtable<JComponent, String>();

	public LocalDialog(final Frame f)
	{
		super(f, "Local filesystem settings");
		setLocationRelativeTo(this); // center on screen
		final GridBagConstraints c = new GridBagConstraints();
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

		c.gridy = 4;
		c.gridx = 0;
		c.gridwidth = 3;
		blank00INDEXDesc.setText("No 00INDEX Descs");
		blank00INDEXDesc.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		blank00INDEXDesc.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(blank00INDEXDesc, c);


		ok.addMouseListener(this);
		c.gridy = 5;
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
		return Boolean.valueOf(cache.get(backup00INDEXBox)).booleanValue();
	}

	public boolean getBackupCopyOvers()
	{
		return Boolean.valueOf(cache.get(backupCopyOvers)).booleanValue();
	}

	public boolean getBlankDescriptions()
	{
		return Boolean.valueOf(cache.get(blank00INDEXDesc)).booleanValue();
	}

	public boolean wasCancelled()
	{
		return cancelled;
	}

	public void fill(final String rootDir,
					 final String backupDir,
					 final boolean backupIndexes,
					 final boolean backupCopies,
					 final boolean blankDescriptions)
	{
		rootField.setText(rootDir);
		backupField.setText(backupDir);
		backup00INDEXBox.setSelected(backupIndexes);
		backupCopyOvers.setSelected(backupCopies);
		blank00INDEXDesc.setSelected(blankDescriptions);
		cache.put(rootField, rootDir);
		cache.put(backupField, backupDir);
		cache.put(backup00INDEXBox, String.valueOf(backupIndexes));
		cache.put(backupCopyOvers, String.valueOf(backupCopies));
		cache.put(blank00INDEXDesc, String.valueOf(blankDescriptions));
	}

	public void mouseClicked(final MouseEvent arg0)
	{
		if (arg0.getComponent() == cancel)
		{
			rootField.setText(cache.get(rootField));
			backupField.setText(cache.get(backupField));
			backup00INDEXBox.setSelected(Boolean.valueOf(cache.get(backup00INDEXBox)).booleanValue());
			backupCopyOvers.setSelected(Boolean.valueOf(cache.get(backupCopyOvers)).booleanValue());
			blank00INDEXDesc.setSelected(Boolean.valueOf(cache.get(blank00INDEXDesc)).booleanValue());
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
				cache.put(blank00INDEXDesc, String.valueOf(blank00INDEXDesc.isSelected()));
				cancelled = false;
				this.setVisible(false);
			}
		}
	}

	public void setVisible(final boolean truefalse)
	{
		if (truefalse)
			cancelled = true;
		super.setVisible(truefalse);
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
