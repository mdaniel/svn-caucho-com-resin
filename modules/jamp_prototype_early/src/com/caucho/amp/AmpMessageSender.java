package com.caucho.amp;

/** Sends an Amp message via REST, STOMP, or WebSockets. */
public interface AmpMessageSender {
	void sendMessage(String name, Object payload, String toInvoker, String fromInvoker) throws Exception;
}
