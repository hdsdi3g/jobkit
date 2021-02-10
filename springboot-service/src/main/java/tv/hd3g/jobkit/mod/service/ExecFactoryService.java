package tv.hd3g.jobkit.mod.service;

import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.tool.ExecutableTool;

public interface ExecFactoryService {

	Exec createNewExec(final String execName);

	Exec createNewExec(final ExecutableTool tool);

}
