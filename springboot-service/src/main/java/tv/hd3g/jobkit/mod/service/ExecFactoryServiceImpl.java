package tv.hd3g.jobkit.mod.service;

import java.io.FileNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.tool.ExecutableTool;

@Service
public class ExecFactoryServiceImpl implements ExecFactoryService {

	@Autowired
	ExecutableFinder executableFinder;

	@Override
	public Exec createNewExec(final String execName) {
		try {
			return new Exec(execName, executableFinder);
		} catch (final FileNotFoundException e) {
			throw new FileNotFoundRuntimeException(e);
		}
	}

	@Override
	public Exec createNewExec(final ExecutableTool tool) {
		try {
			return new Exec(tool, executableFinder);
		} catch (final FileNotFoundException e) {
			throw new FileNotFoundRuntimeException(e);
		}
	}

	public static class FileNotFoundRuntimeException extends RuntimeException {
		public FileNotFoundRuntimeException(final FileNotFoundException e) {
			super(e);
		}
	}

}
