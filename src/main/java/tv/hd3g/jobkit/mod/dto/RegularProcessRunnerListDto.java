package tv.hd3g.jobkit.mod.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RegularProcessRunnerListDto extends BaseRepresentationModel {

	private final Set<RegularProcessRunnerDto> services;
	private final List<String> execPath;
	private final String senderReference;

	public RegularProcessRunnerListDto(final Set<RegularProcessRunnerDto> services,
	                                   final List<String> execPath,
	                                   final String senderReference) {
		this.services = services;
		this.execPath = execPath;
		this.senderReference = senderReference;
	}

	public Set<RegularProcessRunnerDto> getServices() {
		return services;
	}

	public List<String> getExecPath() {
		return execPath;
	}

	public String getSenderReference() {
		return senderReference;
	}

	@Override
	public int hashCode() {
		final var prime = 31;
		var result = super.hashCode();
		result = prime * result + Objects.hash(execPath, senderReference, services);
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
		final var other = (RegularProcessRunnerListDto) obj;
		return Objects.equals(execPath, other.execPath) && Objects.equals(senderReference, other.senderReference)
		       && Objects.equals(services, other.services);
	}
}
