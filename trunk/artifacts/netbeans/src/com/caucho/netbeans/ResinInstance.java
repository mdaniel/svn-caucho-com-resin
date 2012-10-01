/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */
package com.caucho.netbeans;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.netbeans.api.server.ServerInstance;
import org.netbeans.api.server.properties.InstanceProperties;
import org.netbeans.spi.server.ServerInstanceImplementation;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import static org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.URL_ATTR;
import static org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.USERNAME_ATTR;
import static org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.PASSWORD_ATTR;
import static org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.DISPLAY_NAME_ATTR;
import static org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties.HTTP_PORT_NUMBER;

public class ResinInstance implements ServerInstanceImplementation {

  private final static Logger log = Logger.getLogger(ResinInstance.class.getName());
  private ServerInstance _server;
  private InstanceProperties _instanceProperties;
  private ResinNode _basicNode;
  private ResinNode _fullNode;
  private String _displayName = "Resin";
  private String _home;
  private String _root;
  private String _host;
  private String _address;
  private int _port;
  private String _url;
  private String _user = "deploy";
  private String _password = "deploy";
  private String _conf;
  private String _webapps = "webapps";

  public ResinInstance(InstanceProperties properties) {
    _instanceProperties = properties;
    _url = properties.getString(URL_ATTR, null);
    _displayName = properties.getString(DISPLAY_NAME_ATTR, null);
    _home = properties.getString("home", null);
    _root = properties.getString("root", null);
    _host = properties.getString("host", null);
    _address = properties.getString("address", null);
    _port = properties.getInt(HTTP_PORT_NUMBER, 8080);
    _user = properties.getString(USERNAME_ATTR, "admin");
    _password = properties.getString(PASSWORD_ATTR, "deploy");
    _conf = properties.getString("conf", null);
    _webapps = properties.getString("webapps", null);
  }

  public ResinInstance(String displayName, String home, String root, String host, String address, int port, String user, String password, String conf, String webapps) {
    _displayName = displayName;
    _home = home;
    _root = root;
    _host = host;
    _address = address;
    _port = port;
    _user = user;
    _password = password;
    _conf = conf;
    _webapps = webapps;
    _url = createUrl(home, root, host, address, port, displayName);
  }

  void persist(InstanceProperties properties) {
    _instanceProperties = properties;
    properties.putString(URL_ATTR, _url);
    properties.putString(DISPLAY_NAME_ATTR, _displayName);
    properties.putString("home", _home);
    properties.putString("root", _root);
    properties.putString("host", _host);
    properties.putString("address", _address);
    properties.putString(USERNAME_ATTR, _user);
    properties.putString(PASSWORD_ATTR, _password);
    properties.putString("conf", _conf);
    properties.putString("webapps", _webapps);
    properties.putInt(HTTP_PORT_NUMBER, _port);
  }

  InstanceProperties getInstanceProperties() {
    return _instanceProperties;
  }

  public void setServerInstance(ServerInstance server) {
    _server = server;
  }

  public ServerInstance getServerInstance() {
    return _server;
  }

  @Override
  public String getDisplayName() {
    return _displayName;
  }

  @Override
  public String getServerDisplayName() {
    return _displayName;
  }

  public String getName() {
    return _displayName;
  }

  public void setName(String _name) {
    this._displayName = _name;
  }

  public String getAddress() {
    return _address;
  }

  public void setAddress(String _address) {
    this._address = _address;
  }

  public String getHome() {
    return _home;
  }

  public void setHome(String _home) {
    this._home = _home;
  }

  public String getHost() {
    return _host;
  }

  public void setHost(String _host) {
    this._host = _host;
  }

  public int getPort() {
    return _port;
  }

  public void setPort(int _port) {
    this._port = _port;
  }

  public String getRoot() {
    return _root;
  }

  public void setRoot(String _root) {
    this._root = _root;
  }

  public String getPassword() {
    return _password;
  }

  public void setPassword(String _password) {
    this._password = _password;
  }

  public String getUser() {
    return _user;
  }

  public void setUser(String _user) {
    this._user = _user;
  }

  public String getConf() {
    return _conf;
  }

  public void setConf(String _conf) {
    this._conf = _conf;
  }

  public String getUrl() {
    return _url;
  }

  public String getWebapps() {
    return _webapps;
  }

  public void setWebapps(String webapps) {
    _webapps = webapps;
  }

  @Override
  public Node getFullNode() {
    if (_fullNode == null) {
      _fullNode = new ResinNode(Children.LEAF, this);
    }

    return _fullNode;
  }

  @Override
  public Node getBasicNode() {
    if (_basicNode == null) {
      _basicNode = new ResinNode(Children.LEAF, this);
    }

    return _basicNode;
  }

  @Override
  public JComponent getCustomizer() {
    JPanel commonCustomizer = new ResinInstanceCustomizer(this);
    //JPanel vmCustomizer = new VmCustomizer();

    Collection<JPanel> pages = new LinkedList<JPanel>();
    //Collection<? extends CustomizerCookie> lookupAll = lookup.lookupAll(CustomizerCookie.class);
    //for (CustomizerCookie cookie : lookupAll) {
    //pages.addAll(cookie.getCustomizerPages());
    //}
    //pages.add(vmCustomizer);

    JTabbedPane tabbedPane = null;
    for (JPanel page : pages) {
      if (tabbedPane == null) {
        tabbedPane = new JTabbedPane();
        tabbedPane.add(commonCustomizer);
      }

      tabbedPane.add(page);
    }

    return tabbedPane != null ? tabbedPane : commonCustomizer;
  }

  @Override
  public void remove() {
    ResinInstanceProvider.getInstance().remove(this);
  }

  @Override
  public boolean isRemovable() {
    return true;
  }

  public String createUrl(String home, String root, String host, String address, int port, String displayName) {
    StringBuilder url = new StringBuilder();
    url.append("resin:home:").append('"').append(home).append("\":").
            append("root:").append('"').append(root).append("\":").
            append("host:").append('"').append(host).append("\":").
            append("address:").append('"').append(address).append(':').append(port).
            append("display-name:").append('"').append(displayName).append('"');

    return url.toString();
  }

  public static String makeConfName(String resinName) {
    StringBuilder builder = new StringBuilder("netbeans-");
    for (char c : resinName.toCharArray()) {
      if (c == ' ') {
        builder.append('_');
      } else if (c == ':') {
        builder.append('_');
      } else if (c == '/' || c == '\\') {
        builder.append('_');
      } else {
        builder.append(c);
      }
    }

    builder.append(".xml");

    return builder.toString();
  }

  public static boolean isResinHome(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    File home = new File(value);

    if (!home.exists() || !home.isDirectory()) {
      return false;
    }

    if (!new File(home, "conf/resin.xml").exists()) {
      return false;
    }

    if (!new File(home, "lib/resin.jar").exists()) {
      return false;
    }

    return true;
  }
}
