package tv.hd3g.jobkit.mod.controller;

import static java.time.temporal.ChronoUnit.SECONDS;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tv.hd3g.commons.authkit.CheckBefore;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.mod.BackgroundServiceId;
import tv.hd3g.jobkit.mod.dto.BaseRepresentationModel;
import tv.hd3g.jobkit.mod.dto.WsDtoLink;
import tv.hd3g.jobkit.mod.exception.JobKitRestException;

@RestController
@CheckBefore("jobkitAction")
@RequestMapping(value = "/v1/jobkit/action", produces = APPLICATION_JSON_VALUE)
@Validated
public class JobKitActionController {

	private static final String UUID_VAR = "uuid";
	@Autowired
	private JobKitEngine jobKitEngine;
	@Autowired
	private BackgroundServiceId backgroundServiceId;

	@PutMapping(value = "{uuid}/enable")
	public ResponseEntity<BaseRepresentationModel> enable(@PathVariable("uuid") @NotEmpty final String uuid) {
		getBackgroundServiceByUUID(uuid).enable();
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@PutMapping(value = "{uuid}/disable")
	public ResponseEntity<BaseRepresentationModel> disable(@PathVariable("uuid") @NotEmpty final String uuid) {
		getBackgroundServiceByUUID(uuid).disable();
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@PutMapping(value = "{uuid}/timed-interval/{duration}")
	public ResponseEntity<BaseRepresentationModel> setTimedInterval(@PathVariable("uuid") @NotEmpty final String uuid,
	                                                                @PathVariable("duration") @Positive final long duration) {
		getBackgroundServiceByUUID(uuid).setTimedInterval(Duration.of(duration, SECONDS));
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@PutMapping(value = "{uuid}/priority/{priority}")
	public ResponseEntity<BaseRepresentationModel> setPriority(@PathVariable("uuid") @NotEmpty final String uuid,
	                                                           @PathVariable("priority") @NotNull final int priority) {
		getBackgroundServiceByUUID(uuid).setPriority(priority);
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@PutMapping(value = "{uuid}/retry-after-time-factor/{factor}")
	public ResponseEntity<BaseRepresentationModel> setRetryAfterTimeFactor(@PathVariable("uuid") @NotEmpty final String uuid,
	                                                                       @PathVariable("factor") @Positive final double factor) {
		getBackgroundServiceByUUID(uuid).setRetryAfterTimeFactor(factor);
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	private BackgroundService getBackgroundServiceByUUID(final String uuid) {
		return Optional.ofNullable(backgroundServiceId.getByUUID(UUID.fromString(uuid)))
		        .orElseThrow(() -> new JobKitRestException(SC_NOT_FOUND, "Can't found this service UUID"));
	}

	@PutMapping(value = "all/enable")
	public ResponseEntity<BaseRepresentationModel> enableAll() {
		backgroundServiceId.forEach(BackgroundService::enable);
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@PutMapping(value = "all/disable")
	public ResponseEntity<BaseRepresentationModel> disableAll() {
		backgroundServiceId.forEach(BackgroundService::disable);
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	@PutMapping(value = "shutdown")
	public ResponseEntity<BaseRepresentationModel> shutdown() {
		jobKitEngine.shutdown();
		final var result = new BaseRepresentationModel();
		createHateoasLinks(result);
		return new ResponseEntity<>(result, OK);
	}

	private void createHateoasLinks(final BaseRepresentationModel res) {
		prepHLink(res, c -> c.enable(UUID_VAR), "enable", PUT);
		prepHLink(res, c -> c.disable(UUID_VAR), "disable", PUT);
		prepHLink(res, c -> c.setTimedInterval(UUID_VAR, 1), "setTimedInterval", PUT);
		prepHLink(res, c -> c.setPriority(UUID_VAR, 0), "setPriority", PUT);
		prepHLink(res, c -> c.setRetryAfterTimeFactor(UUID_VAR, 1d), "setRetryAfterTimeFactor", PUT);
		prepHLink(res, JobKitActionController::enableAll, "enableAll", PUT);
		prepHLink(res, JobKitActionController::disableAll, "disableAll", PUT);
		prepHLink(res, JobKitActionController::shutdown, "shutdown", PUT);
	}

	/**
	 * prepareHateoasLink
	 */
	private void prepHLink(final BaseRepresentationModel ressource,
	                       final Function<JobKitActionController, Object> linkTo,
	                       final String rel,
	                       final RequestMethod method) {
		final var c = JobKitActionController.class;
		final var link = linkTo.apply(methodOn(c));
		ressource.add(new WsDtoLink(linkTo(link).withRel(rel), method));
	}

}
