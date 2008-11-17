/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import com.caucho.bam.*;
import com.caucho.git.*;
import com.caucho.server.resin.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.*;
import java.util.HashMap;

/**
 * Deploy Client API
 */
public class DeployClient
{
  private static final L10N L = new L10N(DeployClient.class);

  private BamBroker _broker;
  private BamConnection _conn;
  private String _deployJid;
  
  private HmuxClient _client;

  public DeployClient()
  {
    Resin resin = Resin.getCurrent();

    _broker = resin.getManagement().getAdminBroker();
    _conn = _broker.getConnection("admin", null);

    _deployJid = "deploy@resin.caucho";
  }

  public DeployClient(String serverId)
  {
    Resin resin = Resin.getCurrent();

    _broker = resin.getManagement().getAdminBroker();
    _conn = _broker.getConnection("admin", null);

    _deployJid = "deploy@" + serverId + ".resin.caucho";
  }
  
  public DeployClient(String host, int port)
  {
    _client = new HmuxClient(host, port);
    
    _deployJid = "deploy@resin.caucho";
  }

  public String deployJar(Path jar,
			  String tag,
			  String user,
			  String message,
			  String version,
			  HashMap<String,String> extraAttr)
    throws IOException
  {
    GitCommitJar commit = new GitCommitJar(jar);

    try {
      return deployJar(commit, tag, user, message, version, extraAttr);
    } finally {
      commit.close();
    }
  }

  public String deployJar(InputStream jar,
			  String tag,
			  String user,
			  String message,
			  String version,
			  HashMap<String,String> extraAttr)
    throws IOException
  {
    GitCommitJar commit = new GitCommitJar(jar);

    try {
      return deployJar(commit, tag, user, message, version, extraAttr);
    } finally {
      commit.close();
    }
  }

  public String deployJar(GitCommitJar commit,
			  String tag,
			  String user,
			  String message,
			  String version,
			  HashMap<String,String> extraAttr)
    throws IOException
  {
    String []files = getCommitList(commit.getCommitList());

    for (String file : files) {
      InputStream is = commit.openFile(file);

      try {
	sendFile(file, is);
      } finally {
	if (is != null)
	  is.close();
      }
    }

    return commit(tag, commit.getDigest(), user, message, extraAttr);
  }

  //
  // low-level routines
  //

  public String []getCommitList(String []commitList)
  {
    DeployCommitListQuery query = new DeployCommitListQuery(commitList);
    
    DeployCommitListQuery result = (DeployCommitListQuery) queryGet(query);

    return result.getCommitList();
  }

  public String calculateFileDigest(InputStream is, long length)
    throws IOException
  {
    return GitCommitTree.calculateBlobDigest(is, length);
  }

  public String addDeployFile(String tag, String name, String sha1)
  {
    DeployAddFileQuery query
      = new DeployAddFileQuery(tag, name, sha1);

    return (String) querySet(query);
  }
	  
  public boolean sendFile(String sha1, long length, InputStream is)
    throws IOException
  {
    return sendFile(sha1, GitCommitTree.writeBlob(is, length));
  }

  public boolean sendFile(String sha1, InputStream is)
    throws IOException
  {
    try {
      int i = 0;

      DeploySendQuery query;
      boolean isEnd = false;
      
      while (! isEnd) {
	byte []buffer = new byte[8192];

	int len = is.read(buffer, 0, buffer.length);

	if (len <= 0) {
	  isEnd = true;
	}
	else if (len < buffer.length) {
	  int len2 = is.read(buffer, len, buffer.length - len);

	  if (len2 > 0)
	    len = len + len2;
	  else
	    isEnd = true;
	}

	query = new DeploySendQuery(i++, sha1, buffer, len, isEnd);

	Boolean value = (Boolean) querySet(query);

	if (value == null || ! value)
	  return false;
      }

      return true;
    } finally {
      is.close();
    }
  }

  public String commit(String tag,
		       String sha1,
		       String user,
		       String message,
		       HashMap<String,String> attr)
  {
    DeployCommitQuery query
      = new DeployCommitQuery(tag, sha1, user, message, attr);

    return (String) querySet(query);
  }

  public StatusQuery status(String tag)
  {
    StatusQuery query = new StatusQuery(tag);

    return (StatusQuery) queryGet(query);
  }

  public HostQuery []listHosts()
  {
    ListHostsQuery query = new ListHostsQuery();

    return (HostQuery []) queryGet(query);
  }

  public WebAppQuery []listWebApps(String[] hosts)
  {
    ListWebAppsQuery []query = new ListWebAppsQuery[hosts.length];

    for (int i = 0; i < hosts.length; i++) {
      ListWebAppsQuery q = new ListWebAppsQuery();
      q.setHost(hosts[i]);

      query[i] = q;
    }

    return (WebAppQuery []) queryGet(query);
  }

  /**
   * Deploys a controller, but does not start it
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean deploy(String type, String host, String name)
  {
    String tag = type + "/" + host + "/" + name;

    return deploy(tag);
  }

  /**
   * Undeploy controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean deploy(String tag)
  {
    ControllerDeployQuery query = new ControllerDeployQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Starts a controller
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean start(String type, String host, String name)
  {
    String tag = type + "/" + host + "/" + name;

    return start(tag);
  }

  /**
   * Starts a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean start(String tag)
  {
    ControllerStartQuery query = new ControllerStartQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Stops a controller
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean stop(String type, String host, String name)
  {
    String tag = type + "/" + host + "/" + name;

    return stop(tag);
  }

  /**
   * Stops a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean stop(String tag)
  {
    ControllerStopQuery query = new ControllerStopQuery(tag);

    return (Boolean) querySet(query);
  }

  /**
   * Undeploys a controller
   *
   * @param type the controller type: war, ear, etc.
   * @param host the virtual host
   * @param name the web-app/ear name
   */
  public Boolean undeploy(String type, String host, String name)
  {
    String tag = type + "/" + host + "/" + name;

    return undeploy(tag);
  }

  /**
   * Undeploy a controller based on a deployment tag: wars/foo.com/my-war
   *
   * @param tag the encoded controller name
   */
  public Boolean undeploy(String tag)
  {
    ControllerUndeployQuery query = new ControllerUndeployQuery(tag);

    return (Boolean) querySet(query);
  }

  public Boolean []executeWebAppCommand(String [][]apps,
                                        WebAppCommandQuery.WebAppCommand command)
  {
    WebAppCommandQuery []query = new WebAppCommandQuery[apps.length];

    for (int i = 0; i < apps.length; i++) {
      String []app = apps[i];

      WebAppCommandQuery q = new WebAppCommandQuery();

      q.setHost(app[0]);
      q.setWebAppId(app[1]);
      q.setCommand(command);

      query[i] = q;
    }

    return (Boolean[]) querySet(query);
  }

  private Serializable queryGet(Serializable query)
  {
    if (_conn != null)
      return _conn.queryGet(_deployJid, query);
    else {
      return (Serializable) _client.queryGet(_deployJid, query);
    }
  }

  private Serializable querySet(Serializable query)
  {
    if (_conn != null)
      return _conn.querySet(_deployJid, query);
    else {
      return (Serializable) _client.querySet(_deployJid, query);
    }
  }

  @Override
  public String toString()
  {
    if (_broker != null)
      return getClass().getSimpleName() + "[" + _deployJid + "]";
    else
      return getClass().getSimpleName() + "[" + _client + "]";
  }
}