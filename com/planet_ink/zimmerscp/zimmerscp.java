package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.TreePath;

public class zimmerscp extends javax.swing.JFrame implements MouseListener, WindowListener
{
	private static final long serialVersionUID = -2866392485562765792L;
	private static final int DEFAULT_COL_WIDTH = 250;
	private static final double VERSION=1.0;
	private JSplitPane jSplitPane1;
	private SourceTree jTreeS;
	private DestTree jTreeD1;
	private DestTree jTreeD2;
	private JLabel jTreeTextS;
	private JLabel jTreeTextD1;
	private JLabel jTreeTextD2;
	private JSplitPane jSplitPane2;
	private JSplitPane jSplitPane0;
	private JTextField jSettingsTextField = null;
	private List<Properties> allSettings=new Vector<Properties>();

	public static zimmerscp INSTANCE;

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				INSTANCE = new zimmerscp();
				INSTANCE.setLocationRelativeTo(null);
				INSTANCE.setVisible(true);
			}
		});
	}

	public zimmerscp()
	{
		super();
		initGUI();
		SwingUtilities.invokeLater(new Runnable()
		{
			zimmerscp myForm = null;

			public Runnable initMe(zimmerscp form)
			{
				this.myForm = form;
				return this;
			}

			public void run()
			{
				try
				{
					myForm.loadSettings();
				}
				catch (Exception e)
				{
				}
			}
		}.initMe(this));
	}
	
	private Properties findPropsByName(String name)
	{
		for(int i=0;i<allSettings.size();i++)
		{
			Properties p=allSettings.get(i);
			String n=p.getProperty("name");
			if(n.trim().equalsIgnoreCase(name.trim()))
				return p;
		}
		return null;
	}

	private void initGUI()
	{
		try
		{
			final JFrame me = this;
			this.setTitle("ZimmerSCP v"+VERSION);
			this.addWindowListener(this);
			{
				FileNode jTreeModelS = new FileNode(null);
				jTreeS = new SourceTree("jTreeModelS", this, jTreeModelS);
				jTreeModelS.setTree(jTreeS);
				jTreeS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 400));
				jTreeS.addMouseListener(this);
				jTreeTextS = new JLabel("Local: <Click Here>");
				jTreeTextS.setOpaque(true);
				jTreeTextS.addMouseListener(this);
				jTreeTextS.setBackground(new Color(255, 255, 140));
				jTreeTextS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 11));
				JPanel jTreePanelS = new JPanel();
				jTreePanelS.setLayout(new BorderLayout());
				jTreePanelS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 400));
				jTreePanelS.add(jTreeTextS, BorderLayout.NORTH);
				JScrollPane jScrollTreeS = new JScrollPane(jTreeS);
				jScrollTreeS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 400));
				jTreePanelS.add(jScrollTreeS, BorderLayout.CENTER);
				
				JPanel jSettingsPanel = new JPanel();
				jSettingsPanel.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 100));
				jSettingsPanel.setSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 100));
				final JComboBox jSettingsListBox = new JComboBox();
				jSettingsListBox.setPreferredSize(new Dimension(DEFAULT_COL_WIDTH-40, 20));
				final List<ListDataListener> listener=new LinkedList<ListDataListener>();
				jSettingsListBox.setModel(new ComboBoxModel(){
					private Object selectedOne = null;
					@Override public int getSize() { return allSettings.size(); }
					@Override public Object getElementAt(int index) {
						if((index<0)||(index>=allSettings.size())) return null;
						Properties P=allSettings.get(index);
						return P.containsKey("name")?P.get("name"):"Unknown?!";
					}
					@Override public void addListDataListener(ListDataListener l) { listener.add(l); }
					@Override public void removeListDataListener(ListDataListener l) { listener.remove(l); }
					@Override public void setSelectedItem(Object anItem) { selectedOne = anItem; }
					@Override public Object getSelectedItem() { return selectedOne; }
				});
				jSettingsTextField = new JTextField();
				jSettingsTextField.setColumns(20);
				jSettingsTextField.setEditable(true);
				JButton jLoadButton = new JButton("Load");
				jLoadButton.addActionListener(new ActionListener(){
					@Override public void actionPerformed(ActionEvent e) {
						Properties p= null;
						String propName=(String)jSettingsListBox.getSelectedItem();
						if(propName!=null)
							p=findPropsByName(propName);
						if(p==null)
							JOptionPane.showMessageDialog(me,"Select a setting first!");
						else
						{
							if(p.containsKey("name"))
								jSettingsTextField.setText(p.getProperty("name"));
							else
								jSettingsTextField.setText("default");
							jTreeS.loadSettings(me, jTreeTextS, p);
							jTreeD1.loadSettings(me, jTreeTextD1, p);
							jTreeD2.loadSettings(me, jTreeTextD2, p);
						}
					}
				});
				JButton jAddButton = new JButton("Add"); 
				jAddButton.addActionListener(new ActionListener(){
					@Override public void actionPerformed(ActionEvent e) {
						Properties p=findPropsByName(jSettingsTextField.getText());
						if((p!=null)
						&&(JOptionPane.showConfirmDialog(me, "Update setting '"+p.getProperty("name")+"'???") == JOptionPane.OK_OPTION))
							return;
						if(p!=null)
							allSettings.remove(p);
						p = new Properties();
						p.setProperty("name", jSettingsTextField.getText());
						jTreeS.saveSettings(p);
						jTreeD1.saveSettings(p);
						jTreeD2.saveSettings(p);
						allSettings.add(p);
						for(ListDataListener l : listener)
							l.contentsChanged(new ListDataEvent(jSettingsListBox, ListDataEvent.INTERVAL_ADDED, allSettings.size()-1,allSettings.size()));
					}
				});
				JButton jDelButton = new JButton("Delete");
				jDelButton.addActionListener(new ActionListener(){
					@Override public void actionPerformed(ActionEvent e) {
						Properties p= null;
						String propName=(String)jSettingsListBox.getSelectedItem();
						if(propName!=null)
							p=findPropsByName(propName);
						if(p==null)
							JOptionPane.showMessageDialog(me,"Select a setting first!");
						else
						if(JOptionPane.showConfirmDialog(me, "Delete setting '"+p.getProperty("name")+"'???") == JOptionPane.OK_OPTION)
						{
							int x=allSettings.indexOf(p);
							allSettings.remove(p);
							for(ListDataListener l : listener)
								l.contentsChanged(new ListDataEvent(jSettingsListBox, ListDataEvent.INTERVAL_ADDED,x,x+1));
						}
					}
				});
				
				jSettingsPanel.add(jSettingsTextField);
				jSettingsPanel.add(jSettingsListBox);
				jSettingsPanel.add(jLoadButton);
				jSettingsPanel.add(jAddButton);
				jSettingsPanel.add(jDelButton);

				RemoteNode treeModel;
				treeModel = new RemoteNode(null,null);
				jTreeD1 = new DestTree("jTreeModelD1", this, treeModel, 1);
				treeModel.setTree(jTreeD1);
				jTreeD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				jTreeD1.addMouseListener(this);
				jTreeTextD1 = new JLabel("Remote #1: <Click Here>");
				jTreeTextD1.setOpaque(true);
				jTreeTextD1.setBackground(new Color(140, 255, 140));
				jTreeTextD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 11));
				jTreeTextD1.addMouseListener(this);
				JPanel jTreePanelD1 = new JPanel();
				jTreePanelD1.setLayout(new BorderLayout());
				jTreePanelD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				jTreePanelD1.add(jTreeTextD1, BorderLayout.NORTH);
				JScrollPane jScrollTreeD1 = new JScrollPane(jTreeD1);
				jScrollTreeD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				jTreePanelD1.add(jScrollTreeD1, BorderLayout.CENTER);

				treeModel = new RemoteNode(null,null);
				jTreeD2 = new DestTree("jTreeModelD2", this, treeModel, 2);
				treeModel.setTree(jTreeD2);
				jTreeD2.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				jTreeD2.addMouseListener(this);
				jTreeTextD2 = new JLabel("Remote #2: <Click Here>");
				jTreeTextD2.setOpaque(true);
				jTreeTextD2.setBackground(new Color(140, 140, 255));
				jTreeTextD2.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 11));
				jTreeTextD2.addMouseListener(this);
				jTreeD2.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				JPanel jTreePanelD = new JPanel();
				jTreePanelD.setLayout(new BorderLayout());
				jTreePanelD.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				jTreePanelD.add(jTreeTextD2, BorderLayout.NORTH);
				JScrollPane jScrollTreeD = new JScrollPane(jTreeD2);
				jScrollTreeD.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
				jTreePanelD.add(jScrollTreeD, BorderLayout.CENTER);

				jSplitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jTreePanelD1, jTreePanelD);
				jSplitPane2.setDividerLocation(DEFAULT_COL_WIDTH);
				jSplitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jTreePanelS, jSettingsPanel);
				jSplitPane1.setDividerLocation(500);
				jSplitPane0 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jSplitPane1, jSplitPane2);
				jSplitPane0.setDividerLocation(DEFAULT_COL_WIDTH);
				this.getContentPane().add(jSplitPane0);
			}
			setSize(DEFAULT_COL_WIDTH * 3, 500);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public DestTree getOtherRemoteTree(DestTree thanThis)
	{
		return (thanThis == jTreeD1) ? jTreeD2 : jTreeD1;
	}

	public SourceTree getSourceTree()
	{
		return jTreeS;
	}

	public void mouseEntered(MouseEvent arg0)
	{
	}

	public void mouseExited(MouseEvent arg0)
	{
	}

	public void mousePressed(MouseEvent e)
	{
		if ((e.isPopupTrigger()) && (e.getComponent() instanceof JTree)
				&& (((JTree) e.getComponent()).getPathForLocation(e.getPoint().x, e.getPoint().y) != null))
		{
			if (e.getComponent() == jTreeS)
			{
				jTreeS.setSelectionPath(jTreeS.getPathForLocation(e.getPoint().x, e.getPoint().y));
				jTreeS.getContextMenu().show(e.getComponent(), e.getX(), e.getY());
			}
			else if (e.getComponent() instanceof DragDropTree)
			{
				DragDropTree tree = (DragDropTree)e.getComponent();
				TreePath path = tree.getPathForLocation(e.getPoint().x, e.getPoint().y);
				((DragDropTree) e.getComponent()).setSelectionPath(path);
				PopupMenu menu = ((DragDropTree) e.getComponent()).getContextMenu();
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		mousePressed(e);
	}

	public static JDialog showWorkingDialog(Frame f)
	{
		JDialog dialog = new JDialog(f, "Working ...");
		dialog.setLocationRelativeTo(f); // center on screen
		dialog.setVisible(true);
		dialog.toFront(); // raise above other java windows
		return dialog;
	}

	public void mouseClicked(MouseEvent arg0)
	{
		if (arg0.getComponent() == jTreeTextS)
			jTreeS.affectSettings(this, jTreeTextS);
		else if (arg0.getComponent() == jTreeTextD1)
			jTreeD1.affectSettings(this, jTreeTextD1);
		else if (arg0.getComponent() == jTreeTextD2)
			jTreeD2.affectSettings(this, jTreeTextD2);
	}

	public void loadSettings() throws Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream(
				new File(System.getProperty("user.home") + File.separatorChar + ".settings" + File.separatorChar + "zimmerscp.ini")));
		if(p.containsKey("name"))
			jSettingsTextField.setText(p.getProperty("name"));
		else
			jSettingsTextField.setText("default");
		
		jTreeS.loadSettings(this, jTreeTextS, p);
		jTreeD1.loadSettings(this, jTreeTextD1, p);
		jTreeD2.loadSettings(this, jTreeTextD2, p);
		for(int i=2;;i++)
		{
			String is=Integer.toString(i);
			Properties newP=null;
			for(Object key : p.keySet())
			{
				if(key.toString().startsWith(is)&&(!Character.isDigit(key.toString().charAt(is.length()))))
				{
					if(newP==null)
					{
						newP=new Properties();
						allSettings.add(newP);
					}
					newP.put(key.toString().substring(is.length()), p.get(key));
				}
			}
			if(newP==null)
				break;
		}
	}

	public void windowActivated(WindowEvent arg0)
	{
	}

	public void windowClosing(WindowEvent arg0)
	{
		File F = new File(System.getProperty("user.home") + File.separatorChar + ".settings");
		try
		{
			F.mkdir();
		}
		catch (Exception e)
		{
		}
		F = new File(System.getProperty("user.home") + File.separatorChar + ".settings" + File.separatorChar + "zimmerscp.ini");
		try
		{
			FileOutputStream file = new FileOutputStream(F);
			Properties p = new Properties();
			p.setProperty("name", jSettingsTextField.getText());
			jTreeS.saveSettings(p);
			jTreeD1.saveSettings(p);
			jTreeD2.saveSettings(p);
			int n=2;
			for(Properties P : allSettings)
			{
				for(Object key : P.keySet())
				{
					p.put(""+n+key.toString(), P.get(key));
				}
				n++;
			}
			p.store(file, "properties for for zimmerscp");
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unable to save settings: " + e.getMessage());
		}
		System.exit(0);
	}

	public void windowDeactivated(WindowEvent arg0)
	{
	}

	public void windowDeiconified(WindowEvent arg0)
	{
	}

	public void windowIconified(WindowEvent arg0)
	{
	}

	public void windowOpened(WindowEvent arg0)
	{
	}

	public void windowClosed(WindowEvent arg0)
	{
	}
}
