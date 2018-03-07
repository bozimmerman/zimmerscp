package com.planet_ink.zimmerscp;

import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
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
import javax.swing.tree.TreeNode;

import com.jcraft.jsch.JSchException;

public class DestTree extends DragDropTree
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -875565996009941468L;
	private RemoteDialog dialog = null;
	private SCPConnection conn = null;
	private int num = 0;
	private PopupMenu popupMenu = null;
	private Frame f = null;

	public DestTree(String name, Frame f, RemoteNode m, int num)
	{
		super(name, f, m);
		this.f=f;
		this.num = num;
		this.add(getContextMenu());
	}

	public PopupMenu getContextMenu()
	{
		TreeNode node = this.getSelectedNode();
		if (popupMenu == null)
		{
			popupMenu = new PopupMenu("Target Option");
			popupMenu.addActionListener(this);
		}
		popupMenu.removeAll();
		if (node instanceof RemoteNode)
		{
			RemoteNode rnode = (RemoteNode) node;
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

	public void affectSettings(Frame f, JLabel label)
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

	public void loadSettings(Frame f, JLabel label, Properties p)
	{
		String host = p.getProperty("remote" + num + ".host");
		String user = p.getProperty("remote" + num + ".user");
		String password = p.getProperty("remote" + num + ".password");
		String knownhosts = p.getProperty("remote" + num + ".knownhosts");
		String dir = p.getProperty("remote" + num + ".dir");
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
			dialog.fill(host, user, password, dir, Boolean.valueOf(mindex), Boolean.valueOf(msync), Boolean.valueOf(cr00index));
			if (conn != null)
				conn.close();
			conn = new SCPConnection(host, knownhosts, user, password);
			setDestination(f, dir, this, (RemoteNode) this.getModel().getRoot(), label);
		}
	}

	public void saveSettings(Properties p)
	{
		RemoteNode n = (RemoteNode) this.getModel().getRoot();
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

	public void setDestination(Frame f, String root, JTree tree, RemoteNode node, JLabel textField)
	{
		textField.setText("Remote #" + num + ": <Click Here>");
		JDialog dlg = zimmerscp.showWorkingDialog(f);
		tree.clearSelection();
		String host = conn.getHost();
		node.init("", root, System.currentTimeMillis(), 'd');
		node.removeAllChildren();
		try
		{
			textField.setText("Remote #" + num + ": " + host + ":" + root);
			node.setConnection(conn);
			node.loadKids();
		}
		catch (JSchException e)
		{
			dlg.dispose();
			JOptionPane.showMessageDialog(this, "Unable to connect to " + host);
		}
		catch (Exception e)
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
	
	@SuppressWarnings("unchecked")
	protected boolean TestNodeImport(TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof RemoteNode)
				{
					RemoteNode node = (RemoteNode) location.getPath().getLastPathComponent();
					if (node.isDirectory())
					{
						try
						{
							Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
							if (o instanceof FileNode)
								return true;
							else 
							if (o instanceof List)
							{
								@SuppressWarnings("rawtypes")
								List<File> fl = (List) o;
								for (File f : fl)
									if ((f == null) || (!f.exists()))
										return false;
								return fl.size() > 0;
							}
							if((o instanceof RemoteNode)
							&&(!((RemoteNode)o).isSoftlink())
							&&(node.getTree()==((RemoteNode)o).getTree()))
								return true;
						}
						catch (Exception e)
						{
						}
					}
				}
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean HandleNodeImport(TransferHandler.TransferSupport t)
	{
		if (t.getDropLocation() instanceof JTree.DropLocation)
		{
			JTree.DropLocation location = (JTree.DropLocation) t.getDropLocation();
			if (location.getPath() != null)
				if (location.getPath().getLastPathComponent() instanceof RemoteNode)
				{
					RemoteNode node = (RemoteNode) location.getPath().getLastPathComponent();
					try
					{
						Object o = t.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if(o instanceof RemoteNode)
							return SoftlinkRemote((RemoteNode)o, node);
						else
						{
							List<File> fl = new ArrayList<File>();
							if (o instanceof FileNode)
								fl.add(((FileNode) o).getFile());
							else if (o instanceof List)
								fl = (List) o;
							for (File f : fl)
								if ((f == null) || (!f.exists()))
									return false;
							if (fl.size() > 0)
								return TransferLocalRemote(fl, node);
						}
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
	
	private final boolean TransferLocalRemote(List<File> srcFs, RemoteNode dest)
	{
		Vector<File> files=new Vector<File>();
		for(File F : srcFs)
			if(F.getName().equalsIgnoreCase("00index"))
			{
				if(files.size()>0)
					files.insertElementAt(F, 0);
				else
					files.addElement(F);
			}
			else
				files.addElement(F);
		for(File srcF : files)
			if(!TransferLocalRemote(srcF,dest))
				return false;
		return true;
	}

	private final boolean TransferLocalRemote(File srcF, RemoteNode dest)
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
				if(!MakeDirectoryRemote(dest,srcF.getName(),null,false))
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
			File[] filelist = srcF.listFiles();
			Vector<File> files=new Vector<File>();
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
			for(File nxtF : files)
				if(!TransferLocalRemote(nxtF,nxtDir))
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
			JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				RemoteNode otherNode = null;
				if(getSync())
				{
					try
					{
						otherNode = FindSibling(existFile);
					}
					catch(Exception e){
						return false;
					}
				}
				boolean response=UnsafeDeleteRemoteFile(this,existFile);
				if(response && getSync()&&(otherNode != null))
				{
					response=UnsafeDeleteRemoteFile(zimmerscp.INSTANCE.getOtherRemoteTree(this), otherNode);
				}
				if(!response)
				{
					JOptionPane.showMessageDialog(f, "Unable to delete remote file "+existFile.getFullName());
					return false;
				}
			}
			finally
			{
				dlg.dispose();
			}
		}
		JDialog dlg = zimmerscp.showWorkingDialog(f);
		try {
			dest.getConnection().sendFile(srcF.getAbsolutePath(),dest.combine(dest.getFullName(),srcF.getName()));
			if(getSync())
			{
				RemoteNode otherDir = null;
				try
				{
					otherDir = FindSibling(dest);
					if(otherDir==null)
					{
						JOptionPane.showMessageDialog(f, "Unable to sync '"+srcF.getAbsolutePath()+" to remote dir "+dest.getFullName());
						return false;
					}
				}
				catch(Exception e){
					return false;
				}
				DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this); 
				otherDir.getConnection().sendFile(srcF.getAbsolutePath(),otherDir.combine(otherDir.getFullName(),srcF.getName()));
				otherDir.removeAllChildren();
				otherDir.safeLoadKids();
				otherTree.updateUI();
				otherTree.repaint();
			}
			dest.removeAllChildren();
			dest.safeLoadKids();
			updateUI();
			repaint();
			RemoteNode fileNode = dest.findChildNode(srcF.getName(), srcF.isDirectory());
			if(fileNode == null)
			{
				JOptionPane.showMessageDialog(f, "Unable to find '"+srcF.getName()+" after copy.");
				return false;
			}
			if((zeroZeroIndexFile!=null)&&(getManageIndexes())&&(!srcF.getName().equalsIgnoreCase("00index")))
			{
				zeroZeroIndexFile=this.getMy00INDEX(fileNode);
				if(!AddToBoth00INDEX(zeroZeroIndexFile, fileNode, null,false))
					return false;
			}
		}
		catch(Exception e)
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

	private boolean New00INDEX(RemoteNode node)
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
		File tempDirF=new File(System.getProperty("java.io.tmpdir"));
		if((!tempDirF.exists())||(!tempDirF.isDirectory()))
		{
			JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
			return false;
		}
		File F=new File(tempDirF,"00INDEX");
		if(F.exists())
			if(!F.delete())
			{
				JOptionPane.showMessageDialog(f, "Unable to delete "+F.getAbsolutePath()+".");
				return false;
			}
		try
		{
			FileOutputStream fo=new FileOutputStream(F);
			fo.write((char)10);
			fo.close();
			if(!TransferLocalRemote(F, node))
				return false;
			return true;
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to create 00INDEX file at "+node.getFullName()+".");
			return false;
		}
	}
	
	private File PrepareViewEditFile(RemoteNode node)
	{
		if(node.isDirectory())
		{
			JOptionPane.showMessageDialog(f, "Directories can not be edited like that.");
			return null;
		}
		File tempDirF=new File(System.getProperty("java.io.tmpdir"));
		if((!tempDirF.exists())||(!tempDirF.isDirectory()))
		{
			JOptionPane.showMessageDialog(f, "System temp directory is not valid.");
			return null;
		}
		File F=new File(tempDirF,node.getFileName());
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
		JDialog dlg = zimmerscp.showWorkingDialog(f);
		try {
			node.getConnection().getFile(node.getFullName(), F.getAbsolutePath());
		}
		catch(Exception e)
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
	
	private boolean ViewRemote(RemoteNode node)
	{
		File F=PrepareViewEditFile(node);
		if(F==null) return false;
		new FileDialog(f, node.getFullName(), F, false).setVisible(true);
		return true;
	}
	
	private boolean EditRemote(RemoteNode node)
	{
		File F=PrepareViewEditFile(node);
		if(F==null) return false;
		FileDialog dialog = new FileDialog(f,node.getFullName(),F, true);
		dialog.setVisible(true);
		if(dialog.isDirty())
		{
			JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				node.getConnection().deleteFile(node.getFullName(), false);
				node.getConnection().sendFile(F.getAbsolutePath(),node.getFullName());
				if(getSync())
				{
					try
					{
						RemoteNode otherNode = FindSibling(node);
						if(otherNode != null)
						{
							otherNode.getConnection().deleteFile(otherNode.getFullName(), false);
							otherNode.getConnection().sendFile(F.getAbsolutePath(),otherNode.getFullName());
						}
					}
					catch(Exception e){
						JOptionPane.showMessageDialog(f, "Sync Error editing remote file "+node.getFullName()+"\n"+e.getMessage());
						return false;
					}
				}
			}
			catch(Exception e)
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
	
	private final boolean DeleteRemote(RemoteNode node)
	{
		if(node == getModel().getRoot())
		{
			JOptionPane.showMessageDialog(f, "Root nodes can not be deleted.");
			return false;
		}
		RemoteNode otherNode = null;
		if(getSync())
			try
			{
				otherNode = FindSibling(node);
			}
			catch(Exception e)
			{
				if(JOptionPane.showConfirmDialog(f, "Sync node error: "+e.getMessage()+"\nContinue to delete?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
		if(JOptionPane.showConfirmDialog(f, "Delete file '"+node.getFileName()+"'","Delete Node",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return false;
		if(node.isDirectory())
		{
			if(JOptionPane.showConfirmDialog(f, "This will delete this directory and all children.  Confirm.","Delete Directory",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
				return false;
		}
		if(!BackUp(node,false))
			return false;
		 
		RemoteNode zeroZeroIndexFile = getMy00INDEX(node);
		if((zeroZeroIndexFile==null)
		&&(getManageIndexes())
		&&(!node.getFileName().equalsIgnoreCase("00index"))
		&&(JOptionPane.showConfirmDialog(f, "00INDEX file not found.\nContinue to delete?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
			return false;
		
		if((zeroZeroIndexFile!=null)&&(getManageIndexes())&&(!node.getFileName().equalsIgnoreCase("00index")))
		{
			if(!RemoveFromBoth00INDEX(zeroZeroIndexFile, node, null, false))
				return false;
		}
		boolean response=UnsafeDeleteRemoteFile(this,node);
		if(response && getSync() && (otherNode != null))
			response=UnsafeDeleteRemoteFile(zimmerscp.INSTANCE.getOtherRemoteTree(this), otherNode);
		return response;
	}
	
	private final boolean SoftlinkRemote(RemoteNode node, RemoteNode destDir)
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
		RemoteNode otherNode = null;
		if(getSync())
			try
			{
				otherNode = FindSibling(node);
			}
			catch(Exception e)
			{
				if(JOptionPane.showConfirmDialog(f, "Sync node error: "+e.getMessage()+"\nContinue to create link?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
		RemoteNode otherDestDir = null;
		if(getSync())
			try
			{
				otherDestDir = FindSibling(destDir);
			}
			catch(Exception e)
			{
				if(JOptionPane.showConfirmDialog(f, "Sync node error: "+e.getMessage()+"\nContinue to create link?","Sync Node not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
					return false;
			}
		if(JOptionPane.showConfirmDialog(f, "Softlink file '"+node.getFileName()+"' into directory '"+destDir.getFileName()+"'","Create Soft Link",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)
			return false;
		
		if(destDir.findChildNode(node.getFileName(), node.isDirectory())!=null)
		{
			JOptionPane.showMessageDialog(f, "That file seems to already exist in "+destDir.getFileName()+".");
			return false;
		}
		
		RemoteNode destZeroZeroIndexFile = getIn00INDEX(destDir);
		if((destZeroZeroIndexFile==null)
		&&(getManageIndexes())
		&&(!node.getFileName().equalsIgnoreCase("00index"))
		&&(JOptionPane.showConfirmDialog(f, "Destination 00INDEX file not found.\nContinue to create softlink?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
			return false;
		
		RemoteNode nodeZeroZeroIndexFile = getMy00INDEX(node);
		if((nodeZeroZeroIndexFile==null)
		&&(getManageIndexes())
		&&(!node.getFileName().equalsIgnoreCase("00index"))
		&&(JOptionPane.showConfirmDialog(f, "Source 00INDEX file not found.\nContinue to create softlink?","00INDEX not found",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))
			return false;
		
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
			description = Get00INDEXDescription(nodeZeroZeroIndexFile, node);
			if(description != null)
			{
				if(!AddToBoth00INDEX(destZeroZeroIndexFile, node, description, true))
					return false;
				if((otherDestZeroZeroIndexFile!=null)&&(otherNode != null))
				{
					DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this); 
					if(!otherTree.AddToBoth00INDEX(otherDestZeroZeroIndexFile, otherNode, description, true))
						return false;
				}
			}
		}
		
		// time to build the relative path
		Vector<RemoteNode> sourcePath = buildPathToRoot(node);
		Vector<RemoteNode> destPath = buildPathToRoot(destDir);
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
				DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this); 
				otherTree.conn.createSoftLink(finalSourcePath,  otherDestDir.getFullName()+otherDestDir.separatorChar()+otherNode.getFileName());
				otherDestDir.removeAllChildren();
				otherDestDir.safeLoadKids();
				otherTree.updateUI();
				otherTree.repaint();
			}
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to create softlink for "+node.getFullName()+".");
			return false;
		}
		return true;
	}
	
	private final RemoteNode getMy00INDEX(RemoteNode node)
	{
		return getIn00INDEX((RemoteNode)node.getParent());
	}
	
	private final RemoteNode getIn00INDEX(RemoteNode node)
	{
		RemoteNode my00index=node.findChildNodeIgnoreCase("00index", false);
		if(my00index!=null) return my00index;
		if(dialog.getCreate00INDEX())
			if(New00INDEX(node))
				return node.findChildNodeIgnoreCase("00index", false);
		return null;
	}
	
	private boolean RemoveFromBoth00INDEX(RemoteNode zeroZeroIndexFile, RemoteNode node, String[] oldDesc, boolean neverEverSync)
	{
		File F=null;
		try
		{
			File tempDirF=new File(System.getProperty("java.io.tmpdir"));
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
			JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				if(!BackUp(zeroZeroIndexFile,false))
					return false;
				zeroZeroIndexFile.getConnection().getFile(zeroZeroIndexFile.getFullName(), F.getAbsolutePath());
				String[] cr=new String[1];
				Vector<String> lines = ReadStringFile(F,cr);
				String s=null;
				boolean deedIsDone=false;
				String fileName = node.getFileName() + (node.isDirectory()?"/":"");
				boolean isLink = node.isSoftlink();
				for(int i=0;i<lines.size();i++)
				{
					s=lines.elementAt(i);
					if(s.startsWith("*")) 
						continue;
					if((s.trim().equalsIgnoreCase(fileName)
						||(isLink&&s.trim().equalsIgnoreCase(fileName+"/")))
					&&(!Character.isWhitespace(s.charAt(0))))
					{
						lines.remove(i);
						while((i<lines.size()) 
						&& (lines.elementAt(i).trim().length()>0) 
						&& (Character.isWhitespace(lines.elementAt(i).charAt(0))))
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
						&&(lines.elementAt(i-1).trim().length()==0)
						&&(lines.elementAt(i).trim().length()==0))
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
				WriteStringFile(lines,F,cr);
				zeroZeroIndexFile.getConnection().sendFile(F.getAbsolutePath(),zeroZeroIndexFile.getFullName());
			}
			finally
			{
				dlg.dispose();
			}
			if(getSync()&&(!neverEverSync))
			{
				DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this);
				RemoteNode otherNode = FindSibling(node);
				RemoteNode otherZeroZeroIndexFile = null;
				if((otherTree!=null)&&(otherNode!=null))
					otherZeroZeroIndexFile = otherTree.getMy00INDEX(otherNode);
				if(otherZeroZeroIndexFile!=null)
				{
					String[] otherOldDesc = ((oldDesc!=null)&&(oldDesc.length>0)&&(oldDesc[0].length()>0))?null:oldDesc;
					if(!otherTree.RemoveFromBoth00INDEX(otherZeroZeroIndexFile, otherNode, otherOldDesc, true))
						return false;
				}
			}
			return true;
		}
		catch(Exception e)
		{
			return JOptionPane.showConfirmDialog(f, "Unable to manage 00INDEX file for "+node.getFullName()+"\n"+e.getMessage()+"\nContinue with the operation?","Error",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
		}
	}

	private boolean AddToBoth00INDEX(RemoteNode zeroZeroIndexFile, RemoteNode node, String description, boolean neverEverSync)
	{
		File F=null;
		try
		{
			File tempDirF=new File(System.getProperty("java.io.tmpdir"));
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
			JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				if(!BackUp(zeroZeroIndexFile,false))
					return false;
				zeroZeroIndexFile.getConnection().getFile(zeroZeroIndexFile.getFullName(), F.getAbsolutePath());
				String[] cr=new String[1];
				Vector<String> lines = ReadStringFile(F,cr);
				String s=null;
				String fileName = node.getFileName() + (node.isDirectory()?"/":"");
				int placeToInsert=node.isDirectory()?0:lines.size()-1;
				boolean fileexists=false;
				for(int i=placeToInsert;i<lines.size()&& (i>=0);i=i+(node.isDirectory()?1:-1))
				{
					s=lines.elementAt(i);
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
						boolean fileIsDirectory = s.endsWith("/");
						if(s.equalsIgnoreCase(fileName))
						{
							fileexists=s.equals(fileName);
							lines.remove(i);
							while((i<lines.size()) 
							&& (lines.elementAt(i).trim().length()>0) 
							&& (Character.isWhitespace(lines.elementAt(i).charAt(0))))
							{
								description=(description==null)?lines.elementAt(i):(description+" "+lines.elementAt(i).trim());
								lines.remove(i);
							}
							if((i>0)
							&&(i<lines.size())
							&&(lines.elementAt(i-1).trim().length()==0)
							&&(lines.elementAt(i).trim().length()==0))
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
				description = MakeDescription(this,(RemoteNode)node.getParent(),node.getFileName(),description);
				if(description == null) return false;
				if(placeToInsert >= lines.size())
					lines.addElement("");
				lines.insertElementAt("", placeToInsert++);
				lines.insertElementAt(fileName, placeToInsert++);
				String d=description;
				while((d!=null)&&(d.length()>0))
				{
					if(d.length()<78)
					{
						lines.insertElementAt("  "+d.trim(), placeToInsert++);
						d=null;
					}
					else
					{
						int x=d.lastIndexOf(" ", 78);
						if(x<0) x=d.indexOf(" ");
						if(x<0) x=d.length();
						lines.insertElementAt("  "+d.substring(0,x).trim(), placeToInsert++);
						d=d.substring(x).trim();
					}
				}
				if(!fileexists)
				{
					zeroZeroIndexFile.getConnection().deleteFile(zeroZeroIndexFile.getFullName(), false);
					WriteStringFile(lines,F,cr);
					zeroZeroIndexFile.getConnection().sendFile(F.getAbsolutePath(),zeroZeroIndexFile.getFullName());
				}
			}
			finally
			{
				dlg.dispose();
			}
			if(getSync()&&(!neverEverSync))
			{
				DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this);
				RemoteNode otherNode = FindSibling(node);
				RemoteNode otherZeroZeroIndexFile = null;
				if((otherTree!=null)&&(otherNode!=null))
					otherZeroZeroIndexFile = otherTree.getMy00INDEX(otherNode);
				if(otherZeroZeroIndexFile!=null)
					if(!otherTree.AddToBoth00INDEX(otherZeroZeroIndexFile, otherNode, description, true))
						return false;
			}
			return true;
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to add "+node.getFullName()+"\n to "+zeroZeroIndexFile.getFullName()+"\n"+e.getMessage());
			return false;
		}
	}

	private String Get00INDEXDescription(RemoteNode zeroZeroIndexFile, RemoteNode node)
	{
		File F=null;
		String description = null;
		try
		{
			File tempDirF=new File(System.getProperty("java.io.tmpdir"));
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
			JDialog dlg = zimmerscp.showWorkingDialog(f);
			try
			{
				zeroZeroIndexFile.getConnection().getFile(zeroZeroIndexFile.getFullName(), F.getAbsolutePath());
				Vector<String> lines = ReadStringFile(F,new String[1]);
				String s=null;
				String fileName = node.getFileName() + (node.isDirectory()?"/":"");
				int startIndex=node.isDirectory()?0:lines.size()-1;
				for(int i=startIndex;i<lines.size()&& (i>=0);i=i+(node.isDirectory()?1:-1))
				{
					s=lines.elementAt(i);
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
							&& (lines.elementAt(i).trim().length()>0) 
							&& (Character.isWhitespace(lines.elementAt(i).charAt(0))))
							{
								description=(description==null)?lines.elementAt(i):(description+" "+lines.elementAt(i).trim());
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
		catch(Exception e)
		{
		}
		return null;
	}
	
	private Vector<String> ReadStringFile(File F, String[] cr) throws IOException
	{
		cr[0]="\r";
		FileReader fr=new FileReader(F);
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
		BufferedReader br=new BufferedReader(new FileReader(F));
		String s=br.readLine();
		Vector<String> lines=new Vector<String>();
		while(s!=null)
		{
			lines.addElement(s);
			s=br.readLine();
		}
		br.close();
		return lines;
	}
	
	private boolean WriteStringFile(Vector<String> lines, File F, String[] cr) throws IOException
	{
		FileOutputStream fw=new FileOutputStream(F);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		for(String l : lines)
			bytes.write(((String)(l+cr[0])).getBytes());
		fw.write(bytes.toByteArray());
		fw.close();
		return true;
	}
	
	private boolean BackUp(RemoteNode node, boolean fromCopy)
	{
		SourceTree srcTree = zimmerscp.INSTANCE.getSourceTree();
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
			Vector<RemoteNode> treeUpPath = buildPathToRoot(node);
			for(int i=treeUpPath.size()-1;i>=1;i--)
			{
				RemoteNode dir=treeUpPath.elementAt(i);
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
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, "Unable to backup file to "+F.getAbsolutePath());
			return false;
		}
	}
	
	private boolean UnsafeDeleteRemoteFile(DestTree tree, RemoteNode node)
	{
		JDialog dlg = zimmerscp.showWorkingDialog(f);
		try
		{
			RemoteNode parentNode= (RemoteNode)node.getParent();
			String fullName = node.getFullName();
			node.getConnection().deleteFile(fullName, node.isDirectory());
			parentNode.removeAllChildren();
			parentNode.safeLoadKids();
			tree.updateUI();
			tree.repaint();
			return true;
		}
		catch(Exception e)
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
	
	private String MakeDescription(DestTree tree, RemoteNode destDirNode, String fileName, String description)
	{
		if(!tree.getManageIndexes()) return (description==null)?"":description;
		if((description==null)&&(tree.getManageIndexes()))
			description = JOptionPane.showInputDialog(f,"Enter a description for "+destDirNode.getFileName());
		return description;
	}
	
	private boolean MakeDirectoryRemote(RemoteNode destNode, String dirName, String description, boolean neverEverSync)
	{
		description = MakeDescription(this,destNode,dirName,description);
		if(description == null) return false;
		JDialog dlg = zimmerscp.showWorkingDialog(f);
		try
		{
			destNode.getConnection().makeDirectory(destNode.combine(destNode.getFullName(),dirName));
			destNode.removeAllChildren();
			destNode.safeLoadKids();
			updateUI();
			repaint();
			RemoteNode fileNode = destNode.findChildNode(dirName, true);
			if(fileNode == null)
			{
				JOptionPane.showMessageDialog(f, "Unable to find '"+dirName+"' after copy.");
				return false;
			}
			RemoteNode zeroZeroIndexFile=this.getMy00INDEX(fileNode);
			if((zeroZeroIndexFile != null)&&(!AddToBoth00INDEX(zeroZeroIndexFile, fileNode, description,true)))
				return false;
			if(getSync()&&(!neverEverSync))
			{
				RemoteNode otherNode = FindSibling(destNode);
				if(otherNode == null)
				{
					JOptionPane.showMessageDialog(f, "Sync node sibling of "+destNode.getFullName()+" not found.");
					return false;
				}
				dlg.dispose();
				return zimmerscp.INSTANCE.getOtherRemoteTree(this).MakeDirectoryRemote(otherNode,dirName,description,true);
			}
			return true;
		}
		catch(Exception e)
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
		RemoteNode root=(RemoteNode)getModel().getRoot();
		Vector<RemoteNode> path = new Vector<RemoteNode>();
		while(node != root)
		{
			path.add(node);
			node=(RemoteNode)node.getParent();
		}
		return path;
	}
	
	private final RemoteNode FindSibling(RemoteNode node) throws Exception
	{
		Vector<RemoteNode> path = buildPathToRoot(node);
		DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this);
		if(otherTree==null) throw new Exception("Tree discovery problem");
		RemoteNode inode = (RemoteNode)otherTree.getModel().getRoot();
		for(int i=path.size()-1;i>=0;i--)
		{
			RemoteNode n=path.elementAt(i);
			inode=inode.findChildNode(n.getFileName(), n.isDirectory());
			if(inode==null) 
				throw new Exception("Path discovery error");
		}
		if(inode.getFileName().equals(node.getFileName()))
			return inode;
		throw new Exception("Path discovery error");
	}
	
	private final boolean MakeDirRemote(RemoteNode node)
	{
		if((node==null)||(!node.isDirectory())) return false;
		String dirName = JOptionPane.showInputDialog(f,"Directory name");
		if(dirName==null) return true;
		if(!MakeDirectoryRemote(node,dirName,null,false))
			return false;
		return true;
	}
	
	private final boolean RenameRemote(RemoteNode node)
	{
		if(node==null) return false;
		String newName = JOptionPane.showInputDialog(f,"New name");
		if((newName==null)||newName.equals(node.getFileName())) 
			return true;
		RemoteNode siblingNode = null;
		if(getSync())
		{
			try
			{
				siblingNode = FindSibling(node);
				if(siblingNode==null)
				{
					JOptionPane.showMessageDialog(f, "Unable to sync '"+node.getFullName());
					return false;
				}
			}
			catch(Exception e){
				JOptionPane.showMessageDialog(f, "Sync Error: "+e.getMessage());
				return false;
			}
		}
		if(!RenameRemote(node,newName))
			return false;
		if(getSync() && (siblingNode != null))
		{
			DestTree otherTree = zimmerscp.INSTANCE.getOtherRemoteTree(this);
			return otherTree.RenameRemote(siblingNode,newName);
		}
		return true;
	}
	
	private final boolean RenameRemote(RemoteNode node, String newName)
	{
		RemoteNode myDir = (RemoteNode)node.getParent();
		RemoteNode oldNode = node;
		if(myDir ==null)
		{
			JOptionPane.showMessageDialog(f, "Can't rename a parentless node: "+node.getFullName()+".");
			return false;
		}
		if(!newName.equalsIgnoreCase(node.getFileName()))
		{
			RemoteNode possMatch = myDir.findChildNodeIgnoreCase(newName); 
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
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(f, "Rename failure: "+node.getFullName()+"\n"+e.getMessage());
			return false;
		}
		node=myDir.findChildNode(newName);
		if(node==null) return false;
		if(getManageIndexes() && (zeroZeroIndexFile!=null))
		{
			String[] oldDesc = new String[1];
			zeroZeroIndexFile = getMy00INDEX(node);
			if(!RemoveFromBoth00INDEX(zeroZeroIndexFile, oldNode, oldDesc, true))
				return false;
			if(!AddToBoth00INDEX(zeroZeroIndexFile, node, oldDesc[0], true))
				return false;
		}
		return true;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equalsIgnoreCase("Info"))
			new RemoteFileInfoDialog(f, (RemoteNode) getSelectedNode()).setVisible(true);
		else if (e.getActionCommand().equalsIgnoreCase("Delete"))
			DeleteRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("View"))
			ViewRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Edit"))
			EditRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("MakeDir"))
			MakeDirRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("Rename"))
			RenameRemote((RemoteNode) getSelectedNode());
		else if (e.getActionCommand().equalsIgnoreCase("New 00INDEX"))
			New00INDEX((RemoteNode) getSelectedNode());
	}
}
