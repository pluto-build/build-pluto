package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.common.Exec.ExecutionError;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;

import build.pluto.BuildUnit.State;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.RequiredBuilderFailed;
import build.pluto.buildjava.JavaBulkCompiler;
import build.pluto.buildjava.JavaCompilerInput;
import build.pluto.buildjava.JavaRunner;
import build.pluto.buildjava.JavaRunnerInput;
import build.pluto.buildjava.compiler.JavacCompiler;
import build.pluto.buildjava.util.FileExtensionFilter;
import build.pluto.buildmaven.MavenDependencyResolver;
import build.pluto.buildmaven.input.MavenInput;
import build.pluto.dependency.Origin;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;

public class TestSourceCode extends Builder<TestSourceCode.Input, Out<List<File>>> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File testSourceDir;
        public final File testDataDir;
        public final Origin testSourceOrigin;
        public final File testBinDir;
        public final List<File> sourceClassPath;
        public final Origin sourceClassesOrigin;
        public final File targetDir;

        /**
         * @param testSourceDir Base directory for test code (not source code).
         * @param testBinDir Base directory for compiled test code.
         * @param sourceClassPath Classpath required for compiled source code.
         * @param targetDir Base directory for other generated artifacts.
         */
        public Input(
        		File testSourceDir,
        		File testDataDir,
        		Origin testSourceOrigin,
                File testBinDir,
                List<File> sourceClassPath,
                Origin sourceClassesOrigin,
                File targetDir) {
        	this.testSourceDir = testSourceDir;
        	this.testDataDir = testDataDir;
        	this.testSourceOrigin = testSourceOrigin;
            this.testBinDir = testBinDir;
            this.sourceClassPath = sourceClassPath;
            this.sourceClassesOrigin = sourceClassesOrigin;
            this.targetDir = targetDir;
        }
    }
    
    public static BuilderFactory<Input, Out<List<File>>, TestSourceCode> factory = BuilderFactoryFactory.of(TestSourceCode.class, Input.class);

    public TestSourceCode(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.testBinDir, "pluto.test.dep");
    }

    @Override
    protected String description(Input input) {
        return "Test pluto";
    }

    @Override
    protected Out<List<File>> build(Input input) throws Throwable {

    	Origin.Builder compilerOrigin = Origin
    			.Builder()
    			.add(input.testSourceOrigin)
    			.add(input.sourceClassesOrigin);
    	
    	Origin.Builder javaOrigin = Origin
    			.Builder()
    			.add(input.sourceClassesOrigin);
    	
    	// 3.a) resolve maven test dependencies
    	MavenInput mavenInput = new MavenInput
    			.Builder()
    			.addDependency(ExternalDependencies.JUNIT)
    			.build();
    	List<File> mavenJars = requireBuild(MavenDependencyResolver.factory, mavenInput).val();
    	compilerOrigin.add(lastBuildReq());
    	
    	// 3.b) compile pluto test code
    	requireBuild(input.testSourceOrigin);
    	List<File> testSourceFiles = FileCommands.listFilesRecursive(input.testSourceDir, new FileExtensionFilter("java"));
    	JavaCompilerInput javacInput = new JavaCompilerInput
				.Builder()
				.addInputFiles(testSourceFiles)
				.setSourceOrigin(compilerOrigin.get())
				.setTargetDir(input.testBinDir)
				.addSourcePaths(input.testSourceDir)
				.addClassPaths(mavenJars)
				.addClassPaths(input.sourceClassPath)
				.setCompiler(JavacCompiler.instance)
				.get();
    	requireBuild(JavaBulkCompiler.factory, javacInput);
    	javaOrigin.add(lastBuildReq());

    	// 3.c) run tests
    	FileCommands.copyDirectory(input.testDataDir, new File(input.testBinDir, "testdata"));
    	
    	JavaRunnerInput javaInput = JavaRunnerInput
    			.Builder()
    			.setDescription("Execute pluto unit tests")
    			.setWorkingDir(input.testBinDir)
    			.setMainClass("org.junit.runner.JUnitCore")
    			.addProgramArgs("build.pluto.test.PlutoTestSuite")
    			.addClassPaths(input.testBinDir)
    			.addClassPaths(input.sourceClassPath)
    			.addClassPaths(mavenJars)
    			.setClassOrigin(javaOrigin.get())
    			.get();
    	
    	try {
			ExecutionResult er = requireBuild(JavaRunner.factory, javaInput).val();
			if (er.outMsgs.length > 1) {
				String status = er.outMsgs[er.outMsgs.length - 2];
				report("Test result " + status);
			}
			else {
				String msg = StringCommands.printListSeparated(er.outMsgs, "\n") + StringCommands.printListSeparated(er.errMsgs, "\n");
				String cmd = StringCommands.printListSeparated(er.cmds, " ");
				throw new IllegalStateException("Could not find status message of test execution: " + cmd + "\n" + msg);
			}
    	} catch (RequiredBuilderFailed e) {
    		Throwable cause = e.getRootCause();
			if (cause instanceof ExecutionError) {
				ExecutionError ee = (ExecutionError) cause;
	    		String msg = StringCommands.printListSeparated(ee.outMsgs, "\n") + StringCommands.printListSeparated(ee.errMsgs, "\n");
	    		String cmd = StringCommands.printListSeparated(ee.cmds, " ");
	    		reportError("Pluto tests failed>>>\n" + cmd + "\n" + msg + "\n" + "<<<Pluto tests failed");
	    		setState(State.FAILURE);
			}
    	}
    	
		List<File> classpath = new ArrayList<>();
		classpath.add(input.testBinDir);
		classpath.addAll(input.sourceClassPath);
		classpath.addAll(mavenJars);

    	return OutputPersisted.of(Collections.unmodifiableList(classpath));
    }
}
