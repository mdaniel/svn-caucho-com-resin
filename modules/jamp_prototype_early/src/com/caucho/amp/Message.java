package com.caucho.amp;

/** Represents an AMP message. */
public class Message {
	private String messageType;
	private String to;
	private String from;
	private String methodName;
	private Object args;

	public Message(String messageType, String to, String from,
			String methodName, Object args) {
		super();
		this.messageType = messageType;
		this.to = to;
		this.from = from;
		this.methodName = methodName;
		this.args = args;
	}

	@Override
	public String toString() {
		return "Message |||messageType=" + messageType + ", to=" + to + ", from="
				+ from + ", methodName=" + methodName + ", args=" + args + "|||";
	}

	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public Object getArgs() {
		return args;
	}
	public void setArgs(Object args) {
		this.args = args;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

}
