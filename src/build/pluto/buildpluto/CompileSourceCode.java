package build.pluto.buildpluto;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.FileCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildgit.GitInput;
import build.pluto.buildgit.GitRemoteSynchronizer;
import build.pluto.buildjava.JavaBulkCompiler;
import build.pluto.buildjava.JavaCompilerInput;
import build.pluto.buildjava.compiler.JavacCompiler;
import build.pluto.buildjava.util.FileExtensionFilter;
import build.pluto.buildmaven.MavenDependencyResolver;
import build.pluto.buildmaven.MavenPackager;
import build.pluto.buildmaven.input.MavenPackagerInput;
import build.pluto.buildmaven.input.MavenInput;
import build.pluto.dependency.Origin;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;

public class CompileSourceCode extends Builder<CompileSourceCode.Input, Out<List<File>>> {

    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File sourceDir;
        public final Origin sourceOrigin;
        public final File binDir;
        public final File targetDir;

        /**
         * @param sourceDir Base directory for source code (not test code).
         * @param binDir Base directory for compiled source code.
         * @param targetDir Base directory for other generated artifacts.
         */
        public Input(
        		File sourceDir,
        		Origin sourceOrigin,
                File binDir,
                File targetDir) {
        	this.sourceDir = sourceDir;
        	this.sourceOrigin = sourceOrigin;
            this.binDir = binDir;
            this.targetDir = targetDir;
        }
    }
    
    public static BuilderFactory<Input, Out<List<File>>, CompileSourceCode> factory = BuilderFactoryFactory.of(CompileSourceCode.class, Input.class);

    public CompileSourceCode(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.binDir, "pluto.compile.dep");
    }

    @Override
    protected String description(Input input) {
        return "Build pluto";
    }

    @Override
    protected Out<List<File>> build(Input input) throws Throwable {

    	Origin.Builder compilerOrigin = Origin.Builder().add(input.sourceOrigin);
    	
    	// 2.a) resolve maven dependencies
    	MavenInput mavenInput = new MavenInput
    			.Builder()
    			.addDependency(ExternalDependencies.COMMONS_CLI)
    			.build();
    	List<File> mavenJars = requireBuild(MavenDependencyResolver.factory, mavenInput).val();
    	compilerOrigin.add(lastBuildReq());
    	
    	// 2.b) resolve and build git dependencies
    	File sugarjCommonJar = buildGitMaven(
    			new File(input.targetDir, "sugarj-common"),
    			ExternalDependencies.SUGARJ_COMMON_GIT_REPO,
    			"java7");
			compilerOrigin.add(lastBuildReq());

    	File javaUtilJar = buildGitMaven(
    			new File(input.targetDir, "java-util"),
    			ExternalDependencies.JAVA_UTIL_PLUTO_GIT_REPO,
    			"master");
			compilerOrigin.add(lastBuildReq());

    	// 2.c) compile pluto source code
    	requireBuild(input.sourceOrigin);
    	List<File> sourceFiles = FileCommands.listFilesRecursive(input.sourceDir, new FileExtensionFilter("java"));
    	JavaCompilerInput javaInput = new JavaCompilerInput
				.Builder()
				.addInputFiles(sourceFiles)
				.setSourceOrigin(compilerOrigin.get())
				.setTargetDir(input.binDir)
				.addSourcePaths(input.sourceDir)
				.addClassPaths(mavenJars)
				.addClassPaths(sugarjCommonJar, javaUtilJar)
				.setCompiler(JavacCompiler.instance)
				.get();
    	requireBuild(JavaBulkCompiler.factory, javaInput);
    	
    	// 2.d) return classpath for compiled code
    	List<File> classpath = new ArrayList<>(mavenJars);
    	classpath.add(sugarjCommonJar);
    	classpath.add(javaUtilJar);
    	classpath.add(input.binDir);
    	
    	return OutputPersisted.of(Collections.unmodifiableList(classpath));
    }

	private File buildGitMaven(File rootDir, String gitRepository, String branch) throws IOException {
    	GitInput gitInput = new GitInput
    			.Builder(rootDir, gitRepository)
    			.setBranch(branch)
    			.setConsistencyCheckInterval(TimeUnit.MINUTES.toMillis(15))
    			.build();
    	requireBuild(GitRemoteSynchronizer.factory, gitInput);

      Origin gitOrigin = Origin.from(lastBuildReq());
    	String jarName = FileCommands.fileName(rootDir);
    	report("Maven build " + jarName);
      MavenPackagerInput packageInput = new MavenPackagerInput
					.Builder()
					.setWorkingDir(rootDir)
		      .setJarName(jarName)
					.setClassOrigin(gitOrigin)
					.get();
      ExecutionResult result = requireBuild(MavenPackager.factory, packageInput).val();

    	File jar = new File(rootDir, "target/" + jarName + ".jar").getAbsoluteFile();
      require(jar);
    	if (!jar.exists())
    		throw new IllegalStateException("File " + jar + " does not exist.");
    	return jar;
	}
}
