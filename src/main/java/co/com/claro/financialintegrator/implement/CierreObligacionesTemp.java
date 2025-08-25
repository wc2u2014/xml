package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.CallableStatement;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.log4j.Logger;
import org.apache.poi.ss.format.CellTextFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesDatosGenerales;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesFechas;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesIntereses;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesMoras;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesMovimientos;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesNovedades;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesPlanPago;
import co.com.claro.financialintegrator.domain.ArchivoCierreObligacionesSaldoMesAnterior;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleConnection;


/**
 * Cargue archivo de consulta de usuarios
 * 
 * @author Oracle
 *
 */
public class CierreObligacionesTemp extends GenericProccess {
	private Logger logger = Logger.getLogger(CierreObligacionesTemp.class);

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
	
	public String renameFile(String fileName)
			throws FinancialIntegratorException {
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
			logger.error(
					"Error creando nombre de archivo de salida "
							+ e.getMessage(), e);
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

	/**
	 * Proceso del archivo para generar archivo de activaciones
	 */
	@Override
	public void process() {
		logger.info(".............. Iniciando proceso Activaciones Dev.................. ");
		// TODO Auto-generated method stub
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
			String path_excel_tmp = this.getPros().getProperty("pathExcelTmp");

			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim());
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + history);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_excel_tmp);
			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}
			logger.info("................Buscando Archivos de Consulta Usuarios.................. ");
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
						
						String fileNameConsulta = fileConsultaUsuarios.getName();
						String fileNameConsultaFullPath = this.getPros().getProperty("path").trim()
								+ fileNameConsulta;
						// Se mueve los archivos carpeta de procesos para
						// empezar el
						// flujo
						String fileNameConsultaUsuariosProcess = this.getPros().getProperty("path").trim()+path_process
								+ fileNameConsulta;
						try {
							if (!FileUtil.fileExist(fileNameConsultaUsuariosProcess) && fileNameConsulta.endsWith(this.getPros().getProperty("ExtfileConsulta"))) {
								if ((FileUtil.copy(fileNameConsultaFullPath, fileNameConsultaUsuariosProcess))) {
									this.getPgpUtil()
									.setPathInputfile(fileNameConsultaUsuariosProcess);
									// Toca formatear nombre para quitar prefijo BGH
									// Y
									// PREFIJO TXT Y PGP
									String fileOuput = this.getPros()
											.getProperty("path").trim()
											+ path_process
											+ renameFile(fileNameConsulta);
									this.getPgpUtil().setPathOutputfile(fileOuput);
									// Se verifica si se desencripta archivo
									//this.getPgpUtil().decript();
									
									/***********************************************************
									 * 
									 */
									
									//FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
									/*********************************************************
									 * 
									 */
									
									
									if(fileNameConsulta.startsWith(this.getPros().getProperty("fileNovedades")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileNovedades"));
										
										
								
										
										
										procesarArchivoNovedades(fileOuput,uid);
										
									}else if(fileNameConsulta.startsWith(this.getPros().getProperty("fileMovimientos")))
									{
										//if (!FileUtil.fileExist(fileOuput)) {
											//FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
										//}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileMovimientos"));
										
										
										
										
										//Empieza conversion a EXCEL
										String fileExcel=fileNameConsulta.replace(".PGP", "");										
										fileExcel=fileExcel.replace(this.getPros().getProperty("fileOutputExt"), "");

										
										procesarArchivoMovimientos(fileOuput,uid);
										
									}else if(fileNameConsulta.startsWith(this.getPros().getProperty("fileIntereses")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileIntereses"));
										
										

										
										procesarArchivoIntereses(fileOuput,uid);
										
									}else if(fileNameConsulta.startsWith(this.getPros().getProperty("fileMoras")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileMoras"));
										
										
										
										

										procesarArchivoMoras(fileOuput,uid);
										
									}
									else if(fileNameConsulta.startsWith(this.getPros().getProperty("fileFechas")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileFechas"));
										
										
										
										

										
										procesarArchivoFechas(fileOuput,uid);
										
									}else if(fileNameConsulta.startsWith(this.getPros().getProperty("fileSaldoMesesAnteriores")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileSaldoMesesAnteriores"));
										
										
										
										
										procesarArchivoSaldos(fileOuput,uid);
										
									}else if(fileNameConsulta.startsWith(this.getPros().getProperty("fileDatosGenerales")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("fileDatosGenerales"));
										
										
									
										
										procesarArchivoDatosGenerales(fileOuput,uid);
										
									}else if(fileNameConsulta.startsWith(this.getPros().getProperty("filePlanPagos")))
									{
//										if (!FileUtil.fileExist(fileOuput)) {
//											FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
//										}
										logger.info(" SE DETECTA EL ARCHIVO COMO: "+this.getPros().getProperty("filePlanPagos"));
										
										
										
										
										procesarArchivoPlanPagos(fileOuput,uid);
										
									}
									
									
									
									
										logger.info(" EL ARCHIVO SE HA PROCESADO EL ARCHIVO");
										

										
										
										
										
										
										
										
										
										
										
										
										

								} else {
									logger.error("ARCHIVO DE CONTROL ESTA VACIO..");
								}
								logger.info(" ELIMINADO ARCHIVO ");
								FileUtil.delete(fileNameConsultaFullPath);
								FileUtil.delete(fileNameConsultaUsuariosProcess);
							} else {
								logger.info(" ARCHIVOS DE ACTIVACIONES EXISTE NO SE PROCESA");
							}

						} catch (FinancialIntegratorException ex) {
							logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : " + ex.getMessage(),ex);
							String observacion = "Error copiando archivos  para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameConsulta, observacion,uid);
						} catch (Exception ex) {
							logger.error(" ERRROR GENERAL EN PROCESO.. : " + ex.getMessage(),ex);
							String observacion = "Error copiando archivos de para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameConsulta, observacion,uid);
						}
					} 
				}
			} else {
				logger.error("NO SE ENCONTRARON LOS ARCHIVOS DE CIERRE DE OBLIGACIONES");
				// this.sendMail();
			}
		} catch (

		Exception e) {
			logger.error("Excepcion no Controlada  en proceso de Cierre " + e.getMessage(), e);
		}

	}
	
	


	//Inician Metodos de guardar en BD
	/**
	 * guardarArchivoBDNovedades
	 * @param lineConsulta ArchivoCierreObligacionesNovedades
	 * @return Integer
	 */
	private ArrayList<Integer> guardarArchivoBDNovedades(List<ArchivoCierreObligacionesNovedades> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesNovedades consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_NOVEDADES").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_NOVEDADES").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarNovedades").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			
			
			
			if (cs != null) {
				if(cs.getArray(4)!=null) {
					while(cs.getArray(4).getResultSet().next())
					{
						listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
					}
				}
				logger.info("Call : " + this.getPros().getProperty("callRegistrarNovedades").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarNovedades").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarNovedades").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	/**
	 * guardarArchivoBDMovimientos
	 * @param lineConsulta ArchivoCierreObligacionesMovimientos
	 * @return Integer
	 */
	private ArrayList<Integer> guardarArchivoBDMovimientos(List<ArchivoCierreObligacionesMovimientos> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesMovimientos consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_MOVIMIENTOS").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_MOVIMIENTOS").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarMovimientos").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			while(cs.getArray(4).getResultSet().next())
			{
				listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
			}
			
			
			
			
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrarMovimientos").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarMovimientos").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarMovimientos").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	
	/**
	 * guardarArchivoBDIntereses
	 * @param lineConsulta ArchivoCierreObligacionesIntereses
	 * @return Integer
	 */
	private ArrayList<Integer> guardarArchivoBDIntereses(List<ArchivoCierreObligacionesIntereses> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesIntereses consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_INTERESES").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_INTERESES").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarIntereses").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			while(cs.getArray(4).getResultSet().next())
			{
				listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
			}
			
			
			
			
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrarIntereses").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarIntereses").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarIntereses").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	
	/**
	 * guardarArchivoBDMoras
	 * @param ArchivoCierreObligacionesMoras
	 * @return Integer
	 */
	private ArrayList<Integer> guardarArchivoBDMoras(List<ArchivoCierreObligacionesMoras> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesMoras consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_MORAS").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_MORAS").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarMoras").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			while(cs.getArray(4).getResultSet().next())
			{
				listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
			}
			
			
			
			
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrarMoras").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarMoras").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarMoras").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	
	private ArrayList<Integer> guardarArchivoBDFechas(List<ArchivoCierreObligacionesFechas> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesFechas consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_FECHAS").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_FECHAS").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarFechas").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			
			
			
			if (cs != null) {
				if(cs.getArray(4)!=null) {
					while(cs.getArray(4).getResultSet().next())
					{
						listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
					}
				}
				logger.info("Call : " + this.getPros().getProperty("callRegistrarFechas").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarFechas").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarFechas").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	/**
	 * guardarArchivoBDSaldos
	 * @param lineConsulta
	 * @return
	 */
	private ArrayList<Integer> guardarArchivoBDSaldos(List<ArchivoCierreObligacionesSaldoMesAnterior> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesSaldoMesAnterior consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_SALDOMESESANTERIORES").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_SALDOMESESANTERIORES").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarSaldoMesesAnteriores").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			while(cs.getArray(4).getResultSet().next())
			{
				listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
			}
			
			
			
			
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrarSaldoMesesAnteriores").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarSaldoMesesAnteriores").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarSaldoMesesAnteriores").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	
	/**
	 * guardarArchivoBDSaldos
	 * @param lineConsulta ArchivoCierreObligacionesSaldoMesAnterior
	 * @return Integer
	 */
	private ArrayList<Integer> guardarArchivoBDDatosGenerales(List<ArchivoCierreObligacionesDatosGenerales> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesDatosGenerales consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_DATOSGENERALES").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_DATOSGENERALES").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarDatosGenerales").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			while(cs.getArray(4).getResultSet().next())
			{
				listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
			}
			
			
			
			
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrarDatosGenerales").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarDatosGenerales").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarDatosGenerales").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	/**
	 * guardarArchivoBDPlanPagos
	 * @param lineConsulta ArchivoCierreObligacionesPlanPago
	 * @return Integer
	 */
	private ArrayList<Integer> guardarArchivoBDPlanPagos(List<ArchivoCierreObligacionesPlanPago> lineConsulta,String uid) {
		Database _database = null;
		ArrayList<Integer> listaFallidos=null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ArchivoCierreObligacionesPlanPago consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT_PLANPAGOS").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY_PLANPAGOS").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrarPlanPagos").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.NUMERIC);
	
			
			
			cs = _database.executeCallOutputsV2(_database.getConn(uid), output,
					input,uid);
			listaFallidos= new ArrayList<Integer>();
			
			while(cs.getArray(4).getResultSet().next())
			{
				listaFallidos.add(cs.getArray(4).getResultSet().getInt(1));
			}
			
			
			
			
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrarPlanPagos").trim() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getInt(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarPlanPagos").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrarPlanPagos").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return listaFallidos;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	private List<ArchivoCierreObligacionesNovedades> procesarArchivoNovedades(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitNovedades"));
		
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesNovedades> lines = new ArrayList<ArchivoCierreObligacionesNovedades>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesNovedades consulta = new ArchivoCierreObligacionesNovedades();
						consulta.setNumeroDelCredito(value_split[0].trim());
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[1].trim())?null:sdfrmt.parse(value_split[1].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechaDeLaNovedad(date);
						
						consulta.setModificacion(convertString(value_split[2],25));
						consulta.setUsuario(convertString(value_split[3],10));
						consulta.setNombrearchivo(f.getName().trim());
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesNovedades>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				
				
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDNovedades(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesNovedades archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getFechaDeLaNovedad()+"\\|"+archivo.getModificacion()+"\\|"+archivo.getNumeroDelCredito()+"\\|"+archivo.getUsuario());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					//Se envia lista para crear archivo excel
					}
				
				
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDNovedades(lines,uid);
				if(listaFallidos!=null)
				{
					
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesNovedades archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getFechaDeLaNovedad()+"\\|"+archivo.getModificacion()+"\\|"+archivo.getNumeroDelCredito()+"\\|"+archivo.getUsuario());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();
				//Se envia lista para crear archivo excel

				}
			
			
			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesNovedades",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}

	private String convertString(String campo, int largo)  {
//		String temp = new String(campo.trim().getBytes("ISO-8859-1"), "UTF-8");
//		
//		// first encode the utf-16 string as a ByteBuffer
//		ByteBuffer bb = Charset.forName("ISO-8859-1").encode(CharBuffer.wrap(campo.trim()));
//		// then decode those bytes as US-ASCII
//		CharBuffer ascii = Charset.forName("US-ASCII").decode(bb);
//		return ascii.toString();
		
		
		// strips off all non-ASCII characters
	    String text = campo.replaceAll("Ñ", "N");
	    
	 // strips off all non-ASCII characters
	     text = text.replaceAll("ñ", "n");

		
	    // strips off all non-ASCII characters
	     text = text.replaceAll("[^\\x00-\\x7F]", "");

	    // erases all the ASCII control characters
	    text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

	    
	    // removes non-printable characters from Unicode
	    text = text.replaceAll("\\p{C}", "");
	    
		if(text.length()>largo) {
			text = text.substring(0, largo-1);
		}
		return text;
	}
	
	
	
	
	
	
	private List<ArchivoCierreObligacionesMovimientos> procesarArchivoMovimientos(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitMovimientos"));
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesMovimientos> lines = new ArrayList<ArchivoCierreObligacionesMovimientos>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesMovimientos consulta = new ArchivoCierreObligacionesMovimientos();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[1].trim())?null:sdfrmt.parse(value_split[1].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechatransaccion(date);
						String tx = value_split[2];
						if(tx==null || "".equals(tx.trim())) {
							tx = "0";
						} else {
							tx = tx.trim();
						}
						consulta.setTransaccion(new Long(tx));
						String concepto = value_split[3];
						if(concepto==null || "".equals(concepto.trim())) {
							concepto = "0";
						} else {
							concepto = concepto.trim();
						}					
						consulta.setConcepto(new Long(concepto));
						
						Date javaDate2 = ("00000000".equals(value_split[4].trim())?null:sdfrmt.parse(value_split[4].trim()));
					    java.sql.Date date2=null;
					    if(javaDate2!=null)
					    {
					    	date2= new java.sql.Date(javaDate2.getTime());						
					    }
					    consulta.setFechafacturacion(date2);
					    Date javaDate3 = ("00000000".equals(value_split[5].trim())?null:sdfrmt.parse(value_split[5].trim()));
					    java.sql.Date date3=null;
					    if(javaDate3!=null)
					    {
					    	date3= new java.sql.Date(javaDate3.getTime());						
					    }					    
					    consulta.setFechaaplicacion(date3);
					    
						consulta.setValortransaccion(Double.parseDouble(value_split[6].trim()));
						consulta.setInteres(value_split[7].trim());
						String cuota = value_split[8];
						if(cuota==null || "".equals(cuota.trim())) {
							cuota = "0";
						} else {
							cuota = cuota.trim();
						}
						consulta.setNumeroinicialcuotas(new Long(cuota));						
						consulta.setNombrearchivo(f.getName().trim());


						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesMovimientos>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
						
					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDMovimientos(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesMovimientos archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechatransaccion()+"\\|"+archivo.getTransaccion()+"\\|"+archivo.getConcepto()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getFechaaplicacion()+"\\|"+archivo.getValortransaccion()+"\\|"+archivo.getInteres()+"\\|"+archivo.getNumeroinicialcuotas());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();

					}
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDMovimientos(lines,uid);
				if(listaFallidos!=null)
				{
					
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesMovimientos archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechatransaccion()+"\\|"+archivo.getTransaccion()+"\\|"+archivo.getConcepto()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getFechaaplicacion()+"\\|"+archivo.getValortransaccion()+"\\|"+archivo.getInteres()+"\\|"+archivo.getNumeroinicialcuotas());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();
				//Se envia lista para crear archivo excel

				}
			
			

			
			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesMovimientos",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}
	
	/**
	 * procesarArchivoIntereses
	 * @param fileNameConsultaUsuariosCopy
	 * @param fileNameConsultaUsuariosExcel 
	 * @return ArchivoCierreObligacionesIntereses
	 */
	private List<ArchivoCierreObligacionesIntereses> procesarArchivoIntereses(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitIntereses"));
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesIntereses> lines = new ArrayList<ArchivoCierreObligacionesIntereses>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesIntereses consulta = new ArchivoCierreObligacionesIntereses();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[1].trim())?null:sdfrmt.parse(value_split[1].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechafacturacion(date);
						consulta.setCodigo(new Long(value_split[2].trim()));
						consulta.setValor(Double.parseDouble(value_split[3].trim()));
												
						consulta.setNombrearchivo(f.getName().trim());
						
						
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesIntereses>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);	
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
						

					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDIntereses(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesIntereses archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getCodigo()+"\\|"+archivo.getValor());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					//Se envia lista para crear archivo excel
					}
				
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDIntereses(lines,uid);
				if(listaFallidos!=null)
				{
					
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesIntereses archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getCodigo()+"\\|"+archivo.getValor());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();

				}
			

			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesIntereses",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
				
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}
	
	
	
	/**
	 * procesarArchivoMoras
	 * @param fileNameConsultaUsuariosCopy
	 * @param fileNameConsultaUsuariosExcel 
	 * @return ArchivoCierreObligacionesMoras
	 */
	private List<ArchivoCierreObligacionesMoras> procesarArchivoMoras(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitMoras"));
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesMoras> lines = new ArrayList<ArchivoCierreObligacionesMoras>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesMoras consulta = new ArchivoCierreObligacionesMoras();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[1].trim())?null:sdfrmt.parse(value_split[1].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechafacturacion(date);
						
						Date javaDate2 = ("00000000".equals(value_split[2].trim())?null:sdfrmt.parse(value_split[2].trim()));
					    java.sql.Date date2=null;
					    if(javaDate2!=null)
					    {
					    	date2= new java.sql.Date(javaDate2.getTime());						
					    }
					    consulta.setFechamora(date2);
					    
						consulta.setValorinicial(Double.parseDouble(value_split[3].trim()));
						consulta.setValoractual(Double.parseDouble(value_split[4].trim()));
						consulta.setTasanominal(value_split[5].trim());
						consulta.setEdadmora(new Long(value_split[6].trim()));
						consulta.setEstadotransaccion(value_split[7].trim());
						
						
						consulta.setNombrearchivo(f.getName().trim());
						
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesMoras>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
						

					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDMoras(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesMoras archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getFechamora()+"\\|"+archivo.getValorinicial()+"\\|"+archivo.getValoractual()+"\\|"+archivo.getTasanominal()+"\\|"+archivo.getEdadmora()+"\\|"+archivo.getEstadotransaccion());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					}
				
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDMoras(lines,uid);
				if(listaFallidos!=null)
				{
					
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesMoras archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getFechamora()+"\\|"+archivo.getValorinicial()+"\\|"+archivo.getValoractual()+"\\|"+archivo.getTasanominal()+"\\|"+archivo.getEdadmora()+"\\|"+archivo.getEstadotransaccion());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();
				}
			

			
			
			
			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesMoras",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();	
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}
	
	private List<ArchivoCierreObligacionesFechas> procesarArchivoFechas(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitFechas"));
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesFechas> lines = new ArrayList<ArchivoCierreObligacionesFechas>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				if (!line.trim().equals("")) {
					String[] value_split = line.split("\\|");
					try {
						ArchivoCierreObligacionesFechas consulta = new ArchivoCierreObligacionesFechas();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[1].trim())?null:sdfrmt.parse(value_split[1].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }
						consulta.setCreacioncredito(date);
											
						Date javaDate2 = ("00000000".equals(value_split[2].trim())?null:sdfrmt.parse(value_split[2].trim()));
					    java.sql.Date date2=null;
					    if(javaDate2!=null)
					    {
					    	date2= new java.sql.Date(javaDate2.getTime());						
					    }
					    consulta.setUltimobloqueo(date2);
					    					    
					    Date javaDate3 = ("00000000".equals(value_split[3].trim())?null:sdfrmt.parse(value_split[3].trim()));
					    java.sql.Date date3=null;
					    if(javaDate3!=null)
					    {
					    	date3= new java.sql.Date(javaDate3.getTime());						
					    }					    
					    consulta.setDudosorecaudo(date3);
					    
					    Date javaDate4 = ("00000000".equals(value_split[4].trim())?null:sdfrmt.parse(value_split[4].trim()));
					    java.sql.Date date4=null;
					    if(javaDate4!=null)
					    {
					    	date4= new java.sql.Date(javaDate4.getTime());						
					    }					    
					    consulta.setUltimamora(date4);
						
					    Date javaDate5 = ("00000000".equals(value_split[5].trim())?null:sdfrmt.parse(value_split[5].trim()));
					    java.sql.Date date5=null;
					    if(javaDate5!=null)
					    {
					    	date5= new java.sql.Date(javaDate5.getTime());						
					    }					    
					    consulta.setUltimopago(date5);
					    
					    Date javaDate6 = ("00000000".equals(value_split[6].trim())?null:sdfrmt.parse(value_split[6].trim()));
					    java.sql.Date date6=null;
					    if(javaDate6!=null)
					    {
					    	date6= new java.sql.Date(javaDate6.getTime());						
					    }					    
					    consulta.setUltimaventa(date6);
					    
					    Date javaDate7 = ("00000000".equals(value_split[7].trim())?null:sdfrmt.parse(value_split[7].trim()));
					    java.sql.Date date7=null;
					    if(javaDate7!=null)
					    {
					    	date7= new java.sql.Date(javaDate7.getTime());						
					    }					    
					    consulta.setUltimoajuste(date7);
					    
					    Date javaDate8 = ("00000000".equals(value_split[8].trim())?null:sdfrmt.parse(value_split[8].trim()));
					    java.sql.Date date8=null;
					    if(javaDate8!=null)
					    {
					    	date8= new java.sql.Date(javaDate8.getTime());						
					    }					    
					    consulta.setUltimamodificacion(date8);
					    
					    Date javaDate9 = ("00000000".equals(value_split[9].trim())?null:sdfrmt.parse(value_split[9].trim()));
					    java.sql.Date date9=null;
					    if(javaDate9!=null)
					    {
					    	date9= new java.sql.Date(javaDate9.getTime());						
					    }					    
					    consulta.setPrimeruso(date9);
					    
					    Date javaDate10 = ("00000000".equals(value_split[10].trim())?null:sdfrmt.parse(value_split[10].trim()));
					    java.sql.Date date10=null;
					    if(javaDate10!=null)
					    {
					    	date10= new java.sql.Date(javaDate10.getTime());						
					    }					    
					    consulta.setProximafacturacion(date10);
						
					    Date javaDate11 = ("00000000".equals(value_split[11].trim())?null:sdfrmt.parse(value_split[11].trim()));
					    java.sql.Date date11=null;
					    if(javaDate11!=null)
					    {
					    	date11= new java.sql.Date(javaDate11.getTime());						
					    }					    
					    consulta.setProcesoactual(date11);
					    
					    Date javaDate12 = ("00000000".equals(value_split[12].trim())?null:sdfrmt.parse(value_split[12].trim()));
					    java.sql.Date date12=null;
					    if(javaDate12!=null)
					    {
					    	date12= new java.sql.Date(javaDate12.getTime());						
					    }					    
					    consulta.setProcesoanterior(date12);
					    
					    Date javaDate13 = ("00000000".equals(value_split[13].trim())?null:sdfrmt.parse(value_split[13].trim()));
					    java.sql.Date date13=null;
					    if(javaDate13!=null)
					    {
					    	date13= new java.sql.Date(javaDate13.getTime());						
					    }					    
					    consulta.setProximocorte(date13);
					    
					    Date javaDate14 = ("00000000".equals(value_split[14].trim())?null:sdfrmt.parse(value_split[14].trim()));
					    java.sql.Date date14=null;
					    if(javaDate14!=null)
					    {
					    	date14= new java.sql.Date(javaDate14.getTime());						
					    }					    
					    consulta.setCorteanterior(date14);
					    
					    Date javaDate15 = ("00000000".equals(value_split[15].trim())?null:sdfrmt.parse(value_split[15].trim()));
					    java.sql.Date date15=null;
					    if(javaDate15!=null)
					    {
					    	date15= new java.sql.Date(javaDate15.getTime());						
					    }					    
					    consulta.setLimitepago(date15);
					    
					    Date javaDate16 = ("00000000".equals(value_split[16].trim())?null:sdfrmt.parse(value_split[16].trim()));
					    java.sql.Date date16=null;
					    if(javaDate16!=null)
					    {
					    	date16= new java.sql.Date(javaDate16.getTime());						
					    }					    
					    consulta.setLimitepagoanterior(date16);
					    
					    
					    
						
						consulta.setNombrearchivo(f.getName().trim());
						
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesFechas>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
						
					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				try {
					if((lines.size()%blockLimit)==0) {
						ArrayList<Integer>listaFallidos=guardarArchivoBDFechas(lines,uid);
						if(listaFallidos!=null)
						{
							
							
							for(int i=0;i<listaFallidos.size();i++)
							{
								ArchivoCierreObligacionesFechas archivo=lines.get(listaFallidos.get(i).intValue()-1);
								fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getCreacioncredito()+"\\|"+archivo.getUltimobloqueo()+"\\|"+archivo.getDudosorecaudo()+"\\|"+archivo.getUltimamora()+"\\|"+archivo.getUltimopago()+"\\|"+archivo.getUltimaventa()+"\\|"+archivo.getUltimoajuste()+"\\|"+archivo.getUltimamodificacion()+"\\|"+archivo.getPrimeruso()+"\\|"+archivo.getProximafacturacion()+"\\|"+archivo.getProcesoactual()+"\\|"+archivo.getProcesoanterior()+"\\|"+archivo.getProximocorte()+"\\|"+archivo.getCorteanterior()+"\\|"+archivo.getLimitepago()+"\\|"+archivo.getLimitepagoanterior());
							}
							totalFallidos+=fallidos.size();
							if(fallidos.size()>0) {
							fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
							bw = new BufferedWriter(fw);
							for(int i =0;i<fallidos.size();i++)
							{
								bw.write(fallidos.get(i));
								bw.newLine();
							}
							
							
							}
							
							
							
						}
						
						lines.clear();
						fallidos.clear();
						}
				} catch (Exception e) {
					logger.error("Error guardando archivo : " + e.getMessage(), e);
				}
				
			}
			
			try {
				if(lines.size()>0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDFechas(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesFechas archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getCreacioncredito()+"\\|"+archivo.getUltimobloqueo()+"\\|"+archivo.getDudosorecaudo()+"\\|"+archivo.getUltimamora()+"\\|"+archivo.getUltimopago()+"\\|"+archivo.getUltimaventa()+"\\|"+archivo.getUltimoajuste()+"\\|"+archivo.getUltimamodificacion()+"\\|"+archivo.getPrimeruso()+"\\|"+archivo.getProximafacturacion()+"\\|"+archivo.getProcesoactual()+"\\|"+archivo.getProcesoanterior()+"\\|"+archivo.getProximocorte()+"\\|"+archivo.getCorteanterior()+"\\|"+archivo.getLimitepago()+"\\|"+archivo.getLimitepagoanterior());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					}
			} catch (Exception e) {
				logger.error("Error guardando archivo : " + e.getMessage(), e);
			}
			
		
			
			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesMovimientos",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Throwable e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}
	
	
	/**
	 * procesarArchivoSaldos
	 * @param fileNameConsultaUsuariosCopy
	 * @param fileNameConsultaUsuariosExcel 
	 * @return ArchivoCierreObligacionesSaldoMesAnterior
	 */
	private List<ArchivoCierreObligacionesSaldoMesAnterior> procesarArchivoSaldos(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitSaldoMesesAnteriores"));
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesSaldoMesAnterior> lines = new ArrayList<ArchivoCierreObligacionesSaldoMesAnterior>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesSaldoMesAnterior consulta = new ArchivoCierreObligacionesSaldoMesAnterior();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[1].trim())?null:sdfrmt.parse(value_split[1].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechafacturacion(date);
						consulta.setPagominimo(Double.parseDouble(value_split[2].trim()));
						consulta.setSaltototalcorte (Double.parseDouble(value_split[3].trim()));
						consulta.setSaltocapitalcorte (Double.parseDouble(value_split[4].trim()));
						
						consulta.setNombrearchivo(f.getName().trim());
						
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesSaldoMesAnterior>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);

						}
						

					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDSaldos(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesSaldoMesAnterior archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getPagominimo()+"\\|"+archivo.getSaltototalcorte()+"\\|"+archivo.getSaltocapitalcorte());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					}
				
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDSaldos(lines,uid);
				if(listaFallidos!=null)
				{
					
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesSaldoMesAnterior archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getFechafacturacion()+"\\|"+archivo.getPagominimo()+"\\|"+archivo.getSaltototalcorte()+"\\|"+archivo.getSaltocapitalcorte());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();
				}
			
			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesMovimientos",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}
	
	
	/**
	 * procesarArchivoDatosGenerales
	 * @param fileNameConsultaUsuariosCopy
	 * @param fileNameConsultaUsuariosExcel 
	 * @return ArchivoCierreObligacionesDatosGenerales
	 */
	private List<ArchivoCierreObligacionesDatosGenerales> procesarArchivoDatosGenerales(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitDatosGenerales"));
		int numeroLineasArchivo=0;
		String path_error = this.getPros().getProperty("pathError");
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesDatosGenerales> lines = new ArrayList<ArchivoCierreObligacionesDatosGenerales>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesDatosGenerales consulta = new ArchivoCierreObligacionesDatosGenerales();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						consulta.setNumeroidentificacion(new Long(value_split[1].trim()));
						consulta.setTipoidentificacion(convertString(value_split[2].trim(), 40) );
						consulta.setIdimpresionextracto(convertString(value_split[3].trim(), 5));
						consulta.setCiudaddepartamento(convertString(value_split[4].trim(), 40));
						consulta.setTelefonocasa(convertString(value_split[5].trim(), 20));
						consulta.setTelefonooficina(convertString(value_split[6].trim(), 20));
						consulta.setZonapostal(convertString(value_split[7].trim(), 10));
						consulta.setReferenciapago(convertString(value_split[8].trim(), 15));
						consulta.setValorcuotaactualfija(Double.parseDouble(value_split[9].trim()));
						consulta.setImei(convertString(value_split[10].trim(), 25));
						consulta.setBloqueoimei(convertString(value_split[11].trim(), 5));
						consulta.setTipocredito(convertString(value_split[12].trim(), 40));
						consulta.setGrupoafinidad(convertString(value_split[13].trim(), 40));
						consulta.setOficinaventa(convertString(value_split[14].trim(), 40));
						consulta.setCodigoasesor(convertString(value_split[15].trim(), 20));
						consulta.setCuentatarjetadomiciliacion(convertString(value_split[16].trim(), 20));
						consulta.setFranquiciadomiciliacion(convertString(value_split[17].trim(), 20));
						consulta.setTipocuentadomiciliacion(convertString(value_split[18].trim(), 5));
						consulta.setCiclofacturacion(convertString(value_split[19].trim(), 40));
						consulta.setCodigotasainteres(convertString(value_split[20].trim(), 40));
						consulta.setTipoproceso(convertString(value_split[21].trim(), 20));
						consulta.setCuentarr(convertString(value_split[22].trim(), 25));
						consulta.setReferenciaequipo(convertString(value_split[23].trim(), 25));
						consulta.setNumerocelular(convertString(value_split[24].trim(), 25));
						consulta.setNumerocontrato(convertString(value_split[25].trim(), 25));
						consulta.setCustcodeservicio(convertString(value_split[26].trim(), 25));
						consulta.setCustcodecuentamaestra(convertString(value_split[27].trim(), 25));
						consulta.setExentoiva(convertString(value_split[28].trim(), 5));
						consulta.setFlagnuncacobro(convertString(value_split[29].trim(), 5));
						consulta.setFlagnocobro(convertString(value_split[30].trim(), 5));
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("00000000".equals(value_split[31].trim())?null:sdfrmt.parse(value_split[31].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechavigencianocobro(date);
						
						consulta.setFlagexclusionaceleracion(convertString(value_split[32].trim(), 5));
						Date javaDate2 = ("00000000".equals(value_split[33].trim())?null:sdfrmt.parse(value_split[33].trim()));
					    java.sql.Date date2=null;
					    if(javaDate2!=null)
					    {
					    	date2= new java.sql.Date(javaDate2.getTime());						
					    }						
						consulta.setFechaaceleracion(date2);						
						consulta.setCuotaspendientesantesacelerar(convertString(value_split[34].trim(), 3));
						consulta.setValoracelerado(Double.parseDouble(value_split[35].trim()));
						consulta.setNumerofactura(convertString(value_split[36].trim(), 40));
						consulta.setDireccionenvio(convertString(value_split[37].trim(), 230));
						
						consulta.setNombrearchivo(f.getName().trim());
						
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesDatosGenerales>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
						
					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDDatosGenerales(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesDatosGenerales archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getNumeroidentificacion()+"\\|"+archivo.getTipoidentificacion()+"\\|"+archivo.getIdimpresionextracto()+"\\|"+archivo.getCiudaddepartamento()+"\\|"+archivo.getTelefonocasa()+"\\|"+archivo.getTelefonooficina()+"\\|"+archivo.getZonapostal()+"\\|"+archivo.getReferenciapago()+"\\|"+archivo.getValorcuotaactualfija()+"\\|"+archivo.getImei()+"\\|"+archivo.getBloqueoimei()+"\\|"+archivo.getTipocredito()+"\\|"+archivo.getGrupoafinidad()+"\\|"+archivo.getOficinaventa()+"\\|"+archivo.getCodigoasesor()+"\\|"+archivo.getCuentatarjetadomiciliacion()+"\\|"+archivo.getFranquiciadomiciliacion()+"\\|"+archivo.getTipocuentadomiciliacion()+"\\|"+archivo.getCiclofacturacion()+"\\|"+archivo.getCodigotasainteres()+"\\|"+archivo.getTipoproceso()+"\\|"+archivo.getCuentarr()+"\\|"+archivo.getReferenciaequipo()+"\\|"+archivo.getNumerocelular()+"\\|"+archivo.getNumerocontrato()+"\\|"+archivo.getCustcodeservicio()+"\\|"+archivo.getCustcodecuentamaestra()+"\\|"+archivo.getExentoiva()+"\\|"+archivo.getFlagnuncacobro()+"\\|"+archivo.getFlagnocobro()+"\\|"+archivo.getFechavigencianocobro()+"\\|"+archivo.getFlagexclusionaceleracion()+"\\|"+archivo.getFechaaceleracion()+"\\|"+archivo.getCuotaspendientesantesacelerar()+"\\|"+archivo.getValoracelerado()+"\\|"+archivo.getNumerofactura()+"\\|"+archivo.getDireccionenvio());
						}
						totalFallidos+=fallidos.size();
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					}
				
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDDatosGenerales(lines,uid);
				if(listaFallidos!=null)
				{
					
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesDatosGenerales archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getNumeroidentificacion()+"\\|"+archivo.getTipoidentificacion()+"\\|"+archivo.getIdimpresionextracto()+"\\|"+archivo.getCiudaddepartamento()+"\\|"+archivo.getTelefonocasa()+"\\|"+archivo.getTelefonooficina()+"\\|"+archivo.getZonapostal()+"\\|"+archivo.getReferenciapago()+"\\|"+archivo.getValorcuotaactualfija()+"\\|"+archivo.getImei()+"\\|"+archivo.getBloqueoimei()+"\\|"+archivo.getTipocredito()+"\\|"+archivo.getGrupoafinidad()+"\\|"+archivo.getOficinaventa()+"\\|"+archivo.getCodigoasesor()+"\\|"+archivo.getCuentatarjetadomiciliacion()+"\\|"+archivo.getFranquiciadomiciliacion()+"\\|"+archivo.getTipocuentadomiciliacion()+"\\|"+archivo.getCiclofacturacion()+"\\|"+archivo.getCodigotasainteres()+"\\|"+archivo.getTipoproceso()+"\\|"+archivo.getCuentarr()+"\\|"+archivo.getReferenciaequipo()+"\\|"+archivo.getNumerocelular()+"\\|"+archivo.getNumerocontrato()+"\\|"+archivo.getCustcodeservicio()+"\\|"+archivo.getCustcodecuentamaestra()+"\\|"+archivo.getExentoiva()+"\\|"+archivo.getFlagnuncacobro()+"\\|"+archivo.getFlagnocobro()+"\\|"+archivo.getFechavigencianocobro()+"\\|"+archivo.getFlagexclusionaceleracion()+"\\|"+archivo.getFechaaceleracion()+"\\|"+archivo.getCuotaspendientesantesacelerar()+"\\|"+archivo.getValoracelerado()+"\\|"+archivo.getNumerofactura()+"\\|"+archivo.getDireccionenvio());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();
				}
			
			
			
			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesMovimientos",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();	
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}
	
	
	
	private List<ArchivoCierreObligacionesPlanPago> procesarArchivoPlanPagos(String fileNameConsultaUsuariosCopy,String uid) {
		logger.info("READ FILE UTF8..");
		String path_error = this.getPros().getProperty("pathError");
		int blockLimit=Integer.parseInt(this.getPros().getProperty("blockLimitPlanPagos"));
		int numeroLineasArchivo=0;
		ArrayList<String> fallidos=new ArrayList<String>();
		long totalFallidos=0;
		
		List<ArchivoCierreObligacionesPlanPago> lines = new ArrayList<ArchivoCierreObligacionesPlanPago>();
		File f = null;
		BufferedReader b = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {

					try {
						ArchivoCierreObligacionesPlanPago consulta = new ArchivoCierreObligacionesPlanPago();
						consulta.setNumerocredito(new Long(value_split[0].trim()));
						consulta.setCuota(new Long(value_split[1].trim()));
						consulta.setCapital(Double.parseDouble (value_split[2].trim()));
						consulta.setIntereses(Double.parseDouble(value_split[3].trim()));
						consulta.setIvaintereses(Double.parseDouble (value_split[4].trim()));
						consulta.setValorseguro(Double.parseDouble(value_split[5].trim()));
						consulta.setValorcuotapagar(Double.parseDouble (value_split[6].trim()));
						consulta.setSaldodeuda(Double.parseDouble(value_split[7].trim()));
						
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    Date javaDate = ("".equals(value_split[8])||"00000000".equals(value_split[8].trim())?null:sdfrmt.parse(value_split[8].trim()));
					    java.sql.Date date=null;
					    if(javaDate!=null)
					    {
					    	date= new java.sql.Date(javaDate.getTime());						
					    }						
						consulta.setFechacorte(date);
						
						Date javaDate2 = (value_split[9]==null||"".equals(value_split[9].trim())||"00000000".equals(value_split[9].trim())?null:sdfrmt.parse(value_split[9].trim()));
					    java.sql.Date date2=null;
					    if(javaDate2!=null)
					    {
					    	date2= new java.sql.Date(javaDate2.getTime());						
					    }						
					    consulta.setFechalimitepago(date2);
					    
						consulta.setNombrearchivo(f.getName().trim());
						
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Set<ConstraintViolation<ArchivoCierreObligacionesPlanPago>> violations = validator.validate(consulta);
						if(violations.size()>0)
						{
							fallidos.add(line);
							logger.error("Linea " + line+"    "+f.getName() +"---Objeto---"+ consulta.toString());
							logger.error(violations.iterator().next().getMessage());
						}else
						{
							lines.add(consulta);
						}
						
					} catch (Exception ex) {
						fallidos.add(line);						
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
				else
				{
					totalFallidos++;
				}
				
				numeroLineasArchivo++;
				
				if((lines.size()%blockLimit)==0) {
					ArrayList<Integer>listaFallidos=guardarArchivoBDPlanPagos(lines,uid);
					if(listaFallidos!=null)
					{
						
						
						for(int i=0;i<listaFallidos.size();i++)
						{
							ArchivoCierreObligacionesPlanPago archivo=lines.get(listaFallidos.get(i).intValue()-1);
							fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getCuota()+"\\|"+archivo.getCapital()+"\\|"+archivo.getIntereses()+"\\|"+archivo.getIvaintereses()+"\\|"+archivo.getValorseguro()+"\\|"+archivo.getValorcuotapagar()+"\\|"+archivo.getSaldodeuda()+"\\|"+archivo.getFechacorte()+"\\|"+archivo.getFechalimitepago());
						}
						totalFallidos+=fallidos.size();
						
						if(fallidos.size()>0) {
						fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
						bw = new BufferedWriter(fw);
						for(int i =0;i<fallidos.size();i++)
						{
							bw.write(fallidos.get(i));
							bw.newLine();
						}
						
						
						}
						
						
						
					}
					
					lines.clear();
					fallidos.clear();
					}
				
			}
			
			if(lines.size()>0) {
				ArrayList<Integer>listaFallidos=guardarArchivoBDPlanPagos(lines,uid);
				if(listaFallidos!=null)
				{
					logger.info(f.getCanonicalPath()+" PATHHHHHHHHH   "+f.getAbsolutePath()+"   "+f.getPath());
					
					for(int i=0;i<listaFallidos.size();i++)
					{
						ArchivoCierreObligacionesPlanPago archivo=lines.get(listaFallidos.get(i).intValue()-1);
						fallidos.add(archivo.getNumerocredito()+"\\|"+archivo.getCuota()+"\\|"+archivo.getCapital()+"\\|"+archivo.getIntereses()+"\\|"+archivo.getIvaintereses()+"\\|"+archivo.getValorseguro()+"\\|"+archivo.getValorcuotapagar()+"\\|"+archivo.getSaldodeuda()+"\\|"+archivo.getFechacorte()+"\\|"+archivo.getFechalimitepago());
					}
					totalFallidos+=fallidos.size();
					if(fallidos.size()>0) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					for(int i =0;i<fallidos.size();i++)
					{
						bw.write(fallidos.get(i));
						bw.newLine();
					}
					
					
					}
					
					
					
				}
				
				lines.clear();
				fallidos.clear();

				}
			

			String observacion = "Se ha procesado el archivo correctamente";
			registrar_auditoria_cierreObligaciones(
					f.getName().trim(),
					observacion,"CierreObligacionesMovimientos",new BigDecimal(numeroLineasArchivo),new BigDecimal(totalFallidos),uid);
			

		} catch (FileNotFoundException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error procesando archivo : " + e.getMessage(), e);
		} finally {	
			try {
				if (b != null)
					b.close();
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
				
				

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}

}
