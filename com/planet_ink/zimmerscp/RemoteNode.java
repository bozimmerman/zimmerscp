package com.planet_ink.zimmerscp;

import java.util.*;

/**
 * 
 * @author Bo Zimmerman
 * 
 */
public class RemoteNode extends ZCPNode<RemoteNode>
{
	private static final long serialVersionUID = -932570022188085958L;
	private SCPConnection conn;
	private DestTree tree=null;
	private boolean checkedForKids = false;
	private String fileName = "";
	private String fullName = "";
	private short o = 0;
	private short g = 0;
	private short w = 0;
	private char fileType=' ';
	private long timestamp = 0;


	public RemoteNode(DestTree tree, SCPConnection conn)
	{
		this.conn = conn;
		this.tree = tree;
	}

	public DestTree getTree(){ return tree;}
	public RemoteNode setTree(DestTree tree){ this.tree=tree; return this;}
	
	private short parseAtt(String s, int index)
	{
		short att = 0;
		if (s.charAt(index) != '-')
			att |= 4;
		if (s.charAt(index + 1) != '-')
			att |= 2;
		if (s.charAt(index + 2) != '-')
			att |= 1;
		return att;
	}

	public void parseUnixAttributes(String s, int startIndex)
	{
		o = parseAtt(s, startIndex);
		g = parseAtt(s, startIndex+3);
		w = parseAtt(s, startIndex+6);
	}
	
	public void init(String parentDir, String fileName, long timestamp, char fileType)
	{
		this.fileType=fileType;
		if(fileType=='l')
		{
			int x=fileName.lastIndexOf(" -> ");
			if(x>0) fileName=fileName.substring(0,x).trim();
		}
		this.fileName = fileName;
		this.fullName = combine(parentDir,fileName);
		this.timestamp=timestamp;
	}

	public String getFileName()
	{
		return fileName;
	}

	public SCPConnection getConnection()
	{
		return conn;
	}

	public boolean isDirectory()
	{
		return fileType=='d';
	}

	public boolean isSoftlink()
	{
		return fileType=='l';
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public long getSize()
	{
		return size;
	}

	public String getOwner()
	{
		return owner;
	}

	public String getGroup()
	{
		return group;
	}

	public char separatorChar() { return '/';}
	
	public String combine(String path, String name)
	{
		if(path.trim().endsWith(String.valueOf(separatorChar())))
		{
			if(name.trim().startsWith(String.valueOf(separatorChar())))
				return path+name.trim().substring(1);
			else
				return path+name;
		}
		else
		if(name.trim().startsWith(String.valueOf(separatorChar())))
			return path+name;
		return path+separatorChar()+name;
	}

	private String typeDesc(short s)
	{
		StringBuffer str = new StringBuffer(3);
		if ((s & 4) == 4)
			str.append('r');
		else
			str.append('-');
		if ((s & 2) == 2)
			str.append('w');
		else
			str.append('-');
		if ((s & 1) == 1)
			str.append('x');
		else
			str.append('-');
		return str.toString();
	}

	public String getTypeDesc()
	{
		return typeDesc(o) + typeDesc(g) + typeDesc(w);
	}

	public long size = 0;
	public String owner;
	public String group;

	public String toString()
	{
		return fileName;
	}

	public Object getUserObject()
	{
		return this;
	}

	public void setConnection(SCPConnection conn)
	{
		this.conn = conn;
	}

	public void removeAllChildren()
	{
		super.removeAllChildren();
		checkedForKids = false;
	}

	public int getChildCount()
	{
		if (!checkedForKids)
			safeLoadKids();
		return super.getChildCount();
	}

	public String getFullName()
	{
		return conn != null ? fullName : null;
	}

	@SuppressWarnings("unchecked")
	public Enumeration children()
	{
		if (!checkedForKids)
			safeLoadKids();
		return super.children();
	}

	public boolean getAllowsChildren()
	{
		return (size == 0) || isDirectory();
	}

	public void safeLoadKids()
	{
		try
		{
			loadKids();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public synchronized void loadKids() throws Exception
	{
		if ((checkedForKids) || (conn == null))
			return;
		checkedForKids = true;
		if (!isDirectory())
			return;
		Vector<RemoteNode> V = conn.getDirectory(tree,fullName + (fullName.endsWith("/")?"/":""));
		if (V != null)
			for (RemoteNode n : V)
				add(n);
	}

}
