package java.lang;

import java.io.*;

public interface AutoCloseable {
  void close() throws IOException;
}