package co.com.claro.financialintegrator.implement;

import java.io.File;

import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.interfaces.GenericProccess;

public class App extends GenericProccess{
	private Logger logger = Logger.getLogger(SaldosaFavorClaro.class);
	@Override
	public void process() {
		String properties =System.getenv().get("PATH_PROPERTIES_INTEGRATOR") +"Activaciones/Activaciones.properties";
		logger.info("File Properties "+properties);
		File f = new File(properties);
		logger.info("File Exist "+f.exists());
	}

}
