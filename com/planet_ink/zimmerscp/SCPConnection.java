package com.planet_ink.zimmerscp;

import java.io.*;
import java.util.Vector;

import com.jcraft.jsch.*;

public class SCPConnection
{
	private String host;
	private String user;
	private String password;
	public String knownHostsFile;
	private Session _session = null;
	private String lastReceivedFile = null;
	private java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

	public String getHost()
	{
		return host;
	}

	public String getUser()
	{
		return user;
	}

	public String getPassword()
	{
		return password;
	}

	public String getKnownHostsFile()
	{
		return knownHostsFile;
	}

	public SCPConnection(String myHost, String myKnownHostsFile, String myUser, String myPassword)
	{
		host = myHost;
		user = myUser;
		password = myPassword;
		knownHostsFile = myKnownHostsFile;
	}

	private boolean connect() throws JSchException
	{
		if (_session == null)
		{
			JSch jsch = new JSch();
			// jsch.setKnownHosts("c:\\hosts");
			_session = jsch.getSession(user, host, 22);

			// username and password will be given via UserInfo interface.
			UserInfo ui = new MyUserInfo(password);
			_session.setUserInfo(ui);
			_session.connect();
		}
		return true;
	}

	public String getLastReceivedFile()
	{
		return lastReceivedFile;
	}

	/**
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private int checkAck(InputStream in) throws IOException
	{
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2)
		{
			StringBuffer sb = new StringBuffer();
			int c;
			do
			{
				c = in.read();
				sb.append((char) c);
			}
			while (c != '\n');
			if (b == 1)
			{ // error
				System.out.print(sb.toString());
			}
			if (b == 2)
			{ // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

	/**
	 * 
	 * @param sourceFilename
	 * @param destFilename
	 * @return
	 */
	public boolean sendFile(String sourceFilename, String destFilename) throws Exception
	{
		FileInputStream fis = null;
		try
		{
			connect();
			String command = "scp -p -t '" + destFilename + "'";
			Channel channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0)
			{
				return false;
			}

			// send "C0644 filesize filename" where filename doesn't contain a /
			long filesize = (new File(sourceFilename)).length();
			command = "C0644 " + filesize + " ";
			if (sourceFilename.lastIndexOf('/') > 0)
			{
				command += sourceFilename.substring(sourceFilename.lastIndexOf('/') + 1);
			}
			else
			{
				command += sourceFilename;
			}
			command += "\n";

			out.write(command.getBytes());
			out.flush();

			if (checkAck(in) != 0)
			{
				return false;
			}

			// send the contents of the source file
			fis = new FileInputStream(sourceFilename);
			byte[] buf = new byte[1024];
			while (true)
			{
				int len = fis.read(buf, 0, buf.length);

				if (len <= 0)
				{
					break;
				}

				out.write(buf, 0, len);
			}

			fis.close();
			fis = null;

			// send '\0' to end it
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			if (checkAck(in) != 0)
			{
				return false;
			}

			out.close();

			channel.disconnect();
			return true;
		}
		finally
		{
			try
			{
				if (fis != null)
				{
					fis.close();
				}
			}
			catch (Exception ee)
			{
				ee.printStackTrace();
			}
		}
	}


	/**
	 * 
	 * @param filename
	 * @return
	 */
	public boolean deleteFile(String filename, boolean recursive) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			String command = "rm -"+(recursive?"rf":"f")+" '"+ filename + "'";
			channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();
			InputStream in = channel.getInputStream();
			try
			{
				if (checkAck(in) != 0)
					return false;
			}
			finally
			{
				in.close();
			}
			return true;
		}
		finally
		{
			if(channel!=null)
				channel.disconnect();
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public boolean moveFile(String oldFilename, String newFilename) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			String command = "mv '"+oldFilename+"' '"+ newFilename + "'";
			channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();
			InputStream in = channel.getInputStream();
			try
			{
				if (checkAck(in) != 0)
					return false;
			}
			finally
			{
				in.close();
			}
			return true;
		}
		finally
		{
			if(channel!=null)
				channel.disconnect();
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public boolean createSoftLink(String filePath, String newLinkPath) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			String command = "ln -s '"+filePath+"' '"+ newLinkPath + "'";
			channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();
			InputStream in = channel.getInputStream();
			try
			{
				if (checkAck(in) != 0)
					return false;
			}
			finally
			{
				in.close();
			}
			return true;
		}
		finally
		{
			if(channel!=null)
				channel.disconnect();
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	public boolean makeDirectory(String filename) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			String command = "mkdir '"+ filename + "'";
			channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);
			channel.connect();
			InputStream in = channel.getInputStream();
			try
			{
				if (checkAck(in) != 0)
					return false;
			}
			finally
			{
				in.close();
			}
			return true;
		}
		finally
		{
			if(channel!=null)
				channel.disconnect();
		}
	}
	
	public void close()
	{
		_session.disconnect();
		_session=null;
	}
	
	public Channel OpenSession(String exec)
	{
		try
		{
			return _session.openChannel(exec);
		}
		catch(JSchException je)
		{
			try
			{
				close();
				connect();
				return OpenSession(exec);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * 
	 * @param remoteDirectory
	 * @return
	 */
	public Vector<RemoteNode> getDirectory(DestTree tree, String remoteDirectory) throws Exception
	{
		try
		{
			connect();
			remoteDirectory = remoteDirectory.trim();
			if (remoteDirectory.endsWith("/"))
				remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
			if (remoteDirectory.endsWith("\\"))
				remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
			String command = "ls -lAU --time-style=long-iso '" + remoteDirectory + "'";
			if(!remoteDirectory.endsWith("/"))
				remoteDirectory += "/";
			Channel channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);

			InputStream in = channel.getInputStream();

			channel.connect();

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int x = 1;
			while (x > 0)
			{
				x = in.read(buf, 0, 1024);
				if (x > 0)
					bout.write(buf, 0, x);
			}
			in.close();
			channel.disconnect();
			BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray())));
			String s = br.readLine();
			Vector<RemoteNode> nodes = new Vector<RemoteNode>();
			if(s!=null)
			{
				if (s.length() == 0)
					s = br.readLine();
				if (!s.startsWith("total"))
					throw new IOException("Unexpected second line: " + s);
				s = br.readLine();
				int y = 0;
				while ((s != null) && (s.trim().length() > 0))
				{
					RemoteNode node = new RemoteNode(tree,this);
					if (s.indexOf(' ') != 10)
						throw new IOException("Unexpected no att: " + s);
					node.parseUnixAttributes(s, 1);
					x = 10;
					while (s.charAt(x + 1) == ' ')
						x++;
					x = s.indexOf(' ', x + 1); // get past the funny number
					while (s.charAt(x + 1) == ' ')
						x++;
					y = s.indexOf(' ', x + 1);
					node.owner = s.substring(x + 1, y);
					while (s.charAt(y + 1) == ' ')
						y++;
					x = s.indexOf(' ', y + 1);
					node.group = s.substring(y + 1, x);
					while (s.charAt(x + 1) == ' ')
						x++;
					y = s.indexOf(' ', x + 1);
					try
					{
						node.size = Long.parseLong(s.substring(x + 1, y));
					}
					catch (Exception e)
					{
						s = br.readLine();
						continue;
					}
					while (s.charAt(y + 1) == ' ')
						y++;
					x = s.indexOf(' ', y + 1);
					while (s.charAt(x + 1) == ' ')
						x++;
					x = s.indexOf(' ', x + 1); // get the time too
					node.init(remoteDirectory, s.substring(x + 1), dateFormat.parse(s.substring(y + 1, x)).getTime(), s.charAt(0));
					nodes.add(node);
					s = br.readLine();
				}
			}
			nodes.trimToSize();
			return nodes;
		}
		finally
		{
		}
	}

	/**
	 * 
	 * @param sourceFilename
	 * @param destFilename
	 * @return
	 */
	public boolean getFile(String sourceFilename, String destFilename) throws Exception
	{
		FileOutputStream fos = null;
		try
		{
			connect();
			String command = "scp -f '" + sourceFilename + "'";
			Channel channel = OpenSession("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			while (true)
			{
				int c = checkAck(in);
				if (c != 'C')
				{
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true)
				{
					if (in.read(buf, 0, 1) < 0)
					{
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				lastReceivedFile = null;
				for (int i = 0;; i++)
				{
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a)
					{
						lastReceivedFile = new String(buf, 0, i);
						break;
					}
				}

				// System.out.println("filesize="+filesize+", file="+file);

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				fos = new FileOutputStream(destFilename);
				int foo;
				while (true)
				{
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0)
					{
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;
				if (checkAck(in) != 0)
				{
					return false;
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}
			return true;
		}
		finally
		{
			try
			{
				if (fos != null)
				{
					fos.close();
				}
			}
			catch (Exception ee)
			{
				ee.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @author Bo Zimmerman
	 * 
	 */
	private static class MyUserInfo implements UserInfo
	{
		private String passwd;

		public MyUserInfo(String passwd)
		{
			super();
			this.passwd = passwd;
		}

		public String getPassword()
		{
			return passwd;
		}

		public boolean promptYesNo(String str)
		{
			return true;
		}

		public String getPassphrase()
		{
			return null;
		}

		public boolean promptPassphrase(String message)
		{
			return true;
		}

		public void showMessage(String message)
		{
			System.out.println(message);
		}

		public boolean promptPassword(String message)
		{
			return true;
		}
	}
}
