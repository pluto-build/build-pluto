package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionError;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;

import build.pluto.BuildUnit.State;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildjava.JavaBulkBuilder;
import build.pluto.buildjava.JavaInput;
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
    	JavaInput javaInput = new JavaInput
				.Builder()
				.addInputFiles(testSourceFiles)
				.setSourceOrigin(compilerOrigin.get())
				.setTargetDir(input.testBinDir)
				.addSourcePaths(input.testSourceDir)
				.addClassPaths(mavenJars)
				.addClassPaths(input.sourceClassPath)
				.setCompiler(JavacCompiler.instance)
				.get();
    	requireBuild(JavaBulkBuilder.factory, javaInput);

    	// 3.c) run tests
    	List<File> classpath = new ArrayList<>(mavenJars);
    	classpath.add(input.testBinDir.getAbsoluteFile());
    	for (File cp : input.sourceClassPath)
    		classpath.add(cp.getAbsoluteFile());
    	String classpathString = StringCommands.printListSeparated(classpath, ":");

    	report("Execute pluto unit tests");
    	try {
    		// Start new JVM in separate process for testing 
	    	Exec.run(input.testSourceDir.getParentFile(), 
	    			"java",
	    			"-cp", classpathString,
	    			"org.junit.runner.JUnitCore",
	    			"build.pluto.test.PlutoTestSuite");
    	} catch (ExecutionError e) {
    		String msg = StringCommands.printListSeparated(e.outMsgs, "\n") + StringCommands.printListSeparated(e.errMsgs, "\n");
    		String cmd = StringCommands.printListSeparated(e.cmds, " ");
    		reportError("Pluto tests failed>>>\n" + cmd + "\n" + msg);
    		report("<<<Pluto tests failed");
    		setState(State.FAILURE);
    	}
    	
    	return OutputPersisted.of(Collections.unmodifiableList(classpath));
    }
}
