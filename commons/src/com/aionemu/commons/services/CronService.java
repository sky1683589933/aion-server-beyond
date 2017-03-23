package com.aionemu.commons.services;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.services.cron.CronServiceException;
import com.aionemu.commons.services.cron.RunnableRunner;
import com.aionemu.commons.utils.GenericValidator;

/**
 * @author SoulKeeper
 */
public final class CronService {

	private static final Logger log = LoggerFactory.getLogger(CronService.class);

	/**
	 * We really should use some dependency injection (Spring?)<br>
	 * Current code is bloated, should be much cleaner
	 */
	private static CronService instance;
	private static TimeZone timeZone;

	private Scheduler scheduler;

	private Class<? extends RunnableRunner> runnableRunner;

	public static CronService getInstance() {
		return instance;
	}

	public static synchronized void initSingleton(Class<? extends RunnableRunner> runableRunner, TimeZone defaultTimeZone) {
		if (instance != null) {
			throw new CronServiceException("CronService is already initialized");
		}

		CronService cs = new CronService();
		cs.init(runableRunner);
		instance = cs;
		timeZone = defaultTimeZone;
	}

	/**
	 * Empty private constructor to prevent instantiation.<br>
	 * Can be instantiated using reflection (for tests), but no real use for application please!
	 */
	private CronService() {
	}

	public synchronized void init(Class<? extends RunnableRunner> runnableRunner) {

		if (scheduler != null) {
			return;
		}

		if (runnableRunner == null) {
			throw new CronServiceException("RunnableRunner class must be defined");
		}

		this.runnableRunner = runnableRunner;

		Properties properties = new Properties();
		properties.setProperty("org.quartz.threadPool.threadCount", "1");

		try {
			scheduler = new StdSchedulerFactory(properties).getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			throw new CronServiceException("Failed to initialize CronService", e);
		}
	}

	public void shutdown() {

		Scheduler localScheduler;
		synchronized (this) {
			if (scheduler == null) {
				return;
			}

			localScheduler = scheduler;
			scheduler = null;
			runnableRunner = null;
		}

		try {
			localScheduler.shutdown(false);
		} catch (SchedulerException e) {
			log.error("Failed to shutdown CronService correctly", e);
		}
	}

	public void schedule(Runnable r, String cronExpression) {
		schedule(r, cronExpression, false);
	}

	public void schedule(Runnable r, String cronExpression, boolean longRunning) {
		try {
			JobDataMap jdm = new JobDataMap();
			jdm.put(RunnableRunner.KEY_RUNNABLE_OBJECT, r);
			jdm.put(RunnableRunner.KEY_PROPERTY_IS_LONGRUNNING_TASK, longRunning);
			jdm.put(RunnableRunner.KEY_CRON_EXPRESSION, cronExpression);

			String jobId = "Started at ms" + System.currentTimeMillis() + "; ns" + System.nanoTime();
			JobKey jobKey = new JobKey("JobKey:" + jobId);
			JobDetail jobDetail = JobBuilder.newJob(runnableRunner).usingJobData(jdm).withIdentity(jobKey).build();

			CronScheduleBuilder csb = CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(timeZone);
			CronTrigger trigger = TriggerBuilder.newTrigger().withSchedule(csb).build();

			scheduler.scheduleJob(jobDetail, trigger);
		} catch (Exception e) {
			throw new CronServiceException("Failed to start job", e);
		}
	}

	public void cancel(Runnable r) {
		Map<Runnable, JobDetail> map = getRunnables();
		JobDetail jd = map.get(r);
		cancel(jd);
	}

	public void cancel(JobDetail jd) {

		if (jd == null) {
			return;
		}

		if (jd.getKey() == null) {
			throw new CronServiceException("JobDetail should have JobKey");
		}

		try {
			scheduler.deleteJob(jd.getKey());
		} catch (SchedulerException e) {
			throw new CronServiceException("Failed to delete Job", e);
		}
	}

	protected Collection<JobDetail> getJobDetails() {
		if (scheduler == null) {
			return Collections.emptySet();
		}

		try {
			Set<JobKey> keys = scheduler.getJobKeys(null);

			if (GenericValidator.isBlankOrNull(keys)) {
				return Collections.emptySet();
			}

			Set<JobDetail> result = new HashSet<>(keys.size());
			for (JobKey jk : keys) {
				result.add(scheduler.getJobDetail(jk));
			}

			return result;
		} catch (Exception e) {
			throw new CronServiceException("Can't get all active job details", e);
		}
	}

	public Map<Runnable, JobDetail> getRunnables() {
		Collection<JobDetail> jobDetails = getJobDetails();
		if (GenericValidator.isBlankOrNull(jobDetails)) {
			return Collections.emptyMap();
		}

		Map<Runnable, JobDetail> result = new HashMap<>();
		for (JobDetail jd : jobDetails) {
			if (GenericValidator.isBlankOrNull(jd.getJobDataMap())) {
				continue;
			}

			if (jd.getJobDataMap().containsKey(RunnableRunner.KEY_RUNNABLE_OBJECT)) {
				result.put((Runnable) jd.getJobDataMap().get(RunnableRunner.KEY_RUNNABLE_OBJECT), jd);
			}
		}

		return Collections.unmodifiableMap(result);
	}

	public List<? extends Trigger> getJobTriggers(JobDetail jd) {
		return getJobTriggers(jd.getKey());
	}

	public List<? extends Trigger> getJobTriggers(JobKey jk) {
		if (scheduler == null) {
			return Collections.emptyList();
		}

		try {
			return scheduler.getTriggersOfJob(jk);
		} catch (SchedulerException e) {
			throw new CronServiceException("Can't get triggers for JobKey " + jk, e);
		}
	}
}
