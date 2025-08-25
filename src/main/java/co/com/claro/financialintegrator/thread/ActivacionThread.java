package co.com.claro.financialintegrator.thread;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;


public class ActivacionThread extends GenericProccess implements Job{
	private Logger logger = Logger.getLogger(ActivacionThread.class);
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		JobDataMap map=  context.getJobDetail().getJobDataMap();
		String dataSource = map.getString("DatabaseDataSource");
		String callTruncateAjustesAscard = map.getString("callTruncateAjustesAscard");
		try {
			truncateAjustesAscard(dataSource, callTruncateAjustesAscard,uid);
		} catch (FinancialIntegratorException e) {
			logger.error("Error depurando tabla de ajustes claro ",e);
		}
		
	}

	@Override
	public void process() {		
	}
	
	/**
	 * Consulta si el proceso ya fue ejecutado
	 * @param dataSource
	 * @param reporte
	 * @param callConsultaEjecucionBloqueo
	 * @return
	 * @throws FinancialIntegratorException
	 */
	private boolean truncateAjustesAscard(String dataSource, String callDepuracionControl,String uid) throws FinancialIntegratorException{
		Database _database = getDatabase(dataSource);
		boolean estado= false;
		if (_database!=null){
			_database.setCall(callDepuracionControl);
			//
			List<Integer> output = new ArrayList<Integer>();
			//
			List<Object> input = new ArrayList<Object>();	

			try {
				_database.executeCallOutputs( output,
						input,uid);
			} catch (Exception ex) {
				_database.disconnetCs(uid);
				_database.disconnet(uid);
				logger.error("Error obteniendo configuracion " + ex.getMessage(),ex);
				throw new FinancialIntegratorException(ex.getMessage());  
				
			}	
			_database.disconnetCs(uid);
			_database.disconnet(uid);
				
		}
		return estado;
	}	
	
	/**
	 * se obtiene configuracion de base de datos
	 * @param dataSource
	 * @return
	 */
	private Database getDatabase(String dataSource){
		try {
			logger.debug("dataSource " + dataSource);
			return new Database(dataSource);
		} catch (Exception ex) {
			logger.error("Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
			return null;
		}		
	}	

}
