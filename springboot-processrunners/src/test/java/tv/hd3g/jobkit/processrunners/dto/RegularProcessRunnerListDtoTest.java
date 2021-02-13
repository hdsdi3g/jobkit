package tv.hd3g.jobkit.processrunners.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tv.hd3g.testtools.HashCodeEqualsTest;

class RegularProcessRunnerListDtoTest extends HashCodeEqualsTest {
	static Random random = new Random();

	@Mock
	Set<RegularProcessRunnerDto> services;
	@Mock
	List<String> execPath;
	String senderReference;

	RegularProcessRunnerListDto regularProcessRunnerListDto;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		senderReference = String.valueOf(random.nextLong());
		regularProcessRunnerListDto = new RegularProcessRunnerListDto(services, execPath, senderReference);
	}

	@Test
	void testGetServices() {
		assertEquals(services, regularProcessRunnerListDto.getServices());
	}

	@Test
	void testGetExecPath() {
		assertEquals(execPath, regularProcessRunnerListDto.getExecPath());
	}

	@Test
	void testGetSenderReference() {
		assertEquals(senderReference, regularProcessRunnerListDto.getSenderReference());
	}

	@Override
	protected Object[] makeSameInstances() {
		return new Object[] { regularProcessRunnerListDto,
		                      new RegularProcessRunnerListDto(services, execPath, senderReference) };
	}
}
