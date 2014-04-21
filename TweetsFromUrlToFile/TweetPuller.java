import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;


public class TweetPuller{
  public static void main(String[] args){
    if(args.length < 2){
      System.err.println("Too few arguments");
      return;
    }
    String urlString = args[0];
    String fileName = args[1] + ".twtext";
    URL url;
  try {
    url = new URL(urlString);
      InputStream in = url.openStream();
      ReadableByteChannel rbc = Channels.newChannel(in);
      FileOutputStream fos = new FileOutputStream(fileName);
      fos.getChannel().transferFrom(rbc,0,Long.MAX_VALUE);
  } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
  }
}