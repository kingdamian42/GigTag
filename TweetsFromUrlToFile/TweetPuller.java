import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.TimerTask;
import java.util.Timer;


public class TweetPuller{
  public static void main(String[] args){
    if(args.length < 2){
      System.err.println("Too few arguments");
      return;
    }
    String urlString = args[0];
    String fileName = args[1] + ".twtext";
    int interval = Integer.parseInt(args[2]);
    MyTimerTask task = new MyTimerTask(urlString,fileName);
    Timer timer = new Timer();
    timer.schedule(task,0,interval);
  }

  private static class MyTimerTask extends TimerTask{
    String urlString;
    String fileName;

    public MyTimerTask(String url, String fileName){
      this.urlString = url;
      this.fileName = fileName;
    }

    public void run(){
      try {
        URL url;
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

}