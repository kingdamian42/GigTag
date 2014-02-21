package hashTagStream;

import com.google.common.collect.Lists;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.util.JSON;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class HashTagStream {

	private Properties prop;
	private boolean stopBit = true;
	private String textUri;
	private BlockingQueue<String> tweetDB;

	private void oauth(String hashtag, String consumerKey, String consumerSecret, String token, String secret){
		//Blocking queues to hold stream json strings/events
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
		BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		endpoint.trackTerms(Lists.newArrayList(hashtag));//only tracking based off the hashtag input

		Authentication auth = new OAuth1(consumerKey, consumerSecret, token, secret);

		BasicClient client = new ClientBuilder().name("GigTag")
				.hosts(Constants.STREAM_HOST).endpoint(endpoint)
				.authentication(auth)
				.processor(new StringDelimitedProcessor(msgQueue))
				.eventMessageQueue(eventQueue).build();

		// Establish a connection
		client.connect();
		try{	
			MongoClientURI uri = new MongoClientURI(textUri);
			MongoClient m = new MongoClient(uri);
			DB db = m.getDB("gigtagapp");
			DBCollection coll = db.getCollection(hashtag);
			for (;;) {
				String msg = msgQueue.take();
				if(!stopBit){
					DBObject doc = (DBObject) JSON.parse(msg);
					coll.insert(doc);
					if(tweetDB != null){
						tweetDB.add(doc.toString());
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			client.stop();
		}
	}

	public void startScanning(final String tag) {
		new Thread() {
			public void run() {
				try {
					oauth(tag, prop.getProperty("consumerKey"), prop.getProperty("consumerSecret"), prop.getProperty("token"), prop.getProperty("secret"));
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}.start();
	}


	private void startReader(String tag){
		tweetDB = new LinkedBlockingQueue<String>(100000);
		DBCursor cursor = null;
		try{
			MongoClientURI uri = new MongoClientURI(textUri);
			MongoClient m = new MongoClient(uri);
			DB db = m.getDB(prop.getProperty("dbname"));
			final DBCollection coll = db.getCollection(tag);
			cursor = coll.find();
			//we get the whole db to start, then we will only get changes
			while (cursor.hasNext()) {
				DBObject obj = cursor.next();
				tweetDB.add(obj.toString());
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if(cursor != null)
				cursor.close();
		}
	}
	
	private void createAndShowGUI(String tag){
		final JFrame frame = new JFrame(tag);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel tweetPanel = new JPanel();
		final JTextArea output = new JTextArea();
		final JScrollPane scrollPane = new JScrollPane(output);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(400, 400));
		frame.getContentPane().add(tweetPanel);
		tweetPanel.add(scrollPane);
		output.setEditable(false);
		frame.pack();
		frame.setVisible(true);
		startReader(tag);
		new Thread(){
			public void run(){
				for(;;){
					String msg;
					try {
						msg = tweetDB.take();
						output.insert(msg + "\n", 0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public void startStreamToDatabase(){
		stopBit = false;
	}

	public void stopStreamToDatabase(){
		stopBit = true;
	}

	public HashTagStream(){
		loadProperties();
	}

	public void loadProperties(){
		//load the properties first
		prop = new Properties();
		FileInputStream input = null;
		try {
			input = new FileInputStream("conf/conf.properties");
			prop.load(input);
			input.close();
		} catch (IOException e) {
			System.out.println("Cannot find conf/config.properties");
			System.exit(1);
		}
		textUri = prop.getProperty("mongouriHead") + 
				prop.getProperty("USER") + ":" + 
				prop.getProperty("PASS") + 
				prop.getProperty("mongouriTail");
	}

	public static void main(String[] args) {
		final String tag = args.length > 0 ? args[0] : "#GigTagTest";
		final HashTagStream server = new HashTagStream();
		server.startScanning(tag);
		server.startStreamToDatabase();
		if(args.length > 1 && args[1].equals("gui")){
			SwingUtilities.invokeLater(new Thread(){
				public void run(){
					server.createAndShowGUI(tag);
				}
			});
		}
	}//
}
