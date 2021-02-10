package tv.hd3g.jobkit.mod.service;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.lineSeparator;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static tv.hd3g.commons.mailkit.SendMailDto.MessageGrade.EVENT_NOTICE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.commons.mailkit.SendMailDto;
import tv.hd3g.commons.mailkit.SendMailService;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer.RegularProcessRunnerEntry;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer.RegularProcessRunnerEntry.AfterExecEntry;
import tv.hd3g.jobkit.mod.service.RegularProcessRunnerServiceImpl.Task;
import tv.hd3g.jobkit.mod.service.RegularProcessRunnerServiceImpl.Task.ProcessExecutionException;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@SpringBootTest
@ActiveProfiles({ "DefaultMock" })
class RegularProcessRunnerServiceImplTest {

	@Autowired
	ScheduledExecutorService scheduledExecutorService;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	RegularProcessRunnersConfigurer regularProcessRunnersConfigurer;
	@Autowired
	SendMailService sendMailService;
	@Autowired
	BackgroundServiceId backgroundServiceId;
	@Autowired
	ExecutableFinder executableFinder;
	@Autowired
	ExecFactoryService execFactoryService;

	@Captor
	ArgumentCaptor<Consumer<ProcesslauncherBuilder>> beforeRunCaptor;

	@Autowired
	RegularProcessRunnerServiceImpl regularProcessRunnerServiceImpl;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		Mockito.reset(executableFinder);
		Mockito.reset(execFactoryService);
		Mockito.reset(jobKitEngine);
		Mockito.reset(sendMailService);
	}

	@Test
	void testStartError_Bad_Exec() throws FileNotFoundException {
		when(executableFinder.get(anyString())).thenThrow(new FileNotFoundException("(test context)"));

		assertThrows(FileNotFoundException.class, () -> {
			regularProcessRunnerServiceImpl.start();
		});
	}

	private String extractExecName(final String fullCmdLine) {
		return fullCmdLine.split(" ")[0];
	}

	private Parameters extractParams(final String fullCmdLine) {
		if (fullCmdLine.indexOf(" ") == -1) {
			return new Parameters();
		}
		return new Parameters(fullCmdLine.substring(fullCmdLine.indexOf(" ") + 1));
	}

	@Test
	void testStart() throws Exception {
		when(executableFinder.get(anyString())).thenReturn(new File(currentTimeMillis() + ".mock"));

		checkBadWorkingDir();

		/**
		 * Remove the last configured services (the buggy).
		 */
		final var confServices = regularProcessRunnersConfigurer.getServices();
		confServices.remove(2);

		/**
		 * Disable all sended mails
		 */
		final var sendFrom = regularProcessRunnersConfigurer.getSendFrom();
		regularProcessRunnersConfigurer.setSendFrom(null);

		final var execServiceMap = confServices.stream()
		        .collect(toUnmodifiableMap(s -> s, s -> {
			        final var exec = Mockito.mock(Exec.class);
			        final var execName = extractExecName(s.getCommandLine());
			        final var params = extractParams(s.getCommandLine());
			        when(execFactoryService.createNewExec(eq(execName))).thenReturn(exec);
			        when(exec.getExecutableName()).thenReturn(execName);
			        when(exec.getExecutableFile()).thenReturn(new File(execName));
			        when(exec.getParameters()).thenReturn(params);
			        when(exec.getReadyToRunParameters()).thenReturn(params);
			        return exec;
		        }));

		/**
		 * jobKitEngine start service
		 */
		final var bckServiceMap = confServices.stream()
		        .collect(toUnmodifiableMap(s -> s, s -> Mockito.mock(BackgroundService.class)));

		confServices.forEach(service -> {
			when(jobKitEngine.startService(
			        eq(service.getName()), eq(service.getSpoolName()), eq(service.getPeriodTime()), any(Task.class)))
			                .thenReturn(bckServiceMap.get(service));
		});

		regularProcessRunnerServiceImpl.start();

		final var taskCaptureMap = confServices.stream()
		        .collect(toUnmodifiableMap(s -> s, s -> ArgumentCaptor.forClass(Task.class)));
		confServices.forEach(service -> {
			verify(jobKitEngine, times(1)).startService(
			        eq(service.getName()), eq(service.getSpoolName()), eq(service.getPeriodTime()),
			        taskCaptureMap.get(service).capture());
		});

		/**
		 * Get/check Task
		 */
		taskCaptureMap.values().stream().forEach(t -> assertNotNull(t.getValue()));
		final Map<RegularProcessRunnerEntry, Task> taskMap = taskCaptureMap.entrySet().stream()
		        .collect(toUnmodifiableMap(Entry::getKey, entry -> entry.getValue().getValue()));

		taskMap.entrySet().forEach(entry -> {
			final var confService = entry.getKey();
			final var task = entry.getValue();
			assertEquals(confService, task.getRunnerConf());
			assertEquals(Optional.ofNullable(confService.getEnv()).orElse(Map.of()), task.getEnv());
			assertEquals(execServiceMap.get(confService), task.getExec());
		});

		/**
		 * RUN
		 */
		final var capturedStdOutErrTextRetention = Mockito.mock(CapturedStdOutErrTextRetention.class);
		execServiceMap.values()
		        .forEach(exec -> {
			        try {
				        when(exec.runWaitGetText(any())).thenReturn(capturedStdOutErrTextRetention);
			        } catch (final IOException e1) {
			        }
		        });

		taskMap.values().forEach(Task::run);

		execServiceMap.values()
		        .forEach(exec -> {
			        try {
				        verify(exec, times(1)).runWaitGetText(beforeRunCaptor.capture());
			        } catch (final IOException e) {
			        }
		        });

		checkBeforeRun(confServices);
		checkSetPriority(confServices, bckServiceMap);
		checkSetRetryAfterTimeFactor(confServices, bckServiceMap);
		checkBackgroundServiceIdRegister(confServices, bckServiceMap);
		checkRunFirstAtBoot(confServices, taskMap);
		checkMakeConfigurationDto(confServices);

		/**
		 * No configured services: do nothing (and no errors).
		 */
		final var bckConfServices = List.copyOf(confServices);
		confServices.clear();
		regularProcessRunnersConfigurer.setDisabledAtStart(false);
		regularProcessRunnerServiceImpl.afterPropertiesSet();
		confServices.addAll(bckConfServices);

		/**
		 * Check no mail was "sended", re-enable all sended mails
		 */
		verify(sendMailService, never()).sendEmail(any(SendMailDto.class));
		regularProcessRunnersConfigurer.setSendFrom(sendFrom);
	}

	/**
	 * Test bad working-dir
	 */
	private void checkBadWorkingDir() throws IOException {
		assertThrows(FileNotFoundException.class, () -> {
			regularProcessRunnerServiceImpl.start();
		});
	}

	private void checkBeforeRun(final List<RegularProcessRunnerEntry> confServices) throws IOException {
		final var beforeRuns = beforeRunCaptor.getAllValues();
		assertNotNull(beforeRuns);
		assertFalse(beforeRuns.isEmpty());

		final var processlauncherBuilder = Mockito.mock(ProcesslauncherBuilder.class);
		beforeRuns.forEach(br -> br.accept(processlauncherBuilder));

		/**
		 * Check setEnvironmentVar to process (non-strict test)
		 */
		final var envKeysCount = confServices.stream()
		        .map(s -> Optional.ofNullable(s.getEnv()).orElse(Map.of()))
		        .mapToInt(Map::size).sum();
		verify(processlauncherBuilder, times(envKeysCount)).setEnvironmentVar(anyString(), anyString());

		/**
		 * Check setWorkingDirectory to process (non-strict test)
		 */
		final var workingDirCount = (int) confServices.stream()
		        .filter(s -> s.getWorkingDir() != null)
		        .count();
		verify(processlauncherBuilder, times(workingDirCount)).setWorkingDirectory(any(File.class));

		/**
		 * Check invalid setWorkingDirectory
		 */
		when(processlauncherBuilder.setWorkingDirectory(any(File.class)))
		        .thenThrow(new IOException("Test say invalid working dir"));
		assertThrows(IllegalArgumentException.class,
		        () -> beforeRuns.forEach(br -> br.accept(processlauncherBuilder)));
	}

	/**
	 * Check setPriority
	 */
	private void checkSetPriority(final List<RegularProcessRunnerEntry> confServices,
	                              final Map<RegularProcessRunnerEntry, BackgroundService> bckServiceMap) {
		confServices.forEach(service -> {
			verify(bckServiceMap.get(service), times(1)).setPriority(eq(service.getPriority()));
		});
	}

	/**
	 * Check setRetryAfterTimeFactor
	 */
	private void checkSetRetryAfterTimeFactor(final List<RegularProcessRunnerEntry> confServices,
	                                          final Map<RegularProcessRunnerEntry, BackgroundService> bckServiceMap) {
		confServices.forEach(service -> {
			final var factor = service.getRetryAfterTimeFactor();
			if (factor < 1d) {
				verify(bckServiceMap.get(service), times(0)).setRetryAfterTimeFactor(anyDouble());
			} else {
				verify(bckServiceMap.get(service), times(1)).setRetryAfterTimeFactor(eq(factor));
			}
		});
	}

	/**
	 * Check backgroundServiceIdRegister service for UUID <-> Service mapper
	 */
	private void checkBackgroundServiceIdRegister(final List<RegularProcessRunnerEntry> confServices,
	                                              final Map<RegularProcessRunnerEntry, BackgroundService> bckServiceMap) {
		confServices.forEach(service -> {
			verify(backgroundServiceId, times(1)).register(eq(bckServiceMap.get(service)));
		});
	}

	/**
	 * Check run-first-at-boot: true
	 */
	private void checkRunFirstAtBoot(final List<RegularProcessRunnerEntry> confServices,
	                                 final Map<RegularProcessRunnerEntry, Task> taskCaptureMap) {
		confServices.stream()
		        .filter(RegularProcessRunnerEntry::isRunFirstAtBoot)
		        .forEach(service -> {
			        verify(jobKitEngine, times(1)).runOneShot(
			                eq(service.getName()), eq(service.getSpoolName()), eq(service.getPriority()),
			                eq(taskCaptureMap.get(service)), any());
		        });
	}

	/**
	 * new RegularProcessRunnerDto()
	 */
	private void checkMakeConfigurationDto(final List<RegularProcessRunnerEntry> confServices) {
		final var dto = regularProcessRunnersConfigurer.makeConfigurationDto();
		assertNull(dto.getExecPath());
		assertEquals("send-ref-email", dto.getSenderReference());

		final var dtoServices = dto.getServices();
		assertNotNull(dtoServices);
		assertEquals(confServices.size(), dtoServices.size());

		confServices.forEach(confService -> {
			final var sDto = dtoServices.stream()
			        .filter(s -> s.getName().equals(confService.getName())).findFirst().get();
			assertEquals(confService.getCommandLine(), sDto.getCommandLine());
			assertEquals(confService.getComment(), sDto.getComment());
			assertEquals(Optional.ofNullable(confService.getEnv()).orElse(Map.of()).keySet(), sDto.getEnv());
			assertEquals(confService.getSpoolName(), sDto.getSpoolName());
			assertEquals(confService.getWorkingDir(), sDto.getWorkingDir());
		});
	}

	@Test
	void testDestroy() throws Exception {
		regularProcessRunnerServiceImpl.destroy();
		verify(jobKitEngine, atLeastOnce()).shutdown();
		verify(jobKitEngine, atLeastOnce()).waitToClose();
		verify(scheduledExecutorService, atLeastOnce()).shutdownNow();
	}

	@Nested
	class Run {

		@Mock
		RegularProcessRunnerEntry runnerConf;
		@Mock
		Exec exec;
		@Mock
		CapturedStdOutErrTextRetention output;
		@Captor
		ArgumentCaptor<SendMailDto> sendedMailCapture;

		Task task;

		String execName;
		String params;
		String sendFrom;
		String replyTo;
		String senderReference;
		List<String> sendToAdmin;
		String defaultTemplateNameDone;
		String defaultTemplateNameError;
		Map<String, Object> templateVars;

		@BeforeEach
		void init() throws Exception {
			MockitoAnnotations.openMocks(this).close();

			execName = "exec-f";
			when(exec.getExecutableName()).thenReturn(execName);
			when(exec.getExecutableFile()).thenReturn(new File(execName));
			params = "-p";
			when(exec.getReadyToRunParameters()).thenReturn(new Parameters(params));
			when(exec.getParameters()).thenReturn(new Parameters(params));

			when(runnerConf.getName()).thenReturn("name");
			when(runnerConf.getSpoolName()).thenReturn("spoolname");
			when(runnerConf.getComment()).thenReturn("comment");

			when(output.getStdout(eq(false), eq(lineSeparator()))).thenReturn("stdOut");
			when(output.getStderr(eq(false), eq(lineSeparator()))).thenReturn("stdErr");

			task = regularProcessRunnerServiceImpl.new Task(runnerConf, exec);

			defaultTemplateNameDone = regularProcessRunnersConfigurer.getDefaultTemplateNameDone();
			defaultTemplateNameError = regularProcessRunnersConfigurer.getDefaultTemplateNameError();
			replyTo = regularProcessRunnersConfigurer.getReplyTo();
			senderReference = regularProcessRunnersConfigurer.getSenderReference();
			sendFrom = regularProcessRunnersConfigurer.getSendFrom();
			sendToAdmin = List.of("admin1@jobkkit.local", "admin2@jobkkit.local");
			templateVars = null;
		}

		@AfterEach
		void ends() {
			if (templateVars == null) {
				return;
			}
			assertEquals(runnerConf.getName(), templateVars.get("serviceName"));
			assertEquals(runnerConf.getSpoolName(), templateVars.get("spoolName"));
			assertEquals(runnerConf.getComment(), templateVars.get("comment"));
			assertEquals(execName, templateVars.get("execName"));
			assertEquals(new File(execName), templateVars.get("execFile"));
			assertEquals(params, templateVars.get("commandLine"));
			assertEquals("", templateVars.get("workingDir"));
		}

		private SendMailDto getSendedMail() {
			verify(sendMailService, times(1)).sendEmail(sendedMailCapture.capture());
			final var result = sendedMailCapture.getValue();
			assertNotNull(result);
			return result;
		}

		@Test
		void checkMails_ok_BasicConf_WithoutA() throws IOException {
			when(exec.runWaitGetText(any())).thenReturn(output);
			when(runnerConf.getAfterDone()).thenReturn(new AfterExecEntry());

			/**
			 * Check no SendTo (A)
			 */
			assertThrows(IllegalStateException.class, () -> task.run());
		}

		@Test
		void checkMails_ok_BasicConf() throws IOException {
			when(exec.runWaitGetText(any())).thenReturn(output);

			final var mailConf = new AfterExecEntry();
			mailConf.setSendTo(List.of("me@here"));
			when(runnerConf.getAfterDone()).thenReturn(mailConf);
			when(runnerConf.getAfterError()).thenThrow(new IllegalArgumentException(
			        "You can't call getAfterError if no errors"));

			task.run();

			final var mail = getSendedMail();
			assertEquals("jobkit:" + runnerConf.getName(), mail.getExternalReference());
			assertEquals(EVENT_NOTICE, mail.getGrade());
			assertEquals(Locale.getDefault(), mail.getLang());
			assertEquals(List.of("me@here"), mail.getRecipientsAddr());
			assertEquals(List.of(), mail.getRecipientsCCAddr());
			assertEquals(List.of(), mail.getRecipientsBCCAddr());
			assertEquals(replyTo, mail.getReplyToAddr());
			assertEquals(sendFrom, mail.getSenderAddr());
			assertEquals(senderReference, mail.getSenderReference());
			assertEquals(defaultTemplateNameDone, mail.getTemplateName());

			templateVars = mail.getTemplateVars();
			assertNotNull(templateVars);
			assertEquals("stdOut", templateVars.get("stdout"));
			assertEquals("stdErr", templateVars.get("stderr"));
			assertEquals("", templateVars.get("stackTrace"));
		}

		@Test
		void checkMails_err_BasicConf() throws IOException {
			when(exec.runWaitGetText(any())).thenThrow(new IOException("exec-error"));

			when(runnerConf.getAfterDone()).thenThrow(new IllegalArgumentException(
			        "You can't call getAfterDone if an error is returned"));
			when(runnerConf.getAfterError()).thenReturn(null);

			assertThrows(ProcessExecutionException.class, () -> task.run());

			final var mail = getSendedMail();
			assertEquals("jobkit:" + runnerConf.getName(), mail.getExternalReference());
			assertEquals(EVENT_NOTICE, mail.getGrade());
			assertEquals(Locale.getDefault(), mail.getLang());
			assertEquals(sendToAdmin, mail.getRecipientsAddr());
			assertEquals(List.of(), mail.getRecipientsCCAddr());
			assertEquals(List.of(), mail.getRecipientsBCCAddr());
			assertEquals(replyTo, mail.getReplyToAddr());
			assertEquals(sendFrom, mail.getSenderAddr());
			assertEquals(senderReference, mail.getSenderReference());
			assertEquals(defaultTemplateNameError, mail.getTemplateName());

			templateVars = mail.getTemplateVars();
			assertNotNull(templateVars);
			assertEquals("", templateVars.get("stdout"));
			assertEquals("", templateVars.get("stderr"));
			assertTrue(((String) templateVars.get("stackTrace")).startsWith("java.io.IOException: exec-error"));
		}

		@Test
		void checkMails_ok_FullConf() throws IOException {
			when(exec.runWaitGetText(any())).thenReturn(output);

			final var mailConf = new AfterExecEntry();
			mailConf.setSendTo(List.of("me@here"));
			mailConf.setSendCc(List.of("cc1", "cc2"));
			mailConf.setReplyTo("reply");
			mailConf.setLang(ROOT);
			mailConf.setTemplateName("templateNm");
			mailConf.setAddToTemplateVars(Map.of("k1", "v1", "k2", "v2"));

			when(runnerConf.getAfterDone()).thenReturn(mailConf);
			when(runnerConf.getAfterError()).thenThrow(new IllegalArgumentException(
			        "You can't call getAfterError if no errors"));

			task.run();

			final var mail = getSendedMail();
			assertEquals("jobkit:" + runnerConf.getName(), mail.getExternalReference());
			assertEquals(EVENT_NOTICE, mail.getGrade());
			assertEquals(ROOT, mail.getLang());
			assertEquals(List.of("me@here"), mail.getRecipientsAddr());
			assertEquals(List.of("cc1", "cc2"), mail.getRecipientsCCAddr());
			assertEquals(List.of(), mail.getRecipientsBCCAddr());
			assertEquals("reply", mail.getReplyToAddr());
			assertEquals(sendFrom, mail.getSenderAddr());
			assertEquals(senderReference, mail.getSenderReference());
			assertEquals("templateNm", mail.getTemplateName());

			templateVars = mail.getTemplateVars();
			assertNotNull(templateVars);
			assertEquals("stdOut", templateVars.get("stdout"));
			assertEquals("stdErr", templateVars.get("stderr"));
			assertEquals("", templateVars.get("stackTrace"));
			assertEquals("v1", templateVars.get("k1"));
			assertEquals("v2", templateVars.get("k2"));
		}

		@Test
		void checkMails_err_FullConf() throws IOException {
			when(exec.runWaitGetText(any())).thenThrow(new IOException("exec-error"));

			final var mailConf = new AfterExecEntry();
			mailConf.setSendTo(List.of("me@here"));
			mailConf.setSendCc(List.of("cc1", "cc2"));
			mailConf.setReplyTo("reply");
			mailConf.setLang(ROOT);
			mailConf.setTemplateName("templateNm");
			mailConf.setAddToTemplateVars(Map.of("k1", "v1", "k2", "v2"));

			when(runnerConf.getAfterDone()).thenThrow(new IllegalArgumentException(
			        "You can't call getAfterDone if an error is returned"));
			when(runnerConf.getAfterError()).thenReturn(mailConf);

			assertThrows(ProcessExecutionException.class, () -> task.run());

			final var mail = getSendedMail();
			assertEquals("jobkit:" + runnerConf.getName(), mail.getExternalReference());
			assertEquals(EVENT_NOTICE, mail.getGrade());
			assertEquals(ROOT, mail.getLang());
			assertEquals(List.of("me@here"), mail.getRecipientsAddr());
			assertEquals(List.of("cc1", "cc2"), mail.getRecipientsCCAddr());
			assertEquals(sendToAdmin, mail.getRecipientsBCCAddr());
			assertEquals("reply", mail.getReplyToAddr());
			assertEquals(sendFrom, mail.getSenderAddr());
			assertEquals(senderReference, mail.getSenderReference());
			assertEquals("templateNm", mail.getTemplateName());

			templateVars = mail.getTemplateVars();
			assertNotNull(templateVars);
			assertEquals("", templateVars.get("stdout"));
			assertEquals("", templateVars.get("stderr"));
			assertTrue(((String) templateVars.get("stackTrace")).startsWith("java.io.IOException: exec-error"));
			assertEquals("v1", templateVars.get("k1"));
			assertEquals("v2", templateVars.get("k2"));
		}

		@Test
		void checkMails_err_FullConf_ToDefaultAdmin_ToOnly() throws IOException {
			when(exec.runWaitGetText(any())).thenThrow(new IOException("exec-error"));

			final var mailConf = new AfterExecEntry();
			mailConf.setSendTo(List.of("me@here", "admin1@jobkkit.local"));
			when(runnerConf.getAfterError()).thenReturn(mailConf);

			assertThrows(ProcessExecutionException.class, () -> task.run());

			final var mail = getSendedMail();
			assertEquals(List.of("me@here", "admin1@jobkkit.local"), mail.getRecipientsAddr());
			assertEquals(List.of("admin2@jobkkit.local"), mail.getRecipientsCCAddr());
			assertEquals(List.of(), mail.getRecipientsBCCAddr());

			templateVars = mail.getTemplateVars();
		}

		@Test
		void checkMails_err_FullConf_ToDefaultAdmin_ToAndCC() throws IOException {
			when(exec.runWaitGetText(any())).thenThrow(new IOException("exec-error"));

			final var mailConf = new AfterExecEntry();
			mailConf.setSendTo(List.of("me@here", "admin1@jobkkit.local"));
			mailConf.setSendCc(List.of("admin2@jobkkit.local", "cc1"));
			when(runnerConf.getAfterError()).thenReturn(mailConf);

			assertThrows(ProcessExecutionException.class, () -> task.run());

			final var mail = getSendedMail();
			assertEquals(List.of("me@here", "admin1@jobkkit.local"), mail.getRecipientsAddr());
			assertEquals(List.of("admin2@jobkkit.local", "cc1"), mail.getRecipientsCCAddr());
			assertEquals(List.of(), mail.getRecipientsBCCAddr());

			templateVars = mail.getTemplateVars();
		}

		@Test
		void checkMails_err_FullConf_ToDefaultAdmin_CConly() throws IOException {
			when(exec.runWaitGetText(any())).thenThrow(new IOException("exec-error"));

			final var mailConf = new AfterExecEntry();
			mailConf.setSendTo(List.of("me@here"));
			mailConf.setSendCc(List.of("admin1@jobkkit.local", "cc1"));
			when(runnerConf.getAfterError()).thenReturn(mailConf);

			assertThrows(ProcessExecutionException.class, () -> task.run());

			final var mail = getSendedMail();
			assertEquals(List.of("me@here"), mail.getRecipientsAddr());
			assertEquals(List.of("admin1@jobkkit.local", "cc1"), mail.getRecipientsCCAddr());
			assertEquals(List.of("admin2@jobkkit.local"), mail.getRecipientsBCCAddr());

			templateVars = mail.getTemplateVars();
		}
	}

}
