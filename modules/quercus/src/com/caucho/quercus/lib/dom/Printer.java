/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.Logger;

public class Printer {
  private static final Logger log = Logger.getLogger(Printer.class.getName());
  private static final L10N L = new L10N(Printer.class);

  private final DOMDocument _document;

  public Printer(DOMDocument document)
  {
    _document = document;
  }

  /**
   * @param out
   * @return true if successful, false if there is an encoding error
   * @throws IOException
   */
  public boolean print(WriteStream out)
    throws IOException
  {
    String version = _document.getVersion();
    String encoding = _document.getEncoding();

    out.print("<?xml");
    printAttribute(out, "version", version);
    printAttribute(out, "encoding", encoding);
    out.println("?>");

    if (encoding != null) {
      out.setEncoding(encoding);

      // XXX: how to know if encoding is valid?
      // if ( ??? )
      // log.log(Level.FINE, L.l("unsupported encoding `{0}'", encoding));
      // return false;
      // }
    }

    return true;
  }

  /**
   * Print a leading space, and an attribute name=value pair.
   *
   * @param out the WriteStream
   * @param name the attribute name
   * @param value the value, null causes the attribute to not be printed.
   */
  private void printAttribute(WriteStream out, String name, String value)
    throws IOException
  {
    if (value == null)
      return;

    out.print(' ');
    out.print(name);
    out.print('=');

    boolean isSingleQuote = value.indexOf('\'') >= 0;
    boolean isDoubleQuote = value.indexOf('"') >= 0;

    boolean useSingleQuote = isDoubleQuote && !isSingleQuote;
    boolean escapeDoubleQuote = !useSingleQuote && isDoubleQuote;

    out.print(useSingleQuote ? '\'' : '"');

    if (escapeDoubleQuote) {
      final int length = value.length();

      for (int i = 0; i < length; i++) {
        char ch = value.charAt(i);

        if (ch == '"')
          out.print("&quot;");
        else
          out.print(ch);
      }
    }
    else {
      out.print(value);
    }

    out.print(useSingleQuote ? '\'' : '"');
  }
}
