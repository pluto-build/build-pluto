package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildmaven.MavenDependencyResolver;
import build.pluto.buildmaven.input.MavenInput;
import build.pluto.dependency.Origin;
import build.pluto.output.None;

public class CompileSourceCode extends Builder<CompileSourceCode.Input, None> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File sourceDir;
        public final Origin sourceOrigin;
        public final File target;
        public List<? extends BuildRequest<?, ?, ?, ?>> requiredUnits;

        public Input(
        		File sourceDir,
        		Origin sourceOrigin,
                File target) {
        	this.sourceDir = sourceDir;
        	this.sourceOrigin = sourceOrigin;
            this.target = target;
        }
    }
    
    public static BuilderFactory<Input, None, CompileSourceCode> factory = BuilderFactoryFactory.of(CompileSourceCode.class, Input.class);

    public CompileSourceCode(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.target, "pluto.compile.dep");
    }

    @Override
    protected String description(Input input) {
        return "Build pluto";
    }

    @Override
    protected None build(Input input) throws Throwable {

    	Origin.Builder compilerOrigin = Origin.Builder();
    	
    	// 2) build pluto source code
    	// 2.a) resolve maven dependencies
    	MavenInput mavenInput = new MavenInput
    			.Builder()
    			.addRepository(ExternalDependencies.PLUTO_MAVEN_REPO)
    			.addDependency(ExternalDependencies.COMMONS_CLI)
    			.build();
    	requireBuild(MavenDependencyResolver.factory, mavenInput);
    	compilerOrigin.add(lastBuildReq());
    	
    	// 2.b) resolve and build git dependencies
    	
    	
    	// 2.c) compile pluto source code
    	requireBuild(input.sourceOrigin);

    	return null;
    }
}
