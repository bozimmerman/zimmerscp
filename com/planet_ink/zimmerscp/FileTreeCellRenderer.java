package com.planet_ink.zimmerscp;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class FileTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = 1L;

	private final Map<String,ImageIcon> icons = Collections.synchronizedMap(new TreeMap<String,ImageIcon>());

	public FileTreeCellRenderer()
	{
	}

	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value,
		final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus)
	{
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if(value instanceof DefaultMutableTreeNode)
		{
			if(value instanceof ZCPNode)
			{
				@SuppressWarnings("rawtypes")
				final ZCPNode fileNode = (ZCPNode)value;
				final String iconName = fileNode.getIconName();
				ImageIcon icon = icons.get(iconName);
				if(icon == null)
				{
					try
					{
						icon = new ImageIcon(this.getClass().getResource("/icons/"+iconName));
						icons.put(iconName,icon);
					}
					catch(final Exception e)
					{
						return this;
					}
				}
				setIcon(icon);
			}
		}
		return this;
	}
}