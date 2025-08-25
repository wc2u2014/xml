package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileUtil;

import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.domain.ArchivoRegistrarBeneficioFinanciacion;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.registrarBeneficioFinanciacion.InputParameters;
import co.com.claro.financingintegrator.registrarBeneficioFinanciacion.RegistrarBeneficioFinancicionCuotasGratisInterface;
import co.com.claro.financingintegrator.registrarBeneficioFinanciacion.RegistrarBeneficioFinancicionCuotasGratisPortBindingQSService;
import co.com.claro.financingintegrator.registrarBeneficioFinanciacion.WSResult;

public class RegistrarBeneficioFinanciacion extends GenericProccess{
	private Logger logger = Logger.getLogger(RegistrarBeneficioFinanciacion.class);
	
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
		logger.info(".............. Iniciando proceso RegistroBeneficioFinanciacion Dev.................. ");
		

		    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		String waitTime = this.getPros().getProperty("waitTimeProccess");
		int wait=Integer.parseInt(waitTime);
		Calendar fechaActual = Calendar.getInstance();
		fechaActual.add(Calendar.HOUR,-wait);
		try {
			// carpeta de proceso
			String path_process = this.getPros().getProperty("fileProccess");
			
			// carpeta de proceso
			String urlService = this.getPros().getProperty("urlService");
			
			// carpeta de error
			String path_error = this.getPros().getProperty("fileError");

			// carpeta donde_se_guardan_archivos proceso de ascard
			try {
				FileUtil.createDirectory(this.getPros().getProperty("path").trim());
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_process);
				FileUtil.createDirectory(this.getPros().getProperty("path").trim() + path_error);

			} catch (FinancialIntegratorException e) {
				logger.error("Error creando directorio para processar archivo de ASCARD " + e.getMessage());
			}
			logger.info("................Buscando Archivos de Registro de Beneficio de financiacion.................. ");
			// Se buscan Archivos de registroBeneficioFinanciacion y de control
			List<File> fileProcessList = null;
			// Se busca archivo de registroBeneficioFinanciacion
			try {

				fileProcessList = FileUtil.findFileNameFormPattern(this.getPros().getProperty("path"),
						this.getPros().getProperty("ExtfileRegistrarBeneficio"));
			} catch (FinancialIntegratorException e) {
				logger.error("Error leyendos Archivos de RegistrarBeneficioFinanciacion del directorio " + e.getMessage());
			}
			if (fileProcessList != null && !fileProcessList.isEmpty()) {
				for (File fileRegistrarBeneficioFinanciacion : fileProcessList) {
					// Se verifica si la pareja de archivos existen
					
					if (fileRegistrarBeneficioFinanciacion.lastModified() <=fechaActual.getTimeInMillis())	     
					{
					if (fileRegistrarBeneficioFinanciacion != null) {
						logger.info("............Procesando registrarBeneficioFinanciacion.........");

						String fileNameRegistrarBeneficioFinanciacion = fileRegistrarBeneficioFinanciacion.getName();
						String fileNameRegistrarBeneficioFinanciacionFullPath = this.getPros().getProperty("path").trim()
								+ fileNameRegistrarBeneficioFinanciacion;
						// Se mueve los archivos carpeta de procesos para
						// empezar el
						// flujo
						String fileNameRegistrarBeneficioFinanciacionProcess = this.getPros().getProperty("path").trim()+path_process
								+ fileNameRegistrarBeneficioFinanciacion;
						try {
							if (!FileUtil.fileExist(fileNameRegistrarBeneficioFinanciacionProcess)) {
								if ((FileUtil.copy(fileNameRegistrarBeneficioFinanciacionFullPath, fileNameRegistrarBeneficioFinanciacionProcess))) {
									String fileOuput = this.getPros()
											.getProperty("path").trim()
											+ path_process
											+ renameFile(fileNameRegistrarBeneficioFinanciacion);									
									if (!FileUtil.fileExist(fileOuput)) {
										FileUtil.copy(fileNameRegistrarBeneficioFinanciacionProcess, fileOuput);
									}
									procesarArchivo(fileOuput, path_error,urlService);									
									
										logger.info(" EL ARCHIVO SE HA PROCESADO");
										String observacion = "Se ha procesado el archivo correctamente";
										Boolean res = registrar_auditoriaV2(
												fileNameRegistrarBeneficioFinanciacion,
												observacion,uid);
										logger.info("Auditoria res: " + res + "*****************************************");

								} else {
									logger.error("ARCHIVO DE CONTROL ESTA VACIO..");
								}

							} else {
								logger.info(" ARCHIVOS DE ACTIVACIONES EXISTE NO SE PROCESA");
							}

						} catch (FinancialIntegratorException ex) {
							logger.error(" ERRROR COPIANDO ARCHIVOS PARA PROCESO : " + ex.getMessage());
							String observacion = "Error copiando archivos  para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameRegistrarBeneficioFinanciacion, observacion,uid);
						} catch (Exception ex) {
							logger.error(" ERRROR GENERAL EN PROCESO.. : " + ex.getMessage(), ex);
							String observacion = "Error copiando archivos de para el proceso " + ex.getMessage();
							registrar_auditoriaV2(fileNameRegistrarBeneficioFinanciacion, observacion,uid);
						}
						logger.info(" ELIMINANDO ARCHIVO ");
						FileUtil.delete(fileNameRegistrarBeneficioFinanciacionFullPath);
						//FileUtil.delete(fileNameRegistrarBeneficioFinanciacionProcess);
					} 
					
					}else
					 {
						logger.error("No ha pasado el tiempo para el procesamiento del Archivo"+fileRegistrarBeneficioFinanciacion.getName());
					}	
				}
			} else {
				logger.error("NO SE ENCONTRARON LOS ARCHIVOS DE CONTROL O REGISTRAR BENEFICIO FINANCIACION");
			}
		} catch (

		Exception e) {
			logger.error("Excepcion no Controlada  en proceso de Registro de beneficio de financiacion " + e.getMessage(), e);
		}

	}
	
	private Boolean  registrarBeneficio (ArchivoRegistrarBeneficioFinanciacion registro, String urlService) {
		
		//RegistrarBeneficioFinancicionCuotasGratisInterface servicio = new RegistrarBeneficioFinancicionCuotasGratisPortBindingQSService(urlService).getRegistrarBeneficioFinancicionCuotasGratisPortBindingQSPort();
		RegistrarBeneficioFinancicionCuotasGratisInterface servicio = new RegistrarBeneficioFinancicionCuotasGratisPortBindingQSService().getRegistrarBeneficioFinancicionCuotasGratisPortBindingQSPort();
		WSResult serviceRS;
			BigDecimal tipoIdentificacion = new BigDecimal(registro.getTipoIdentificacion());
			InputParameters inputParameters = new InputParameters();
			inputParameters.setOperacion(registro.getOperacion());
			inputParameters.setTipoIdentificacion(tipoIdentificacion);
			inputParameters.setNumeroIdentificacion(registro.getNumeroIdentificacion());
			inputParameters.setCuentaResponsablePago(registro.getCuentaResponsablePago());
			inputParameters.setImeiSerial(registro.getImeiSerial());
			inputParameters.setCodigoCampania(registro.getCodigoCampania());
			inputParameters.setFechaCorteFacturacion(registro.getFechaCorteFacturacion());
			try{
				serviceRS = servicio.registrarBeneficioFinancicionCuotasGratis(inputParameters);
				if (serviceRS.getCODIGO().equals("0000")) {
					logger.info("Error haciendo el registro");
					return false;
				}
			}catch (Throwable e) {
				System.out.println(e);
				e.printStackTrace();
				return false;
			}
			logger.info("Registro exitoso");
			return true;
	}
	
	private void procesarArchivo(String fileNameRegistrarBeneficioFinanciacionCopy, String path_error, String urlService) {
		logger.info("READ FILE UTF8..");
		List<String> lines = new ArrayList<String>();
		ArchivoRegistrarBeneficioFinanciacion regBenFin = new ArchivoRegistrarBeneficioFinanciacion();
		Boolean Status;
		File f = null;
		BufferedReader b = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		try {
			f = new File(fileNameRegistrarBeneficioFinanciacionCopy);
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			fw = new FileWriter(f.getParentFile().getParentFile()+path_error+"Error_"+f.getName(), true);
			bw = new BufferedWriter(fw);
			String line = "";
			while ((line = b.readLine()) != null) {
				
				String[] value_split = line.split("\\|");
				if (!line.trim().equals("")) {
					try {
						regBenFin.setOperacion("21");
						regBenFin.setTipoIdentificacion(value_split[0].trim());
						regBenFin.setNumeroIdentificacion(value_split[1].trim());
						regBenFin.setCuentaResponsablePago(value_split[2].trim());
						regBenFin.setImeiSerial(value_split[3].trim());
						regBenFin.setCodigoCampania(value_split[4].trim());
						String fecha = sdf.format(Calendar.getInstance().getTime());
						regBenFin.setFechaCorteFacturacion(fecha);
						Status = registrarBeneficio(regBenFin,urlService );
						if (!Status) {
							lines.add(line);
						}
						logger.info("Linea " + line +"---Objeto---"+ regBenFin.toString());
					} catch (Exception ex) {
						logger.error("Error leyendo linea " + line, ex);
						System.out.println("Error leyendo linea: " + line);
						lines.add(line);
					}
				}
			}
			if(lines.size() > 0) {
				for (int i=0; i<lines.size(); i++) {
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
}
