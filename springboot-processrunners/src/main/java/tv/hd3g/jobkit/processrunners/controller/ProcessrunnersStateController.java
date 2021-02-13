package tv.hd3g.jobkit.processrunners.controller;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tv.hd3g.commons.authkit.CheckBefore;
import tv.hd3g.jobkit.processrunners.RegularProcessRunnersConfigurer;
import tv.hd3g.jobkit.processrunners.dto.RegularProcessRunnerListDto;

@RestController
@CheckBefore("jobkitState")
@RequestMapping(value = "/v1/jobkit/processrunners/state", produces = APPLICATION_JSON_VALUE)
public class ProcessrunnersStateController {

	@Autowired
	private RegularProcessRunnersConfigurer regularProcessRunnersConfigurer;

	@GetMapping(value = "conf")
	@CheckBefore("jobkitConf")
	public ResponseEntity<RegularProcessRunnerListDto> getConf() {
		final var result = regularProcessRunnersConfigurer.makeConfigurationDto();
		return new ResponseEntity<>(result, OK);
	}

}
