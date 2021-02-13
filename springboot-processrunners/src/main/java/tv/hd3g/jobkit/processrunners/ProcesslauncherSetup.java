package tv.hd3g.jobkit.processrunners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Configuration
public class ProcesslauncherSetup {

	@Autowired
	public ProcesslauncherSetup(final ResourceBundleMessageSource rbms) {
		rbms.addBasenames("jobkit-messages");
	}

	@Bean
	ExecutableFinder getExecutableFinder() {
		return new ExecutableFinder();
	}
}
