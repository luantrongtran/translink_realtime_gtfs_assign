package realtime_translink;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GtfsService implements Job {

	private static GtfsFeed gtfsFeed;

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// TODO Auto-generated method stub
//		System.out.println("Working");
//		if (gtfsFeed == null) {
//			gtfsFeed = new GtfsFeed();
//		}
//		gtfsFeed.getNewFeed();
		System.out.println("s");
	}

}
