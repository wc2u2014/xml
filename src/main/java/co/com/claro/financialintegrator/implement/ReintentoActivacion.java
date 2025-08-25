package co.com.claro.financialintegrator.implement;

import java.sql.SQLException;
import java.util.Calendar;

import javax.sql.rowset.CachedRowSet;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class ReintentoActivacion extends GenericProccess {
	private Logger logger = Logger.getLogger(ReintentoActivacion.class);

	@Override
	public void process() {
		try {
                     UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
			logger.info("Proceso de Reintento Activaciones ");
			//Parametros
			String dataSourceBSCS = this.getPros()
					.getProperty("DatabaseDataSourceBSCS").trim();
			String dataSourceIntegrador = this.getPros()
					.getProperty("DatabaseDataSourceIntegrador").trim();
			String queryBSCS = this.getPros().getProperty("sqlBSCS");
			String queryParametros = "SELECT * from INT_PARAMETROS WHERE LLAVE='FECHA_REINTENTO'";
			String insert = this.getPros().getProperty("insertIntegrador");
			Database _databaseIntegrador = new Database();
			_databaseIntegrador.setDataSource(dataSourceIntegrador);
			//SE consulta ultima fecha Integrador
			CachedRowSet cr = _databaseIntegrador.execQuery(queryParametros,uid);
			String fecha="";
			if (cr!=null){				
				
				if (cr.next()){					
					fecha = cr.getString("VALOR");
								}
			}
			//Se la fecha de último proceso  existe se procesa 
			if (!fecha.equals("")){
				//se consulta informacion en BSCS
				logger.info("Reintento Activacion : " + dataSourceBSCS);
				Database _databaseBSCS = new Database();
				_databaseBSCS.setDataSource(dataSourceBSCS);
				//Se modifica query bscs
				queryBSCS = queryBSCS.replace(":FECHAPROCESO", ("'"+fecha+"'"));
				logger.info(" queryBSCS  "+ queryBSCS);
				cr = _databaseBSCS.execQuery(queryBSCS,uid);
				if (cr!=null){
					//Se borra tabla
					String _truncate =" TRUNCATE TABLE INT_ACTIVACIONES_BSCS ";
					_databaseIntegrador.updateQuery(_truncate,uid);
					//se ingresa datos
					_databaseIntegrador.insertQueryReintento(cr, insert,uid);
					logger.info("Se ha consultado información en bscs" + cr.size());					
					String fechaNueva = DateUtils.convertToString(Calendar.getInstance(), "yyyy-MM-dd");
					String updateFecha="UPDATE INT_PARAMETROS SET VALOR = '" + fechaNueva +"' WHERE LLAVE = 'FECHA_REINTENTO' ";				
					logger.info("Fecha Update "+updateFecha);	//Truncate table
					
					_databaseIntegrador.updateQuery(updateFecha,uid);
				}		
				try {
					cr.close();
				} catch (SQLException e) {
					logger.error("Error cerrando conexión "+e.getMessage());
				}				
				String fechaNueva = DateUtils.convertToString(Calendar.getInstance(), "yyyy-MM-dd");
				String updateFecha="UPDATE INT_PARAMETROS SET VALOR = '" + fechaNueva +"' WHERE LLAVE = 'FECHA_REINTENTO' ";				
				logger.info("Fecha Update "+updateFecha);	//Truncate table
				
				_databaseIntegrador.updateQuery(updateFecha,uid);
			
			}else{
				logger.error("Parametro [ FECHA_REINTENTO ] debe estar Configurando"  );
			}

		} catch (Exception ex) {
			logger.error("Error en reintento de activación "+ex.getMessage(),ex);
		}

	}

}
