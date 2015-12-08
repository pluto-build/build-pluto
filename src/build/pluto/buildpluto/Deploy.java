package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildmaven.MavenDeployer;
import build.pluto.buildmaven.MavenHandler;
import build.pluto.buildmaven.input.Artifact;
import build.pluto.dependency.Origin;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;

public class Deploy extends Builder<Deploy.Input, Out<File>> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File classesDir;
        public final Origin classesOrigin;
        public final String version;
        public final File targetDir;

        /**
         * @param classesDir Base directory for source code (not test code).
         * @param version Base directory for compiled source code.
         * @param targetDir Base directory for other generated artifacts.
         */
        public Input(
        		File classesDir,
        		Origin classesOrigin,
        		String version,
                File targetDir) {
        	this.classesDir = classesDir;
        	this.classesOrigin = classesOrigin;
            this.version = version;
            this.targetDir = targetDir;
        }
    }
    
    public static BuilderFactory<Input, Out<File>, Deploy> factory = BuilderFactoryFactory.of(Deploy.class, Input.class);

    public Deploy(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.targetDir, "pluto.deploy.dep");
    }

    @Override
    protected String description(Input input) {
        return "Deploy pluto";
    }

    @Override
    protected Out<File> build(Input input) throws Throwable {

    	// 4.a) package jar file
    	requireBuild(input.classesOrigin);
    	File artifactJar = null;
    	Origin artifactOrigin = null;
    	
    	// 4.b) generate pom file
    	File pomFile = null;
    	Origin pomOrigin = null;
    	Artifact artifact = new Artifact("build.pluto", "pluto", input.version, null, null);
    	
    	// 4.c) deploy to maven
    	MavenDeployer.Input deployInput = new MavenDeployer.Input(
			artifact, 
			artifactJar, 
			artifactOrigin, 
			pomFile, 
			pomOrigin,
			MavenHandler.DEFAULT_LOCAL,
			ExternalDependencies.PLUTO_MAVEN_REPO);
    	requireBuild(MavenDeployer.factory, deployInput);

    	return OutputPersisted.of(artifactJar);
    }
}
