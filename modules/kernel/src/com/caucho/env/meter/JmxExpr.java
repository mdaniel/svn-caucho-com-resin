/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.env.meter;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.jmx.Jmx;
import com.caucho.util.L10N;

abstract public class JmxExpr {
  private static final L10N L = new L10N(JmxCalculationMeterImpl.class);
  private static final Logger log = Logger.getLogger(JmxCalculationMeterImpl.class.getName());
  ;
  abstract protected void sample();
    
  abstract protected double calculate();
  
  abstract protected static class JmxContainerExpr extends JmxExpr {
    private ArrayList<JmxExpr> _children = new ArrayList<JmxExpr>(); 

    public void addValue(JmxValue value)
    {
      add(value);
    }
    
    public void addDelta(JmxDelta value)
    {
      add(value);
    }
    
    public void addRate(JmxRate expr)
    {
      add(expr);
    }
    
    public void addRatio(JmxRatio expr)
    {
      add(expr);
    }
    
    public void addAdd(JmxAdd expr)
    {
      add(expr);
    }
    
    public void add(JmxExpr expr)
    {
      _children.add(expr);
    }
    
    protected ArrayList<JmxExpr> getChildren()
    {
      return _children;
    }
    
    protected void sample()
    {
      int size = _children.size();
      
      for (int i = 0; i < size; i++) {
        _children.get(i).sample();
      }
    }
  }
  
  public static class JmxValue extends JmxExpr {
    private MBeanServer _server;
    private ObjectName _objectName;
    private String _attribute;
    
    private double _lastSample;

    public JmxValue()
    {
      _server = Jmx.getGlobalMBeanServer();
    }
    
    public void setObjectName(String name)
    {
      try {
        _objectName = Jmx.getObjectName(name);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    public void setAttribute(String name)
    {
      _attribute = name;
    }
    
    @PostConstruct
    public void init()
    {
      if (_objectName == null)
        throw new ConfigException(L.l("objectName is required for JMX <value>"));
      
      if (_attribute == null)
        throw new ConfigException(L.l("attribute is required for JMX <value>"));
    }

    /**
     * Polls the statistics attribute.
     */
    @Override
    protected void sample()
    {
      try {
        Object value = _server.getAttribute(_objectName, _attribute);

        if (value == null) {
          _lastSample =  0;
          return;
        }
        
        _lastSample = ((Number) value).doubleValue();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);

        _lastSample = 0;
      }
    }
    
    @Override
    public double calculate()
    {
      return _lastSample;
    }
   
  }
  
  public static class JmxDelta extends JmxExpr {
    private MBeanServer _server;
    private ObjectName _objectName;
    private String _attribute;
    private boolean _isOptional;
    
    private double _prevSample;
    private double _lastSample;

    public JmxDelta()
    {
      _server = Jmx.getGlobalMBeanServer();
    }
    
    public void setObjectName(String name)
    {
      try {
        _objectName = Jmx.getObjectName(name);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    public void setAttribute(String name)
    {
      _attribute = name;
    }

    public boolean isOptional()
    {
      return _isOptional;
    }

    @Configurable
    public void setOptional(boolean optional)
    {
      _isOptional = optional;
    }

    @PostConstruct
    public void init()
    {
      if (_objectName == null)
        throw new ConfigException(L.l("objectName is required for JMX <value>"));
      
      if (_attribute == null)
        throw new ConfigException(L.l("attribute is required for JMX <value>"));
    }

    /**
     * Polls the statistics attribute.
     */
    @Override
    protected void sample()
    {
      try {
        _prevSample = _lastSample;
        
        Object value = _server.getAttribute(_objectName, _attribute);

        if (value == null) {
          _lastSample =  0;
          return;
        }
        
        _lastSample = ((Number) value).doubleValue();
      } catch (Exception e) {
        if (isOptional()
            && e instanceof javax.management.InstanceNotFoundException)
          log.log(Level.FINEST, e.toString(), e);
        else
          log.log(Level.FINE, e.toString(), e);

        _lastSample = 0;
      }
    }
    
    @Override
    public double calculate()
    {
      return _lastSample - _prevSample;
    }
  }
  
  public static class JmxRate extends JmxContainerExpr {
    @Override
    public double calculate()
    {
      ArrayList<JmxExpr> children = getChildren();
      int size = children.size();
      
      double total = 0;
      
      for (int i = 0; i < size; i++) {
        total += children.get(i).calculate();
      }
      
      if (total == 0)
        return 0;
      
      JmxExpr firstChild = children.get(0);
      
      return firstChild.calculate() / total;
    }
  }
  
  public static class JmxRatio extends JmxContainerExpr {
    @Override
    public double calculate()
    {
      ArrayList<JmxExpr> children = getChildren();
      int size = children.size();
      
      double total = 0;
      
      for (int i = 1; i < size; i++) {
        total += children.get(i).calculate();
      }
      
      if (total == 0 || size == 0)
        return 0;
      
      JmxExpr firstChild = children.get(0);
      
      return firstChild.calculate() / total;
    }
  }
  
  public static class JmxAdd extends JmxContainerExpr {
    @Override
    public double calculate()
    {
      ArrayList<JmxExpr> children = getChildren();
      int size = children.size();
      
      double total = 0;
      
      for (int i = 0; i < size; i++) {
        total += children.get(i).calculate();
      }
      
      return total;
    }
  }
}
