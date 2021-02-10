package tv.hd3g.jobkit.mod;

import static java.lang.Thread.MIN_PRIORITY;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import tv.hd3g.jobkit.engine.BackgroundServiceEvent;
import tv.hd3g.jobkit.engine.ExecutionEvent;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Configuration
public class JobKitSetup {

	private static final Logger log = LogManager.getLogger();

	@Autowired
	public JobKitSetup(final ResourceBundleMessageSource rbms) {
		rbms.addBasenames("jobkit-messages");
	}

	@Bean
	ScheduledExecutorService getScheduledExecutor() {
		return new ScheduledThreadPoolExecutor(1, r -> {
			final var t = new Thread(r);
			t.setDaemon(false);
			t.setPriority(MIN_PRIORITY + 1);
			t.setName("SchTaskStarter");
			t.setUncaughtExceptionHandler((thrd, e) -> log.fatal("Regular scheduled thread have an uncaught error", e));
			return t;
		});
	}

	@Bean
	@Autowired
	JobKitEngine getJobKitEngine(final ScheduledExecutorService scheduledExecutor,
	                             final ExecutionEvent executionEvent,
	                             final BackgroundServiceEvent backgroundServiceEvent) {
		return new JobKitEngine(scheduledExecutor, executionEvent, backgroundServiceEvent);
	}

	@Bean
	ExecutableFinder getExecutableFinder() {
		return new ExecutableFinder();
	}

	@Bean
	BackgroundServiceId getBackgroundServiceId() {
		return new BackgroundServiceId();
	}
}
