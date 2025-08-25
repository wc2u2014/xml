package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class ConsultaUsr extends GenericProccess {

	private Logger logger = Logger.getLogger(ConsultaUsr.class);

	@Override
	public void process() {
		logger.info(".............. Iniciando proceso Consulta Usr .................. ");
	   UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		try {
			// carpeta de proceso
			String path_process = this.getPros().getProperty("fileProccess");
			// carpeta donde_se_guardan_archivos proceso de ascard
			String history = this.getPros().getProperty("pathHistory");

			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim());
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + history);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}
			logger.info("................ Buscando Archivos de Consulta Usr .................. ");
			// Se buscan Archivos de activaciones y de control
			List<File> fileProcessList = null;
			// Se busca archivo de Activacion
			try {
				fileProcessList = FileUtil.findFileNameFormPattern(this.getPros().getProperty("path"),
						this.getPros().getProperty("ExtfileConsulta"));
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendos Archivos de Activacion del directorio " + e.getMessage());
			}
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileConsultaUsuarios : fileProcessList) {
					// Se verifica si la pareja de archivos existen
					if (fileConsultaUsuarios != null) {
						logger.info("............Procesando consuta usuarios.........");
						this.copyHistory(fileConsultaUsuarios.getName());
						// this.copyBscs(fileConsultaUsuarios.getName());
						String fileNameConsulta = fileConsultaUsuarios.getName();
						String fileNameConsultaFullPath = this.getPros().getProperty("path").trim() + fileNameConsulta;
						// Se mueve los archivos carpeta de procesos para empezar el flujo
						String fileNameConsultaUsuariosProcess = this.getPros().getProperty("path").trim()
								+ path_process + fileNameConsulta;
						try {
							if (!FileUtil.fileExist(fileNameConsultaUsuariosProcess)) {
								if ((FileUtil.copy(fileNameConsultaFullPath, fileNameConsultaUsuariosProcess))) {
									this.getPgpUtil().setPathInputfile(fileNameConsultaUsuariosProcess);
									// Toca formatear nombre para quitar prefijo BGH Y PREFIJO TXT Y PGP
									String fileOuput = this.getPros().getProperty("path").trim() + path_process
											+ renameFile(fileNameConsulta);
									this.getPgpUtil().setPathOutputfile(fileOuput);
									// Se verifica si se desencripta archivo
									this.getPgpUtil().decript();

									this.copyBscs(renameFile(fileNameConsulta));

									if (!FileUtil.fileExist(fileOuput)) {
										FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
									}

									logger.info(" EL ARCHIVO SE HA PROCESADO EL ARCHIVO");
									String observacion = "Se ha procesado el archivo correctamente";
									registrar_auditoriaV2(fileNameConsulta, observacion,uid);

								} else {
									logger.error("ARCHIVO DE CONTROL ESTA VACIO..");
								}

							} else {
								logger.info(" ARCHIVOS DE ACTIVACIONES EXISTE NO SE PROCESA");
							}

						} catch (FinancialIntegratorException ex) {
							logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : " + ex.getMessage());
							String observacion = "Error copiando archivos  para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameConsulta, observacion,uid);
						} catch (Exception ex) {
							logger.error(" ERRROR GENERAL EN PROCESO.. : " + ex.getMessage());
							String observacion = "Error copiando archivos de para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameConsulta, observacion,uid);
						}
						logger.info(" ELIMINADO ARCHIVO ");
						FileUtil.delete(fileNameConsultaFullPath);
						FileUtil.delete(fileNameConsultaUsuariosProcess);
					}
				}
			} else {
				logger.error("NO SE ENCONTRARON LOS ARCHIVOS DE CONTROL O ACTIVACIONES");
			}
		} catch (Exception e) {
			logger.error("Excepcion no Controlada  en proceso de activaciones " + e.getMessage(), e);
		}

	}

	private String copyHistory(String fileName) {
		String pathHistory = this.getPros().getProperty("path") + this.getPros().getProperty("pathHistory");
		String pathFileOriginal = this.getPros().getProperty("path") + fileName;
		String copyFileName = pathHistory + fileName + "_process";
		logger.info("pathFileOriginal " + pathFileOriginal + " copyFileName " + copyFileName);
		try {
			if (!FileUtil.fileExist(copyFileName)) {
				FileUtil.copy(pathFileOriginal, copyFileName);
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error copiano archivos " + e.getMessage(), e);
		}
		return copyFileName;
	}

	private String copyBscs(String fileName) {
		String path_processBSC = this.getPros().getProperty("path") + this.getPros().getProperty("fileProccessBSCS");
		String pathFileOriginal = this.getPros().getProperty("path") + this.getPros().getProperty("fileProccess")
				+ fileName;
		String copyFileName = path_processBSC + fileName;
		logger.info("pathFileOriginal " + pathFileOriginal + " copyFileName " + copyFileName);
		try {
			if (!FileUtil.fileExist(copyFileName)) {
				FileUtil.copy(pathFileOriginal, copyFileName);
			}
		} catch (FinancialIntegratorException e) {
			logger.error("Error copiando archivos " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error copiando archivos " + e.getMessage(), e);
		}
		return copyFileName;
	}

	public String renameFile(String fileName) throws FinancialIntegratorException {
		try {
			String extencion = this.getPros().getProperty("fileOutputExt");
			fileName = fileName.replace(".txt", "");
			fileName = fileName.replace(".TXT", "");
			fileName = fileName.replace(".pgp", "");
			fileName = fileName.replace(".PGP", "");
			fileName = fileName + extencion;
			logger.info(" FileName Output : " + fileName);
			return fileName;

		} catch (Exception e) {
			logger.error("Error creando nombre de archivo de salida " + e.getMessage(), e);
			throw new FinancialIntegratorException(e.getMessage());
		}
	}

	/**
	 * Se borra archivo en caso de error del proceso
	 */
	public void deleteEncriptFile() {
		try {
			File deleteActivaciones = FileUtil
					.findFile(this.getPros().getProperty("path") + this.getPros().getProperty("pathCopyFile"), ".PGP");

			if (deleteActivaciones != null) {
				logger.info("Borrando archivos: " + deleteActivaciones.getName());
				deleteActivaciones.setWritable(true);
				deleteActivaciones.delete();
			}
		} catch (FinancialIntegratorException e1) {
			logger.error("ERROR BORRANDO ARCHIVOS DE ASCARD " + e1.getMessage());
		}
	}

}
