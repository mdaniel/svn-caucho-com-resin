/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.relaxng;

import com.caucho.v5.relaxng.pattern.GrammarPattern;
import com.caucho.v5.relaxng.program.Item;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;

/**
 * JARV Schema implementation
 */
public class SchemaImpl implements Schema {
  protected static final L10N L = new L10N(SchemaImpl.class);

  private String _filename;
  
  private LruCache<Object,Item> _programCache
    = new LruCache<Object,Item>(1024);
  
  private Item _startItem;

  public SchemaImpl(GrammarPattern grammar)
    throws RelaxException
  {
    _startItem = grammar.getStart().createItem(grammar);

    if (_startItem == null)
      throw new RelaxException(L.l("Expected a start item."));
  }
  
  /**
   * Returns the program.
   */
  public Item getStartItem()
  {
    return _startItem;
  }

  public LruCache<Object,Item> getProgramCache()
  {
    return _programCache;
  }

  /**
   * Sets the schema filename.
   */
  public void setFilename(String filename)
  {
    _filename = filename;
  }
  
  /**
   * Creates a verifier from the schema.
   */
  public Verifier newVerifier()
  {
    return new VerifierImpl(this);
  }

  public String toString()
  {
    if (_filename != null)
      return "SchemaImpl[" + _filename + "]";
    else
      return "SchemaImpl[]";
  }
}