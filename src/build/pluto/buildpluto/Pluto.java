package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;

import build.pluto.builder.BuildRequest;
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

        public final File target;
        public final File jarLocation;

        public Input(
                File target,
                File jarLocation) {
            this.target = target;
            this.jarLocation = jarLocation;
        }
    }
    
    public static BuilderFactory<Input, None, Pluto> factory = BuilderFactoryFactory.of(Pluto.class, Input.class);

    public Pluto(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.target, "pluto.dep");
    }

    @Override
    protected String description(Input input) {
        return "Build pluto";
    }

    @Override
    protected None build(Input input) throws Throwable {
    	
    	// 1) download pluto source code
    	File gitDir = new File(input.target, "src");
    	GitInput gitInput = new GitInput
			.Builder(gitDir, ExternalDependencies.PLUTO_GIT_REPO)
    		.setBranch("master")
    		.setConsistencyCheckInterval(RemoteRequirement.CHECK_ALWAYS)
    		.build();
    	requireBuild(GitRemoteSynchronizer.factory, gitInput);
    	Origin sourceOrigin = Origin.from(lastBuildReq());
    	
    	// 2) build pluto source code
    	File sourceDir = new File(gitDir, "src");
    	File binDir = new File(input.target, "bin");
    	CompileSourceCode.Input compileInput = new CompileSourceCode.Input(
    			sourceDir,
    			sourceOrigin,
    			binDir);
    	requireBuild(CompileSourceCode.factory, compileInput);
    	BuildRequest<?, ?, ?, ?> compileReq = lastBuildReq();
    	
    	// 3) test pluto source code
    	// 3.a) resolve maven test dependencies
    	// 3.b) resolve and build git test dependencies
    	// 3.c) compile pluto test code
    	// 3.d) run tests
    	// 4) deploy
    	
    	return null;
    }
}
