package example;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.hessian.io.*;

public class HessianSerializeServlet extends HttpServlet {
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    res.setContentType("text/html");
    
    PrintWriter out = res.getWriter();

    byte []data = hessianSerialize();

    out.println("<pre>");
    out.println("Hessian serialize size: " + data.length);
    out.println("Deserialize: " + hessianDeserialize(data));
    out.println("");
    
    data = hessianDeflate();
    
    out.println("Deflate serialize size: " + data.length);
    out.println("Inflate: " + hessianInflate(data));
    out.println("");

    data = javaSerialize();
    out.println("java.io serialize size: " + data.length);
  }

  private byte []hessianSerialize()
    throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    Hessian2Output out = new Hessian2Output(bos);

    hessianSerialize(out);

    out.close();

    return bos.toByteArray();
  }

  private Object hessianDeserialize(byte []data)
    throws IOException
  {
    ByteArrayInputStream bis = new ByteArrayInputStream(data);

    Hessian2Input in = new Hessian2Input(bis);

    return hessianDeserialize(in);
  }

  private byte []hessianDeflate()
    throws IOException
  {
    Deflation envelope = new Deflation();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    Hessian2Output out = new Hessian2Output(bos);

    out = envelope.wrap(out);

    hessianSerialize(out);

    out.close();

    return bos.toByteArray();
  }

  private Object hessianInflate(byte []data)
    throws IOException
  {
    Deflation envelope = new Deflation();

    ByteArrayInputStream bis = new ByteArrayInputStream(data);

    Hessian2Input in = new Hessian2Input(bis);

    in = envelope.unwrap(in);

    return hessianDeserialize(in);
  }

  private void hessianSerialize(Hessian2Output out)
    throws IOException
  {
    out.startMessage();
      
    out.writeInt(3);

    Car car1 = new Car(Model.EDSEL, Color.GREEN, 1954);

    out.writeObject(car1);

    Car car2 = new Car(Model.MODEL_T, Color.BLACK, 1937);

    out.writeObject(car2);

    Car car3 = new Car(Model.CIVIC, Color.BLUE, 1998);

    out.writeObject(car3);

    out.completeMessage();
  }

  private Object hessianDeserialize(Hessian2Input in)
    throws IOException
  {
    in.startMessage();

    ArrayList list = new ArrayList();

    int length = in.readInt();

    for (int i = 0; i < length; i++) {
      list.add(in.readObject());
    }

    in.completeMessage();

    return list;
  }

  private byte []javaSerialize()
    throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    ObjectOutputStream out = new ObjectOutputStream(bos);
      
    out.writeInt(3);

    Car car1 = new Car(Model.EDSEL, Color.GREEN, 1954);

    out.writeObject(car1);

    Car car2 = new Car(Model.MODEL_T, Color.BLACK, 1937);

    out.writeObject(car2);

    Car car3 = new Car(Model.CIVIC, Color.BLUE, 1998);

    out.writeObject(car3);

    out.close();

    return bos.toByteArray();
  }
}
