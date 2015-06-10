/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.cloud.jmx;

import java.util.HashMap;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import com.caucho.amp.AmpSystem;
import com.caucho.amp.ServiceManagerAmp;
import com.caucho.amp.hamp.ClientHamp;
import com.caucho.baratine.client.ServiceManagerClient;
import com.caucho.bartender.BartenderSystem;
import com.caucho.bartender.ServerBartender;
import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.StringValueDeserializer;
import com.caucho.hessian.io.StringValueSerializer;
import com.caucho.util.L10N;

/**
 * JMX Service API
 */
public class JmxClient
{
  private static final L10N L = new L10N(JmxClient.class);

  private JmxActorApi _client;

  private String _address;

  private ClientHamp _rampClient;

  JmxClient(String serverId)
  {
    ServerBartender server
      = BartenderSystem.getCurrent().findServerByName(serverId);

    if (server == null)
      throw new IllegalArgumentException(L.l("'{0}' is an unknown remote server in the cluster",
                                             serverId));
    _address = "champ://" + server.getId() + "/jmx";

    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    //_conn = broker.getConnection("admin.resin", cookie, "jmx");
    _client = rampManager.lookup(_address).as(JmxActorApi.class);
  }
  
  JmxClient(String host, int port, String user, String password)
  {
    String url = "hamp://" + host + ":" + port + "/hamp";
    String selfHost = "localhost";
    
    _rampClient = new ClientHamp(url, user, password);
    _rampClient.setVirtualHost("admin.resin");
    // client.connect(user, password);
    _rampClient.connect();
    
    _client = _rampClient.lookup("remote:///jmx").as(JmxActorApi.class);
  }

  protected void initExtSerializerFactory(ExtSerializerFactory factory)
  {
    factory.addSerializer(ObjectName.class,
                          new StringValueSerializer());

    factory.addDeserializer(ObjectName.class,
                            new StringValueDeserializer(ObjectName.class));
  }

  public MBeanInfo getMBeanInfo(String name)
  {
    return _client.getMBeanInfo(name);
  }

  @SuppressWarnings("unchecked")
  public HashMap lookup(String name)
  {
    return _client.lookup(name);
  }

  public String []query(String pattern)
  {
    return _client.query(pattern);
  }

  public Object invoke(String objectName, String methodName,
                       Object []args, String []sig)
  {
    return _client.invoke(objectName, methodName, args, sig);
  }
  
  public void close()
  {
    ServiceManagerClient client = _rampClient;
    _rampClient = null;
    
    if (client != null) {
      client.close();
    }
  }

  @Override
  public String toString()
  {
    // return getClass().getSimpleName() + "[" + _client + "]";
    return getClass().getSimpleName() + "[" + _client + "]";
  }
}
