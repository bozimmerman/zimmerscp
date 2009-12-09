package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;


public class RemoteDialog extends JDialog implements MouseListener
{
	private static final long serialVersionUID = 4616634940742071923L;
	private static final int DIALOG_WIDTH = 200;
	private JTextField hostField = new JTextField();
	private JTextField userField = new JTextField();
	private JTextField passwordField = new JTextField();
	private JTextField rootField = new JTextField();
	private JCheckBox manageIndexBox = new JCheckBox();
	private JCheckBox manageSyncBox = new JCheckBox();
	private JCheckBox create00INDEXBox = new JCheckBox();
	private JButton ok = new JButton("Ok");
	private JButton cancel = new JButton("Cancel");
	private Hashtable<JComponent, String> cache = new Hashtable<JComponent, String>();
	private boolean cancelled = false;

	public RemoteDialog(Frame f)
	{
		super(f, "Enter remote connection information");
		setLocationRelativeTo(this); // center on screen
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		getContentPane().setLayout(new GridBagLayout());
		getContentPane().add(new JLabel("Host:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		hostField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		hostField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(hostField, c);

		c.gridy = 1;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("User:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		userField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		userField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(userField, c);

		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Password:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		passwordField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		passwordField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(passwordField, c);

		c.gridy = 3;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Root Dir:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		rootField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		rootField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(rootField, c);

		c.gridy = 4;
		c.gridx = 0;
		c.gridwidth = 3;
		manageIndexBox.setText("Manage 00INDEX files");
		manageIndexBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		manageIndexBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(manageIndexBox, c);

		c.gridy = 5;
		c.gridx = 0;
		c.gridwidth = 3;
		manageSyncBox.setText("Sync With Others");
		manageSyncBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		manageSyncBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(manageSyncBox, c);


		c.gridy = 6;
		c.gridx = 0;
		c.gridwidth = 3;
		create00INDEXBox.setText("Create 00INDEX files");
		create00INDEXBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		create00INDEXBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(create00INDEXBox, c);
		
		ok.addMouseListener(this);
		c.gridy = 7;
		c.gridx = 1;
		c.gridwidth = 1;
		c.insets.set(10, 10, 10, 10);
		getContentPane().add(ok, c);
		cancel.addMouseListener(this);
		c.gridx = 2;
		getContentPane().add(cancel, c);
		this.pack();

		cache.put(hostField, "");
		cache.put(userField, "");
		cache.put(passwordField, "");
		cache.put(rootField, "");
		cache.put(manageIndexBox, String.valueOf(true));
		cache.put(manageSyncBox, String.valueOf(true));
		cache.put(create00INDEXBox, String.valueOf(false));
	}

	public String getHost()
	{
		return cache.get(hostField);
	}

	public String getUser()
	{
		return cache.get(userField);
	}

	public String getPassword()
	{
		return cache.get(passwordField);
	}

	public String getRoot()
	{
		return cache.get(rootField);
	}

	public boolean getManageIndex()
	{
		return Boolean.valueOf(cache.get(manageIndexBox));
	}

	public boolean getManageSync()
	{
		return Boolean.valueOf(cache.get(manageSyncBox));
	}

	public boolean getCreate00INDEX()
	{
		return Boolean.valueOf(cache.get(create00INDEXBox));
	}

	public boolean wasCancelled()
	{
		return cancelled;
	}

	public void fill(String host, String user, String password, String root, boolean manageIndexes, boolean keepSync, boolean create00INDEX)
	{
		hostField.setText(host);
		userField.setText(user);
		passwordField.setText(password);
		rootField.setText(root);
		manageIndexBox.setSelected(manageIndexes);
		manageSyncBox.setSelected(keepSync);
		create00INDEXBox.setSelected(create00INDEX);
		cache.put(hostField, host);
		cache.put(userField, user);
		cache.put(passwordField, password);
		cache.put(rootField, root);
		cache.put(manageIndexBox, String.valueOf(manageIndexes));
		cache.put(manageSyncBox, String.valueOf(keepSync));
		cache.put(create00INDEXBox, String.valueOf(create00INDEX));
	}

	public void mouseClicked(MouseEvent arg0)
	{
		if (arg0.getComponent() == cancel)
		{
			cancelled = true;
			this.setVisible(false);
		}
		else if (arg0.getComponent() == ok)
		{
			if (hostField.getText().trim().length() == 0)
				hostField.requestFocus();
			else if (userField.getText().trim().length() == 0)
				userField.requestFocus();
			else if (passwordField.getText().trim().length() == 0)
				passwordField.requestFocus();
			else if (rootField.getText().trim().length() == 0)
				rootField.requestFocus();
			else
			{
				cache.put(hostField, hostField.getText());
				cache.put(userField, userField.getText());
				cache.put(passwordField, passwordField.getText());
				cache.put(rootField, rootField.getText());
				cache.put(manageIndexBox, String.valueOf(manageIndexBox.isSelected()));
				cache.put(manageSyncBox, String.valueOf(manageSyncBox.isSelected()));
				cache.put(create00INDEXBox, String.valueOf(create00INDEXBox.isSelected()));
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
