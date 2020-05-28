package tv.hd3g.jobkit.mod;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import tv.hd3g.jobkit.mod.dto.RegularProcessRunnerDto;
import tv.hd3g.jobkit.mod.dto.RegularProcessRunnerListDto;

@Configuration
@ConfigurationProperties(prefix = "jobkit.processrunners")
@Validated
public class RegularProcessRunnersConfigurer {

	private List<RegularProcessRunnerEntry> services;
	private Set<RegularProcessRunnerDto> servicesDto;

	private String sendFrom;
	private String replyTo;
	private String senderReference;
	private List<String> execPath;
	private List<String> sendToAdmin;
	private String defaultTemplateNameDone;
	private String defaultTemplateNameError;
	private boolean disabledAtStart;

	@Validated
	public static class RegularProcessRunnerEntry {
		@NotEmpty
		private String name;
		@NotEmpty
		private String spoolName;
		private String comment;
		@NotNull
		private Duration periodTime;

		private int priority;
		private double retryAfterTimeFactor;
		private boolean runFirstAtBoot;

		@NotEmpty
		private String commandLine;
		private Map<String, String> env;
		private File workingDir;

		private AfterExecEntry afterDone;
		private AfterExecEntry afterError;

		public static class AfterExecEntry {
			private List<String> sendTo;
			private List<String> sendCc;
			private String replyTo;
			private String templateName;
			private Locale lang;
			private Map<String, Object> addToTemplateVars;

			public List<String> getSendTo() {
				return sendTo;
			}

			public void setSendTo(final List<String> sendTo) {
				this.sendTo = sendTo;
			}

			public List<String> getSendCc() {
				return sendCc;
			}

			public void setSendCc(final List<String> sendCc) {
				this.sendCc = sendCc;
			}

			public String getReplyTo() {
				return replyTo;
			}

			public void setReplyTo(final String replyTo) {
				this.replyTo = replyTo;
			}

			public String getTemplateName() {
				return templateName;
			}

			public void setTemplateName(final String templateName) {
				this.templateName = templateName;
			}

			public Locale getLang() {
				return lang;
			}

			public void setLang(final Locale lang) {
				this.lang = lang;
			}

			public Map<String, Object> getAddToTemplateVars() {
				return addToTemplateVars;
			}

			public void setAddToTemplateVars(final Map<String, Object> addToTemplateVars) {
				this.addToTemplateVars = addToTemplateVars;
			}

		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getSpoolName() {
			return spoolName;
		}

		public void setSpoolName(final String spoolName) {
			this.spoolName = spoolName;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(final String comment) {
			this.comment = comment;
		}

		public Duration getPeriodTime() {
			return periodTime;
		}

		public void setPeriodTime(final Duration periodTime) {
			this.periodTime = periodTime;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(final int priority) {
			this.priority = priority;
		}

		public double getRetryAfterTimeFactor() {
			return retryAfterTimeFactor;
		}

		public void setRetryAfterTimeFactor(final double retryAfterTimeFactor) {
			this.retryAfterTimeFactor = retryAfterTimeFactor;
		}

		public boolean isRunFirstAtBoot() {
			return runFirstAtBoot;
		}

		public void setRunFirstAtBoot(final boolean runFirstAtBoot) {
			this.runFirstAtBoot = runFirstAtBoot;
		}

		public String getCommandLine() {
			return commandLine;
		}

		public void setCommandLine(final String commandLine) {
			this.commandLine = commandLine;
		}

		public Map<String, String> getEnv() {
			return env;
		}

		public void setEnv(final Map<String, String> env) {
			this.env = env;
		}

		public File getWorkingDir() {
			return workingDir;
		}

		public void setWorkingDir(final File workingDir) {
			this.workingDir = workingDir;
		}

		public AfterExecEntry getAfterDone() {
			return afterDone;
		}

		public void setAfterDone(final AfterExecEntry afterDone) {
			this.afterDone = afterDone;
		}

		public AfterExecEntry getAfterError() {
			return afterError;
		}

		public void setAfterError(final AfterExecEntry afterError) {
			this.afterError = afterError;
		}
	}

	public List<RegularProcessRunnerEntry> getServices() {
		return services;
	}

	public void setServices(final List<RegularProcessRunnerEntry> services) {
		this.services = services;
	}

	public String getSendFrom() {
		return sendFrom;
	}

	public void setSendFrom(final String sendFrom) {
		this.sendFrom = sendFrom;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(final String replyTo) {
		this.replyTo = replyTo;
	}

	public String getSenderReference() {
		return senderReference;
	}

	public void setSenderReference(final String senderReference) {
		this.senderReference = senderReference;
	}

	public List<String> getExecPath() {
		return execPath;
	}

	public void setExecPath(final List<String> execPath) {
		this.execPath = execPath;
	}

	public List<String> getSendToAdmin() {
		return sendToAdmin;
	}

	public void setSendToAdmin(final List<String> sendToAdmin) {
		this.sendToAdmin = sendToAdmin;
	}

	public String getDefaultTemplateNameDone() {
		return defaultTemplateNameDone;
	}

	public String getDefaultTemplateNameError() {
		return defaultTemplateNameError;
	}

	public void setDefaultTemplateNameDone(final String defaultTemplateNameDone) {
		this.defaultTemplateNameDone = defaultTemplateNameDone;
	}

	public void setDefaultTemplateNameError(final String defaultTemplateNameError) {
		this.defaultTemplateNameError = defaultTemplateNameError;
	}

	public void setServicesDto(final Set<RegularProcessRunnerDto> servicesDto) {
		this.servicesDto = servicesDto;
	}

	/**
	 * Beware of correctly start RegularProcessRunnerService (for set servicesDto) before call makeConfigurationDto.
	 */
	public RegularProcessRunnerListDto makeConfigurationDto() {
		return new RegularProcessRunnerListDto(servicesDto, execPath, senderReference);
	}

	public void setDisabledAtStart(final boolean disabledAtStart) {
		this.disabledAtStart = disabledAtStart;
	}

	public boolean isDisabledAtStart() {
		return disabledAtStart;
	}
}
