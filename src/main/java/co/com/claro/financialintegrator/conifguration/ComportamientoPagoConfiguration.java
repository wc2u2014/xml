package co.com.claro.financialintegrator.conifguration;

import co.com.claro.financialintegrator.interfaces.GenericConfiguration;

public class ComportamientoPagoConfiguration extends GenericConfiguration{

	@Override
	public void loadConfiguration() {
		FileProccessConfiguration fileProcess = new FileProccessConfiguration();
		fileProcess.setSecretKeyFile("E:\\PGPProject\\PGPProject\\Secretkey\\0xF9B70DCC-sec.asc");
		fileProcess.setPassphrase("12345678890312");
		fileProcess.setLocalDirectory("E:\\local_ftp_comportamiento_pago\\");
		
	}

}
