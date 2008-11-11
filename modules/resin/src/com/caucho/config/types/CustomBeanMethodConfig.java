/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.types;

import com.caucho.config.*;
import com.caucho.config.program.*;
import com.caucho.config.type.*;
import com.caucho.config.j2ee.*;
import com.caucho.jca.program.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;
import com.caucho.xml.QName;

import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

import javax.annotation.*;

import javax.resource.spi.*;

import javax.webbeans.*;

import org.w3c.dom.Node;

/**
 * Custom bean configured by namespace
 */
public class CustomBeanMethodConfig {
  private static final Logger log
    = Logger.getLogger(CustomBeanMethodConfig.class.getName());
  
  private static final L10N L = new L10N(CustomBeanMethodConfig.class);

  private Method _method;
  
  private ArrayList<Annotation> _annotationList
    = new ArrayList<Annotation>();

  public CustomBeanMethodConfig(Method method)
  {
    _method = method;
  }

  public Method getMethod()
  {
    return _method;
  }

  public void addAnnotation(Annotation ann)
  {
    _annotationList.add(ann);
  }

  public Annotation []getAnnotations()
  {
    Annotation []annotations = new Annotation[_annotationList.size()];
    _annotationList.toArray(annotations);

    return annotations;
  }
}
