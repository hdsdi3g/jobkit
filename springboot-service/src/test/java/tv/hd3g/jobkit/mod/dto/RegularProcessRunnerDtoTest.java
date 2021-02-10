package tv.hd3g.jobkit.mod.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.testtools.HashCodeEqualsTest;

class RegularProcessRunnerDtoTest extends HashCodeEqualsTest {
	static Random random = new Random();

	@Mock
	Set<String> env;
	@Mock
	File workingDir;

	String name;
	String spoolName;
	String comment;
	String commandLine;

	RegularProcessRunnerDto regularProcessRunnerDto;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		name = String.valueOf(random.nextLong());
		spoolName = String.valueOf(random.nextLong());
		comment = String.valueOf(random.nextLong());
		commandLine = String.valueOf(random.nextLong());

		regularProcessRunnerDto = new RegularProcessRunnerDto(name, spoolName, comment, commandLine, env, workingDir);
	}

	@Test
	void testGetName() {
		assertEquals(name, regularProcessRunnerDto.getName());
	}

	@Test
	void testGetSpoolName() {
		assertEquals(spoolName, regularProcessRunnerDto.getSpoolName());
	}

	@Test
	void testGetComment() {
		assertEquals(comment, regularProcessRunnerDto.getComment());
	}

	@Test
	void testGetCommandLine() {
		assertEquals(commandLine, regularProcessRunnerDto.getCommandLine());
	}

	@Test
	void testGetEnv() {
		assertEquals(env, regularProcessRunnerDto.getEnv());
	}

	@Test
	void testGetWorkingDir() {
		assertEquals(workingDir, regularProcessRunnerDto.getWorkingDir());
	}

	@Override
	protected Object[] makeSameInstances() {
		return new Object[] { regularProcessRunnerDto,
		                      new RegularProcessRunnerDto(name, spoolName, comment, commandLine, env, workingDir) };
	}
}
