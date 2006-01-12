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

package com.caucho.quercus.lib;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Reference;

import com.caucho.quercus.env.*;

/**
 * PHP XML
 */
public class QuercusXmlModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusXmlModule.class);

  /**
   * Converts from iso-8859-1 to utf8
   */
  public static Value utf8_encode(String str)
  {
    // XXX: need to make a marker for the string

    return new StringValue(str);
  }

  /**
   * Converts from utf8 to iso-8859-1
   */
  public static Value utf8_decode(String str)
  {
    // XXX: need to make a marker for the string

    return new StringValue(str);
  }

  public boolean xml_parse(@NotNull XmlClass parser,
                           @NotNull String data,
                           @Optional boolean isFinal)
    throws Exception
  {
    if (parser == null)
      return false;

    return parser.xml_parse(data, isFinal);
  }

  /**
   * returns a new Xml Parser
   */
  public XmlClass xml_parser_create(Env env,
                                    @Optional String outputEncoding)
  {
    return new XmlClass(env,outputEncoding,null);
  }

  /**
   *
   * @param env
   * @param outputEncoding
   * @param separator
   * @return namespace aware Xml Parser
   */
  public XmlClass xml_parser_create_ns(Env env,
                                       @Optional String outputEncoding,
                                       @Optional(":") String separator)
  {
    return new XmlClass(env,outputEncoding,separator);
  }

  public boolean xml_set_element_handler(@NotNull XmlClass parser,
                                         @NotNull Value startElementHandler,
                                         @NotNull Value endElementHandler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_element_handler(startElementHandler, endElementHandler);
  }

  public boolean xml_set_character_data_handler(@NotNull XmlClass parser,
                                                @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_character_data_handler(handler);
  }

  public boolean xml_set_processing_instruction_handler(@NotNull XmlClass parser,
                                                        @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_processing_instruction_handler(handler);
  }

  public boolean xml_set_default_handler(@NotNull XmlClass parser,
                                         @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_default_handler(handler);
  }

  public boolean xml_start_namespace_decl_handler(@NotNull XmlClass parser,
                                                  @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_start_namespace_decl_handler(handler);
  }

  public boolean xml_set_end_namespace_decl_handler(@NotNull XmlClass parser,
                                                    @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_end_namespace_decl_handler(handler);
  }

  public int xml_parse_into_struct(@NotNull XmlClass parser,
                                   @NotNull String data,
                                   @Reference Value valueArray,
                                   @Optional @Reference Value indexArray)
    throws Exception
  {
    if (parser == null)
      return 0;

    return parser.xml_parse_into_struct(data, valueArray, indexArray);
  }
}

