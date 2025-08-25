/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.claro.financialintegrator.implement;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileFInanciacion;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAjustesAscard;
import co.com.claro.FileUtilAPI.TemplateFormatosFinanciero;
import co.com.claro.FileUtilAPI.TemplateRecaudoBancosConsolidado;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.ConsultaEntidadFinanciacion;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.ConsultaEntidadFinanciacionInterface;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.InputParameters;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.MensajeType;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.ObjectFactory;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.WSResult;

import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FinancialIntegratorException.WebServicesException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.WebServicesAPI.App;
import co.com.claro.WebServicesAPI.ConsultaEntidadFinanciacionConsuming;
import co.com.claro.WebServicesAPI.InsertarRechazosFranquiciasConsuming;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

/**
 * IF50: Procesa los archivos de bancos y franquicias que se generar en el día y
 * genera un formato unico de BANCOS que es enviado a ASCARD
 * 
 * @author freddylemus
 */
public class RecaudoBancos extends GenericProccess {

	private Logger logger = Logger.getLogger(RecaudoBancos.class);
	public static Map<String, MensajeType> mapEntidadFinanciacion = new HashMap();

	/**
	 * Se genera nombre del archivo
	 * 
	 * @return
	 */
	public String nameFile() {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha")
					.trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtText").trim();
			String prefix = this.getPros().getProperty("fileOutputPrefix")
					.trim();	
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archico " + ex.getMessage(), ex);
			;
			return null;
		}

	}

	/**
	 * agrega extención pgp a archivo
	 * 
	 * @param name
	 * @return
	 */
	public String nameFile(String name) {
		String extName = this.getPros().getProperty("fileOutputExtPGP").trim();
		return (name + extName);
	}

	/**
	 * Se quita la maskara del archivo para poder buscar el formato
	 * 
	 * @param fileName
	 *            Nombre del archivo
	 * @return
	 */
	public String[] replaceMask(String fileName) {
		String format = fileName.substring(fileName.length() - 4,
				fileName.length());
		String[] file = new String[2];
		String nameFile = "";
		// Obtiene el nombre del archivo si hora mintus y segundos
		if (fileName.contains("ASCARD") || fileName.contains("ascard")) {
			Integer indexAscard = fileName.indexOf("ascard");
			nameFile = fileName.substring(0, indexAscard + 6);
			file[0] = (nameFile + format);
			file[1] = null;
		} else {

			if (fileName.toUpperCase().contains("FRAGATA")) {
				nameFile = fileName.substring(0, fileName.length() - 12);
				file[0] = (nameFile + format);
				file[1] = null;
			} else {
				nameFile = fileName.substring(0, fileName.length() - 12);
				file[0] = (nameFile + format);
				file[1] = fileName.substring(fileName.length() - 12,
						(fileName.length() - 12) + 2);
			}
		}
		return file;
	}

	/**
	 * En mascar la tarejta
	 * 
	 * @param p_NUMERO_TARJETA
	 * @return
	 */
	private String maskTarjetaCredito(String p_NUMERO_TARJETA) {
		Integer lenght = p_NUMERO_TARJETA.length();
		String tarCor = p_NUMERO_TARJETA.substring(
				p_NUMERO_TARJETA.length() - 4, p_NUMERO_TARJETA.length());
		String complement = ObjectUtils.complement("", "X", lenght - 4, true);
		return (complement + tarCor);
	}

	/**
	 * Se inserta archivo de franquicias
	 * 
	 * @param line
	 *            linea recahzada
	 */
	public void insertarRechazosFranquicias(FileOuput line, String franquicia,
			String codRespuesta) {
		try {
			String p_NUMERO_TARJETA = this.maskTarjetaCredito(line.getType(
					TemplateRecaudoBancosConsolidado.TARCLIENTE)
					.getValueString());
			// Fecha transaccion
			String s_FECHA_TRANSACCION = line.getType(
					TemplateRecaudoBancosConsolidado.FECCOP).getValueString();
			Calendar p_FECHA_TRANSACCION = DateUtils.convertToCalendar(
					s_FECHA_TRANSACCION, "yyyyMMdd");
			// Codigo Respuesta
			String p_CODIGO_RESPUESTA = codRespuesta;
			// Valores transaccion
			String s_VALOR_TRANSACCION = line.getType(
					TemplateRecaudoBancosConsolidado.VALTRA).getValueString();
			String s_VALOR_IVA = line.getType(
					TemplateRecaudoBancosConsolidado.VALIVA).getValueString();
			BigDecimal p_VALOR_TRANSACCION = NumberUtils
					.convertStringTOBigDecimal(s_VALOR_TRANSACCION);
			BigDecimal p_VALOR_IVA = NumberUtils
					.convertStringTOBigDecimal(s_VALOR_IVA);
			//
			String p_REFERENCIA_PAGO = line.getType(
					TemplateRecaudoBancosConsolidado.NUMTAR).getValueString();
			p_REFERENCIA_PAGO = String.valueOf(Long
					.parseLong(p_REFERENCIA_PAGO));
			String p_FRANQUICIA = franquicia;
			logger.info("p_NUMERO_TARJETA : " + p_NUMERO_TARJETA);
			logger.info("s_FECHA_TRANSACCION : " + p_FECHA_TRANSACCION);
			logger.info("p_CODIGO_RESPUESTA : " + p_CODIGO_RESPUESTA);
			logger.info("p_VALOR_TRANSACCION : " + p_VALOR_TRANSACCION);
			logger.info("p_VALOR_IVA : " + p_VALOR_IVA);
			logger.info("p_REFERENCIA_PAGO : " + p_REFERENCIA_PAGO);
			logger.info("p_FRANQUICIA : " + p_FRANQUICIA);

			String addresPoint = this.getPros()
					.getProperty("WSInsertarRechazosFranquicias").trim();
			logger.info("addresPoint = " + addresPoint);
			String timeOut = this.getPros()
					.getProperty("WSLInsertarRechazosFranquiciasTimeOut")
					.trim();
			InsertarRechazosFranquiciasConsuming _insertar = new InsertarRechazosFranquiciasConsuming(
					addresPoint, timeOut);
			_insertar.InsertarRechazosFranquicias(p_NUMERO_TARJETA,
					p_FECHA_TRANSACCION, p_CODIGO_RESPUESTA,
					p_VALOR_TRANSACCION, p_VALOR_IVA, p_REFERENCIA_PAGO,
					p_FRANQUICIA);
		} catch (FinancialIntegratorException e) {
			logger.error("Error guardando pago rechazado " + e.getMessage(), e);
			e.printStackTrace();
		} catch (WebServicesException e) {
			logger.error("Error guardando pago rechazado " + e.getMessage(), e);
			e.printStackTrace();
		}
	}

	/**
	 * Metodo que estandariza los archivos de recaudos para poder ser
	 * unificados, segun la caracteristicas de cada banco o franquicia
	 * 
	 * @param entidadType
	 * @param path_file
	 * @return
	 */
	public FileFInanciacion procesarArchivo(MensajeType entidadType,
			String path_file) {
		logger.info("procesarArchivo tipoFormato=" + entidadType.getFORMATO()
				+ ", codMotivoPagoAscard="
				+ entidadType.getMOTIVOPAGOASCARD() + ", path_file="
				+ path_file);
		FileConfiguration fileConfig = null;
		try {
			/**
			 * Se verifica el formato del archivo para obtener el template a
			 * utilizar
			 */
			if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.ASOBANCARIA_98)) {
				fileConfig = TemplateFormatosFinanciero
						.config_Asobancaria_98(path_file);
			} else if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.ASOBANCARIA_98_COLP)) {
				fileConfig = TemplateFormatosFinanciero
						.config_Asobancaria_98_colp(path_file);
			} else if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.ASOBANCARIA_2001)) {
				fileConfig = TemplateFormatosFinanciero
						.config_Asobancaria_2001(path_file);
			} else if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.BANCOLOMBIA_FACTURANET)) {
				fileConfig = TemplateFormatosFinanciero
						.Bancolombia_Facturanet(path_file);
			} else if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.VISA_CREDENCIAL_DINERS)) {
				fileConfig = TemplateFormatosFinanciero
						.VisaCredencialDiners(path_file);
			} else if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.TC_MASTER)) {
				fileConfig = TemplateFormatosFinanciero
						.Mastercard_Comcel(path_file);
			} else if (entidadType
					.getFORMATO()
					.equals(TemplateRecaudoBancosConsolidado.TC_FRAGATA_ENVIO_RESPUESTA)) {
				fileConfig = TemplateFormatosFinanciero
						.Franquicias_Fragata(path_file);
			} else {
				logger.error("** Tipo de formato No soportado: "
						+ entidadType.getFORMATO());
				return null;
			}

			FileFInanciacion fileFInanciacion = null;
			FileFInanciacion fileFInanciacionResult = new FileFInanciacion();
			fileFInanciacionResult.setFileBody(new ArrayList<FileOuput>());
			/*
			 * Si el archivo es facturanet : se lee el archivo de formato
			 * BANCOLOMBIA
			 */
			if (TemplateRecaudoBancosConsolidado.BANCOLOMBIA_FACTURANET
					.equals(entidadType.getFORMATO())) {
				fileFInanciacion = FileUtil
						.readFileFinanciacionFacnet(fileConfig);
			} else {
				// se lee archivo de financiación.
				fileFInanciacion = FileUtil.readFileFinanciacion(fileConfig);
			}
			logger.info("Formato : " + entidadType.getFORMATO());
			// Se verifica si el formato encontrado, se encuentra configurado
			// para procesar
			if (entidadType.getFORMATO().equals(
					TemplateRecaudoBancosConsolidado.ASOBANCARIA_98)
					|| entidadType
							.getFORMATO()
							.equals(TemplateRecaudoBancosConsolidado.ASOBANCARIA_98_COLP)
					|| entidadType.getFORMATO().equals(
							TemplateRecaudoBancosConsolidado.ASOBANCARIA_2001)
					|| entidadType
							.getFORMATO()
							.equals(TemplateRecaudoBancosConsolidado.BANCOLOMBIA_FACTURANET)
					|| entidadType
							.getFORMATO()
							.equals(TemplateRecaudoBancosConsolidado.TC_FRAGATA_ENVIO_RESPUESTA)
					|| entidadType
							.getFORMATO()
							.equals(TemplateRecaudoBancosConsolidado.VISA_CREDENCIAL_DINERS)
					|| entidadType.getFORMATO().equals(
							TemplateRecaudoBancosConsolidado.TC_MASTER)) {
				logger.info("Procesando archivos (Header Size) :"
						+ fileFInanciacion.getFileHeader().size());
				if (fileFInanciacion.getFileHeader() == null
						|| fileFInanciacion.getFileHeader().size() == 0) {
					List<FileOuput> FileOuputListHeader = new ArrayList<FileOuput>();
					FileOuput file = new FileOuput();
					FileOuputListHeader.add(file);
					fileFInanciacion.setFileHeader(FileOuputListHeader);
				}
				for (FileOuput lineHeader : fileFInanciacion.getFileHeader()) {
					logger.info("Procesando archivos (Body size) :"
							+ fileFInanciacion.getFileBody().size());
					for (FileOuput line : fileFInanciacion.getFileBody()) {
						// si el archivo tiene cabecera, se obtienen datos y se
						// agregan al body.
						if (lineHeader.getTypes() != null) {
							//logger.info("Procensando Header");
							if (lineHeader
									.isExists(TemplateRecaudoBancosConsolidado.FECCOP)) {
								line.getTypes()
										.add(lineHeader
												.getType(TemplateRecaudoBancosConsolidado.FECCOP));
							}
							if (lineHeader
									.isExists(TemplateRecaudoBancosConsolidado.OFCCAP)) {
								line.getTypes()
										.add(lineHeader
												.getType(TemplateRecaudoBancosConsolidado.OFCCAP));
							}
							if (lineHeader
									.isExists(TemplateRecaudoBancosConsolidado.FECCON)) {
								line.getTypes()
										.add(lineHeader
												.getType(TemplateRecaudoBancosConsolidado.FECCON));
							}
						}
						// Sila entidad es fragata se agregan datos que no se
						// envian
						// OFCCAP , FECCON
						if (entidadType
								.getFORMATO()
								.equals(TemplateRecaudoBancosConsolidado.TC_FRAGATA_ENVIO_RESPUESTA)) {
							// Se crea dato de codigo de oficia
							Type type = new Type();
							type.setLength(4);
							type.setSeparator("");
							type.setName(TemplateRecaudoBancosConsolidado.OFCCAP);
							type.setTypeData(new ObjectType(String.class
									.getName(), ""));
							type.setPosicion(1);
							type.setValueString("00FR"); // Default
							type.setComplement(true);
							type.setLeftOrientation(true);
							type.setStringcomplement("");
							line.getTypes().add(type);

							type = new Type();
							type.setLength(4);
							type.setSeparator("");
							type.setName(TemplateRecaudoBancosConsolidado.FECCON);
							type.setTypeData(new ObjectType(String.class
									.getName(), ""));
							type.setValueString(DateUtils
									.getDateToDay_yyyyMMdd());
							line.getTypes().add(type);

							type = new Type();
							type.setLength(6);
							type.setSeparator("");
							type.setName(TemplateRecaudoBancosConsolidado.CODAUT);
							type.setTypeData(new ObjectType(String.class
									.getName(), ""));
							type.setValueString("000000");
							line.getTypes().add(type);

						}
						// Sila entidad es VISA_CREDENCIAL_DINERS se agregan y
						// MODIFICAN datos
						if (entidadType
								.getFORMATO()
								.equals(TemplateRecaudoBancosConsolidado.VISA_CREDENCIAL_DINERS)) {
							logger.info("Codigo Autorizacion VISA: "
									+ line.getType(
											TemplateRecaudoBancosConsolidado.CODAUT)
											.getValueString());

							String codAut = String
									.valueOf(
											line.getType(
													TemplateRecaudoBancosConsolidado.CODAUT)
													.getValueString()).trim();

							line.getType(
									TemplateRecaudoBancosConsolidado.CODAUT)
									.setValueString(codAut);
							Type type = new Type();
							type.setLength(4);
							type.setSeparator("");
							type.setName(TemplateRecaudoBancosConsolidado.VALTOT);
							type.setTypeData(new ObjectType(String.class
									.getName(), ""));
							type.setValueString(line.getType(
									TemplateRecaudoBancosConsolidado.VALTRA)
									.getValueString());
							line.getType(
									TemplateRecaudoBancosConsolidado.VALIVA)
									.setValueString("00000000000000000");
						}
						// Sila entidad es MASTER-TC se agregan y MODIFICAN
						// datos
						if (entidadType.getFORMATO().equals(
								TemplateRecaudoBancosConsolidado.TC_MASTER)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.VALIVA)
									.setValueString("00000000000000000");
							Type type = new Type();
							type.setLength(4);
							type.setSeparator("");
							type.setName(TemplateRecaudoBancosConsolidado.OFCCAP);
							type.setTypeData(new ObjectType(String.class
									.getName(), ""));
							type.setPosicion(1);
							type.setValueString("051"); // Default
							type.setComplement(true);
							type.setLeftOrientation(true);
							type.setStringcomplement("");
							line.getTypes().add(type);

							String codAut = String
									.valueOf(
											line.getType(
													TemplateRecaudoBancosConsolidado.CODAUT)
													.getValueString()).trim();
							codAut = codAut.substring(codAut.length() - 6,
									codAut.length());
							logger.info(" CDOUT ... MDF : " + codAut);
							line.getType(
									TemplateRecaudoBancosConsolidado.CODAUT)
									.setValueString(codAut);
						}
						// si la configuración tiene separador se le quita
						line.getType(TemplateRecaudoBancosConsolidado.NUMTAR)
								.setSeparator("");
						line.getType(TemplateRecaudoBancosConsolidado.VALTRA)
								.setSeparator("");
						if (line.isExists(TemplateRecaudoBancosConsolidado.VALIVA)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.VALIVA)
									.setSeparator("");
						}
						if (line.isExists(TemplateRecaudoBancosConsolidado.FECCON)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.FECCON)
									.setSeparator("");
						}
						if (line.isExists(TemplateRecaudoBancosConsolidado.FECCOP)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.FECCOP)
									.setSeparator("");
						}
						if (line.isExists(TemplateRecaudoBancosConsolidado.OFCCAP)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.OFCCAP)
									.setSeparator("");
						}
						if (line.isExists(TemplateRecaudoBancosConsolidado.CODAUT)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.CODAUT)
									.setSeparator("");
							//
						}

						String valorTRans = String.valueOf(line.getType(
								TemplateRecaudoBancosConsolidado.VALTRA)
								.getValueString());
						// Se valida que el formato no sea ASOBANCARIA_2001 o
						// BANCOLOMBIA_FACTURANET para no agregar decimales
						if (!entidadType
								.getFORMATO()
								.equals(TemplateRecaudoBancosConsolidado.ASOBANCARIA_2001)
								&& !entidadType
										.getFORMATO()
										.equals(TemplateRecaudoBancosConsolidado.BANCOLOMBIA_FACTURANET)
								&& !entidadType
										.getFORMATO()
										.equals(TemplateRecaudoBancosConsolidado.VISA_CREDENCIAL_DINERS)) {
							valorTRans = String.valueOf(line.getType(
									TemplateRecaudoBancosConsolidado.VALTRA)
									.getValueString()
									+ "00");
						}
						if (entidadType
								.getFORMATO()
								.equals(TemplateRecaudoBancosConsolidado.BANCOLOMBIA_FACTURANET)) {
							String numTra = String.valueOf(line.getType(
									TemplateRecaudoBancosConsolidado.NUMTAR)
									.getValueString());
							logger.info("Numero de transaccion: " + numTra);
							numTra = numTra.substring(0, 19);
							numTra = numTra.trim();
							numTra = ObjectUtils.complement(numTra, "0", 19,
									true);
							logger.info("Numero de transaccion: " + numTra);
							line.getType(
									TemplateRecaudoBancosConsolidado.NUMTAR)
									.setValueString(numTra);
							line.getType(
									TemplateRecaudoBancosConsolidado.NUMTAR)
									.setComplement(false);
							line.getType(
									TemplateRecaudoBancosConsolidado.NUMTAR)
									.setLeftOrientation(true);
							line.getType(
									TemplateRecaudoBancosConsolidado.NUMTAR)
									.setLength(19);

						}

						// si existe el valor total se agrega , si no se agrega
						// por el valor normal
						if (line.isExists(TemplateRecaudoBancosConsolidado.VALTOT)) {
							line.getType(
									TemplateRecaudoBancosConsolidado.VALTOT)
									.setValueString(
											ObjectUtils.complement(valorTRans,
													"0", 17, true));
							line.getType(
									TemplateRecaudoBancosConsolidado.VALTOT)
									.setComplement(false);
						} else {

							Type type = new Type();
							type.setLength(1);
							type.setSeparator("");
							type.setName(TemplateRecaudoBancosConsolidado.VALTOT);
							type.setTypeData(new ObjectType(String.class
									.getName(), ""));
							type.setPosicion(1);
							type.setValueString(ObjectUtils.complement(
									valorTRans, "0", 17, true)); // Default
							type.setComplement(false);
							type.setLeftOrientation(true);
							type.setStringcomplement("0");
							line.getTypes().add(type);
						}

						line.getType(TemplateRecaudoBancosConsolidado.VALTRA)
								.setValueString(
										ObjectUtils.complement(valorTRans, "0",
												17, true));
						line.getType(TemplateRecaudoBancosConsolidado.VALTRA)
								.setComplement(false);

						if (!line
								.isExists(TemplateRecaudoBancosConsolidado.CODMTV)) {
							line.getTypes().add(
									TemplateFormatosFinanciero
											.getTypeCODMTV(entidadType
													.getMOTIVOPAGOASCARD()
													.toString()));
						}

						if (entidadType.getTIPO().getValue().endsWith(
								TemplateFormatosFinanciero.TIPO_TC)) {

						}
						// Se realiza proceso de depuración, cuando es enviado
						// el campo estado
						if (line.isExists(TemplateRecaudoBancosConsolidado.ESTADO)) {
							// SE PREGUNTA POR FRANQUICIA
							if (entidadType
									.getFORMATO()
									.equals(TemplateRecaudoBancosConsolidado.VISA_CREDENCIAL_DINERS)) {

								if (line.getType(
										TemplateRecaudoBancosConsolidado.ESTADO)
										.getValueString()
										.trim()
										.equals(TemplateRecaudoBancosConsolidado.ESTADO_VISA_CREDENCIA_DINNERS)) {
									fileFInanciacionResult.getFileBody().add(
											line);
								} else {
									// Se guarda rechazo de franquicias visa
									this.insertarRechazosFranquicias(
											line,
											line.getType(
													TemplateRecaudoBancosConsolidado.OFCCAP)
													.getValueString(),
											line.getType(
													TemplateRecaudoBancosConsolidado.CODRES)
													.getValueString());
								}

							}
							if (entidadType.getFORMATO().equals(
									TemplateRecaudoBancosConsolidado.TC_MASTER)) {
								logger.info("ESTADO TC-MASTER: "
										+ line.getType(
												TemplateRecaudoBancosConsolidado.ESTADO)
												.getValueString());
								if (line.getType(
										TemplateRecaudoBancosConsolidado.ESTADO)
										.getValueString()
										.trim()
										.equals(TemplateRecaudoBancosConsolidado.ESTADO_TC_MASTER)) {
									fileFInanciacionResult.getFileBody().add(
											line);
								} else {
									String OFCAP = this.getPros()
											.getProperty("MASTER_OFCAP").trim();
									// Se guarda rechazo de franquicias visa
									this.insertarRechazosFranquicias(
											line,
											OFCAP,
											line.getType(
													TemplateRecaudoBancosConsolidado.ESTADO)
													.getValueString());
								}

							}
						} else {
							fileFInanciacionResult.getFileBody().add(line);
						}

					}

				}
			}

			return fileFInanciacionResult;
		} catch (FinancialIntegratorException ex) {
			logger.error(
					"Error leyendo archivos de Recaudo:  "
							+ fileConfig.getFileName() + " Ex: "
							+ ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(
					"Error leyendo archivos de Recaudo:  "
							+ fileConfig.getFileName() + " Ex: "
							+ ex.getMessage(), ex);
		}

		return null;
	}

	/**
	 * Mètodo que consulta la entidad dependiendo el archivo y el ciclo
	 * 
	 * @param wsdlAdress
	 * @param timeOut
	 * @param nombreArchivo
	 * @param ciclo
	 * @return
	 */
	public MensajeType consultEntidad(String wsdlAdress, String timeOut,
			String nombreArchivo, String ciclo) {
		try {
			
			URL url = new URL(wsdlAdress);
			ConsultaEntidadFinanciacion service = new ConsultaEntidadFinanciacion(url);
			ObjectFactory factory = new ObjectFactory();
			InputParameters input = factory.createInputParameters();
			
			input.setNOMBREARCHIVO(factory.createInputParametersNOMBREARCHIVO(nombreArchivo));
			input.setPCICLOSERVICIO(factory.createInputParametersPCICLOSERVICIO(ciclo));
			
			ConsultaEntidadFinanciacionInterface consulta = service.getConsultaEntidadFinanciacionPortBinding();
			BindingProvider bindingProvider = (BindingProvider) consulta;
			bindingProvider.getRequestContext().put("javax.xml.ws.client.connectionTimeout",
					Integer.valueOf(timeOut));
			bindingProvider.getRequestContext().put("javax.xml.ws.client.receiveTimeout", Integer.valueOf(timeOut));
			
			WSResult result = consulta.consultaEntidadFinanciacion(input);
					
			ConsultaEntidadFinanciacionConsuming consuming = new ConsultaEntidadFinanciacionConsuming(
					wsdlAdress, timeOut);

			if (result.getMENSAJE() != null) {
				return result.getMENSAJE();

			}
		} catch (MalformedURLException e) {
			logger.error("Error WebServicesException :  " + e.getMessage(), e);
		}
		return null;

	}

	/**
	 * Metodo que ejecuta el CRON de Recaudo.
	 */
	@Override
	public void process() {
		logger.info("................Iniciando proceso RecaudoBancos 5.1 \n\\n ");
                    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}		
		// Se genera el nombre del arhivo
		String fileNamePsnr = this.nameFile();
		if (fileNamePsnr != null) {
			try {

				String path_process = this.getPros()
						.getProperty("fileProccess");
				String path_process_history = this.getPros().getProperty(
						"fileHistory");
				String path_ascard_process = this.getPros().getProperty(
						"pathCopyFile");
				String path = this.getPros().getProperty("path").trim();
				String addresPoint = this.getPros()
						.getProperty("WSConsultaEntidadFinanciera").trim();
				logger.info("addresPoint = " + addresPoint);
				String timeOut = this.getPros()
						.getProperty("WSLConsultaEntidadFinancieraTimeOut")
						.trim();
				String filenameregex = this.getPros().getProperty(
						"file.filenameregex");
				logger.info("filenameregex = " + filenameregex);
				String file_extensionFile = this.getPros().getProperty(
						"fileOutputExtText");

				// Se crea carpeta para los archivos consolidados
				FileUtil.createDirectory(path + path_ascard_process);
				// se crea directorio de historicos
				FileUtil.createDirectory(path + path_process_history);

				String path_file = path + File.separator + path_process;
				String path_file_history = path + File.separator
						+ path_process_history;

				List<FileOuput> linesConsolidado = new ArrayList<FileOuput>();
				List<FileOuput> linesRechazados = new ArrayList<FileOuput>();

				
				String fileWritePath = path + File.separator + path_process+File.separator+ fileNamePsnr;
				
				// Lista de Archivos no procesados
				List<File> listFileNoProcess = new ArrayList<File>();
				// Lista de archivos procesados
				List<File> listFileProcess = new ArrayList<File>();
				listFileProcess = FileUtil.findListFileName(path_file,
						filenameregex);
				// Se itera por los archivos en que estan en el directorio y
				// procede
				// a unificarlos en archivo de recaudo
				logger.info("Archivos Encontrados: " + listFileProcess.size());
				for (File file : listFileProcess) {
					// Si archivo encontrado es diferente al UNIFICADO
					if (!file.getName().contains("PSRN")) {
						logger.info("Procesando archivo: " + file.getName());

						// Se consulta la entidad a la cual pertenece el archivo
						String nameFileprocess[] = this.replaceMask(file
								.getName());
						logger.info("Buscando archivo " + nameFileprocess[0]
								+ " : " + nameFileprocess[1]);
						MensajeType entidadType = consultEntidad(addresPoint,
								timeOut, nameFileprocess[0], nameFileprocess[1]);
						// Si el archivo pertenece a entidad se procesa
						if (entidadType != null) {
							String tipoFormato = entidadType.getFORMATO();
							// Se procesa archivo y se consolida

							FileFInanciacion fileFInanciacion = null;
							fileFInanciacion = procesarArchivo(entidadType,
									file.getAbsolutePath());

							if (fileFInanciacion != null) {
								try{
									Integer linesFiles =  fileFInanciacion
											.getFileBody().size();
									BigDecimal valor = new BigDecimal(0);
									for (FileOuput foutput : fileFInanciacion
											.getFileBody()){
										BigDecimal valorFormat = (BigDecimal) ObjectUtils.format(foutput.getType(TemplateRecaudoBancosConsolidado.VALTOT).getValueString(), BigDecimal.class.getName(), null,2);
										valor = valor.add( valorFormat);
									}
									//Se registra control archivo
									this.registrar_control_archivo(this.getPros().getProperty("BatchName", "").trim(), nameFileprocess[0], file.getName(), linesFiles.toString() ,valor,uid);
								}catch(Exception ex){
									logger.error("error contando lineas "+ex.getMessage(),ex);
								}
								linesConsolidado.addAll(fileFInanciacion
										.getFileBody());
							} else {
								// Archivo no se proceso
								listFileNoProcess.add(file);
							}

						} else {
							logger.info(" Archivo no encontrado en Base del integrador = "
									+ file.getName());
						}
					}
				}
				// Se realiza sumatorial y totales para cabecera de archivo
				int cantidadTx = linesConsolidado.size();
				// Si existen registro, se genera archivo de bancos diarios
				if (cantidadTx > 0) {
					double sumatoriaValores = 0;
					for (FileOuput fileOuput : linesConsolidado) {
						if (fileOuput
								.isExists(TemplateRecaudoBancosConsolidado.VALTOT)) {
							sumatoriaValores += Double
									.parseDouble(fileOuput
											.getType(
													TemplateRecaudoBancosConsolidado.VALTOT)
											.getValueString());
						}

					}

					FileOuput header = new FileOuput();
					header.setHeader(TemplateRecaudoBancosConsolidado
							.typesTemplateHeadar(cantidadTx, sumatoriaValores));
					logger.info("Validando Archivo existe: " + fileWritePath);
					// Se valida que archivo no exista para el día (Si ya existe
					// los
					// archivos se procesan
					// en el día siguiente
					String fileNamePsnrPgp = this.nameFile(fileNamePsnr);
					String fileWritePathPgp = path + File.separator
							+ path_ascard_process +File.separator+ fileNamePsnrPgp;					
					if (!FileUtil.fileExist(fileWritePath)
							&& !FileUtil.fileExist(fileWritePathPgp)) {
						linesConsolidado.add(header);

						// Se obtien el template para BANCOS
						List<Type> template = TemplateRecaudoBancosConsolidado
								.typesTemplateRecaudoBancosConsolidado();
						// Se crea archivo unificado
						FileUtil.createFileFormat(fileWritePath,
								linesConsolidado, new ArrayList<Type>(),
								template);
						//Se mueve archivo
						//copyControlRecaudo(fileNamePsnr,fileWritePath);
						// Cifrar el archivo
						this.getPgpUtil().setPathInputfile(fileWritePath);
						this.getPgpUtil().setPathOutputfile(fileWritePathPgp);
						this.getPgpUtil().encript();

						//FileUtil.move(fileWritePathPgp, fileNameFInal);
						// Se mueven archivos procesados en el día
						logger.info(" *** *** Proceso de eliminación de Archivo "
								+ listFileProcess.size());
						for (File file : listFileProcess) {
							if (!listFileNoProcess.contains(file)) {
								logger.info("proceso delete final para file= "
										+ file.getAbsolutePath());
								//copyControlRecaudo(file.getName(),file.getAbsolutePath());
								FileUtil.rename(path_file + File.separator
										+ file.getName(), path_file_history
										+ File.separator + file.getName());								
								FileUtil.delete(file.getAbsolutePath());
							} else {
								registrar_auditoria(file.getName(),
										"Archivo NO se proceso Correctamente",uid);
							}

						}
						//Se mueve archivo de proceso
						logger.info("Moviendo archivo de proceso "+fileWritePath+" To "+( path_file_history	+ File.separator + fileNamePsnr));						
						FileUtil.move(fileWritePath, path_file_history
								+ File.separator + fileNamePsnr);
						
						logger.info("proceso final");
						String obervacion = "Archivo Procesado Exitosamente";
						registrar_auditoria(fileWritePath, obervacion,uid);

					} else {

						logger.error("Los archivos ya existen, NO se sobreescribirÃ¡n  ");
					}

				} else {
					logger.info(" *** *** Proceso de eliminación de Archivo "
							+ listFileProcess.size());
					for (File file : listFileProcess) {
						if (!listFileNoProcess.contains(file)) {
							logger.info("proceso delete final para file= "
									+ file.getAbsolutePath());
							FileUtil.rename(
									path_file + File.separator + file.getName(),
									path_file_history + File.separator
											+ file.getName()
							/*
							 * + DateUtils .getCurrently_yyyyMMddHHmmss() +
							 * "_processed"
							 */);
							FileUtil.delete(file.getAbsolutePath());
						} else {
							registrar_auditoria(file.getName(),
									"Archivo NO se proceso Correctamente",uid);
						}

					}
					logger.info("No existen archivos para procesar");
				}
			} catch (FinancialIntegratorException e) {
				logger.error("Error  " + e.getMessage(), e);
				String obervacion = "Error  Copiando Archivos: "
						+ e.getMessage();
				registrar_auditoria(fileNamePsnr, obervacion,uid);
			} catch (Exception e) {
				logger.error("Exception " + e.getMessage(), e);
				String obervacion = "Error  Copiando Archivos: "
						+ e.getMessage();
				// registrar_auditoria(fileWritePath, obervacion,uid);
			} finally {
				logger.info("finally---------Finaliza proceso RecaudoBancos 5.1 \n\\n ");
			}
		} else {
			logger.error("error en proceso de banco , No se puede generar archivo de banco ");
		}
	}

}
