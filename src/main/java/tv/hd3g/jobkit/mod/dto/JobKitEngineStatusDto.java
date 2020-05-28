package tv.hd3g.jobkit.mod.dto;

import java.util.Objects;

import org.springframework.hateoas.ResourceSupport;

import tv.hd3g.jobkit.engine.status.JobKitEngineStatus;

public class JobKitEngineStatusDto extends ResourceSupport {

	private final JobKitEngineStatus jobKitEngineStatus;

	public JobKitEngineStatusDto(final JobKitEngineStatus jobKitEngineStatus) {
		this.jobKitEngineStatus = jobKitEngineStatus;
	}

	public JobKitEngineStatus getLastStatus() {
		return jobKitEngineStatus;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(jobKitEngineStatus);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final JobKitEngineStatusDto other = (JobKitEngineStatusDto) obj;
		return Objects.equals(jobKitEngineStatus, other.jobKitEngineStatus);
	}
}
