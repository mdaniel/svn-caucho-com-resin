/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.jsf.cfg;

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import javax.xml.bind.annotation.*;

import com.caucho.config.*;
import com.caucho.util.*;

@XmlRootElement(name="faces-config")
public class FacesConfig
{
  @XmlAttribute(name="id")
  private String _id;

  @XmlAttribute(name="version")
  private String _version;

  @XmlElement(name="application")
  private ArrayList<ApplicationConfig> _applicationList
    = new ArrayList<ApplicationConfig>();

  @XmlElement(name="factory")
  private ArrayList<FactoryConfig> _factoryList
    = new ArrayList<FactoryConfig>();
  /*
  @XmlElement(name="component")
  private ArrayList<ComponentConfig> _componentList
    = new ArrayList<ComponentConfig>();

  @XmlElement(name="converter")
  private ArrayList<ConverterConfig> _converterList
    = new ArrayList<ConverterConfig>();

  @XmlElement(name="managed-bean")
  private ArrayList<ManagedBeanConfig> _managedBeanList
    = new ArrayList<ManagedBeanConfig>();

  @XmlElement(name="navigation-rule")
  private ArrayList<NavigationRuleConfig> _navigationRuleList
    = new ArrayList<NavigationRuleConfig>();

  @XmlElement(name="referenced-bean")
  private ArrayList<ReferencedBeanConfig> _referencedBeanList
    = new ArrayList<ReferencedBeanConfig>();

  @XmlElement(name="render-kit")
  private ArrayList<RenderKitConfig> _renderKitList
    = new ArrayList<RenderKitConfig>();

  @XmlElement(name="lifecycle")
  private ArrayList<LifecycleConfig> _lifecycleList
    = new ArrayList<LifecycleConfig>();

  @XmlElement(name="validator")
  private ArrayList<ValidatorConfig> _validatorList
    = new ArrayList<ValidatorConfig>();
  */

  @XmlElement(name="faces-config-extension")
  private void setFacesConfigExtension(BuilderProgram program)
  {
  }
}
