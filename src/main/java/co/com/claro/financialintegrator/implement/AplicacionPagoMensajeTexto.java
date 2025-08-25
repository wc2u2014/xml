package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.TemplateAjustesAscard;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class AplicacionPagoMensajeTexto extends GenericProccess {
	private Logger logger = Logger.getLogger(AplicacionPagoMensajeTexto.class);
	private String call = "";
	private String nameFile="";
	private String nameFileTmp="";


	/**
	 * Se genera el nombre del archivo, de forma conigurable
	 * @param formatDateFileOutput formato de facha de salida
	 * @param extFileOutput extencio del archivo de salida
	 * @param suffixFileOutPuts sufijo del archivo
	 * @return
	 */
	public String nameFile(String formatDateFileOutput, String extFileOutput, String suffixFileOutPut) {
		try {
			
			String dateFormat = DateUtil.getDateFormFormat(formatDateFileOutput);
			String nameFile = suffixFileOutPut + dateFormat + extFileOutput;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archico " + ex.getMessage(), ex);
			;
			return null;
		}

	}
	/**
	 * metodo que valida que la linea si aplica un pago exitoso
	 * 
	 * @param fo
	 * @return
	 */
	private Boolean validarLinea(FileOuput fo) {
		try {
			String ESTADO_REC_MOVDIARIO = "000";
			String MVDTRN_REC_MOVDIARIO = "095";
			// String MVDTRN =this.getPros().getProperty("MVDTRNVALUE");*/
			String estadoTrn = "";
			String mvtrn = "";
			if (fo.getType(TemplatePagosAscard.ESTADO) != null) {
				estadoTrn = fo.getType(TemplatePagosAscard.ESTADO)
						.getValueString().trim();
			}
			if (fo.getType(TemplatePagosAscard.MVDTRN) != null) {
				mvtrn = fo.getType(TemplatePagosAscard.MVDTRN).getValueString()
						.trim();
			}

			// Se valida en estado
			return (estadoTrn.equals(ESTADO_REC_MOVDIARIO) && mvtrn
					.equals(MVDTRN_REC_MOVDIARIO));

		} catch (FinancialIntegratorException e) {
			logger.error("Valor estado no existe " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * inicializa array para comunicar base de datos
	 * 
	 * @param P_NUMERO_PRODUCTO
	 * @return
	 */
	private List<ARRAY> init_arrays(ArrayList P_NUMERO_PRODUCTO,String uid) {
		try {
			Database _database = Database.getSingletonInstance(uid);
			ArrayDescriptor P_REFERENCIA_PAGO_TYPE = ArrayDescriptor
					.createDescriptor("P_NRO_PRODUCTO_TYPE",
							_database.getConn(uid));
			List<ARRAY> arrays = new ArrayList<ARRAY>();
			//
			ARRAY P_NUMERO_PRODUCTO_ARRAY = new ARRAY(P_REFERENCIA_PAGO_TYPE,
					_database.getConn(uid), P_NUMERO_PRODUCTO.toArray());
			arrays.add(P_NUMERO_PRODUCTO_ARRAY);
			return arrays;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ejecuta procedimiento en base de datos
	 * 
	 * @param call
	 * @param array
	 * @param _database
	 * @param lineName
	 */
	private Boolean executeProd(String call, List<ARRAY> array, Database _database,String uid) {
		try {
			//logger.info("** excecute Call ** ");
			_database.setCall(call);
			/*List<ARRAY> list = new ArrayList<ARRAY>();
			list.add(array);*/
			_database.executeCallCursor(array,uid);
			return true;
		} catch (Exception ex) {
			logger.error("error ejecutando procedimiento " + ex.getMessage(),
					ex);
			return false;
		}

	}
	
	
	/**
	 * complete lista de linea con resultado del procedimeinto almacenado
	 * 
	 * @param list
	 * @param array
	 * @return
	 */
	private List<FileOuput> completedLine(HashMap<String, FileOuput> map,
			List<ARRAY> array,String uid) {
		Database _database = Database.getSingletonInstance(uid);
		if (map.size() > 0 && array != null) {
			if (executeProd(call, array, _database,uid)) {
				//logger.info("Executed Prod True ");
				ResultSet rs;
				try {
					rs = (java.sql.ResultSet) _database.getCs().getObject(3);
					List<FileOuput> _result = new ArrayList<FileOuput>();
					while (rs.next()) {

						String nroProducto = rs.getString("NRO_PRODUCTO");
						
						if (map.containsKey(nroProducto)) {
							logger.info("Find .. Nro Producto "+nroProducto);
							String min = rs.getString("MIN");
							String customerId = rs
									.getString("CUSTOMER_ID_SERVICIO");

							FileOuput _fileOuput = (FileOuput) map
									.get(nroProducto);
							List<Type> types = _fileOuput.getTypes();
							List<Type> typesNew = new ArrayList<Type>();
							// MIN
							Type type = new Type();
							type.setName(TemplatePagosAscard.MIN);
							type.setSeparator(";");
							type.setValueString(min);
							typesNew.add(type);
							//
							type = new Type();
							type.setName(TemplatePagosAscard.CUSTOMER);
							type.setSeparator(";");
							type.setValueString(customerId);
							typesNew.add(type);
							typesNew.addAll(types);
							//
							type = new Type();
							type.setName(TemplatePagosAscard.CUSTCODE);
							type.setSeparator(";");
							type.setValueString(_fileOuput.getType(TemplatePagosAscard.CUSTCODE).getValueString());
							typesNew.add(type);
							//
							logger.info(_fileOuput.getType(TemplatePagosAscard.VALORPAGO).getValueString());
							type = new Type();
							type.setName(TemplatePagosAscard.VALORPAGO);
							type.setSeparator(";");
							type.setValueString(_fileOuput.getType(TemplatePagosAscard.VALORPAGO).getValueString());
							typesNew.add(type);
							//
							type = new Type();
							type.setName(TemplatePagosAscard.REFERENCIA);
							type.setSeparator(";");
							type.setValueString(_fileOuput.getType(TemplatePagosAscard.REFERENCIA).getValueString());
							typesNew.add(type);
							//
							type = new Type();
							type.setName(TemplatePagosAscard.TIPOPAGO);
							type.setSeparator(";");
							type.setValueString(_fileOuput.getType(TemplatePagosAscard.TIPOPAGO).getValueString());
							typesNew.add(type);
							//
							type = new Type();
							type.setName(TemplatePagosAscard.FECHAPAGO);
							type.setSeparator(";");
							type.setValueString(_fileOuput.getType(TemplatePagosAscard.FECHAPAGO).getValueString());
							typesNew.add(type);
							//						
							_result.add(new FileOuput(typesNew));
						}

					}
					_database.getCs().close();
					rs.close();
					return _result;
				} catch (SQLException e) {
					logger.info(
							"Error complementando lineas " + e.getMessage(), e);
				} catch (Exception e) {
					logger.info(
							"Error complementando lineas " + e.getMessage(), e);
				}
			}
		}
		return null;

	}

	/**
	 * Procesa los archivo de rechazasos de Ascard y los retorn en on objeto de
	 * salida para generar excel
	 * 
	 * @param path_file
	 * @return
	 */
	private Boolean procesarArchivo(String pathFile, int maxLineas, String uid) {
		TemplatePagosAscard _template = new TemplatePagosAscard();
		HashMap<String, FileOuput> listResult = new HashMap<String, FileOuput>();
		// List<FileOuput> listResult = new ArrayList<FileOuput>();
		List<FileOuput> list = null;

		try {
			FileConfiguration _configuration = _template
					.configurationMovMonetarioDiario(pathFile);
			List<Type> _configurationOutput = _template
					.configurationAplicacionPagosMensajeTexto();
			// Se lee el archivo linea a linea
			File f = new File(pathFile);
			BufferedReader b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			int posLinea = 0;
			ArrayList P_NUMERO_PRODUCTO = new ArrayList();
			
			while ((line = b.readLine()) != null) {
				
				
				FileOuput _fo = FileUtil.readLine(_configuration, line);
				if (validarLinea(_fo)) {
					List<Type> typesLine = new ArrayList<Type>();
					// NRO PRODUCTO
					String nroProducto = "";
					String MVDBIN = "";
					try {
						MVDBIN = _fo.getType("MVDBIN").getValueString();
						// logger.info("MVDBIN " + MVDBIN);
						MVDBIN = MVDBIN.substring(3);

					} catch (FinancialIntegratorException e) {
						logger.error("Propiedad MVDBIN no existe se pondra por defecto");

					}
					String MVDTAR = "";
					try {
						MVDTAR = _fo.getType("MVDTAR").getValueString();
						// logger.info("MVDTAR " + MVDTAR);
						MVDTAR = MVDTAR.substring(5);
					} catch (FinancialIntegratorException e) {
						logger.error("Propiedad MVDTAR no existe se pondra por defecto");

					}
					nroProducto = MVDBIN + MVDTAR;
					// CUSTCODE
					String cuscode = "";
					try {
						cuscode = _fo.getType("RESPPAGO").getValueString()
								.trim();
					} catch (FinancialIntegratorException e) {
						logger.error("Propiedad RESPPAGO no existe se pondra por defecto");

					}

					Type type = new Type();
					type.setName(TemplatePagosAscard.CUSTCODE);
					type.setValueString(cuscode);
					typesLine.add(type);
					// VALOR
					String valor = "";
					try {
						valor = _fo.getType(TemplatePagosAscard.VALOR)
								.getValueString();
					} catch (FinancialIntegratorException e) {
						logger.error("Propiedad VALOR no existe se pondra por defecto");

					}
					type = new Type();
					type.setName(TemplatePagosAscard.VALORPAGO);
					type.setValueString(valor);
					typesLine.add(type);
					// REFERENCIA
					String referencia = "";
					try {
						referencia = _fo.getType(TemplatePagosAscard.REFPAGO)
								.getValueString().trim();
					} catch (FinancialIntegratorException e) {
						logger.error("Propiedad REFPAGO no existe se pondra por defecto");

					}
					type = new Type();
					type.setName(TemplatePagosAscard.REFERENCIA);
					type.setValueString(referencia);
					typesLine.add(type);
					// FECHA
					String fecha = "";
					try {
						fecha = _fo.getType(TemplatePagosAscard.FECHA)
								.getValueString();
						fecha = DateUtils.transforDateString(fecha, "yyyyMMdd","ddMMyyyy");
					} catch (FinancialIntegratorException e) {
						logger.error("Propiedad FECHA no existe se pondra por defecto");

					}
					type = new Type();
					type.setName(TemplatePagosAscard.FECHAPAGO);
					type.setValueString(fecha);
					typesLine.add(type);
					//
					type = new Type();
					type.setName(TemplatePagosAscard.TIPOPAGO);
					type.setValueString("2");
					typesLine.add(type);
					//
					//logger.info("Pago Exitoso " + nroProducto);
					listResult.put(nroProducto, new FileOuput(typesLine));
					P_NUMERO_PRODUCTO.add(nroProducto);
					// el numero de lienas leidas supera las maximas
					if (posLinea >= maxLineas) {
						try{
							//logger.info("Numero de lineas " + posLinea);
							// Se crear archivo
							List<FileOuput> lineas = completedLine(listResult,init_arrays(P_NUMERO_PRODUCTO,uid),uid);

							if (lineas != null && lineas.size() > 0) {
								logger.info("Lineas Completadas " + lineas.size());
								FileUtil.appendFile(
										this.nameFileTmp,
										lineas,
										new ArrayList<Type>(),
										TemplatePagosAscard
												.configurationAplicacionPagosMensajeTexto(),
										false);
								
							}
							listResult.clear();
							P_NUMERO_PRODUCTO.clear();
							P_NUMERO_PRODUCTO = new ArrayList();
							listResult = new HashMap<String, FileOuput>();
							posLinea = 0;
						}catch(Exception ex){
							logger.error("Error procesnado y ejecutando Procedimiento "+ex.getMessage(),ex);
							String obervacion="Error procesnado y ejecutando Procedimiento "+ex.getMessage();
							registrar_auditoria(this.nameFile, obervacion,uid);
						}
						
					}
					posLinea++;
				}

			}
			return true;
		} catch (UnsupportedEncodingException e) {
			logger.error("Error procesando Archivo "+e.getMessage(),e);
			String obervacion="Error procesando Archivo "+e.getMessage();
			registrar_auditoria(this.nameFile, obervacion,uid);
		} catch (FileNotFoundException e) {
			logger.error("Error procesando Archivo "+e.getMessage(),e);
			String obervacion="Error procesando Archivo "+e.getMessage();
			registrar_auditoria(this.nameFile, obervacion,uid);
		} catch (IOException e) {
			logger.error("Error procesando Archivo "+e.getMessage(),e);
			String obervacion="Error procesando Archivo "+e.getMessage();
			registrar_auditoria(this.nameFile, obervacion,uid);
		} catch (Exception e) {
			logger.error("Error procesando Archivo "+e.getMessage(),e);;
			String obervacion="Error procesando Archivo "+e.getMessage();
			registrar_auditoria(this.nameFile, obervacion,uid);
		}
		return false;

	}

	@Override
	public void process() {
             UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info(" ** Aplicacion Pago Mensaje Texto V 1.0 ** ");
		//Se leen propiedades
		HashMap<String, String> prop = consultarPropiedadesBatch(uid);
		setPros(prop);
		String path="";
		String path_process="";
		String path_process_ftp="";
		String extfileProcess="";
		Integer lineNumber=0;
		String pathKeyfile="";
		String passphrase="";
		String signingPublicKeyFilePath="";
		String pgpJarFile="";
		String dataSource="";
		String formatDateFileOutput="";
		String extFileOutput="";
		String suffixFileOutPut="";
		try{
			path = prop.get("file.pathProccess"); 
			path_process = prop.get("file.fileProccess"); 
			path_process_ftp =prop.get("file.pathCopyFile");
			extfileProcess =prop.get("file.ExtfileProcess");
			lineNumber =1000;
			// Se inicializa PGP
			pathKeyfile =prop.get("pgp.pathKeyfile");  
			passphrase = prop.get("pgp.passphrase");
			signingPublicKeyFilePath = prop.get("pgp.signingPublicKeyFilePath");
			pgpJarFile = prop.get("pgp.pgpJarFile");
			inicializarPGP(pathKeyfile, signingPublicKeyFilePath, passphrase,
					pgpJarFile,uid);
			// Properties Database
			dataSource = prop.get("database.DatabaseDataSource");
			this.call = prop.get("database.callAplicacionPagos");
			//Propiedades Archivos de Salida
			formatDateFileOutput= prop.get("fileOutput.fecha");
			extFileOutput=prop.get("fileOutput.extText");
			suffixFileOutPut=prop.get("fileOutput.prefix");
			
		}catch(Exception ex){
			logger.error("Error leyendo propiedades "+ex.getMessage(),ex);
			return;
		}
		//Name Tmp File
		//Name File
		this.nameFile=nameFile(formatDateFileOutput, extFileOutput, suffixFileOutPut);
		this.nameFileTmp=path+path_process+nameFile+".filepart";		
		this.nameFile=path+path_process_ftp+nameFile;
		
		//Auditoria
		this.setAddresPointAuditoria(prop.get("wsdl.WSLAuditoriaBatchAddress"));
		this.setTimeOutaddresPointAuditoria(prop.get("wsdl.WSLAuditoriaBatchPagoTimeOut"));
		Database _database = Database.getSingletonInstance(dataSource, null,uid);

		// Se busca el archivo a processar
		List<File> fileProcessList = null;
		try {
			logger.info("Buscando Archivos Movimientos diarios : " + path);
			fileProcessList = FileUtil.findFileNameFormEndPattern(path,
					extfileProcess);
		} catch (FinancialIntegratorException e) {
			logger.error("Error leyendos Archivos del directorio ", e);
		}
		// carateristicas
		if (fileProcessList != null) {
			for (File fileProcess : fileProcessList) {
				if (fileProcess != null) {
					logger.info("Procesar Archivo " + fileProcess.getName());
					String fileName = fileProcess.getName();
					String fileNameFullPath = path + fileName;
					// Se mueve archivo a encriptado a carpeta de process
					String fileNameCopy = path + path_process + "processes_"
							+ fileName;
					logger.info("Exist File: " + fileNameCopy);
					if (!FileUtil.fileExist(fileNameCopy)) {
						try {
							if (FileUtil.copy(fileNameFullPath, fileNameCopy)) {
								// Se desencripta el archivo
								this.getPgpUtil()
										.setPathInputfile(fileNameCopy);
								String fileOuput = path + path_process
										+ fileName + ".TXT";
								this.getPgpUtil().setPathOutputfile(fileOuput);
								try {
									this.getPgpUtil().decript();
									// Se procesa archivo
									logger.info("File temporary created into folder created into folder "+this.nameFileTmp);
									if (this.procesarArchivo(fileOuput, lineNumber,uid)){										 
										 FileUtil.move(this.nameFileTmp,this.nameFile);
										 logger.info("File temporary moved to "+this.nameFile);
										 String obervacion = "Archivo Procesado Exitosamente";
										 registrar_auditoria(nameFile, obervacion,uid);
									}
								} catch (PGPException e) {
									logger.error(" ERROR DESENCRIPTANDO ARCHIVO : "
											+ e.getMessage());
									 String obervacion = "Error, desencriptando archivo "+e.getMessage();
									 registrar_auditoria(fileName, obervacion,uid);
								}
							}
						} catch (FinancialIntegratorException e) {
							logger.error(" ERROR PROCESANDO ARCHIVO : "
									+ e.getMessage());
							String obervacion = "Error, procesando archivo "+e.getMessage();
							registrar_auditoria(nameFile, obervacion,uid);
						}
					}
					logger.info(" ** ELIMINADO ARCHIVO ** ");
					FileUtil.delete(fileNameFullPath);
				}
			}
		}
		// Se desconecta la base de datos
		_database.disconnet(uid);

	}

}
