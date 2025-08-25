package co.com.claro.financialintegrator.implement;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleTypes;

public class ReporteCentralesSegundaFactura extends GenericProccess {
	private Logger logger = Logger.getLogger(ReporteCentralesSegundaFactura.class);



	@Override
	public void process() {
		logger.info(" ** Reporte Centrales Segunda Factura V 1.0 ** ");
		  UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
		HashMap<String, String> prop = consultarPropiedadesBatch(uid);
		setPros(prop);
		

		ResultSet ciclosResultSet=null;
		ResultSet segundaFacturaResultSet=null;
		CallableStatement callableStatementCiclos=null;
		CallableStatement callableStatementSegundaFactura=null;
		Database database=null;
		String dataSource=null;
		String callCiclos = "";
		String callSegundaFactura = "";
		try {
            
			//Propiedades de base de datos, donde esta el data source.
			dataSource = prop.get("database.DatabaseDataSource");
			//Paquete de base de datos que va a a ejecutar.
			callCiclos = prop.get("database.callSegundaFacturaCiclos");
			callSegundaFactura = prop.get("database.callSegundaFactura");
			database = Database.getSingletonInstance(dataSource, null,uid);
            
			// Obtiene una conexión remota, solo para en caso de pruebas locales, para desplegar en el servidor utilizar getConnection.
			Connection connection=database.getConnectionRemote(uid);
			callableStatementCiclos = connection.prepareCall("{" + callCiclos + "}");
			logger.info(callCiclos);
			
			String resultCiclos= "";
			callableStatementCiclos.setString(1,resultCiclos);
			// cStmt.setNull(2,Types.REF);
			callableStatementCiclos.registerOutParameter(2, OracleTypes.CURSOR);
			
			callableStatementCiclos.execute();
			ciclosResultSet = (ResultSet) callableStatementCiclos.getObject(2);

			String pathFile = prop.get("file.path");
			String name = prop.get("file.name");
			

		
			long startTime = System.currentTimeMillis();
			// ... do something ...
			
			
			
			
			while (ciclosResultSet.next()) {
				String ciclo=ciclosResultSet.getString(1);
				logger.info("num Ciclo: "+ciclo);
				String resultSegundaFactura=null;
				callableStatementSegundaFactura = connection.prepareCall("{" + callSegundaFactura + "}");
				callableStatementSegundaFactura.setString(1,ciclosResultSet.getString(1));
				callableStatementSegundaFactura.setString(2,resultSegundaFactura);
				callableStatementSegundaFactura.registerOutParameter(3, OracleTypes.CURSOR);
				callableStatementSegundaFactura.execute();
				
				segundaFacturaResultSet = (ResultSet) callableStatementSegundaFactura.getObject(3);
				FileWriter fstream = new FileWriter(pathFile +ciclo+ name);
				BufferedWriter out = new BufferedWriter(fstream);
				logger.info(pathFile +ciclo+ name);
				
				while (segundaFacturaResultSet.next()) {
					
					String record="";
					
					//no esta en el pdf
					String referenciaPago= segundaFacturaResultSet.getString("referencia_pago");
					record = record+ String.format("%1$-" + 10 + "s\t", referenciaPago);
					
					//no esta en el pdf
					String custCodeResponsable= segundaFacturaResultSet.getString("custcode_responsable_pago");
					record = record+ String.format("%1$-" + 10 + "s\t", custCodeResponsable);
					
					String region= segundaFacturaResultSet.getString("region");
					record = record+ String.format("%1$-" + 10 + "s\t", region);
					
					String nombre= segundaFacturaResultSet.getString("nombre_persona");
					record = record+ String.format("%1$-" + 200 + "s\t", nombre);
					
					//String fecha_corte= segundaFacturaResultSet.getString("fecha_corte");
					//record = record+ String.format("%1$-" + 10 + "s", fecha_corte);
					
					String saldo= segundaFacturaResultSet.getString("saldo_cuenta");
					record = record+ String.format("%1$-" + 15 + "s\t", saldo);
					
					String direccion= segundaFacturaResultSet.getString("direccion_1");
					record = record+ String.format("%1$-" + 100 + "s\t", direccion);
					
					String ciudad= segundaFacturaResultSet.getString("ciudad");
					record = record+ String.format("%1$-" + 10 + "s\t", ciudad);
					
					String min= segundaFacturaResultSet.getString("min");
					record = record+ String.format("%1$-" + 10 + "s\t", min);
					
					String identificacion= segundaFacturaResultSet.getString("numero_identificacion");
					record = record+ String.format("%1$-" + 10 + "s\t", identificacion);
					
					//String credito= segundaFacturaResultSet.getString("credito");
					//record = record+ String.format("%1$-" + 10 + "s", credito);
					
					//No esta en el pdf
					String numProducto= segundaFacturaResultSet.getString("nro_producto");
					record = record+ String.format("%1$-" + 10 + "s\t", numProducto);
					
					logger.info(record);
					out.write(record);
					
					out.newLine();
				}
				
				out.close();
			}
			
			long estimatedTime = System.currentTimeMillis() - startTime;
			logger.info("Tiempo de escritura"+estimatedTime);
			

		} catch (Exception ex) {
			logger.error("Error leyendo propiedades " + ex.getMessage(), ex);
			return;
		} finally {
            try{
			if (ciclosResultSet != null) {
				ciclosResultSet.close();
			}

			if (callableStatementCiclos != null) {
				callableStatementCiclos.close();
			}
			
			if (segundaFacturaResultSet != null) {
				segundaFacturaResultSet.close();
			}

			if (callableStatementSegundaFactura != null) {
				callableStatementSegundaFactura.close();
			}
			

			if (database != null) {
				database.disconnet(uid);
			}
            }catch(Exception e){
            	e.printStackTrace();
            }

		}

	}

	public static void main(String[] args) {

		// Para probar localmente
		
		//Se setea esta variable para la ruta local.
		System.setProperty("PATH_PROPERTIES_INTEGRATOR", "D:/Oracle/Clientes/Claro/Integrador/Properties");

		//Ruta para arrancar el contexto xml.
		new ClassPathXmlApplicationContext(
				"spring/ReporteCentralesSegundaFactura/reporteCentralesSegundaFactura-config.xml");
		
		
	}
	

}
