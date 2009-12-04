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

package com.caucho.config.inject;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.type.*;
import com.caucho.config.types.*;
import com.caucho.config.gen.*;
import com.caucho.util.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.enterprise.inject.spi.Bean;

/**
 * Configuration for a SimpleBean method, e.g. for an XML configuration of
 * a @Produces
 */
public class SimpleBeanField
{
  private static final L10N L = new L10N(SimpleBeanField.class);
  
  private Field _field;

  private Annotation []_annotations;

  public SimpleBeanField(Field field, Annotation []annotations)
  {
    _field = field;
    _annotations = annotations;
  }

  public Field getField()
  {
    return _field;
  }

  public Annotation []getAnnotations()
  {
    return _annotations;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _field.getName() + "]";
  }
}
