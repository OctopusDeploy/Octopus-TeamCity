package octopus.teamcity.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Output {
  public interface Writer {
    void write(String text);
  }

  public static class ReaderThread extends Thread {
    private final InputStream is;
    private final Writer output;

    public ReaderThread(InputStream is, Writer output) {
      this.is = is;
      this.output = output;
    }

    @Override
    public void run() {
      final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
      final BufferedReader br = new BufferedReader(isr);
      String line;

      try {
        while ((line = br.readLine()) != null) {
          output.write(line.replaceAll("[\\r\\n]", ""));
        }
      } catch (IOException e) {
        output.write("ERROR: " + e.getMessage());
      }
    }
  }
}
