package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;


public class RemoteDialog extends JDialog implements MouseListener
{
	private static final long	serialVersionUID	= 4616634940742071923L;
	private static final int	DIALOG_WIDTH		= 200;
	private final JTextField	hostField			= new JTextField();
	private final JTextField	userField			= new JTextField();
	private final JTextField	passwordField		= new JTextField();
	private final JTextField	rootField			= new JTextField();
	private final JTextField	portMaxavField		= new JTextField();
	private final JCheckBox		manageIndexBox		= new JCheckBox();
	private final JCheckBox		manageSyncBox		= new JCheckBox();
	private final JCheckBox		create00INDEXBox	= new JCheckBox();
	private final JButton		ok					= new JButton("Ok");
	private final JButton		cancel				= new JButton("Cancel");
	private boolean				cancelled			= false;

	private final Hashtable<JComponent, String> cache = new Hashtable<JComponent, String>();

	public RemoteDialog(final Frame f)
	{
		super(f, "Remote connection settings");
		setLocationRelativeTo(this); // center on screen
		final GridBagConstraints c = new GridBagConstraints();
		
		int gridy=-1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = ++gridy;
		c.gridwidth = 1;
		getContentPane().setLayout(new GridBagLayout());
		getContentPane().add(new JLabel("Host:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		hostField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		hostField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(hostField, c);

		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("User:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		userField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		userField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(userField, c);

		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Password:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		passwordField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		passwordField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(passwordField, c);

		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("Root Dir:"), c);
		c.gridx = 1;
		c.gridwidth = 2;
		rootField.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		rootField.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(rootField, c);

		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 1;
		getContentPane().add(new JLabel("SSHD Port:"), c);
		c.gridx = 1;
		c.gridwidth = 1;
		portMaxavField.setMinimumSize(new Dimension(DIALOG_WIDTH/4, 21));
		portMaxavField.setPreferredSize(new Dimension(DIALOG_WIDTH/4, 21));
		getContentPane().add(portMaxavField, c);

		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 3;
		manageIndexBox.setText("Manage 00INDEX files");
		manageIndexBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		manageIndexBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(manageIndexBox, c);

		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 3;
		manageSyncBox.setText("Sync With Others");
		manageSyncBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		manageSyncBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(manageSyncBox, c);


		c.gridy = ++gridy;
		c.gridx = 0;
		c.gridwidth = 3;
		create00INDEXBox.setText("Create 00INDEX files");
		create00INDEXBox.setMinimumSize(new Dimension(DIALOG_WIDTH, 21));
		create00INDEXBox.setPreferredSize(new Dimension(DIALOG_WIDTH, 21));
		getContentPane().add(create00INDEXBox, c);

		ok.addMouseListener(this);
		c.gridy = ++gridy;
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
		cache.put(portMaxavField, "22");
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

	public int getSSHDPort()
	{
		try
		{
			return Integer.parseInt(cache.get(portMaxavField).trim());
		}
		catch(final Exception e)
		{
			return 22;
		}
	}

	public boolean getManageIndex()
	{
		return Boolean.valueOf(cache.get(manageIndexBox)).booleanValue();
	}

	public boolean getManageSync()
	{
		return Boolean.valueOf(cache.get(manageSyncBox)).booleanValue();
	}

	public boolean getCreate00INDEX()
	{
		return Boolean.valueOf(cache.get(create00INDEXBox)).booleanValue();
	}

	public boolean wasCancelled()
	{
		return cancelled;
	}

	public void fill(final String host, final String user, final String password, final String root, final int port, 
					final boolean manageIndexes, final boolean keepSync, final boolean create00INDEX)
	{
		hostField.setText(host);
		userField.setText(user);
		passwordField.setText(password);
		rootField.setText(root);
		portMaxavField.setText(""+port);
		manageIndexBox.setSelected(manageIndexes);
		manageSyncBox.setSelected(keepSync);
		create00INDEXBox.setSelected(create00INDEX);
		cache.put(hostField, host);
		cache.put(userField, user);
		cache.put(passwordField, password);
		cache.put(rootField, root);
		cache.put(portMaxavField, ""+port);
		cache.put(manageIndexBox, String.valueOf(manageIndexes));
		cache.put(manageSyncBox, String.valueOf(keepSync));
		cache.put(create00INDEXBox, String.valueOf(create00INDEX));
	}

	public boolean isInt(final String s)
	{
		try
		{
			int port = Integer.valueOf(s.trim());
			if(port >=0)
				return true;
		}
		catch(Exception e)
		{}
		return false;
	}
	
	public void mouseClicked(final MouseEvent arg0)
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
			else 
			if (userField.getText().trim().length() == 0)
				userField.requestFocus();
			else 
			if (passwordField.getText().trim().length() == 0)
				passwordField.requestFocus();
			else
			if (rootField.getText().trim().length() == 0)
				rootField.requestFocus();
			else
			if (!isInt(portMaxavField.getText()))
				portMaxavField.requestFocus();
			else
			{
				cache.put(hostField, hostField.getText());
				cache.put(userField, userField.getText());
				cache.put(passwordField, passwordField.getText());
				cache.put(rootField, rootField.getText());
				cache.put(portMaxavField, portMaxavField.getText().trim());
				cache.put(manageIndexBox, String.valueOf(manageIndexBox.isSelected()));
				cache.put(manageSyncBox, String.valueOf(manageSyncBox.isSelected()));
				cache.put(create00INDEXBox, String.valueOf(create00INDEXBox.isSelected()));
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
