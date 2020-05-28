package tv.hd3g.jobkit.mod.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.engine.status.JobKitEngineStatus;
import tv.hd3g.jobkit.engine.status.SpoolerStatus;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.dto.BackgroundServiceIdDto;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "DefaultMock" })
class JobKitStateControllerTest {

	private static final String baseMapping = JobKitStateController.class.getAnnotation(RequestMapping.class)
	        .value()[0];
	private static final ResultMatcher statusOkUtf8Hateoas = ResultMatcher.matchAll(
	        status().isOk(),
	        content().contentType(APPLICATION_JSON_UTF8),
	        checkHateoasPresence());

	@Mock
	HttpServletRequest request;

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	BackgroundServiceId backgroundServiceId;

	HttpHeaders baseHeaders;

	@BeforeEach
	private void init() {
		MockitoAnnotations.initMocks(this);
		Mockito.reset(jobKitEngine, backgroundServiceId);
		// DataGenerator.setupMock(request);

		baseHeaders = new HttpHeaders();
		baseHeaders.setAccept(Arrays.asList(APPLICATION_JSON_UTF8));
	}

	private static ResultMatcher checkHateoasPresence() {
		final var linkPresence = jsonPath("$.links").isArray();
		return ResultMatcher.matchAll(linkPresence);
	}

	@Test
	void testGetLastStatus() throws Exception {
		final var spoolerStatus = new SpoolerStatus(Set.of(), 1, false);
		final var status = new JobKitEngineStatus(spoolerStatus, Set.of());
		when(jobKitEngine.getLastStatus()).thenReturn(status);

		mvc.perform(get(baseMapping + "/" + "status")
		        .headers(baseHeaders))
		        .andExpect(statusOkUtf8Hateoas)
		        .andExpect(jsonPath("$.lastStatus").isMap());

		verify(jobKitEngine, times(1)).getLastStatus();
	}

	@Test
	void testGetConf() throws Exception {
		mvc.perform(get(baseMapping + "/" + "conf")
		        .headers(baseHeaders))
		        .andExpect(statusOkUtf8Hateoas)
		        .andExpect(jsonPath("$.senderReference").value("send-ref-email"));
	}

	@Test
	void testGetIds() throws Exception {
		final var dto = new BackgroundServiceIdDto(Set.of());
		when(backgroundServiceId.getAllRegistedAsDto()).thenReturn(dto);

		mvc.perform(get(baseMapping + "/" + "ids")
		        .headers(baseHeaders))
		        .andExpect(statusOkUtf8Hateoas)
		        .andExpect(jsonPath("$.servicesIds").isArray());

		verify(backgroundServiceId, times(1)).getAllRegistedAsDto();
	}

}
