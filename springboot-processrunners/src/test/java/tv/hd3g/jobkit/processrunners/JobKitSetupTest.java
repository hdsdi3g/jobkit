package tv.hd3g.jobkit.processrunners;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.ResourceBundleMessageSource;

import tv.hd3g.jobkit.engine.BackgroundServiceEvent;
import tv.hd3g.jobkit.engine.ExecutionEvent;

class JobKitSetupTest {

	@Mock
	ScheduledExecutorService scheduledExecutor;
	@Mock
	ExecutionEvent executionEvent;
	@Mock
	BackgroundServiceEvent backgroundServiceEvent;
	@Mock
	ResourceBundleMessageSource resourceBundleMessageSource;

	ProcesslauncherSetup processlauncherSetup;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		processlauncherSetup = new ProcesslauncherSetup(resourceBundleMessageSource);
	}

	@Test
	void testGetExecutableFinder() {
		assertNotNull(processlauncherSetup.getExecutableFinder());
	}

}
