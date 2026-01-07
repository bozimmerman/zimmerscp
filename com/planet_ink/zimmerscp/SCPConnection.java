package com.planet_ink.zimmerscp;

import java.io.*;
import java.util.Vector;

import com.jcraft.jsch.*;

public class SCPConnection
{
	private final String	host;
	private final String	user;
	private final String	password;
	private final int		sshdPort;
	public String			knownHostsFile;
	private Session			_session			= null;
	private String			lastReceivedFile	= null;

	private final java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

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

	public int getSSHDPort()
	{
		return sshdPort;
	}

	public String getKnownHostsFile()
	{
		return knownHostsFile;
	}

	public SCPConnection(final String myHost, final String myKnownHostsFile, final String myUser, final String myPassword, final int sshdPort)
	{
		host = myHost;
		user = myUser;
		password = myPassword;
		knownHostsFile = myKnownHostsFile;
		this.sshdPort = sshdPort;
	}

	private boolean connect() throws JSchException
	{
		if (_session == null)
		{
			final JSch jsch = new JSch();
			// jsch.setKnownHosts("c:\\hosts");
			_session = jsch.getSession(user, host, sshdPort);

			// username and password will be given via UserInfo interface.
			final UserInfo ui = new MyUserInfo(password);
			_session.setUserInfo(ui);
			_session.connect();
		}
		return true;
	}

	public String getLastReceivedFile()
	{
		return lastReceivedFile;
	}

	public static int clearAck(final InputStream in) throws IOException
	{
		int x=0;
		while(in.available()>0)
		{
			in.read();
			x++;
		}
		return x;

	}

	/**
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static int checkAck(final InputStream in) throws IOException
	{
		final long bitTimeOut = System.currentTimeMillis() + 200;
		while((in.available()==0)&&(System.currentTimeMillis() < bitTimeOut))
			try { Thread.sleep(1); } catch (final InterruptedException e) { }
		final int b = in.read();
		System.err.println(" response code is "+b);
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
			final long timeout = System.currentTimeMillis() + 5000;
			int c;
			do
			{
				c = in.read();
				if(c != -1)
					System.err.print(c);
			} while((c!='\n') && (System.currentTimeMillis() < timeout));
			if(System.currentTimeMillis() >= timeout)
				System.err.println("");
		}
		return b;
	}

	/**
	 *
	 * @param sourceFilename
	 * @param destFilename
	 * @return
	 */
	public boolean sendFile(final String sourceFilename, final String destFilename) throws Exception
	{
		FileInputStream fis = null;
		try
		{
			connect();
			String command = "scp -p -t '" + destFilename + "'";
			final Channel channel = openCommand(command);
			if(channel == null)
				return false;

			final OutputStream out = channel.getOutputStream();
			final InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0)
			{
				return false;
			}

			// send "C0644 filesize filename" where filename doesn't contain a /
			final long filesize = (new File(sourceFilename)).length();
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
			final byte[] buf = new byte[1024];
			while (true)
			{
				final int len = fis.read(buf, 0, buf.length);

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
			catch (final Exception ee)
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
	public boolean deleteFile(final String filename, final boolean recursive) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			final String command = "rm -"+(recursive?"rf":"f")+" '"+ filename + "'";
			channel = openCommand(command);
			if(channel == null)
				return false;
			final InputStream in = channel.getInputStream();
			channel.connect();
			try
			{
				if (checkAck(in) != 0)
				{
					in.close();
					final String s=this.getFileRow(filename);
					System.err.println("check: "+s);
					return s==null;
				}
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
	public boolean moveFile(final String oldFilename, final String newFilename) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			final String command = "mv '"+oldFilename+"' '"+ newFilename + "'";
			channel = openCommand(command);
			if(channel == null)
				return false;
			final InputStream in = channel.getInputStream();
			channel.connect();
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
	public boolean createSoftLink(final String filePath, final String newLinkPath) throws Exception
	{
		Channel channel = null;
		try
		{
			connect();
			final String command = "ln -s '"+filePath+"' '"+ newLinkPath + "'";
			channel = openCommand(command);
			if(channel == null)
				return false;
			final InputStream in = channel.getInputStream();
			channel.connect();
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
	public boolean makeDirectory(final String filename) throws Exception
	{
		Channel channel = null;
		try
		{
			if(getFileRow(filename) != null)
				return false;
			connect();
			final String command = "mkdir '"+ filename + "'";
			channel = openCommand(command);
			if(channel == null)
				return false;
			channel.connect();
			final InputStream in = channel.getInputStream();
			try
			{
				final int x = checkAck(in);
				in.close();
				channel.disconnect();
				if(x==0)
					return true;
				close();
				final String fileRow = getFileRow(filename);
				if(fileRow != null)
					return true;
			}
			finally
			{
				in.close();
			}
			return false;
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

	public Channel openSession(final String exec)
	{
		try
		{
			return _session.openChannel(exec);
		}
		catch(final JSchException je)
		{
			try
			{
				close();
				connect();
				return openSession(exec);
			}
			catch(final Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
		}
	}

	public Channel openCommand(final String command) throws JSchException
	{
		final Channel channel = openSession("exec");
		if(channel == null)
			return null;
		System.out.println(command);
		((ChannelExec) channel).setCommand(command);
		return channel;
	}

	public String getFileRow(String remoteDirectory) throws IOException, JSchException
	{
		try
		{
			connect();
			remoteDirectory = remoteDirectory.trim();
			if (remoteDirectory.endsWith("/"))
				remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
			if (remoteDirectory.endsWith("\\"))
				remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
			final String command = "ls -lAUd --time-style=long-iso '" + remoteDirectory + "'";
			if(!remoteDirectory.endsWith("/"))
				remoteDirectory += "/";
			final Channel channel = openCommand(command);
			if(channel == null)
				return null;

			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try(final InputStream in = channel.getInputStream())
			{
				channel.connect();

				final byte[] buf = new byte[1024];
				int x = 1;
				while (x > 0)
				{
					x = in.read(buf, 0, 1024);
					if (x > 0)
						bout.write(buf, 0, x);
				}
				in.close();
			}
			channel.disconnect();
			final BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray())));
			String s = br.readLine();
			if(s!=null)
			{
				if (s.length() == 0)
					s = br.readLine();
				if ((s!=null)&&(s.length()>0)&&(!s.startsWith("total")))
					return s;
				s = br.readLine();
				if ((s!=null)&&(s.length() == 0))
					s = br.readLine();
			}
			return s;
		}
		finally
		{
		}
	}

	/**
	 *
	 * @param remoteDirectory
	 * @return
	 */
	public Vector<RemoteNode> getDirectory(final DestTree tree, String remoteDirectory) throws Exception
	{
		try
		{
			connect();
			remoteDirectory = remoteDirectory.trim();
			if (remoteDirectory.endsWith("/"))
				remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
			if (remoteDirectory.endsWith("\\"))
				remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
			final String command = "ls -lAU --time-style=long-iso '" + remoteDirectory + "'";
			if(!remoteDirectory.endsWith("/"))
				remoteDirectory += "/";
			final Channel channel = openCommand(command);
			if(channel == null)
				return new Vector<RemoteNode>(1);

			final InputStream in = channel.getInputStream();

			channel.connect();

			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			final byte[] buf = new byte[1024];
			int x = 1;
			while (x > 0)
			{
				x = in.read(buf, 0, 1024);
				if (x > 0)
					bout.write(buf, 0, x);
			}
			in.close();
			channel.disconnect();
			final BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray())));
			String s = br.readLine();
			final Vector<RemoteNode> nodes = new Vector<RemoteNode>();
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
					final RemoteNode node = new RemoteNode(tree,this);
					x = 10;
					if((s.indexOf('.')==10)&&(s.indexOf(' ')==11))
						x=11;
					else
					if((s.indexOf('+')==10)&&(s.indexOf(' ')==11))
						x=11;
					else
					if (s.indexOf(' ') != 10)
						throw new IOException("Unexpected no att: " + s);
					node.parseUnixAttributes(s, 1);
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
					catch (final Exception e)
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
	public boolean getFile(final String sourceFilename, final String destFilename) throws Exception
	{
		FileOutputStream fos = null;
		try
		{
			connect();
			final String command = "scp -f '" + sourceFilename + "'";
			final Channel channel = openCommand(command);
			if(channel == null)
				return false;

			final OutputStream out = channel.getOutputStream();
			final InputStream in = channel.getInputStream();

			channel.connect();

			final byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			while (true)
			{
				final int c = checkAck(in);
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
					filesize = filesize * 10L + buf[0] - '0';
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
			catch (final Exception ee)
			{
				ee.printStackTrace();
			}
		}
	}

	public String getFileHash(final String remoteFilePath) throws Exception
	{
		connect();
		final String command = "cksum '" + remoteFilePath + "'";
		final Channel channel = openCommand(command);
		if(channel == null)
			throw new Exception("Unable to open command channel");

		channel.setInputStream(null);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		channel.setOutputStream(baos);

		channel.connect();
		while(!channel.isClosed())
		{
			try { Thread.sleep(100); } catch(final Exception e) {}
		}
		channel.disconnect();

		String output = baos.toString().trim();
		if(output.length() == 0)
			throw new Exception("No hash output received");

		final int spaceIndex = output.indexOf(' ');
		if(spaceIndex > 0)
			output = output.substring(0, spaceIndex);
		System.out.println("Result: "+output);
		return output;
	}

	/**
	 *
	 * @author Bo Zimmerman
	 *
	 */
	private static class MyUserInfo implements UserInfo
	{
		private final String passwd;

		public MyUserInfo(final String passwd)
		{
			super();
			this.passwd = passwd;
		}

		public String getPassword()
		{
			return passwd;
		}

		public boolean promptYesNo(final String str)
		{
			return true;
		}

		public String getPassphrase()
		{
			return null;
		}

		public boolean promptPassphrase(final String message)
		{
			return true;
		}

		public void showMessage(final String message)
		{
			System.out.println(message);
		}

		public boolean promptPassword(final String message)
		{
			return true;
		}
	}
}
