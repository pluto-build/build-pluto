package build.pluto.buildpluto;

import build.pluto.buildmaven.input.ArtifactConstraint;
import build.pluto.buildmaven.input.Dependency;
import build.pluto.buildmaven.input.Repository;
import build.pluto.dependency.RemoteRequirement;

public class ExternalDependencies {
	public static final String PLUTO_GIT_REPO = "https://github.com/pluto-build/pluto";
	
	public static final String SUGARJ_COMMON_GIT_REPO = "https://github.com/sugar-lang/common";
	
	public static final String JAVA_UTIL_PLUTO_GIT_REPO = "https://github.com/pluto-build/java-util";

//	public static final Repository PLUTO_MAVEN_REPO = new Repository(
//			"pluto-build", 
//			"https://raw.githubusercontent.com/pluto-build/pluto-build.github.io/master/mvnrepository/",
//			"default",
//			Repository.ENABLED_DEFAULT_POLICY,
//			Repository.DISABLED_DEFAULT_POLICY);
	public static final Repository PLUTO_MAVEN_REPO = new Repository(
			"pluto-build", 
			"file:///Users/seba/projects/build/maven-repository/",
			"default",
			Repository.ENABLED_DEFAULT_POLICY,
			Repository.DISABLED_DEFAULT_POLICY);
	
    public static final Dependency JUNIT = 
        new Dependency(
                new ArtifactConstraint(
                    "junit",
                    "junit",
                    "4.12",
                    null,
                    null),
        		RemoteRequirement.CHECK_NEVER);
    
    public static final Dependency COMMONS_CLI =
        new Dependency(
            new ArtifactConstraint(
                    "commons-cli",
                    "commons-cli",
                    "1.3.1",
                    null,
                    null),
    		RemoteRequirement.CHECK_NEVER);
}
