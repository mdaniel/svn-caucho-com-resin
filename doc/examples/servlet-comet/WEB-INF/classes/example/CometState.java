package example;

import javax.servlet.*;

public class CometState {
  private final AsyncContext _async;

  private int _count;

  public CometState(AsyncContext async)
  {
    _async = async;
  }

  public boolean isClosed()
  {
    ServletRequest request = _async.getRequest();

    return request == null || ! request.isAsyncStarted();
  }

  public boolean wake()
  {
    _async.getRequest().setAttribute("comet.count", ++_count);

    if (_count <= 10) {
      _async.dispatch();

      return ! isClosed();
    }
    else {
      _async.complete();

      return false;
    }
  }
}
