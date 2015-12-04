package build.pluto.buildpluto;

import java.io.File;

import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;

public class Main {

	public static void main(String[] args) throws Throwable {
//		Log.log.setLoggingLevel(Log.DETAIL);
		
		Pluto.Input input = new Pluto.Input(
      		  new File("target/pluto"),
      		  new File("target/pluto.jar"));
		
		BuildManagers.build(new BuildRequest<>(Pluto.factory, input));
	}

}
