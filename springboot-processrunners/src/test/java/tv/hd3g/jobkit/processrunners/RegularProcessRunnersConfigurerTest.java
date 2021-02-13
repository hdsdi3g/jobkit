package tv.hd3g.jobkit.processrunners;

import static java.time.temporal.ChronoUnit.NANOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.jobkit.processrunners.RegularProcessRunnersConfigurer.RegularProcessRunnerEntry;
import tv.hd3g.jobkit.processrunners.RegularProcessRunnersConfigurer.RegularProcessRunnerEntry.AfterExecEntry;
import tv.hd3g.jobkit.processrunners.dto.RegularProcessRunnerDto;

class RegularProcessRunnersConfigurerTest {
	static Random random = new Random();

	@Mock
	List<RegularProcessRunnerEntry> services;
	@Mock
	Set<RegularProcessRunnerDto> servicesDto;
	@Mock
	List<String> execPath;
	@Mock
	List<String> sendToAdmin;

	String sendFrom;
	String replyTo;
	String senderReference;
	String defaultTemplateNameDone;
	String defaultTemplateNameError;

	RegularProcessRunnersConfigurer regularProcessRunnersConfigurer;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		sendFrom = String.valueOf(random.nextLong());
		replyTo = String.valueOf(random.nextLong());
		senderReference = String.valueOf(random.nextLong());
		defaultTemplateNameDone = String.valueOf(random.nextLong());
		defaultTemplateNameError = String.valueOf(random.nextLong());

		regularProcessRunnersConfigurer = new RegularProcessRunnersConfigurer();
	}

	@Test
	void testGetServices() {
		assertNull(regularProcessRunnersConfigurer.getServices());
	}

	@Test
	void testSetServices() {
		regularProcessRunnersConfigurer.setServices(services);
		assertEquals(services, regularProcessRunnersConfigurer.getServices());
	}

	@Test
	void testGetSendFrom() {
		assertNull(regularProcessRunnersConfigurer.getSendFrom());
	}

	@Test
	void testSetSendFrom() {
		regularProcessRunnersConfigurer.setSendFrom(sendFrom);
		assertEquals(sendFrom, regularProcessRunnersConfigurer.getSendFrom());
	}

	@Test
	void testGetReplyTo() {
		assertNull(regularProcessRunnersConfigurer.getReplyTo());
	}

	@Test
	void testSetReplyTo() {
		regularProcessRunnersConfigurer.setReplyTo(replyTo);
		assertEquals(replyTo, regularProcessRunnersConfigurer.getReplyTo());
	}

	@Test
	void testGetSenderReference() {
		assertNull(regularProcessRunnersConfigurer.getSenderReference());
	}

	@Test
	void testSetSenderReference() {
		regularProcessRunnersConfigurer.setSenderReference(senderReference);
		assertEquals(senderReference, regularProcessRunnersConfigurer.getSenderReference());
	}

	@Test
	void testGetExecPath() {
		assertNull(regularProcessRunnersConfigurer.getExecPath());
	}

	@Test
	void testSetExecPath() {
		regularProcessRunnersConfigurer.setExecPath(execPath);
		assertEquals(execPath, regularProcessRunnersConfigurer.getExecPath());
	}

	@Test
	void testGetSendToAdmin() {
		assertNull(regularProcessRunnersConfigurer.getSendToAdmin());
	}

	@Test
	void testSetSendToAdmin() {
		regularProcessRunnersConfigurer.setSendToAdmin(sendToAdmin);
		assertEquals(sendToAdmin, regularProcessRunnersConfigurer.getSendToAdmin());
	}

	@Test
	void testGetDefaultTemplateNameDone() {
		assertNull(regularProcessRunnersConfigurer.getDefaultTemplateNameDone());
	}

	@Test
	void testGetDefaultTemplateNameError() {
		assertNull(regularProcessRunnersConfigurer.getDefaultTemplateNameError());
	}

	@Test
	void testSetDefaultTemplateNameDone() {
		regularProcessRunnersConfigurer.setDefaultTemplateNameDone(defaultTemplateNameDone);
		assertEquals(defaultTemplateNameDone, regularProcessRunnersConfigurer.getDefaultTemplateNameDone());
	}

	@Test
	void testSetDefaultTemplateNameError() {
		regularProcessRunnersConfigurer.setDefaultTemplateNameError(defaultTemplateNameError);
		assertEquals(defaultTemplateNameError, regularProcessRunnersConfigurer.getDefaultTemplateNameError());
	}

	@Test
	void testMakeConfigurationDto() {
		final var cDto = regularProcessRunnersConfigurer.makeConfigurationDto();
		assertNotNull(cDto);
		assertNull(cDto.getSenderReference());
		assertNull(cDto.getServices());
		assertNull(cDto.getExecPath());

		regularProcessRunnersConfigurer.setSenderReference(senderReference);
		regularProcessRunnersConfigurer.setServicesDto(servicesDto);
		regularProcessRunnersConfigurer.setExecPath(execPath);

		final var cDto2 = regularProcessRunnersConfigurer.makeConfigurationDto();
		assertNotNull(cDto2);
		assertEquals(senderReference, cDto2.getSenderReference());
		assertEquals(servicesDto, cDto2.getServices());
		assertEquals(execPath, cDto2.getExecPath());
	}

	@Test
	void testIsDisabledAtStart() {
		assertFalse(regularProcessRunnersConfigurer.isDisabledAtStart());
	}

	@Test
	void setSetDisabledAtStart() {
		regularProcessRunnersConfigurer.setDisabledAtStart(true);
		assertTrue(regularProcessRunnersConfigurer.isDisabledAtStart());
	}

	@Nested
	class RegularProcessRunnerEntryTest {

		@Mock
		Map<String, String> env;
		@Mock
		File workingDir;
		@Mock
		AfterExecEntry afterDone;
		@Mock
		AfterExecEntry afterError;

		String name;
		String spoolName;
		String comment;
		Duration periodTime;
		int priority;
		double retryAfterTimeFactor;
		boolean runFirstAtBoot;
		String commandLine;

		RegularProcessRunnerEntry regularProcessRunnerEntry;

		@BeforeEach
		void init() throws Exception {
			MockitoAnnotations.openMocks(this).close();
			name = String.valueOf(random.nextLong());
			spoolName = String.valueOf(random.nextLong());
			comment = String.valueOf(random.nextLong());
			periodTime = Duration.of(Math.abs(random.nextLong()), NANOS);
			priority = random.nextInt();
			retryAfterTimeFactor = random.nextDouble();
			runFirstAtBoot = random.nextBoolean();
			commandLine = String.valueOf(random.nextLong());

			regularProcessRunnerEntry = new RegularProcessRunnerEntry();
		}

		@Test
		void testGetName() {
			assertNull(regularProcessRunnerEntry.getName());
		}

		@Test
		void testSetName() {
			regularProcessRunnerEntry.setName(name);
			assertEquals(name, regularProcessRunnerEntry.getName());
		}

		@Test
		void testGetSpoolName() {
			assertNull(regularProcessRunnerEntry.getSpoolName());
		}

		@Test
		void testSetSpoolName() {
			regularProcessRunnerEntry.setSpoolName(spoolName);
			assertEquals(spoolName, regularProcessRunnerEntry.getSpoolName());
		}

		@Test
		void testGetComment() {
			assertNull(regularProcessRunnerEntry.getComment());
		}

		@Test
		void testSetComment() {
			regularProcessRunnerEntry.setComment(comment);
			assertEquals(comment, regularProcessRunnerEntry.getComment());
		}

		@Test
		void testGetPeriodTime() {
			assertNull(regularProcessRunnerEntry.getPeriodTime());
		}

		@Test
		void testSetPeriodTime() {
			regularProcessRunnerEntry.setPeriodTime(periodTime);
			assertEquals(periodTime, regularProcessRunnerEntry.getPeriodTime());
		}

		@Test
		void testGetPriority() {
			assertEquals(0, regularProcessRunnerEntry.getPriority());
		}

		@Test
		void testSetPriority() {
			regularProcessRunnerEntry.setPriority(priority);
			assertEquals(priority, regularProcessRunnerEntry.getPriority());
		}

		@Test
		void testGetRetryAfterTimeFactor() {
			assertEquals(0d, regularProcessRunnerEntry.getRetryAfterTimeFactor());
		}

		@Test
		void testSetRetryAfterTimeFactor() {
			regularProcessRunnerEntry.setRetryAfterTimeFactor(retryAfterTimeFactor);
			assertEquals(retryAfterTimeFactor, regularProcessRunnerEntry.getRetryAfterTimeFactor());
		}

		@Test
		void testIsRunFirstAtBoot() {
			assertFalse(regularProcessRunnerEntry.isRunFirstAtBoot());
		}

		@Test
		void testSetRunFirstAtBoot() {
			regularProcessRunnerEntry.setRunFirstAtBoot(runFirstAtBoot);
			assertEquals(runFirstAtBoot, regularProcessRunnerEntry.isRunFirstAtBoot());
		}

		@Test
		void testGetCommandLine() {
			assertNull(regularProcessRunnerEntry.getCommandLine());
		}

		@Test
		void testSetCommandLine() {
			regularProcessRunnerEntry.setCommandLine(commandLine);
			assertEquals(commandLine, regularProcessRunnerEntry.getCommandLine());
		}

		@Test
		void testGetEnv() {
			assertNull(regularProcessRunnerEntry.getEnv());
		}

		@Test
		void testSetEnv() {
			regularProcessRunnerEntry.setEnv(env);
			assertEquals(env, regularProcessRunnerEntry.getEnv());
		}

		@Test
		void testGetWorkingDir() {
			assertNull(regularProcessRunnerEntry.getWorkingDir());
		}

		@Test
		void testSetWorkingDir() {
			regularProcessRunnerEntry.setWorkingDir(workingDir);
			assertEquals(workingDir, regularProcessRunnerEntry.getWorkingDir());
		}

		@Test
		void testGetAfterDone() {
			assertNull(regularProcessRunnerEntry.getAfterDone());

		}

		@Test
		void testSetAfterDone() {
			regularProcessRunnerEntry.setAfterDone(afterDone);
			assertEquals(afterDone, regularProcessRunnerEntry.getAfterDone());
		}

		@Test
		void testGetAfterError() {
			assertNull(regularProcessRunnerEntry.getAfterError());
		}

		@Test
		void testSetAfterError() {
			regularProcessRunnerEntry.setAfterError(afterError);
			assertEquals(afterError, regularProcessRunnerEntry.getAfterError());
		}
	}

	@Nested
	class AfterExecEntryTest {

		@Mock
		Locale lang;
		@Mock
		Map<String, Object> addToTemplateVars;
		@Mock
		List<String> sendTo;
		@Mock
		List<String> sendCc;

		String replyTo;
		String templateName;

		AfterExecEntry afterExecEntry;

		@BeforeEach
		void init() throws Exception {
			MockitoAnnotations.openMocks(this).close();
			replyTo = String.valueOf(random.nextLong());
			templateName = String.valueOf(random.nextLong());

			afterExecEntry = new AfterExecEntry();
		}

		@Test
		void testGetSendTo() {
			assertNull(afterExecEntry.getSendTo());
		}

		@Test
		void testSetSendTo() {
			afterExecEntry.setSendTo(sendTo);
			assertEquals(sendTo, afterExecEntry.getSendTo());
		}

		@Test
		void testGetSendCc() {
			assertNull(afterExecEntry.getSendCc());
		}

		@Test
		void testSetSendCc() {
			afterExecEntry.setSendCc(sendCc);
			assertEquals(sendCc, afterExecEntry.getSendCc());
		}

		@Test
		void testGetReplyTo() {
			assertNull(afterExecEntry.getReplyTo());
		}

		@Test
		void testSetReplyTo() {
			afterExecEntry.setReplyTo(replyTo);
			assertEquals(replyTo, afterExecEntry.getReplyTo());
		}

		@Test
		void testGetTemplateName() {
			assertNull(afterExecEntry.getTemplateName());
		}

		@Test
		void testSetTemplateName() {
			afterExecEntry.setTemplateName(templateName);
			assertEquals(templateName, afterExecEntry.getTemplateName());
		}

		@Test
		void testGetLang() {
			assertNull(afterExecEntry.getLang());
		}

		@Test
		void testSetLang() {
			afterExecEntry.setLang(lang);
			assertEquals(lang, afterExecEntry.getLang());
		}

		@Test
		void testGetAddToTemplateVars() {
			assertNull(afterExecEntry.getAddToTemplateVars());
		}

		@Test
		void testSetAddToTemplateVars() {
			afterExecEntry.setAddToTemplateVars(addToTemplateVars);
			assertEquals(addToTemplateVars, afterExecEntry.getAddToTemplateVars());
		}
	}
}
