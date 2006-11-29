/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic.context;

import com.caucho.portal.generic.Renderer;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class RendererResponseHandler
  extends AbstractResponseHandler
{
  protected static final Logger log = 
    Logger.getLogger(RendererResponseHandler.class.getName());

  private ConnectionContext _context;
  private Renderer _renderer;
  private RenderRequest _renderRequest;
  private RenderResponse _renderResponse;
  private String _namespace;

  private PrintWriter _writer;
  private boolean _writerIsWrapped;
  private OutputStream _outputStream;
  private boolean _outputStreamIsWrapped;

  private boolean _wasReset;

  public RendererResponseHandler()
  {
  }

  public RendererResponseHandler( ConnectionContext context,
                                  ResponseHandler responseHandler, 
                                  Renderer renderer,
                                  RenderRequest renderRequest,
                                  RenderResponse renderResponse,
                                  String namespace )
  {
    open(context, responseHandler, renderer, renderRequest, renderResponse, namespace);
  }

  public void open( ConnectionContext context,
                    ResponseHandler responseHandler, 
                    Renderer renderer,
                    RenderRequest renderRequest,
                    RenderResponse renderResponse,
                    String namespace )
  {
    if (_renderer != null)
      throw new IllegalStateException("already open");

    super.open(responseHandler);

    _context = context;
    _renderer = renderer;
    _renderRequest = renderRequest;
    _renderResponse = renderResponse;
    _namespace = namespace;
  }

  public void finish()
    throws IOException
  {
    try {
      finishWriter(false);
      finishOutputStream(false);
    }
    catch (Exception ex) {
      setError(ex);
    }
    finally {
      _wasReset = false;
      _writer = null;
      _writerIsWrapped = false;
      _outputStream = null;
      _outputStreamIsWrapped = false;
      _namespace = null;
      _renderRequest = null;
      _renderResponse = null;
      _renderer = null;
    }
  }

  public void finishWriter( boolean isDiscarded )
    throws IOException
  {
    if (_writerIsWrapped) {
      PrintWriter writer = _writer;
      _writer = null;
      _writerIsWrapped = false;

      _renderer.finish( writer, _renderRequest, _namespace, isDiscarded );
    }
  }

  public void finishOutputStream( boolean isDiscarded )
    throws IOException
  {
    if (_outputStreamIsWrapped) {
      OutputStream outputStream = _outputStream;
      _outputStream = null;
      _outputStreamIsWrapped = false;

      _renderer.finish( outputStream, _renderRequest, _namespace, isDiscarded );
    }
  }

  public String getDefaultContentType()
  {
    return _renderer == null ? null : _renderer.getDefaultContentType();
  }

  public boolean isAlwaysWrite()
  {
    return _renderer != null && _renderer.isAlwaysWrite();
  }

  public PrintWriter getWriter()
    throws IOException
  {
    if (_writer != null)
      return _writer;

    PrintWriter writer = super.getWriter();

    if (_renderer != null) {

      PrintWriter rendererWriter 
        = _renderer.getWriter( writer, _renderRequest, _namespace );

      if (rendererWriter != null) {
        writer = rendererWriter;
        _writerIsWrapped = true;
      }
    }

    return _writer = writer;
  }

  public boolean isWriter()
  {
    return _writerIsWrapped;
  }

  public boolean isAlwaysStream()
  {
    return _renderer != null && _renderer.isAlwaysStream();
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    if (_outputStream != null)
      return _outputStream;

    OutputStream outputStream = super.getOutputStream();

    if (_renderer != null) {
      OutputStream rendererStream 
        = _renderer.getOutputStream(outputStream, _renderRequest, _namespace );

      if (rendererStream != null) {
        outputStream = rendererStream;
        _outputStreamIsWrapped = true;
      }
    }

    return _outputStream = outputStream;
  }

  public boolean isOutputStream()
  {
    return _outputStreamIsWrapped;
  }

  public void flushBuffer() 
    throws IOException
  {
    if (!_wasReset && !isWriter() && !isOutputStream()) {
      if (_renderer.isAlwaysWrite()) {
        getWriter();
      }
      else if (_renderer.isAlwaysStream()) {
        getOutputStream();
      }
    }
  }

  public void resetBuffer()
  {
    _wasReset = true;

    try {
      finishWriter(true);
      finishOutputStream(true);
    }
    catch (IOException ex) {
      setError(ex);
    }
  }

  public void reset()
  {
    _wasReset = true;

    try {
      finishWriter(true);
      finishOutputStream(true);
    }
    catch (IOException ex) {
      setError(ex);
    }
  }

  /**
   * @param renderAgain if true, immediately call getWriter()
   * or getOutputStream() again
   */
  public void reset(boolean renderAgain)
  {
    try {
      boolean isWriter = isWriter();
      boolean isOutputStream = isOutputStream();

      finishWriter(true);
      finishOutputStream(true);

      if (isWriter)
        getWriter();

      if (isOutputStream)
        getOutputStream();

    }
    catch (IOException ex) {
      setError(ex);
    }
  }
}
