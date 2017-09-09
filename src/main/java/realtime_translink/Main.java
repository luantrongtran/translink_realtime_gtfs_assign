package realtime_translink;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class Main {
	
	public static boolean debug = true;

	public Main() {
		try {
			SchedulerFactory sf = new StdSchedulerFactory();
			Scheduler sched = sf.getScheduler();

			JobDetail job = JobBuilder.newJob(GtfsService.class).withIdentity("job1", "group1").build();
			JobDetail job2 = JobBuilder.newJob(GtfsService.class).withIdentity("job2", "group1").build();


			Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "group1")
					.withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 6-10 * * ?"))
					
					//every 5 seconds
//				    .withSchedule(CronScheduleBuilder.cronSchedule("0/5 * * * * ?"))
					.build();
			
			Trigger trigger2 = TriggerBuilder.newTrigger().withIdentity("trigger2", "group1")
					.withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 15-19 * * ?"))
					
					//every 5 seconds
//				    .withSchedule(CronScheduleBuilder.cronSchedule("0/5 * * * * ?"))
					.build();

			sched.scheduleJob(job, trigger);
			sched.scheduleJob(job2, trigger2);
			sched.start();

//			sched.addJob(job, true);
//			sched.scheduleJob(trigger);
//			
//			sched.triggerJob(job.getKey());
			

		} catch (Exception e) {

		}
	}

	public static void main(String[] args) {
		new Main();
	}
}