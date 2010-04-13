using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Diagnostics;

namespace Caucho.IIS
{
  public class HmuxChannelFactory
  {
    internal void Busy()
    {
      Trace.TraceInformation("HmuxChannelFactory.Busy() NYI");
    }

    internal void FailSocket()
    {
      Trace.TraceInformation("HmuxChannelFactory.FailSocket() NYI");
    }

    internal void SetCpuLoadAvg(double loadAvg)
    {
      Trace.TraceInformation("HmuxChannelFactory.SetCpuLoadAvg() NYI");
    }
  }
}
