package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.ConsultaUsuariosOIM;
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
public class ConsultaUsuarios extends GenericProccess {	private Logger logger = Logger.getLogger(ConsultaUsuarios.class);

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
		String path_processBSC = "";
		if (fileName.contains("hash"))
			path_processBSC = this.getPros().getProperty("path") + this.getPros().getProperty("fileProccessBSCSHash");
		else
			path_processBSC = this.getPros().getProperty("path") + this.getPros().getProperty("fileProccessBSCS");
		String pathFileOriginal = this.getPros().getProperty("path") + this.getPros().getProperty("fileProccess") + fileName;
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
	   UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		try {
			// carpeta de proceso
			String path_process = this.getPros().getProperty("fileProccess");
			String file_partial = this.getPros().getProperty("filePartial");
			// carpeta donde_se_guardan_archivos proceso de ascard
			String history = this.getPros().getProperty("pathHistory");

			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim());
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + history);
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
						this.copyHistory(fileConsultaUsuarios.getName());
						//this.copyBscs(fileConsultaUsuarios.getName());
						String fileNameConsulta = fileConsultaUsuarios.getName();
						String fileNameConsultaFullPath = this.getPros().getProperty("path").trim()
								+ fileNameConsulta;
						// Se mueve los archivos carpeta de procesos para
						// empezar el
						// flujo
						String fileNameConsultaUsuariosProcess = this.getPros().getProperty("path").trim()+path_process
								+ fileNameConsulta;
						try {
							if (!FileUtil.fileExist(fileNameConsultaUsuariosProcess)) {
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
									this.getPgpUtil().decript();
									
									this.copyBscs(renameFile(fileNameConsulta));
									
									if (!FileUtil.fileExist(fileOuput)) {
										FileUtil.copy(fileNameConsultaUsuariosProcess, fileOuput);
									}
									
									if (!fileNameConsulta.contains("hash")) {
										List<ConsultaUsuariosOIM> lineConsulta = procesarArchivo(fileOuput);

										if (fileNameConsultaUsuariosProcess.contains(file_partial)) {
											guardarArchivoBD(lineConsulta, 1,uid);
										} else {
											guardarArchivoBD(lineConsulta, 0,uid);
										}

										logger.info(" EL ARCHIVO SE HA PROCESADO EL ARCHIVO");
										String observacion = "Se ha procesado el archivo correctamente";
										registrar_auditoriaV2(fileNameConsulta, observacion,uid);
									}

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
				// this.sendMail();
			}
		} catch (

		Exception e) {
			logger.error("Excepcion no Controlada  en proceso de activaciones " + e.getMessage(), e);
		}

	}

	private void guardarArchivoBD(List<ConsultaUsuariosOIM> lineConsulta, int tipoCargue, String uid) {
		Database _database = null;
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
			// logger.debug("urlWeblogic " + urlWeblogic);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo informaci�n de  configuracion "
							+ ex.getMessage(), ex);
		}
		java.sql.Struct[] struct = new java.sql.Struct[lineConsulta.size()];
		java.sql.Array array = null;
		int i = 0;

		for (ConsultaUsuariosOIM consulta : lineConsulta) {
			
			try {
				struct[i] = _database.getConn(uid).createStruct(this.getPros().getProperty("TYPE_STRUCT").trim(), consulta.getArray());
			} catch (Exception e) {
				logger.error("Error creando struct ", e);
			}			
			i++;
		}
		
		try {
			array = ((OracleConnection) _database.getConn(uid))
					.createOracleArray(this.getPros().getProperty("TYPE_ARRAY").trim(), struct);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error("Error creando array ", e);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(this.getPros().getProperty("callRegistrar").trim());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			input.add(tipoCargue);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output,
					input,uid);
			if (cs != null) {
				logger.info("Call : " + this.getPros().getProperty("callRegistrar").trim() + " - P_EXITO : "
						+ cs.getInt(3) + " - P_ERROR : " + cs.getString(4));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrar").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callRegistrar").trim() + " : " + e.getMessage(),
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
	}

	private List<ConsultaUsuariosOIM> procesarArchivo(String fileNameConsultaUsuariosCopy) {
		logger.info("READ FILE UTF8..");
		List<ConsultaUsuariosOIM> lines = new ArrayList<ConsultaUsuariosOIM>();
		File f = null;
		BufferedReader b = null;
		try {
			f = new File(fileNameConsultaUsuariosCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {

				if (!line.trim().equals("")) {

					try {
						ConsultaUsuariosOIM consulta = new ConsultaUsuariosOIM();
						String dataUsuario[]=line.split("\\|",-1);
						
						SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyyMMdd");
					    sdfrmt.setLenient(false);
					    
						consulta.setUsuario(dataUsuario[0].trim());
						consulta.setNombre(dataUsuario[1].trim());
						consulta.setMarcaEspecial(dataUsuario[2].trim());
						consulta.setPerfil(dataUsuario[3].trim());
						consulta.setEmail(dataUsuario[4].trim());						
						consulta.setEstado(dataUsuario[5].trim());
						consulta.setSesiones(Long.parseLong(dataUsuario[6].trim()));
						consulta.setCodEntidad(Long.parseLong(dataUsuario[7].trim()));						
						consulta.setNumIdentificacion(dataUsuario[8].trim());
						consulta.setArea(dataUsuario[9].trim());						
						consulta.setTelefono(dataUsuario[10].trim());
						consulta.setCodOficina(Long.parseLong(dataUsuario[11].trim()));
						consulta.setEntidad(dataUsuario[12].trim());
						consulta.setCreador(dataUsuario[13].trim());
						
						 Date javaDate = ("00000000".equals(dataUsuario[14].trim())?null:sdfrmt.parse(dataUsuario[14].trim()));
						    java.sql.Date date=null;
						    if(javaDate!=null)
						    {
						    	date= new java.sql.Date(javaDate.getTime());						
						    }											
						consulta.setFechaCreacion(date);
						
						
						  javaDate = ("00000000".equals(dataUsuario[15].trim())?null:sdfrmt.parse(dataUsuario[15].trim()));
						    java.sql.Date date2=null;
						    if(javaDate!=null)
						    {
						    	date2= new java.sql.Date(javaDate.getTime());						
						    }											
						consulta.setFechaUltimoAcceso(date2);						

						  javaDate = ("00000000".equals(dataUsuario[16].trim())?null:sdfrmt.parse(dataUsuario[16].trim()));
						    java.sql.Date date3=null;
						    if(javaDate!=null)
						    {
						    	date3= new java.sql.Date(javaDate.getTime());						
						    }											
						consulta.setFechaExpiracionPassword(date3);	
						consulta.setObservaciones(dataUsuario[17].trim());					
						lines.add(consulta);
						logger.error("Linea " + line +"---Objeto---"+ consulta.toString());
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
					}

				}
			}

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

			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return lines;

	}

}
