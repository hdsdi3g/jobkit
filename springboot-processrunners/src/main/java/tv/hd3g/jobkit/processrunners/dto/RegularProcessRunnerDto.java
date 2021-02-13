package tv.hd3g.jobkit.processrunners.dto;

import java.io.File;
import java.util.Objects;
import java.util.Set;

public class RegularProcessRunnerDto {

	private final String name;
	private final String spoolName;
	private final String comment;
	private final String commandLine;
	private final Set<String> env;
	private final File workingDir;

	public RegularProcessRunnerDto(final String name,
	                               final String spoolName,
	                               final String comment,
	                               final String commandLine,
	                               final Set<String> env,
	                               final File workingDir) {
		this.name = name;
		this.spoolName = spoolName;
		this.comment = comment;
		this.commandLine = commandLine;
		this.env = env;
		this.workingDir = workingDir;
	}

	public String getName() {
		return name;
	}

	public String getSpoolName() {
		return spoolName;
	}

	public String getComment() {
		return comment;
	}

	public String getCommandLine() {
		return commandLine;
	}

	public Set<String> getEnv() {
		return env;
	}

	public File getWorkingDir() {
		return workingDir;
	}

	@Override
	public int hashCode() {
		return Objects.hash(commandLine, comment, env, name, spoolName, workingDir);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RegularProcessRunnerDto other = (RegularProcessRunnerDto) obj;
		return Objects.equals(commandLine, other.commandLine) && Objects.equals(comment, other.comment) && Objects
		        .equals(env, other.env) && Objects.equals(name, other.name) && Objects.equals(spoolName,
		                other.spoolName) && Objects.equals(workingDir, other.workingDir);
	}

}
