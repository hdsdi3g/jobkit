package tv.hd3g.jobkit.mod.service;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.EVENT_NOTICE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer.RegularProcessRunnerEntry;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer.RegularProcessRunnerEntry.AfterExecEntry;
import tv.hd3g.jobkit.mod.dto.RegularProcessRunnerDto;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Service
public class RegularProcessRunnerServiceImpl implements InitializingBean, DisposableBean {

	private static final Logger log = LogManager.getLogger();

	@Autowired
	private ExecutableFinder executableFinder;
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private RegularProcessRunnersConfigurer regularProcessRunnersConfigurer;
	@Autowired
	private SendMailService sendMailService;
	@Autowired
	private BackgroundServiceId backgroundServiceId;
	@Autowired
	private ExecFactoryService execFactoryService;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (regularProcessRunnersConfigurer.isDisabledAtStart() == false) {
			start();
		} else if (regularProcessRunnersConfigurer.getServices() != null
		           && regularProcessRunnersConfigurer.getServices().isEmpty() == false) {
			log.info("Don't start service RegularProcessRunner");
		}
	}

	public void start() throws FileNotFoundException {
		final var services = regularProcessRunnersConfigurer.getServices();
		if (services == null || services.isEmpty()) {
			log.debug("No configured services to start for RegularProcessRunner");
			return;
		}

		log.info("Init service RegularProcessRunner for {} services", services.size());

		if (regularProcessRunnersConfigurer.getSendFrom() == null) {
			log.info("No senderAddr configured for regularProcessRunners: cancel mail sends");
		}

		final List<RegularProcessRunnerEntry> onErrors = new ArrayList<>();

		/**
		 * Extract + check execName
		 */
		services.forEach(runnerConf -> {
			final var firstSpacePos = runnerConf.getCommandLine().indexOf(' ');
			try {
				if (firstSpacePos < 1) {
					executableFinder.get(runnerConf.getCommandLine().trim());
				} else {
					executableFinder.get(runnerConf.getCommandLine().substring(0, firstSpacePos).trim());
				}
			} catch (final FileNotFoundException e) {
				log.fatal("Check service \"{}\" configuration", runnerConf.getName(), e);
				onErrors.add(runnerConf);
			}
		});
		if (onErrors.isEmpty() == false) {
			log.fatal("Service RegularProcessRunners configuration error, please check current PATH and conf execPath");
			throw new FileNotFoundException("Can't found some executables declared by RegularProcessRunners: "
			                                + onErrors.stream()
			                                        .map(RegularProcessRunnerEntry::getName)
			                                        .collect(joining(", ")));
		}

		/**
		 * Check WorkingDir presence
		 */
		services.forEach(runnerConf -> {
			if (runnerConf.getWorkingDir() != null
			    && runnerConf.getWorkingDir().exists() == false) {
				onErrors.add(runnerConf);
			}
		});

		if (onErrors.isEmpty() == false) {
			log.fatal("Service RegularProcessRunners configuration error, please check workingDir paths");
			throw new FileNotFoundException("Can't access to workingDir declared by RegularProcessRunners: "
			                                + onErrors.stream()
			                                        .map(RegularProcessRunnerEntry::getName)
			                                        .collect(joining(", ")));
		}

		final var servicesDto = services.stream().map(this::startService).collect(toUnmodifiableSet());
		regularProcessRunnersConfigurer.setServicesDto(servicesDto);

		log.debug("Ends of init service RegularProcessRunner");
	}

	public class Task implements Runnable {
		private final RegularProcessRunnerEntry runnerConf;
		private final Map<String, String> env;
		private final Consumer<ProcesslauncherBuilder> beforeRun;
		private final Exec exec;
		private final File execFile;
		private final String execName;

		Task(final RegularProcessRunnerEntry runnerConf, final Exec exec) {
			this.runnerConf = runnerConf;
			this.exec = exec;
			execName = exec.getExecutableName();
			execFile = exec.getExecutableFile();
			env = Optional.ofNullable(runnerConf.getEnv()).orElse(Map.of());

			final var workingDirectory = runnerConf.getWorkingDir();
			beforeRun = pbuilder -> {
				env.forEach(pbuilder::setEnvironmentVar);
				if (workingDirectory != null) {
					try {
						pbuilder.setWorkingDirectory(workingDirectory);
					} catch (final IOException e) {
						throw new IllegalArgumentException(
						        "Invalid workingDirectory for " + runnerConf.getName(), e);
					}
				}
			};
		}

		@Override
		public void run() {
			CapturedStdOutErrTextRetention result;
			try {
				log.info("Start process {}, in {}/{}", execName, runnerConf.getSpoolName(), runnerConf.getName());
				result = exec.runWaitGetText(beforeRun);
			} catch (final Exception e) {
				log.error("Execution error for {} used by {}", execName, runnerConf.getName(), e);
				sendMailNotification(null, e, runnerConf.getAfterError());
				throw new ProcessExecutionException(e);
			}

			log.info("Process {} ends, in {}/{}", execName, runnerConf.getSpoolName(), runnerConf.getName());
			sendMailNotification(result, null, runnerConf.getAfterDone());
		}

		public class ProcessExecutionException extends RuntimeException {
			private ProcessExecutionException(final Throwable cause) {
				super(execName + " " + exec.getParameters().toString(), cause);
			}
		}

		private void sendMailNotification(final CapturedStdOutErrTextRetention output,
		                                  final Exception error,
		                                  final AfterExecEntry providedMailConf) {
			AfterExecEntry mailConf;
			if (providedMailConf == null && error != null) {
				/**
				 * Empty conf: send only for admins, with default/global conf
				 */
				mailConf = new AfterExecEntry();
			} else if (providedMailConf == null) {
				/**
				 * No errors, and empty conf, do nothing.
				 */
				return;
			} else {
				/**
				 * Send mail to this conf, with errors/or not.
				 */
				mailConf = providedMailConf;
			}

			final String senderAddr = regularProcessRunnersConfigurer.getSendFrom();
			if (senderAddr == null) {
				return;
			}

			String templateName;
			if (error == null) {
				templateName = optional(mailConf.getTemplateName(),
				        regularProcessRunnersConfigurer.getDefaultTemplateNameDone(),
				        "jobkit-regular-process-runner-default-mail");
			} else {
				templateName = optional(mailConf.getTemplateName(),
				        regularProcessRunnersConfigurer.getDefaultTemplateNameError(),
				        "jobkit-regular-process-runner-error-mail");
			}

			final var configuredVarsStream = Optional.ofNullable(mailConf.getAddToTemplateVars())
			        .orElse(Map.of()).entrySet().stream();

			final var mailVarsStream = Map.of(
			        "serviceName", runnerConf.getName(),
			        "spoolName", runnerConf.getSpoolName(),
			        "comment", Optional.ofNullable(runnerConf.getComment()).orElse(""),
			        "execName", exec.getExecutableName(),
			        "execFile", execFile,
			        "commandLine", exec.getReadyToRunParameters().toString(),
			        "workingDir", Optional.ofNullable(runnerConf.getWorkingDir())
			                .map(File::getPath)
			                .orElse(""),
			        "stdout", Optional.ofNullable(output)
			                .map(captureOutput -> captureOutput.getStdout(false, lineSeparator()))
			                .orElse(""),
			        "stderr", Optional.ofNullable(output)
			                .map(captureOutput -> captureOutput.getStderr(false, lineSeparator()))
			                .orElse(""),
			        "stackTrace", Optional.ofNullable(error)
			                .map(e -> {
				                final StringWriter swException = new StringWriter();
				                final var pwException = new PrintWriter(swException);
				                error.printStackTrace(pwException);
				                return swException.getBuffer().toString();
			                })
			                .orElse(""))
			        .entrySet().stream();

			final Map<String, Object> templateVars = Stream.concat(configuredVarsStream, mailVarsStream)
			        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (l, r) -> r));

			/**
			 * Only send errors to admins
			 */
			List<String> sendToAdmin;
			if (error != null) {
				sendToAdmin = Optional.ofNullable(regularProcessRunnersConfigurer.getSendToAdmin())
				        .orElse(List.of());
			} else {
				sendToAdmin = List.of();
			}

			final var recipientsAddr = Optional.ofNullable(mailConf.getSendTo()).orElse(sendToAdmin);
			if (recipientsAddr.isEmpty()) {
				throw new IllegalStateException("No A recipients to send this message, check conf for jobkit "
				                                + runnerConf.getName());
			}

			final var recipientsCCAddr = Optional.ofNullable(mailConf.getSendCc()).orElseGet(() -> sendToAdmin.stream()
			        .filter(addr -> recipientsAddr.contains(addr) == false)
			        .collect(toUnmodifiableList()));
			final var recipientsBCCAddr = sendToAdmin.stream()
			        .filter(addr -> recipientsAddr.contains(addr) == false
			                        && recipientsCCAddr.contains(addr) == false)
			        .collect(toUnmodifiableList());

			final var sendMailDto = new SendMailDto(
			        templateName,
			        Optional.ofNullable(mailConf.getLang()).orElse(Locale.getDefault()),
			        templateVars,
			        senderAddr,
			        recipientsAddr,
			        recipientsCCAddr,
			        recipientsBCCAddr);

			sendMailDto.setReplyToAddr(optional(
			        mailConf.getReplyTo(),
			        regularProcessRunnersConfigurer.getReplyTo(),
			        senderAddr));
			sendMailDto.setExternalReference("jobkit:" + runnerConf.getName());
			sendMailDto.setSenderReference(regularProcessRunnersConfigurer.getSenderReference());
			sendMailDto.setGrade(EVENT_NOTICE);
			sendMailService.sendEmail(sendMailDto);
		}

		private String optional(final String... values) {
			if (values == null || values.length == 0) {
				return null;
			}
			return Arrays.stream(values)
			        .filter(Objects::nonNull)
			        .map(String::trim)
			        .filter(v -> v.isEmpty() == false)
			        .findFirst().orElse(null);
		}

		Map<String, String> getEnv() {
			return env;
		}

		Exec getExec() {
			return exec;
		}

		RegularProcessRunnerEntry getRunnerConf() {
			return runnerConf;
		}
	}

	private RegularProcessRunnerDto startService(final RegularProcessRunnerEntry runnerConf) {
		final var firstSpacePos = runnerConf.getCommandLine().indexOf(' ');
		String execName;
		String commandLine;
		if (firstSpacePos < 1) {
			execName = runnerConf.getCommandLine().trim();
			commandLine = "";
		} else {
			execName = runnerConf.getCommandLine().substring(0, firstSpacePos).trim();
			commandLine = runnerConf.getCommandLine().substring(firstSpacePos).trim();
		}

		final var exec = execFactoryService.createNewExec(execName);
		exec.getParameters().clear().addBulkParameters(commandLine);

		final Task task = new Task(runnerConf, exec);

		final var service = jobKitEngine.startService(
		        runnerConf.getName(),
		        runnerConf.getSpoolName(),
		        runnerConf.getPeriodTime(),
		        task);
		service.setPriority(runnerConf.getPriority());
		if (runnerConf.getRetryAfterTimeFactor() > 1) {
			service.setRetryAfterTimeFactor(runnerConf.getRetryAfterTimeFactor());
		}
		backgroundServiceId.register(service);

		if (runnerConf.isRunFirstAtBoot()) {
			jobKitEngine.runOneShot(
			        runnerConf.getName(),
			        runnerConf.getSpoolName(),
			        runnerConf.getPriority(),
			        task,
			        e -> {
			        });
		}

		return new RegularProcessRunnerDto(
		        runnerConf.getName(),
		        runnerConf.getSpoolName(),
		        runnerConf.getComment(),
		        (exec.getExecutableName() + " " + exec.getReadyToRunParameters().toString()).trim(),
		        Optional.ofNullable(runnerConf.getEnv()).orElse(Map.of()).keySet(),
		        runnerConf.getWorkingDir());
	}

	@Override
	public void destroy() throws Exception {
		jobKitEngine.shutdown();
		scheduledExecutorService.shutdownNow();
		jobKitEngine.waitToClose();
	}

}
