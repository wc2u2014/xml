package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class RecaudoBancosMoveFilesMultiplesProcess extends GenericProccess {
	private Logger logger = Logger.getLogger(RecaudoBancosMoveFilesMultiplesProcess.class);
	
	/**
	 * se envia mails de archivos
	 */
	public void sendMail(String path,String uid){
		try {
			this.initPropertiesMails(uid);
			this.getMail().sendMail(path);
		} catch (FinancialIntegratorException e) {
			logger.error("error enviando archivo de recaudos bancos "+e.getMessage(),e );
		}catch (Exception e) {
			logger.error("error enviando archivo de recaudos bancos "+e.getMessage(),e );
		}
	}
	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" ** Recaudos Bancos Moves Files Multiples Process V 1.0 ** ");
		//initialize Properties
		HashMap<String, String> prop = consultarPropiedadesBatch(uid);
		setPros(prop);
		//Path process Recaudos Bancos
		String path = this.getPros()
				.getProperty("path");
		String fileProccess = this.getPros()
				.getProperty("fileProccess");
		String filenameregex = this.getPros().getProperty(
				"file.filenameregex");
		String path_process_history = this.getPros().getProperty("fileHistory");
		//Patch copy files to Bloqueo Terminal
		String pathCopyFileBloqueoTerminal = this.getPros()
				.getProperty("pathCopyFileBloqueoTerminal");
		//Patch copy files Avisos Pago
		String pathCopyFileAvisosPago = this.getPros()
						.getProperty("pathCopyFileAvisosPago");
		//Patch copy files pathCopyFileControlRecaudos
		String pathCopyFileControlRecaudos = this.getPros()
						.getProperty("pathCopyFileControlRecaudos");		
		//Building path of file exist into Batch Recaudos Bancos
		String path_file_history = path + File.separator
				+ path_process_history;
		//
		List<File> fileProcessList = null;
		
		// Se buscan archivos y mueven los archivos para ser procesados
		try {
			List<File> listFile = FileUtil.findListFileName(path,
					filenameregex);
			//Se copian los archivos
			for (File file : listFile) {
				String fileNameCopyBloqueo=pathCopyFileBloqueoTerminal+File.separator+file.getName();
				String fileNameCopyAvisosPago=pathCopyFileAvisosPago+File.separator+file.getName();
				String fileNameCopyControlRecaudos=pathCopyFileControlRecaudos+File.separator+file.getName();
				String fileNameCopyProces=path+File.separator+ fileProccess+File.separator+file.getName();
				String fileNameFullPath=path+File.separator+file.getName();	
				String path_history = path_file_history + File.separator
				+ file.getName();
				if (!FileUtil.fileExist(path_history)) {
					logger.info("Copy File "+file.getName());
					if (!FileUtil.fileExist(fileNameCopyBloqueo) && !FileUtil.fileExist(fileNameCopyProces)) {
						logger.info("Copy File Bloqueo Terminal :  "+fileNameCopyBloqueo);
						FileUtil.copy(fileNameFullPath, fileNameCopyBloqueo);
						this.sendMail(fileNameFullPath,uid);										
					}
					if (!FileUtil.fileExist(fileNameCopyAvisosPago) && !FileUtil.fileExist(fileNameCopyProces)) {
						logger.info("Copy File Avisos Pago : "+fileNameCopyAvisosPago);
						FileUtil.copy(fileNameFullPath, fileNameCopyAvisosPago);																
					}
					if (!FileUtil.fileExist(fileNameCopyControlRecaudos) && !FileUtil.fileExist(fileNameCopyProces)) {
						logger.info("Copy File Control Recaudos : "+fileNameCopyControlRecaudos);
						FileUtil.copy(fileNameFullPath, fileNameCopyControlRecaudos);																
					}					
					//Inicializar propiedades
					//Compilar batch de recaudo
					FileUtil.move(fileNameFullPath, fileNameCopyProces);	
				}else {
					logger.info("Delete File : " + path + File.separator
							+ file.getName());
					FileUtil.delete(path + File.separator + file.getName());
				}
				
			}
		} catch (FinancialIntegratorException e) {
			logger.error(" error procesando archivos de bancos para notificar bloqueo de terminal  "+e.getMessage(),e);
		} catch (Exception e) {
			logger.error(" error procesando archivos de bancos para notificar bloqueo de terminal  "+e.getMessage(),e);
		}
	}

}
