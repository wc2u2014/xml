package co.com.claro.financialintegrator.thread;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.DateUtils;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;

public class AplicacionPagoBSCSThread{
	private Logger logger = Logger.getLogger(AplicacionPagoBSCSThread.class);
	private String callBSCSAvisosPago;
	private String formatDates; 
	private Connection bscsConnection;
	private FileOuput line;
	
	/**
	 * Hilo para procesar una peticion de avisos pagos
	 * @param callBSCSAvisosPago
	 * @param bscsConnection
	 * @param line
	 */
	public AplicacionPagoBSCSThread(String callBSCSAvisosPago,
			Connection bscsConnection, String formatDates ,FileOuput line) {
		super();
		this.callBSCSAvisosPago = callBSCSAvisosPago;
		this.bscsConnection = bscsConnection;
		this.line = line;
		this.formatDates=formatDates;
	}
	/**
	 * metodo que ejecuta el Hilo
	 */
	private void execute(){
                UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		logger.info("Ejecutando hilos de procedimiento BSCS .. ");
		try {
			String REFERENCIA_PAGO = line.getType("REFERENCIA_PAGO").getValueString().trim();
			String NUMERO_CREDITO = line.getType("NUMERO_CREDITO").getValueString().trim();
			String CUSTOMER_ID =line.getType("CUSTOMER_ID").getValueString().trim();
			String CUTSCODE_RESPONSEBLE_PAGO =line.getType("CUTSCODE_RESPONSEBLE_PAGO").getValueString().trim();
			String NUMEROFACTURA =line.getType("NUMEROFACTURA").getValueString().trim();
			String FECHA_FACTURACION =line.getType("FECHA_FACTURACION").getValueString().trim();
			String FECHA_LIMITE_PAGO =line.getType("FECHA_LIMITE_PAGO").getValueString().trim();
			String CICLO_USUARIO =line.getType("CICLO_USUARIO").getValueString().trim();
			String MONTO_TOTAL_FACTURA =line.getType("MONTO_TOTAL_FACTURA").getValueString().trim();
			String MONTO_INTERES_FACTURA =line.getType("MONTO_INTERES_FACTURA").getValueString().trim();
			String MONTO_MORA_FACTURA =line.getType("MONTO_MORA_FACTURA").getValueString().trim();
			String TOTAL_ADECUADO =line.getType("TOTAL_ADECUADO").getValueString().trim();			
			BigDecimal eqDiasMora = new BigDecimal(0);
			String MORA = line.getType("MORA").getValueString().trim();
			
		
			String request ="E_c_id_ascard: "+NUMERO_CREDITO
			+",E_c_customer_id:"+CUSTOMER_ID
			+",E_c_custcode:"+CUTSCODE_RESPONSEBLE_PAGO
			+",E_c_num_factura:"+NUMEROFACTURA
			+",E_n_referencia_pago:"+REFERENCIA_PAGO
			+",E_f_gene_fact:"+FECHA_FACTURACION
			+",E_f_limit_pago:"+FECHA_LIMITE_PAGO
			+",E_i_monto_factura:"+MONTO_TOTAL_FACTURA
			+",E_i_total_adeudado:"+TOTAL_ADECUADO
			+",E_i_monto_inter_factura:"+MONTO_INTERES_FACTURA
			+",E_q_dias_mora:"+eqDiasMora
			+",E_q_vector_mora:"+MORA;
			
					
			Database  _databasebscs =new Database();
			_databasebscs.setCall(callBSCSAvisosPago);
			//Si el procesimiendo es diferente a vacio 
			if (callBSCSAvisosPago!=null && !callBSCSAvisosPago.equals("") ){
				List<Object> parameter = new ArrayList<Object>();
				//
				parameter.add(NUMERO_CREDITO);
				parameter.add( NumberUtils.convertStringTOBigDecimal(CUSTOMER_ID));
				parameter.add(CUTSCODE_RESPONSEBLE_PAGO);
				if (!NUMEROFACTURA.trim().equals("")){
					parameter.add(NUMEROFACTURA);
				}
				else{
					parameter.add(null);
				}				
				parameter.add(REFERENCIA_PAGO);
				parameter.add(FECHA_FACTURACION);			
				parameter.add(FECHA_LIMITE_PAGO);
				parameter.add(NumberUtils.convertStringTOBigDecimal(MONTO_TOTAL_FACTURA));
				parameter.add(NumberUtils.convertStringTOBigDecimal(TOTAL_ADECUADO));
				parameter.add(NumberUtils.convertStringTOBigDecimal(MONTO_INTERES_FACTURA));
				parameter.add(eqDiasMora);
				parameter.add(NumberUtils.convertStringTOBigDecimal(MORA));
				//
				List<Integer> typesOut = new ArrayList<Integer>();
				typesOut.add(java.sql.Types.NUMERIC);
				typesOut.add(java.sql.Types.VARCHAR);
				CallableStatement cs=null;
				try{
					cs=_databasebscs.executeCallOutputs(bscsConnection,typesOut, parameter,uid);
					if(cs!=null){
						logger.debug("REQUEST : "+request+" ,Codigo Respuesta : "+cs.getString(parameter.size()+1)+", Descripcion Respuesta : "+"Codigo Respuesta : "+cs.getString(parameter.size()+2));
					}
				} catch (SQLException e) {
					logger.info("ERROR REQUEST : "+request+" : "+e.getMessage());
					logger.error("Error ejecutando procedimeinto en BSCS "+e.getMessage(),e);
				}catch (Exception e) {
					logger.info("ERROR REQUEST : "+request+" : "+e.getMessage());
					logger.error("Error ejecutando procedimeinto en BSCS "+e.getMessage(),e);
				}finally{
					if(cs!=null){
						try {
							cs.close();
						} catch (SQLException e) {
							logger.error("Error cerrando CallebaleStament BSCS "+e.getMessage(),e);					
						}
					}
				}
				
			}
			
			
		} catch (FinancialIntegratorException e) {
			logger.error("Error procesando linea "+e.getMessage(),e);			
		} catch (Exception e) {
			logger.error("Error procesando linea "+e.getMessage(),e);
		}
		
	}
	public void run() {
		// TODO Auto-generated method stub
		 execute();
	}

}
