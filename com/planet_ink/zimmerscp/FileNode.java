package com.planet_ink.zimmerscp;

import java.io.*;
import java.util.*;

public class FileNode extends ZCPNode<FileNode>
{
	private static final long serialVersionUID = -2716924200379456830L;
	private File f;
	private boolean checkedForKids = false;
	private SourceTree tree = null;

	public FileNode(SourceTree tree)
	{
		super();
		f = null;
		this.tree=tree;
	}

	public FileNode(SourceTree tree, File f)
	{
		this(tree);
		this.f = f;
	}
	
	public boolean renameTo(File newf)
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
	public FileNode setTree(SourceTree tree){ this.tree=tree; return this;}
	
	@SuppressWarnings("unchecked")
	public Enumeration children()
	{
		if (!checkedForKids)
			build(f);
		return super.children();
	}

	public boolean getAllowsChildren()
	{
		return (f == null) || (f.isDirectory());
	}

	public synchronized FileNode build(File f)
	{
		if ((checkedForKids) || (f == null))
			return this;
		this.f = f;
		checkedForKids = true;
		if ((f != null) && (f.isDirectory()))
		{
			File[] files = f.listFiles();
			for (int x = 0; x < files.length; x++)
				add(new FileNode(tree,files[x]));
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
		return f.getName();
	}

	public long getSize()
	{
		return f.length();
	}

	public long getTimestamp()
	{
		return f.lastModified();
	}

	public boolean isDirectory()
	{
		return f.isDirectory();
	}
}
