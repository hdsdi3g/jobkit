package tv.hd3g.jobkit.mod.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tv.hd3g.commons.authkit.CheckBefore;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.RegularProcessRunnersConfigurer;
import tv.hd3g.jobkit.mod.dto.BackgroundServiceIdDto;
import tv.hd3g.jobkit.mod.dto.BaseRepresentationModel;
import tv.hd3g.jobkit.mod.dto.JobKitEngineStatusDto;
import tv.hd3g.jobkit.mod.dto.RegularProcessRunnerListDto;
import tv.hd3g.jobkit.mod.dto.WsDtoLink;

@RestController
@CheckBefore("jobkitState")
@RequestMapping(value = "/v1/jobkit/state", produces = APPLICATION_JSON_VALUE)
public class JobKitStateController {

	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private RegularProcessRunnersConfigurer regularProcessRunnersConfigurer;
	@Autowired
	private BackgroundServiceId backgroundServiceId;

	@GetMapping(value = "status")
	@CheckBefore("jobkitStatus")
	public ResponseEntity<JobKitEngineStatusDto> getLastStatus() {
		final var result = new JobKitEngineStatusDto(jobKitEngine.getLastStatus());
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@GetMapping(value = "conf")
	@CheckBefore("jobkitConf")
	public ResponseEntity<RegularProcessRunnerListDto> getConf() {
		final var result = regularProcessRunnersConfigurer.makeConfigurationDto();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@GetMapping(value = "ids")
	@CheckBefore("jobkitStatus")
	public ResponseEntity<BackgroundServiceIdDto> getIds() {
		final var result = backgroundServiceId.getAllRegistedAsDto();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	private void createHateoasLinks(final BaseRepresentationModel res) {
		prepHLink(res, JobKitStateController::getLastStatus, "getLastStatus", GET);
		prepHLink(res, JobKitStateController::getConf, "getConf", GET);
		prepHLink(res, JobKitStateController::getIds, "getIds", GET);
	}

	/**
	 * prepareHateoasLink
	 */
	private void prepHLink(final BaseRepresentationModel ressource,
	                       final Function<JobKitStateController, Object> linkTo,
	                       final String rel,
	                       final RequestMethod method) {
		final var c = JobKitStateController.class;
		final var link = linkTo.apply(methodOn(c));
		ressource.add(new WsDtoLink(linkTo(link).withRel(rel), method));
	}

}
