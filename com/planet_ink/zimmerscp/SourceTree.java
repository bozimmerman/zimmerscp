package com.planet_ink.zimmerscp;

import java.awt.Frame;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SourceTree extends DragDropTree
{
	private static final long serialVersionUID = 5315786947498638856L;

	private LocalDialog	dialog		= null;
	private PopupMenu	popupMenu	= null;
	private Frame		f			= null;
	public DragDropTree me			= this;

	public SourceTree(final String name, final Frame f, final FileNode m)
	{
		super(name, f, m);
		this.f=f;
		this.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		this.add(getContextMenu());
	}

	public PopupMenu getContextMenu()
	{
		final TreeNode node = this.getSelectedNode();
		if (popupMenu == null)
		{
			popupMenu = new PopupMenu("Local Option");
			popupMenu.addActionListener(this);
		}
		popupMenu.removeAll();
		if (node instanceof FileNode)
		{
			final FileNode fnode = (FileNode) node;
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

	public void affectSettings(final Frame f, final JLabel label)
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

	public void refresh(final Frame f, final JLabel label)
	{
		setLocal(f, dialog.getRootDir(), label, this, (FileNode) this.getModel().getRoot());
	}

	public void loadSettings(final Frame f, final JLabel label, final Properties p)
	{
		final String local = p.getProperty("local.dir");
		String backups = p.getProperty("local.backups");
		String bindex = p.getProperty("local.backupindexes");
		String bcopies = p.getProperty("local.backupcopies");
		String blankdescs = p.getProperty("local.blankdescs");
		if ((local != null) && (local.length() > 0))
		{
			if (backups == null)
				backups = "";
			if (bindex == null)
				bindex = "false";
			if (bcopies == null)
				bcopies = "false";
			if (blankdescs == null)
				blankdescs = "false";
			if (dialog == null)
				dialog = new LocalDialog(f);
			dialog.fill(local, backups,
					Boolean.valueOf(bindex).booleanValue(),
					Boolean.valueOf(bcopies).booleanValue(),
					Boolean.valueOf(blankdescs).booleanValue());
			setLocal(f, local, label, this, (FileNode) this.getModel().getRoot());
		}
	}

	public void saveSettings(final Properties p)
	{
		final FileNode n = (FileNode) this.getModel().getRoot();
		if (n.getFile() != null)
		{
			p.setProperty("local.dir", n.getFile().getAbsolutePath());
			p.setProperty("local.backups", dialog.getBackupDir());
			p.setProperty("local.backupindexes", String.valueOf(dialog.getBackupIndexes()));
			p.setProperty("local.backupcopies", String.valueOf(dialog.getBackupCopyOvers()));
			p.setProperty("local.blankdescs", String.valueOf(dialog.getBlankDescriptions()));
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

	public boolean getBlankDescs()
	{
		return dialog.getBlankDescriptions();
	}

	private void setLocal(final Frame f, final String root, final JLabel text, final JTree tree, final FileNode node)
	{
		text.setText("<Click Settings Button>");
		final File F = new File(root);
		if ((F.exists()) && (F.isDirectory()))
		{
			text.setText(F.getPath());
			tree.clearSelection();
			node.removeAllChildren();
			final JDialog dlg = zimmerscp.showWorkingDialog(f);
			node.build(F);
			tree.updateUI();
			tree.repaint();
			dlg.dispose();
		}
		else
			JOptionPane.showMessageDialog(this, "The local directory is invalid.");
	}

	private boolean viewLocal(final FileNode node)
	{
		if(node.getFile().isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Directories can not be viewed like that.");
			return false;
		}
		new FileDialog(f,node.getFile().getAbsolutePath(),node.getFile(), false).setVisible(true);
		return true;
	}

	private boolean editLocal(final FileNode node)
	{
		if(node.getFile().isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Directories can not be viewed like that.");
			return false;
		}
		final FileDialog dialog =new FileDialog(f,node.getFile().getAbsolutePath(),node.getFile(), true);
		dialog.setVisible(true);
		if(dialog.isDirty())
		{
			// we're done, really.. the dialog resaved it for us.
		}
		return true;
	}

	public void actionPerformed(final ActionEvent e)
	{
		if (e.getActionCommand().equalsIgnoreCase("Info"))
			new FileInfoDialog(f, (FileNode) getSelectedNode()).setVisible(true);
		else if (e.getActionCommand().equalsIgnoreCase("Delete"))
		{
			if(me.getSelectionModel().getSelectionMode() == TreeSelectionModel.SINGLE_TREE_SELECTION)
				deleteLocal((FileNode) getSelectedNode(),false);
			else
			{
				final TreePath[] paths = me.getSelectionPaths();
				final Set<FileNode> parents = new TreeSet<FileNode>();
				for(final TreePath tP : paths)
				{
					final FileNode node = (FileNode)tP.getLastPathComponent();
					parents.add((FileNode)node.getParent());
				}
				for(final TreePath tP : paths)
				{
					final FileNode node = (FileNode)tP.getLastPathComponent();
					if(!deleteLocal(node,true))
						break;
				}
				if(parents.size()>0)
				{
					for(final FileNode p : parents)
					{
						p.removeAllChildren();
						p.build(p.getFile());
					}
					updateUI();
					repaint();
				}
			}
		}
		else if (e.getActionCommand().equalsIgnoreCase("MakeDir"))
			makeDirLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Rename"))
			renameLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("View"))
			viewLocal((FileNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Edit"))
			editLocal((FileNode) getSelectedNode());
	}

	private final boolean makeDirLocal(final FileNode node)
	{
		final String dirName = JOptionPane.showInputDialog(f,"Directory name");
		final File F = new File(node.getFile(),dirName);
		if(!F.mkdir())
		{
			JOptionPane.showMessageDialog(f, "Unable to create directory.");
			return false;
		}
		node.add(new FileNode(this,F));
		node.sort();
		updateUI();
		repaint();
		return true;
	}

	private final boolean renameLocal(final FileNode node)
	{
		final String newName = JOptionPane.showInputDialog(f,"New name",node.getFileName());
		if((newName==null)||(newName.length()==0))
			return false;
		final File newFile = new File(node.getFile().getParentFile(),newName);
		if(!node.renameTo(newFile))
		{
			JOptionPane.showMessageDialog(f, "Unable to rename "+node.getFile().getAbsolutePath()+" to " + newFile.getAbsolutePath());
			return false;
		}
		updateUI();
		repaint();
		return true;
	}

	private final boolean deleteLocal(final FileNode node, final boolean supressCleanup)
	{
		if(node == getModel().getRoot())
		{
			JOptionPane.showMessageDialog(f, "Root nodes can not be deleted.");
			return false;
		}
		if(JOptionPane.showConfirmDialog(f, "Delete file '"+node.getFile().getName()+"'","Delete Node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return false;
		final FileNode parentNode= (FileNode)node.getParent();
		if(node.getFile().isDirectory())
		{
			if(JOptionPane.showConfirmDialog(f, "This will delete this directory and all children.  Confirm.","Delete Directory",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
		}
		try
		{

			deleteRecurseFile(node.getFile());
			if(!supressCleanup)
			{
				parentNode.removeAllChildren();
				parentNode.build(parentNode.getFile());
				updateUI();
				repaint();
			}
			return true;
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, e.getMessage());
			return false;
		}
	}

	private void deleteRecurseFile(final File F) throws IOException
	{
		if(F==null)
			return;
		if(F.isDirectory())
		{
			final File[] FS = F.listFiles();
			for(int f=0;f<FS.length;f++)
				deleteRecurseFile(FS[f]);
		}
		if(!F.delete())
			throw new IOException("Couldn't delete file "+F.getAbsolutePath());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected boolean handleNodeImport(final Object sn, final Object tn)
	{
		final FileNode tnode = (FileNode)tn;
		List<RemoteNode> fl = new ArrayList<RemoteNode>();
		if (sn instanceof RemoteNode)
			fl.add((RemoteNode) sn);
		else if (sn instanceof List)
			fl = (List) sn;
		if (fl.size() > 0)
			return transferRemoteLocal(fl.iterator().next().getTree(), fl, tnode);
		return false;

	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected boolean testNodeImport(final Object sn, final Object tn)
	{
		if(tn instanceof FileNode)
		{
			final FileNode tnode = (FileNode)tn;
			if (tnode.getFile().isDirectory())
			{
				try
				{
					if (sn instanceof RemoteNode)
						return true;
					if (sn instanceof List)
						return ((List)sn).size()>0;
				}
				catch (final Exception e)
				{
				}
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	protected boolean testNodeImport(final TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			final JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof FileNode)
				{
					final FileNode node = (FileNode) location.getPath().getLastPathComponent();
					if (node.getFile().isDirectory())
					{
						try
						{
							final Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
							if (o instanceof RemoteNode)
								return true;
							if (o instanceof FileNode)
								return true;
							if (o instanceof List)
								return ((List)o).size()>0;
						}
						catch (final Exception e)
						{
						}
					}
				}
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes" })
	protected boolean handleNodeImport(final TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			final JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof FileNode)
				{
					final FileNode node = (FileNode) location.getPath().getLastPathComponent();
					try
					{
						final Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						final List<RemoteNode> fl = new ArrayList<RemoteNode>();
						if (o instanceof RemoteNode)
							fl.add((RemoteNode) o);
						else
						if(o instanceof FileNode)
						{
							final FileNode oparent = (FileNode)((FileNode)o).getParent();
							((FileNode)o).renameTo(new File(node.getFile(), ((FileNode)o).getFileName()));
							oparent.removeAllChildren();
							oparent.build(oparent.getFile());
							node.removeAllChildren();
							node.build(node.getFile());
							updateUI();
							repaint();
							me.expandPath(location.getPath());
						}
						else
						if (o instanceof List)
						{
							for(final Object o1 : (List)o)
							{
								if(o1 instanceof RemoteNode)
									fl.add((RemoteNode)o1);
								else
								if(o1 instanceof FileNode)
								{
									final FileNode oparent = (FileNode)node.getParent();
									((FileNode)o1).renameTo(new File(node.getFile(), ((FileNode)o1).getFileName()));
									oparent.removeAllChildren();
									oparent.build(oparent.getFile());
									node.removeAllChildren();
									node.build(node.getFile());
									updateUI();
									repaint();
									me.expandPath(location.getPath());
								}
							}
						}
						if (fl.size() > 0)
							return transferRemoteLocal(fl.iterator().next().getTree(), fl, node);
					}
					catch (final UnsupportedFlavorException e)
					{
						e.printStackTrace();
					}
					catch (final IOException e)
					{
						e.printStackTrace();
					}
					return false;
				}
		}
		return false;
	}

	private final boolean transferRemoteLocal(final DestTree rTree, final List<RemoteNode> srcRs, final FileNode dest)
	{
		for(final RemoteNode srcF : srcRs)
			if(!transferRemoteLocal(rTree, srcF,dest))
				return false;
		return true;
	}

	private final boolean transferRemoteLocal(final DestTree rTree, final RemoteNode srcR, final FileNode dest)
	{
		if(!dest.getFile().isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Destination, somehow, is not a directory.");
			return false;
		}
		if(srcR.isDirectory())
		{
			FileNode nxtDir = null;
			for(final Enumeration<FileNode> e=dest.children(); e.hasMoreElements();)
			{
				final FileNode fn = e.nextElement();
				if(fn.getFile().isDirectory() && fn.getFile().getName().equals(srcR.getFileName()))
				{
					nxtDir=fn;
					break;
				}
			}
			if(nxtDir==null)
			{
				final File F=new File(dest.getFile(),srcR.getFileName());
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
				dest.sort();
			}
			for(final Enumeration<RemoteNode> e=srcR.children(); e.hasMoreElements();)
			{
				final RemoteNode nxtR=e.nextElement();
				if(!transferRemoteLocal(rTree,nxtR,nxtDir))
					return false;
			}
			return true;
		}
		final File F=new File(dest.getFile(),srcR.getFileName());
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
		final JDialog dlg = zimmerscp.showWorkingDialog(f);
		try {
			srcR.getConnection().getFile(srcR.getFullName(), F.getAbsolutePath());
		}
		catch(final Exception e)
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

	@Override
	public void dragEnter(final DropTargetDragEvent dtde)
	{
	}

	@Override
	public void dragOver(final DropTargetDragEvent dtde)
	{
	}

	@Override
	public void dropActionChanged(final DropTargetDragEvent dtde)
	{
	}

	@Override
	public void dragExit(final DropTargetEvent dte)
	{
	}

	@Override
	public void drop(final DropTargetDropEvent dtde)
	{
		try
		{
			final Transferable tr = dtde.getTransferable();
			final DataFlavor[] flavors = tr.getTransferDataFlavors();
			for (int i = 0; i < flavors.length; i++)
			{
				if (flavors[i].isFlavorJavaFileListType())
				{
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					final Object o = tr.getTransferData(flavors[i]);
					final Point loc = dtde.getLocation();
					final TreePath destinationPath = getPathForLocation(loc.x, loc.y);
					final DefaultMutableTreeNode tNode = (DefaultMutableTreeNode)destinationPath.getLastPathComponent();
					if(!(o instanceof List))
					{
						if(this.testNodeImport(o, tNode))
							this.handleNodeImport(o, tNode);
						continue;
					}
					@SuppressWarnings("rawtypes")
					final List list = (List) o;
					for (int j = 0; j < list.size(); j++)
					{
						final File F = new File(""+list.get(j));
						if(F.exists())
						{
							if(tNode instanceof FileNode)
							{
								final FileNode fn = (FileNode)tNode;
								if(fn.isDirectory())
								{
									final File newF = new File(fn.getFile(), F.getName());
									if(newF.exists())
									{
										JOptionPane.showMessageDialog(f, "Unable to copy local file "+F.getAbsolutePath()+": File Exists.");
										dtde.dropComplete(true);
										return;
									}
									final JDialog dlg = zimmerscp.showWorkingDialog(f);
									try(FileInputStream fin = new FileInputStream(F))
									{
										try(FileOutputStream fout = new FileOutputStream(newF))
										{
											final byte[] buf = new byte[1024];
											while (true)
											{
												final int len = fin.read(buf, 0, buf.length);
												if (len <= 0)
												{
													break;
												}
												fout.write(buf, 0, len);
											}
										}
									}
									finally
									{
										dlg.dispose();
									}
									fn.removeAllChildren();
									fn.build(fn.getFile());
									updateUI();
									repaint();
								}
							}
						}
						else
						{
							if(this.testNodeImport(o, tNode))
								this.handleNodeImport(o, tNode);
						}
						//System.out.println("JFLT"+list.get(j) + "\n");
					}
					dtde.dropComplete(true);
					return;
				}
				else if (flavors[i].isFlavorSerializedObjectType())
				{
					/*
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					final Object o = tr.getTransferData(flavors[i]);
					System.out.println("Object: " + o);
					dtde.dropComplete(false);
					*/
				}
				else if (flavors[i].isRepresentationClassInputStream())
				{
					/*
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					final InputStreamReader rin = new InputStreamReader((InputStream) tr.getTransferData(flavors[i]));
					long bytes = 0;
					while(rin.ready())
					{
						if(rin.read() != -1)
							bytes++;
					}
					System.out.println("bytes: "+bytes);
					*/
				}
			}
			dtde.rejectDrop();
		}
		catch (final Exception e)
		{
			//e.printStackTrace();
			//dtde.rejectDrop();
		}
	}

}
