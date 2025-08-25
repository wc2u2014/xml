package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileFInanciacion;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateAjustesAscard;
import co.com.claro.FileUtilAPI.TemplateAnulacionPagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplateAnulacionSicacom;
import co.com.claro.FileUtilAPI.TemplateAperturaCredito;
import co.com.claro.FileUtilAPI.TemplateNovedadesNoMonetarias;
import co.com.claro.FileUtilAPI.TemplateDatosDemograficos;
import co.com.claro.FileUtilAPI.TemplatePagosAscard;
import co.com.claro.FileUtilAPI.TemplatePagosNoAbonados;
import co.com.claro.FileUtilAPI.TemplatePromocionAscard;
import co.com.claro.FileUtilAPI.TemplateRecaudoBancosConsolidado;
import co.com.claro.FileUtilAPI.TemplateRecaudoSicacom;
import co.com.claro.FileUtilAPI.TemplateRecaudosBancosRR;
import co.com.claro.FileUtilAPI.TemplateSalidaAjustesAscard;
import co.com.claro.FileUtilAPI.TemplateSalidaAnulacionBancos;
import co.com.claro.FileUtilAPI.TemplateSalidaAplicacionPnaAsc;
import co.com.claro.FileUtilAPI.TemplateTobePagos;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.conifguration.MetadataConf;
import co.com.claro.financialintegrator.conifguration.ProccesingAutomaticConf;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import co.com.claro.financingintegrator.consultaentidadfinanciacion.MensajeType;
import oracle.jdbc.OracleConnection;

public class ControlRecaudoUpgrade extends GenericProccess {

	private Logger logger = Logger.getLogger(ControlRecaudoUpgrade.class);

	/**
	 * validacion de archivo por tipo proceso
	 * 
	 * @param pathFile
	 * @param fileName
	 * @param TypeProcess
	 * @return
	 */
	private Boolean validateFile(String pathFile, String fileName,
			int TypeProcess) {
		switch (TypeProcess) {
		// Sicacom
		case 2:
			return this.validateFileSicacom(pathFile, fileName);
		default:
			return true;
		}
	}

	/**
	 * Valida la fecha de creacion del archivo sea del dia anterior
	 * 
	 * @param pathFile
	 * @return
	 */
	private boolean validDateFileSicacom(String pathFile, String fileName) {
		return true;
	}

	/**
	 * retorna si un archivo de sicacom es valido para procesar para el control
	 * recaudo
	 * 
	 * @param fileName
	 * @param pathAscard
	 * @return
	 */
	private Boolean validateFileSicacom(String pathFile, String fileName) {
		String path = this.getPros().getProperty("pathAscardSicacom");
		String filePGP = new RecaudosSICACOM().renameFile(fileName);
		// logger.info("Find File" + filePGP + " into path: " + path);
		Boolean fileExistAscard = FileUtil.findFileIntoPath(path, filePGP);
		if (fileExistAscard) {
			logger.debug(fileName
					+ " Arhivo no enviado a Ascard no se procesara ");
			return false;
		}
		Boolean fileDateValid = this.validDateFileSicacom(pathFile, fileName);
		if (!fileDateValid) {
			logger.info(fileName
					+ " Arhivo no valido por fecha de modificacion: ");
			;
			return false;
		}
		return true;
	}

	/**
	 * Configura archivos Sicacom
	 * 
	 * @param fileName
	 * @param path
	 * @param conf
	 * @return
	 */
	private FileConfiguration getConfigurationByFileSicacom(String fileName,
			String path, ProccesingAutomaticConf conf) {
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);

		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.CODIGOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(3);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.NOMBRECENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(4);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.VALTOT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(5);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.TIPOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(8);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("CICLO_SERVICIO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue("");
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(10);
		conf.getFields().add(mf);
		//
		conf.setNumFieldsHeader(0);
		return new TemplateRecaudoSicacom().configurationRecaudoSicacomUG(path);
	}
	
	/**
	 * Configura archivos Sicacom
	 * 
	 * @param fileName
	 * @param path
	 * @param conf
	 * @return
	 */
	private FileConfiguration getConfigurationByFileAnulacionSicacom(String fileName,
			String path, ProccesingAutomaticConf conf) {
		conf.setFields(new ArrayList<MetadataConf>());
		//
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.TIPOREGISTROBOD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(3);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.CODIGOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(4);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.NOMBRECENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(5);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.VALTOT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.TIPOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(8);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(10);
		conf.getFields().add(mf);
		//
		conf.setNumFieldsHeader(0);
		return new TemplateAnulacionSicacom().configurationAnulacionSicacomUG(path);
	}	

	/**
	 * Configura archivos Sicacom
	 * 
	 * @param fileName
	 * @param path
	 * @param conf
	 * @return
	 */
	private FileConfiguration getConfigurationByFileSalidaAnulacionSicacom(String fileName,
			String path, ProccesingAutomaticConf conf) {
		conf.setFields(new ArrayList<MetadataConf>());
		//
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.TIPOREGISTROBOD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(3);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.CODIGOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(4);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.NOMBRECENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(5);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.VALTOT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.TIPOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(8);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(10);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.CODIGOERROR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(8);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionSicacom.DESCRIPCIONERROR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(8);
		conf.getFields().add(mf);		
		//
		conf.setNumFieldsHeader(0);
		return new TemplateAnulacionSicacom().configurationSalidaAnulacionSicacomUG(path);
	}		
	
	/**
	 * obtener configuración de archivos de bancos
	 * 
	 * @param entidadType
	 *            resultado de mensaje
	 * @param path_file
	 *            ruta del archivo
	 * @return
	 */
	private void fileConfigurationBancos(String fileName,
			ProccesingAutomaticConf conf) {
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateRecaudoBancosConsolidado.CODUNI);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setDefaulValue("0000000000");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoBancosConsolidado.CODMTV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		conf.getFields().add(mf);
		// Fecha
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoBancosConsolidado.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(3);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoBancosConsolidado.OFCCAP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		conf.getFields().add(mf);
		// REFERENCIA PAGO
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoBancosConsolidado.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(4);
		mf.setJavaType(new ObjectType(Long.class.getName(), ""));
		conf.getFields().add(mf);
		// VALOR PAGADO
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoBancosConsolidado.VALTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setOrder(4);
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
	}

	/**
	 * Configuracion de archivos de bancos
	 * 
	 * @param fileName
	 *            nombre del archivo
	 * @param path
	 *            ruta del archivo
	 * @param conf
	 *            configuracion
	 * @return
	 */
	private void read_file_bancos(ProccesingAutomaticConf conf,String uid) {
		FileConfiguration fileConfigurarion = null;
		try {
			RecaudoBancos recaudos = new RecaudoBancos();
			String nameFileprocess[] = recaudos.replaceMask(conf.getFileName());
			String addresPoint = this.getPros()
					.getProperty("WSConsultaEntidadFinanciera").trim();
			logger.info("addresPoint = " + addresPoint);
			String timeOut = this.getPros()
					.getProperty("WSLConsultaEntidadFinancieraTimeOut").trim();
			MensajeType entidadType = recaudos.consultEntidad(addresPoint,
					timeOut, nameFileprocess[0], nameFileprocess[1]);

			FileFInanciacion fileFInanciacion = null;
			fileFInanciacion = recaudos.procesarArchivo(entidadType,
					conf.getPath());
			_proccess_block(fileFInanciacion.getFileBody(), null, conf,uid);
		} catch (Exception e) {
			logger.error(
					"Error procesando archivo de Recaudo Bancos "
							+ e.getMessage(), e);
			registrar_auditoriaV2(
					conf.getFileName(),
					"Error procesando archivo de Recaudo Bancos "
							+ e.getMessage(),uid);
		}

	}
	
	/**
	 * Configura archivos Apertura Credito
	 * 
	 * @param fileName
	 * @param path
	 * @param conf
	 * @return
	 */
	private FileConfiguration getConfigurationByFileAperturaCreditos(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Apertura Credito ** ");
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf;
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.FECHA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.NUMCRE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(3);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(4);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(5);
		conf.getFields().add(mf);
		//
//		conf.setNumFieldsHeader(0);
		TemplateAperturaCredito _template = new TemplateAperturaCredito();
		return _template.configurationAperturaCredito(path);
	}	

	/**
	 * Configura archivos Apertura Credito
	 * 
	 * @param fileName
	 * @param path
	 * @param conf
	 * @return
	 */
	private FileConfiguration getConfigurationByFileRespuestaAperturaCreditos(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Respuesta Apertura Credito ** ");
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf;
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.FECHA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		mf.setOrder(4);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.NUMCRE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.CODRESP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(0);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAperturaCredito.DESC);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
//		conf.setNumFieldsHeader(0);
		TemplateAperturaCredito _template = new TemplateAperturaCredito();
		return _template.configurationRespuestaAperturaCredito(path);
	}		
	
	/**
	 * Configura archivos Sicacom
	 * 
	 * @param fileName
	 * @param path
	 * @param conf
	 * @return
	 */
	private FileConfiguration getConfigurationByFileMovimientos(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Movimiento diario ** ");
		conf.setFields(new ArrayList<MetadataConf>());
		//
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDTRN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(2);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.FECHA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		mf.setOrder(4);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.VALOR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.BANCO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDORI);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.ESTADO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.REFPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDBIN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(3);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDTTR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDFAP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		mf.setOrder(5);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDCUO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDNCO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDCAU);
		mf.setAplicaCargue(true);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDCES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDNES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDOFI);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDUSU);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.RESPPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MVDFAC);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(6);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		conf.setNumFieldsHeader(0);
		TemplatePagosAscard _template = new TemplatePagosAscard();
		return _template.configurationMovMonetarioDiario(path);
	}

	/**
	 * configuracion rechazos pagos bancos
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return
	 */
	private FileConfiguration getConfigurationByFileRechazsosPagos(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Rechazos Pagos ** ");
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.REFPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.VALOR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.BANCO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		// mf.setDefaulValue(3);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("CERCOD");
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MENSAJE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.FECHA);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		TemplatePagosAscard _template = new TemplatePagosAscard();
		return _template.configurationSalidaRecaudoBancos(path);
	}

	/**
	 * Configuración Ajustes
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return 
	 * @return
	 */
	private FileConfiguration getConfigurationByFileAjustes(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.FECHA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.REFPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.VALOR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.MOTIVOAJUSTES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.CODTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.PLZTRN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateAjustesAscard.ORIGEN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		return TemplateAjustesAscard.configurationAjustesAscardClaro(path);
	}
	
	/**
	 * Configuración Ajustes
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return 
	 * @return
	 */
	private FileConfiguration getConfigurationByFilePromocionAscard(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.CUSTCODE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.NUMERO_CREDITO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.CODIGO_CAMPANIA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.TIPO_BENEFICIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.VALOR_APLICADO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.FECHA_GENERACION);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.DESCRIPCION);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.CODIGO_TIPO_RESPUESTA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setDecimal(2);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplatePromocionAscard.DESCRIPCION_RESPUESTA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setDecimal(2);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//	
		return TemplatePromocionAscard.configurationPromocionAscard(path);
	}	
	
	/**
	 * Configuración Novedades No Monetarias
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return 
	 * @return
	 */
	private FileConfiguration getConfigurationByFileNovedadesNoMonetarias(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.NUMERO_CREDITO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.NOMBRE_CAMPO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.VALOR_CAMPO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.CODIGO_OBSERVACION);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.PARAMETRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);	
		//
		return TemplateNovedadesNoMonetarias.configurationNovedadesNoMonetarias(path);
	}	
	
	/**
	 * Configuración Respuesta Novedades No Monetarias
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return 
	 * @return
	 */
	private FileConfiguration getConfigurationByFileRespuestaNovedadesNoMonetarias(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.NUMERO_CREDITO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.NOMBRE_CAMPO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.VALOR_CAMPO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.NOGTPR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateNovedadesNoMonetarias.DESCRIPCION);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);	
		//
		return TemplateNovedadesNoMonetarias.configurationRespuestaNovedadesNoMonetarias(path);
	}	

	/**
	 * Configuración de pagos no abonados
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta de archivo
	 * @param conf
	 *            configuración
	 * @return
	 */
	private FileConfiguration getConfigurationByFilePagosNoAbonados(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.FECHA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.REFPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.VALOR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.BANCO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.NUMCOM);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.CODTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosNoAbonados.ORIGEN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		return TemplatePagosNoAbonados.configurationPNA(path);
	}

	/**
	 * configuración rechazos sicacom
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return
	 */
	private FileConfiguration getConfigurationByFileRechazsosSicacom(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Rechazos sicacom ** ");
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.REFPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.VALOR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("MOTIVO_PAGO");
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setDefaulValue(3);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("Codigo Error");
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplatePagosAscard.MENSAJE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("Fechayhora");
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(java.sql.Timestamp.class.getName(),
				"yyyyMMddHHmmss"));
		mf.setOrder(8);
		conf.getFields().add(mf);
		//
		TemplatePagosAscard _template = new TemplatePagosAscard();
		FileConfiguration f = _template.configurationRechazosSicacom(path);
		f.setHeader(true);
		return _template.configurationRechazosSicacom(path);
	}

	/**
	 * Configuración de pagos no abonados
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta de archivo
	 * @param conf
	 *            configuración
	 * @return
	 */
	private FileConfiguration getConfigurationByFileSalidaAjustes(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Salida Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateSalidaAjustesAscard.CODMTV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAjustesAscard.FECHAAJSUTE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAjustesAscard.REFERENCIA_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAjustesAscard.VALORPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAjustesAscard.CERCOD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAjustesAscard.CERDES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		return TemplateSalidaAjustesAscard
				.configurationBatchSalidaAjustesAscard(path);
	}

	/**
	 * Configuración de pagos no abonados
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta de archivo
	 * @param conf
	 *            configuración
	 * @return
	 */
	private FileConfiguration getConfigurationByFileSalidaPNA(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Salida Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateSalidaAplicacionPnaAsc.CODMTV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAplicacionPnaAsc.FECHAAJSUTE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAplicacionPnaAsc.REFERENCIA_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAplicacionPnaAsc.VALORPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAplicacionPnaAsc.CERCOD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAplicacionPnaAsc.CERDES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		return TemplateSalidaAplicacionPnaAsc
				.configurationBatchSalidaAplicacionPNA(path);
	}
	/**
	 * Configuracion de Recaudo bancos RR
	 * @param fileName nombre de archivo
	 * @param path ruta
	 * @param conf objeto de configuracion
	 * @return
	 */
	private FileConfiguration getConfigurationByRecaudoBancosRR(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Recaudo RR ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		//
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODUNI);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODFRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.VABDIV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);	
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODAUT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.NUMCOM);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODMTV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.FECCON);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);	
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.OFCCAP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.VALTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.VALTOT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.LETCOM);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		return TemplateRecaudosBancosRR
				.configurationRecaudosBancosRR(path);
	}
	
	/**
	 * Configuracion de Recaudo bancos RR
	 * @param fileName nombre de archivo
	 * @param path ruta
	 * @param conf objeto de configuracion
	 * @return
	 */
	private FileConfiguration getConfigurationByAnulacionBancos(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Anulacion Bancos ** ");
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.FECHA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.REFPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.VALOR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.BANCO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.NUMCOM);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.CODTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateAnulacionPagosNoAbonados.ORIGEN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);			
		//
		return TemplateAnulacionPagosNoAbonados.configurationPNA(path);
	}	
	
	/**
	 * Configuracion de Recaudo bancos RR
	 * @param fileName nombre de archivo
	 * @param path ruta
	 * @param conf objeto de configuracion
	 * @return
	 */
	private FileConfiguration getConfigurationBySalidaRecaudoBancosRR(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Recaudo RR ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		//
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODUNI);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODFRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.VABDIV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);	
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODAUT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.NUMCOM);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODMTV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.FECCON);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);	
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.OFCCAP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CODTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.VALTRA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.VALTOT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.LETCOM);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CERCOD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CERCON);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudosBancosRR.CERDES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);		
		conf.getFields().add(mf);
		//
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		return TemplateRecaudosBancosRR
				.configurationRecaudosBancosRROutput(path);
	}
	
	/**
	 * Configuracion de Recaudo bancos RR
	 * @param fileName nombre de archivo
	 * @param path ruta
	 * @param conf objeto de configuracion
	 * @return
	 */
	private FileConfiguration getConfigurationBySalidaAnulacionBancos(
			String fileName, String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Anulacion Bancos ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateSalidaAnulacionBancos.CODMTV);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAnulacionBancos.FECHAAJSUTE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAnulacionBancos.REFERENCIA_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAnulacionBancos.VALORPAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAnulacionBancos.CERCOD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		//
		mf = new MetadataConf();
		mf.setName(TemplateSalidaAnulacionBancos.CERDES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(String.class.getName(), ""));
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		return TemplateSalidaAnulacionBancos.configurationSalidaAnulacionBancos(path);
	}
	
	
	/**
	 * Configuración Respuesta Novedades No Monetarias
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return 
	 * @return
	 */
	private FileConfiguration getConfigurationByFileTobePagos(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateTobePagos.NOMBRE_CLIENTE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//v 		
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.NUMERO_DOCUMENTO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.TIPO_DOCUMENTO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.PROCESO_VENTA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.NUMERO_CREDITO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.CANAL_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.VALOR_FACTURADO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDefaulValue(new java.math.BigDecimal("0.00"));
		mf.setDecimal(2);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.VALOR_SALDO_PENDIENTE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setDecimal(2);
		conf.getFields().add(mf);	
		
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.FECHA_LIMITE_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(java.sql.Date.class.getName(), "yyyyMMdd"));
		conf.getFields().add(mf);
		//		
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.REFERENCIA_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setNotNull(true);
		conf.getFields().add(mf);		
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.CORREO_CLIENTE);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//		
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.MIN);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		mf.setOrder(1);
		conf.getFields().add(mf);	
		//
		mf = new MetadataConf();
		mf.setName(TemplateTobePagos.EDAD_MORA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		mf.setJavaType(new ObjectType(BigDecimal.class.getName(), ""));
		conf.getFields().add(mf);			

		//
		mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		//
		return TemplateTobePagos.configurationTobePagos(path);
	}
	
	/**
	 * Configuración Datos Demograficos
	 * 
	 * @param fileName
	 *            nombre de archivo
	 * @param path
	 *            ruta
	 * @param conf
	 *            objeto de configuración
	 * @return 
	 * @return
	 */
	private FileConfiguration getConfigurationByFileDatosDemograficos(String fileName,
			String path, ProccesingAutomaticConf conf) {
		logger.info(" ** Config Ajustes ** ");
		//
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName("FECHA_REGISTRO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis()));
		mf.setOrder(9);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.NOMBRES);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.APELLIDOS);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.TIPO_IDENTIFICACION);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.NUMERO_IDENTIFICACION);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CUST_CODE_RESPONSABLE_PAGO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.EXCENTO_IVA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CODIGO_SALUDO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.DIRECCION_COMPLETA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CIUDAD_DEPARTAMENTO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.INDICADOR_ACT_MASIVA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.NUMERO_CREDITO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CODIGO_OFICINA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CORREO_PARTE1);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.TIPO_PERSONA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.SEXO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.ESTADO_CIVIL);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CORREO_PARTE2);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.NOMBRE_DEPARTAMENTO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.NOMBRE_CIUDAD);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.TELEFONO_CASA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.TELEFONO_OFICINA);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CUSTCODE_SERVICIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateDatosDemograficos.CUSTOMER_ID_SERVICIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		mf.setOrder(1);
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName("NOMBRE_ARCHIVO");
		mf.setAplicaCargue(true);
		mf.setType("DEFAULT");
		mf.setDefaulValue(fileName);
		mf.setOrder(7);
		conf.getFields().add(mf);
		//
		return TemplateDatosDemograficos.configurationDatosDemograficos(path);
	}
	
	
	/**
	 * Consulta procedimiento para obtener metadata y parametros para lectura de
	 * archivo y procesamiento
	 * 
	 * @param fileName
	 * @param path
	 * @return
	 */
	private ProccesingAutomaticConf getConf(int typProcess, String fileName,
			String path) {

		ProccesingAutomaticConf conf = new ProccesingAutomaticConf();
		switch (typProcess) {
		case 1:
			fileConfigurationBancos(fileName, conf);
			conf.setTypeStruct(this.getPros()
					.getProperty("RecaudoBancos.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("RecaudoBancos.TYPE_ARRAY").trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarControlRecaudosBancos").trim());
			conf.setFileName(fileName);
			conf.setPath(path);
			break;
		case 2:
			conf.setFileConfiguration(this.getConfigurationByFileSicacom(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("Sicacom.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros().getProperty("Sicacom.TYPE_ARRAY")
					.trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarControlRecaudosSicacom").trim());
			break;

		case 3:
			conf.setFileConfiguration(this.getConfigurationByFileMovimientos(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("Movimientos.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("Movimientos.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarMovimientos")
					.trim());
			break;
		case 4:
			conf.setFileConfiguration(this
					.getConfigurationByFileRechazsosSicacom(fileName, path,
							conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("RechazasosSicacom.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("RechazasosSicacom.TYPE_ARRAY").trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarControlRechazos").trim());
			break;
		case 5:
			conf.setFileConfiguration(this
					.getConfigurationByFileRechazsosPagos(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("RechazasosSicacom.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("RechazasosSicacom.TYPE_ARRAY").trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarControlRechazos").trim());
			break;
		case 6:
			conf.setFileConfiguration(this.getConfigurationByFileAjustes(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("Ajustes.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros().getProperty("Ajustes.TYPE_ARRAY")
					.trim());
			conf.setCall(this.getPros().getProperty("callRegistrarAjustes")
					.trim());
			break;
		case 7:
			conf.setFileConfiguration(this
					.getConfigurationByFilePagosNoAbonados(fileName, path, conf));
			conf.setTypeStruct(this.getPros().getProperty("PNA.TYPE_STRUCT")
					.trim());
			conf.setTypeArray(this.getPros().getProperty("PNA.TYPE_ARRAY")
					.trim());
			conf.setCall(this.getPros().getProperty("callRegistrarPNA").trim());
			break;
		case 8:
			conf.setFileConfiguration(this.getConfigurationByFileSalidaAjustes(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("SALIDAJUSTES.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("SALIDAJUSTES.TYPE_ARRAY").trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarSalidaAjustes").trim());
			break;
		case 9:
			conf.setFileConfiguration(this.getConfigurationByFileSalidaAjustes(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("SALIDAPNA.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("SALIDAPNA.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarSalidaPNA")
					.trim());
			break;
		case 10:
			conf.setFileConfiguration(this.getConfigurationByRecaudoBancosRR(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("BANCOSRR.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("BANCOSRR.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarBancosRR")
					.trim());
			break;
		case 11:
			conf.setFileConfiguration(this.getConfigurationBySalidaRecaudoBancosRR(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("SALIDABANCOSRR.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("SALIDABANCOSRR.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarSalidaBancosRR")
					.trim());
			break;
		case 12:
			conf.setFileConfiguration(this.getConfigurationByAnulacionBancos(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("AnulacionBancos.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("AnulacionBancos.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarAnulacionBancos")
					.trim());
			break;
		case 13:
			conf.setFileConfiguration(this.getConfigurationByFileAnulacionSicacom(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("AnulacionSicacom.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros().getProperty("AnulacionSicacom.TYPE_ARRAY")
					.trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarAnulacionSicacom").trim());
			break;	
		case 14:
			conf.setFileConfiguration(this.getConfigurationBySalidaAnulacionBancos(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("SalidaAnulacionBancos.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros().getProperty("SalidaAnulacionBancos.TYPE_ARRAY")
					.trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarSalidaAnulacionBancos").trim());
			break;			
		case 15:
			conf.setFileConfiguration(this.getConfigurationByFileSalidaAnulacionSicacom(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("SalidaAnulacionSicacom.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros().getProperty("SalidaAnulacionSicacom.TYPE_ARRAY")
					.trim());
			conf.setCall(this.getPros()
					.getProperty("callRegistrarSalidaAnulacionSicacom").trim());
			break;		
		case 16:
			conf.setFileConfiguration(this.getConfigurationByRecaudoBancosRR(
					fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("AnulacionRecaudoRR.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("AnulacionRecaudoRR.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarAnulacionRecaudoRR")
					.trim());
			break;
		case 17:
			conf.setFileConfiguration(this.getConfigurationBySalidaRecaudoBancosRR(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("SalidaAnulacionRecaudoRR.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("SalidaAnulacionRecaudoRR.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarSalidaAnulacionRecaudoRR")
					.trim());
			break;		
		case 18:
			conf.setFileConfiguration(this.getConfigurationByFileNovedadesNoMonetarias(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("NovedadesNoMonetarias.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("NovedadesNoMonetarias.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarNovedadesNoMonetarias")
					.trim());
			break;	
		case 19:
			conf.setFileConfiguration(this.getConfigurationByFileRespuestaNovedadesNoMonetarias(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("RespuestaNovedadesNoMonetarias.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("RespuestaNovedadesNoMonetarias.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarRespuestaNovedadesNoMonetarias")
					.trim());
			break;		
		case 20:
			conf.setFileConfiguration(this.getConfigurationByFilePromocionAscard(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("PromocionesAscard.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("PromocionesAscard.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarPromocionesAscard")
					.trim());
			break;		
		case 21:
			conf.setFileConfiguration(this.getConfigurationByFileAperturaCreditos(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("AperturaCredito.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("AperturaCredito.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarAperturaCredito")
					.trim());
			break;		
		case 22:
			conf.setFileConfiguration(this.getConfigurationByFileRespuestaAperturaCreditos(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("RespuestaAperturaCredito.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("RespuestaAperturaCredito.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarRespuestaAperturaCredito")
					.trim());
			break;	
		case 23:
			conf.setFileConfiguration(this.getConfigurationByFileTobePagos(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("TobePagos.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("TobePagos.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarTobePagos")
					.trim());
			break;			
		case 24:
			conf.setFileConfiguration(this.getConfigurationByFileDatosDemograficos(fileName, path, conf));
			conf.setTypeStruct(this.getPros()
					.getProperty("DatosDemograficos.TYPE_STRUCT").trim());
			conf.setTypeArray(this.getPros()
					.getProperty("DatosDemograficos.TYPE_ARRAY").trim());
			conf.setCall(this.getPros().getProperty("callRegistrarDatosDemograficos")
					.trim());
			break;
			
			
		default:
			logger.info("Archivo no configurado "+ typProcess);
			break;
		}
		
		return conf;
	}

	/**
	 * Se procesa el archivo en bloque y se va ejecutando en base de datos *
	 * 
	 * @param conf
	 * @return
	 */
	private Boolean read_file_block(ProccesingAutomaticConf conf,String uid) {
		// Configuración de archivo
		FileConfiguration inputFile = conf.getFileConfiguration();
		String limit_blockString = this.getPros().getProperty("limitBlock")
				.trim();
		Long limit_block = Long.parseLong(limit_blockString);
		Long limitCount = 0L;
		Long sizeFile = 0L;
		//
		System.out.println("READ FILE BLOCK FILE BLOCK");
		List<FileOuput> lines = new ArrayList<FileOuput>();
		//
		File f = null;
		BufferedReader b = null;
		String nameFile = "";
		// Result process
		Boolean result = true;
		try {
			f = new File(inputFile.getFileName());
			nameFile = f.getName();
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			String lastLine = "";
			FileOuput headerLine = null;
			int index = 0;
			int header = (inputFile.getTypesHeader() == null ? 0 : 1);
			while ((line = b.readLine()) != null) {
				System.out.println(line);
				if (header == 0) {
					if (!line.trim().equals("")) {
						if (!FileUtil.isFooter(line, inputFile)) {
							try {
								FileOuput _FileOuput = FileUtil.readLine(
										inputFile.getTypes(), line);
								if (_FileOuput != null) {
									lines.add(_FileOuput);
									lastLine = line;
									index++;
								}
							} catch (Exception ex) {
								logger.error("Error leyendo linea " + line, ex);
								System.out.println("Error leyendo linea:: "
										+ line);
								ex.printStackTrace();
							}
						}

					}
				} else {
					headerLine = FileUtil.readLine(inputFile.getTypesHeader(),
							line);
				}
				header = 0;
				// Se revisa el limite para la creacion en invocacion del
				// proceso
				if (limitCount >= limit_block) {
					System.out.println("-- PROCESS BLOCK ... ");
					result = _proccess_block(lines, headerLine, conf,uid);
					lines.clear();
					limitCount = 0L;
					// logger.debug("Lines new size " + lines.size());
				}
				limitCount++;
				sizeFile++;
			}
			// se verifica que no hayan lineas para procesae
			if (lines.size() > 0) {
				result = _proccess_block(lines, headerLine, conf,uid);
			}
		} catch (Exception ex) {
			System.err.println("Error en proceso :" + ex.getMessage());
			ex.printStackTrace();
		}
		return true;
	}

	/**
	 * de procesa lineas y se llena objecto de roles y se ordena
	 * 
	 * @param lines
	 *            linea
	 * @param headerLine
	 *            header
	 * @param conf
	 *            configuracion
	 * @return
	 */
	public Boolean _proccess_block(List<FileOuput> lines, FileOuput headerLine,
			ProccesingAutomaticConf conf,String uid) {

		List<Object[]> roles = new LinkedList<Object[]>();
		// Object[] objecHeader = new Object[conf.getFields().size()];
		HashMap<String, Object> objecHeader_dafault = new HashMap<String, Object>();
		int headerPos = 0;

		// Header
		for (MetadataConf f : conf.getFields()) {
			System.out.println(f.getName() + " - " + f.getType());
			if (f.getType().equals("HEADER")) {
				if (headerLine != null) {
					try {
						Object obj = headerLine.getType(f.getName()).getValue();
						if (f.getJavaType() != null) {
							obj = ObjectUtils.format(
									headerLine.getType(f.getName())
											.getValueString(), f.getJavaType()
											.getClazzName(), f.getJavaType()
											.getFormat(), f.getDecimal());
						}
						objecHeader_dafault.put(f.getName(), obj);

					} catch (FinancialIntegratorException e) {
						logger.error("eror agregando obj :-> " + e.getMessage()
								+ " name: " + f.getName() + " default"
								+ f.getDefaulValue());
						if (f.getDefaulValue() != null) {
							objecHeader_dafault.put(f.getName(),
									f.getDefaulValue());
						}

					} catch (Exception e) {
						logger.error("eror agregando obj :-> " + e.getMessage()
								+ " name: " + f.getName() + " default"
								+ f.getDefaulValue());
						if (f.getDefaulValue() != null) {
							objecHeader_dafault.put(f.getName(),
									f.getDefaulValue());
						}
					}
				}
			} else if (f.getType().equals("DEFAULT")) {
				objecHeader_dafault.put(f.getName(), f.getDefaulValue());
			}
		}
		// Se lee el archivo y se obtienen lineas

		for (FileOuput fo : lines) {
			if (fo.getTypes() != null) {

				HashMap<String, Object> objecs = new HashMap<String, Object>();
				for (MetadataConf f : conf.getFields()) {
					if (f.getType().equals("BODY")) {
						try {

							Object obj = fo.getType(f.getName()).getValue();
							if (f.getJavaType() != null) {
								obj = ObjectUtils.format(fo
										.getType(f.getName()).getValueString(),
										f.getJavaType().getClazzName(), f
												.getJavaType().getFormat(), f
												.getDecimal());
							}
							objecs.put(f.getName(), obj);
						} catch (FinancialIntegratorException e) {
							logger.error("eror agregando obj :-> "
									+ e.getMessage() + " name: " + f.getName()
									+ " default" + f.getDefaulValue());
							if (f.getDefaulValue() != null) {
								objecs.put(f.getName(), f.getDefaulValue());
							}
						} catch (Exception e) {
							logger.error("eror agregando obj :-> "
									+ e.getMessage() + " name: " + f.getName()
									+ " fo " + fo.getTypes() + " default"
									+ f.getDefaulValue());
							if (f.getDefaulValue() != null) {
								objecs.put(f.getName(), f.getDefaulValue());
							}
						}
					}
				}
				/**
				 * Se llena coleccion con la información de linea
				 */
				Object[] collection = new Object[conf.getFields().size()];
				int pos = 0;
				boolean insert = true;
				for (MetadataConf f : conf.getFields()) {
					if (objecHeader_dafault.containsKey(f.getName())) {
						collection[pos] = objecHeader_dafault.get(f.getName());
					}
					if (objecs.containsKey(f.getName())) {
						collection[pos] = objecs.get(f.getName());
					}
					// Se verifica que el valor no puede ser null, y si es null
					// no se inserta
					if (f.isNotNull() && collection[pos] == null) {
						insert = false;
						break;
					}
					pos++;
				}
				if (insert) {
					roles.add(collection);
				}
			}
		}
		if (roles.size() > 0) {
			logger.info("************ Execute Block Size : " + roles.size());
			execute_prod(roles, conf,uid);
		} else {
			logger.info("Values ..... emtpy ");
		}
		return true;
	}

	/**
	 * se ejecuta el procedimiento almacenado
	 * 
	 * @param roles
	 * @param conf
	 * @return
	 */
	private Boolean execute_prod(List<Object[]> roles,
			ProccesingAutomaticConf conf,String uid) {
		Database _database;
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
			return false;
		}
		java.sql.Struct[] struct = null;
		java.sql.Array array = null;
		logger.info("Type Struct " + conf.getTypeStruct() + " - TypeArray: "
				+ conf.getTypeArray());
		try {
			// Se construye Struct
			struct = new Struct[roles.size()];
			int i = 0;
			int error = 0;
			for (Object[] rol : roles) {
				logger.info("Role " + i + " : " + Arrays.toString(rol));
				logger.info("TAMAÑOS TIPOS:"+rol.length);
//				for (int j = 0; j < rol.length; j++) {
//					logger.info("TIPOSCLASS"+j+":"+rol[j].getClass().toString());
//					logger.info("TIPOSValue"+j+":"+rol[j].toString());
//				}
				try {
					struct[i] = _database.getConn(uid).createStruct(
							conf.getTypeStruct(), rol);
					i++;
				} catch (SQLException e) {
					logger.error("Error creando struct " + e.getMessage()
							+ " - " + e.getErrorCode() + Arrays.toString(rol),
							e);
					error++;
				} catch (Exception e) {
					logger.error("Error creando struct " + e.getMessage()
							+ " - " + Arrays.toString(rol), e);
					error++;
				}

			}
			/**
			 * si existe error creando structos
			 */
			if (error > 0) {
				logger.info("No se han creado los siguientes Structs :" + error);
				List<Struct> structWthNull = new ArrayList<Struct>();
				for (java.sql.Struct s : struct) {
					if (s != null) {
						structWthNull.add(s);
					}
				}
				struct = structWthNull
						.toArray(new java.sql.Struct[structWthNull.size()]);
			}
		} catch (Exception ex) {
			logger.error("Error construyendo struct " + ex.getMessage(), ex);
			_database.disconnet(uid);
			return false;
		}

		// se contruye array
		try {
			array = ((OracleConnection) _database.getConn(uid)).createOracleArray(
					conf.getTypeArray(), struct);
//			ResultSet rs = array.getResultSet();
//			while (rs.next()) {
//			logger.info("ArrayType:"+rs.getObject(2).toString());	
//			logger.info("ArrayTypeClass:"+rs.getObject(2).getClass().toString());
//			Struct struc = (Struct)rs.getObject(2);
//			logger.info("StructTYPE:"+struc.getSQLTypeName());
//			Object [] attributes =struc.getAttributes();
//			for (int j = 0; j < struc.getAttributes().length; j++) {
//				logger.info("StructValue"+j+":"+attributes[j].toString());
//				logger.info("StructValue"+j+":"+attributes[j].getClass());
//			}
//			}
		} catch (SQLException e) {
			logger.error("Error construyendo array : " + e.getMessage(), e);
			_database.disconnet(uid);
			return false;
		} catch (Exception e) {
			logger.error("Error construyendo array : " + e.getMessage(), e);
			_database.disconnet(uid);
			return false;
		}
		_database.disconnet(uid);
		CallableStatement cs = null;
		try {
			// Se invoca procedimiento
			_database.setCall(conf.getCall());
			List<Object> input = new ArrayList<Object>();
			input.add(array);
			List<Integer> output = new ArrayList<Integer>();
			output.add(java.sql.Types.NUMERIC);
			output.add(java.sql.Types.VARCHAR);
			cs = _database.executeCallOutputs(_database.getConn(uid), output,
					input,uid);
			if (cs != null) {
				logger.info("Call : " + conf.getCall() + " - P_EXITO : "
						+ cs.getInt(2) + " - P_ERROR : " + cs.getString(3));
			}
		} catch (SQLException e) {
			logger.error(
					"ERROR call : " + conf.getCall() + " : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error(
					"ERROR call : " + conf.getCall() + " : " + e.getMessage(),
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
		return true;
	}

	/**
	 * Busca los archivos por expresiones regulares
	 * 
	 * @param path
	 * @param existFilesProcess
	 */
	private void processFiles(int typProcess, String path, String path_process,
			String existFilesProcess,String uid) {
		List<File> fileProcessList = null;
		// Se busca archivos para procesar
		try {
			logger.info("Fin Files into: " + path);
			// Se busca archivos que tenga la extención configurada
			fileProcessList = FileUtil.findFileNameToExpresionRegular(path,
					existFilesProcess);
		} catch (FinancialIntegratorException e) {
			logger.error("Error reading files into folder " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Error reading files into folder " + e.getMessage(), e);
			return;
		}
		logger.info("fileProcessList: " + fileProcessList.size());
		// Se verifica que exista un archivo en la ruta y con las carateristicas
		if (fileProcessList != null && !fileProcessList.isEmpty()) {
			for (File fileProcess : fileProcessList) {
				// Si archivo existe
				if (fileProcess != null) {
					String fileName = fileProcess.getName();
					String fileNameFullPath = path + fileName;
					// Se valida si el archivo se procesa
					if (validateFile(fileNameFullPath, fileName, typProcess)) {
						// Se mueve archivo a encriptado a carpeta de process
						String fileNameCopy = path_process + File.separator
								+ "processes_" + fileName;
						try {
							logger.info("Exist File: " + fileNameCopy);
							if (!FileUtil.fileExist(fileNameCopy)) {
								if (FileUtil.copy(fileNameFullPath,
										fileNameCopy)) {
									Boolean result = false;
									ProccesingAutomaticConf conf = null;
									switch (typProcess) {
									case 1:
										logger.info("Archivos de Recaudos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_bancos(conf,uid);
										break;
									case 2:
										logger.info("Archivos de Recaudos Sicacom");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;

									case 3:
										logger.info("Archivos de Movimeintos Diarios");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 4:
										logger.info("Archivos de Rechazos Sicacom");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 5:
										logger.info("Archivos de Rechazos Pagos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 6:
										logger.info("Archivo Ajustes");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 7:
										logger.info("Archivo Pagos No Abonados");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 8:
										logger.info("Archivo Salida Ajustes");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 9:
										logger.info("Archivo Salida PNA");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 10:
										logger.info("Archivo Bancos RR");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 11:
										logger.info("Archivo Salida Bancos RR");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 12:
										logger.info("Archivo Anulacion Bancos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 13:
										logger.info("Archivo Anulacion Sicacom");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;
									case 14:
										logger.info("Archivo Salida Anulacion Bancos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;		
									case 15:
										logger.info("Archivo Salida Anulacion Sicacom");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
									case 16:
										logger.info("Archivo Anulacion Recaudo RR");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);
										break;		
									case 17:
										logger.info("Archivo Salida Anulacion Recaudo RR");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;			
									case 18:
										logger.info("Archivo Novedades No Monetarias");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;		
									case 19:
										logger.info("Archivo Respuesta Novedades No Monetarias");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;		
									case 20:
										logger.info("Archivo Promociones Ascard");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;			
									case 21:
										logger.info("Archivo Apertura Creditos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;		
									case 22:
										logger.info("Archivo Respuesta Apertura Creditos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;	
									case 23:
										logger.info("Archivo To be de Pagos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;	
									case 24:
										logger.info("Archivo Datos Demográficos");
										conf = getConf(typProcess, fileName,
												fileNameCopy);
										read_file_block(conf,uid);										
										break;
									}

									String observacion = "Archivo Procesado Con exito";
									registrar_auditoriaV2(fileName, observacion,uid);
									FileUtil.delete(fileNameFullPath);
								}
							} else {
								logger.info("File Exist " + fileName
										+ " Delete ");
								FileUtil.delete(fileNameFullPath);
							}
						} catch (Exception ex) {
							logger.error("Error en proceso :" + typProcess
									+ ", ex " + ex.getMessage(), ex);
						}
					}
				}
			}
		}
	}

	@Override
	public void process() {
              UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		try {
		// TODO Auto-generated method stub
              
		logger.info(" Control Recaudo Upgrade 1.0.1 ");
		if (!inicializarProps(uid)) {
			logger.info(" ** Don't initialize properties ** ");
			return;
		}
		// Properties racaudos
		String pathCopy = "";
		String path_process_recaudos = "";
		String ExtfileProcess_recaudos = "";
		// Properties recaudo sicacom
		String path_process_recaudos_sicacom = "";
		String ExtfileProcess_recaudos_sicacom = "";
		// Properties recaudo movimientos
		String path_process_salidas_movimientos = "";
		String ExtfileProcess_salidas_movimientos = "";
		// Properties rechazos sicacom
		String path_process_rechazos_sicacom = "";
		String ExtfileProcess_rechazos_sicacom = "";
		// Properties rechazos pagos
		String path_process_rechazos_pagos = "";
		String ExtfileProcess_rechazos_pagos = "";
		// Ajustes
		String path_process_ajustes = "";
		String ExtfileProcess_Ajustes = "";
		// Pagos no abonados
		String path_process_pna = "";
		String ExtfileProcess_pna = "";
		//
		String path_process_salida_ajustes = "";
		String ExtfileProcess_salida_ajustes = "";
		//
		String path_process_salida_pna = "";
		String ExtfileProcess_salida_pna = "";
		//
		String path_process_recaudo_rr = "";
		String ExtfileProcess_recaudo_rr = "";
		//
		String path_process_salida_bancos_rr="";
		String ExtfileProcess_salida_bancos_rr ="";
		//
		String path_process_anulacion_bancos="";
		String ExtfileProcess_anulacion_bancos ="";
		//
		String path_process_anulacion_sicacom="";
		String ExtfileProcess_anulacion_sicacom ="";		
		//
		String path_process_salida_anulacion_bancos="";
		String ExtfileProcess_salida_anulacion_bancos ="";
		//
		String path_process_salida_anulacion_sicacom="";
		String ExtfileProcess_salida_anulacion_sicacom ="";		
		//
		String path_process_anulacion_recaudo_rr = "";
		String ExtfileProcess_anulacion_recaudo_rr = "";		
		//
		String path_process_salida_anulacion_rr="";
		String ExtfileProcess_salida_anulacion_rr ="";			
		//
		String path_process_novedades_no_monetarias = "";
		String ExtfileProcess_novedades_no_monetarias = "";			
		//
		String path_process_respuesta_novedades_no_monetarias = "";
		String ExtfileProcess_respuesta_novedades_no_monetarias = "";		
		//
		String path_process_promocion_ascard = "";
		String ExtfileProcess_promocion_ascard  = "";	
		//
		String path_process_apertura_credito = "";
		String ExtfileProcess_apertura_credito = "";			
		//
		String path_process_respuesta_apertura_credito = "";
		String ExtfileProcess_respuesta_apertura_credito = "";		
		//		
		String path_process_tobe_pagos = "";
		String ExtfileProcess_tobe_pagos = "";		
		//
		String path_process_datos_demograficos = "";
		String ExtfileProcess_datos_demograficos = "";
		try {
			// Properties racaudos
			pathCopy = this.getPros().getProperty("pathCopy").trim();
			path_process_recaudos = this.getPros()
					.getProperty("fileProccessRecaudos").trim();
			ExtfileProcess_recaudos = this.getPros().getProperty(
					"ExtfileProcessRecaudos");
			// Properties sicacom
			path_process_recaudos_sicacom = this.getPros()
					.getProperty("fileProccessRecaudosSicacom").trim();
			ExtfileProcess_recaudos_sicacom = this.getPros().getProperty(
					"ExtfileProcessRecaudosSicacom");
			// Properties salidas movimientos
			path_process_salidas_movimientos = this.getPros()
					.getProperty("fileProccessSalidasMovimientos").trim();
			ExtfileProcess_salidas_movimientos = this.getPros().getProperty(
					"ExtfileProcessSalidasMovimientos");
			// Properties rechazos recudos sicacom
			path_process_rechazos_sicacom = path_process_salidas_movimientos;
			ExtfileProcess_rechazos_sicacom = this.getPros().getProperty(
					"ExtfileProcessRechazosSicacom");
			// Properties rechazos pagos
			path_process_rechazos_pagos = path_process_salidas_movimientos;
			ExtfileProcess_rechazos_pagos = this.getPros().getProperty(
					"ExtfileProcessRechazosPagos");
			// Properties ajustes
			path_process_ajustes = this.getPros()
					.getProperty("fileProccessAjustes").trim();
			ExtfileProcess_Ajustes = this.getPros().getProperty(
					"ExtfileProcessAjustes");
			// Properties pagos no abonados
			path_process_pna = this.getPros().getProperty("fileProccessPNA")
					.trim();
			ExtfileProcess_pna = this.getPros()
					.getProperty("ExtfileProcessPNA");
			//
			path_process_salida_ajustes = this.getPros()
					.getProperty("fileProccessSalidasAjustes").trim();
			ExtfileProcess_salida_ajustes = this.getPros().getProperty(
					"ExtfileProcessSalidaAjustes");
			// Properties salida PNA
			path_process_salida_pna = this.getPros()
					.getProperty("fileProccessSalidasPNA").trim();
			ExtfileProcess_salida_pna = this.getPros().getProperty(
					"ExtfileProcessSalidaPNA");
			// Properties salida PNA
			path_process_recaudo_rr = this.getPros()
					.getProperty("fileProccessRecaudoRR").trim();
			ExtfileProcess_recaudo_rr = this.getPros().getProperty(
					"ExtfileProcessRecaudoRR");
			//
			path_process_salida_bancos_rr = path_process_salidas_movimientos;
			ExtfileProcess_salida_bancos_rr = this.getPros().getProperty(
					"ExtfileProcessSalidaBancosRR");			
			//
			path_process_anulacion_bancos = this.getPros()
					.getProperty("fileProccessAnulacionBancos").trim();
			ExtfileProcess_anulacion_bancos = this.getPros().getProperty(
					"ExtfileProcessAnulacionBancos");			
			//
			path_process_anulacion_sicacom = this.getPros()
					.getProperty("fileProccessAnulacionSicacom").trim();
			ExtfileProcess_anulacion_sicacom = this.getPros().getProperty(
					"ExtfileProcessAnulacionSicacom");				
			//
			path_process_salida_anulacion_bancos = this.getPros()
					.getProperty("fileProccessSalidasAnulacionBancos").trim();
			ExtfileProcess_salida_anulacion_bancos = this.getPros().getProperty(
					"ExtfileProcessSalidaAnulacionBancos");
			//
			path_process_salida_anulacion_sicacom = this.getPros()
					.getProperty("fileProccessSalidaAnulacionSicacom").trim();
			ExtfileProcess_salida_anulacion_sicacom = this.getPros().getProperty(
					"ExtfileProcessSalidaAnulacionSicacom");			
			//
			path_process_anulacion_recaudo_rr = this.getPros()
					.getProperty("fileProccessAnulacionRecaudoRR").trim();
			ExtfileProcess_anulacion_recaudo_rr = this.getPros().getProperty(
					"ExtfileProcessAnulacionRecaudoRR");
			//
			path_process_salida_anulacion_rr = this.getPros()
					.getProperty("fileProccessSalidaAnulacionRecaudoRR").trim();
			ExtfileProcess_salida_anulacion_rr = this.getPros().getProperty(
					"ExtfileProcessSalidaAnulacionRecaudoRR");	
			//
			path_process_novedades_no_monetarias = this.getPros()
					.getProperty("fileProccessNovedadesNoMonetarias").trim();
			ExtfileProcess_novedades_no_monetarias = this.getPros().getProperty(
					"ExtfileProcessNovedadesNoMonetarias");		
			//
			path_process_respuesta_novedades_no_monetarias = this.getPros()
					.getProperty("fileProccessRespuestaNovedadesNoMonetarias").trim();
			ExtfileProcess_respuesta_novedades_no_monetarias = this.getPros().getProperty(
					"ExtfileProcessRespuestaNovedadesNoMonetarias");	
			//
			path_process_promocion_ascard = this.getPros()
					.getProperty("fileProccessPromocionAscard").trim();
			ExtfileProcess_promocion_ascard = this.getPros().getProperty(
					"ExtfileProcessPromocionAscard");	
			
			//
			path_process_apertura_credito = this.getPros()
					.getProperty("fileProccessAperturaCredito").trim();
			ExtfileProcess_apertura_credito = this.getPros().getProperty(
					"ExtfileProcessAperturaCredito");		
			//
			path_process_respuesta_apertura_credito = this.getPros()
					.getProperty("fileProccessRespuestaAperturaCredito").trim();
			ExtfileProcess_respuesta_apertura_credito = this.getPros().getProperty(
					"ExtfileProcessRespuestaAperturaCredito");				
			//
			path_process_tobe_pagos = this.getPros()
					.getProperty("fileProccesTobePagos").trim();
			ExtfileProcess_tobe_pagos = this.getPros().getProperty(
					"ExtfileProcessTobePagos");		
			//
			path_process_datos_demograficos = this.getPros()
					.getProperty("fileProccessDatosDemograficos").trim();
			ExtfileProcess_datos_demograficos = this.getPros().getProperty(
					"ExtfileProcessDatosDemograficos");
			//logger.info("ExtfileProcessTobePagos: " + pathCopy);
			//
			logger.info("pathCopy: " + pathCopy);
			logger.info("path_process_recaudos: " + path_process_recaudos);
			logger.info("ExtfileProcess_recaudos: " + ExtfileProcess_recaudos);
		} catch (Exception ex) {
			logger.error("Error find properties " + ex.getMessage());
			return;
		}
		this.processFiles(1, path_process_recaudos, pathCopy,
				ExtfileProcess_recaudos,uid);
		this.processFiles(2, path_process_recaudos_sicacom, pathCopy,
				ExtfileProcess_recaudos_sicacom,uid);
		this.processFiles(3, path_process_salidas_movimientos, pathCopy,
				ExtfileProcess_salidas_movimientos,uid);
		this.processFiles(4, path_process_rechazos_sicacom, pathCopy,
				ExtfileProcess_rechazos_sicacom,uid);
		this.processFiles(5, path_process_rechazos_pagos, pathCopy,
				ExtfileProcess_rechazos_pagos,uid);
		this.processFiles(6, path_process_ajustes, pathCopy,
				ExtfileProcess_Ajustes,uid);
		this.processFiles(7, path_process_pna, pathCopy, ExtfileProcess_pna,uid);
		this.processFiles(8, path_process_salida_ajustes, pathCopy,
				ExtfileProcess_salida_ajustes,uid);
		this.processFiles(9, path_process_salida_pna, pathCopy,
				ExtfileProcess_salida_pna,uid);
		this.processFiles(10, path_process_recaudo_rr, pathCopy,
				ExtfileProcess_recaudo_rr,uid);
		this.processFiles(11 ,path_process_salida_bancos_rr, pathCopy,
				ExtfileProcess_salida_bancos_rr,uid);
		this.processFiles(12 ,path_process_anulacion_bancos, pathCopy,
				ExtfileProcess_anulacion_bancos,uid);
		this.processFiles(13 ,path_process_anulacion_sicacom, pathCopy,
				ExtfileProcess_anulacion_sicacom,uid);		
		this.processFiles(14 ,path_process_salida_anulacion_bancos, pathCopy,
				ExtfileProcess_salida_anulacion_bancos,uid);
		this.processFiles(15 ,path_process_salida_anulacion_sicacom, pathCopy,
				ExtfileProcess_salida_anulacion_sicacom,uid);		
		this.processFiles(16 ,path_process_anulacion_recaudo_rr, pathCopy,
				ExtfileProcess_anulacion_recaudo_rr,uid);
		this.processFiles(17 ,path_process_salida_anulacion_rr, pathCopy,
				ExtfileProcess_salida_anulacion_rr,uid);	
		this.processFiles(18 ,path_process_novedades_no_monetarias, pathCopy,
				ExtfileProcess_novedades_no_monetarias,uid);	
		this.processFiles(19 ,path_process_respuesta_novedades_no_monetarias, pathCopy,
				ExtfileProcess_respuesta_novedades_no_monetarias,uid);			
		this.processFiles(20 ,path_process_promocion_ascard, pathCopy,
				ExtfileProcess_promocion_ascard,uid);			
		this.processFiles(21 ,path_process_apertura_credito, pathCopy,
				ExtfileProcess_apertura_credito,uid);	
		this.processFiles(22 ,path_process_respuesta_apertura_credito, pathCopy,
				ExtfileProcess_respuesta_apertura_credito,uid);		
		this.processFiles(23 ,path_process_tobe_pagos, pathCopy,
				ExtfileProcess_tobe_pagos,uid);			
		this.processFiles(24 ,path_process_datos_demograficos, pathCopy,
				ExtfileProcess_datos_demograficos,uid);
		} catch (Throwable e) {
			logger.error(
					"Error General " + e.getMessage(),
					e);
		}
	}

}
