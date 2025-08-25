package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.ArchivoResReaperturaCreditoCierreObl;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;


public class ResReaperturaCreditoCierreObl extends GenericProccess{
	private Logger logger = Logger.getLogger(ResReaperturaCreditoCierreObl.class);
	
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
	
	@Override
	public void process() {
		logger.info(".............. Iniciando proceso Respuesta Reapertura credito cierre obligaciones.................. ");
		    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
                if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		try {
			// carpeta de procesos
			String path_process = this.getPros().getProperty("fileProccess");
			String path_processBSCS = this.getPros().getProperty("fileProccessBSCS");
			// carpeta de error
			String path_error = this.getPros().getProperty("fileError");

			// carpeta donde_se_guardan_archivos proceso de ascard
			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim());
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_processBSCS);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_error);
				

			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}
			logger.info("................Buscando Archivos de respuesta de ereapertura credito cierre de obligaciones .................. ");
			// Se buscan Archivos de respuesta reapertura credito
			List<File> fileProcessList = null;
			try {

				fileProcessList = FileUtil.findFileNameFormPattern(this.getPros().getProperty("path"),
						this.getPros().getProperty("ExtfileResReapCred"));
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendos Archivos de respuesta de reapertura credito de cierre de obligaciones del directorio " + e.getMessage());
			}
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileResReaperturaCred : fileProcessList) {
					// Se verifica si la pareja de archivos existen
					if (fileResReaperturaCred != null) {
						logger.info("............Procesando respuesta reapertura credito cierre de obligaciones.........");

						String fileNameResReaperturaCred = fileResReaperturaCred.getName();
						String fileNameResReaperturaCredFullPath = this.getPros().getProperty("path").trim()
								+ fileNameResReaperturaCred;
						// Se mueve los archivos carpeta de procesos para
						// empezar el
						// flujo
						String fileNameResReaperturaCredProcess = this.getPros().getProperty("path").trim()+path_process
								+ fileNameResReaperturaCred;
						try {
							if (!FileUtil.fileExist(fileNameResReaperturaCredProcess)) {
								if ((FileUtil.copy(fileNameResReaperturaCredFullPath, fileNameResReaperturaCredProcess))) {
									
									this.getPgpUtil()
									.setPathInputfile(fileNameResReaperturaCredProcess);
									String fileOuput = this.getPros()
											.getProperty("path").trim()
											+ path_process
											+ renameFile(fileNameResReaperturaCred);									
									if (!FileUtil.fileExist(fileOuput)) {
										FileUtil.copy(fileNameResReaperturaCredProcess, fileOuput);
									}
									this.getPgpUtil().setPathOutputfile(fileOuput);
									// Se verifica si se desencripta archivo
									try {
										this.getPgpUtil().decript();
										logger.info(" Inicia procesado ded archivo");
										procesarArchivo(renameFile(fileOuput), path_error,uid);
										logger.info(" EL ARCHIVO SE HA PROCESADO");
										copyControlRecaudo(renameFile(fileNameResReaperturaCred), fileOuput,uid);
										
										logger.info(" ELIMINADO ARCHIVO ARCHIVO ENCRIPTADO");
										FileUtil.delete(fileNameResReaperturaCredProcess);
										String observacion = "Se ha procesado el archivo correctamente";
										Boolean res = registrar_auditoriaV2(
												fileNameResReaperturaCred,
												observacion,uid);
										logger.info("Auditoria res: " + res);
										String fileOuputBSCS = this.getPros()
												.getProperty("path").trim()
												+ path_processBSCS
												+ renameFile(fileNameResReaperturaCred);
										FileUtil.copy(fileOuput, fileOuputBSCS);

									} catch (PGPException ex) {
										logger.error(
												"Error desencriptando archivo: ",
												ex);
										// Se genera error con archivo se guarda en la auditoria

										String obervacion = ex.getMessage();
										registrar_auditoriaV2(fileNameResReaperturaCredProcess, obervacion,uid);
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
							registrar_auditoriaV2(fileNameResReaperturaCred, observacion,uid);
						} catch (Exception ex) {
							logger.error(" ERRROR GENERAL EN PROCESO.. : " + ex.getMessage(), ex);
							String observacion = "Error copiando archivos de para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameResReaperturaCred, observacion,uid);
						}
						logger.info(" ELIMINANDO ARCHIVO ");
						FileUtil.delete(fileNameResReaperturaCredFullPath);
						//FileUtil.delete(fileNameRegistrarBeneficioFinanciacionProcess);
					} 
				}
			} else {
				logger.error("NO SE ENCONTRARON LOS ARCHIVOS DE RESPUESTA REAPERTURA DE CREDITO CIERRE DE OBLIGACIONES");
			}
		} catch (

		Exception e) {
			logger.error("Excepcion no Controlada  en proceso de respuesta reapertura credito cierre obl " + e.getMessage(), e);
		}

	}
	
	private Boolean  reaperturaCreditoBD (Long numeroCredito,String uid) {
		Database _database = null;
		Boolean res = false;

		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			cs=_database.getConn(uid).prepareCall(this.getPros().getProperty("callReaperturaCredito").trim());
			cs.setLong(1, numeroCredito);
			cs.registerOutParameter(2, java.sql.Types.VARCHAR);
			cs.execute();
			String respuesta = cs.getString(2);	
			if (!respuesta.toUpperCase().trim().equals("TRUE")) {
				logger.info("Call: " + this.getPros().getProperty("callReaperturaCredito").trim() + " - P_EXITO : "
						+ cs.getString(2));
				res=false;
			}
			else{
				res= true;
				}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callReaperturaCredito").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callReaperturaCredito").trim() + " : " + e.getMessage(),
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
		return res;
	}
	
	private void procesarArchivo(String fileNameResReaperturaCredCopy, String path_error,String uid) {
		logger.info("READ FILE UTF8..");
		List<String> lines = new ArrayList<String>();
		ArchivoResReaperturaCreditoCierreObl resReapCred = new ArchivoResReaperturaCreditoCierreObl();
		Boolean Status;
		Boolean StatusSaldoFavor;
		File f = null;
		BufferedReader b = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			f = new File(fileNameResReaperturaCredCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			while ((line = b.readLine()) != null) {
			
				String[] value_split = new String[3];
				value_split[0]=line.substring(0, 8);
				value_split[1]=line.substring(8, 24);
				value_split[2]=line.substring(24, 34);	
				if (!line.trim().equals("")) {
					try {
						resReapCred.setNumeroCredito(Long.parseLong(value_split[1].trim()));
						resReapCred.setFecha(value_split[0].trim());
						resReapCred.setUsuario(value_split[2].trim());
						
						logger.info("Se elimina de cierre de obligaciones inicio" );
						Status = reaperturaCreditoBD(resReapCred.getNumeroCredito(),uid);
						logger.info("Se elimina de cierre de obligaciones fin " + Status );

						if (Status==false) {
							lines.add(line);
							logger.error("Linea " + line +"---Objeto---"+ resReapCred.toString()+"NO SE PUDO ELIMINAR");
						}
						//ELIMINAR DE TABLA INTEGTEST.INT_CREDITOS_SALDO_FAVOR_p
						
						logger.info("Se elimina de saldo a favor inicio" );
						StatusSaldoFavor = EliminaSaldoFavor(resReapCred.getNumeroCredito(),uid);
						logger.info("Se elimina de saldo a favor fin " + StatusSaldoFavor);

						if (StatusSaldoFavor==false) {
							lines.add(line);
							logger.error("Linea " + line +"---Objeto---"+ resReapCred.toString()+"NO SE PUDO ELIMINAR SALDO A FAVOR");
						}
						
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
						lines.add(line);
					}
				}
			}
			if(lines.size()>0) {
				for (int i=0; i<lines.size(); i++) {
					fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
					bw = new BufferedWriter(fw);
					bw.write(lines.get(i));
					bw.newLine();
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
				if(bw!=null)
					bw.close();	
				if(fw!=null)
					fw.close();
			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}

	}
	
	private Boolean  EliminaSaldoFavor (Long numeroCredito,String uid) {
		Database _database = null;
		Boolean res = false;

		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion EliminaSaldoFavor"
							+ ex.getMessage(), ex);
		}
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			cs=_database.getConn(uid).prepareCall(this.getPros().getProperty("callEliminarCreditoSaldoFavor").trim());
			cs.setLong(1, numeroCredito);
			cs.registerOutParameter(2, java.sql.Types.VARCHAR);
			cs.execute();
			String respuesta = cs.getString(2);	
			if (!respuesta.toUpperCase().trim().equals("TRUE")) {
				logger.info("Call: " + this.getPros().getProperty("callEliminarCreditoSaldoFavor").trim() + " - P_EXITO : "
						+ cs.getString(2));
				res=false;
			}
			else{
				res= true;
				}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callEliminarCreditoSaldoFavor").trim() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + this.getPros().getProperty("callEliminarCreditoSaldoFavor").trim() + " : " + e.getMessage(),
					e);
		} finally {
			if (cs != null) {
				try {
					cs.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando CallebaleStament BSCS eliminar credito saldo favor"
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return res;
	}
}
