package com.planet_ink.zimmerscp;

import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.tree.TreeNode;

public class SourceTree extends DragDropTree
{
	private static final long serialVersionUID = 5315786947498638856L;
	private LocalDialog dialog = null;
	private PopupMenu popupMenu = null;
	private Frame f = null;

	public SourceTree(String name, Frame f, FileNode m)
	{
		super(name, f, m);
		this.f=f;
		this.add(getContextMenu());
	}

	public PopupMenu getContextMenu()
	{
		TreeNode node = this.getSelectedNode();
		if (popupMenu == null)
		{
			popupMenu = new PopupMenu("Local Option");
			popupMenu.addActionListener(this);
		}
		popupMenu.removeAll();
		if (node instanceof FileNode)
		{
			FileNode fnode = (FileNode) node;
			popupMenu.add("Info");
			if(fnode.getFile() != null)
			{
				popupMenu.add("Delete");
				popupMenu.add("Rename");
				if (fnode.getFile().isDirectory())
				{
					popupMenu.add("MakeDir");
				}
				else
				{
					popupMenu.add("View");
					popupMenu.add("Edit");
				}
			}
		}
		return popupMenu;
	}

	public void affectSettings(Frame f, JLabel label)
	{
		if (dialog == null)
			dialog = new LocalDialog(f);
		dialog.setLocationRelativeTo(this); // center on screen
		dialog.setModal(true);
		dialog.toFront(); // raise above other java windows
		dialog.setVisible(true);
		if (!dialog.wasCancelled())
		{
			setLocal(f, dialog.getRootDir(), label, this, (FileNode) this.getModel().getRoot());
		}
	}

	public void loadSettings(Frame f, JLabel label, Properties p)
	{
		String local = p.getProperty("local.dir");
		String backups = p.getProperty("local.backups");
		String bindex = p.getProperty("local.backupindexes");
		String bcopies = p.getProperty("local.backupcopies");
		if ((local != null) && (local.length() > 0))
		{
			if (backups == null)
				backups = "";
			if (bindex == null)
				bindex = "false";
			if (bcopies == null)
				bcopies = "false";
			if (dialog == null)
				dialog = new LocalDialog(f);
			dialog.fill(local, backups, Boolean.valueOf(bindex), Boolean.valueOf(bcopies));
			setLocal(f, local, label, this, (FileNode) this.getModel().getRoot());
		}
	}

	public void saveSettings(Properties p)
	{
		FileNode n = (FileNode) this.getModel().getRoot();
		if (n.getFile() != null)
		{
			p.setProperty("local.dir", n.getFile().getAbsolutePath());
			p.setProperty("local.backups", dialog.getBackupDir());
			p.setProperty("local.backupindexes", String.valueOf(dialog.getBackupIndexes()));
			p.setProperty("local.backupcopies", String.valueOf(dialog.getBackupCopyOvers()));
		}
	}

	public boolean getBackupIndexes()
	{
		return dialog.getBackupIndexes();
	}

	public boolean getBackupCopyOvers()
	{
		return dialog.getBackupCopyOvers();
	}

	public String getBackupDir()
	{
		return dialog.getBackupDir();
	}

	private void setLocal(Frame f, String root, JLabel text, JTree tree, FileNode node)
	{
		text.setText("Local: <Click Here>");
		File F = new File(root);
		if ((F.exists()) && (F.isDirectory()))
		{
			text.setText("Local: " + F.getPath());
			tree.clearSelection();
			node.removeAllChildren();
			JDialog dlg = zimmerscp.showWorkingDialog(f);
			node.build(F);
			tree.updateUI();
			tree.repaint();
			dlg.dispose();
		}
		else
			JOptionPane.showMessageDialog(this, "The local directory is invalid.");
	}

	private boolean ViewLocal(FileNode node)
	{
		if(node.getFile().isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Directories can not be viewed like that.");
			return false;
		}
		new FileDialog(f,node.getFile().getAbsolutePath(),node.getFile(), false).setVisible(true);
		return true;
	}
	
	private boolean EditLocal(FileNode node)
	{
		if(node.getFile().isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Directories can not be viewed like that.");
			return false;
		}
		FileDialog dialog =new FileDialog(f,node.getFile().getAbsolutePath(),node.getFile(), true);
		dialog.setVisible(true);
		if(dialog.isDirty())
		{
			// we're done, really.. the dialog resaved it for us.
		}
		return true;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equalsIgnoreCase("Info"))
			new FileInfoDialog(f, (FileNode) getSelectedNode()).setVisible(true);
		else if (e.getActionCommand().equalsIgnoreCase("Delete"))
			DeleteLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("MakeDir"))
			MakeDirLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Rename"))
			RenameLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("View"))
			ViewLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Edit"))
			EditLocal((FileNode) getSelectedNode());
	}
	
	private final boolean MakeDirLocal(FileNode node)
	{
		String dirName = JOptionPane.showInputDialog(f,"Directory name");
		File F = new File(node.getFile(),dirName);
		if(!F.mkdir())
		{
			JOptionPane.showMessageDialog(f, "Unable to create directory.");
			return false;
		}
		node.add(new FileNode(this,F));
		updateUI();
		repaint();
		return true;
	}
	
	private final boolean RenameLocal(FileNode node)
	{
		String newName = JOptionPane.showInputDialog(f,"New name");
		File newFile = new File(node.getFile().getParentFile(),newName);
		if(!node.renameTo(newFile))
		{
			JOptionPane.showMessageDialog(f, "Unable to rename "+node.getFile().getAbsolutePath()+" to " + newFile.getAbsolutePath());
			return false;
		}
		updateUI();
		repaint();
		return true;
	}
	
	private final boolean DeleteLocal(FileNode node)
	{
		if(node == getModel().getRoot())
		{
			JOptionPane.showMessageDialog(f, "Root nodes can not be deleted.");
			return false;
		}
		if(JOptionPane.showConfirmDialog(f, "Delete file '"+node.getFile().getName()+"'","Delete Node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return false;
		FileNode parentNode= (FileNode)node.getParent();
		if(node.getFile().isDirectory())
		{
			if(JOptionPane.showConfirmDialog(f, "This will delete this directory and all children.  Confirm.","Delete Directory",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
		}
		try
		{
			
			DeleteRecurseFile(node.getFile());
			parentNode.removeAllChildren();
			parentNode.build(parentNode.getFile());
			updateUI();
			repaint();
			return true;
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, e.getMessage());
			return false;
		}
	}
	
	private void DeleteRecurseFile(File F) throws IOException
	{
		if(F==null) return;
		if(F.isDirectory())
		{
			File[] FS = F.listFiles();
			for(int f=0;f<FS.length;f++)
				DeleteRecurseFile(FS[f]);
		}
		if(!F.delete()) throw new IOException("Couldn't delete file "+F.getAbsolutePath());
	}
	
	@SuppressWarnings("unchecked")
	protected boolean TestNodeImport(TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof FileNode)
				{
					FileNode node = (FileNode) location.getPath().getLastPathComponent();
					if (node.getFile().isDirectory())
					{
						try
						{
							Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
							if (o instanceof RemoteNode)
								return true;
							if (o instanceof List)
								return ((List)o).size()>0;
						}
						catch (Exception e)
						{
						}
					}
				}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean HandleNodeImport(TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof FileNode)
				{
					FileNode node = (FileNode) location.getPath().getLastPathComponent();
					try
					{
						Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						List<RemoteNode> fl = new ArrayList<RemoteNode>();
						if (o instanceof RemoteNode)
							fl.add((RemoteNode) o);
						else if (o instanceof List)
							fl = (List) o;
						if (fl.size() > 0)
							return TransferRemoteLocal(fl.iterator().next().getTree(), fl, node);
					}
					catch (UnsupportedFlavorException e)
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					return false;
				}
		}
		return false;
	}
	
	private final boolean TransferRemoteLocal(DestTree rTree, List<RemoteNode> srcRs, FileNode dest)
	{
		for(RemoteNode srcF : srcRs)
			if(!TransferRemoteLocal(rTree, srcF,dest))
				return false;
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private final boolean TransferRemoteLocal(DestTree rTree, RemoteNode srcR, FileNode dest)
	{
		if(!dest.getFile().isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Destination, somehow, is not a directory.");
			return false;
		}
		if(srcR.isDirectory())
		{
			FileNode nxtDir = null;
			for(Enumeration e=dest.children(); e.hasMoreElements();)
			{
				FileNode fn = (FileNode)e.nextElement();
				if(fn.getFile().isDirectory() && fn.getFile().getName().equals(srcR.getFileName()))
				{
					nxtDir=fn;
					break;
				}
			}
			if(nxtDir==null)
			{
				File F=new File(dest.getFile(),srcR.getFileName());
				if(!F.exists()||(!F.isDirectory()))
				{
					if(F.exists())
					{
						if(JOptionPane.showConfirmDialog(f, "Can not create directory '"+F.getAbsolutePath()+"' due to a file collision.\nDelete it and proceed?","File exists!",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
							return false;
						if(!F.delete())
						{
							JOptionPane.showMessageDialog(f, "Unable to delete file "+F.getAbsolutePath());
							return false;
						}
					}
					if(!F.mkdir())
					{
						JOptionPane.showMessageDialog(f, "Unable to create directory "+F.getAbsolutePath());
						return false;
					}
				}
				nxtDir=new FileNode(dest.getTree(),F);
				dest.add(nxtDir);
			}
			for(Enumeration e=srcR.children(); e.hasMoreElements();)
			{
				RemoteNode nxtR=(RemoteNode)e.nextElement();
				if(!TransferRemoteLocal(rTree,nxtR,nxtDir))
					return false;
			}
			return true;
		}
		File F=new File(dest.getFile(),srcR.getFileName());
		if(F.exists())
		{
			if(F.isDirectory())
			{
				JOptionPane.showMessageDialog(f, "Can not create local file "+F.getAbsolutePath()+" because it collides with a directory.");
				return false;
			}
			if(JOptionPane.showConfirmDialog(f, "File '"+F.getAbsolutePath()+"' already exists.\nDelete it and proceed?","File exists!",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
			if(!F.delete())
			{
				JOptionPane.showMessageDialog(f, "Unable to delete file "+F.getAbsolutePath());
				return false;
			}
		}
		JDialog dlg = zimmerscp.showWorkingDialog(f);
		try {
			srcR.getConnection().getFile(srcR.getFullName(), F.getAbsolutePath());
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to get remote file "+srcR.getFullName()+"\n"+e.getMessage());
			return false;
		}
		finally
		{
			dlg.dispose();
		}
		dest.removeAllChildren();
		dest.build(dest.getFile());
		updateUI();
		repaint();
		return true;
	}

}
