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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jcraft.jsch.JSchException;

public class DestTree extends DragDropTree
{
	private static final long serialVersionUID = -875565996009941468L;

	private RemoteDialog	dialog		= null;
	private SCPConnection	conn		= null;
	private int				num			= 0;
	private PopupMenu		popupMenu	= null;
	private Frame			f			= null;
	private JLabel			topLabel	= null;

	public DestTree(final String name, final Frame f, final RemoteNode m, final int num)
	{
		super(name, f, m);
		this.f=f;
		this.num = num;
		this.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		this.add(getContextMenu());
	}

	public void setNum(final int num)
	{
		this.num = num;
		if(topLabel != null)
			topLabel.setText("Remote Filesystem#"+num);
	}

	public int getNum()
	{
		return num;
	}

	public void setTopLabel(final JLabel label)
	{
		this.topLabel = label;
	}

	public PopupMenu getContextMenu()
	{
		final TreeNode node = this.getSelectedNode();
		if (popupMenu == null)
		{
			popupMenu = new PopupMenu("Target Option");
			popupMenu.addActionListener(this);
		}
		popupMenu.removeAll();
		if (node instanceof RemoteNode)
		{
			final RemoteNode rnode = (RemoteNode) node;
			popupMenu.add("Info");
			popupMenu.add("Delete");
			popupMenu.add("Rename");
			if (rnode.isDirectory())
			{
				popupMenu.add("MakeDir");
				popupMenu.add("New 00INDEX");
			}
			else
			{
				popupMenu.add("View");
				popupMenu.add("Edit");
			}
		}
		return popupMenu;
	}

	public boolean getSync()
	{
		return (dialog != null) ? dialog.getManageSync() : false;
	}

	public boolean getManageIndexes()
	{
		return (dialog != null) ? dialog.getManageIndex() : false;
	}

	public void affectSettings(final Frame f, final JLabel label)
	{
		if (dialog == null)
			dialog = new RemoteDialog(f);
		dialog.setLocationRelativeTo(this); // center on screen
		dialog.setModal(true);
		dialog.toFront(); // raise above other java windows
		dialog.setVisible(true);
		if (!dialog.wasCancelled())
		{
			if (conn != null)
				conn.close();
			conn = new SCPConnection(dialog.getHost(), "", dialog.getUser(), dialog.getPassword());
			setDestination(f, dialog.getRoot(), this, (RemoteNode) this.getModel().getRoot(), label);
		}
	}

	public void refresh(final Frame f, final JLabel label)
	{
		if (dialog == null)
			dialog = new RemoteDialog(f);
		if (conn != null)
			conn.close();
		conn = new SCPConnection(dialog.getHost(), "", dialog.getUser(), dialog.getPassword());
		setDestination(f, dialog.getRoot(), this, (RemoteNode) this.getModel().getRoot(), label);
	}

	public void loadSettings(final Frame f, final JLabel label, final Properties p)
	{
		final String host = p.getProperty("remote" + num + ".host");
		final String user = p.getProperty("remote" + num + ".user");
		final String password = p.getProperty("remote" + num + ".password");
		final String knownhosts = p.getProperty("remote" + num + ".knownhosts");
		final String dir = p.getProperty("remote" + num + ".dir");
		String mindex = p.getProperty("remote" + num + ".manageindexes");
		String msync = p.getProperty("remote" + num + ".managesync");
		String cr00index = p.getProperty("remote" + num + ".create00index");
		if(mindex==null) mindex="false";
		if(msync==null) msync="false";
		if(cr00index==null) cr00index="false";
		if ((host != null) && (dir != null) && (host.length() > 0) && (dir.length() > 0))
		{
			if (dialog == null)
				dialog = new RemoteDialog(f);
			dialog.fill(host, user, password, dir, Boolean.valueOf(mindex).booleanValue(), Boolean.valueOf(msync).booleanValue(), Boolean.valueOf(cr00index).booleanValue());
			if (conn != null)
				conn.close();
			conn = new SCPConnection(host, knownhosts, user, password);
			setDestination(f, dir, this, (RemoteNode) this.getModel().getRoot(), label);
		}
	}

	public void saveSettings(final Properties p)
	{
		final RemoteNode n = (RemoteNode) this.getModel().getRoot();
		if ((n.getFullName() != null) && (conn != null) && (dialog != null))
		{
			p.setProperty("remote" + num + ".host", conn.getHost());
			p.setProperty("remote" + num + ".user", conn.getUser());
			p.setProperty("remote" + num + ".password", conn.getPassword());
			p.setProperty("remote" + num + ".knownhosts", conn.getKnownHostsFile());
			p.setProperty("remote" + num + ".dir", n.getFullName());
			p.setProperty("remote" + num + ".manageindexes", String.valueOf(dialog.getManageIndex()));
			p.setProperty("remote" + num + ".managesync", String.valueOf(dialog.getManageSync()));
			p.setProperty("remote" + num + ".create00index", String.valueOf(dialog.getCreate00INDEX()));
		}
	}

	public void setDestination(final Frame f, final String root, final JTree tree, final RemoteNode node, final JLabel textField)
	{
		textField.setText("<Click Settings Button>");
		final JDialog dlg = zimmerscp.showWorkingDialog(f);
		tree.clearSelection();
		final String host = conn.getHost();
		node.init("", root, System.currentTimeMillis(), 'd');
		node.removeAllChildren();
		try
		{
			textField.setText(host + ":" + root);
			node.setConnection(conn);
			node.loadKids();
		}
		catch (final JSchException e)
		{
			dlg.dispose();
			JOptionPane.showMessageDialog(this, "Unable to connect to " + host);
		}
		catch (final Exception e)
		{
			dlg.dispose();
			JOptionPane.showMessageDialog(this, "Unable to read path: " + root);
		}
		finally
		{
			tree.updateUI();
			tree.repaint();
			dlg.dispose();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected boolean handleNodeImport(final Object sn, final Object tn)
	{
		final RemoteNode node = (RemoteNode) tn;
		if(sn instanceof RemoteNode)
			return softlinkRemote((RemoteNode)sn, node);
		else
		{
			List<File> fl = new ArrayList<File>();
			if (sn instanceof FileNode)
				fl.add(((FileNode) sn).getFile());
			else if (sn instanceof List)
				fl = (List) sn;
			for (final File f : fl)
				if ((f == null) || (!f.exists()))
					return false;
			if (fl.size() > 0)
				return transferLocalRemote(fl, node);
		}
		return false;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected boolean testNodeImport(final Object sn, final Object tn)
	{
		if(tn instanceof RemoteNode)
		{
			final RemoteNode tnode = (RemoteNode)tn;
			if (tnode.isDirectory())
			{
				try
				{
					if (sn instanceof FileNode)
						return true;
					if (sn instanceof List)
					{
						final List<File> fl = (List) sn;
						for (final File f : fl)
							if ((f == null) || (!f.exists()))
								return false;
						return fl.size() > 0;
					}
					if((sn instanceof RemoteNode)
					&&(!((RemoteNode)sn).isSoftlink())
					&&(tnode.getTree()==((RemoteNode)sn).getTree()))
						return true;
				}
				catch (final Exception e)
				{
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected boolean testNodeImport(final TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			final JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof RemoteNode)
				{
					final RemoteNode node = (RemoteNode) location.getPath().getLastPathComponent();
					if (node.isDirectory())
					{
						try
						{
							final Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
							if (o instanceof FileNode)
								return true;
							else
							if (o instanceof List)
							{
								@SuppressWarnings("rawtypes")
								final List<File> fl = (List) o;
								for (final File f : fl)
									if ((f == null) || (!f.exists()))
										return false;
								return fl.size() > 0;
							}
							if((o instanceof RemoteNode)
							&&(!((RemoteNode)o).isSoftlink())
							&&(node.getTree()==((RemoteNode)o).getTree()))
								return true;
						}
						catch (final Exception e)
						{
						}
					}
				}
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean handleNodeImport(final TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			final JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof RemoteNode)
				{
					final RemoteNode node = (RemoteNode) location.getPath().getLastPathComponent();
					try
					{
						final Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if(o instanceof RemoteNode)
							return softlinkRemote((RemoteNode)o, node);
						else
						{
							List<File> fl = new ArrayList<File>();
							if (o instanceof FileNode)
								fl.add(((FileNode) o).getFile());
							else if (o instanceof List)
								fl = (List) o;
							for (final File f : fl)
								if ((f == null) || (!f.exists()))
									return false;
							if (fl.size() > 0)
								return transferLocalRemote(fl, node);
						}
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

	private final boolean transferLocalRemote(final List<File> srcFs, final RemoteNode dest)
	{
		final Vector<File> files=new Vector<File>();
		for(final File F : srcFs)
			if(F.getName().equalsIgnoreCase("00index"))
			{
				if(files.size()>0)
					files.insertElementAt(F, 0);
				else
					files.addElement(F);
			}
			else
				files.addElement(F);
		for(final File srcF : files)
			if(!transferLocalRemote(srcF,dest))
				return false;
		return true;
	}

	private final boolean transferLocalRemote(final File srcF, final RemoteNode dest)
	{
		if(!dest.isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Destination, somehow, is not a directory.");
			return false;
		}
		RemoteNode zeroZeroIndexFile = null;
		if(getManageIndexes()&&(!srcF.getName().equalsIgnoreCase("00index")))
		{
			zeroZeroIndexFile = getIn00INDEX(dest);
			if((zeroZeroIndexFile==null)
			&&(getManageIndexes())
			&&(JOptionPane.showConfirmDialog(f, "00INDEX file not found.\nContinue to copy?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
				return false;
		}
		if(srcF.isDirectory())
		{
			RemoteNode nxtDir = dest.findChildNode(srcF.getName(),srcF.isDirectory());
			if(nxtDir==null)
			{
				if(!makeDirectoryRemote(dest,srcF.getName(),null,false))
					return false;
				nxtDir = dest.findChildNode(srcF.getName(),srcF.isDirectory());
				if(nxtDir==null)
				{
					JOptionPane.showMessageDialog(f, "Directory, somehow, was not made in dest tree.");
					return false;
				}
				dest.add(nxtDir);
				dest.sort();
			}
			final File[] filelist = srcF.listFiles();
			final Vector<File> files=new Vector<File>();
			for(int f=0;f<filelist.length;f++)
				if(filelist[f].getName().equalsIgnoreCase("00index"))
				{
					if(f>0)
						files.insertElementAt(filelist[f], 0);
					else
						files.addElement(filelist[f]);
				}
				else
					files.addElement(filelist[f]);
			for(final File nxtF : files)
				if(!transferLocalRemote(nxtF,nxtDir))
					return false;
			dest.removeAllChildren();
			dest.safeLoadKids();
			updateUI();
			repaint();
			return true;
		}
		RemoteNode existFile = dest.findChildNode(srcF.getName(), true);
		if(existFile==null) existFile = dest.findChildNode(srcF.getName(), false);
		if(existFile!=null)
		{
			if(existFile.isDirectory())
			{
				JOptionPane.showMessageDialog(f, "Can not create remote file "+existFile.getFullName()+" because it collides with a directory.");
				return false;
			}
			if(JOptionPane.showConfirmDialog(f, "Remote File '"+existFile.getFullName()+"' already exists.\nDelete it and proceed?","File exists!",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
			final JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				if(getSync())
				{
					try
					{
						boolean response=unsafeDeleteRemoteFile(this,existFile);
						if(!response)
						{
							if(JOptionPane.showConfirmDialog(f, "Error: Continue to delete?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
								return false;
						}
						for(final RemoteNode otherNode : findSiblings(existFile))
						{
							if(response && getSync()&&(otherNode != null))
							{
								final DestTree otherTree = otherNode.getTree();
								response=unsafeDeleteRemoteFile(otherTree, otherNode);
								if(!response)
								{
									JOptionPane.showMessageDialog(f, "Unable to delete remote file "+existFile.getFullName());
									return false;
								}
							}
						}
					}
					catch(final Exception e){
						JOptionPane.showMessageDialog(f, "Unable to delete remote file "+existFile.getFullName()+"\n"+e.getMessage());
						return false;
					}
				}
			}
			finally
			{
				dlg.dispose();
			}
		}
		final JDialog dlg = zimmerscp.showWorkingDialog(f);
		try
		{
			dest.getConnection().sendFile(srcF.getAbsolutePath(),dest.combine(dest.getFullName(),srcF.getName()));
			if(getSync())
			{
				try
				{
					for(final RemoteNode otherDir : findSiblings(dest))
					{
						final DestTree otherTree = otherDir.getTree();
						otherDir.getConnection().sendFile(srcF.getAbsolutePath(),otherDir.combine(otherDir.getFullName(),srcF.getName()));
						otherDir.removeAllChildren();
						otherDir.safeLoadKids();
						otherTree.updateUI();
						otherTree.repaint();
					}
				}
				catch(final Exception e){
					JOptionPane.showMessageDialog(f, "Unable to sync '"+srcF.getAbsolutePath()+" to remote dir "+dest.getFullName()+"\n"+e.getMessage());
					return false;
				}
			}
			dest.removeAllChildren();
			dest.safeLoadKids();
			updateUI();
			repaint();
			final RemoteNode fileNode = dest.findChildNode(srcF.getName(), srcF.isDirectory());
			if(fileNode == null)
			{
				JOptionPane.showMessageDialog(f, "Unable to find '"+srcF.getName()+" after copy.");
				return false;
			}
			if((zeroZeroIndexFile!=null)&&(getManageIndexes())&&(!srcF.getName().equalsIgnoreCase("00index")))
			{
				zeroZeroIndexFile=this.getMy00INDEX(fileNode);
				if(!addToBoth00INDEX(zeroZeroIndexFile, fileNode, null,false))
					return false;
			}
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to send remote file "+srcF.getName()+"\n"+e.getMessage());
			return false;
		}
		finally
		{
			dlg.dispose();
		}
		return true;
	}

	private boolean new00INDEX(final RemoteNode node)
	{
		if(!node.isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Only directories can have new 00INDEX files created in them.");
			return false;
		}
		if(node.findChildNodeIgnoreCase("00INDEX")!=null)
		{
			JOptionPane.showMessageDialog(f, "A 00INDEX file already exists!");
			return false;
		}
		final File tempDirF=new File(System.getProperty("java.io.tmpdir"));
		if((!tempDirF.exists())||(!tempDirF.isDirectory()))
		{
			JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
			return false;
		}
		final File F=new File(tempDirF,"00INDEX");
		if(F.exists())
			if(!F.delete())
			{
				JOptionPane.showMessageDialog(f, "Unable to delete "+F.getAbsolutePath()+".");
				return false;
			}
		try
		{
			final FileOutputStream fo=new FileOutputStream(F);
			fo.write((char)10);
			fo.close();
			if(!transferLocalRemote(F, node))
				return false;
			return true;
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to create 00INDEX file at "+node.getFullName()+".");
			return false;
		}
	}

	private File prepareViewEditFile(final RemoteNode node)
	{
		if(node.isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Directories can not be edited like that.");
			return null;
		}
		final File tempDirF=new File(System.getProperty("java.io.tmpdir"));
		if((!tempDirF.exists())||(!tempDirF.isDirectory()))
		{
			JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
			return null;
		}
		final File F=new File(tempDirF,node.getFileName());
		if(F.exists())
		{
			if(F.isDirectory())
			{
				JOptionPane.showMessageDialog(f, "Can not create local file "+F.getAbsolutePath()+" because it collides with a directory.");
				return null;
			}
			if(!F.delete())
			{
				JOptionPane.showMessageDialog(f, "Unable to delete file "+F.getAbsolutePath());
				return null;
			}
		}
		final JDialog dlg = zimmerscp.showWorkingDialog(f);
		try {
			node.getConnection().getFile(node.getFullName(), F.getAbsolutePath());
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to get remote file "+node.getFullName()+"\n"+e.getMessage());
			return null;
		}
		finally
		{
			dlg.dispose();
		}
		return F;
	}

	private boolean viewRemote(final RemoteNode node)
	{
		final File F=prepareViewEditFile(node);
		if(F==null) return false;
		new FileDialog(f, node.getFullName(), F, false).setVisible(true);
		return true;
	}

	private boolean editRemote(final RemoteNode node)
	{
		final File F=prepareViewEditFile(node);
		if(F==null) return false;
		final FileDialog dialog = new FileDialog(f,node.getFullName(),F, true);
		dialog.setVisible(true);
		if(dialog.isDirty())
		{
			final JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				if(!node.getConnection().deleteFile(node.getFullName(), false))
				{
					if(JOptionPane.showConfirmDialog(f, "Error deleting remote file "+node.getFullName()+". Continue?", "Delete node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
						return false;
				}
				if(!node.getConnection().sendFile(F.getAbsolutePath(),node.getFullName()))
				{
					if(JOptionPane.showConfirmDialog(f, "Error sending remote file "+node.getFullName()+". Continue?", "Send node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
						return false;
				}
				if(getSync())
				{
					try
					{
						for(final RemoteNode otherNode : findSiblings(node))
						{
							if(!otherNode.getConnection().deleteFile(otherNode.getFullName(), false))
							{
								if(JOptionPane.showConfirmDialog(f, "Error deleting remote file "+node.getFullName()+". Continue?", "Delete node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
									return false;
							}
							if(!otherNode.getConnection().sendFile(F.getAbsolutePath(),otherNode.getFullName()))
							{
								if(JOptionPane.showConfirmDialog(f, "Error sending remote file "+node.getFullName()+". Continue?", "Send node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
									return false;
							}
						}
					}
					catch(final Exception e)
					{
						JOptionPane.showMessageDialog(f, "Sync Error editing remote file "+node.getFullName()+"\n"+e.getMessage());
						return false;
					}
				}
			}
			catch(final Exception e)
			{
				JOptionPane.showMessageDialog(f, "Error editing remote file "+node.getFullName()+"\n"+e.getMessage());
				return false;
			}
			finally
			{
				dlg.dispose();
			}
		}
		return true;
	}

	private final boolean deleteRemote(final RemoteNode node)
	{
		if(node == getModel().getRoot())
		{
			JOptionPane.showMessageDialog(f, "Root nodes can not be deleted.");
			return false;
		}
		if(getSync())
		{
			if(JOptionPane.showConfirmDialog(f, "Delete file '"+node.getFileName()+"'","Delete Node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
			if(node.isDirectory())
			{
				if(JOptionPane.showConfirmDialog(f, "This will delete this directory and all children.  Confirm.","Delete Directory",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
			if(!backUp(node,false))
				return false;
			final RemoteNode zeroZeroIndexFile = getMy00INDEX(node);
			if((zeroZeroIndexFile==null)
			&&(getManageIndexes())
			&&(!node.getFileName().equalsIgnoreCase("00index"))
			&&(JOptionPane.showConfirmDialog(f, "00INDEX file not found.\nContinue to delete?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
				return false;

			if((zeroZeroIndexFile!=null)&&(getManageIndexes())&&(!node.getFileName().equalsIgnoreCase("00index")))
			{
				if(!removeFromBoth00INDEX(zeroZeroIndexFile, node, null, false))
					return false;
			}
			boolean response=unsafeDeleteRemoteFile(this,node);
			if(!response)
			{
				if(JOptionPane.showConfirmDialog(f, "Error: Continue to delete?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
			try
			{
				for(final RemoteNode otherNode : findSiblings(node))
				{
					if(response && getSync() && (otherNode != null))
					{
						final DestTree otherTree = otherNode.getTree();
						response=unsafeDeleteRemoteFile(otherTree, otherNode);
						if(!response)
						{
							if(JOptionPane.showConfirmDialog(f, "Error: Continue to delete?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
								return false;
						}
					}
				}
			}
			catch(final Exception e)
			{
				if(JOptionPane.showConfirmDialog(f, "Sync node error: "+e.getMessage()+"\nContinue to delete?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
		}

		return true;
	}

	private final boolean softlinkRemote(final RemoteNode node, final RemoteNode destDir)
	{
		if(node == getModel().getRoot())
		{
			JOptionPane.showMessageDialog(f, "Root nodes can not be linked anywhere.");
			return false;
		}
		if(node==null)
		{
			JOptionPane.showMessageDialog(f, "Node is missing.");
			return false;
		}
		if(destDir==null)
		{
			JOptionPane.showMessageDialog(f, "Destination is missing.");
			return false;
		}
		if(!destDir.isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Destination is not a directory.");
			return false;
		}
		if(JOptionPane.showConfirmDialog(f, "Softlink file '"+node.getFileName()+"' into directory '"+destDir.getFileName()+"'","Create Soft Link",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return false;
		if(destDir.findChildNode(node.getFileName(), node.isDirectory())!=null)
		{
			JOptionPane.showMessageDialog(f, "That file seems to already exist in "+destDir.getFileName()+".");
			return false;
		}
		final RemoteNode destZeroZeroIndexFile = getIn00INDEX(destDir);
		if((destZeroZeroIndexFile==null)
		&&(getManageIndexes())
		&&(!node.getFileName().equalsIgnoreCase("00index"))
		&&(JOptionPane.showConfirmDialog(f, "Destination 00INDEX file not found.\nContinue to create softlink?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
			return false;
		final RemoteNode nodeZeroZeroIndexFile = getMy00INDEX(node);
		if((nodeZeroZeroIndexFile==null)
		&&(getManageIndexes())
		&&(!node.getFileName().equalsIgnoreCase("00index"))
		&&(JOptionPane.showConfirmDialog(f, "Source 00INDEX file not found.\nContinue to create softlink?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
			return false;
		if(getSync())
		{
			try
			{
				for(final RemoteNode otherNode : findSiblings(node))
				{
					try
					{
						final RemoteNode otherDestDir = findSiblingNode(otherNode.getTree(), destDir);
						RemoteNode otherDestZeroZeroIndexFile = null;
						if(getSync() && getManageIndexes() && (otherDestDir!=null))
						{
							otherDestZeroZeroIndexFile = getIn00INDEX(otherDestDir);
							if((otherDestZeroZeroIndexFile==null)
							&&(JOptionPane.showConfirmDialog(f, "Destination Sync 00INDEX file not found.\nContinue to create softlink?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
								return false;
						}
						if((destZeroZeroIndexFile==null)
						&&(getManageIndexes())
						&&(!node.getFileName().equalsIgnoreCase("00index"))
						&&(JOptionPane.showConfirmDialog(f, "Destination 00INDEX file not found.\nContinue to create softlink?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
							return false;

						String description = null;
						if((destZeroZeroIndexFile!=null)
						&&(nodeZeroZeroIndexFile!=null)
						&&(getManageIndexes())
						&&(!node.getFileName().equalsIgnoreCase("00index")))
						{
							description = get00INDEXDescription(nodeZeroZeroIndexFile, node);
							if(description != null)
							{
								if(!addToBoth00INDEX(destZeroZeroIndexFile, node, description, true))
									return false;
								if((otherDestZeroZeroIndexFile!=null)&&(otherNode != null))
								{
									for(final DestTree otherTree : zimmerscp.INSTANCE.getOtherRemoteTree(this))
									{
										if(!otherTree.addToBoth00INDEX(otherDestZeroZeroIndexFile, otherNode, description, true))
											return false;
									}
								}
							}
						}

						// time to build the relative path
						final Vector<RemoteNode> sourcePath = buildPathToRoot(node);
						final Vector<RemoteNode> destPath = buildPathToRoot(destDir);
						while((sourcePath.size()>0)&&(destPath.size()>0)&&(sourcePath.lastElement() == destPath.lastElement()))
						{
							sourcePath.removeElementAt(sourcePath.size()-1);
							destPath.removeElementAt(destPath.size()-1);
						}
						String finalSourcePath = "";
						for(int i=0;i<destPath.size();i++)
							finalSourcePath+=".."+node.separatorChar();
						for(int i=sourcePath.size()-1;i>0;i--)
							finalSourcePath+=sourcePath.elementAt(i).getFileName()+node.separatorChar();
						if(sourcePath.size()>0)
							finalSourcePath+=sourcePath.firstElement().getFileName();

						try
						{
							conn.createSoftLink(finalSourcePath, destDir.getFullName()+destDir.separatorChar()+node.getFileName());
							destDir.removeAllChildren();
							destDir.safeLoadKids();
							updateUI();
							repaint();
							if(getSync() && (otherNode != null))
							{
								final DestTree otherTree = otherNode.getTree();
								otherTree.conn.createSoftLink(finalSourcePath,  otherDestDir.getFullName()+otherDestDir.separatorChar()+otherNode.getFileName());
								otherDestDir.removeAllChildren();
								otherDestDir.safeLoadKids();
								otherTree.updateUI();
								otherTree.repaint();
							}
						}
						catch(final Exception e)
						{
							if(JOptionPane.showConfirmDialog(f, "Unable to create softlink for "+otherNode.getFullName()+"\n"+e.getMessage()+"\n.  Continue creating soft links?",destDir.getFullName(),JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
								return false;
						}
					}
					catch(final Exception e)
					{
						if(JOptionPane.showConfirmDialog(f, "Unable to create softlink for "+otherNode.getFullName()+"\n"+e.getMessage()+"\n.  Continue creating soft links?",otherNode.getFullName(),JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
							return false;
					}
				}
			}
			catch(final Exception e)
			{
				if(JOptionPane.showConfirmDialog(f, "Sync node error: "+e.getMessage()+"\nContinue to create link?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
		}
		return true;
	}

	private final RemoteNode getMy00INDEX(final RemoteNode node)
	{
		return getIn00INDEX((RemoteNode)node.getParent());
	}

	private final RemoteNode getIn00INDEX(final RemoteNode node)
	{
		final RemoteNode my00index=node.findChildNodeIgnoreCase("00index", false);
		if(my00index!=null) return my00index;
		if(dialog.getCreate00INDEX())
			if(new00INDEX(node))
				return node.findChildNodeIgnoreCase("00index", false);
		return null;
	}

	private boolean removeFromBoth00INDEX(final RemoteNode zeroZeroIndexFile, final RemoteNode node, final String[] oldDesc, final boolean neverEverSync)
	{
		File F=null;
		try
		{
			final File tempDirF=new File(System.getProperty("java.io.tmpdir"));
			if((!tempDirF.exists())||(!tempDirF.isDirectory()))
			{
				JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
				return false;
			}
			F=new File(tempDirF,zeroZeroIndexFile.getFileName());
			if(F.exists())
				if(!F.delete())
				{
					JOptionPane.showMessageDialog(f, "Unable to delete "+F.getAbsolutePath()+".");
					return false;
				}
			final JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				if(!backUp(zeroZeroIndexFile,false))
					return false;
				zeroZeroIndexFile.getConnection().getFile(zeroZeroIndexFile.getFullName(), F.getAbsolutePath());
				final String[] cr=new String[1];
				final List<String> lines = readStringFile(F,cr);
				String s=null;
				boolean deedIsDone=false;
				final String fileName = node.getFileName() + (node.isDirectory()?"/":"");
				final boolean isLink = node.isSoftlink();
				for(int i=0;i<lines.size();i++)
				{
					s=lines.get(i);
					if(s.startsWith("*"))
						continue;
					if((s.trim().equalsIgnoreCase(fileName)
						||(isLink&&s.trim().equalsIgnoreCase(fileName+"/")))
					&&(!Character.isWhitespace(s.charAt(0))))
					{
						lines.remove(i);
						while((i<lines.size())
						&& (lines.get(i).trim().length()>0)
						&& (Character.isWhitespace(lines.get(i).charAt(0))))
						{
							if((oldDesc!=null)&&(oldDesc.length>0))
							{
								if(oldDesc[0]==null)oldDesc[0]="";
								oldDesc[0]+=" " +lines.remove(i).trim();
							}
							else
								lines.remove(i);
						}
						if((i>0)
						&&(i<lines.size())
						&&(lines.get(i-1).trim().length()==0)
						&&(lines.get(i).trim().length()==0))
							lines.remove(i);
						deedIsDone=true;
						break;
					}
				}
				if(!deedIsDone)
				{
					return JOptionPane.showConfirmDialog(f, "Unable to find "+node.getFileName()+" in "+zeroZeroIndexFile.getFullName()+"\nContinue with the operation?","Error",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
				}
				zeroZeroIndexFile.getConnection().deleteFile(zeroZeroIndexFile.getFullName(), false);
				writeStringFile(lines,F,cr);
				zeroZeroIndexFile.getConnection().sendFile(F.getAbsolutePath(),zeroZeroIndexFile.getFullName());
			}
			finally
			{
				dlg.dispose();
			}
			if(getSync()&&(!neverEverSync))
			{
				for(final RemoteNode otherNode : findSiblings(node))
				{
					final DestTree otherTree = otherNode.getTree();
					RemoteNode otherZeroZeroIndexFile = null;
					if((otherTree!=null)&&(otherNode!=null))
						otherZeroZeroIndexFile = otherTree.getMy00INDEX(otherNode);
					if(otherZeroZeroIndexFile!=null)
					{
						final String[] otherOldDesc = ((oldDesc!=null)&&(oldDesc.length>0)&&(oldDesc[0].length()>0))?null:oldDesc;
						if(!otherTree.removeFromBoth00INDEX(otherZeroZeroIndexFile, otherNode, otherOldDesc, true))
							return false;
					}
				}
			}
			return true;
		}
		catch(final Exception e)
		{
			return JOptionPane.showConfirmDialog(f, "Unable to manage 00INDEX file for "+node.getFullName()+"\n"+e.getMessage()+"\nContinue with the operation?","Error",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
		}
	}

	private boolean addToBoth00INDEX(final RemoteNode zeroZeroIndexFile, final RemoteNode node, String description, final boolean neverEverSync)
	{
		File F=null;
		try
		{
			final File tempDirF=new File(System.getProperty("java.io.tmpdir"));
			if((!tempDirF.exists())||(!tempDirF.isDirectory()))
			{
				JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
				return false;
			}
			F=new File(tempDirF,zeroZeroIndexFile.getFileName());
			if(F.exists())
				if(!F.delete())
				{
					JOptionPane.showMessageDialog(f, "Unable to delete "+F.getAbsolutePath()+".");
					return false;
				}
			final JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				if(!backUp(zeroZeroIndexFile,false))
					return false;
				zeroZeroIndexFile.getConnection().getFile(zeroZeroIndexFile.getFullName(), F.getAbsolutePath());
				final String[] cr=new String[1];
				final List<String> lines = readStringFile(F,cr);
				String s=null;
				final String fileName = node.getFileName() + (node.isDirectory()?"/":"");
				int placeToInsert=node.isDirectory()?0:lines.size()-1;
				boolean fileexists=false;
				for(int i=placeToInsert;i<lines.size()&& (i>=0);i=i+(node.isDirectory()?1:-1))
				{
					s=lines.get(i);
					if(s.startsWith("*"))
					{
					}
					else
					if(s.trim().length()==0)
						placeToInsert=i;
					else
					if((s.length()>0)&&(Character.isWhitespace(s.charAt(0)))) // is a comment
					{
					}
					else
					{
						s=s.trim();
						final boolean fileIsDirectory = s.endsWith("/");
						if(s.equalsIgnoreCase(fileName))
						{
							fileexists=s.equals(fileName);
							lines.remove(i);
							while((i<lines.size())
							&& (lines.get(i).trim().length()>0)
							&& (Character.isWhitespace(lines.get(i).charAt(0))))
							{
								description=(description==null)?lines.get(i):(description+" "+lines.get(i).trim());
								lines.remove(i);
							}
							if((i>0)
							&&(i<lines.size())
							&&(lines.get(i-1).trim().length()==0)
							&&(lines.get(i).trim().length()==0))
								lines.remove(i);
							placeToInsert=i;
							break;
						}
						else
						if(node.isDirectory()!=fileIsDirectory)
							break;
						else
						if((node.isDirectory())&&(s.compareToIgnoreCase(fileName)>0))
							break;
						else
						if((!node.isDirectory())&&(s.compareToIgnoreCase(fileName)<0))
							break;
					}
				}
				description = makeDescription(this,(RemoteNode)node.getParent(),node.getFileName(),description);
				if(description == null) return false;
				if(placeToInsert >= lines.size())
					lines.add("");
				lines.add(placeToInsert++,"");
				lines.add(placeToInsert++, fileName);
				String d=description;
				while((d!=null)&&(d.length()>0))
				{
					if(d.length()<78)
					{
						lines.add(placeToInsert++, "  "+d.trim());
						d=null;
					}
					else
					{
						int x=d.lastIndexOf(" ", 78);
						if(x<0) x=d.indexOf(" ");
						if(x<0) x=d.length();
						lines.add(placeToInsert++, "  "+d.substring(0,x).trim());
						d=d.substring(x).trim();
					}
				}
				if(!fileexists)
				{
					zeroZeroIndexFile.getConnection().deleteFile(zeroZeroIndexFile.getFullName(), false);
					writeStringFile(lines,F,cr);
					zeroZeroIndexFile.getConnection().sendFile(F.getAbsolutePath(),zeroZeroIndexFile.getFullName());
				}
			}
			finally
			{
				dlg.dispose();
			}
			if(getSync()&&(!neverEverSync))
			{
				for(final RemoteNode otherNode : findSiblings(node))
				{
					final DestTree otherTree = otherNode.getTree();
					RemoteNode otherZeroZeroIndexFile = null;
					if((otherTree!=null)&&(otherNode!=null))
						otherZeroZeroIndexFile = otherTree.getMy00INDEX(otherNode);
					if(otherZeroZeroIndexFile!=null)
						if(!otherTree.addToBoth00INDEX(otherZeroZeroIndexFile, otherNode, description, true))
							return false;
				}
			}
			return true;
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to add "+node.getFullName()+"\n to "+zeroZeroIndexFile.getFullName()+"\n"+e.getMessage());
			return false;
		}
	}

	private String get00INDEXDescription(final RemoteNode zeroZeroIndexFile, final RemoteNode node)
	{
		File F=null;
		String description = null;
		try
		{
			final File tempDirF=new File(System.getProperty("java.io.tmpdir"));
			if((!tempDirF.exists())||(!tempDirF.isDirectory()))
			{
				JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
				return null;
			}
			F=new File(tempDirF,zeroZeroIndexFile.getFileName());
			if(F.exists())
				if(!F.delete())
				{
					JOptionPane.showMessageDialog(f, "Unable to delete "+F.getAbsolutePath()+".");
					return null;
				}
			final JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				zeroZeroIndexFile.getConnection().getFile(zeroZeroIndexFile.getFullName(), F.getAbsolutePath());
				final List<String> lines = readStringFile(F,new String[1]);
				String s=null;
				final String fileName = node.getFileName() + (node.isDirectory()?"/":"");
				final int startIndex=node.isDirectory()?0:lines.size()-1;
				for(int i=startIndex;i<lines.size()&& (i>=0);i=i+(node.isDirectory()?1:-1))
				{
					s=lines.get(i);
					if(s.startsWith("*"))
					{
					}
					else
					if(s.trim().length()==0)
					{

					}
					else
					if((s.length()>0)&&(Character.isWhitespace(s.charAt(0)))) // is a comment
					{
					}
					else
					{
						s=s.trim();
						if(s.equals(fileName))
						{
							lines.remove(i);
							while((i<lines.size())
							&& (lines.get(i).trim().length()>0)
							&& (Character.isWhitespace(lines.get(i).charAt(0))))
							{
								description=(description==null)?lines.get(i):(description+" "+lines.get(i).trim());
								lines.remove(i);
							}
							return description;
						}
					}
				}
			}
			finally
			{
				dlg.dispose();
			}
		}
		catch(final Exception e)
		{
		}
		return null;
	}

	private static List<String> readStringFile(final File F, final String[] cr) throws IOException
	{
		cr[0]="\r";
		final FileReader fr=new FileReader(F);
		int c=fr.read();
		while(c>=0)
		{
			if((c=='\r')||(c=='\n'))
			{
				cr[0]=""+((char)c);
				c=fr.read();
				if(((c=='\r')||(c=='\n'))&&(c!=cr[0].charAt(0)))
					cr[0]+=((char)c);
				break;
			}
			c=fr.read();
		}
		fr.close();
		final BufferedReader br=new BufferedReader(new FileReader(F));
		String s=br.readLine();
		final Vector<String> lines=new Vector<String>();
		while(s!=null)
		{
			lines.addElement(s);
			s=br.readLine();
		}
		br.close();
		return lines;
	}

	private static boolean writeStringFile(final List<String> lines, final File F, final String[] cr) throws IOException
	{
		final FileOutputStream fw=new FileOutputStream(F);
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		for(final String l : lines)
			bytes.write((l+cr[0]).getBytes());
		fw.write(bytes.toByteArray());
		fw.close();
		return true;
	}

	private boolean backUp(final RemoteNode node, final boolean fromCopy)
	{
		final SourceTree srcTree = zimmerscp.INSTANCE.getSourceTree();
		File F=null;
		try
		{
			if(srcTree.getBackupDir().trim().length()==0)
				return true;
			File backupDirF = new File(srcTree.getBackupDir());
			if((!backupDirF.exists())||(!backupDirF.isDirectory()))
			{
				JOptionPane.showMessageDialog(f, "Backup directory is not valid.");
				return false;
			}
			if((node.getFileName().equalsIgnoreCase("00index"))
			&&(!srcTree.getBackupIndexes()))
				return true;
			if(fromCopy && (!srcTree.getBackupCopyOvers()))
				return true;
			final Vector<RemoteNode> treeUpPath = buildPathToRoot(node);
			for(int i=treeUpPath.size()-1;i>=1;i--)
			{
				final RemoteNode dir=treeUpPath.elementAt(i);
				backupDirF=new File(backupDirF,dir.getFileName());
				if(!backupDirF.exists())
					backupDirF.mkdir();
			}


			int num=0;
			F=null;
			while(F==null)
			{
				F=new File(backupDirF,node.getFileName()+"~"+num);
				if(F.exists())
				{
					F=null;
					num++;
				}
			}
			node.getConnection().getFile(node.getFullName(), F.getAbsolutePath());
			return true;
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to backup file to "+F.getAbsolutePath());
			return false;
		}
	}

	private boolean unsafeDeleteRemoteFile(final DestTree tree, final RemoteNode node)
	{
		final JDialog dlg = zimmerscp.showWorkingDialog(f);
		try
		{
			final RemoteNode parentNode= (RemoteNode)node.getParent();
			final String fullName = node.getFullName();
			node.getConnection().deleteFile(fullName, node.isDirectory());
			parentNode.removeAllChildren();
			parentNode.safeLoadKids();
			tree.updateUI();
			tree.repaint();
			return true;
		}
		catch(final Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(f, e.getMessage());
			return false;
		}
		finally
		{
			dlg.dispose();
		}
	}

	private String makeDescription(final DestTree tree, final RemoteNode destDirNode, final String fileName, String description)
	{
		if(!tree.getManageIndexes()) return (description==null)?"":description;
		if((description==null)&&(tree.getManageIndexes()))
			description = JOptionPane.showInputDialog(f,"Enter a description for "+destDirNode.getFileName());
		return description;
	}

	private boolean makeDirectoryRemote(final RemoteNode destNode, final String dirName, String description, final boolean neverEverSync)
	{
		description = makeDescription(this,destNode,dirName,description);
		if(description == null) return false;
		final JDialog dlg = zimmerscp.showWorkingDialog(f);
		try
		{
			if(!destNode.getConnection().makeDirectory(destNode.combine(destNode.getFullName(),dirName)))
			{
				if(JOptionPane.showConfirmDialog(f, "Error making remote dir "+destNode.getFullName()+". Continue?", "Create dir",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
			destNode.removeAllChildren();
			destNode.safeLoadKids();
			updateUI();
			repaint();
			final RemoteNode fileNode = destNode.findChildNode(dirName, true);
			if(fileNode == null)
			{
				JOptionPane.showMessageDialog(f, "Unable to find '"+dirName+"' after copy.");
				return false;
			}
			final RemoteNode zeroZeroIndexFile=this.getMy00INDEX(fileNode);
			if((zeroZeroIndexFile != null)&&(!addToBoth00INDEX(zeroZeroIndexFile, fileNode, description,true)))
				return false;
			if(getSync()&&(!neverEverSync))
			{
				dlg.dispose();
				for(final RemoteNode otherNode : findSiblings(destNode))
				{
					final DestTree otherTree = otherNode.getTree();
					if(!otherTree.makeDirectoryRemote(otherNode,dirName,description,true))
					{
						if(JOptionPane.showConfirmDialog(f, "Error making remote dir "+otherNode.getFullName()+". Continue?", "Create dir",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
							return false;
					}
				}
			}
			return true;
		}
		catch(final Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(f, e.getMessage());
			return false;
		}
		finally
		{
			dlg.dispose();
		}
	}

	private Vector<RemoteNode> buildPathToRoot(RemoteNode node)
	{
		final RemoteNode root=(RemoteNode)getModel().getRoot();
		final Vector<RemoteNode> path = new Vector<RemoteNode>();
		while(node != root)
		{
			path.add(node);
			node=(RemoteNode)node.getParent();
		}
		return path;
	}

	private final RemoteNode findSiblingNode(final DestTree otherTree, final RemoteNode node) throws Exception
	{
		final Vector<RemoteNode> path = buildPathToRoot(node);
		RemoteNode inode = (RemoteNode)otherTree.getModel().getRoot();
		for(int i=path.size()-1;i>=0;i--)
		{
			final RemoteNode n=path.elementAt(i);
			inode=inode.findChildNode(n.getFileName(), n.isDirectory());
			if(inode==null)
				throw new Exception("Path discovery error in #"+otherTree.num);
		}
		if(inode.getFileName().equals(node.getFileName()))
			return inode;
		throw new Exception("Path discovery error in #"+otherTree.num);
	}

	private final RemoteNode[] findSiblings(final RemoteNode node) throws Exception
	{
		final List<RemoteNode> otherNodes = new ArrayList<RemoteNode>();
		final Vector<RemoteNode> path = buildPathToRoot(node);
		final DestTree[] others = zimmerscp.INSTANCE.getOtherRemoteTree(this);
		for(final DestTree otherTree : others)
		{
			if(otherTree==null)
				throw new Exception("Tree discovery problem");
			RemoteNode inode = (RemoteNode)otherTree.getModel().getRoot();
			for(int i=path.size()-1;i>=0;i--)
			{
				final RemoteNode n=path.elementAt(i);
				inode=inode.findChildNode(n.getFileName(), n.isDirectory());
				if(inode==null)
					throw new Exception("Path discovery error in #"+otherTree.num);
			}
			if(inode.getFileName().equals(node.getFileName()))
				otherNodes.add(inode);
			else
				throw new Exception("Path discovery error in #"+otherTree.num);
		}
		if((otherNodes.size()==0)&&(others.length>0))
			throw new Exception("Tree sync discovery problem for "+node.getFileName());
		return otherNodes.toArray(new RemoteNode[otherNodes.size()]);
	}

	private final boolean makeDirRemote(final RemoteNode node)
	{
		if((node==null)||(!node.isDirectory())) return false;
		final String dirName = JOptionPane.showInputDialog(f,"Directory name");
		if(dirName==null) return true;
		if(!makeDirectoryRemote(node,dirName,null,false))
			return false;
		return true;
	}

	private final boolean renameRemote(final RemoteNode node)
	{
		if(node==null) return false;
		final String newName = JOptionPane.showInputDialog(f,"New name",node.getFileName());
		if((newName==null)||newName.equals(node.getFileName()))
			return true;
		if(!renameRemote(node,newName))
		{
			if(JOptionPane.showConfirmDialog(f, "Unable to rename "+node.getFullName()+".\nContinue to rename?","Unable to rename",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
		}
		if(getSync())
		{
			try
			{
				for(final RemoteNode siblingNode : findSiblings(node))
				{
					final DestTree otherTree = siblingNode.getTree();
					if(!otherTree.renameRemote(siblingNode, newName))
					{
						if(JOptionPane.showConfirmDialog(f, "Unable to rename "+siblingNode.getFullName()+".\nContinue to rename?","Unable to rename",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
							return false;
					}

				}
			}
			catch(final Exception e){
				JOptionPane.showMessageDialog(f, "Sync Error: "+e.getMessage());
				return false;
			}
		}
		return true;
	}

	private final boolean renameRemote(RemoteNode node, final String newName)
	{
		final RemoteNode myDir = (RemoteNode)node.getParent();
		final RemoteNode oldNode = node;
		if(myDir ==null)
		{
			JOptionPane.showMessageDialog(f, "Can't rename a parentless node: "+node.getFullName()+".");
			return false;
		}
		if(!newName.equalsIgnoreCase(node.getFileName()))
		{
			final RemoteNode possMatch = myDir.findChildNodeIgnoreCase(newName);
			if((possMatch!=null)&&(possMatch!=node))
			{
				JOptionPane.showMessageDialog(f, "Can't rename, file exists: "+possMatch.getFullName()+".");
				return false;
			}
		}
		RemoteNode zeroZeroIndexFile = null;
		if(getManageIndexes()&&(!node.getFileName().equalsIgnoreCase("00index")))
		{
			zeroZeroIndexFile = getMy00INDEX(node);
			if((zeroZeroIndexFile==null)
			&&(getManageIndexes())
			&&(JOptionPane.showConfirmDialog(f, "00INDEX file not found.\nContinue to rename?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
				return false;
		}
		try
		{
			conn.moveFile(node.getFullName(),myDir.combine(myDir.getFullName(),newName));
			myDir.removeAllChildren();
			myDir.loadKids();
			updateUI();
			repaint();
		}
		catch(final Exception e)
		{
			JOptionPane.showMessageDialog(f, "Rename failure: "+node.getFullName()+"\n"+e.getMessage());
			return false;
		}
		node=myDir.findChildNode(newName);
		if(node==null) return false;
		if(getManageIndexes() && (zeroZeroIndexFile!=null))
		{
			final String[] oldDesc = new String[1];
			zeroZeroIndexFile = getMy00INDEX(node);
			if(!removeFromBoth00INDEX(zeroZeroIndexFile, oldNode, oldDesc, true))
				return false;
			if(!addToBoth00INDEX(zeroZeroIndexFile, node, oldDesc[0], true))
				return false;
		}
		return true;
	}

	public void actionPerformed(final ActionEvent e)
	{
		if (e.getActionCommand().equalsIgnoreCase("Info"))
			new RemoteFileInfoDialog(f, (RemoteNode) getSelectedNode()).setVisible(true);
		else if (e.getActionCommand().equalsIgnoreCase("Delete"))
			deleteRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("View"))
			viewRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Edit"))
			editRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("MakeDir"))
			makeDirRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Rename"))
			renameRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("New 00INDEX"))
			new00INDEX((RemoteNode) getSelectedNode());
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
							if(tNode instanceof RemoteNode)
							{
								final RemoteNode fn = (RemoteNode)tNode;
								if(fn.isDirectory())
									transferLocalRemote(F, fn);
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
