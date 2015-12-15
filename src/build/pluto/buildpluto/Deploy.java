package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sugarj.common.FileCommands;

import com.github.maven.plugins.site.SiteMojo;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildjava.JavaJar;
import build.pluto.buildjava.util.FileExtensionFilter;
import build.pluto.buildmaven.MavenDeployer;
import build.pluto.buildmaven.MavenHandler;
import build.pluto.buildmaven.PomGenerator;
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
    	File artifactJar = new File(input.targetDir, "pluto.jar");
    	
    	requireBuild(input.classesOrigin);
    	List<File> classfilePaths = FileCommands.listFilesRecursive(input.classesDir, new FileExtensionFilter("class"));
        if (classfilePaths.isEmpty())
        	throw new IllegalStateException("No class files found for packaging");
        Set<File> classefiles = new HashSet<>(classfilePaths);
        
        JavaJar.Input jarInput = new JavaJar.Input(
                JavaJar.Mode.CreateOrUpdate,
                artifactJar,
                null,
                Collections.singletonMap(input.classesDir, classefiles),
                input.classesOrigin);
        requireBuild(JavaJar.factory, jarInput);
    	Origin artifactOrigin = Origin.from(lastBuildReq());
    	
    	// 4.b) generate pom file
    	Artifact artifact = new Artifact("build.pluto", "pluto", input.version, null, null);
    	File pomFile = new File(input.targetDir, "pom.xml");
    	// TODO install dependencies into pom
    	requireBuild(PomGenerator.factory, new PomGenerator.Input(artifact, pomFile));
    	Origin pomOrigin = Origin.from(lastBuildReq());
    	
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
