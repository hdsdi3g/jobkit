package tv.hd3g.jobkit.processrunners.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "DefaultMock" })
class JobKitStateControllerTest {

	private static final String baseMapping = ProcessrunnersStateController.class.getAnnotation(RequestMapping.class)
	        .value()[0];
	private static final ResultMatcher statusOkUtf8 = ResultMatcher.matchAll(
	        status().isOk(),
	        content().contentType(APPLICATION_JSON_VALUE));

	@Mock
	HttpServletRequest request;

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	JobKitEngine jobKitEngine;

	HttpHeaders baseHeaders;

	@BeforeEach
	private void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		Mockito.reset(jobKitEngine);
		// DataGenerator.setupMock(request);

		baseHeaders = new HttpHeaders();
		baseHeaders.setAccept(Arrays.asList(APPLICATION_JSON));
	}

	@Test
	void testGetConf() throws Exception {
		mvc.perform(get(baseMapping + "/" + "conf")
		        .headers(baseHeaders))
		        .andExpect(statusOkUtf8)
		        .andExpect(jsonPath("$.senderReference").value("send-ref-email"));
	}

}
