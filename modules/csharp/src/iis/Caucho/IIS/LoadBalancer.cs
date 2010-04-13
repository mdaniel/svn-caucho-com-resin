using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net.Sockets;

namespace Caucho.IIS
{
  public class LoadBalancer
  {
    HmuxChannelFactory _pool;

    public LoadBalancer()
    {
      _pool = new HmuxChannelFactory();
    }

    public HmuxChannel OpenServer(String sessionId)
    {
      Socket hmuxSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
      hmuxSocket.Connect("localhost", 6810);
      HmuxChannel channel = new HmuxChannel(hmuxSocket, _pool);

      return channel;
    }
  }

}
