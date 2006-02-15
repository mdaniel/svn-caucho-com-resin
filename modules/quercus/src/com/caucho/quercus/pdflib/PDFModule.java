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

package com.caucho.quercus.pdflib;

import java.io.IOException;

import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;

import com.caucho.util.L10N;

/**
 * PHP PDF routines.
 */
public class PDFModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(PDFModule.class);

  private static final Logger log =
    Logger.getLogger(PDFModule.class.getName());

  /**
   * Returns the height of an image.
   */
  public static String pdf_get_errmsg(PDF pdf)
  {
    if (pdf != null)
      return pdf.get_errmsg();
    else
      return "";
  }

  /**
   * Returns the height of an image.
   */
  public static int pdf_get_errnum(PDF pdf)
  {
    if (pdf != null)
      return pdf.get_errnum();
    else
      return 0;
  }

  /**
   * Returns the height of an image.
   */
  public static double pdf_get_image_height(PDFImage image)
  {
    if (image != null)
      return image.get_height();
    else
      return 0;
  }

  /**
   * Returns the width of an image.
   */
  public static double pdf_get_image_width(PDFImage image)
  {
    if (image != null)
      return image.get_width();
    else
      return 0;
  }

  /**
   * Creates a new PDF object.
   */
  public static PDF pdf_new(Env env)
    throws IOException
  {
    return new PDF(env);
  }

  /**
   * Starts the document.
   */
  public static boolean pdf_begin_document(@NotNull PDF pdf,
					   String fileName,
					   @Optional String optList)
    throws IOException
  {
    if (pdf != null)
      return pdf.begin_document(fileName, optList);
    else
      return false;
  }

  /**
   * Starts the page.
   */
  public static boolean pdf_begin_page(@NotNull PDF pdf,
				       double w, double h,
				       @Optional String optList)
    throws IOException
  {
    if (pdf != null)
      return pdf.begin_page(w, h);
    else
      return false;
  }

  /**
   * Starts the page.
   */
  public static boolean pdf_end_page(@NotNull PDF pdf)
    throws IOException
  {
    if (pdf != null)
      return pdf.end_page();
    else
      return false;
  }

  /**
   * Starts the document.
   */
  public static boolean pdf_end_document(Env env,
					 @NotNull PDF pdf,
					 @Optional String optList)
    throws IOException
  {
    if (pdf != null)
      return pdf.end_document(env, optList);
    else
      return false;
  }

  /**
   * Fills
   */
  public boolean pdf_fill_stroke(@NotNull PDF pdf)
    throws IOException
  {
    if (pdf != null)
      return pdf.fill_stroke();
    else
      return false;
  }

  /**
   * Returns the result as a string.
   */
  public static Value pdf_get_buffer(@NotNull PDF pdf)
  {
    if (pdf != null)
      return pdf.get_buffer();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Creates a rectangle
   */
  public boolean pdf_rect(@NotNull PDF pdf,
			  double x, double y, double width, double height)
    throws IOException
  {
    if (pdf != null)
      return pdf.rect(x, y, width, height);
    else
      return false;
  }

  /**
   * Sets the dashing
   *
   * @param b black length
   * @param w which length
   */
  public boolean pdf_setdash(@NotNull PDF pdf, double b, double w)
    throws IOException
  {
    if (pdf != null)
      return pdf.setdash(b, w);
    else
      return false;
  }
}
