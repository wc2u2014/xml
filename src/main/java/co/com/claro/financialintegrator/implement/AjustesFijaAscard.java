package co.com.claro.financialintegrator.implement;

import co.com.claro.financialintegrator.domain.UidServiceResponse;
import org.apache.log4j.Logger;

import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
/**
 * Clase que procesa los archivos de Ajustes Claro Para el proceso de FIFA
 * @author Carlos Guzman
 *
 */
public class AjustesFijaAscard extends GenericProccess{
	private Logger logger = Logger.getLogger(AjustesFijaAscard.class);
	@Override
	public void process() {
              UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info("Ajustes Ascard FIJA V 1.O");
		AjustesAscard ajustes = new AjustesAscard();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		logger.info("ExtfileProcessAJUF "+this.getPros().getProperty("ExtfileProcessAJUF"));
		logger.info("fileOutputPrefixAJUF "+this.getPros().getProperty("fileOutputPrefixAJUF"));
		ajustes.setPros(this.getPros());
		ajustes.setPgpUtil(this.getPgpUtil());	
		/**
		 * Se ejecuta el proceso de generacion de archivo de ajustes
		 */
		ajustes.processFileAjustes(this.getPros().getProperty("ExtfileProcessAJUF"),this.getPros().getProperty("fileOutputPrefixAJUF").trim(),this.getPros().getProperty("ExtfileProcessRecursive").trim(),uid);
	}

}
