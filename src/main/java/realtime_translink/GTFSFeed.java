package realtime_translink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class GTFSFeed {
//	private static Logger log = Logger.getLogger(GTFSFeed.class);
	
	static Map<String, Logger> logs;
	private static List<String> routes ;
	private static String selectedRoutes = "60-863, IPCA-826";
	
	private static String outputFolder = "output";
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	private static SimpleDateFormat sdf_h = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
	
	public static boolean override_file = true;

	public static void getNewFeed() {
		String[] r = selectedRoutes.split(",");
		routes = new ArrayList<String>();
		for(String route_id : r) {
			routes.add(route_id.trim());
		}
		
		//adding loggers
		logs = new HashMap<String, Logger>();
		for (int i = 0 ; i < routes.size(); i++) {
			System.setProperty("logFileName", routes.get(i)+".csv");
			Logger log = Logger.getLogger(GTFSFeed.class);
			logs.put(routes.get(i), log);
		}
		
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
					if (routes.contains(routeId)) {
//						Logger log = logs.get(routeId);
//						log.info("New trip");
//						log.info(tripUpdate);
						System.out.println(tripUpdate);
						logTrip(routeId, tripUpdate);
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * One tripUpdate may have different trips.
	 * In other words, one route may have many buses
	 * @param route_id
	 * @param tripUpdate
	 * @throws IOException
	 */
	public static void logTrip(String route_id, TripUpdate tripUpdate) throws IOException {
		//Each route id will have a folder
		String dirName = outputFolder + "\\" + route_id + "\\" + sdf.format(new Date()) + "\\";
		File dir = new File(dirName);
		if(!dir.exists()) {
			dir.mkdirs();
		}
		
		TripDescriptor ts = tripUpdate.getTrip();
		
		//write json file
		String jFilename = dir.getCanonicalPath() + "\\" + ts.getTripId() + ".json";
		File jFile = new File (jFilename);
		FileWriter jFwriter = new FileWriter(jFile, override_file);
		BufferedWriter jBuf = new BufferedWriter(jFwriter);
		PrintWriter jPWriter = new PrintWriter(jBuf);
		jPWriter.println(tripUpdate);
		jPWriter.close();
		
		
		///////Write .csv file
		String filename = dir.getCanonicalPath() + "\\" + ts.getTripId() + ".csv";
		File f = new File(filename);
//		if(f.exists()) {
//			f.createNewFile();
//		}
		
		
		FileWriter fwriter = new FileWriter(filename, override_file);
		BufferedWriter buf = new BufferedWriter(fwriter);
		PrintWriter pWriter = new PrintWriter(buf);
		
		//luant: print file
		pWriter.println("///////////////////");
//		pWriter.print(tripUpdate);
		
		TripDescriptor tripDesc = tripUpdate.getTrip();
		//print trip info
		String tripInfo = tripDesc.getTripId() + "," + tripDesc.getStartTime() +"," + tripDesc.getStartTime();
		pWriter.println(tripInfo);
		
		//print stops: stop_id, stop_sequence, stop_arrival, stop_depature, stop_delay
		tripUpdate.getStopTimeUpdateList();
		for(StopTimeUpdate su : tripUpdate.getStopTimeUpdateList()) {
			String suInfo = su.getStopId() + "," + su.getStopSequence() 
			+ "," + sdf_h.format(su.getArrival().getTime()) 
			+ sdf_h.format(su.getDeparture().getTime()) + "," + su.getArrival().getDelay();
			
			pWriter.println(suInfo);
		}
		
		pWriter.close();
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