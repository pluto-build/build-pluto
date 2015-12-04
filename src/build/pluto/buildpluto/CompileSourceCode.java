package build.pluto.buildpluto;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
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

public class CompileSourceCode extends Builder<CompileSourceCode.Input, None> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File sourceDir;
        public final Origin sourceOrigin;
        public final File binDir;
        public final File targetDir;

        /**
         * @param sourceDir Base directory for source code (not test code).
         * @param binDir Base directory for generated binaries.
         * @param targetDir Base directory for git clones
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
    
    public static BuilderFactory<Input, None, CompileSourceCode> factory = BuilderFactoryFactory.of(CompileSourceCode.class, Input.class);

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
    protected None build(Input input) throws Throwable {

    	Origin.Builder compilerOrigin = Origin.Builder().add(input.sourceOrigin);
    	
    	// 2.a) resolve maven dependencies
    	MavenInput mavenInput = new MavenInput
    			.Builder()
    			.addDependency(ExternalDependencies.COMMONS_CLI)
    			.build();
    	ArrayList<File> mavenJars = requireBuild(MavenDependencyResolver.factory, mavenInput).val();
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
