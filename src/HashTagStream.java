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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class HashTagStream {
	public static void oauth(String hashtag, String consumerKey, String consumerSecret, String token, String secret){
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

			for (int msgRead = 0; msgRead < 100000; msgRead++) {
				String msg = msgQueue.take();
				String textUri = Config.mongouri;
				MongoClientURI uri = new MongoClientURI(textUri);
				MongoClient m = new MongoClient(uri);
				DB db = m.getDB("gigtagapp");
				DBCollection coll = db.getCollection(hashtag);
				DBObject doc = (DBObject) JSON.parse(msg);
				coll.insert(doc);
				// System.out.println("Inserted " + doc.toString());
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			client.stop();
		}

		// Print some stats
		System.out.printf("The client read %d messages!\n", client.getStatsTracker().getNumMessages());
	}

	public static void startScanning(final String tag) {
		new Thread() {
			public void run() {
				try {
					HashTagStream.oauth(tag, Config.consumerKey, Config.consumerSecret, Config.token, Config.secret);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}.start();
	}

	public static void startReader(String tag){
		try{
			String textUri = Config.mongouri;
			MongoClientURI uri = new MongoClientURI(textUri);
			MongoClient m = new MongoClient(uri);
			DB db = m.getDB("gigtagapp");
			final DBCollection coll = db.getCollection(tag);
			for (;;) {
				DBCursor cursor = coll.find();
				try {
					while (cursor.hasNext()) {
						DBObject obj = cursor.next();
						DBObject user = (DBObject) obj.get("user");
						//System.out.println("@" + user.get("screen_name") + ": "+ obj.get("text"));
						System.out.println(obj);
					}
					Thread.sleep(5000);
					System.out.println("================================================================");
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				finally {
					cursor.close();
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		HashTagStream.startScanning("#GigTag");
		startReader("#GigTag");
	}//
}
