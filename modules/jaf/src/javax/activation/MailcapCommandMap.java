/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.activation;

import java.util.*;
import java.io.*;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.logging.*;

/**
 * MailcapCommandMap extends the CommandMap
 *  abstract class. It implements a CommandMap whose configuration
 *  is based on mailcap files (RFC 1524). The MailcapCommandMap can
 *  be configured both programmatically and via configuration
 *  files.
 *   <b>Mailcap file search order:</b> The MailcapCommandMap looks in various places in the user's
 *  system for mailcap file entries. When requests are made
 *  to search for commands in the MailcapCommandMap, it searches  
 *  mailcap files in the following order:
 *   <ol>
 *  <li> Programatically added entries to the MailcapCommandMap instance.
 *  <li> The file <code>.mailcap</code> in the user's home directory.
 *  <li> The file &lt;<i>java.home</i>&gt;<code>/lib/mailcap</code>.
 *  <li> The file or resources named <code>META-INF/mailcap</code>.
 *  <li> The file or resource named <code>META-INF/mailcap.default</code>
 *  (usually found only in the <code>activation.jar</code> file).
 *  </ol>
 *   <b>Mailcap file format:</b> Mailcap files must conform to the mailcap
 *  file specification (RFC 1524, <i>A User Agent Configuration Mechanism
 *  For Multimedia Mail Format Information</i>). 
 *  The file format consists of entries corresponding to
 *  particular MIME types. In general, the specification 
 *  specifies <i>applications</i> for clients to use when they
 *  themselves cannot operate on the specified MIME type. The 
 *  MailcapCommandMap extends this specification by using a parameter mechanism
 *  in mailcap files that allows JavaBeans(tm) components to be specified as
 *  corresponding to particular commands for a MIME type. When a mailcap file is
 *  parsed, the MailcapCommandMap recognizes certain parameter signatures,
 *  specifically those parameter names that begin with <code>x-java-</code>.
 *  The MailcapCommandMap uses this signature to find
 *  command entries for inclusion into its registries.
 *  Parameter names with the form <code>x-java-&lt;name></code>
 *  are read by the MailcapCommandMap as identifying a command
 *  with the name <i>name</i>. When the <i>name</i> is <code>
 *  content-handler</code> the MailcapCommandMap recognizes the class
 *  signified by this parameter as a <i>DataContentHandler</i>.
 *  All other commands are handled generically regardless of command 
 *  name. The command implementation is specified by a fully qualified
 *  class name of a JavaBean(tm) component. For example; a command for viewing
 *  some data can be specified as: <code>x-java-view=com.foo.ViewBean</code>. 
 *  MailcapCommandMap aware mailcap files have the 
 *  following general form: <code>
 *  # Comments begin with a '#' and continue to the end of the line.<br>
 *  &lt;mime type>; ; &lt;parameter list><br>
 *  # Where a parameter list consists of one or more parameters,<br>
 *  # where parameters look like: x-java-view=com.sun.TextViewer<br>
 *  # and a parameter list looks like: <br>
 *  text/plain; ; x-java-view=com.sun.TextViewer; x-java-edit=com.sun.TextEdit
 *  <br>
 *  # Note that mailcap entries that do not contain 'x-java' parameters<br>
 *  # and comply to RFC 1524 are simply ignored:<br>
 *  image/gif; /usr/dt/bin/sdtimage %s<br>
 *  </code>
 */
public class MailcapCommandMap extends CommandMap {

  private static Logger log =
    Logger.getLogger("javax.activation.MailcapCommandMap");

  /** HashMap<String,HashMap<String,CommandInfo>> */
  HashMap _preferredCommands = new HashMap();

  /** HashMap<String,HashSet<CommandInfo>> */
  HashMap _allCommands = new HashMap();

  /**
   * The default Constructor.
   */
  public MailcapCommandMap()
  {
    // add mailcaps in reverse order
    ClassLoader currentCL = Thread.currentThread().getContextClassLoader();

    if (currentCL==null)
      currentCL = MimetypesFileTypeMap.class.getClassLoader();

    try {
      addMailcap(currentCL.getResourceAsStream("META-INF/mailcap.default"));
    }
    catch(IOException e) {
      log.log(Level.FINER, "ignoring exception", e);
    }

    try {
      addMailcap(currentCL.getResourceAsStream("META-INF/mailcap"));
    }
    catch(IOException e) {
      log.log(Level.FINER, "ignoring exception", e);
    }

    String uhome = System.getProperty("user.home");
    String jhome = System.getProperty("java.home");

    try {
      addMailcap(new File(jhome+File.separatorChar+
			  "lib"+File.separatorChar+"mailcap"));
    }
    catch(IOException e) {
      log.log(Level.FINER, "ignoring exception", e);
    }

    try {
      addMailcap(new File(uhome+File.separatorChar+".mailcap"));
    }
    catch(IOException e) {
      log.log(Level.FINER, "ignoring exception", e);
    }
  }

  /**
   * Constructor that allows the caller to specify the path
   * of a mailcap file.
   *
   * @param fileName The name of the mailcap file to open
   */
  public MailcapCommandMap(String fileName) throws IOException
  {
    this();
    FileInputStream is = new FileInputStream(fileName);

    try {
      addMailcap(is);
    } finally {
      is.close();
    }
  }

  /**
   * Constructor that allows the caller to specify an InputStream
   * containing a mailcap file.
   *
   * @param is InputStream of the mailcap file to open
   */
  public MailcapCommandMap(InputStream is)
    throws IOException
  {
    this();
    addMailcap(is);
  }

  /**
   * Get the preferred command list for a MIME Type. The
   * MailcapCommandMap searches the mailcap files as described above
   * under Mailcap file search order.  The result of the search is a
   * proper subset of available commands in all mailcap files known to
   * this instance of MailcapCommandMap.  The first entry for a
   * particular command is considered the preferred command.
   *
   * @param mimeType the MIME type 
   * @return the CommandInfo objects representing the preferred commands. 
   */
  public CommandInfo[] getPreferredCommands(String mimeType)
  {
    HashMap commandInfoMap =
      (HashMap)_preferredCommands.get(mimeType);

    if (commandInfoMap==null)
      return new CommandInfo[0];

    CommandInfo []commands = new CommandInfo[commandInfoMap.size()];
    commandInfoMap.values().toArray(commands);
    return commands;
  }

  /**
   * Get all the available commands in all mailcap files known to
   * this instance of MailcapCommandMap for this MIME type.
   *
   * @param mimeType the MIME type 
   * @return the CommandInfo objects representing all the commands. 
   */
  public CommandInfo[] getAllCommands(String mimeType)
  {
    HashSet commandInfoSet =
      (HashSet)_allCommands.get(mimeType);

    if (commandInfoSet==null)
      return new CommandInfo[0];

    CommandInfo []commands = new CommandInfo[commandInfoSet.size()];
    commandInfoSet.toArray(commands);
    return commands;
  }

  /**
   * Get the command corresponding to cmdName for the MIME type.
   *
   * @param mimeType the MIME type
   * @param cmdName the command name 
   * @return the CommandInfo object corresponding to the command. 
   */
  public CommandInfo getCommand(String mimeType, String cmdName)
  {
    HashMap commandMap = 
      (HashMap)_preferredCommands.get(mimeType);

    if (commandMap==null)
      return null;

    return (CommandInfo)commandMap.get(cmdName);
  }

  /**
   * Add entries to the registry.  Programmatically added entries are
   * searched before other entries.  The string that is passed in
   * should be in mailcap format.
   *
   * NOTE: this can be a multiline string.
   *
   * @param mail_cap a correctly formatted mailcap string
   */
  public void addMailcap(String mail_cap)
  {
    try {
      addMailcap(new StringReader(mail_cap));
    }
    catch (IOException e) {
      // this should not happen
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the DataContentHandler for the specified MIME type.
   *
   * @param mimeType the MIME type 
   * @return the DataContentHandler 
   */
  public DataContentHandler createDataContentHandler(String mimeType)
  {
    CommandInfo ci = getCommand(mimeType, "content-handler");

    if (ci==null)
      return null;

    try {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      return (DataContentHandler)classLoader.loadClass(ci.getCommandClass()).newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**                                                                      
   * Get all the MIME types known to this command map.                     
   *                                                                       
   * @overrides getMimeTypes  in class                                     
   * @overrides CommandMap                                                 
   * @return array of MIME types as strings                                
   * @since  JAF 1.1                                                       
   */
  public String[] getMimeTypes() {
    String[] mimeTypes = new String[_preferredCommands.size()];
    return 
      (String[])_preferredCommands.keySet().toArray(mimeTypes);
  }

  /**                                                                      
   * Get the native commands for the given MIME type.  Returns an
   * array of strings where each string is an entire mailcap file
   * entry.  The application will need to parse the entry to extract
   * the actual command as well as any attributes it needs. See RFC
   * 1524 for details of the mailcap entry syntax.  Only mailcap
   * entries that specify a view command for the specified MIME type
   * are returned.
   *                                                                       
   * @return array of native command entries                               
   * @since  JAF 1.1                                                       
   */
  public String []getNativeCommands(String mimeType) {

    // Apparently this function is broken in Sun's implementation and
    // the specified behavior is unclear; see test case 340c.
    return new String[0];
  }

  private void addMailcap(InputStream is)
    throws IOException
  {
    if (is==null) return;
    addMailcap(new InputStreamReader(is));
  }

  private void addMailcap(Reader reader)
    throws IOException
  {
    if (reader==null) return;
    BufferedReader br = new BufferedReader(reader);

    for(String s = br.readLine(); s!=null; s = br.readLine())
      addMailcapLine(s);
  }

  public void addMailcap(File file)
    throws IOException
  {
    InputStream is = new FileInputStream(file);
    try {
      addMailcap(is);
    } finally {
      is.close();
    }
  }

  private void addMailcapLine(String line)
    throws IOException
  {
    line = line.trim();

    if (line.length() == 0)
      return;

    if (line.charAt(0) == '#')
      return;

    String[] fields = line.split(";");

    if (fields.length<3) {
      log.log(Level.FINER, "malformed mailcap line: " + line);
      return;
    }

    String mimeType = fields[0];

    for(int i=2; i<fields.length; i++) {

      String field = fields[i].trim();

      if (field.indexOf('=')==-1)
	continue;

      String key = field.substring(0, field.indexOf('='));
      String className = field.substring(field.indexOf('=')+1);

      if (!key.startsWith("x-java-")) continue;
      String verb = key.substring(7);

      CommandInfo commandInfo = new CommandInfo(verb, className);
      
      HashSet commandSet =
	(HashSet)_allCommands.get(mimeType);
      if (commandSet==null)
	_allCommands.put(mimeType, commandSet = new HashSet());
      commandSet.add(commandInfo);
      
      HashMap commandMap =
	(HashMap)_preferredCommands.get(mimeType);
      if (commandMap==null)
	_preferredCommands.put(mimeType, commandMap = new HashMap());
      commandMap.put(verb, commandInfo);
    }
  }

}
