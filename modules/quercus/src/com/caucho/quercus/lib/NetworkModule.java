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

package com.caucho.quercus.lib;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.InetAddress;
import java.net.Socket;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.Attributes;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusException;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;

import com.caucho.quercus.resources.StreamReadWrite;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.SocketStream;

/**
 * Information about PHP network
 */
public class NetworkModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(NetworkModule.class);
  private static final Logger log
    = Logger.getLogger(NetworkModule.class.getName());

  private static final HashMap<String,Value> _constMap =
          new HashMap<String,Value>();
  private static final LinkedHashMap<String, LongValue> _protoToNum =
  				new LinkedHashMap<String, LongValue>();
  private static final LinkedHashMap<String, ServiceNode> _servToNum =
    		  new LinkedHashMap<String, ServiceNode>();

  /**
   * Adds the constant to the PHP engine's constant map.
   *
   * @return the new constant chain
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }

  /**
   * Opens a socket
   */
  public static Value fsockopen(Env env,
				String host,
                                @Optional("80") int port,
                                @Optional @Reference Value errno,
                                @Optional @Reference Value errstr,
                                @Optional double timeout)
  {
    try {
      Socket s = new Socket(host, port);

      SocketStream sock = new SocketStream(s);

      WriteStream os = new WriteStream(sock);
      ReadStream is = new ReadStream(sock, os);

      return new StreamReadWrite(env, is, os);
    } catch (IOException e) {
      throw new QuercusException(e);
    }
  }

  /**
   * Converts string to long
   */
  public static Value ip2long(String ip)
  {
    long v = 0;

    int p = 0;
    int len = ip.length();
    for (int i = 0; i < 4; i++) {
      int digit = 0;
      char ch = 0;

      for (; p < len && '0' <= (ch = ip.charAt(p)) && ch <= '9'; p++) {
        digit = 10 * digit + ch - '0';
      }

      if (p < len && ch != '.')
        return BooleanValue.FALSE;
      else if (p == len && i < 3)
        return BooleanValue.FALSE;

      p++;

      v = 256 * v + digit;
    }

    return new LongValue(v);
  }

  /**
   * Returns the IP address of the given host name.  If the IP address cannot
   * be obtained, then the provided host name is returned instead.
   *
   * @param hostname  the host name who's IP to search for
   *
   * @return the IP for the given host name or, if the IP cannot be obtained,
   * 								the provided host name
   */
  public static Value gethostbyname(String hostname)
  {
  	// php/1m01

  	InetAddress ip = null;

  	try{
  		ip = InetAddress.getByName(hostname);
  	}
  	catch (Exception e) {
  		log.log(Level.WARNING, e.toString(), e);

  		return StringValue.create(hostname);
  	}

  	return StringValue.create(ip.getHostAddress());
  }

  /**
   * Returns the IP addresses of the given host name.  If the IP addresses
   *  cannot be obtained, then the provided host name is returned instead.
   *
   * @param hostname  the host name who's IP to search for
   *
   * @return the IPs for the given host name or, if the IPs cannot be obtained,
   * 								the provided host name
   */
  public static Value gethostbynamel(String hostname)
  {
  	// php/1m02

  	InetAddress ip[] = null;

  	try{
  		ip = InetAddress.getAllByName(hostname);
  	}
  	catch (Exception e) {
  		log.log(Level.WARNING, e.toString(), e);

  		return BooleanValue.FALSE;
  	}

  	ArrayValue ipArray = new ArrayValueImpl();

  	for (int k = 0; k < ip.length; k++) {
  		String currentIPString = ip[k].getHostAddress();

  		StringValue currentIP = new StringValueImpl((currentIPString));

  		ipArray.append(currentIP);
  	}

  	return ipArray;
  }

  /**
   * Returns the IP address of the given host name.  If the IP address cannot
   * be obtained, then the provided host name is returned instead.
   *
   * @param hostname  the host name who's IP to search for
   *
   * @return the IP for the given host name or, if the IP cannot be obtained,
   * 								the provided host name
   */
  public static Value gethostbyaddr(Env env, String ip)
  {
  	// php/1m03

  	String formIPv4 = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
  			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
  			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
  			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

  	CharSequence ipToCS = ip.subSequence(0, ip.length());

  	if (! (Pattern.matches(formIPv4, ipToCS))) {
  		env.warning("Address is not in a.b.c.d form");

  		return BooleanValue.FALSE;
  	}

  	String splitIP[] = null;

  	try {
  	  splitIP = ip.split("\\.");
  	}
  	catch (Exception e) {
  		log.log(Level.WARNING, e.toString(), e);

  		env.warning("Regex expression invalid");

  		return StringValue.create(ip);
  	}

  	byte addr[] = new byte[splitIP.length];

  	for (int k = 0; k < splitIP.length; k++) {
  		Integer intForm = new Integer(splitIP[k]);

  		addr[k] = intForm.byteValue();
  	}

  	InetAddress host = null;

  	try{
  		host = InetAddress.getByAddress(addr);
  	}
  	catch (Exception e) {
  		log.log(Level.WARNING, e.toString(), e);

  		return StringValue.create(ip);
  	}

  	return StringValue.create(host.getHostName());
  }

  /**
   * Returns the protocol number associated with the given protocol name.
   *
   * @param protoName  the name of the protocol
   *
   * @return the number associated with the given protocol name
   */
  public static Value getprotobyname(String protoName)
  {
  	// php/1m04

  	if (! (_protoToNum.containsKey(protoName)))
  		return BooleanValue.FALSE;

  	return LongValue.create((_protoToNum.get(protoName).toLong()));
  }

  /**
   * Returns the protocol number associated with the given protocol name.
   *
   * @param protoName  the name of the protocol
   *
   * @return the number associated with the given protocol name
   */
  public static Value getprotobynumber(int protoNumber)
  {
  	// php/1m05

  	for (Map.Entry<String, LongValue> entry: _protoToNum.entrySet())
  		if (entry.getValue().toLong() == protoNumber)
  			return StringValue.create(entry.getKey());

  	return BooleanValue.FALSE;
  }

  /**
   * Returns the port number associated with the given protocol and service
   * name.
   *
   * @param service  the service name
   * @param protocol  the protocol, either udp or tcp
   *
   * @return the number associated with the given protocol and service name
   */
  public static Value getservbyname(String service, String protocol)
  {
    // php/1m06

    if (! (_servToNum.containsKey(service)))
      return BooleanValue.FALSE;

    ServiceNode node = _servToNum.get(service);

    if (! (node.protocolCheck(protocol)))
      return BooleanValue.FALSE;

    return node.getPort();
  }

  /**
   * Returns the service name associated it the given protocol name and
   * service port.
   *
   * @param port  the service port number
   * @param protocol  the protocol, either udp or tcp
   *
   * @return the number associated with the given protocol name and
   *   service port
   */
  public static Value getservbyport(int port, String protocol)
  {
    // php/1m07

    for (Map.Entry<String, ServiceNode> entry: _servToNum.entrySet()) {
      ServiceNode node = entry.getValue();

      if (node.getPort().toLong() == port &&
      		node.protocolCheck(protocol))
      	return StringValue.create(entry.getKey());
    }

    return BooleanValue.FALSE;
  }

  /**
   * Finds the mx hosts for the given hostname, placing them in mxhosts and
   * their corresponding weights in weight, if provided.  Returns true if any
   * hosts were found.  False otherwise.
   *
   * @param hostname  the hostname to find records for
   * @param mxhosts  an array to add the mx hosts to
   * @param weight  an array to add the weights to
   *
   * @return true if records are found, false otherwise
   */
  public static boolean dns_get_mx(Env env,
  				 String hostname,
				   @Reference Value mxhosts,
				   @Optional @Reference Value weight)
    throws NamingException
  {
    // php/1m08

    DirContext ictx = new InitialDirContext();
    Attributes atrs = ictx.getAttributes("dns:/" + hostname, new String[] {"MX"});

    ArrayValue hosts =  new ArrayValueImpl();

    ArrayValue weights = new ArrayValueImpl();

    try {
	    NamingEnumeration list = atrs.getAll();

	    if (! (list.hasMore()))
	    		return false;

	    String[] tokens = list.next().toString().split("\\s");

	    for (int k = 1; k < tokens.length; k++) {
	    	int weightToInt = Integer.valueOf(tokens[k]).intValue();

	    	weights.append(LongValue.create(weightToInt));

	    	k++;

	    	String uncleanHost = tokens[k];

	    	int numOfCharacters = 0;

	    	if (k < tokens.length - 1)
	    		numOfCharacters = uncleanHost.length() - 2;
	    	else
	    	  numOfCharacters = uncleanHost.length() -1;

	      String cleanHost = uncleanHost.substring(0, numOfCharacters);

	    	hosts.append(StringValue.create(cleanHost));
	    }
    }
    catch(Exception e) {
    	log.log(Level.WARNING, e.toString(), e);
      env.warning("An error occurred while processing the records");

      return false;
    }

    mxhosts.set(hosts);
    weight.set(weights);
    return true;
  }

  /**
   * Finds the mx hosts for the given hostname, placing them in mxhosts and
   * their corresponding weights in weight, if provided.  Returns true if any
   * hosts were found.  False otherwise.
   *
   * @param hostname  the hostname to find records for
   * @param mxhosts  an array to add the mx hosts to
   * @param weight  an array to add the weights to
   *
   * @return true if records are found, false otherwise
   */
  public static boolean dns_check_record(Env env,
  				 String hostname,
  				 @Reference ArrayValue mxhosts,
					 @Optional @Reference  ArrayValue weight)
    throws NamingException
  {
    // php/1m09

  	DirContext ictx = new InitialDirContext();
    Attributes atrs = ictx.getAttributes("dns:/" + hostname);

    ArrayValue hosts =  new ArrayValueImpl();

    ArrayValue weights = new ArrayValueImpl();

    try {
	    NamingEnumeration list = atrs.getAll();

	    if (! (list.hasMore()))
	    		return false;

	    String[] tokens = list.next().toString().split("\\s");

	    for (int k = 1; k < tokens.length; k++) {
	    	int weightToInt = Integer.valueOf(tokens[k]).intValue();

	    	weights.append(LongValue.create(weightToInt));

	    	k++;

	    	String uncleanHost = tokens[k];

	    	int numOfCharacters = 0;

	    	if (k < tokens.length - 1)
	    		numOfCharacters = uncleanHost.length() - 2;
	    	else
	    	  numOfCharacters = uncleanHost.length() -1;

	      String cleanHost = uncleanHost.substring(0, numOfCharacters);

	    	hosts.append(StringValue.create(cleanHost));
	    }
    }
    catch(Exception e) {
    	log.log(Level.WARNING, e.toString(), e);
      env.warning("An error occurred while processing the records");

      return false;
    }

    mxhosts.set(hosts);
    weight.set(weights);
    return true;
  }

  private static class ServiceNode {
    private LongValue _port;

    private boolean _isTCP;
    private boolean _isUDP;

    ServiceNode(int port, boolean tcp, boolean udp)
    {
      _port = LongValue.create(port);
      _isTCP = tcp;
      _isUDP = udp;
    }

    public LongValue getPort()
    {
      return _port;
    }

    public boolean protocolCheck(String protocol)
    {
      if (protocol.equals("tcp"))
      	return _isTCP;
      else if (protocol.equals("udp"))
      	return _isUDP;
      else
      	return false;
    }

    public boolean isTCP()
    {
      return _isTCP;
    }

    public boolean isUDP()
    {
      return _isUDP;
    }
  }

  static {
    _protoToNum.put("ip", LongValue.create(0));
    _protoToNum.put("icmp", LongValue.create(1));
    _protoToNum.put("ggp", LongValue.create(3));
    _protoToNum.put("tcp", LongValue.create(6));
    _protoToNum.put("egp", LongValue.create(8));
    _protoToNum.put("pup", LongValue.create(12));
    _protoToNum.put("udp", LongValue.create(17));
    _protoToNum.put("hmp", LongValue.create(12));
    _protoToNum.put("xns-idp", LongValue.create(22));
    _protoToNum.put("rdp", LongValue.create(27));
    _protoToNum.put("rvd", LongValue.create(66));
    _servToNum.put("echo", new ServiceNode(7, true, true));
    _servToNum.put("discard", new ServiceNode(9, true, true));
    _servToNum.put("systat", new ServiceNode(11, true, true));
    _servToNum.put("daytime", new ServiceNode(13, true, true));
    _servToNum.put("qotd", new ServiceNode(17, true, true));
    _servToNum.put("chargen", new ServiceNode(19, true, true));
    _servToNum.put("ftp-data", new ServiceNode(20, true, false));
    _servToNum.put("ftp", new ServiceNode(21, true, false));
    _servToNum.put("telnet", new ServiceNode(23, true, false));
    _servToNum.put("smtp", new ServiceNode(25, true, false));
    _servToNum.put("time", new ServiceNode(37, true, true));
    _servToNum.put("rlp", new ServiceNode(39, false, true));
    _servToNum.put("nameserver", new ServiceNode(42, true, true));
    _servToNum.put("nicname", new ServiceNode(43, true, false));
    _servToNum.put("domain", new ServiceNode(53, true, true));
    _servToNum.put("bootps", new ServiceNode(67, false, true));
    _servToNum.put("bootpc", new ServiceNode(68, false, true));
    _servToNum.put("tftp", new ServiceNode(69, false, true));
    _servToNum.put("gopher", new ServiceNode(70, true, false));
    _servToNum.put("finger", new ServiceNode(79, true, false));
    _servToNum.put("http", new ServiceNode(80, true, false));
    _servToNum.put("kerberos", new ServiceNode(88, true, true));
    _servToNum.put("hostname", new ServiceNode(101, true, false));
    _servToNum.put("iso-tsap", new ServiceNode(102, true, false));
    _servToNum.put("rtelnet", new ServiceNode(107, true, false));
    _servToNum.put("pop2", new ServiceNode(109, true, false));
    _servToNum.put("pop3", new ServiceNode(110, true, false));
    _servToNum.put("sunrpc", new ServiceNode(111, true, true));
    _servToNum.put("auth", new ServiceNode(113, true, false));
    _servToNum.put("uucp-path", new ServiceNode(117, true, false));
    _servToNum.put("nntp", new ServiceNode(119, true, false));
    _servToNum.put("ntp", new ServiceNode(123, false, true));
    _servToNum.put("epmap", new ServiceNode(135, true, true));
    _servToNum.put("netbios-ns", new ServiceNode(137, true, true));
    _servToNum.put("netbios-dgm", new ServiceNode(138, false, true));
    _servToNum.put("netbios-ssn", new ServiceNode(139, true, false));
    _servToNum.put("imap", new ServiceNode(143, true, false));
    _servToNum.put("pcmail-srv", new ServiceNode(158, true, false));
    _servToNum.put("snmp", new ServiceNode(161, false, true));
    _servToNum.put("snmptrap", new ServiceNode(162, false, true));
    _servToNum.put("print-srv", new ServiceNode(170, true, false));
    _servToNum.put("bgp", new ServiceNode(179, true, false));
    _servToNum.put("irc", new ServiceNode(194, true, false));
    _servToNum.put("ipx", new ServiceNode(213, false, true));
    _servToNum.put("ldap", new ServiceNode(389, true, false));
    _servToNum.put("https", new ServiceNode(443, true, true));
    _servToNum.put("microsoft-ds", new ServiceNode(445, true, true));
    _servToNum.put("kpasswd", new ServiceNode(464, true, true));
    _servToNum.put("isakmp", new ServiceNode(500, false, true));
    _servToNum.put("exec", new ServiceNode(512, true, false));
    _servToNum.put("biff", new ServiceNode(512, false, true));
    _servToNum.put("login", new ServiceNode(513, true, false));
    _servToNum.put("who", new ServiceNode(513, false, true));
    _servToNum.put("cmd", new ServiceNode(514, true, false));
    _servToNum.put("syslog", new ServiceNode(514, false, true));
    _servToNum.put("printer", new ServiceNode(515, true, false));
    _servToNum.put("talk", new ServiceNode(517, false, true));
    _servToNum.put("ntalk", new ServiceNode(518, false, true));
    _servToNum.put("efs", new ServiceNode(520, true, false));
    _servToNum.put("router", new ServiceNode(520, false, true));
    _servToNum.put("timed", new ServiceNode(525, false, true));
    _servToNum.put("tempo", new ServiceNode(526, true, false));
    _servToNum.put("courier", new ServiceNode(530, true, false));
    _servToNum.put("conference", new ServiceNode(531, true, false));
    _servToNum.put("netnews", new ServiceNode(532, true, false));
    _servToNum.put("netwall", new ServiceNode(533, false, true));
    _servToNum.put("uucp", new ServiceNode(540, true, false));
    _servToNum.put("klogin", new ServiceNode(543, true, false));
    _servToNum.put("kshell", new ServiceNode(544, true, false));
    _servToNum.put("new-rwho", new ServiceNode(550, false, true));
    _servToNum.put("remotefs", new ServiceNode(556, true, false));
    _servToNum.put("rmonitor", new ServiceNode(560, false, true));
    _servToNum.put("monitor", new ServiceNode(561, false, true));
    _servToNum.put("ldaps", new ServiceNode(636, true, false));
    _servToNum.put("doom", new ServiceNode(666, true, true));
    _servToNum.put("kerberos-adm", new ServiceNode(749, true, true));
    _servToNum.put("kerberos-iv", new ServiceNode(750, false, true));
    _servToNum.put("kpop", new ServiceNode(1109, true, false));
    _servToNum.put("phone", new ServiceNode(1167, false, true));
    _servToNum.put("ms-sql-s", new ServiceNode(1433, true, true));
    _servToNum.put("ms-sql-m", new ServiceNode(1434, true, true));
    _servToNum.put("wins", new ServiceNode(1512, true, true));
    _servToNum.put("ingreslock", new ServiceNode(1524, true, false));
    _servToNum.put("12tp", new ServiceNode(1701, false, true));
    _servToNum.put("pptp", new ServiceNode(1723, true, false));
    _servToNum.put("radius", new ServiceNode(1812, false, true));
    _servToNum.put("radacct", new ServiceNode(1813, false, true));
    _servToNum.put("nfsd", new ServiceNode(2049, false, true));
    _servToNum.put("knetd", new ServiceNode(2053, true, false));
    _servToNum.put("man", new ServiceNode(9535, true, false));
  }
}

