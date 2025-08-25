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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.ConsultaUsuariosOIM;
import co.com.claro.financialintegrator.domain.ReprocesoImei;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;

public class ImeiReprocesados extends GenericProccess {	private Logger logger = Logger.getLogger(ImeiReprocesados.class);



/**
 * Proceso del archivo para generar archivo de activaciones
 */
@Override
public void process() {
	logger.info(".............. Iniciando proceso ImeiReprocesados.................. ");
	Database _database = null;

	// TODO Auto-generated method stub
            UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
	if (!inicializarProps(uid)) {
		logger.info(" ** Don't initialize properties ** ");
		return;
	}
	try {

		guardarConsultaBD(uid);
	
	} catch (

	Exception e) {
		logger.error("Excepcion no Controlada  en proceso Imei Reprocesados " + e.getMessage(), e);
	}

	CallableStatement cs = null;

	
	try {

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
		
		_database.setCall(this.getPros().getProperty("callConsultaImeiReprocesados").trim());
		List<ReprocesoImei> listaImeiConsulta = new ArrayList<ReprocesoImei>();

			List<Object> input = new ArrayList<Object>();
			List<Integer> output = new ArrayList<Integer>();
			output.add(OracleTypes.VARCHAR);
			output.add(OracleTypes.CURSOR);
			cs = _database.executeCallOutputs(_database.getConn(uid),
					output, input,uid);	
			logger.info("Ejecuta reporte callConsultaImeiReprocesados) "+cs);
			if (cs != null) {
				String result = cs.getString(1);
				logger.info("result:"+result);
				if("TRUE".equals(result)){
					String record="";
					ResultSet rs = (ResultSet) cs.getObject(2);
					long startTime = System.currentTimeMillis();
					if(rs.next()){
						do {
							String imei= rs.getString("IMEI");
							Date fecha= rs.getDate("FECHA_RECHAZO");
							String usuario= rs.getString("USUARIO_CARGUE");	
							String motivoFallo= rs.getString("MOTIVO_FALLO");		
							ReprocesoImei imeiConsulta= new ReprocesoImei(imei,fecha,usuario,motivoFallo,"");
							listaImeiConsulta.add(imeiConsulta);
						} while (rs.next());
						
	
					}
				
				long estimatedTime = System.currentTimeMillis() - startTime;
				logger.info("Tiempo de escritura"+estimatedTime);
				} else {
					logger.info("No se pudo ejecutar el reporte del dia ConsultaImeiReprocesados");
					registrar_auditoriaV2("REPORTE ConsultaImeiReprocesados", "No se pudo ejecutar el reporte del dia ",uid);
				}
				
			}
				
			
			for(ReprocesoImei cimei:listaImeiConsulta) {
			
			enviaRegistroProcedureBD(cimei.getImei(),cimei.getFechaRechazo(),cimei.getUsuario(),cimei.getMotivoFallo(),uid);
			}
			
	} catch (Exception e) {
		logger.error("Error generando reporte ConsultaImeiReprocesados",e);
		registrar_auditoriaV2("ConsultaImeiReprocesados", "Error generando ConsultaImeiReprocesados",uid);
	} finally {
		_database.disconnetCs(uid);
		_database.disconnet(uid);

	}
	
	
	
}

private void guardarConsultaBD(String uid) {
	Database _database2 = null;
	try {
		String dataSource = this.getPros()
				.getProperty("DatabaseDataSource").trim();
		// urlWeblogic = null;

		_database2 = Database.getSingletonInstance(dataSource, null,uid);
		logger.debug("dataSource " + _database2);
		// logger.debug("urlWeblogic " + urlWeblogic);
	} catch (Exception ex) {
		logger.error(
				"Error obteniendo informaci�n de  configuracion "
						+ ex.getMessage(), ex);
	}

	

	CallableStatement cs = null;
	try {
		// Se invoca procedimiento
		_database2.setCall(this.getPros().getProperty("callRegistrar").trim());
		List<Object> input = new ArrayList<Object>();
		List<Integer> output = new ArrayList<Integer>();
		output.add(java.sql.Types.NUMERIC);
		output.add(java.sql.Types.VARCHAR);
		cs = _database2.executeCallOutputs(_database2.getConn(uid), output,
				input,uid);
		if (cs != null) {
			logger.info("Call : " + this.getPros().getProperty("callRegistrar").trim() + " - P_EXITO : "
					+ cs.getInt(1) + " - P_ERROR : " + cs.getString(2));
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
	_database2.disconnet(uid);
	_database2.disconnetCs(uid);		
}




private void actualizarConsultaBD(String imei,Date fechaRechazo,String estado,String salidaProcedimiento ,String uid) {
	Database _database2 = null;
	try {
		String dataSource = this.getPros()
				.getProperty("DatabaseDataSource").trim();
		// urlWeblogic = null;

		_database2 = Database.getSingletonInstance(dataSource, null,uid);
		logger.debug("dataSource " + dataSource);
		// logger.debug("urlWeblogic " + urlWeblogic);
	} catch (Exception ex) {
		logger.error(
				"Error obteniendo informaci�n de  configuracion "
						+ ex.getMessage(), ex);
	}

	

	CallableStatement cs = null;
	try {
		// Se invoca procedimiento
		_database2.setCall(this.getPros().getProperty("callUpdate").trim());
		List<Object> input = new ArrayList<Object>();
		input.add(imei);
		input.add(salidaProcedimiento);		
		input.add(estado);
		input.add(fechaRechazo);

		List<Integer> output = new ArrayList<Integer>();
		output.add(java.sql.Types.NUMERIC);
		output.add(java.sql.Types.VARCHAR);
		cs = _database2.executeCallOutputs(_database2.getConn(uid), output,
				input,uid);
		if (cs != null) {
			logger.info("Call : " + this.getPros().getProperty("callUpdate").trim() + " - P_EXITO : "
					+ cs.getInt(5) + " - P_ERROR : " + cs.getString(6));
		}
	} catch (SQLException e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callRegistrar").trim() + " : " + e.getMessage(),
				e);
	} catch (Exception e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callRegistrar").trim() + " : " + e.getMessage(),
				e);
	}
	
	finally {
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
		_database2.disconnet(uid);
		_database2.disconnetCs(uid);		
}


private void enviaRegistroProcedureBD(String imei,Date fechaRechazo,String usuario,String motivoFalla,String uid ) {
	Database _databaseA2 = null;
	String estado="";
	try {
		String dataSource = this.getPros()
				.getProperty("DatabaseDataSourceA2").trim();
		// urlWeblogic = null;

		_databaseA2 = Database.getSingletonInstance(dataSource, null,uid);
		logger.debug("dataSource " + dataSource);
		// logger.debug("urlWeblogic " + urlWeblogic);
	} catch (Exception ex) {
		logger.error(
				"Error obteniendo informaci�n de  configuracion "
						+ ex.getMessage(), ex);
	}

	

	CallableStatement cs = null;
	try {
		
		

		// Se invoca procedimiento
		_databaseA2.setCall(this.getPros().getProperty("callRegistrarProcedureA2").trim());
		List<Object> input = new ArrayList<Object>();
		input.add(imei);
		
		input.add(fechaRechazo);
		input.add(usuario);
		input.add(motivoFalla);
		List<Integer> output = new ArrayList<Integer>();
		output.add(java.sql.Types.VARCHAR);
		cs = _databaseA2.executeCallOutputsV3(_databaseA2.getConn(uid), output,
				input,uid);
		if (cs != null) {
			logger.info("Call : " + this.getPros().getProperty("callRegistrarProcedureA2").trim() + " - P_SALIDA : "
					+ cs.getString(1) );
			String Salida=cs.getString(1) ;
			estado="Procesado";
			actualizarConsultaBD(imei,fechaRechazo,estado,Salida,uid);
		}
	} catch (SQLException e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callRegistrarProcedureA2").trim() + " : " + e.getMessage(),
				e);
		estado="Error";
		actualizarConsultaBD(imei,fechaRechazo,estado,e.getMessage(),uid);
	} catch (Exception e) {
		logger.error(
				"ERROR call : " + this.getPros().getProperty("callRegistrarProcedureA2").trim() + " : " + e.getMessage(),
				e);
		estado="Error";
		actualizarConsultaBD(imei,fechaRechazo,estado,e.getMessage(),uid);
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
	_databaseA2.disconnet(uid);
	_databaseA2.disconnetCs(uid);		
}



}
