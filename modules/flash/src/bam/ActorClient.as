/*
 * Copyright (c) 2001-2009 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
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
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
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
 * @author Emil Ong
 */

package bam
{
  import bam.ActorErrorMessage;
  import flash.errors.IOError;
  import flash.events.EventDispatcher;

  public class ActorClient extends EventDispatcher {
    protected var _jid:String;

    private var _queryId:int = 0;
    private var _outstandingQueries:Object = new Object();

    protected var _toLinkStream:ActorStream;

    [ArrayElementType("MessageListenerMapEntry")]
    private var _messageListeners:Array;

    [ArrayElementType("MessageErrorListenerMapEntry")]
    private var _messageErrorListeners:Array;

    [ArrayElementType("QueryListenerMapEntry")]
    private var _queryGetListeners:Array;

    [ArrayElementType("QueryListenerMapEntry")]
    private var _querySetListeners:Array;

    [ArrayElementType("QueryListenerMapEntry")]
    private var _queryResultListeners:Array;

    [ArrayElementType("QueryErrorListenerMapEntry")]
    private var _queryErrorListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceUnavailableListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceProbeListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceSubscribeListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceSubscribedListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceUnsubscribeListeners:Array;

    [ArrayElementType("PresenceListenerMapEntry")]
    private var _presenceUnsubscribedListeners:Array;

    [ArrayElementType("PresenceErrorListenerMapEntry")]
    private var _presenceErrorListeners:Array;

    public function get jid():String
    {
      return _jid;
    }

    // output methods

    public function message(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a message because the client is closed.");

      _toLinkStream.message(to, jid, value);
    }

    public function queryGet(to:String, value:Object,
                             onResult:Function = null, 
                             onError:Function = null):void
    {
      var queryId:int = _queryId++;

      if (onResult != null || onError != null) {
        _outstandingQueries[queryId.toString()]
          = new QueryCallbackBundle(onResult, onError);
      }

      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a queryGet because the client is closed.");

      _toLinkStream.queryGet(queryId, to, jid, value);
    }

    public function querySet(to:String, value:Object,
                             onResult:Function = null, 
                             onError:Function = null):void
    {
      var queryId:int = _queryId++;

      if (onResult != null || onError != null) {
        _outstandingQueries[queryId.toString()]
          = new QueryCallbackBundle(onResult, onError);
      }

      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a querySet because the client is closed.");

      _toLinkStream.querySet(queryId, to, jid, value);
    }

    public function presence(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presence because the client is closed.");

      _toLinkStream.presence(to, jid, value);
    }

    public function presenceUnavailable(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceUnavailable because the client is closed.");

      _toLinkStream.presenceUnavailable(to, jid, value);
    }

    public function presenceProbe(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceProbe because the client is closed.");

      _toLinkStream.presenceProbe(to, jid, value);
    }

    public function presenceSubscribe(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceSubscribe because the client is closed.");

      _toLinkStream.presenceSubscribe(to, jid, value);
    }

    public function presenceSubscribed(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceSubscribed because the client is closed.");

      _toLinkStream.presenceSubscribed(to, jid, value);
    }

    public function presenceUnsubscribe(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceUnsubscribe because the client is closed.");

      _toLinkStream.presenceUnsubscribe(to, jid, value);
    }

    public function presenceUnsubscribed(to:String, value:Object):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceUnsubscribed because the client is closed.");

      _toLinkStream.presenceUnsubscribed(to, jid, value);
    }

    public function presenceError(to:String, value:Object, 
                                  error:ActorErrorMessage):void
    {
      if (_toLinkStream == null)
        throw new IOError(toString() + " cannot send a presenceError because the client is closed.");

      _toLinkStream.presenceError(to, jid, value, error);
    }

    // input methods

    public function handleMessage(to:String, from:String, value:Object):void
    {
      for each (var entry:MessageListenerMapEntry in _messageListeners)
        entry.invoke(to, from, value);
    }

    public function handleMessageError(to:String, from:String, 
                                       value:Object, error:ActorErrorMessage):void
    {
      for each (var entry:MessageErrorListenerMapEntry in 
                _messageErrorListeners)
        entry.invoke(to, from, value, error);
    }

    public function handleQueryGet(id:Number, to:String, from:String, 
                                   value:Object):void
    {
      for each (var entry:QueryListenerMapEntry in _queryGetListeners)
        entry.invoke(id, to, from, value);
    }

    public function handleQuerySet(id:Number, to:String, from:String, 
                                   value:Object):void
    {
      for each (var entry:QueryListenerMapEntry in _querySetListeners)
        entry.invoke(id, to, from, value);
    }

    public function handleQueryResult(id:Number, to:String, from:String, 
                                      value:Object):void
    {
      var bundle:QueryCallbackBundle = _outstandingQueries[id];

      trace("bundle = " + bundle);

      if (bundle != null) {
        bundle.onResult(id, to, from, value);

        delete(_outstandingQueries[id]);
      }
      else {
        for each (var entry:QueryListenerMapEntry in _queryResultListeners)
          entry.invoke(id, to, from, value);
      }
    }

    public function handleQueryError(id:Number, to:String, from:String, 
                                     value:Object, error:ActorErrorMessage):void
    {
      var bundle:QueryCallbackBundle = _outstandingQueries[id];

      if (bundle != null) {
        bundle.onError(id, to, from, value, error);

        delete(_outstandingQueries[id]);
      }
      else {
        for each (var entry:QueryErrorListenerMapEntry in _queryErrorListeners)
          entry.invoke(id, to, from, value, error);
      }
    }

    public function handlePresence(to:String, from:String, value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceUnavailable(to:String, from:String, 
                                              value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceProbe(to:String, from:String, 
                                        value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceSubscribe(to:String, from:String, 
                                            value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceSubscribed(to:String, from:String,  
                                             value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceUnsubscribe(to:String, from:String, 
                                              value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceUnsubscribed(to:String, from:String, 
                                               value:Object):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    public function handlePresenceError(to:String, from:String, 
                                        value:Object, error:ActorErrorMessage):void
    {
      for each (var entry:PresenceListenerMapEntry in _presenceListeners)
        entry.invoke(to, from, value);
    }

    // listener registration
    public function addMessageListener(cl:Class, listener:Function):void
    {
      if (_messageListeners == null)
        _messageListeners = [];

      var entry:MessageListenerMapEntry =
        new MessageListenerMapEntry(cl, listener);

      _messageListeners.push(entry);
    }

    public function addMessageErrorListener(cl:Class, listener:Function):void
    {
      if (_messageErrorListeners == null)
        _messageErrorListeners = [];

      var entry:MessageErrorListenerMapEntry =
        new MessageErrorListenerMapEntry(cl, listener);

      _messageErrorListeners.push(entry);
    }

    public function addQueryGetListener(cl:Class, listener:Function):void
    {
      if (_queryGetListeners == null)
        _queryGetListeners = [];

      var entry:QueryListenerMapEntry =
        new QueryListenerMapEntry(cl, listener);

      _queryGetListeners.push(entry);
    }

    public function addQuerySetListener(cl:Class, listener:Function):void
    {
      if (_querySetListeners == null)
        _querySetListeners = [];

      var entry:QueryListenerMapEntry =
        new QueryListenerMapEntry(cl, listener);

      _querySetListeners.push(entry);
    }

    public function addQueryResultListener(cl:Class, listener:Function):void
    {
      if (_queryResultListeners == null)
        _queryResultListeners = [];

      var entry:QueryListenerMapEntry =
        new QueryListenerMapEntry(cl, listener);

      _queryResultListeners.push(entry);
    }

    public function addQueryErrorListener(cl:Class, listener:Function):void
    {
      if (_queryErrorListeners == null)
        _queryErrorListeners = [];

      var entry:QueryErrorListenerMapEntry =
        new QueryErrorListenerMapEntry(cl, listener);

      _queryErrorListeners.push(entry);
    }

    public function addPresenceListener(cl:Class, listener:Function):void
    {
      if (_presenceListeners == null)
        _presenceListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceListeners.push(entry);
    }

    public function addPresenceUnavailableListener(cl:Class, 
                                                   listener:Function):void
    {
      if (_presenceUnavailableListeners == null)
        _presenceUnavailableListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceUnavailableListeners.push(entry);
    }

    public function addPresenceProbeListener(cl:Class, 
                                             listener:Function):void
    {
      if (_presenceProbeListeners == null)
        _presenceProbeListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceProbeListeners.push(entry);
    }

    public function addPresenceSubscribeListener(cl:Class, 
                                                 listener:Function):void
    {
      if (_presenceSubscribeListeners == null)
        _presenceSubscribeListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceSubscribeListeners.push(entry);
    }

    public function addPresenceSubscribedListener(cl:Class, 
                                                  listener:Function):void
    {
      if (_presenceSubscribedListeners == null)
        _presenceSubscribedListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceSubscribedListeners.push(entry);
    }

    public function addPresenceUnsubscribeListener(cl:Class, 
                                                   listener:Function):void
    {
      if (_presenceUnsubscribeListeners == null)
        _presenceUnsubscribeListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceUnsubscribeListeners.push(entry);
    }

    public function addPresenceUnsubscribedListener(cl:Class, 
                                                    listener:Function):void
    {
      if (_presenceUnsubscribedListeners == null)
        _presenceUnsubscribedListeners = [];

      var entry:PresenceListenerMapEntry =
        new PresenceListenerMapEntry(cl, listener);

      _presenceUnsubscribedListeners.push(entry);
    }

    public function addPresenceErrorListener(cl:Class, listener:Function):void
    {
      if (_presenceErrorListeners == null)
        _presenceErrorListeners = [];

      var entry:PresenceErrorListenerMapEntry =
        new PresenceErrorListenerMapEntry(cl, listener);

      _presenceErrorListeners.push(entry);
    }

    public function isClosed():Boolean
    {
      return _toLinkStream == null;
    }

    public function close():void
    {
      _toLinkStream.close();
      _toLinkStream = null;
    }
  }
}

class QueryCallbackBundle {
  private var _onResult:Function;
  private var _onError:Function;

  public function QueryCallbackBundle(onResult:Function,
                                      onError:Function):void
  {
    _onResult = onResult;
    _onError = onError;
  }

  public function onResult(id:Number, to:String, from:String, value:Object):void
  {
    if (_onResult != null)
      _onResult(id, to, from, value);
  }

  public function onError(id:Number, to:String, from:String, value:Object,
                          error:bam.ActorErrorMessage):void
  {
    if (_onError != null)
      _onError(id, to, from, value, error);
  }
}

class MessageListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function MessageListenerMapEntry(clazz:Class, listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(to:String, from:String, value:Object):void
  {
    if (value is _class)
      _listener(to, from, value);
  }
}

class QueryListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function QueryListenerMapEntry(clazz:Class, listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(id:Number, to:String, from:String, 
                         value:Object):void
  {
    if (value is _class)
      _listener(id, to, from, value);
  }
}

class MessageErrorListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function MessageErrorListenerMapEntry(clazz:Class, 
                                               listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(to:String, from:String, 
                         value:Object, error:bam.ActorErrorMessage):void
  {
    if (value is _class)
      _listener(to, from, value, error);
  }
}

class QueryErrorListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function QueryErrorListenerMapEntry(clazz:Class, 
                                             listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(id:Number, to:String, from:String, 
                         value:Object, error:bam.ActorErrorMessage):void
  {
    if (value is _class)
      _listener(id, to, from, value, error);
  }
}

class PresenceListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function PresenceListenerMapEntry(clazz:Class, listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(to:String, from:String, value:Object):void
  {
    if (value is _class)
      _listener(to, from, value);
  }
}

class PresenceErrorListenerMapEntry {
  private var _class:Class;
  private var _listener:Function;

  public function PresenceErrorListenerMapEntry(clazz:Class, 
                                                listener:Function):void
  {
    _class = clazz;
    _listener = listener;
  }

  public function invoke(to:String, from:String, 
                         value:Object, error:bam.ActorErrorMessage):void
  {
    if (value is _class)
      _listener(to, from, value, error);
  }
}


