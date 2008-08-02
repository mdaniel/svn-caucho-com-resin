package example;

import com.caucho.bam.SimpleBamService;
import com.caucho.bam.annotation.Message;

import java.util.ArrayList;
import java.util.Date;

public class ExampleService extends SimpleBamService
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

  public ArrayList<String> getMessages()
  {
    return _messages;
  }
}