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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.filters;

import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites URL patterns.
 *
 * @since Resin 3.0.0
 */
public class RewriteFilter implements Filter
{
  private static final Logger log = Log.open(RewriteFilter.class);
  private static final L10N L = new L10N(RewriteFilter.class);

  private ServletContext _app;

  private ArrayList<RewriteEntry> _entries = new ArrayList<RewriteEntry>();

  /**
   * Adds a rewrite entry.
   */
  public void addRewrite(RewriteEntry rewrite)
  {
    _entries.add(rewrite);
  }

  public void init(FilterConfig config)
    throws ServletException
  {
    _app = config.getServletContext();
  }

  /**
   * Creates a wrapper to compress the output.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    try {
      String url = req.getRequestURI();

      for (int i = 0; i < _entries.size(); i++) {
        RewriteEntry entry = _entries.get(i);

        Pattern pattern = entry.getRegexp();
        Matcher matcher = pattern.matcher(url);

        if (! matcher.find(0))
          continue;

        String replacement = replace(matcher, entry.getTarget());

        String query = req.getQueryString();

        if (query != null) {
          if (replacement.indexOf('?') > 0)
            replacement = replacement + '&' + query;
          else
            replacement = replacement + '?' + query;
        }

        if (log.isLoggable(Level.FINER))
          log.finer(L.l("forwarding `{0}' to `{1}'",
                        url, replacement));

        if (replacement.startsWith("/")) {
          RequestDispatcher disp = _app.getRequestDispatcher(replacement);

          if (disp != null) {
            disp.forward(request, response);
            return;
          }
        }

        res.sendRedirect(res.encodeRedirectURL(replacement));
        return;
      }

      nextFilter.doFilter(request, response);
    } catch (FileNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);

      res.sendError(404);
    }
  }

  private String replace(Matcher matcher, String target)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < target.length(); i++) {
      char ch = target.charAt(i);

      if (ch != '$' || i == target.length() - 1)
        cb.append(ch);
      else {
        ch = target.charAt(i + 1);
        if (ch >= '0' && ch <= '9') {
          int group = ch - '0';
          cb.append(matcher.group(group));
          i++;
        }
        else if (ch == '$') {
          cb.append('$');
          i++;
        }
        else
          cb.append('$');
      }
    }

    return cb.toString();

  }

  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }

  public static class RewriteEntry {
    private Pattern _pattern;
    private String _target;

    public void setPattern(String pattern)
    {
      _pattern = Pattern.compile(pattern);
    }

    public Pattern getRegexp()
    {
      return _pattern;
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    public String getTarget()
    {
      return _target;
    }
  }
}
