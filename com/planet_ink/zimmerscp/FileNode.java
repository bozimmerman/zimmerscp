package com.planet_ink.zimmerscp;

import java.io.*;
import java.util.*;

import javax.swing.tree.TreeNode;

public class FileNode extends ZCPNode<FileNode>
{
	private static final long	serialVersionUID	= -2716924200379456830L;
	private File				f;
	private boolean				checkedForKids		= false;
	private SourceTree			tree				= null;
	private volatile Boolean	isDirectory			= null;

	public FileNode(final SourceTree tree)
	{
		super();
		f = null;
		this.tree=tree;
	}

	public FileNode(final SourceTree tree, final File f)
	{
		this(tree);
		this.f = f;
		isDirectory = Boolean.valueOf(f.isDirectory());
	}

	public boolean renameTo(final File newf)
	{
		if(!f.renameTo(newf))
			return false;
		f=newf;
		return true;
	}

	public void removeAllChildren()
	{
		super.removeAllChildren();
		checkedForKids = false;
	}

	public int getChildCount()
	{
		if (!checkedForKids)
			build(f);
		return super.getChildCount();
	}

	public SourceTree getTree(){ return tree;}
	public FileNode setTree(final SourceTree tree){ this.tree=tree; return this;}

	@SuppressWarnings("unchecked")
	public Enumeration<TreeNode> children()
	{
		if (!checkedForKids)
			build(f);
		return super.children();
	}

	public boolean getAllowsChildren()
	{
		return (f == null) || (isDirectory());
	}

	public synchronized FileNode build(final File f)
	{
		if ((checkedForKids) || (f == null))
			return this;
		this.f = f;
		isDirectory = Boolean.valueOf(f.isDirectory());
		checkedForKids = true;
		if (isDirectory.booleanValue())
		{
			final File[] files = f.listFiles();
			for (int x = 0; x < files.length; x++)
				add(new FileNode(tree,files[x]));
			sort();
		}
		return this;
	}

	public File getFile()
	{
		return f;
	}

	public String toString()
	{
		return (f == null) ? "" : f.getName();
	}

	public Object getUserObject()
	{
		return this;
	}

	public String getFileName()
	{
		if(f == null)
			return "";
		return f.getName();
	}

	public long getSize()
	{
		if(f == null)
			return 0;
		return f.length();
	}

	public long getTimestamp()
	{
		if(f == null)
			return 0;
		return f.lastModified();
	}

	public boolean isDirectory()
	{
		if(f==null)
			return false;
		if(isDirectory == null)
			isDirectory = Boolean.valueOf(f.isDirectory());
		return isDirectory.booleanValue();
	}
}
