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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;

/**
 * Host/Inet utilities.
 */
public final class HostUtil {
  private static final Logger log = Logger.getLogger(HostUtil.class.getName());
  
  private static final TimedCache<String,ArrayList<NetworkInterface>> _cache;
  
  private HostUtil() {}
  
  public static String getLocalHostName()
  {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    for (InetAddress addr : getLocalAddresses()) {
      if (isPrivateNetwork(addr)) {
        return addr.getHostAddress();
      }
    }
    
    return "127.0.0.1";
  }
  
  public static String getLocalHostAddress()
  {
    for (InetAddress addr : getLocalAddresses()) {
      if (isPrivateNetwork(addr)) {
        return addr.getHostAddress();
      }
    }
    
    return "127.0.0.1";
  }

  public static boolean isPrivateNetwork(InetAddress addr)
  {
    byte []bytes = addr.getAddress();

    if (bytes.length != 4)
      return false;
    
    if (bytes[0] == 10)
      return true;
    
    if ((bytes[0] & 0xff) == 172 && 16 <= bytes[1] &&  bytes[1] <= 31)
      return true;
        
    if ((bytes[0] & 0xff) == 192 && (bytes[1] & 0xff) == 168)
      return true;

    return false;
  }
  
  public static boolean isLinkLocalNetwork(InetAddress addr)
  {
    byte []bytes = addr.getAddress();
    
    if (bytes.length != 4)
      return false;
    
    if ((bytes[0] & 0xff) == 169 && (bytes[1] & 0xff) == 254)
      return true;

    return false;
  }
  
  public static ArrayList<InetAddress> getLocalAddresses()
  {
    ArrayList<InetAddress> localAddresses = new ArrayList<InetAddress>();
    
    synchronized (HostUtil.class) {
      try {
        for (NetworkInterface iface : getNetworkInterfaces()) {
          Enumeration<InetAddress> addrEnum = iface.getInetAddresses();
      
          while (addrEnum.hasMoreElements()) {
            InetAddress addr = addrEnum.nextElement();
        
            localAddresses.add(addr);
          }
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    Collections.sort(localAddresses, new LocalIpCompare());
    
    return localAddresses;
  }

  /**
   * @return
   */
  public static synchronized 
  ArrayList<NetworkInterface> getNetworkInterfaces()
  {
    ArrayList<NetworkInterface> interfaceList
      = (ArrayList<NetworkInterface>) _cache.get("network-interfaces");
    
    if (interfaceList == null) {
      interfaceList = new ArrayList<NetworkInterface>();
      
      try {
        Enumeration<NetworkInterface> ifaceEnum
          = NetworkInterface.getNetworkInterfaces();
    
        while (ifaceEnum.hasMoreElements()) {
          NetworkInterface iface = ifaceEnum.nextElement();

          interfaceList.add(iface);
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      
      _cache.put("network-interfaces", interfaceList);
    }

    return new ArrayList<NetworkInterface>(interfaceList);
  }
  
  static class LocalIpCompare implements Comparator<InetAddress>
  {
    @Override
    public int compare(InetAddress a, InetAddress b)
    {
      byte []bytesA = a.getAddress();
      byte []bytesB = b.getAddress();
      
      if (bytesA[0] == bytesB[0]) {
      }
      else if (bytesA[0] == 0) {
        return 1;
      }
      else if (bytesB[0] == 0) {
        return -1;
      }
      else if (bytesA[0] == 127) {
        return 1;
      }
      else if (bytesB[0] == 127) {
        return -1;
      }
      
      if (bytesA.length != bytesB.length) {
        return bytesA.length - bytesB.length;
      }
      
      for (int i = 0; i < bytesA.length; i++) {
        if (bytesA[i] != bytesB[i]) {
          return bytesA[i] - bytesB[i];
        }
      }

      return 0;
    }
    
  }
  
  static {
    long timeout = CurrentTime.isTest() ? Integer.MAX_VALUE : 120000;
  
    _cache = new TimedCache<String,ArrayList<NetworkInterface>>(128, timeout);
  }
}
