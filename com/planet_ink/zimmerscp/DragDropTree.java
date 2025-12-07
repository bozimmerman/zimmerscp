package com.planet_ink.zimmerscp;

import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.tree.*;

public abstract class DragDropTree extends JTree implements ActionListener, DropTargetListener
{
	private static final long serialVersionUID = 4327399837099558314L;
	private final DragDropTree me = this;
	//public final DropTarget dt;

	public TreeNode getSelectedNode()
	{
		if (this.getSelectionPath() == null)
			return null;
		return (TreeNode) this.getSelectionPath().getLastPathComponent();
	}

	protected abstract PopupMenu getContextMenu();

	public abstract void affectSettings(Frame f, JLabel label);

	public abstract void loadSettings(Frame f, JLabel label, Properties p);

	public abstract void saveSettings(Properties p);

	protected abstract boolean handleNodeImport(TransferHandler.TransferSupport t);

	protected abstract boolean testNodeImport(TransferHandler.TransferSupport t);

	protected abstract boolean handleNodeImport(final Object sn, final Object tn);

	protected abstract boolean testNodeImport(final Object sn, final Object tn);

	public void actionPerformed(final ActionEvent e)
	{
	}

	public String getExpansionState()
	{
		final StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < getRowCount(); i++ )
		{
			if ( isExpanded(i) )
				sb.append(i).append(",");
		}
		return sb.toString();
	}

	public void setExpansionState(final String s)
	{
		final String[] indexes = s.split(",");
		for ( final String st : indexes )
		{
			try
			{
				final int row = Integer.parseInt(st);
				expandRow(row);
			}
			catch(Exception e)
			{}
		}
	}

	public DragDropTree(final String name, final Frame f, final DefaultMutableTreeNode n)
	{
		super(n);
		this.setEditable(false);
		this.setName(name);
		this.setDragEnabled(true);
		this.setDropMode(DropMode.ON);
		add(getContextMenu());
		this.setShowsRootHandles(false);// to show the root icon
		this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		//dt = new DropTarget(this,this);
		this.setEditable(false);
		this.setTransferHandler(new TransferHandler()
		{
			private static final long serialVersionUID = 3136844598684414486L;

			@Override
			public int getSourceActions(final JComponent c)
			{
				return COPY;
			}

			@Override
			public Transferable createTransferable(final JComponent c)
			{
				if (c instanceof DragDropTree)
				{
					if(me.getSelectionModel().getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION)
						return (Transferable) ((DragDropTree) c).getSelectedNode();
					else
					return new Transferable()
					{
						@Override
						public DataFlavor[] getTransferDataFlavors()
						{
							return new DataFlavor[] { DataFlavor.javaFileListFlavor };
						}

						@Override
						public boolean isDataFlavorSupported(final DataFlavor flavor)
						{
							return flavor.equals(DataFlavor.javaFileListFlavor);
						}

						@Override
						public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException
						{
							final List<TreeNode> nodes = new ArrayList<TreeNode>();
							for(final TreePath path : me.getSelectionPaths())
							{
								if(path.getLastPathComponent() instanceof TreeNode)
									nodes.add((TreeNode)path.getLastPathComponent());
							}
							return nodes;
						}

					};
				}
				else
				if (c instanceof Transferable)
					return (Transferable) c;
				return null;
			}

			@Override
			public void exportDone(final JComponent c, final Transferable t, final int action)
			{
			}

			@Override
			public boolean canImport(final TransferHandler.TransferSupport t)
			{
				if (t.getComponent() instanceof DragDropTree)
				{
					final boolean success = ((DragDropTree)t.getComponent()).testNodeImport(t);
					return success;
				}

				return false;
			}

			@Override
			public boolean importData(final TransferHandler.TransferSupport t)
			{
				if (t.getComponent() instanceof DragDropTree)
				{
					final boolean success = ((DragDropTree)t.getComponent()).handleNodeImport(t);
					return success;
				}
				return false;
			}
		});
	}
}
