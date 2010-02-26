using System;
using System.Collections.Generic;
using System.Collections;
using System.Linq;
using System.Text;
using System.Xml;
using System.Xml.XPath;

namespace Caucho
{
  public class ResinConf
  {
    private XPathDocument _xPathDoc;
    private XPathNavigator _docNavigator;
    private XmlNamespaceManager _xmlnsMgr;

    public ResinConf(String file)
    {
      _xPathDoc = new XPathDocument(file);
      _docNavigator = _xPathDoc.CreateNavigator();
      _xmlnsMgr = new XmlNamespaceManager(_docNavigator.NameTable);
      _xmlnsMgr.AddNamespace("caucho", "http://caucho.com/ns/resin");
    }

    public IList getServerIds()
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
  }


  public class ResinConfServer
  {
    public String ID { get; set; }
    public String Cluster { get; set; }
    public Boolean IsDynamic { get; set; }
    public String Address { get; set; }
    public int Port { get; set; }

    public ResinConfServer()
    {
      IsDynamic = false;
    }

    public override string ToString()
    {
      if (IsDynamic) {
        return "-dynamic " + Address + ":" + Port;
      } else {

        String id = "".Equals(ID) ? "'default'" : ID;
        String cluster = "".Equals(Cluster) ? "'default'" : Cluster;
        return cluster + ':' + id + "  [cluster@id='" + Cluster + "', server@id='" + ID + "')";
      }
    }
  }
}

