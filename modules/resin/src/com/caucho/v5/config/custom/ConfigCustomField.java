/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.custom;

import com.caucho.v5.config.program.ConfigProgram;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

/**
 * Custom bean configured by namespace
 */
public class ConfigCustomField {
  private Field _field;
  
  private ArrayList<Annotation> _annotationList
    = new ArrayList<>();

  private ArrayList<ConfigProgram> _programList
    = new ArrayList<>();

  public ConfigCustomField(Field field)
  {
    _field = field;
  }

  public Field getField()
  {
    return _field;
  }

  public void addProgramBuilder(ConfigProgram program)
  {
    _programList.add(program);
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
