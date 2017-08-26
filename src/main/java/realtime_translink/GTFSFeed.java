package realtime_translink;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public class GTFSFeed {
	private static Logger log = Logger.getLogger(GTFSFeed.class);
	private static String tracked_route = "60";

	public static void getNewFeed() {
		URL url;
		FeedMessage feed;
		try {
			url = new URL("https://gtfsrt.api.translink.com.au/Feed/SEQ");
			feed = FeedMessage.parseFrom(url.openStream());
			for (FeedEntity entity : feed.getEntityList()) {
				if (entity.hasTripUpdate()) {
					TripUpdate tripUpdate = entity.getTripUpdate();
					TripDescriptor tripDescriptor = tripUpdate.getTrip();

					String routeId = tripDescriptor.getRouteId();

					System.out.println(routeId);
					if (routeId.startsWith(tracked_route)) {
						log.info("New trip");
						log.info(tripUpdate);
						System.out.println(tripUpdate);
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Timer t = new Timer();
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				GTFSFeed.getNewFeed();
			}
		}, 1000, 1500);
	}
}
