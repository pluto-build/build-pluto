package build.pluto.buildpluto;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.output.None;

public class Pluto extends Builder<Pluto.Input, None> {
	
    public static class Input implements Serializable {
        private static final long serialVersionUID = -8432928706675953694L;

        public final File target;
        public final File jarLocation;
        public List<BuildRequest<?, ?, ?, ?>> requiredUnits;

        public Input(
                File target,
                File jarLocation,
                List<BuildRequest<?, ?, ?, ?>> requiredUnits) {
            this.target = target;
            this.jarLocation = jarLocation;
            this.requiredUnits = requiredUnits;
        }
    }
    
    public static BuilderFactory<Input, None, Pluto> factory = BuilderFactoryFactory.of(Pluto.class, Input.class);

    public Pluto(Input input) {
        super(input);
    }

    @Override
    public File persistentPath(Input input) {
        return new File(input.target, "services-base-java.dep");
    }

    @Override
    protected String description(Input input) {
        return "Build pluto";
    }

    @Override
    protected None build(Input input) throws Throwable {
    	
    	// 1) download pluto source code
    	// 2) build pluto source code
    	// 2.a) resolve maven dependencies
    	// 2.b) resolve and build git dependencies
    	// 2.c) compile pluto source code
    	// 3) test pluto source code
    	// 3.a) resolve maven test dependencies
    	// 3.b) resolve and build git test dependencies
    	// 3.c) compile pluto test code
    	// 3.d) run tests
    	// 4) deploy
    	
    	return null;
    }
}
