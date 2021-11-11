package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.TreePath;

public class zimmerscp extends javax.swing.JFrame implements MouseListener, WindowListener
{
	private static final long serialVersionUID = -2866392485562765792L;

	private static final int	DEFAULT_COL_WIDTH	= 250;
	private static final double	VERSION				= 1.9;
	private SourceTree			jTreeS;
	private JLabel				jTreeTextS;
	private JButton				jTreeBSettingsS;
	private JButton				jTreeBRefreshS;
	private JButton				addPaneButton;
	private JPanel				addPanePanel		= null;
	private JTextField			jSettingsTextField	= null;
	private JComboBox<String>	jSettingsListBox	= null;

	private final List<Properties>						allSettings			= new Vector<Properties>();
	private final Map<JButton, Entry<DestTree, JLabel>>	destSettingsButtons	= new Hashtable<JButton, Entry<DestTree, JLabel>>();
	private final Map<JButton, Entry<DestTree, JLabel>>	destRefreshButtons	= new Hashtable<JButton, Entry<DestTree, JLabel>>();
	private final Map<JButton, Entry<DestTree, JPanel>>	destRemoveButtons	= new Hashtable<JButton, Entry<DestTree, JPanel>>();
	private final Map<DestTree, JLabel>					destTrees			= new Hashtable<DestTree, JLabel>();

	public static zimmerscp INSTANCE;

	final Color[] cycle = new Color[] {
		new Color(255, 255, 140),
		new Color(140, 255, 140),
		new Color(140, 140, 255),
		new Color(255, 140, 140),
		new Color(255, 140, 255),
		new Color(140, 255, 255),
	};

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(final String[] args)
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

			public Runnable initMe(final zimmerscp form)
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
				catch (final Exception e)
				{
				}
			}
		}.initMe(this));
	}

	private Properties findPropsByName(final String name)
	{
		for(int i=0;i<allSettings.size();i++)
		{
			final Properties p=allSettings.get(i);
			final String n=p.getProperty("name");
			if(n.trim().equalsIgnoreCase(name.trim()))
				return p;
		}
		return null;
	}

	public JPanel addNextDestTree()
	{
		final int num = this.destTrees.size()+1;
		return addDestTree(num, cycle[num % cycle.length]);
	}

	private JPanel addDestTree(final int num, final Color C)
	{
		if(this.addPanePanel != null)
		{
			this.addPanePanel.remove(addPaneButton);
			this.addPanePanel = null;
		}
		ImageIcon icon;
		icon = new ImageIcon(this.getClass().getResource("settingsicon.png"));
		final JButton jTreeBSettings = new JButton(icon);
		jTreeBSettings.setBorderPainted(false);
		jTreeBSettings.setMaximumSize(new Dimension(18,16));
		jTreeBSettings.setPreferredSize(new Dimension(18,16));
		jTreeBSettings.addMouseListener(this);
		jTreeBSettings.setBackground(C);
		icon = new ImageIcon(this.getClass().getResource("refreshicon.png"));
		final JButton jTreeBRefresh = new JButton(icon);
		jTreeBRefresh.setBorderPainted(false);
		jTreeBRefresh.setMaximumSize(new Dimension(18,16));
		jTreeBRefresh.setPreferredSize(new Dimension(18,16));
		jTreeBRefresh.addMouseListener(this);
		jTreeBRefresh.setBackground(C);
		icon = new ImageIcon(this.getClass().getResource("removeicon.png"));
		final JButton jTreeBRemove = new JButton(icon);
		jTreeBRemove.setBorderPainted(false);
		jTreeBRemove.setMaximumSize(new Dimension(18,16));
		jTreeBRemove.setPreferredSize(new Dimension(18,16));
		jTreeBRemove.addMouseListener(this);
		jTreeBRemove.setBackground(C);
		addPaneButton.setBackground(C);

		RemoteNode treeModel;
		treeModel = new RemoteNode(null,null);
		final DestTree jTreeD1 = new DestTree("jTreeModelD"+num, this, treeModel, num);
		treeModel.setTree(jTreeD1);
		jTreeD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
		jTreeD1.addMouseListener(this);
		final JLabel jTreeTextD1 = new JLabel("<Click Settings Button>");
		final Color C2 = C.brighter();
		jTreeTextD1.setOpaque(true);
		jTreeTextD1.setBackground(C2);
		jTreeTextD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 11));
		jTreeTextD1.addMouseListener(this);
		final JPanel jTreePanelD1 = new JPanel();
		jTreePanelD1.setLayout(new BorderLayout());
		jTreePanelD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
		final JPanel northPanel = new JPanel();
		northPanel.setLayout(new java.awt.BorderLayout());
		jTreePanelD1.add(northPanel, BorderLayout.NORTH);
		northPanel.add(jTreeTextD1, BorderLayout.CENTER);
		final JPanel northnorthPanel = new JPanel();
		northnorthPanel.setLayout(new java.awt.BorderLayout());
		final JLabel jTreeTextType = new JLabel("Remote Filesystem#"+num);
		jTreeD1.setTopLabel(jTreeTextType);
		jTreeTextType.setOpaque(true);
		jTreeTextType.setBackground(C);
		northnorthPanel.add(jTreeTextType, BorderLayout.CENTER);
		northPanel.add(northnorthPanel, BorderLayout.NORTH);
		final JPanel northeastPanel = new JPanel();
		northeastPanel.setLayout(new java.awt.GridLayout(1,4));
		northeastPanel.add(jTreeBRefresh);
		northeastPanel.add(jTreeBSettings);
		northeastPanel.add(addPaneButton);
		northeastPanel.add(jTreeBRemove);
		northnorthPanel.add(northeastPanel, BorderLayout.EAST);
		this.addPanePanel = northeastPanel;

		final JScrollPane jScrollTreeD1 = new JScrollPane(jTreeD1);
		jScrollTreeD1.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 500));
		jTreePanelD1.add(jScrollTreeD1, BorderLayout.CENTER);
		this.destTrees.put(jTreeD1, jTreeTextD1);
		final Entry<DestTree, JLabel> buttonEntry = new Entry<DestTree, JLabel>()
		{
			final DestTree tree = jTreeD1;
			final JLabel label = jTreeTextD1;
			@Override
			public DestTree getKey()
			{
				return tree;
			}

			@Override
			public JLabel getValue()
			{
				return label;
			}
			@Override
			public JLabel setValue(final JLabel value)
			{
				return label;
			}

		};
		this.destRefreshButtons.put(jTreeBRefresh, buttonEntry);
		this.destSettingsButtons.put(jTreeBSettings, buttonEntry);
		this.destRemoveButtons.put(jTreeBRemove, new Entry<DestTree, JPanel>()
		{
			final DestTree tree = jTreeD1;
			final JPanel panel = jTreePanelD1;
			@Override
			public DestTree getKey()
			{
				return tree;
			}

			@Override
			public JPanel getValue()
			{
				return panel;
			}
			@Override
			public JPanel setValue(final JPanel value)
			{
				return panel;
			}
		});

		JSplitPane node = (JSplitPane)getContentPane().getComponent(0);
		while(node.getRightComponent() instanceof JSplitPane)
			node = (JSplitPane)node.getRightComponent();
		if(node.getRightComponent() == null)
			node.setRightComponent(jTreePanelD1);
		else
		{
			final JSplitPane jSplitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, node.getRightComponent(), jTreePanelD1);
			jSplitPane2.setDividerLocation(-1);
			node.setRightComponent(jSplitPane2);
		}
		//this.pack();
		setSize((DEFAULT_COL_WIDTH*(1+destTrees.keySet().size())), 500);
		return jTreePanelD1;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initGUI()
	{
		try
		{
			final JFrame me = this;
			this.setTitle("ZimmerSCP v"+VERSION);
			this.addWindowListener(this);
			{
				final FileNode jTreeModelS = new FileNode(null);
				jTreeS = new SourceTree("jTreeModelS", this, jTreeModelS);
				jTreeModelS.setTree(jTreeS);
				jTreeS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 350));
				jTreeS.addMouseListener(this);
				jTreeTextS = new JLabel("<Click Settings Button>");
				jTreeTextS.setOpaque(true);
				jTreeTextS.setBackground(new Color(255, 255, 200));
				jTreeTextS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 11));

				ImageIcon icon;
				icon = new ImageIcon(this.getClass().getResource("settingsicon.png"));
				jTreeBSettingsS = new JButton(icon);
				jTreeBSettingsS.setBorderPainted(false);
				jTreeBSettingsS.setMaximumSize(new Dimension(18,16));
				jTreeBSettingsS.setPreferredSize(new Dimension(18,16));
				jTreeBSettingsS.addMouseListener(this);
				jTreeBSettingsS.setBackground(new Color(255, 255, 140));
				icon = new ImageIcon(this.getClass().getResource("refreshicon.png"));
				jTreeBRefreshS = new JButton(icon);
				jTreeBRefreshS.setBorderPainted(false);
				jTreeBRefreshS.setMaximumSize(new Dimension(18,16));
				jTreeBRefreshS.setPreferredSize(new Dimension(18,16));
				jTreeBRefreshS.addMouseListener(this);
				jTreeBRefreshS.setBackground(new Color(255, 255, 140));
				final JPanel jTreePanelS = new JPanel();
				jTreePanelS.setLayout(new BorderLayout());
				jTreePanelS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 350));
				final JPanel northPanel = new JPanel();
				northPanel.setLayout(new java.awt.BorderLayout());
				jTreePanelS.add(northPanel, BorderLayout.NORTH);
				northPanel.add(jTreeTextS, BorderLayout.CENTER);
				final JPanel northnorthPanel = new JPanel();
				northnorthPanel.setLayout(new java.awt.BorderLayout());
				final JLabel jTreeTextType = new JLabel("Local Window");
				jTreeTextType.setOpaque(true);
				jTreeTextType.setBackground(new Color(255, 255, 140));
				northnorthPanel.add(jTreeTextType, BorderLayout.CENTER);
				northPanel.add(northnorthPanel, BorderLayout.NORTH);
				final JPanel northeastPanel = new JPanel();
				northeastPanel.setLayout(new java.awt.GridLayout());
				northeastPanel.add(jTreeBRefreshS,BorderLayout.WEST);
				northeastPanel.add(jTreeBSettingsS,BorderLayout.EAST);
				northnorthPanel.add(northeastPanel, BorderLayout.EAST);
				final JScrollPane jScrollTreeS = new JScrollPane(jTreeS);
				jScrollTreeS.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 350));
				jTreePanelS.add(jScrollTreeS, BorderLayout.CENTER);

				final JPanel jSettingsPanel = new JPanel();
				final GridLayout settingsGrid = new GridLayout(3,1);
				jSettingsPanel.setLayout(settingsGrid);
				jSettingsPanel.setMinimumSize(new java.awt.Dimension(DEFAULT_COL_WIDTH, 150));
				jSettingsListBox = new JComboBox();
				jSettingsListBox.setPreferredSize(new Dimension(DEFAULT_COL_WIDTH-100, 20));
				final List<ListDataListener> listener=new LinkedList<ListDataListener>();
				jSettingsListBox.setModel(new DefaultComboBoxModel(){
					private static final long serialVersionUID = 133324323564L;
					@Override public int getSize() { return allSettings.size(); }
					@Override public Object getElementAt(final int index) {
						if((index<0)||(index>=allSettings.size())) return null;
						final Properties P=allSettings.get(index);
						return P.containsKey("name")?P.get("name"):null;
					}
					@Override public void addListDataListener(final ListDataListener l) { listener.add(l); }
					@Override public void removeListDataListener(final ListDataListener l) { listener.remove(l); }
				});
				jSettingsTextField = new JTextField();
				jSettingsTextField.setColumns(13);
				jSettingsTextField.setEditable(true);
				final ActionListener loadAction = new ActionListener()
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						Properties p= null;
						final String propName=(String)jSettingsListBox.getSelectedItem();
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
							clearAllPanels();
							int numPanels = 2;
							if(p.containsKey("panels"))
								numPanels = Integer.parseInt(p.getProperty("panels"));
							for(int i=0;i<numPanels;i++)
								addNextDestTree();
							for(final DestTree T : destTrees.keySet())
								T.loadSettings(me, destTrees.get(T), p);
						}
					}
				};
				jSettingsListBox.addActionListener(loadAction);

				icon = new ImageIcon(this.getClass().getResource("saveicon.png"));
				final JButton jAddButton = new JButton(icon);
				jAddButton.setBorderPainted(false);
				jAddButton.setMaximumSize(new Dimension(18,16));
				jAddButton.setPreferredSize(new Dimension(18,16));
				jAddButton.addActionListener(new ActionListener(){
					@Override public void actionPerformed(final ActionEvent e) {
						Properties p=findPropsByName(jSettingsTextField.getText());
						if((p!=null)
						&&(JOptionPane.showConfirmDialog(me, "Update setting '"+p.getProperty("name")+"'???") != JOptionPane.YES_OPTION))
							return;
						if(p!=null)
							allSettings.remove(p);
						p = new Properties();
						p.setProperty("name", jSettingsTextField.getText());
						p.setProperty("panels", ""+destTrees.size());
						jTreeS.saveSettings(p);
						for(final DestTree T : destTrees.keySet())
							T.saveSettings(p);
						allSettings.add(p);
						for(final ListDataListener l : listener)
							l.contentsChanged(new ListDataEvent(jSettingsListBox, ListDataEvent.INTERVAL_ADDED, allSettings.size()-1,allSettings.size()));
					}
				});
				icon = new ImageIcon(this.getClass().getResource("deleteicon.png"));
				final JButton jDelButton = new JButton(icon);
				jDelButton.setBorderPainted(false);
				jDelButton.setMaximumSize(new Dimension(18,16));
				jDelButton.setPreferredSize(new Dimension(18,16));
				jDelButton.addActionListener(new ActionListener(){
					@Override public void actionPerformed(final ActionEvent e) {
						Properties p= null;
						final String propName=(String)jSettingsListBox.getSelectedItem();
						if(propName!=null)
							p=findPropsByName(propName);
						if(p==null)
							JOptionPane.showMessageDialog(me,"Select a setting first!");
						else
						if(JOptionPane.showConfirmDialog(me, "Delete setting '"+p.getProperty("name")+"'???") == JOptionPane.YES_OPTION)
						{
							final int x=allSettings.indexOf(p);
							allSettings.remove(p);
							for(final ListDataListener l : listener)
								l.contentsChanged(new ListDataEvent(jSettingsListBox, ListDataEvent.INTERVAL_ADDED,x,x+1));
						}
					}
				});

				final JLabel settingsLabel = new JLabel(" --- Configurations --- ", SwingConstants.CENTER);
				jSettingsPanel.add(settingsLabel);
				final JPanel rowOne = new JPanel();
				rowOne.setLayout(new BorderLayout());
				rowOne.add(jSettingsListBox, BorderLayout.CENTER);
				rowOne.add(jDelButton, BorderLayout.EAST);
				jSettingsPanel.add(rowOne);
				final JPanel rowTwo = new JPanel();
				rowTwo.setLayout(new BorderLayout());
				rowTwo.add(jSettingsTextField, BorderLayout.CENTER);
				rowTwo.add(jAddButton, BorderLayout.EAST);
				jSettingsPanel.add(rowTwo);


				icon = new ImageIcon(this.getClass().getResource("addicon.png"));
				addPaneButton = new JButton(icon);
				addPaneButton.setBorderPainted(false);
				addPaneButton.setMaximumSize(new Dimension(18,16));
				addPaneButton.setPreferredSize(new Dimension(18,16));
				addPaneButton.addMouseListener(this);

				final JSplitPane jSplitSourcePane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jTreePanelS, jSettingsPanel);
				jSplitSourcePane1.setDividerLocation(350);
				final JSplitPane jSplitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jSplitSourcePane1, null);
				jSplitSourcePane1.setMinimumSize(new Dimension(DEFAULT_COL_WIDTH,350));
				jSplitSourcePane1.setPreferredSize(new Dimension(DEFAULT_COL_WIDTH,500));
				jSplitPane1.setDividerLocation(-1);
				getContentPane().add(jSplitPane1);

				addDestTree(1, new Color(140, 255, 140));

				pack();
			}
			setSize((DEFAULT_COL_WIDTH*(destTrees.size()+1)), 500);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	public DestTree[] getOtherRemoteTree(final DestTree thanThis)
	{
		final List<DestTree> l = new ArrayList<DestTree>(1);
		for(final DestTree T : destTrees.keySet())
			if(T != thanThis)
				l.add(T);
		return l.toArray(new DestTree[l.size()]);
	}

	public SourceTree getSourceTree()
	{
		return jTreeS;
	}

	public void mouseEntered(final MouseEvent arg0)
	{
	}

	public void mouseExited(final MouseEvent arg0)
	{
	}

	public void mousePressed(final MouseEvent e)
	{
		if ((e.isPopupTrigger()) && (e.getComponent() instanceof JTree)
				&& (((JTree) e.getComponent()).getPathForLocation(e.getPoint().x, e.getPoint().y) != null))
		{
			if (e.getComponent() == jTreeS)
			{
				final TreePath path = jTreeS.getPathForLocation(e.getPoint().x, e.getPoint().y);
				if(jTreeS.getSelectionPaths()==null || jTreeS.getSelectionPaths().length==0)
					jTreeS.setSelectionPath(path);
				jTreeS.getContextMenu().show(e.getComponent(), e.getX(), e.getY());
			}
			else if (e.getComponent() instanceof DragDropTree)
			{
				final DragDropTree tree = (DragDropTree)e.getComponent();
				final TreePath path = tree.getPathForLocation(e.getPoint().x, e.getPoint().y);
				if(tree.getSelectionPaths()==null || tree.getSelectionPaths().length==0)
					((DragDropTree) e.getComponent()).setSelectionPath(path);
				final PopupMenu menu = ((DragDropTree) e.getComponent()).getContextMenu();
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	public void mouseReleased(final MouseEvent e)
	{
		mousePressed(e);
	}

	public void clearAllPanels()
	{
		((JSplitPane)this.getContentPane().getComponent(0)).setRightComponent(null);
		this.destRefreshButtons.clear();
		this.destRemoveButtons.clear();
		this.destSettingsButtons.clear();
		this.destTrees.clear();
		this.addPanePanel = null;
	}

	public static JDialog showWorkingDialog(final Frame f)
	{
		final JDialog dialog = new JDialog(f, "Working ...");
		dialog.setSize(new Dimension(150,10));
		dialog.setLocationRelativeTo(f); // center on screen
		dialog.setVisible(true);
		dialog.toFront(); // raise above other java windows
		return dialog;
	}

	private static void removeFromButtonCache(final Map<JButton, Entry<DestTree,JLabel>> map, final DestTree T)
	{
		for(final Iterator<JButton> i = map.keySet().iterator();i.hasNext();)
		{
			final JButton key = i.next();
			final Entry<DestTree, JLabel> ei = map.get(key);
			if(ei.getKey() == T)
				i.remove();
		}
	}

	private void removeRemotePanel(final DestTree T, final JPanel panel)
	{
		removeFromButtonCache(destRefreshButtons, T);
		removeFromButtonCache(destSettingsButtons, T);
		destTrees.remove(T);

		final LinkedList<JPanel> remainingPanels = new LinkedList<JPanel>();
		final JSplitPane rootNode = (JSplitPane)getContentPane().getComponent(0);
		JSplitPane node = rootNode;
		while(node !=null)
		{
			if((node.getLeftComponent() != panel)
			&& (node.getLeftComponent() instanceof JPanel)
			&& (node != rootNode))
				remainingPanels.add((JPanel)node.getLeftComponent());
			if(node.getRightComponent() instanceof JSplitPane)
				node = (JSplitPane)node.getRightComponent();
			else
			{
				if((node.getRightComponent() instanceof JPanel)
				&&(node.getRightComponent() != panel))
					remainingPanels.add((JPanel)node.getRightComponent());
				node = null;
			}
		}
		rootNode.setRightComponent(null);
		for(final DestTree remT : destTrees.keySet())
		{
			if((remT != T)
			&&(remT.getNum() > T.getNum()))
			{
				remT.setNum(remT.getNum()-1);
			}
		}
		setSize((DEFAULT_COL_WIDTH*(1+destTrees.size())), 500);
		node = rootNode;
		while(remainingPanels.size() > 0)
		{
			final JSplitPane jSplitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, remainingPanels.removeFirst(), null);
			jSplitPane2.setDividerLocation(-1);
			node.setRightComponent(jSplitPane2);
			node = jSplitPane2;
		}
		this.pack();
		setSize((DEFAULT_COL_WIDTH*(1+destTrees.size())), 500);
	}

	public void mouseClicked(final MouseEvent arg0)
	{
		if (arg0.getComponent() == jTreeBSettingsS)
			jTreeS.affectSettings(this, jTreeTextS);
		else if (arg0.getComponent() == jTreeBRefreshS)
			jTreeS.refresh(this, jTreeTextS);
		else if (this.destSettingsButtons.containsKey(arg0.getComponent()))
		{
			final Entry<DestTree, JLabel> e = this.destSettingsButtons.get(arg0.getComponent());
			e.getKey().affectSettings(this, e.getValue());
		}
		else if (this.destRefreshButtons.containsKey(arg0.getComponent()))
		{
			final Entry<DestTree, JLabel> e = this.destRefreshButtons.get(arg0.getComponent());
			e.getKey().refresh(this, e.getValue());
		}
		else if (this.destRemoveButtons.containsKey(arg0.getComponent()))
		{
			if(this.destTrees.size()<2)
				JOptionPane.showMessageDialog(this, "You may not remove the last remote host.");
			else
			if(JOptionPane.showConfirmDialog(this, "Remove this remote host?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
			{
				final Entry<DestTree, JPanel> e = destRemoveButtons.get(arg0.getComponent());
				final DestTree T = e.getKey();
				destRemoveButtons.remove(arg0.getComponent());
				removeRemotePanel(T, e.getValue());
			}
		}
		else
		if(arg0.getComponent() == this.addPaneButton)
		{
			this.addNextDestTree();
		}
	}

	public void loadSettings() throws Exception
	{
		final Properties p = new Properties();
		p.load(new FileInputStream(
				new File(System.getProperty("user.home") + File.separatorChar + ".settings" + File.separatorChar + "zimmerscp.ini")));

		final String selectedItem;
		if(p.containsKey("name"))
		{
			selectedItem = p.getProperty("name");
			jSettingsTextField.setText(selectedItem);
		}
		else
		{
			selectedItem = null;
			jSettingsTextField.setText("default");
		}

		clearAllPanels();
		int numPanels = 2;
		if(p.containsKey("panels"))
			numPanels = Integer.parseInt(p.getProperty("panels"));
		for(int i=0;i<numPanels;i++)
			addNextDestTree();
		jTreeS.loadSettings(this, jTreeTextS, p);
		for(final DestTree T : destTrees.keySet())
			T.loadSettings(this, destTrees.get(T), p);
		for(int i=2;;i++)
		{
			final String is=Integer.toString(i);
			Properties newP=null;
			for(final Object key : p.keySet())
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
		if(selectedItem != null)
		{
			jSettingsListBox.setEditable(true);
			jSettingsListBox.setSelectedItem(selectedItem);
			jSettingsListBox.setEditable(false);
		}
	}

	public void windowActivated(final WindowEvent arg0)
	{
	}

	public void windowClosing(final WindowEvent arg0)
	{
		File F = new File(System.getProperty("user.home") + File.separatorChar + ".settings");
		try
		{
			F.mkdir();
		}
		catch (final Exception e)
		{
		}
		F = new File(System.getProperty("user.home") + File.separatorChar + ".settings" + File.separatorChar + "zimmerscp.ini");
		try
		{
			final FileOutputStream file = new FileOutputStream(F);
			final Properties p = new Properties();
			p.setProperty("name", jSettingsTextField.getText());
			p.setProperty("panels", ""+destTrees.size());
			jTreeS.saveSettings(p);
			for(final DestTree T : destTrees.keySet())
				T.saveSettings(p);
			int n=2;
			for(final Properties P : allSettings)
			{
				for(final Object key : P.keySet())
				{
					p.put(""+n+key.toString(), P.get(key));
				}
				n++;
			}
			p.store(file, "properties for for zimmerscp");
		}
		catch (final Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unable to save settings: " + e.getMessage());
		}
		System.exit(0);
	}

	public void windowDeactivated(final WindowEvent arg0)
	{
	}

	public void windowDeiconified(final WindowEvent arg0)
	{
	}

	public void windowIconified(final WindowEvent arg0)
	{
	}

	public void windowOpened(final WindowEvent arg0)
	{
	}

	public void windowClosed(final WindowEvent arg0)
	{
	}
}
