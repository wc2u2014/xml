package co.com.claro.financialintegrator.util;

import org.apache.log4j.Logger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionLogger {

    private static final Logger logger = Logger.getLogger(TransactionLogger.class);
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    // Mapa para almacenar tiempos de inicio por UID
    private static final ConcurrentHashMap<String, Long> tiempoInicioMap = new ConcurrentHashMap<>();

    private static String getUidOrGenerate(String uid) {
        return (uid == null || uid.trim().isEmpty())
                ? "ID-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000)
                : uid;
    }

    public static void logStart(String adapterName, String uid, String archivo, String origen) {
        uid = getUidOrGenerate(uid);
        long startTime = System.currentTimeMillis();
        tiempoInicioMap.put(uid, startTime);

        String time = formatter.format(new Date(startTime));
        logger.info("[INFO ][" + adapterName + "][" + time + "][Service][" + uid + "][--------------------------------------------START TRANSACTION--------------------------------------------]");
        logger.info("[INFO ][" + adapterName + "][" + time + "][Service][" + uid + "][REQUEST| OPERATION_NAME: " + adapterName + " | XML: <Archivo><Nombre>" + archivo + "</Nombre><Origen>" + origen + "</Origen></Archivo>]");
    }

    public static void logSuccess(String adapterName, String uid, String destino) {
        uid = getUidOrGenerate(uid);
        long endTime = System.currentTimeMillis();
        long startTime = tiempoInicioMap.getOrDefault(uid, endTime);
        long durationMs = endTime - startTime;
        tiempoInicioMap.remove(uid); // Limpieza

        String time = formatter.format(new Date(endTime));
        logger.info("[INFO ][" + adapterName + "][" + time + "][Service][" + uid + "][RESPONSE| OPERATION_NAME: " + adapterName + " | XML: <Resultado><Destino>" + destino + "</Destino><Estado>OK</Estado></Resultado>]");
        logger.info("[INFO ][" + adapterName + "][" + time + "][Service][" + uid + "][TIME|LEGADO: " + adapterName + " | METODO: " + adapterName + " | [" + durationMs + "] ms]");
        logger.info("[INFO ][" + adapterName + "][" + time + "][Service][" + uid + "][---------------------------------------------END TRANSACTION---------------------------------------------]");
    }

    public static void logFailure(String adapterName, String uid, String mensaje, Throwable e) {
        uid = getUidOrGenerate(uid);
        tiempoInicioMap.remove(uid); // Se elimina aunque falle

        String time = formatter.format(new Date());
        logger.error("[ERROR][" + adapterName + "][" + time + "][Service][" + uid + "][FAILURE| OPERATION_NAME: " + adapterName + " | MENSAJE: " + mensaje + "]", e);
    }
}
