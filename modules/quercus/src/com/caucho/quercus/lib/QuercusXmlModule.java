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

  /**
   * @see boolean XmlClass.xml_parse
   *
   * @param parser
   * @param data
   * @param isFinal
   * @return false if parser == null
   * @throws Exception
   */
  public boolean xml_parse(@NotNull XmlClass parser,
                           @NotNull String data,
                           @Optional("true") boolean isFinal)
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
   * XXX: Should we return warning if separator is
   * anything but ":"???
   *
   * @param env
   * @param outputEncoding
   * @param separator
   * @return namespace aware Xml Parser
   */
  public XmlClass xml_parser_create_ns(Env env,
                                       @Optional String outputEncoding,
                                       @Optional("':'") String separator)
  {
    return new XmlClass(env,outputEncoding,separator);
  }

  /**
   * @see boolean XmlClass.xml_set_element_handler
   *
   * @param parser
   * @param startElementHandler
   * @param endElementHandler
   * @return false if parser == null
   */
  public boolean xml_set_element_handler(@NotNull XmlClass parser,
                                         @NotNull Value startElementHandler,
                                         @NotNull Value endElementHandler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_element_handler(startElementHandler, endElementHandler);
  }

  /**
   * @see boolean XmlClass.xml_set_character_data_handler
   *
   * @param parser
   * @param handler
   * @return false if parser == null
   */
  public boolean xml_set_character_data_handler(@NotNull XmlClass parser,
                                                @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_character_data_handler(handler);
  }

  /**
   *
   * @param parser
   * @param obj
   * @return false if parser == null
   */
  public boolean xml_set_object(@NotNull XmlClass parser,
                                @NotNull Value obj)
  {
    if (parser == null)
      return false;

    return parser.xml_set_object(obj);
  }

  /**
   *
   * @param parser
   * @param handler
   * @return false if parser == null
   */
  public boolean xml_set_processing_instruction_handler(@NotNull XmlClass parser,
                                                        @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_processing_instruction_handler(handler);
  }

  /**
   *
   * @param parser
   * @param handler
   * @return false if parser == null
   */
  public boolean xml_set_default_handler(@NotNull XmlClass parser,
                                         @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_default_handler(handler);
  }

  /**
   *
   * @param parser
   * @param handler
   * @return false if parser == null
   */
  public boolean xml_start_namespace_decl_handler(@NotNull XmlClass parser,
                                                  @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_start_namespace_decl_handler(handler);
  }

  /**
   *
   * @param parser
   * @param handler
   * @return false if parser == null
   */
  public boolean xml_set_end_namespace_decl_handler(@NotNull XmlClass parser,
                                                    @NotNull Value handler)
  {
    if (parser == null)
      return false;

    return parser.xml_set_end_namespace_decl_handler(handler);
  }

  /**
   *
   * @param parser
   * @param data
   * @param valueArray
   * @param indexArray
   * @return false if parser == null
   * @throws Exception
   */
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

  /**
   * stub function.  parser_free taken care of by garbage collection
   *
   * @param parser
   * @return false if parser == null, otherwise true
   */
  public boolean xml_parser_free(@NotNull XmlClass parser)
  {
    if (parser == null)
      return false;
    else
      return true;
  }

  // @todo xml_error_string
  // @todo xml_get_current_byte_index
  // @todo xml_get_current_colmn_number
  // @todo xml_get_current_line_number
  // @todo xml_get_error_code
  // @todo xml_parser_get_option
  // @todo xml_parser_set_option
  // @todo xml_set_external_entity_ref_handler
  // @todo xml_set_notation_decl_handler
  // @todo xml_set_unparsed_entity_decl_handler
}

