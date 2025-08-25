
package co.com.claro.financialintegrator.implement;

import java.sql.CallableStatement;
import java.sql.Connection;
import org.apache.log4j.Logger;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;
import oracle.jdbc.OracleTypes;

public class RegistrarRespuestaNovedades extends GenericProccess {
	private Logger logger = Logger.getLogger(RegistrarRespuestaNovedades.class);
	
	@Override
	public void process() {
    long startTime = System.currentTimeMillis();
    logger.info("Iniciando método: process");
    logger.info("Request: N/A");
    UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
    if (!inicializarProps(uid)) {
        logger.info("** No se inicializan las propiedades **");
        logger.info("Response: N/A");
        logger.info("Tiempo de ejecución del método process: " + (System.currentTimeMillis() - startTime) + " ms");
        return;
    }

    CallableStatement csNovedades = null;
    Database databaseNovedades = null;
    String dataSourceNovedades = null;
    String callRegistrarNovedades = null;
    Connection connectionNovedades = null;
    String exitoNovedades = null;

    try {
        logger.info("Propiedades cargadas: " + this.getPros());
        String nombreCampo = this.getPros().getProperty("nombre_campo");
        String valorCampo = this.getPros().getProperty("valor_campo");
        logger.info("Request: nombreCampo = " + nombreCampo + ", valorCampo = " + valorCampo);

        dataSourceNovedades = this.getPros().getProperty("DatabaseDataSourceIntegrador").trim();
        callRegistrarNovedades = this.getPros().getProperty("callRegistrarRespuestaNovedades");

        databaseNovedades = Database.getSingletonInstance(dataSourceNovedades, null,uid);
        connectionNovedades = databaseNovedades.getConnection(uid);

        csNovedades = connectionNovedades.prepareCall(callRegistrarNovedades);
        csNovedades.setString(1, nombreCampo);
        csNovedades.setString(2, valorCampo);
        csNovedades.registerOutParameter(3, OracleTypes.VARCHAR);
        csNovedades.execute();

        exitoNovedades = csNovedades.getString(3);

        if ("TRUE".equals(exitoNovedades)) {
            logger.info("** Registro de novedades correctamente **");
        }

        String observacion = "Archivo Procesado Exitosamente";
        String NombreProceso = "Registrar novedades";
        registrar_auditoriaV2(NombreProceso, observacion,uid);

        logger.info("Response: " + (exitoNovedades != null ? exitoNovedades : "N/A"));

    } catch (Exception e) {
        logger.error("Error registrando novedades", e);
        String observacion = "No se ha Procesado el Archivo con éxito";
        String NombreProceso = "Registrar novedades";
        registrar_auditoriaV2(NombreProceso, observacion,uid);
        logger.info("Response: N/A");
    } finally {
        if (databaseNovedades != null) {
            databaseNovedades.disconnet(uid);
        }
        logger.info("Tiempo de ejecución del método process: " + (System.currentTimeMillis() - startTime) + " ms");
    }
        }
}