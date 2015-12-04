package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildgit.GitInput;
import build.pluto.buildgit.GitRemoteSynchronizer;
import build.pluto.dependency.Origin;
import build.pluto.dependency.RemoteRequirement;
import build.pluto.output.None;

public class Pluto extends Builder<Pluto.Input, None> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File targetDir;
        public final File jarLocation;

        /**
         * @param targetDir Base directory for all generated files.
         * @param jarLocation 
         */
        public Input(
                File targetDir,
                File jarLocation) {
            this.targetDir = targetDir;
            this.jarLocation = jarLocation;
        }
    }
    
    public static BuilderFactory<Input, None, Pluto> factory = BuilderFactoryFactory.of(Pluto.class, Input.class);

    public Pluto(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.targetDir, "pluto.dep");
    }

    @Override
    protected String description(Input input) {
        return "Build pluto";
    }

    @Override
    protected None build(Input input) throws Throwable {
    	
    	// 1) download pluto source code
    	File gitDir = new File(input.targetDir, "src");
    	GitInput gitInput = new GitInput
			.Builder(gitDir, ExternalDependencies.PLUTO_GIT_REPO)
    		.setBranch("master")
    		.setConsistencyCheckInterval(RemoteRequirement.CHECK_ALWAYS)
    		.build();
    	requireBuild(GitRemoteSynchronizer.factory, gitInput);
    	Origin sourceOrigin = Origin.from(lastBuildReq());
    	
    	// 2) build pluto source code
    	File sourceDir = new File(gitDir, "src");
    	File binDir = new File(input.targetDir, "bin");
    	CompileSourceCode.Input compileInput = new CompileSourceCode.Input(
    			sourceDir,
    			sourceOrigin,
    			binDir,
    			input.targetDir);
    	List<File> sourceClassPath = requireBuild(CompileSourceCode.factory, compileInput).val();
    	
    	// 3) test pluto source code
    	File testDir = new File(gitDir, "test");
    	File testBinDir = new File(input.targetDir, "bin-test");
    	TestSourceCode.Input testInput = new TestSourceCode.Input(
    			testDir, 
    			sourceOrigin,
    			testBinDir,
    			sourceClassPath,
    			input.targetDir);
    	requireBuild(TestSourceCode.factory, testInput);
    	
    	// 4) deploy
    	
    	return null;
    }
}
