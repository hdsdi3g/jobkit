package tv.hd3g.jobkit.mod.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.jobkit.mod.service.ExecFactoryService.FileNotFoundRuntimeException;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.tool.ExecutableTool;

@SpringBootTest
@ActiveProfiles({ "ExecFactoryMock" })
class ExecFactoryTest {

	@Autowired
	ExecutableFinder executableFinder;
	@Autowired
	ExecFactory execFactory;

	@BeforeEach
	void init() {
		Mockito.reset(executableFinder);
	}

	@Test
	void testCreateNewExecString() {
		final var exec = execFactory.createNewExec("java");
		assertNotNull(exec);
		assertEquals("java", exec.getExecutableName());
	}

	@Test
	void testCreateNewExecString_fail() throws FileNotFoundException {
		when(executableFinder.get(eq("java"))).thenThrow(new FileNotFoundException("(only for test)"));
		assertThrows(FileNotFoundRuntimeException.class, () -> execFactory.createNewExec("java"));
	}

	@Test
	void testCreateNewExecExecutableTool() {
		final var executableTool = Mockito.mock(ExecutableTool.class);
		when(executableTool.getExecutableName()).thenReturn("java");

		final var exec = execFactory.createNewExec(executableTool);
		assertNotNull(exec);
		assertEquals("java", exec.getExecutableName());
	}

	@Test
	void testCreateNewExecExecutableTool_fail() throws FileNotFoundException {
		when(executableFinder.get(eq("java"))).thenThrow(new FileNotFoundException("(only for test)"));
		final var executableTool = Mockito.mock(ExecutableTool.class);
		when(executableTool.getExecutableName()).thenReturn("java");
		assertThrows(FileNotFoundRuntimeException.class, () -> execFactory.createNewExec(executableTool));
	}

}
