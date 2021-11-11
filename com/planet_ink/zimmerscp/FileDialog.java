package com.planet_ink.zimmerscp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

public class FileDialog extends JDialog implements KeyListener, WindowListener
{
	private static final long	serialVersionUID	= 4616634940742071923L;
	private static final int	DIALOG_WIDTH		= 600;
	private static final int	DIALOG_HEIGHT		= 600;
	private final JTextField	filenameField		= new JTextField();
	private JTextArea			textField			= new JTextArea();
	private EDIT_TYPE			edit;
	private static final String	ZEROES				= "000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	private static final int	ADDRESS_SIZE		= 10;
	private boolean				dirty				= false;
	private final Frame			f;
	private final File			F;
	private String				lastSearch			= "";
	private String				cr					= "\n";

	public FileDialog(final Frame f, final String filename, final String preFormattedText, final EDIT_TYPE edit)
	{
		super(f, (edit != EDIT_TYPE.READ_ONLY)?"ZimmerSCP Editor":"ZimmerSCP Viewer");
		this.f=f;
		this.F=null;
		this.addWindowListener(this);
		init(filename, preFormattedText, edit);
	}

	public FileDialog(final Frame f, final String filename, final File F, final boolean edit)
	{
		super(f, (edit)?"ZimmerSCP Editor":"ZimmerSCP Viewer");
		this.f=f;
		this.F=F;
		this.addWindowListener(this);
		final boolean isTextFile = isTextFile(F);
		EDIT_TYPE newEdit = EDIT_TYPE.READ_ONLY;
		if (edit)
			newEdit = isTextFile ? EDIT_TYPE.EDIT_TEXT : EDIT_TYPE.EDIT_BINARY;
		final String preFormattedText = getStringifiedDataFromFile(F);
		init(filename, preFormattedText, newEdit);
	}

	private void init(final String filename, final String preFormattedText, final EDIT_TYPE edit)
	{
		this.edit = edit;
		this.setModal(true);
		setLocationRelativeTo(this); // center on screen
		final boolean editFlag = edit != EDIT_TYPE.READ_ONLY;
		getContentPane().setLayout(new BorderLayout());
		if(editFlag)
		{
			filenameField.setText(filename);
			filenameField.setBackground(Color.yellow);
		}
		else
			filenameField.setText(filename);
		filenameField.setEditable(false);
		filenameField.setPreferredSize(new java.awt.Dimension(DIALOG_WIDTH, 21));
		filenameField.addKeyListener(this);
		getContentPane().add(filenameField, BorderLayout.NORTH);
		if(edit==EDIT_TYPE.EDIT_BINARY)
			textField = new JBinaryTextArea();
		textField.setEditable(editFlag);
		textField.setText(preFormattedText);
		textField.addKeyListener(this);
		textField.setFont(new Font("Monospaced", Font.PLAIN, 14));
		final JScrollPane jScrollpaneS = new JScrollPane(textField);
		jScrollpaneS.setPreferredSize(new java.awt.Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		jScrollpaneS.addKeyListener(this);
		getContentPane().add(jScrollpaneS, BorderLayout.CENTER);
		this.addKeyListener(this);
		this.pack();
	}

	@Override
	public void keyPressed(final KeyEvent e)
	{
		if(e.isControlDown() && ((e.getKeyCode() == KeyEvent.VK_F)||(e.getKeyCode() == KeyEvent.VK_N)))
		{
			if((e.getKeyCode() == KeyEvent.VK_F)||(lastSearch==null)||(lastSearch.trim().length()==0))
				lastSearch = JOptionPane.showInputDialog("Enter a search string");
			if((lastSearch!=null)&&(lastSearch.trim().length()>0))
			{
				int caretPosition=textField.getCaretPosition();
				if(caretPosition < 0) caretPosition = 0;
				int x=textField.getText().indexOf(lastSearch,caretPosition);
				if((x<0)&&(caretPosition>0))
					x=textField.getText().indexOf(lastSearch);
				if(x>=0)
					textField.setCaretPosition(x+1);
			}
			return;
		}

		if (edit == EDIT_TYPE.READ_ONLY)
		{
			if (e.getKeyCode() ==  KeyEvent.VK_ESCAPE)
				this.dispose();
		}
		else
		if (e.getKeyCode() ==  KeyEvent.VK_ESCAPE)
			closing();
		else
		if (edit == EDIT_TYPE.EDIT_BINARY)
		{
			if((!e.isActionKey())&&(e.getKeyCode()!=KeyEvent.VK_SHIFT)&&(e.getKeyCode()!=KeyEvent.VK_ALT)&&(e.getKeyCode()!=KeyEvent.VK_CONTROL))
			{
				filenameField.setBackground(Color.pink);
				dirty = true;
			}
		}
		else
		if (edit == EDIT_TYPE.EDIT_TEXT)
		{
			if((!e.isActionKey())&&(!e.isConsumed())&&(e.getKeyCode()!=KeyEvent.VK_SHIFT)&&(e.getKeyCode()!=KeyEvent.VK_ALT)&&(e.getKeyCode()!=KeyEvent.VK_CONTROL))
			{
				filenameField.setBackground(Color.pink);
				dirty = true;
			}
		}
	}

	@Override
	public void keyReleased(final KeyEvent e)
	{
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
		if (edit == EDIT_TYPE.EDIT_BINARY)
		{
		}
	}

	public String getText()
	{
		return textField.getText();
	}

	public boolean isDirty() { return dirty;}

	private static int countHexes(final String lineWithoutAddress)
	{
		int ct = 0;
		while((lineWithoutAddress.length()-(ct*3)-1)>ct)
			ct++;
		return ct;
	}

	private static boolean isTextFile(final File F)
	{
		try
		{
			final FileInputStream fio = new FileInputStream(F);
			long length = F.length();
			if (length > 2048)
				length = 2048;
			int c = fio.read();
			while ((c > 0) && ((length--) > 0))
				c = fio.read();
			fio.close();
			if (c == 0)
				return false;
			return true;
		}
		catch (final Exception e)
		{
			return true;
		}
	}

	private static String padWithZeros(final String s, final int padSize)
	{
		if (s.length() >= padSize)
			return s.substring(s.length() - 2);
		final int numZeroes = padSize - s.length();
		return ZEROES.substring(0, numZeroes) + s;
	}

	protected String getStringifiedDataFromFile(final File F)
	{
		final StringBuffer buf = new StringBuffer("");
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(F));
			if (isTextFile(F))
			{
				br.close();
				cr="\n";
				final FileReader fr=new FileReader(F);
				int c=fr.read();
				while(c>=0)
				{
					if((c=='\r')||(c=='\n'))
					{
						cr=""+((char)c);
						c=fr.read();
						if(((c=='\r')||(c=='\n'))&&(c!=cr.charAt(0)))
							cr+=((char)c);
						break;
					}
					c=fr.read();
				}
				fr.close();
				br = new BufferedReader(new FileReader(F));
				String s = br.readLine();
				while (s != null)
				{
					buf.append(s + "\n");
					s = br.readLine();
				}
				br.close();
			}
			else
			{
				final FileInputStream fio = new FileInputStream(F);
				int c = fio.read();
				long length = F.length();
				long ctr = 0;
				long addr = 0;
				final StringBuffer text = new StringBuffer("");
				while ((c >= 0) && ((length--) > 0))
				{
					if (ctr == 0)
					{
						if(addr>0)
							buf.append(text.toString() + "\n");
						buf.append(padWithZeros(Long.toHexString(addr), ADDRESS_SIZE) + " ");
						text.setLength(0);
						ctr = 8;
						addr += 8;
					}
					long l = ((long) c) & 0xff;
					buf.append(padWithZeros(Integer.toHexString((int) l), 2) + " ");
					l = l & 0x7f;
					if ((l < 32) || (l > 126))
						text.append('.');
					else
						text.append((char) l);
					ctr--;
					c = fio.read();
				}
				buf.append(text.toString() + "\n");
				fio.close();
			}
		}
		catch (final Exception e)
		{
			return "File read error: " + e.getMessage() + "\n";
		}
		return buf.toString();
	}

	public byte[] getBytes()
	{
		if (edit != EDIT_TYPE.EDIT_BINARY)
			return textField.getText().getBytes();
		else
		{
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try
			{
				final BufferedReader br=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(textField.getText().getBytes())));
				String line = br.readLine();
				while(line!=null)
				{
					line=line.substring(ADDRESS_SIZE);
					if(line.length()>1)
					{
						final int numHexes = countHexes(line);
						for(int i=0;i<numHexes;i++)
						{
							final String hex=line.substring((i*3),(i*3)+3).trim();
							bout.write(Integer.parseInt(hex, 16));
						}
					}
					line = br.readLine();
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			return bout.toByteArray();
		}
	}

	public static enum EDIT_TYPE
	{
		READ_ONLY, EDIT_TEXT, EDIT_BINARY
	}

	private static class JBinaryTextArea extends JTextArea
	{
		private static final long serialVersionUID = -5734252932744064911L;

		public void processKeyEvent(final KeyEvent e)
		{
			if (e.getKeyCode() ==  KeyEvent.VK_ENTER)
				return;
			else
			if((!e.isActionKey())&&(e.getKeyCode()!=KeyEvent.VK_SHIFT)&&(e.getKeyCode()!=KeyEvent.VK_ALT)&&(e.getKeyCode()!=KeyEvent.VK_CONTROL))
			{
				int relpos=0;
				int selection=getSelectionStart();
				if(getSelectionEnd()>selection+1)
					return;
				final String s = getText();
				while((selection>=0)&&(s.charAt(selection)!='\n'))
				{
					selection--;
					relpos++;
				}
				relpos--;
				selection++;
				if(relpos<=ADDRESS_SIZE)
					return;
				int endPos=selection;
				while((endPos<s.length())&&(s.charAt(endPos)!='\n'))
					endPos++;
				final String line=s.substring(selection,endPos);
				final int numCurrHexes = countHexes(line.substring(ADDRESS_SIZE));
				final int charDivider = ADDRESS_SIZE + (numCurrHexes*3);
				if(relpos > charDivider)
				{
					return;
					/*
					if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE)
					{

					}
					else
					if(e.getKeyCode()==KeyEvent.VK_DELETE)
					{

					}
					else
					{

					}
					super.processKeyEvent(e);
					*/
				}
				else
				{
					if(e.getKeyCode()==KeyEvent.VK_ESCAPE)
					{
					}
					else
					if(e.getKeyCode()==KeyEvent.VK_BACK_SPACE)
					{
					}
					else
					if(e.getKeyCode()==KeyEvent.VK_DELETE)
					{
					}
					else
					if("0123456789abcdefABCDEF".indexOf(e.getKeyChar())<0)
						return;
					else
					if(e.getID()==KeyEvent.KEY_PRESSED)
					{
						if((relpos%3)==1)
						{
							relpos++;
							this.setSelectionStart(this.getSelectionStart()+1);
							this.setSelectionEnd(this.getSelectionEnd()+1);
							selection++;
						}
						final KeyEvent k=new KeyEvent(this, KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0, KeyEvent.VK_DELETE,(char)127);
						super.processKeyEvent(k);
					}
					super.processKeyEvent(e);
				}
			}
			else
				super.processKeyEvent(e);
		}
	}

	public void closing()
	{
		if((!dirty)||(edit==EDIT_TYPE.READ_ONLY))
		{
			dirty=false;
			this.dispose();
			return;
		}
		if(JOptionPane.showConfirmDialog(f, "Save your changes?","File changed",JOptionPane.YES_NO_OPTION)==JOptionPane.NO_OPTION)
		{
			dirty=false;
			this.dispose();
			return;
		}
		if (edit == EDIT_TYPE.EDIT_BINARY)
		{
			try
			{
				final byte[] bout = getBytes();
				final FileOutputStream fo = new FileOutputStream(F);
				fo.write(bout);
				fo.close();
			}
			catch(final Exception ex)
			{
				JOptionPane.showMessageDialog(f,"Unable to save "+F.getAbsolutePath()+"\n"+ex.getMessage());
				dirty=false; // prevent any uploads
			}
		}
		else
		{
			try
			{
				final BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(textField.getText().getBytes())));
				final StringBuffer buf=new StringBuffer("");
				String s = br.readLine();
				while (s != null)
				{
					buf.append(s + cr);
					s = br.readLine();
				}
				br.close();
				final FileOutputStream fo = new FileOutputStream(F);
				fo.write(buf.toString().getBytes());
				fo.close();
			}
			catch(final Exception ex)
			{
				JOptionPane.showMessageDialog(f,"Unable to save "+F.getAbsolutePath()+"\n"+ex.getMessage());
				dirty=false; // prevent any uploads
			}
		}
		this.dispose();
	}


	public void windowActivated(final WindowEvent arg0)
	{
	}

	public void windowClosing(final WindowEvent arg0)
	{
		closing();
	}

	public void windowDeactivated(final WindowEvent arg0)
	{
	}

	public void windowDeiconified(final WindowEvent arg0)
	{
	}

	public void windowIconified(final WindowEvent arg0)
	{
	}

	public void windowOpened(final WindowEvent arg0)
	{
	}

	public void windowClosed(final WindowEvent arg0)
	{
	}
}
