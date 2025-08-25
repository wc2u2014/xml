
package co.com.claro.financialintegrator.implement;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

//import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.ReporteSaldoFavor;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleTypes;
//import weblogic.utils.classfile.expr.TryCatchStatement;

public class ReportesSaldoFavor extends GenericProccess {
	private Logger logger = Logger.getLogger(ReportesSaldoFavor.class);
	
	@Override
	public void process() {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		if (!inicializarProps(uid)) {
			logger.info(" ** No se inicializa propiedades ** ");
			return;
		}
		logger.info("Propiedades "+this.getPros());
		String path = this.getPros().getProperty("path");
		String path_process = this.getPros().getProperty("path_process");
		String pathBSCS = this.getPros().getProperty("pathBSCS");
		String fileOutputPrefixCobranza = this.getPros().getProperty("fileOutputPrefixCobranza").trim();
		String fileOutputPrefixConsolidado = this.getPros().getProperty("fileOutputPrefixRetiradosConsolidado").trim();
		String fileOutputPrefixDetallado = this.getPros().getProperty("fileOutputPrefixRetiradosDetallado").trim();
		try {
		FileUtil.createDirectory(this.getPros().getProperty("path").trim());
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ path_process);
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ pathBSCS);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio"
			+ e.getMessage());
		}
		
		//REPORTE_COBRANZAS_SALDO_FAVOR
		List<ReporteSaldoFavor> lineFileCreateCobranza = consultarCobranzasSaldos(uid);
		String fileNameOutputCobranza =this.nameFile(fileOutputPrefixCobranza);
		String fileNameCobranza =path + path_process + fileNameOutputCobranza;
		String fileNameBSCSCobranza =path + pathBSCS + fileNameOutputCobranza;
		creaReporteCobranzaSaldos(fileNameCobranza, lineFileCreateCobranza);	
		try {
			FileUtil.move(fileNameCobranza, fileNameBSCSCobranza);
		} catch (FinancialIntegratorException e) {
			logger.error("Error moviendo archivo de Cobranza"
					+ e.getMessage());
		}
		String observacion = "Reporte Cobranza Saldos creado y enviado con exito";
		registrar_auditoriaV2(fileNameCobranza, observacion,uid);
	    
		//REPORTE_RETIRADOS_CONSOLIDADO_SALDO_FAVOR
		List<ReporteSaldoFavor> lineFileCreateConsolidado = consultarConsolidadoSaldos(uid);
		String fileNameOutputConsolidado =this.nameFile(fileOutputPrefixConsolidado);
		String fileNameConsolidado =path + path_process + fileNameOutputConsolidado;
		String fileNameBSCSConsolidado =path + pathBSCS + fileNameOutputConsolidado;
		creaReporteRetiradosConsolidadoSaldoFavor(fileNameConsolidado, lineFileCreateConsolidado);
		try {
			FileUtil.move(fileNameConsolidado, fileNameBSCSConsolidado);
		} catch (FinancialIntegratorException e) {
			logger.error("Error moviendo archivo consolidado"
					+ e.getMessage());
		}
		observacion = "Reporte retirados consolidado saldo a favor creado y enviado con exito";
		registrar_auditoriaV2(fileNameConsolidado, observacion,uid);
		
		//REPORTE_RETIRADOS_DETALLADO_SALDO_FAVOR
		List<ReporteSaldoFavor> lineFileCreateDetallado = consultarDetalladoSaldos(uid);
		String fileNameOutputDetallado =this.nameFile(fileOutputPrefixDetallado);
		String fileNameDetallado =path + path_process + fileNameOutputDetallado;
		String fileNameBSCSDetallado =path + pathBSCS + fileNameOutputDetallado;
		creaReporteRetiradosDetalladoSaldoFavor(fileNameDetallado, lineFileCreateDetallado);
		try {
			FileUtil.move(fileNameDetallado, fileNameBSCSDetallado);
		} catch (FinancialIntegratorException e) {
			logger.error("Error moviendo archivo detallado"
					+ e.getMessage());
		}
		observacion = "Reporte retirados detallado saldo a favor creado y enviado con exito";
		registrar_auditoriaV2(fileNameDetallado, observacion,uid);
	}
	
	private List<ReporteSaldoFavor> consultarCobranzasSaldos(String uid) {
		CallableStatement csCobranza=null;
	    Database databaseCobranza = null;
	    String dataSourceCobranza = null;
	    String callReporteCobranzasSaldoFavor = null;
	    Connection connectionCobranza = null;
	    String exitoCobranza = null;
	    ResultSet rs = null;   
	    List<ReporteSaldoFavor> listRSF = new ArrayList<ReporteSaldoFavor>();
		try { 
			dataSourceCobranza = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim(); 
			callReporteCobranzasSaldoFavor = this.getPros().getProperty("callReporteCobranzasSaldoFavor");
			databaseCobranza = Database.getSingletonInstance(dataSourceCobranza, null,uid);
			connectionCobranza=databaseCobranza.getConnection(uid);
			csCobranza = connectionCobranza.prepareCall(callReporteCobranzasSaldoFavor);
			csCobranza.registerOutParameter(1,OracleTypes.VARCHAR);
			csCobranza.registerOutParameter(2, OracleTypes.CURSOR);
			csCobranza.execute();
			
			exitoCobranza = csCobranza.getString(1);
			if (exitoCobranza.equals("TRUE")) {
				rs = (ResultSet) csCobranza.getObject(2);
				int count =0;
				while (rs.next()) {
				    ReporteSaldoFavor RSF = new ReporteSaldoFavor();
					RSF.setBin(rs.getString("bin"));
					RSF.setTotSaldoFavor(rs.getString("Tot_saldo_favor"));
					RSF.setCantidad(rs.getString("cantidad"));
					listRSF.add(count,RSF);
					count++;
				}
			}
			String observacion = "Ejecutada correctamente consulta de reporte cobranzas saldo a favor";
			String NombreProceso = "Reporte cobranzas saldo a favor";
			registrar_auditoriaV2(NombreProceso, observacion,uid);
		} catch (Exception e) {
			logger.error("Error consulta de reporte cobranzas saldo a favor"
					+ e.getMessage());
			String observacion = "Ejecutada correctamente consulta de reporte cobranzas saldo a favor";
			String NombreProceso = "reporte cobranzas saldo a favor";
			registrar_auditoriaV2(NombreProceso, observacion,uid);
		}
		try {
		databaseCobranza.disconnetCs(uid);
		databaseCobranza.disconnet(uid);
		}catch (Exception e){
			logger.error("Error cerrando consulta de reporte cobranzas saldo a favor"
					+ e.getMessage());
		}
		return listRSF;
	}
	
	private List<ReporteSaldoFavor> consultarConsolidadoSaldos(String uid) {
		CallableStatement csConsolidado=null;
	    Database databaseConsolidado = null;
	    String dataSourceConsolidado = null;
	    String callReporteConsolidadosSaldoFavor = null;
	    Connection connectionConsolidado = null;
	    String exitoConsolidado = null;
	    ResultSet rs = null;   
	    List<ReporteSaldoFavor> listRSF = new ArrayList<ReporteSaldoFavor>();
		try { 
			dataSourceConsolidado = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim(); 
			callReporteConsolidadosSaldoFavor = this.getPros().getProperty("callReporteRetiradosConsolidadoSaldoFavor");
			databaseConsolidado = Database.getSingletonInstance(dataSourceConsolidado, null,uid);
			connectionConsolidado=databaseConsolidado.getConnection(uid);
			csConsolidado = connectionConsolidado.prepareCall(callReporteConsolidadosSaldoFavor);
			csConsolidado.registerOutParameter(1,OracleTypes.VARCHAR);
			csConsolidado.registerOutParameter(2, OracleTypes.CURSOR);
			// Se invoca procedimiento
			csConsolidado.execute();
			
			exitoConsolidado = csConsolidado.getString(1);
			if (exitoConsolidado.equals("TRUE")) {
				rs = (ResultSet) csConsolidado.getObject(2);
				int count =0;
				while (rs.next()) {
				    ReporteSaldoFavor RSF = new ReporteSaldoFavor();
					RSF.setBin(rs.getString("bin"));
					RSF.setEdadCredito(rs.getString("edad_credito"));
					RSF.setTotSaldoFavor(rs.getString("Tot_saldo_favor"));
					RSF.setCantidad(rs.getString("cantidad"));
					listRSF.add(count,RSF);
					count++;
				}
			}
			String observacion = "Ejecutada correctamente consulta de reporte Consolidado saldo a favor";
			String NombreProceso = "Reporte Consolidado saldo a favor";
			registrar_auditoriaV2(NombreProceso, observacion,uid);
		} catch (Exception e) {
			logger.error("Error consulta de reporte cobranzas saldo a favor"
					+ e.getMessage());
			String observacion = "Ejecutada correctamente consulta de reporte Consolidado saldo a favor";
			String NombreProceso = "reporte Consolidado saldo a favor";
			registrar_auditoriaV2(NombreProceso, observacion,uid);
		}
		try {
		databaseConsolidado.disconnetCs(uid);
		databaseConsolidado.disconnet(uid);
		}catch (Exception e){
			logger.error("Error cerrando consulta de reporte consolidado saldo a favor"
					+ e.getMessage());
		}
		return listRSF;
	}
	
	private List<ReporteSaldoFavor> consultarDetalladoSaldos(String uid) {
		CallableStatement csDetallado=null;
	    Database databaseDetallado = null;
	    String dataSourceDetallado = null;
	    String callReporteDetalladosSaldoFavor = null;
	    BigDecimal pageNumber = new BigDecimal(1);
	    BigDecimal pageSize = new BigDecimal(10000);
	    int validaRS = 0;
	    Connection connectionDetallado = null;
	    String exitoDetallado = null;
	    ResultSet rs = null;   
	    List<ReporteSaldoFavor> listRSF = new ArrayList<ReporteSaldoFavor>();
		try { 
			dataSourceDetallado = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim(); 
			callReporteDetalladosSaldoFavor = this.getPros().getProperty("callReporteRetiradosDetalladoSaldoFavor");
			databaseDetallado = Database.getSingletonInstance(dataSourceDetallado, null,uid);
			while (validaRS == 0) {
				connectionDetallado=databaseDetallado.getConnection(uid);
				csDetallado = connectionDetallado.prepareCall(callReporteDetalladosSaldoFavor);
				
				csDetallado.setBigDecimal(1, pageNumber);
				csDetallado.setBigDecimal(2, pageSize);
				csDetallado.registerOutParameter(3,OracleTypes.VARCHAR);
				csDetallado.registerOutParameter(4, OracleTypes.CURSOR);
				csDetallado.execute();
				exitoDetallado = csDetallado.getString(3);
				if (exitoDetallado.equals("TRUE")) {
					rs = (ResultSet) csDetallado.getObject(4);
					if (rs.next()) {
						int count =0;
						while (rs.next()) {
						    ReporteSaldoFavor RSF = new ReporteSaldoFavor();
							RSF.setBin(rs.getString("bin"));
							RSF.setNumerocredito(rs.getString("numero_credito"));
							RSF.setEdadCredito(rs.getString("edad_credito"));
							RSF.setSaldo(rs.getString("Saldo"));
							listRSF.add(count,RSF);
							count++;
						}
						String observacion = "Ejecutada correctamente consulta de reporte Detallado saldo a favor. Pagina: " + pageNumber;
						String NombreProceso = "Reporte Detallado saldo a favor";
						registrar_auditoriaV2(NombreProceso, observacion,uid);
						pageNumber = pageNumber.add(new BigDecimal(1));
					}
					else {
						validaRS = 1;
						logger.info("Ya no hay mas paginas que recorrer.");
					}
				}
			try {
				databaseDetallado.disconnetCs(uid);
				databaseDetallado.disconnet(uid);
				}catch (Exception e){
					logger.error("Error cerrando consulta de reporte Detallado saldo a favor"
							+ e.getMessage());
				}
			}
		} catch (Exception e) {
			logger.error("Error consulta de reporte cobranzas saldo a favor. Pagina: " + pageNumber + " "
					+ e.getMessage());
			String observacion = "Ejecutada correctamente consulta de reporte Detallado saldo a favor";
			String NombreProceso = "reporte Detallado saldo a favor";
			registrar_auditoriaV2(NombreProceso, observacion,uid);
		}
		return listRSF;
	}
	
	public String nameFile(String fileOutputPrefix) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha")
					.trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExt").trim();
			String prefix =	fileOutputPrefix.trim();	
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archivo Reporte Saldos" + ex.getMessage(), ex);
			;
			return null;
		}

	}
	
    private void creaReporteCobranzaSaldos(String fileNameReporteSaldosFavor, List<ReporteSaldoFavor> fileCreate) {
	    
    	String fechaInicio = this.getPros().getProperty("FechaInicio").trim();
    	
    	try {
    		FileWriter fw = new FileWriter(fileNameReporteSaldosFavor);
			BufferedWriter bw = new BufferedWriter(fw);
			logger.info(fileNameReporteSaldosFavor);
			
			String fechaArch = DateUtil.getDateFormFormat("dd/MM/yyyy");
            String binAux = null;
			bw.write(fechaArch);
			bw.newLine();
			bw.write(fechaInicio + " - " +fechaArch);
			bw.newLine();bw.newLine();
			logger.info("Prueba");
			for (ReporteSaldoFavor repSalFav:fileCreate) {
				String bin = repSalFav.getBin();
				if (!bin.equals(binAux)) { 
					binAux = bin;
					bw.write(bin);
					bw.newLine();
				}
				bw.write(repSalFav.getTotSaldoFavor() + "|" + repSalFav.getCantidad());
				bw.newLine();bw.newLine();
			}
			bw.close();
    	} catch (Exception ex){
    		logger.error(
					"Error escribiendo en el archivo Reporte Saldos Favor Cobranza" + ex.getMessage(), ex);
			;
    	}
    }
    	
	private void creaReporteRetiradosConsolidadoSaldoFavor(String fileNameReporteSaldosConsolidado, List<ReporteSaldoFavor> fileCreate) {
	    
    	String fechaInicio = this.getPros().getProperty("FechaInicio").trim();
    	
    	try {
    		FileWriter fw = new FileWriter(fileNameReporteSaldosConsolidado);
			BufferedWriter bw = new BufferedWriter(fw);
			
			String fechaArch = DateUtil.getDateFormFormat("dd/MM/yyyy");
            String binAux = null;
			bw.write(fechaArch);
			bw.newLine();
			bw.write(fechaInicio + " - " +fechaArch);
			bw.newLine();bw.newLine();
			for (ReporteSaldoFavor repSalFav:fileCreate) {
				String bin = repSalFav.getBin();
				if (!bin.equals(binAux)) { 
					binAux = bin;
					bw.newLine();
					bw.write(bin);
					bw.newLine();
				}
				bw.write(repSalFav.getTotSaldoFavor() + "|" + repSalFav.getEdadCredito() + "|" + repSalFav.getCantidad());
				bw.newLine();
			}
			bw.close();
    	} catch (Exception ex){
    		logger.error(
					"Error escribiendo en el archivo Reporte Saldos Favor Consolidado" + ex.getMessage(), ex);
			;
    	}
	}
	
   private void creaReporteRetiradosDetalladoSaldoFavor(String fileNameReporteSaldosDetallado, List<ReporteSaldoFavor> fileCreate) {
	    
    	String fechaInicio = this.getPros().getProperty("FechaInicio").trim();
    	
    	try {
    		FileWriter fw = new FileWriter(fileNameReporteSaldosDetallado);
			BufferedWriter bw = new BufferedWriter(fw);
			
			String fechaArch = DateUtil.getDateFormFormat("dd/MM/yyyy");
			bw.write(fechaArch);
			bw.newLine();
			bw.write(fechaInicio + " - " +fechaArch);
			bw.newLine();bw.newLine();
			for (ReporteSaldoFavor repSalFav:fileCreate) {
				bw.write(repSalFav.getBin() + "|" + repSalFav.getNumerocredito() + "|" + repSalFav.getEdadCredito() + "|" + repSalFav.getSaldo());
				bw.newLine();
				logger.info(repSalFav.getBin() + "|" + repSalFav.getNumerocredito() + "|" + repSalFav.getEdadCredito() + "|" + repSalFav.getSaldo());
			}
			bw.close();
    	} catch (Exception ex){
    		logger.error(
					"Error escribiendo en el archivo Reporte Saldos Favor Detallado" + ex.getMessage(), ex);
			;
    	}
	}

}
