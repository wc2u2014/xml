package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.AbstractDocument.LeafElement;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

/***
 * IF38: Reciba archidos de proceso y de control de campañas de facturación y
 * los unifica y encriprta y los deja en ASCARD
 * 
 * @author Oracle
 *
 */
public class CampanaFacturacion extends GenericProccess {
	private Logger logger = Logger.getLogger(CampanaFacturacion.class);

	/**
	 * Valida que el archivo de control corresponda al archivo de proceso
	 * 
	 * @param lineControl
	 * @param lineProceso
	 * @return
	 */
	public Boolean _validadControl(FileOuput lineControl,
			List<FileOuput> lineProceso) {
		try {
			Integer lineasSaldoControl = (Integer) lineControl.getType(
					"CantidadRegistro").getValue();
			logger.info("Lineas de Control: " + lineasSaldoControl + " - "
					+ lineProceso.size());
			if (lineasSaldoControl > 0 && lineasSaldoControl == lineProceso.size() ) {

				Double totalSaldofinanciar = 0d;

				for (FileOuput _line : lineProceso) {
					try {
						totalSaldofinanciar = totalSaldofinanciar
								+ ((Double) _line.getType("Codigo").getValue());
					} catch (FinancialIntegratorException ex) {
						logger.error("Error Validando Linea Activacion::..."
								+ ex.getMessage());
						return false;
					}
				}
				Double totalSaldoControl = (Double) lineControl.getType(
						"SumatoriaCampoAFinanciar").getValue();
				logger.info("Compare Saldos (totalSaldoControl=totalSaldofinanciar ): "
						+ String.format("%.2f", totalSaldoControl)
						+ " : "
						+ String.format("%.2f", totalSaldofinanciar));
				return totalSaldofinanciar.equals(totalSaldoControl);
			} else {
				logger.error("Las lineas del archivo no coinciden..");
				return false;
			}
		} catch (FinancialIntegratorException ex) {
			logger.error("Error Validando Archivo..." + ex.getMessage());
			return false;
		}

	}

	/**
	 * Envia un mail , si no se encuentra archivo.
	 */
	private void sendMail(String file) {
		logger.info("Enviando mail");
		String toAddress[] = this.getPros().getProperty("toAddress").trim()
				.split(";");
		logger.info("toAddress: " + toAddress[0]);
		logger.info("toAddress: " + Arrays.toString(toAddress));
		String fromAddress = this.getPros().getProperty("fromAddress").trim();
		logger.info("fromAddress: " + fromAddress);
		String subject = this.getPros().getProperty("subject").trim();
		logger.info("subject: " + subject);
		String msgBody = this.getPros().getProperty("msgBody").trim() + " "
				+ file;
		logger.info("msgBody: " + msgBody);
		try {
			this.getMail().sendMail(toAddress, fromAddress, subject, msgBody);

		} catch (FinancialIntegratorException e) {
			logger.error("Error enviando mail: " + e.getMessage());

		} catch (Exception e) {
			logger.error("Error enviando mail: " + e.getMessage());
		}
	}
	/**
	 * Renombra archivo de campaña de facturacion
	 * @param file
	 * @return
	 */
	/**
	 * Renombra archivo de campaña de facturacion
	 * @param file
	 * @return
	 */
	public String rename(String file){
		String key="PROM_EQP_ASCARD_";
		file = file.replace(".txt", "");
		file = file.replace(".TXT", "");
		file = file.replace(".Txt", "");
		file = file.replace(key, "");
		String cc=file.substring(file.length()-2, file.length());
		System.out.println(cc);
		String hh  = file.substring(0,14);
		return key+cc+"_"+hh+".Txt.PGP";
		
	}
			

	/**
	 * Procesa los archivos de campaña de facturación
	 */
	@Override
	public void process() {
		logger.info(" -- CAMPAÑA DE FACTURACION  --");
	  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();		
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// carpeta de proceso
		String path_process = this.getPros().getProperty("fileProccess");
		// carpeta donde_se_guardan_archivos proceso de ascard
		String path_ascard_process = this.getPros().getProperty("pathCopyFile");

		try {
			FileUtil.createDirectory(this.getPros().getProperty("path").trim()
					+ path_ascard_process);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio para processar archivo de ASCARD "
					+ e.getMessage());
		}
		// Se buscan Archivos de activaciones y de control
		List<File> fileProcessList = null;
		File fileControl = null;
		// Se busca archivo de Activacion
		try {

			fileProcessList = FileUtil
					.findFileNameFormEndPattern(
							this.getPros().getProperty("path"), this.getPros()
									.getProperty("Extfile"));
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos de proceso del directorio "
					+ e.getMessage());
		}		
		// Se verifica si la pareja de archivos existen
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProceso : fileProcessList) {
				// Se busca archivo de Control
				try {
					if (fileProceso != null) {
						logger.info("ARCHIVO DE PROCESO: "
								+ fileProceso.getName() + " ExistFileControl: "
								+ this.getPros().getProperty("ExtfileControl"));
						String fileControlName = fileProceso.getName()
								+ this.getPros().getProperty("ExtfileControl");
						logger.info("FIND ARCHIVO DE CONTROL: "
								+ fileControlName);
						fileControl = FileUtil.findFileName(this.getPros()
								.getProperty("path"), fileControlName);
						logger.info("FIND ARCHIVO DE CONTROL: " + fileControl);
					}

				} catch (FinancialIntegratorException e) {
					logger.error("Error leyendos Archivos de Activacion del directorio "
							+ e.getMessage());
				}
				if (fileProceso != null && fileControl != null) {
					logger.info("............Procesando activaciones.........");
					// Archivo de Control
					String fileNameControl = fileControl.getName();
					String fileNameControlFullPath = this.getPros()
							.getProperty("path").trim()
							+ fileNameControl;
					String fileNameControlCopy = this.getPros()
							.getProperty("path").trim()
							+ path_process + "processes_" + fileNameControl;
					// Archivo de Proceso
					String fileNameProceso = fileProceso.getName();
					String fileNameProcesoFullPath = this.getPros()
							.getProperty("path").trim()
							+ fileNameProceso;
					// Se mueve los archivos carpeta de procesos para empezar el
					// flujo

					String fileNameProcesoCopy = this.getPros()
							.getProperty("path").trim()
							+ path_process + "processes_" + fileNameProceso;
					String fileNameProcesoOutputCopy = this.getPros()
							.getProperty("path").trim()
							+ path_process
							+ "processes_output_"
							+ fileNameProceso;
					try {
						if (!FileUtil.fileExist(fileNameProcesoCopy)) {
							if (FileUtil.copy(fileNameControlFullPath,
									fileNameControlCopy)
									&& (FileUtil.copy(fileNameProcesoFullPath,
											fileNameProcesoCopy))) {
								List<FileOuput> lineControl = FileUtil
										.readFile(this
												.configurationFileControl(fileNameControlFullPath));
								List<FileOuput> lineProceso = FileUtil
										.readFile(this
												.configurationFileProceso(fileNameProcesoCopy));
								if (!lineControl.isEmpty()) {
									if (this._validadControl(
											lineControl.get(0), lineProceso) ) {
										// Se crea HEADER DE ARCHIVO DE
										// ACTIVACIONES CON
										// ARCHIVO DE CONTROL
										FileOuput header = new FileOuput();
										String headerString = lineControl
												.get(0)
												.getType("CantidadRegistro")
												.getValueString()
												+ lineControl
														.get(0)
														.getType(
																"CantidadRegistro")
														.getSeparator()
												+ lineControl
														.get(0)
														.getType(
																"SumatoriaCampoAFinanciar")
														.getValueString();
										try{
										//Se registra el control del archivo recibido										
											this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), null, fileProceso.getName(), lineControl.get(0).getType("CantidadRegistro").getValueString() ,NumberUtils.convertStringTOBigDecimal(lineControl.get(0).getType("SumatoriaCampoAFinanciar").getValueString()),uid);
										}catch(Exception ex){
											logger.error("Error registrando control de archivo "+ex.getMessage(),ex);
										}
										header.setHeader(headerString);
										lineProceso.add(header);
										FileUtil.createFile(
												fileNameProcesoOutputCopy,
												lineProceso,
												new ArrayList<Type>());
										// Se encripta archivo de proceso
										Boolean encrypt = true;
										String fileNameProcesoPGP = this
												.getPros().getProperty("path")
												.trim()
												+ path_ascard_process
												+ rename(fileNameProceso);
										this.getPgpUtil().setPathInputfile(
												fileNameProcesoOutputCopy);
										this.getPgpUtil().setPathOutputfile(
												fileNameProcesoPGP);
										try {
											this.getPgpUtil().encript();					
											
										} catch (PGPException e) {
											logger.error(
													" ERROR ENCRIPTANDO EL ARCHIVO DE PROCESO "
															+ e.getMessage(), e);
											encrypt = false;
											String obervacion = "Error encriptando archivo de proceso "
													+ e.getMessage();
											registrar_auditoria(
													fileNameProceso, obervacion,uid);
										}
										if (encrypt) {
											String obervacion = "Archivo Procesado Exitosamente";
											registrar_auditoria(
													fileNameProceso, obervacion,uid);
										}
									} else {
										logger.error(" No se ha validado correctamente archivos ");
										String observacion = "No se ha validado correctamente archivos ";
										registrar_auditoria(fileNameProceso,
												observacion,uid);
									}
								} else {
									logger.error(" ARCHIVOS DE  CONTROL VACIO ");
									String observacion = "Archivo de  Control vacion ";
									registrar_auditoria(fileNameProceso,
											observacion,uid);
								}
							}
						} else {
							logger.info(" ARCHIVOS DE CAMPAÑAS DE FACTURACION EXISTE NO SE PROCESA - SE ELIMINAN");
							String context = "/spring/campanaFacturacion/ftpacampfacturacion-config.xml";
							logger.info(" ELIMINADO ARCHIVO "
									+ fileNameControlFullPath);
							FileUtil.delete(fileNameControlFullPath);
							// deleteFileFTP(fileNameControl, context);
							logger.info(" ELIMINADO ARCHIVO "
									+ fileNameProcesoFullPath);
							FileUtil.delete(fileNameProcesoFullPath);
							// deleteFileFTP(fileNameProceso, context);
						}
					} catch (FinancialIntegratorException ex) {
						logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : "
								+ ex.getMessage(),ex);
						String observacion = "Error copiando archivos de para el proceso "
								+ ex.getMessage();
						registrar_auditoria(fileNameProceso, observacion,uid);
					} catch (Exception ex) {
						logger.error(" ERRROR GENERAL EN PROCESO.. : "
								+ ex.getMessage(),ex);
						String observacion = "Error copiando archivos de para el proceso "
								+ ex.getMessage();
						registrar_auditoria(fileNameProceso, observacion,uid);
					}
					logger.info(" ELIMINADO ARCHIVO ");
					FileUtil.delete(fileNameControlFullPath);
					FileUtil.delete(fileNameProcesoFullPath);
				} else {
					logger.info(" Archivo No Existe ");
					String fileName = "";
					String fileNameProcesoFullPath = "";
					String fileNameControlFullPath = "";
					if (fileProceso == null && fileControl == null) {
						fileName = "Archivo de Campa&ntilde;a, Control ";
					} else if (fileProceso == null) {
						fileName = " Archivo de Campa&ntilde;a ";
						fileNameControlFullPath = this.getPros()
								.getProperty("path").trim()
								+ fileControl.getName();
					} else if (fileControl == null) {
						fileName = "Archivo de Control ";
						fileNameProcesoFullPath = this.getPros()
								.getProperty("path").trim()
								+ fileProceso.getName();
					}
					if (!fileNameControlFullPath.equals("")) {
						FileUtil.delete(fileNameControlFullPath);
					}
					if (!fileNameProcesoFullPath.equals("")) {
						FileUtil.delete(fileNameProcesoFullPath);
					}
					sendMail(fileName);
				}
			}
		}

	}

	/**
	 * Configuración de archivo de control para poder leerlo
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileControl(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CantidadRegistro");
		type.setTypeData(new ObjectType(Integer.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("SumatoriaCampoAFinanciar");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

	/**
	 * Configuración de archivo de proceso para poder leerlo
	 * 
	 * @param file
	 * @return
	 */
	public FileConfiguration configurationFileProceso(String file) {

		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("CustcodeServicio");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Codigo");
		type.setTypeData(new ObjectType(Double.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Tipo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("ValorAaplicar");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("FechaDeGeneración");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("Descripcion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(0);
		type.setSeparator("|");
		type.setName("IMEI");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setHeader(false);
		return _fileConfiguration;
	}

}
