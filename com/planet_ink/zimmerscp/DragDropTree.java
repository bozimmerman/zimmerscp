package com.planet_ink.zimmerscp;

import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.*;
import javax.swing.tree.*;

public abstract class DragDropTree extends JTree implements ActionListener
{
	private static final long serialVersionUID = 4327399837099558314L;
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

	protected abstract boolean HandleNodeImport(TransferHandler.TransferSupport t);
	
	protected abstract boolean TestNodeImport(TransferHandler.TransferSupport t);
	
	public void actionPerformed(ActionEvent e)
	{
	}
	
	public DragDropTree(String name, Frame f, DefaultMutableTreeNode n)
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
		this.setTransferHandler(new TransferHandler()
		{
			private static final long serialVersionUID = 3136844598684414486L;

			public int getSourceActions(JComponent c)
			{
				return COPY;
			}

			public Transferable createTransferable(JComponent c)
			{
				if (c instanceof DragDropTree)
					return (Transferable) ((DragDropTree) c).getSelectedNode();
				if (c instanceof Transferable)
					return (Transferable) c;
				return null;
			}

			public void exportDone(JComponent c, Transferable t, int action)
			{
			}

			public boolean canImport(TransferHandler.TransferSupport t)
			{
				if (t.getComponent() instanceof DragDropTree)
					return ((DragDropTree)t.getComponent()).TestNodeImport(t);
				return false;
			}

			public boolean importData(TransferHandler.TransferSupport t)
			{
				if (t.getComponent() instanceof DragDropTree)
					return ((DragDropTree)t.getComponent()).HandleNodeImport(t);
				return false;
			}
		});
	}
}
