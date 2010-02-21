package example;

import javax.servlet.*;

public class CometState {
  private ServletRequest _request;

  private int _count;

  public CometState(ServletRequest request)
  {
    _request = request;
  }

  public boolean isClosed()
  {
    return _request == null || ! _request.isAsyncStarted();
  }

  public boolean wake()
  {
    _request.setAttribute("comet.count", ++_count);

    AsyncContext async = _request.getAsyncContext();

    if (_count <= 10 && async != null) {
      async.dispatch();

      return ! isClosed();
    }
    else if (async != null) {
      async.complete();
    }

    return false;
  }
}
