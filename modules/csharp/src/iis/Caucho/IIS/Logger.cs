using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Diagnostics;

namespace Caucho.IIS
{
  public class Logger
  {
    private const String LOG_SOURCE = "Resin IIS Plugin";

    private EventLog _log;
    private EventLogEntryType _logLevel = EventLogEntryType.FailureAudit;

    private static Logger _logger = null;

    internal Logger(EventLog log)
    {
      _log = log;
    }

    public static Logger GetLogger()
    {
      if (_logger != null)
        return _logger;

      if (_logger == null) {
        try {
          if (!EventLog.SourceExists(LOG_SOURCE)) {
            EventLog.CreateEventSource(LOG_SOURCE, "Application");
          }

          EventLog log = new EventLog();
          log.Log = "Application";
          log.Source = LOG_SOURCE;
          log.WriteEntry(LOG_SOURCE + ": Initializing...");

          _logger = new Logger(log);
        } catch (Exception) {
          //security does not allow to write create source or use EventLog
        }
      }

      if (_logger == null)
        _logger = new NoopLogger();

      return _logger;
    }

    public void Error(String message, params Object[] args)
    {
      Log(EventLogEntryType.Error, message, args);
    }

    public void Warning(String message, params Object[] args)
    {
      Log(EventLogEntryType.Warning, message, args);
    }

    public void Info(String message, params Object[] args)
    {
      Log(EventLogEntryType.Information, message, args);
    }

    public virtual void Log(EventLogEntryType entryType, String message, params Object[] args)
    {
      if (entryType <= _logLevel)
        _log.WriteEntry(String.Format(message, args), entryType);
    }
  }

  public class NoopLogger : Logger
  {

    public NoopLogger():base(null)
    {
    }

    public override void Log(EventLogEntryType entryType, string message, object[] args)
    {
      //nothing
    }
  }

}
