package realtime_translink;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class GtfsFeed {
	// private static Logger log = Logger.getLogger(GTFSFeed.class);

	Map<String, Logger> logs;
	private List<String> routes;
	private String selectedRoutes = "66-863, 345-863, 444-863";

	private String outputFolder = "./output";

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat sdf_h = new SimpleDateFormat("HH:mm:ss, dd/MM/yyyy");

	private SimpleDateFormat sdf_t = new SimpleDateFormat("HH:mm:ss");

	public boolean appending_file = false;

	public String directorySeparator = "/";

	public boolean isSingleTripTracking = false;
	public String singleModeTripId = "";

	public void getNewFeed() {
		String[] r = selectedRoutes.split(",");
		routes = new ArrayList<String>();
		for (String route_id : r) {
			routes.add(route_id.trim());
		}

		// adding loggers
		logs = new HashMap<String, Logger>();
		for (int i = 0; i < routes.size(); i++) {
			System.setProperty("logFileName", routes.get(i) + ".csv");
			Logger log = Logger.getLogger(GtfsFeed.class);
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

					// System.out.println(routeId);
					if (routes.contains(routeId)) {
						TripDescriptor t = tripUpdate.getTrip();
						if (isSingleTripTracking == true) {
							if (singleModeTripId.isEmpty()) {
								System.out.println(tripUpdate.getStopTimeUpdateCount());
								if (t.getTripId().startsWith("UNPLANNED") || tripUpdate.getStopTimeUpdateCount() < 3) {
									continue;
								}

								if (t.getStartTime().compareTo(sdf_t.format(new Date())) == 1) {
									continue;
								}

								singleModeTripId = tripUpdate.getTrip().getTripId();
							} else {
								if (!singleModeTripId.equals(tripUpdate.getTrip().getTripId())) {
									continue;
								}
							}
						}
						// Logger log = logs.get(routeId);
						// log.info("New trip");
						// log.info(tripUpdate);
						// System.out.println(tripUpdate);
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
	 * One tripUpdate may have different trips. In other words, one route may
	 * have many buses.
	 * 
	 * The way that GTFS realtime feed works is that all stops of a trip has
	 * been visted will not be sent in the GTFS realtime feed.
	 * 
	 * @param route_id
	 * @param tripUpdate
	 * @throws IOException
	 */
	public void logTrip(String route_id, TripUpdate tripUpdate) throws IOException {
		// Each route id will have a folder
		String dirName = outputFolder + directorySeparator + route_id + directorySeparator + sdf.format(new Date())
				+ directorySeparator;
		File dir = new File(dirName);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		TripDescriptor ts = tripUpdate.getTrip();

		// write json file
		String jFilename = dir.getCanonicalPath() + directorySeparator + ts.getTripId() + ".json";
		File jFile = new File(jFilename);
		if(!jFile.exists()) {
			FileWriter jFwriter = new FileWriter(jFile, appending_file);
			BufferedWriter jBuf = new BufferedWriter(jFwriter);
			PrintWriter jPWriter = new PrintWriter(jBuf);
			jPWriter.println(tripUpdate);
			jPWriter.close();
		}
		/////// Write .csv file
		String filename = dir.getCanonicalPath() + directorySeparator + ts.getTripId() + ".csv";

		// read file first
		BufferedReader rbuf = null;
		List<String> existingContent = new ArrayList<String>();
		try {
			File f = new File(filename);
			if (f.exists()) {
				FileInputStream fin = new FileInputStream(f);
				String s = IOUtils.toString(fin);

				String[] arrS = s.split("\n");

				existingContent = Arrays.asList(arrS);
				// FileReader freader = new FileReader(filename);
				// rbuf = new BufferedReader(freader);
				// String line = rbuf.readLine();
				// while (line != null) {
				// existingContent.add(line);
				// System.out.println(line);
				// }
			}
		} catch (Exception ex) {
			System.out.println(ex);
		} finally {
			if (rbuf != null) {
				rbuf.close();
			}
		}

		FileWriter fwriter = new FileWriter(filename, appending_file);
		BufferedWriter buf = new BufferedWriter(fwriter);
		PrintWriter pWriter = new PrintWriter(buf);

		// luant: print file
		// pWriter.println("///////////////////");

		TripDescriptor tripDesc = tripUpdate.getTrip();
		// print trip info
		// String tripInfo = tripDesc.getTripId() + "," +
		// tripDesc.getStartTime() + "," + tripDesc.getStartTime();
		// pWriter.println(tripInfo);

		// print stops: stop_id, stop_sequence, stop_arrival, stop_depature,
		// stop_delay
		tripUpdate.getStopTimeUpdateList();
		List<StopTimeUpdate> lstStops = tripUpdate.getStopTimeUpdateList();

		if (existingContent.size() > 0) {
			int bottomIndex = existingContent.size() - 1;// Math.min(existingContent.size()
															// - 1,
															// lstStops.size() -
															// 1);

			if (Main.debug) {
				System.out.println("pre: " + existingContent.size());
				System.out.println("new: " + lstStops.size());

				for (String s : existingContent) {
					System.out.println(s);
				}
				System.out.println("***");
				for (StopTimeUpdate su : lstStops) {
					String suInfo = ts.getRouteId() + "," + su.getStopId() + "," + su.getStopSequence() + ","
							+ sdf_h.format(su.getArrival().getTime() * 1000) + "," + su.getArrival().getDelay() + ","
							+ sdf_h.format(su.getDeparture().getTime() * 1000) + "," + su.getDeparture().getDelay();
					System.out.println(suInfo);
				}
			}

			for (int i = lstStops.size() - 1; i >= 0; i--) {
				StopTimeUpdate su = lstStops.get(i);
				String suInfo = su.getStopId() + "," + su.getStopSequence() + ","
						+ sdf_h.format(su.getArrival().getTime() * 1000) + "," + su.getArrival().getDelay() + ","
						+ sdf_h.format(su.getDeparture().getTime() * 1000) + "," + su.getDeparture().getDelay();

				existingContent.set(bottomIndex, suInfo);
				bottomIndex--;
				if (bottomIndex < 0) {
					break;
				}
			}

			for (int i = 0; i < existingContent.size(); i++) {
				pWriter.println(existingContent.get(i));
			}
		} else {
			for (StopTimeUpdate su : tripUpdate.getStopTimeUpdateList()) {
				String suInfo = su.getStopId() + "," + su.getStopSequence() + ","
						+ sdf_h.format(su.getArrival().getTime() * 1000) + "," + su.getArrival().getDelay() + ","
						+ sdf_h.format(su.getDeparture().getTime() * 1000) + "," + su.getDeparture().getDelay();

				pWriter.println(suInfo);
			}
		}

		pWriter.close();
	}

	public static void main(String[] args) throws Exception {
		// final List<TimeFrame> tf = new ArrayList<TimeFrame>();
		// tf.add(new TimeFrame(6, 10));
		// tf.add(new TimeFrame(15, 19));
		//
		// Timer t = new Timer();
		// t.schedule(new TimerTask() {
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		// GTFSFeed.getNewFeed();
		// }
		// }, 2000, 10000);
	}

	// public void execute(JobExecutionContext arg0) throws
	// JobExecutionException {
	// // TODO Auto-generated method stub
	// System.out.println("Hello world!!");
	// }
}