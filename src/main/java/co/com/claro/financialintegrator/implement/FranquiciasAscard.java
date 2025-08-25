package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

/**
 * IF55: Recibe archivo de Franquicias encriptado por ascard y lo pone en ruta
 * dl FTP BSCS desencriptado y renombrado
 * 
 * @author Carlos Guzman
 *
 */
public class FranquiciasAscard extends GenericProccess {
	private Logger logger = Logger.getLogger(FranquiciasAscard.class);

	/**
	 * Renombramiento de archivos
	 * 
	 * @param fileName
	 * @return
	 */
	public String renameFile(String fileName) {
		fileName = fileName.replace(".PGP", "");
		fileName = fileName.replace(".pgp", "");
		// fileName = fileName.replace("_Ascard", "");
		return fileName;
	}

	/**
	 * Proceso de archvio de franquicias
	 */
	@Override
	public void process() {
		logger.info("PROCESANDO BATCH DE FRANQUICIAS");
		// TODO Auto-generated method stub
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		processFranquicias(this.getPros().getProperty("ExtfileTCVISA"),uid);
		processFranquicias(this.getPros().getProperty("ExtfileTCCREDEN"),uid);
		processFranquicias(this.getPros().getProperty("ExtfileTCDINERS"),uid);
		processFranquicias(this.getPros().getProperty("ExtfileConvenio"),uid);
		processFranquicias(this.getPros().getProperty("ExtfileFragata"),uid);

	}

	/**
	 * Se procesa archivo de Franquicias, por tipo de franquicias
	 * 
	 * @param existFile
	 *            nombre archivo de franquicias
	 */
	private void processFranquicias(String existFile,String uid) {
		String path_process = this.getPros().getProperty("fileProccess");
		String path_processBSC = this.getPros().getProperty("fileProccessBSCS");
		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_processBSC);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de BSCS "
					+ e.getMessage());
		}
		List<File> fileProcessList = null;
		String fileName = "";
		try {

			// Se busca archivo TCVISA
			fileProcessList = FileUtil.findFileNameFormPattern(this.getPros()
					.getProperty("path"), existFile);
			// Se verifica que exista un archivo en la ruta y con las
			// carateristicas
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File file : fileProcessList) {
					if (file != null) {
						// Se arma ruta para copiar archivo de Datos Min
						fileName = file.getName();
						String fileNamePath = this.getPros()
								.getProperty("path").trim()
								+ fileName;
						String fileNameCopy = this.getPros()
								.getProperty("path").trim()
								+ path_process + "processes_" + fileName;
						if (!FileUtil.fileExist(fileNameCopy)) {
							if (FileUtil.copy(fileNamePath, fileNameCopy)) {
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								String fileOuput = this.getPros()
										.getProperty("path").trim()
										+ path_processBSC
										+ renameFile(fileName);
								this.getPgpUtil().setPathOutputfile(fileOuput);
								// Se verifica si se desencripta archivo
								try {
									this.getPgpUtil().decript();
									try{
										Integer linesFiles =  FileUtil.countLinesNew(fileOuput) ;
										//Se registra control archivo
										this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, file.getName(), linesFiles.toString() ,null,uid);
									}catch(Exception ex){
										logger.error("error contando lineas "+ex.getMessage(),ex);
									}
									String obervacion = "Archivo Procesado Exitosamente";
									registrar_auditoria(fileName, obervacion,uid);
								} catch (PGPException ex) {
									logger.error(
											"Error desencriptando archivo: ",
											ex);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = ex.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
								} catch (Exception e) {
									logger.error(
											"Error desencriptando archivo: ", e);
									// Se genera error con archivo se guarda en
									// la
									// auditoria
									String obervacion = e.getMessage();
									registrar_auditoria(fileName, obervacion,uid);
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNamePath);
							}
						} else {
							logger.info(" ELIMINADO ARCHIVO " + existFile
									+ " EXISTE");
							FileUtil.delete(fileNamePath);
							// String context =
							// "/spring/bgh/ftpFranquicias-config.xml";
							// deleteFileFTP(fileName, context);
						}
					}
				}
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos Franquicias : " + existFile
					+ e.getMessage());
			String obervacion = "Error leyendos Archivos  Franquicias: "
					+ existFile + e.getMessage();
			registrar_auditoria(fileName, obervacion,uid);
		} catch (Exception e) {
			String obervacion = "Error leyendos Archivos Franquicias: "
					+ existFile + e.getMessage();
			registrar_auditoria(fileName, obervacion,uid);
		}
	}

}
