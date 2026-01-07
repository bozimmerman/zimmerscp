package com.planet_ink.zimmerscp;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public abstract class ZCPNode<T extends ZCPNode<T>> extends DefaultMutableTreeNode implements Transferable, Comparable<ZCPNode<T>>
{
	private static final long serialVersionUID = 1373888130366843583L;
	private boolean markedChanged = false;

	@SuppressWarnings("unchecked")
	public T findChildNode(final String name, final boolean nameIsDirectory)
	{
		for(final Enumeration<TreeNode> e=children(); e.hasMoreElements();)
		{
			final T fn = (T)e.nextElement();
			if((fn.isDirectory()== nameIsDirectory)
			&& name.equals(fn.getFileName()))
				return fn;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public T findChildNodeIgnoreCase(final String name)
	{
		for(final Enumeration<TreeNode> e=children(); e.hasMoreElements();)
		{
			final T fn = (T)e.nextElement();
			if(name.equalsIgnoreCase(fn.getFileName()))
				return fn;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public T findChildNode(final String name)
	{
		for(final Enumeration<TreeNode> e=children(); e.hasMoreElements();)
		{
			final T fn = (T)e.nextElement();
			if(name.equals(fn.getFileName()))
				return fn;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public T findChildNodeIgnoreCase(final String name, final boolean nameIsDirectory)
	{
		for(final Enumeration<TreeNode> e=children(); e.hasMoreElements();)
		{
			final T fn = (T)e.nextElement();
			if((fn.isDirectory()== nameIsDirectory)
			&&(name.equalsIgnoreCase(fn.getFileName())))
				return fn;
		}
		return null;
	}

	public void add(final MutableTreeNode node)
	{
		synchronized(this)
		{
			super.add(node);
		}
	}

	@SuppressWarnings("unchecked")
	public void sort()
	{
		if(children != null)
		{
			@SuppressWarnings("rawtypes")
			final
			List list = super.children;
			Collections.sort(list);
		}
	}

	public abstract String getFileName();
	public abstract boolean isDirectory();
	public abstract long getTimestamp();
	public abstract long getSize();

	public void setMarked(final boolean tf)
	{
		this.markedChanged = tf;
	}

	public boolean isMarked()
	{
		return this.markedChanged;
	}

	public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException
	{
		return this;
	}

	public DataFlavor[] getTransferDataFlavors()
	{
		return new DataFlavor[] { DataFlavor.javaFileListFlavor };
	}

	public boolean isDataFlavorSupported(final DataFlavor flavor)
	{
		return DataFlavor.javaFileListFlavor == flavor;
	}

	public int compareTo(final ZCPNode<T> arg0)
	{
		if(isDirectory()!=arg0.isDirectory())
			return isDirectory()?-1:1;
		return toString().compareToIgnoreCase(arg0.toString());
	}

	private static final Set<String> TEXT_EXTENSIONS = new HashSet<String>(Arrays.asList(
		"txt", "java", "c", "cpp", "h", "hpp", "cs", "py", "rb", "php", "js", "ts",
		"html", "htm", "xml", "json", "css", "scss", "less", "sql", "sh", "bash",
		"bat", "cmd", "ps1", "pl", "lua", "r", "m", "swift", "go", "rs", "kt",
		"scala", "clj", "md", "rst", "tex", "yaml", "yml", "toml", "ini", "conf",
		"cfg", "properties", "log", "csv", "tsv", "diff", "patch", "gradle", "maven"
	));

	public String getIconName()
	{
		final String star = markedChanged ? "star" : "";
		if(this.isDirectory())
			return star+"diricon.png";
		final String filename = getFileName();
		if((filename == null)||(filename.length()==0))
			return star+"fileicon.png";
		if(filename.toUpperCase().endsWith("00INDEX"))
			return star+"indexicon.png";
		final int dotIndex = filename.lastIndexOf('.');
		if(dotIndex < 0 || dotIndex == filename.length() - 1)
			return star+"fileicon.png";
		final String ext = filename.substring(dotIndex + 1).toLowerCase();
		if(TEXT_EXTENSIONS.contains(ext))
			return star+"fileicon.png";
		return star+"binicon.png";
	}

}
