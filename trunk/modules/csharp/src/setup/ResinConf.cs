/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.XPath;
using System.IO;

namespace Caucho
{
  public class ResinConf
  {
    private String _resinConf;
    private XPathDocument _xPathDoc;
    private XPathNavigator _docNavigator;
    private XmlNamespaceManager _xmlnsMgr;
    private Hashtable _properties;

    public ResinConf(String file)
    {
      _xPathDoc = new XPathDocument(file);
      _docNavigator = _xPathDoc.CreateNavigator();
      _xmlnsMgr = new XmlNamespaceManager(_docNavigator.NameTable);
      _xmlnsMgr.AddNamespace("caucho", "http://caucho.com/ns/resin");
      _xmlnsMgr.AddNamespace("resin", "urn:java:com.caucho.resin");
      _resinConf = file;
    }

    public IList getServers()
    {
      IList result = new List<ResinConfServer>();

      XPathNodeIterator ids = _docNavigator.Select("caucho:resin/caucho:cluster/caucho:server/@id", _xmlnsMgr);
      while (ids.MoveNext()) {
        ResinConfServer server = new ResinConfServer();
        server.ID = ids.Current.Value;
        //cluster@id
        XPathNodeIterator it = ids.Current.SelectAncestors("cluster", "http://caucho.com/ns/resin", false);
        it.MoveNext();
        server.Cluster = it.Current.GetAttribute("id", "");

        result.Add(server);
      }

      XPathNodeIterator multi = _docNavigator.Select("caucho:resin/caucho:cluster/caucho:server-multi", _xmlnsMgr);

      while (multi.MoveNext()) {
        String idPrefix = multi.Current.GetAttribute("id-prefix", "");
        String addressList = multi.Current.GetAttribute("address-list", "");

        XPathNodeIterator it = multi.Current.SelectAncestors("cluster", "http://caucho.com/ns/resin", false);
        it.MoveNext();
        String cluster = it.Current.GetAttribute("id", "");

        String[] addresses = null;

        if (addressList.StartsWith("${")) {
          String addressListKey = addressList.Substring(2, addressList.Length - 3);
          addressList = (String)getProperties()[addressListKey];
        }

        if (addressList == null)
          continue;

        addresses = addressList.Split(new Char[] { ';', ' ' });

        for (int i = 0; i < addresses.Length; i++) {
          ResinConfServer server = new ResinConfServer();
          server.ID = idPrefix + i;
          server.Cluster = cluster;

          result.Add(server);
        }
      }

      return result;
    }

    public String getRootDirectory()
    {
      String rootDirectory = null;

      XPathNavigator nav = _docNavigator.SelectSingleNode("caucho:resin/@root-directory", _xmlnsMgr);
      if (nav != null)
        rootDirectory = nav.Value;

      if (null == rootDirectory || "".Equals(rootDirectory)) {
        nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:root-directory/text()", _xmlnsMgr);
        if (nav != null)
          rootDirectory = nav.Value;
      }

      return rootDirectory;
    }

    public String GetJmxPort(String cluster, String server)
    {
      XPathNodeIterator jvmArgs
        = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/caucho:jvm-arg/text()", _xmlnsMgr);
      while (jvmArgs.MoveNext()) {
        String value = jvmArgs.Current.Value;
        if (value.StartsWith("-Dcom.sun.management.jmxremote.port="))
          return value.Substring(36);
      }

      jvmArgs = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/caucho:jvm-arg/text()", _xmlnsMgr);
      while (jvmArgs.MoveNext()) {
        String value = jvmArgs.Current.Value;
        if (value.StartsWith("-Dcom.sun.management.jmxremote.port="))
          return value.Substring(36);
      }

      return null;
    }

    public String GetWatchDogPort(String cluster, String server)
    {
      XPathNavigator nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/caucho:watchdog-port/text()", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/@watchdog-port", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/caucho:watchdog-port/text()", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      nav = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/@watchdog-port", _xmlnsMgr);
      if (nav != null)
        return nav.Value;

      return null;
    }

    public bool IsDynamicServerEnabled(String cluster)
    {
      XPathNavigator navigator = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster[@id='" + cluster + "']", _xmlnsMgr);

      if (navigator != null && navigator.MoveToFirstChild()) {
        if ("ElasticCloudService".Equals(navigator.LocalName))
          return true;

        while (navigator.MoveToFollowing(XPathNodeType.Element))
          if ("ElasticCloudService".Equals(navigator.LocalName))
            return true;
      }

      navigator = _docNavigator.SelectSingleNode("caucho:resin/caucho:cluster-default", _xmlnsMgr);

      if (navigator != null && navigator.MoveToFirstChild()) {
        if ("ElasticCloudService".Equals(navigator.LocalName))
          return true;

        while (navigator.MoveToFollowing(XPathNodeType.Element))
          if ("ElasticCloudService".Equals(navigator.LocalName))
            return true;
      }

      Hashtable properties = getProperties();

      if (properties != null && "true".Equals((String)properties["elastic_cloud_enable"], StringComparison.CurrentCultureIgnoreCase))
        return true;

      return false;
    }

    public String GetDebugPort(String cluster, String server)
    {
      XPathNodeIterator jvmArgs
        = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server[@id='" + server + "']/caucho:jvm-arg/text()", _xmlnsMgr);
      String debug = null;
      int addressIndex = -1;
      while (jvmArgs.MoveNext()) {
        String value = jvmArgs.Current.Value;
        addressIndex = value.IndexOf("address=");
        if (addressIndex > -1)
          debug = value;
      }

      if (debug == null) {
        jvmArgs = _docNavigator.Select("caucho:resin/caucho:cluster[@id='" + cluster + "']/caucho:server-default/caucho:jvm-arg/text()", _xmlnsMgr);
        while (jvmArgs.MoveNext()) {
          String value = jvmArgs.Current.Value;
          addressIndex = value.IndexOf("address=");
          if (addressIndex > -1)
            debug = value;
        }
      }
      if (debug == null)
        return debug;

      StringBuilder sb = new StringBuilder();
      for (int i = addressIndex + 8; i < debug.Length; i++) {
        if (Char.IsDigit(debug[i]))
          sb.Append(debug[i]);
        else if (sb.Length > 0)
          break;
      }

      if (sb.Length > 0)
        return sb.ToString();
      else
        return null;
    }

    private Hashtable getProperties()
    {
      if (_properties != null)
        return _properties;

      _properties = new Hashtable();

      XPathNavigator nav = _docNavigator.SelectSingleNode("caucho:resin", _xmlnsMgr);
      if (nav.MoveToFirstChild()) {
        do {
          if ("properties".Equals(nav.LocalName)) {
            String path = nav.GetAttribute("path", "");
            parse(path, _resinConf, _properties);
          }
        } while (nav.MoveToFollowing(XPathNodeType.Element));
      }

      return _properties;
    }

    private static void parse(String path, String resinConf, Hashtable properties)
    {
      if (path.StartsWith("cloud:"))
        return;

      String file;
      if (path.StartsWith("${__DIR__}/")) {
        file = resinConf.Substring(0, resinConf.LastIndexOf('\\')) + '\\' + path.Substring(11, path.Length - 11);
      } else {
        file = path;
      }

      if (! File.Exists(file))
        return;

      TextReader reader = null;

      try {
        reader = File.OpenText(file);
        String line;
        while ((line = reader.ReadLine()) != null) {
          if (line.StartsWith("#"))
            continue;

          int sepIdx = line.IndexOf(':');

          if (sepIdx == -1)
            continue;

          String key = line.Substring(0, sepIdx).Trim();
          String value = line.Substring(sepIdx + 1, line.Length - sepIdx - 1).Trim();

          properties.Remove(key);

          properties.Add(key, value);
        }
      } finally {
        if (file != null)
          reader.Close();
      }
    }

    static public ResinConfServer ParseDynamic(String value)
    {
      String[] values = value.Split('-');
      String cluster = values[1];
      String id = values[2]; 
      
      ResinConfServer server = new ResinConfServer();
      server.IsDynamic = true;
      server.Cluster = cluster;
      server.ID = id;

      return server;
    }
  }

  public class ResinConfServer
  {
    public String ID { get; set; }
    public String Cluster { get; set; }
    public Boolean IsDynamic { get; set; }

    public ResinConfServer()
    {
      IsDynamic = false;
    }

    public override string ToString()
    {
      if (IsDynamic) {
        return "dynamic:" + Cluster + ":" + ID;
      } else {

        String id = "".Equals(ID) ? "'default'" : ID;
        String cluster = "".Equals(Cluster) ? "'default'" : Cluster;
        return cluster + ':' + id + "  [cluster@id='" + Cluster + "', server@id='" + ID + "')";
      }
    }
  }
}

