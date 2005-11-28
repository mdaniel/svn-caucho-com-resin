/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package javax.mail;

import java.util.ArrayList;

/**
 * Represents attributes the message wants to prefetch.
 */
public class FetchProfile {
  private ArrayList _items = new ArrayList();

  /**
   * Adds an item.
   */
  public void add(Item item)
  {
    _items.add(item);
  }

  /**
   * Adds a header to the attributes.
   */
  public void add(String header)
  {
    add(new Item(new String [] { header }));
  }

  /**
   * Returns true if the fetch profile contains the item.
   */
  public boolean contains(Item item)
  {
    return _items.contains(item);
  }

  /**
   * Returns true if the fetch profile contains the item.
   */
  public boolean contains(String header)
  {
    for (int i = 0; i < _items.size(); i++) {
      Item item = (Item) _items.get(i);

      String []headers = item.getHeaders();

      for (int j = 0; j < headers.length; j++)
	if (headers[j].equals(header))
	  return true;
    }

    return false;
  }

  /**
   * Returns an array of the items.
   */
  public Item []getItems()
  {
    Item []items = new Item[_items.size()];

    _items.toArray(items);

    return items;
  }

  /**
   * Returns the header names.
   */
  public String []getHeaderNames()
  {
    ArrayList headerList = new ArrayList();

    for (int i = 0; i < _items.size(); i++) {
      Item item = (Item) _items.get(i);

      String []itemHeaders = item.getHeaders();

      for (int j = 0; j < itemHeaders.length; j++)
	headerList.add(itemHeaders[j]);
    }

    String []headers = new String[headerList.size()];

    headerList.toArray(headers);

    return headers;
  }

  public static class Item {
    private static final Item ENVELOPE
      = new Item(new String [] {
	"From", "To", "Cc", "Bcc", "ReplyTo", "Subject", "Date"
	});
    private static final Item CONTENT_INFO
      = new Item(new String [] {
	"ContentType", "ContentDisposition", "ContentDescription",
	"Size", "LineCount"
      });
    private static final Item FLAGS
      = new Item(new String[] {"Flags"});
    
    private String []_headers;

    protected Item(String []headers)
    {
      _headers = headers;
    }

    public String []getHeaders()
    {
      return _headers;
    }
  }
}
