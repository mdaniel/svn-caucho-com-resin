package example;

import java.util.ArrayList;

import javax.annotation.Named;
import javax.context.ApplicationScoped;

@ApplicationScoped
@Named
public class ExampleMessages
{
  private ArrayList<String> _messages = new ArrayList<String>();
  
  public void addMessage(String text)
  {
    if (_messages.size() > 10)
      _messages.remove(0);
    
    _messages.add(text);
  }

  public ArrayList<String> getMessages()
  {
    return _messages;
  }
}