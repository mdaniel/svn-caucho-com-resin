package example;

import com.caucho.servlets.comet.*;

public class CometItem {
  private final CometController _controller;

  private int _count;

  public CometItem(CometController controller)
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
