package example;

import com.caucho.bam.SimpleActor;
import com.caucho.bam.Message;

import java.util.ArrayList;
import java.util.Date;

public class ExampleService extends SimpleActor
{
  private ArrayList<String> _messages = new ArrayList<String>();
  
  @Message
  public void message(String to, String from, ExampleMessage msg)
  {
    if (_messages.size() > 10)
      _messages.remove(0);

    String body = msg.getBody();
    String text = body + " [from=" + from + " at " + new Date() + "]";
    
    _messages.add(text);
  }
  
  @Message
  public void message(String to, String from, String body)
  {
    if (_messages.size() > 10)
      _messages.remove(0);

    String text = body + " [from=" + from + " at " + new Date() + "]";
    
    _messages.add(text);
  }

  public void addMessage(String text)
  {
    _messages.add(text);
  }

  public ArrayList<String> getMessages()
  {
    return _messages;
  }
}