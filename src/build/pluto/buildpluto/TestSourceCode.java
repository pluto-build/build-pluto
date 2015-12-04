package build.pluto.buildpluto;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildgit.GitInput;
import build.pluto.buildgit.GitRemoteSynchronizer;
import build.pluto.buildjava.JavaBulkBuilder;
import build.pluto.buildjava.JavaInput;
import build.pluto.buildjava.compiler.JavacCompiler;
import build.pluto.buildjava.util.FileExtensionFilter;
import build.pluto.buildmaven.MavenDependencyResolver;
import build.pluto.buildmaven.input.MavenInput;
import build.pluto.dependency.Origin;
import build.pluto.output.None;

public class TestSourceCode extends Builder<TestSourceCode.Input, None> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File sourceDir;
        public final Origin sourceOrigin;
        public final File binDir;
        public final File targetDir;

        /**
         * @param testSourceDir Base directory for test code (not source code).
         * @param testBinDir Base directory for compiled test code.
         * @param targetDir Base directory for other artifacts.
         */
        public Input(
        		File testSourceDir,
        		Origin sourceOrigin,
                File testBinDir,
                File targetDir) {
        	this.sourceDir = testSourceDir;
        	this.sourceOrigin = sourceOrigin;
            this.binDir = testBinDir;
            this.targetDir = targetDir;
        }
    }
    
    public static BuilderFactory<Input, None, TestSourceCode> factory = BuilderFactoryFactory.of(TestSourceCode.class, Input.class);

    public TestSourceCode(Input input) {
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
    protected None build(Input input) throws Throwable {

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
    	
    	File javaUtilJar = buildGitMaven(
    			new File(input.targetDir, "java-util"),
    			ExternalDependencies.JAVA_UTIL_PLUTO_GIT_REPO,
    			"master");
    	
    	// 2.c) compile pluto source code
    	requireBuild(input.sourceOrigin);
    	List<File> sourceFiles = FileCommands.listFilesRecursive(input.sourceDir, new FileExtensionFilter("java"));
    	JavaInput javaInput = new JavaInput
				.Builder()
				.addInputFiles(sourceFiles)
				.setFilesOrigin(compilerOrigin.get())
				.setTargetDir(input.binDir)
				.addSourcePaths(input.sourceDir)
				.addClassPaths(mavenJars)
				.addClassPaths(sugarjCommonJar, javaUtilJar)
				.setCompiler(JavacCompiler.instance)
				.get();
    	requireBuild(JavaBulkBuilder.factory, javaInput);
    	
    	return null;
    }

	private File buildGitMaven(File rootDir, String gitRepository, String branch) throws IOException {
    	GitInput gitInput = new GitInput
    			.Builder(rootDir, gitRepository)
    			.setBranch(branch)
    			.build();
    	requireBuild(GitRemoteSynchronizer.factory, gitInput);
    	
    	String jarName = FileCommands.fileName(rootDir);
    	String command = String.format("mvn package --batch-mode -DskipTests -Djar.finalName=%s", jarName);
    	
    	report("Maven build " + jarName);
    	// TODO build-maven must provide a MavenBuilder that installs dependencies on required files
    	Exec.run(rootDir, command.split(" "));
    	
    	File jar = new File(rootDir, "target/" + jarName + ".jar");
    	if (!jar.exists())
    		throw new IllegalStateException("File " + jar + " does not exist.");
    	return jar;
	}
}
