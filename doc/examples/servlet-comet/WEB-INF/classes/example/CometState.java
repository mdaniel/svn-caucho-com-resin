package example;

import com.caucho.servlet.comet.*;

public class CometState {
  private final CometController _controller;

  private int _count;

  public CometState(CometController controller)
  {
    _controller = controller;
  }

  public boolean isActive()
  {
    return _controller.isActive();
  }

  public boolean wake()
  {
    _controller.setAttribute("comet.count", ++_count);

    if (_count <= 10) {
      _controller.wake();
    
      return _controller.isActive();
    }
    else {
      _controller.close();

      return false;
    }
  }
}
