package co.com.claro.financialintegrator.implement;

import co.com.claro.financialintegrator.domain.UidServiceResponse;
import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class SalidaAjustesFijaAscard extends GenericProccess{
	private Logger logger = Logger.getLogger(SalidaAjustesFijaAscard.class);
	@Override
	public void process() {
		logger.info("Salida de Ajustes Ascard FIJA ");
		// TODO Auto-generated method stub
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		SalidaAjustesAscard _salida = new SalidaAjustesAscard();
		_salida.setPros(this.getPros());
		_salida.setPgpUtil(this.getPgpUtil());
		_salida.processSalidaDeAjustes(uid);
		
	}

}
