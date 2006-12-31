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

package com.caucho.jsf.html;

import java.io.*;
import java.util.*;

import javax.faces.context.*;
import javax.faces.render.*;

public class HtmlBasicRenderKit extends RenderKit {
  private HashMap<Key,Renderer> _rendererMap
    = new HashMap<Key,Renderer>();

  private Key _key = new Key();

  public HtmlBasicRenderKit()
  {
    addRenderer("javax.faces.Input", "javax.faces.Secret",
		HtmlInputSecretRenderer.RENDERER);
    
    addRenderer("javax.faces.Input", "javax.faces.Text",
		HtmlInputTextRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Format",
		HtmlOutputFormatRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Label",
		HtmlOutputLabelRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Link",
		HtmlOutputLinkRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Text",
		HtmlOutputTextRenderer.RENDERER);
    
    addRenderer("javax.faces.Panel", "javax.faces.Grid",
		HtmlPanelGridRenderer.RENDERER);
  }
  
  public void addRenderer(String family,
			  String rendererType,
			  Renderer renderer)
  {
    _rendererMap.put(new Key(family, rendererType), renderer);
  }
  
  public Renderer getRenderer(String family,
			      String rendererType)
  {
    if (family == null || rendererType == null)
      return null;
    
    _key.init(family, rendererType);
    
    return _rendererMap.get(_key);
  }

  public ResponseStateManager getResponseStateManager()
  {
    return null;
  }

  public ResponseWriter createResponseWriter(Writer writer,
					     String contentTypeList,
					     String characterEncoding)
  {
    return new HtmlResponseWriter(writer, contentTypeList, characterEncoding);
  }

  public ResponseStream createResponseStream(OutputStream out)
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "HtmlBasicRenderKit[]";
  }

  static final class Key {
    private String _family;
    private String _type;

    Key()
    {
    }
      
    Key(String family, String type)
    {
      _family = family;
      _type = type;
    }

    public void init(String family, String type)
    {
      _family = family;
      _type = type;
    }

    @Override
    public int hashCode()
    {
      if (_type != null)
	return _family.hashCode() * 65521 + _type.hashCode();
      else
	return _family.hashCode();
    }

    public boolean equals(Object o)
    {
      Key key = (Key) o;

      if (_type != null)
	return _family.equals(key._family) && _type.equals(key._type);
      else
	return _family.equals(key._family) && key._type != null;
    }

    public String toString()
    {
      return "Key[" + _family + ", " + _type + "]";
    }
  }
}
