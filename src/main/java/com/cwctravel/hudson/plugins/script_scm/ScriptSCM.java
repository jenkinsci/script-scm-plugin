package com.cwctravel.hudson.plugins.script_scm;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.StaplerRequest;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Queue.FlyweightTask;
import hudson.model.Queue.NonBlockingTask;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.Change;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.tasks.Ant;
import hudson.tasks.Publisher;
import hudson.triggers.SCMTrigger.SCMTriggerCause;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class ScriptSCM extends SCM {
	private static class TempBuild extends AbstractBuild<TempProject, TempBuild> {

		public TempBuild(TempProject job, Calendar timestamp) {
			super(job, timestamp);
		}

		public TempBuild(TempProject project, File buildDir) throws IOException {
			super(project, buildDir);
		}

		public TempBuild(TempProject job) throws IOException {
			super(job);
		}

		protected TempBuild(TempProject job, FilePath workspace) {
			super(job, new GregorianCalendar());
			setWorkspace(workspace);
		}

		/*
		 * return -1 to make duration be N/A otherwise
		 * 		null pointer exception if @Override is removed
		 * @see hudson.model.Run#getEstimatedDuration()
		 */
		@Override
		public long getEstimatedDuration() {
			return -1;
		};

		@Override
		public void run() {
			TempProject tempProject = this.getProject();
			String targets = tempProject.getTargets();
			String antName = tempProject.getAntName();
			String antOpts = tempProject.getAntOpts();
			String buildFile = tempProject.getBuildFile();
			String properties = tempProject.getPropertiesStr();
			Ant ant = new Ant(targets, antName, antOpts, buildFile, properties);
			try {
				TaskListener listener = tempProject.getListener();
				ant.perform(this, tempProject.getLauncher(), listener instanceof BuildListener ? (BuildListener)listener : new TempBuildListener(listener));
			}
			catch(IOException iE) {
				throw new ScriptTriggerException("Script Execution failed", iE);
			}
			catch(InterruptedException e) {
				throw new ScriptTriggerException("Script Execution failed", e);
			}
			catch(IllegalArgumentException e) {
				throw new ScriptTriggerException("Script Execution failed", e);
			}
			catch(SecurityException e) {
				throw new ScriptTriggerException("Script Execution failed", e);
			}
		}
	}

	private static class TempProject extends AbstractProject<TempProject, TempBuild> implements FlyweightTask, NonBlockingTask {
		private String targets;

		private String antName;
		private String antOpts;
		private String buildFile;
		private String properties;

		private Launcher launcher;
		private TaskListener listener;
		FilePath projectWorkspace;

		protected TempProject(ItemGroup parent, String name) {
			super(parent, name);

			this.disabled = false;
		}

		@Override
		protected TempBuild newBuild() throws IOException {
			return new TempBuild(this, this.getProjectWorkspace());
		}

		@Override
		public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
			return null;
		}

		@Override
		protected Class<TempBuild> getBuildClass() {
			return TempBuild.class;
		}

		@Override
		public boolean isFingerprintConfigured() {
			return false;
		}

		@Override
		protected void buildDependencyGraph(DependencyGraph paramDependencyGraph) {}

		public String getTargets() {
			return targets;
		}

		public void setTargets(String targets) {
			this.targets = targets;
		}

		public String getAntName() {
			return antName;
		}

		public void setAntName(String antName) {
			this.antName = antName;
		}

		public String getAntOpts() {
			return antOpts;
		}

		public void setAntOpts(String antOpts) {
			this.antOpts = antOpts;
		}

		public String getBuildFile() {
			return buildFile;
		}

		public void setBuildFile(String buildFile) {
			this.buildFile = buildFile;
		}

		public String getPropertiesStr() {
			return properties;
		}

		public void setPropertiesStr(String properties) {
			this.properties = properties;
		}

		public Launcher getLauncher() {
			return launcher;
		}

		public void setLauncher(Launcher launcher) {
			this.launcher = launcher;
		}

		public TaskListener getListener() {
			return listener;
		}

		public void setListener(TaskListener listener) {
			this.listener = listener;
		}

		public FilePath getProjectWorkspace() {
			return projectWorkspace;
		}

		public void setProjectWorkspace(FilePath projectWorkspace) {
			this.projectWorkspace = projectWorkspace;
		}
	}

	private static class TempBuildListener implements BuildListener {
		private static final long serialVersionUID = 8702160265256733186L;

		private final TaskListener listener;

		public TempBuildListener(TaskListener listener) {
			this.listener = listener;
		}

		@Override
		public PrintStream getLogger() {
			return listener.getLogger();
		}

		@Override
		public void annotate(ConsoleNote ann) throws IOException {
			listener.annotate(ann);

		}

		@Override
		public void hyperlink(String url, String text) throws IOException {
			listener.hyperlink(url, text);
		}

		@Override
		public PrintWriter error(String msg) {
			return listener.error(msg);
		}

		@Override
		public PrintWriter error(String format, Object... args) {
			return listener.error(format, args);
		}

		@Override
		public PrintWriter fatalError(String msg) {
			return listener.fatalError(msg);
		}

		@Override
		public PrintWriter fatalError(String format, Object... args) {
			return listener.fatalError(format, args);
		}

		@Override
		public void started(List<Cause> causes) {

		}

		@Override
		public void finished(Result result) {

		}

	}

	public static class PropertiesBuilder {
		private final Map<String, String> properties = new HashMap<String, String>();

		public PropertiesBuilder put(String key, Object value) {
			properties.put(key, value != null ? value.toString() : "");
			return this;
		}

		public PropertiesBuilder clear() {
			properties.clear();
			return this;
		}

		public PropertiesBuilder remove(String key) {
			properties.remove(key);
			return this;
		}

		@Override
		public String toString() {
			StringWriter sW = new StringWriter();
			Properties props = new Properties();
			props.putAll(properties);
			properties.clear();
			try {
				props.store(sW, "");
			}
			catch(IOException e) {

			}

			return sW.toString();
		}
	}

	public static class Utils {
		public String escapePropertyValue(String str) {
			if(str != null) {
				return str.replace("\\", "\\\\").replace("\r", "").replace("\n", "");
			}

			return str;
		}

		public AbstractBuild<?, ?> getLastSuccessfulSCMBuild(AbstractProject<?, ?> project) {
			if(project != null) {
				AbstractBuild<?, ?> b = project.getLastBuild();
				// temporary hack till we figure out what's causing this bug
				while(b != null && (b.isBuilding() || b.getResult() == null || b.getResult().isWorseThan(Result.UNSTABLE)) && (b.getCause(SCMTriggerCause.class) != null)) {
					b = b.getPreviousBuild();
				}
				return b;
			}
			return null;
		}
	}

	private String groovyScript;
	private String groovyScriptFile;
	private String bindings;
	private String groovyClasspath;

	public ScriptSCM(String groovyScript, String groovyScriptFile, String bindings, String groovyClasspath) {
		this.groovyScript = Util.fixEmpty(groovyScript);
		this.groovyScriptFile = Util.fixEmpty(groovyScriptFile);
		this.bindings = Util.fixEmpty(bindings);
		this.groovyClasspath = groovyClasspath;
	}

	public boolean executeAnt(AbstractProject<?, ?> project, FilePath workspace, Launcher launcher, TaskListener listener, String targets,
			String antName, String antOpts, String buildFile, String properties) throws ScriptTriggerException {

		TempProject tempProject = new TempProject(project.getParent(), "ant-perform");
		tempProject.setTargets(targets);
		tempProject.setAntName(antName);
		tempProject.setAntOpts(antOpts);
		tempProject.setBuildFile(buildFile);
		tempProject.setPropertiesStr(properties);
		tempProject.setLauncher(launcher);
		tempProject.setListener(listener);
		tempProject.setProjectWorkspace(workspace);

		try {
			long TIMEOUT_PERIOD = 150000; // try for 2.5 minutes before failing

			tempProject.createExecutable();
			tempProject.scheduleBuild(null);

			Future<TempBuild> futureTempBuild = tempProject.scheduleBuild2(0, null);

			TempBuild newTempBuild = futureTempBuild.get(TIMEOUT_PERIOD, TimeUnit.MILLISECONDS);

			return Result.SUCCESS.equals(newTempBuild.getResult());
		}
		catch(InterruptedException e) {
			throw new ScriptTriggerException(e);
		}
		catch(ExecutionException e) {
			throw new ScriptTriggerException(e);
		}
		catch(TimeoutException e) {
			throw new ScriptTriggerException(e);
		}
		catch(IOException e) {
			throw new ScriptTriggerException(e);
		}
	}

	public boolean executeAnt(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener, String targets, String antName, String antOpts,
			String buildFile, String properties) throws ScriptTriggerException {
		Ant ant = new Ant(targets, antName, antOpts, buildFile, properties);
		try {
			return ant.perform(build, launcher, listener instanceof BuildListener ? (BuildListener)listener : new TempBuildListener(listener));
		}
		catch(IOException iE) {
			throw new ScriptTriggerException("Script Execution failed", iE);
		}
		catch(InterruptedException e) {
			throw new ScriptTriggerException("Script Execution failed", e);
		}
		catch(IllegalArgumentException e) {
			throw new ScriptTriggerException("Script Execution failed", e);
		}
		catch(SecurityException e) {
			throw new ScriptTriggerException("Script Execution failed", e);
		}
	}

	private void evaluateGroovyScript(File workspace, Map<String, Object> input) throws IOException {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		if(!StringUtils.isBlank(groovyClasspath)) {
			compilerConfiguration.setClasspath(groovyClasspath);
		}

		Jenkins instance = Jenkins.getInstance();
		if(instance != null) {
			ClassLoader cl = instance.getPluginManager().uberClassLoader;

			if(cl == null) {
				cl = Thread.currentThread().getContextClassLoader();
			}

			GroovyShell groovyShell = new GroovyShell(cl, new Binding(), compilerConfiguration);

			if(input != null) {
				setGroovySystemObjects(input);
				for(Map.Entry<String, Object> entry: input.entrySet()) {
					groovyShell.setVariable(entry.getKey(), entry.getValue());
				}
				if(groovyScriptFile != null) {
					File scriptFile = new File(groovyScriptFile);
					if(!scriptFile.exists()) {
						scriptFile = new File(workspace, groovyScriptFile);
					}
					String groovyScript = Util.loadFile(scriptFile);
					groovyShell.evaluate(groovyScript);
				}
				else {
					groovyShell.evaluate(groovyScript);
				}
			}
		}
	}

	private void setGroovySystemObjects(Map<String, Object> input) throws IOException {
		if(input != null) {
			input.put("propertiesBuilder", new PropertiesBuilder());
			input.put("utils", new Utils());

			if(bindings != null) {
				Properties p = new Properties();
				p.load(new StringReader(bindings));
				for(Map.Entry<Object, Object> entry: p.entrySet()) {
					input.put((String)entry.getKey(), entry.getValue());
				}
			}
		}
	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher,
			TaskListener listener) throws IOException, InterruptedException {
		Map<String, Object> input = new HashMap<String, Object>();
		FilePath workspace = build.getWorkspace();
		if(workspace != null) {
			input.put("action", "calcRevisionsFromBuild");
			input.put("build", build);
			input.put("launcher", launcher);
			input.put("listener", listener);
			input.put("scm", this);

			FilePath filePath = workspace.createTempFile("revision-state-", "");
			input.put("revisionStatePath", filePath.getRemote());
			input.put("workspacePath", workspace.getRemote());
			input.put("rootPath", build.getRootDir().getAbsolutePath());

			ScriptSCMRevisionState result = null;
			try {
				evaluateGroovyScript(new File(workspace.getRemote()), input);
				result = new ScriptSCMRevisionState();
				result.setRevisionState(filePath.readToString());
			}
			finally {
				filePath.delete();
			}
			return result;
		}
		else {
			throw new IOException("workspace is null");
		}
	}

	@Override
	public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener,
			File changelogFile) throws IOException, InterruptedException {
		Map<String, Object> input = new HashMap<String, Object>();
		input.put("action", "checkout");
		input.put("build", build);
		input.put("launcher", launcher);
		input.put("listener", listener);
		input.put("workspace", workspace);
		input.put("scm", this);
		input.put("workspacePath", workspace.getRemote());
		input.put("changeLogPath", changelogFile.getParentFile().getAbsolutePath());
		input.put("changeLogFile", changelogFile.getAbsolutePath());

		FilePath currentRevisionStatePath = workspace.createTempFile("current-revision", "");
		input.put("currentRevisionStatePath", currentRevisionStatePath.getRemote());

		try {
			evaluateGroovyScript(new File(workspace.getRemote()), input);
			if(currentRevisionStatePath.length() > 0) {
				ScriptSCMRevisionState scmRevisionState = new ScriptSCMRevisionState();
				scmRevisionState.setRevisionState(currentRevisionStatePath.readToString());
				build.addAction(scmRevisionState);
			}
		}
		finally {
			currentRevisionStatePath.delete();
		}
		return true;
	}

	@Override
	protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener,
			SCMRevisionState baseline) throws IOException, InterruptedException {
		PollingResult result = PollingResult.NO_CHANGES;

		AbstractBuild<?, ?> lastBuild = project.getLastBuild();
		if(lastBuild == null || !lastBuild.isBuilding()) {
			Map<String, Object> input = new HashMap<String, Object>();
			input.put("action", "compareRemoteRevisionWith");
			input.put("project", project);
			input.put("launcher", launcher);
			input.put("listener", listener);
			input.put("workspace", workspace);
			input.put("scm", this);
			input.put("baseline", baseline);
			input.put("workspacePath", workspace.getRemote());

			FilePath changeResultPath = workspace.createTempFile("change-result", "");
			input.put("changeResultPath", changeResultPath.getRemote());

			FilePath currentRevisionStatePath = workspace.createTempFile("current-revision", "");
			input.put("currentRevisionStatePath", currentRevisionStatePath.getRemote());

			try {
				evaluateGroovyScript(new File(workspace.getRemote()), input);

				ScriptSCMRevisionState remoteRevisionState = new ScriptSCMRevisionState();
				remoteRevisionState.setRevisionState(currentRevisionStatePath.readToString());
				result = new PollingResult(baseline, remoteRevisionState, Change.valueOf(changeResultPath.readToString()));
			}
			finally {
				changeResultPath.delete();
				currentRevisionStatePath.delete();
			}
		}
		return result;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ScriptSCMChangeLogParser();
	}

	@Override
	public RepositoryBrowser<ScriptSCMChangeLogEntry> getBrowser() {
		return new ScriptSCMBrowser();
	}

	@Extension
	public static final class DescriptorImpl extends SCMDescriptor<ScriptSCM> {
		public DescriptorImpl() {
			super(ScriptSCM.class, ScriptSCMBrowser.class);
			load();
		}

		@Override
		public ScriptSCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			String groovyScript = null;
			String groovyScriptFile = null;
			String bindings = null;
			String groovyClasspath = null;

			JSONObject jsonObject = (JSONObject)formData.get("scriptSource");
			if(jsonObject != null) {
				if(jsonObject.getInt("value") == 0) {
					groovyScript = jsonObject.getString("groovyScript");
				}
				else if(jsonObject.getInt("value") == 1) {
					groovyScriptFile = jsonObject.getString("groovyScriptFile");
				}
			}

			bindings = formData.getString("bindings");
			groovyClasspath = formData.optString("groovyClasspath");

			return new ScriptSCM(groovyScript, groovyScriptFile, bindings, groovyClasspath);
		}

		@Override
		public String getDisplayName() {
			return "Script SCM";
		}
	}

	public String getGroovyScript() {
		return groovyScript;
	}

	public void setGroovyScript(String groovyScript) {
		this.groovyScript = groovyScript;
	}

	public String getGroovyScriptFile() {
		return groovyScriptFile;
	}

	public void setGroovyScriptFile(String groovyScriptFile) {
		this.groovyScriptFile = groovyScriptFile;
	}

	public String getBindings() {
		return bindings;
	}

	public void setBindings(String bindings) {
		this.bindings = bindings;
	}

	public String getGroovyClasspath() {
		return groovyClasspath;
	}

	public void setGroovyClasspath(String groovyClasspath) {
		this.groovyClasspath = groovyClasspath;
	}

}
