Baratine Services in Resin
===========================

.. contents:: Page contents


Baratine Overview
-------------------

Within Resin, Baratine can be considered a synchronization and threading system,
or a simple batching message handler.

Logging systems, for example, have multiple writers that must synchronize
log messages. A simple Baratine logging system can reduce the contention,
eliminate locking, and improve performance by removing the thread traffic
jam.

Other applications where simple Baratine services are useful include writing to
a websocket stream, coordinating with an internal singleton service, and
simple asynchronous messaging, without the need to create a dedicated queues
and broker.

Logging Service
----------------

Benefits of the logging service:

* thread-safe, non-blocking clients
* single writer service
* batching is automatic

The interface to logging is simple, a method to log the message. The call
will not block, unless the service gets so far behind that the queue fills::

  public interface MyLoggerApi
  {
    void log(String message);
  }

The implementation is written as a single-threaded service because Baratine
ensures only a single thread will ever call the service. It is slightly
more complex than a hello, world example, because this log automatically
batches writes. Under load, messages are buffered before writing to the
file, improving performance dramatically::  
  
  package example;

  import java.io.*;
  import io.baratine.core.*;

  public class MyLogger
  {
    private Writer _out;
  
    public MyLogger(Writer out)
    {
      _out = out;
    }
  
    public void log(String message)
      throws IOException
    {
      _out.write(message);
      _out.write("\n");
    }
  
    @AfterBatch
    public void afterBatch()
      throws IOException
    {
      _out.flush();
    }
  
    @OnShutdown
    public void shutdown()
    {
      if (_out != null) {
        _out.close();
      }
    }
  }

Programmatic service creation is simple. Using the Baratine ``ServiceManager``,
the log user first creates a Baratine service with ``service()`` and then
creates a proxy using ``as()``. The client implementation looks like::

  import io.baratine.core.*;
  
  public class MyClient {
    public void test()
    {
      ServiceManager manager = Services.currentManager();
  
      MyLoggerApi log = manager.service(new MyLogger(file))
                               .as(MyLoggerApi.class);
    
      log.message("hello, world");                               
    }
  }

Baratine Server
----------------

The standalone Baratine server is described at http://doc.baratine.io/v0.8/.
It can be used as a microservice platform, when isolation or clustering is
important, or when multi-language clients are needed. Resin itself is built
using Baratine as the foundation for Resin's clustering, and several of
its internal services.

  