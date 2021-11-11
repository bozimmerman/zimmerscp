package com.planet_ink.zimmerscp;

import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.*;
import javax.swing.tree.*;

public abstract class DragDropTree extends JTree implements ActionListener, DropTargetListener
{
	private static final long serialVersionUID = 4327399837099558314L;
	public final DropTarget dt;

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

	public void actionPerformed(final ActionEvent e)
	{
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
		this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); // set
		// single
		// selection
		// for
		// the
		// Tree
		this.setEditable(false);
		dt = new DropTarget(this,this);
		this.setTransferHandler(new TransferHandler()
		{
			private static final long serialVersionUID = 3136844598684414486L;

			public int getSourceActions(final JComponent c)
			{
				return COPY;
			}

			public Transferable createTransferable(final JComponent c)
			{
				if (c instanceof DragDropTree)
					return (Transferable) ((DragDropTree) c).getSelectedNode();
				if (c instanceof Transferable)
					return (Transferable) c;
				return null;
			}

			public void exportDone(final JComponent c, final Transferable t, final int action)
			{
			}

			public boolean canImport(final TransferHandler.TransferSupport t)
			{
				if (t.getComponent() instanceof DragDropTree)
					return ((DragDropTree)t.getComponent()).testNodeImport(t);
				return false;
			}

			public boolean importData(final TransferHandler.TransferSupport t)
			{
				if (t.getComponent() instanceof DragDropTree)
					return ((DragDropTree)t.getComponent()).handleNodeImport(t);
				return false;
			}
		});
	}
}
