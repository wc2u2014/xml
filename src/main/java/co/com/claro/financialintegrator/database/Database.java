package co.com.claro.financialintegrator.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;

import org.apache.log4j.Logger;
import org.springframework.jndi.JndiObjectFactoryBean;

import co.com.claro.financialintegrator.implement.ProcesoCobranzas;

import com.sun.rowset.CachedRowSetImpl;

/**
 * Clase de conexi?n a la base de datos
 *
 * @author Carlos Guzman
 *
 */
public class Database {

    private static Logger logger = Logger.getLogger(Database.class);
    /**
     * nombre del dataSource en el servidor weblogic
     */
    private String dataSource;
    /**
     * conexi?n al Weblogic
     */
    private String urlWeblogic;
    /**
     * conexi?n a la base de datos
     */
    private Connection conn;
    private String call;
    private OracleCallableStatement cs;
    private static Database _database;

    /**
     * se inicializa los parametros de conexi?n
     *
     * @param dataSource
     * @param urlWeblogic
     */
    private Database(String dataSource, String urlWeblogic) {
        super();
        this.dataSource = dataSource;
        this.urlWeblogic = urlWeblogic;
        //this._connection();
    }

    public Database(String dataSource) {
        super();
        this.dataSource = dataSource;
        //this._connection();
    }

    public Database() {

    }

    /*
	 *se obtienen conexiones 
     */
    public static Connection getConnection(String dataSourceSTR, String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][getConnection              ][REQUEST| OPERATION_NAME: getConnection | dataSourceSTR = " + (dataSourceSTR != null ? dataSourceSTR : "N/A") + "]");

        try {
            Context initctx = new InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) initctx.lookup(dataSourceSTR);
            Connection conn = ds.getConnection();
            logger.info(uid + " | [Database                       ][getConnection              ][RESPONSE| OPERATION_NAME: getConnection | Resultado: Conexión obtenida correctamente]");
            return conn;
        } catch (NamingException e1) {
            logger.error(uid + " | [Database                       ][getConnection              ][ERROR| OPERATION_NAME: getConnection | Error leyendo dataSource]", e1);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][getConnection              ][ERROR| OPERATION_NAME: getConnection | Error general obteniendo conexión desde dataSource]", e);
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][getConnection              ][TIME| OPERATION_NAME: getConnection | " + (endTime - startTime) + " ms]");
}

        logger.info(uid + " | [Database                       ][getConnection              ][RESPONSE| OPERATION_NAME: getConnection | Resultado: Conexión: null]");
        return null;
    }

    /**
     * Retorna el mismo Objeto, Patron Singleton
     *
     * @param dataSource
     * @param urlWeblogic
     * @return
     */
    public static Database getSingletonInstance(String dataSource, String urlWeblogic,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][getSingletonInstance       ][REQUEST| OPERATION_NAME: getSingletonInstance | dataSource = " + dataSource + ", urlWeblogic = " + urlWeblogic + "]");

        try {
            if (_database == null) {
                _database = new Database(dataSource, urlWeblogic);
                logger.info(uid + " | [Database                       ][getSingletonInstance       ][INFO| Se creó nueva instancia de Database]");
            } else {
                _database.disconnet(uid);
                _database = new Database(dataSource, urlWeblogic);
                logger.info(uid + " | [Database                       ][getSingletonInstance       ][INFO| Se reinicializó instancia de Database]");
            }

            logger.info(uid + " | [Database                       ][getSingletonInstance       ][RESPONSE| OPERATION_NAME: getSingletonInstance | Resultado: instancia retornada correctamente]");
            return _database;

        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][getSingletonInstance       ][ERROR| OPERATION_NAME: getSingletonInstance | Error creando o reiniciando instancia]", e);
            logger.info(uid + " | [Database                       ][getSingletonInstance       ][RESPONSE| OPERATION_NAME: getSingletonInstance | Resultado: null]");
            return null;
        } finally {
            logger.info(uid + " | [Database                       ][getSingletonInstance       ][TIME| OPERATION_NAME: getSingletonInstance | " + (System.currentTimeMillis() - startTime) + " ms]");
}
    }

    /**
     * Retorna el mismo Objeto, Patron Singleton
     *
     * @return
     */
    public static Database getSingletonInstance(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][getSingletonInstance       ][REQUEST| OPERATION_NAME: getSingletonInstance | Parámetros: N/A]");

        try {
            if (_database == null) {
                _database = new Database();
                logger.info(uid + " | [Database                       ][getSingletonInstance       ][INFO| Se creó nueva instancia de Database]");
            } else {
                logger.info(uid + " | [Database                       ][getSingletonInstance       ][INFO| Instancia existente retornada]");
            }

            logger.info(uid + " | [Database                       ][getSingletonInstance       ][RESPONSE| OPERATION_NAME: getSingletonInstance | Resultado: instancia retornada correctamente]");
            return _database;

        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][getSingletonInstance       ][ERROR| OPERATION_NAME: getSingletonInstance | Error creando instancia]", e);
            logger.info(uid + " | [Database                       ][getSingletonInstance       ][RESPONSE| OPERATION_NAME: getSingletonInstance | Resultado: null]");
            return null;
        } finally {
            logger.info(uid + " | [Database                       ][getSingletonInstance       ][TIME| OPERATION_NAME: getSingletonInstance | " + (System.currentTimeMillis() - startTime) + " ms]");
}
    }

    /**
     * retorna una nueva conexion
     *
     * @return
     */
    public Connection getConnection(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][getConnection              ][REQUEST| OPERATION_NAME: getConnection | this.dataSource = " + (this.dataSource != null ? this.dataSource : "N/A") + "]");

        try {
            Context initctx = new InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) initctx.lookup(this.dataSource);
            Connection conn = ds.getConnection();
            logger.info(uid + " | [Database                       ][getConnection              ][RESPONSE| OPERATION_NAME: getConnection | Resultado: Conexión obtenida correctamente]");
            return conn;

        } catch (NamingException e1) {
            logger.error(uid + " | [Database                       ][getConnection              ][ERROR| NamingException obteniendo dataSource]", e1);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][getConnection              ][ERROR| Excepción general obteniendo conexión]", e);
        } finally {
            logger.info(uid + " | [Database                       ][getConnection              ][TIME| OPERATION_NAME: getConnection | " + (System.currentTimeMillis() - startTime) + " ms]");
}

        logger.info(uid + " | [Database                       ][getConnection              ][RESPONSE| OPERATION_NAME: getConnection | Resultado: Conexión: null]");
        return null;
    }

    public Connection getConnectionRemote(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][getConnectionRemote        ][REQUEST| OPERATION_NAME: getConnectionRemote | this.dataSource = " + (this.dataSource != null ? this.dataSource : "N/A") + "]");

        Context initctx;
        Connection conn = null;

        try {
            weblogic.jndi.Environment environment = new weblogic.jndi.Environment();
            environment.setInitialContextFactory(weblogic.jndi.Environment.DEFAULT_INITIAL_CONTEXT_FACTORY);
            environment.setProviderURL("t3://localhost:7110");
            environment.setSecurityPrincipal("EOC6658A");
            // environment.setSecurityCredentials("P13sch4c0n");

            initctx = environment.getInitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) initctx.lookup(this.dataSource);
            conn = ds.getConnection();

            logger.info(uid + " | [Database                       ][getConnectionRemote        ][RESPONSE| OPERATION_NAME: getConnectionRemote | Resultado: Conexión obtenida correctamente]");
            return conn;

        } catch (NamingException e1) {
            logger.error(uid + " | [Database                       ][getConnectionRemote        ][ERROR| NamingException leyendo dataSource]", e1);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][getConnectionRemote        ][ERROR| Excepción general leyendo dataSource]", e);
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][getConnectionRemote        ][TIME| OPERATION_NAME: getConnectionRemote | " + (endTime - startTime) + " ms]");
}

        logger.info(uid + " | [Database                       ][getConnectionRemote        ][RESPONSE| OPERATION_NAME: getConnectionRemote | Resultado: Conexión: null]");
        return null;
    }

    /**
     * Se establece la conexi?n al DataSource de cobranzas
     *
     * @return
     */
    private void _connection(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][_connection                ][REQUEST| OPERATION_NAME: _connection | this.dataSource = " + (this.dataSource != null ? this.dataSource : "N/A") + "]");

        try {
            Context initctx = new InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) initctx.lookup(this.dataSource);
            this.conn = ds.getConnection();
            logger.info(uid + " | [Database                       ][_connection                ][RESPONSE| OPERATION_NAME: _connection | Resultado: Conexión obtenida correctamente: " + this.conn + "]");
        } catch (NamingException e1) {
            logger.error(uid + " | [Database                       ][_connection                ][ERROR| NamingException leyendo dataSource]", e1);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][_connection                ][ERROR| Excepción general leyendo dataSource]", e);
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][_connection                ][TIME| OPERATION_NAME: _connection | " + (endTime - startTime) + " ms]");
}
    }

    private ARRAY _convert(Object someArray,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][_convert                   ][REQUEST| OPERATION_NAME: _convert | someArray = " + (someArray != null ? someArray.getClass().getName() : "null") + "]");

        try {
            ARRAY ar;
            if (someArray instanceof weblogic.jdbc.wrapper.Array) {
                ar = (oracle.sql.ARRAY) (((weblogic.jdbc.wrapper.Array) someArray)
                        .unwrap(Class.forName("oracle.sql.ARRAY")));
            } else {
                ar = (oracle.sql.ARRAY) someArray;
            }

            logger.info(uid + " | [Database                       ][_convert                   ][RESPONSE| OPERATION_NAME: _convert | Resultado: ARRAY convertido con éxito]");
            return ar;

        } catch (Exception ex) {
            logger.error(uid + " | [Database                       ][_convert                   ][ERROR| Error de conversión de array]", ex);
        } finally {
            logger.info(uid + " | [Database                       ][_convert                   ][TIME| OPERATION_NAME: _convert | " + (System.currentTimeMillis() - startTime) + " ms]");
}

        return null;
    }

    /**
     * execite call with state result
     *
     * @param arrays
     * @return
     */
    public Boolean executeCallState(List<ARRAY> arrays, List<String> other, Boolean checknoResult,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallState           ][REQUEST| OPERATION_NAME: executeCallState | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallState           ][REQUEST| OPERATION_NAME: executeCallState | arrays.size = " + (arrays != null ? arrays.size() : 0) + "]");
        logger.info(uid + " | [Database                       ][executeCallState           ][REQUEST| OPERATION_NAME: executeCallState | other.size = " + (other != null ? other.size() : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallState           ][REQUEST| OPERATION_NAME: executeCallState | checknoResult = " + (checknoResult != null ? checknoResult : "N/A") + "]");

        int pos = 1;
        int result = 0;
        int cantidad = 0;
        int idx = 0;

        try {
            cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
            cs.clearParameters();

            for (ARRAY _array : arrays) {
                try {
                    logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Set ARRAY | pos = " + pos + ", type = " + _array.getSQLTypeName() + "]");
                    cs.setARRAY(pos, _array);
                    pos++;
                } catch (Exception ex) {
                    logger.error(uid + " | [Database                       ][executeCallState           ][ERROR| Fallo set ARRAY en posición " + pos + "]", ex);
                    return false;
                }
            }

            if (other != null) {
                for (String oth : other) {
                    logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Set STRING | pos = " + pos + ", valor = " + oth + "]");
                    cs.setString(pos, oth);
                    pos++;
                }
            }

            result = pos;
            logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Registro de OUT param VARCHAR en pos = " + pos + "]");
            cs.registerOutParameter(pos, OracleTypes.VARCHAR);
            pos++;

            if (Boolean.TRUE.equals(checknoResult)) {
                cantidad = pos;
                cs.registerOutParameter(pos, OracleTypes.NUMBER);
                logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Registro de OUT param NUMBER en pos = " + pos + "]");
                pos++;

                cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
                int codError = pos;
                logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Registro de OUT param ARRAY (codError) en pos = " + codError + "]");
                pos++;

                idx = pos;
                cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
                logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Registro de OUT param ARRAY (idx) en pos = " + idx + "]");
                pos++;
            }

            cs.execute();

            String respuesta = cs.getString(result);
            logger.info(uid + " | [Database                       ][executeCallState           ][RESPONSE| OPERATION_NAME: executeCallState | Resultado: " + respuesta + "]");

            if (Boolean.TRUE.equals(checknoResult) && cantidad > 0) {
                logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Cantidad líneas no procesadas: " + cs.getString(cantidad) + "]");
                ARRAY idxArray = _convert(cs.getArray(idx),uid);
                BigDecimal[] arrIdx = (BigDecimal[]) idxArray.getArray();
                StringBuilder sb = new StringBuilder();
                for (BigDecimal arr : arrIdx) {
                    sb.append(arr).append(",");
                }
                logger.info(uid + " | [Database                       ][executeCallState           ][INFO| Índices de líneas no procesadas: " + sb.toString() + "]");
            }

            return true;

        } catch (Exception ex) {
            logger.error(uid + " | [Database                       ][executeCallState           ][ERROR| Excepción durante la ejecución del procedimiento: " + this.call + "]", ex);
            return false;
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCallState           ][ERROR| Cerrando CallableStatement]", e);
            }
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][executeCallState           ][TIME| OPERATION_NAME: executeCallState | Duración: " + (endTime - startTime) + " ms]");
}
    }

    /**
     * Ejecuta procedimiento con una lista de array
     *
     * @param arrays
     */
    public HashMap<String, Object> executeCallSate(List<ARRAY> arrays,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallSate            ][REQUEST| OPERATION_NAME: executeCallSate | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallSate            ][REQUEST| OPERATION_NAME: executeCallSate | arrays.size = " + (arrays != null ? arrays.size() : "N/A") + "]");

        int pos = 1;

        try {
            cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);

            for (ARRAY _array : arrays) {
                try {
                    logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Set ARRAY | pos = " + pos + ", type = " + _array.getSQLTypeName() + "]");
                    cs.setARRAY(pos, _array);
                    pos++;
                } catch (Exception ex) {
                    logger.error(uid + " | [Database                       ][executeCallSate            ][ERROR| Fallo al inicializar ARRAY en pos = " + pos + "]", ex);
                    return null;
                }
            }

            int state = pos;
            logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Registro de OUT param VARCHAR en pos = " + state + "]");
            cs.registerOutParameter(pos, OracleTypes.VARCHAR);
            pos++;

            int cantidad = pos;
            logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Registro de OUT param NUMBER en pos = " + cantidad + "]");
            cs.registerOutParameter(pos, OracleTypes.NUMBER);
            pos++;

            int codError = pos;
            logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Registro de OUT param ARRAY (codError) en pos = " + codError + "]");
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
            pos++;

            int idx = pos;
            logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Registro de OUT param ARRAY (idx) en pos = " + idx + "]");
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");

            logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Ejecutando procedimiento: " + this.call + "]");
            cs.execute();
            logger.info(uid + " | [Database                       ][executeCallSate            ][INFO| Procedimiento ejecutado correctamente]");

            ARRAY codErrorArray = _convert(cs.getArray(codError),uid);
            ARRAY idxArray = _convert(cs.getArray(idx),uid);

            HashMap<String, Object> result = new HashMap<>();
            result.put("_state", cs.getString(state));
            result.put("_cantidad", cs.getLong(cantidad));
            result.put("_codError", codErrorArray.getArray());
            result.put("_idx", idxArray.getArray());

            logger.info(uid + " | [Database                       ][executeCallSate            ][RESPONSE| OPERATION_NAME: executeCallSate | HashMap generado correctamente]");
            return result;

        } catch (Exception ex) {
            logger.error(uid + " | [Database                       ][executeCallSate            ][ERROR| Excepción durante la ejecución del procedimiento: " + this.call + "]", ex);
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCallSate            ][ERROR| Cerrando CallableStatement]", e);
            }

            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][executeCallSate            ][TIME| OPERATION_NAME: executeCallSate | Duración: " + (endTime - startTime) + " ms]");
}

        return null;
    }

    /**
     * Ejecuta procedimiento con una lista de array
     *
     * @param arrays
     */
    public HashMap<String, Object> executeCall(List<ARRAY> arrays,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCall                ][REQUEST| OPERATION_NAME: executeCall | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCall                ][REQUEST| OPERATION_NAME: executeCall | arrays.size = " + (arrays != null ? arrays.size() : "N/A") + "]");

        int pos = 1;
        int cantidad = 0;
        int codError = 0;
        int idx = 0;

        try {
            cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
            cs.clearParameters();

            for (ARRAY _array : arrays) {
                try {
                    logger.info(uid + " | [Database                       ][executeCall                ][INFO| Set ARRAY | pos = " + pos + ", type = " + _array.getSQLTypeName() + "]");
                    cs.setARRAY(pos, _array);
                    pos++;
                } catch (Exception ex) {
                    logger.error(uid + " | [Database                       ][executeCall                ][ERROR| Error inicializando ARRAY en pos = " + pos + "]", ex);
                    return null;
                }
            }

            cantidad = pos;
            logger.info(uid + " | [Database                       ][executeCall                ][INFO| Registro OUT NUMBER en pos = " + cantidad + "]");
            cs.registerOutParameter(pos, OracleTypes.NUMBER);
            pos++;

            codError = pos;
            logger.info(uid + " | [Database                       ][executeCall                ][INFO| Registro OUT ARRAY codError en pos = " + codError + "]");
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
            pos++;

            idx = pos;
            logger.info(uid + " | [Database                       ][executeCall                ][INFO| Registro OUT ARRAY idx en pos = " + idx + "]");
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");

            logger.info(uid + " | [Database                       ][executeCall                ][INFO| Ejecutando procedimiento almacenado...]");
            cs.execute();
            logger.info(uid + " | [Database                       ][executeCall                ][INFO| Ejecución finalizada correctamente]");

            ARRAY codErrorArray = _convert(cs.getArray(codError),uid);
            ARRAY idxArray = _convert(cs.getArray(idx),uid);

            HashMap<String, Object> result = new HashMap<>();
            result.put("_cantidad", cs.getLong(cantidad));
            result.put("_codError", codErrorArray.getArray());
            result.put("_idx", idxArray.getArray());

            logger.info(uid + " | [Database                       ][executeCall                ][RESPONSE| OPERATION_NAME: executeCall | HashMap generado correctamente]");
            return result;

        } catch (Exception ex) {
            logger.error(uid + " | [Database                       ][executeCall                ][ERROR| Excepción durante ejecución de procedimiento: " + this.call + "]", ex);
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCall                ][ERROR| Cerrando CallableStatement]", e);
            }

            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][executeCall                ][TIME| OPERATION_NAME: executeCall | Duración: " + (endTime - startTime) + " ms]");
}

        return null;
    }

    /**
     * Ejecuta procedimiento con una lista de array
     *
     * @param arrays
     */
    public HashMap<String, Object> executeCallupdate(List<ARRAY> arrays,String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + " | Iniciando método: executeCallupdate");
        logger.info(uid + " | Request - this.call: " + (this.call != null ? this.call : "N/A"));
        logger.info(uid + " | Request - arrays: " + (arrays != null ? arrays.size() : "N/A"));

        int pos = 1;
        OracleCallableStatement cs = null;

        try {
            cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
            cs.clearParameters();

            for (ARRAY _array : arrays) {
                cs.setARRAY(pos, _array);
                logger.debug(uid + " | Posición " + pos + " - Tipo base: " + _array.getBaseTypeName());
                pos++;
            }

            logger.info(uid + " | Posición " + pos + " - OutParam: cantidad_number (NUMBER)");
            int cantidad_number = pos;
            cs.registerOutParameter(pos, OracleTypes.NUMBER);
            pos++;

            logger.info(uid + " | Posición " + pos + " - OutParam: cantidad_update (P_ERROR_TYPE)");
            int cantidad_update = pos;
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
            pos++;

            logger.info(uid + " | Posición " + pos + " - OutParam: cantidad_error (NUMBER)");
            int cantidad_error = pos;
            cs.registerOutParameter(pos, OracleTypes.NUMBER);
            pos++;

            logger.info(uid + " | Posición " + pos + " - OutParam: cantidad_error_types (P_ERROR_TYPE)");
            int cantidad_error_types = pos;
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
            pos++;

            logger.info(uid + " | Posición " + pos + " - OutParam: cantidad_error_idx (P_ERROR_TYPE)");
            int cantidad_error_idx = pos;
            cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");

            logger.info(uid + " | Ejecutando procedimiento...");
            cs.execute();
            logger.info(uid + " | Ejecución completada");

            ARRAY codUpdateEntidad = _convert(cs.getArray(cantidad_update),uid);
            ARRAY codErrorArray = _convert(cs.getArray(cantidad_error_types),uid);
            ARRAY idxArray = _convert(cs.getArray(cantidad_error_idx),uid);

            HashMap<String, Object> result = new HashMap<>();
            result.put("_cantidad_update", cs.getLong(cantidad_number));
            result.put("_cantidad_update_array", codUpdateEntidad.getArray());
            result.put("_cantidad", cs.getLong(cantidad_error));
            result.put("_codError", codErrorArray.getArray());
            result.put("_idx", idxArray.getArray());

            logger.info(uid + " | Response - HashMap generado correctamente");
            return result;

        } catch (Exception ex) {
            logger.error(uid + " | Error ejecutando procedimiento " + this.call, ex);
        } finally {
            try {
                if (cs != null) {
                    cs.close();
                }
            } catch (SQLException e) {
                logger.error(uid + " | Error cerrando CallableStatement", e);
            }

            long endTime = System.currentTimeMillis();
            logger.info(uid + " | Tiempo de ejecución del método executeCallupdate: " + (endTime - startTime) + " ms");
        }

        logger.info(uid + " | Response - Resultado: null");
        return null;
    }

    /**
     * Execute Call returns cursor
     *
     * @return
     */
    public void executeCallCursor(List<ARRAY> arrays,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallCursor          ][REQUEST| OPERATION_NAME: executeCallCursor | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallCursor          ][REQUEST| OPERATION_NAME: executeCallCursor | arrays.size = " + (arrays != null ? arrays.size() : "N/A") + "]");

        OracleCallableStatement cs = null;

        if (this.call != null) {
            try {
                cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
                int pos = 1;

                for (ARRAY _array : arrays) {
                    cs.setARRAY(pos, _array);
                    logger.info(uid + " | [Database                       ][executeCallCursor          ][INFO| Set ARRAY | pos = " + pos + ", baseType = " + _array.getBaseTypeName() + "]");
                    pos++;
                }

                logger.info(uid + " | [Database                       ][executeCallCursor          ][INFO| Registrando OUT parameter: estado (VARCHAR) en pos = " + pos + "]");
                cs.registerOutParameter(pos, OracleTypes.VARCHAR);
                pos++;

                logger.info(uid + " | [Database                       ][executeCallCursor          ][INFO| Registrando OUT parameter: cursor (CURSOR) en pos = " + pos + "]");
                cs.registerOutParameter(pos, OracleTypes.CURSOR);

                logger.info(uid + " | [Database                       ][executeCallCursor          ][INFO| Ejecutando procedimiento almacenado...]");
                cs.executeUpdate();
                logger.info(uid + " | [Database                       ][executeCallCursor          ][RESPONSE| OPERATION_NAME: executeCallCursor | Resultado: N/A]");

            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCallCursor          ][ERROR| SQLException al ejecutar " + this.call + "]", e);
            } catch (Exception e) {
                logger.error(uid + " | [Database                       ][executeCallCursor          ][ERROR| Exception al ejecutar " + this.call + "]", e);
            } finally {
                try {
                    if (cs != null) {
                        cs.close();
                    }
                } catch (SQLException e) {
                    logger.error(uid + " | [Database                       ][executeCallCursor          ][ERROR| Cerrando CallableStatement]", e);
                }

                long endTime = System.currentTimeMillis();
                logger.info(uid + " | [Database                       ][executeCallCursor          ][TIME| OPERATION_NAME: executeCallCursor | Duración: " + (endTime - startTime) + " ms]");
}
        } else {
            logger.warn(uid + " | [Database                       ][executeCallCursor          ][WARN| this.call es null, no se ejecuta el procedimiento]");
            logger.info(uid + " | [Database                       ][executeCallCursor          ][RESPONSE| OPERATION_NAME: executeCallCursor | Resultado: N/A]");
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | [Database                       ][executeCallCursor          ][TIME| OPERATION_NAME: executeCallCursor | Duración: " + (endTime - startTime) + " ms]");
}
    }

    /**
     * excecute update
     */
    public void executeUpdate(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + " | Iniciando método: executeUpdate");
        logger.info(uid + " | Request - this.call: " + (this.call != null ? this.call : "N/A"));

        OracleCallableStatement cs = null;

        if (this.call != null) {
            try {
                cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
                cs.executeUpdate();
                logger.info(uid + " | Response - Resultado: N/A");
            } catch (SQLException e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } catch (Exception e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } finally {
                try {
                    if (cs != null) {
                        cs.close();
                    }
                    if (this.conn != null) {
                        this.conn.close();
                    }
                } catch (SQLException e) {
                    logger.error(uid + " | Error cerrando recursos", e);
                }
                long endTime = System.currentTimeMillis();
                logger.info(uid + " | Tiempo de ejecución del método executeUpdate: " + (endTime - startTime) + " ms");
            }
        } else {
            logger.warn(uid + " | this.call es null, no se ejecuta el procedimiento.");
            logger.info(uid + " | Response - Resultado: N/A");
        }
    }

    /**
     * Execute Call returns cursor
     *
     * @return
     */
    public void executeCall(String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + " | Iniciando método: executeCall");
        logger.info(uid + " | Request - this.call: " + (this.call != null ? this.call : "N/A"));

        if (this.call != null) {
//            OracleCallableStatement cs = null;
            try {
                this.cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
                this.cs.registerOutParameter(1, OracleTypes.VARCHAR);
                this.cs.registerOutParameter(2, OracleTypes.CURSOR);
                this.cs.executeUpdate();
                logger.info(uid + " | Response - Resultado: N/A");
            } catch (SQLException e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } catch (Exception e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } finally {
//                try {
//                    if (cs != null) {
//                        cs.close();
//                    }
//                } catch (SQLException e) {
//                    logger.error(uid + " | Error cerrando CallableStatement", e);
//                }
                long endTime = System.currentTimeMillis();
                logger.info(uid + " | Tiempo de ejecución del método executeCall: " + (endTime - startTime) + " ms");
            }
        } else {
            logger.warn(uid + " | this.call es null, no se ejecuta el procedimiento.");
            logger.info(uid + " | Response - Resultado: N/A");
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | Tiempo de ejecución del método executeCall: " + (endTime - startTime) + " ms");
        }
    }

    /**
     * Execute Call returns cursor
     *
     * @return
     */
    public OracleCallableStatement executeCall(Connection con,String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + " | Iniciando método: executeCall(Connection)");
        logger.info(uid + " | Request - this.call: " + (this.call != null ? this.call : "N/A"));

        if (this.call != null) {
            OracleCallableStatement csCall = null;
            try {
                csCall = (OracleCallableStatement) con.prepareCall(this.call);
                csCall.registerOutParameter(1, OracleTypes.VARCHAR);
                csCall.registerOutParameter(2, OracleTypes.CURSOR);
                csCall.executeUpdate();

                logger.info(uid + " | Response - Resultado: OracleCallableStatement devuelto");
                return csCall;
            } catch (SQLException e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } catch (Exception e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } finally {
                long endTime = System.currentTimeMillis();
                logger.info(uid + " | Tiempo de ejecución del método executeCall(Connection): " + (endTime - startTime) + " ms");
            }
        } else {
            logger.warn(uid + " | this.call es null, no se ejecuta el procedimiento.");
            logger.info(uid + " | Response - Resultado: N/A");
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | Tiempo de ejecución del método executeCall(Connection): " + (endTime - startTime) + " ms");
        }

        return null;
    }

    /**
     * Execute Call with result or cursor
     *
     * @return
     */
    public void executeCallWithResult(Boolean out,String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + " | Iniciando método: executeCallWithResult(Boolean)");
        logger.info(uid + " | Request - this.call: " + (this.call != null ? this.call : "N/A"));
        logger.info(uid + " | Request - parámetro 'out': " + out);

        if (this.call != null) {
            try {
                cs = ((OracleCallableStatement) getConn(uid).prepareCall(this.call));

                if (out) {
                    cs.registerOutParameter(1, OracleTypes.VARCHAR);
                }

                cs.executeUpdate();
                logger.info(uid + " | Response: N/A (no hay valor de retorno explícito)");

            } catch (SQLException e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } catch (Exception e) {
                logger.error(uid + " | Error ejecutando procedimiento " + this.call, e);
            } finally {
                long endTime = System.currentTimeMillis();
                logger.info(uid + " | Tiempo de ejecución del método executeCallWithResult: " + (endTime - startTime) + " ms");
            }
        } else {
            logger.warn(uid + " | No se ejecutó el procedimiento: this.call es null");
            logger.info(uid + " | Response: N/A");
            long endTime = System.currentTimeMillis();
            logger.info(uid + " | Tiempo de ejecución del método executeCallWithResult: " + (endTime - startTime) + " ms");
        }
    }

    /**
     * Metodo generico para invocaci?n de procedimiento almacenado
     *
     * @param parameter Lista de objeto , con listado de parametrs
     * @param output Lista de parametros de salidas
     * @param nameOutput
     * @return
     */
    public HashMap<String, String> executeCallOutputs(List<Object> parameter, List<Integer> output, HashMap<String, Integer> nameOutput,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| OPERATION_NAME: executeCallOutputs | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Parámetros de entrada: " + (parameter != null ? parameter.toString() : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Tipos de salida: " + (output != null ? output.toString() : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Nombres de salida: " + (nameOutput != null ? nameOutput.keySet().toString() : "N/A") + "]");

        HashMap<String, String> result = new HashMap<>();

        if (this.call != null) {
            Connection con = null;
            OracleCallableStatement cs = null;
            try {
                con = getConnection(uid);
                cs = (OracleCallableStatement) con.prepareCall(this.call);

                int pos = 1;

                if (parameter != null) {
                    for (Object obj : parameter) {
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Set parameter | pos = " + pos + ", value = " + obj + "]");
                        if (obj instanceof String) {
                            cs.setString(pos, (String) obj);
                        } else if (obj instanceof Integer) {
                            cs.setInt(pos, (Integer) obj);
                        } else if (obj instanceof Date) {
                            cs.setDate(pos, (Date) obj);
                        } else if (obj instanceof BigDecimal) {
                            cs.setBigDecimal(pos, (BigDecimal) obj);
                        } else if (obj instanceof Float) {
                            cs.setFloat(pos, (Float) obj);
                        } else if (obj instanceof Long) {
                            cs.setLong(pos, (Long) obj);
                        } else if (obj instanceof Double) {
                            cs.setDouble(pos, (Double) obj);
                        } else if (obj instanceof Boolean) {
                            cs.setBoolean(pos, (Boolean) obj);
                        } else {
                            cs.setObject(pos, obj);
                        }
                        pos++;
                    }
                }

                if (output != null) {
                    for (Integer obj : output) {
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Registrando parámetro OUT | pos = " + pos + ", tipo = " + obj + "]");
                        cs.registerOutParameter(pos, obj);
                        pos++;
                    }
                }

                logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Ejecutando procedimiento almacenado...]");
                cs.executeUpdate();

                if (nameOutput != null) {
                    for (Map.Entry<String, Integer> entry : nameOutput.entrySet()) {
                        String val = cs.getString(entry.getValue());
                        result.put(entry.getKey(), val);
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Output capturado | key = " + entry.getKey() + ", valor = " + val + "]");
                    }
                }

                logger.info(uid + " | [Database                       ][executeCallOutputs         ][RESPONSE| Salida del procedimiento: " + result.toString() + "]");

            } catch (Exception ex) {
                logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Ejecutando procedimiento " + this.call + "]", ex);
            } finally {
                if (cs != null) {
                    try {
                        if (!cs.isClosed()) {
                            logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Cerrando CallableStatement]");
                            cs.close();
                        }
                    } catch (SQLException e) {
                        logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Cerrando CallableStatement]", e);
                    }
                }
                if (con != null) {
                    try {
                        if (!con.isClosed()) {
                            logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Cerrando conexión]");
                            con.close();
                        }
                    } catch (SQLException e) {
                        logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Cerrando conexión]", e);
                    }
                }
            }
        } else {
            logger.warn(uid + " | [Database                       ][executeCallOutputs         ][WARN| this.call es null. No se ejecuta el procedimiento.]");
            logger.info(uid + " | [Database                       ][executeCallOutputs         ][RESPONSE| Resultado: N/A]");
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][TIME| OPERATION_NAME: executeCallOutputs | Duración: " + (endTime - startTime) + " ms]");
return result;
    }

    /**
     * ejecuta procedimiento almancenado con salida
     *
     * @param output Arreiglo de tipos de Salidas
     * @param parameter Arreiglo de parametro de entrada
     * @return
     */
    public OracleCallableStatement executeCallOutputs(List<Integer> output, List<Object> parameter,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| OPERATION_NAME: executeCallOutputs | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Parámetros de entrada: " + (parameter != null ? parameter.toString() : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Tipos de salida: " + (output != null ? output.toString() : "N/A") + "]");

        if (this.call != null) {
            OracleCallableStatement cs = null;
            try {
                cs = (OracleCallableStatement) getConn(uid).prepareCall(this.call);
                int pos = 1;

                if (parameter != null) {
                    for (Object obj : parameter) {
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Asignando parámetro IN | pos = " + pos + " | valor = " + obj + "]");
                        if (obj instanceof String) {
                            cs.setString(pos, (String) obj);
                        } else if (obj instanceof Integer) {
                            cs.setInt(pos, (Integer) obj);
                        } else if (obj instanceof Date) {
                            cs.setDate(pos, (Date) obj);
                        } else if (obj instanceof BigDecimal) {
                            cs.setBigDecimal(pos, (BigDecimal) obj);
                        } else if (obj instanceof Float) {
                            cs.setFloat(pos, (Float) obj);
                        } else if (obj instanceof Long) {
                            cs.setLong(pos, (Long) obj);
                        } else if (obj instanceof Double) {
                            cs.setDouble(pos, (Double) obj);
                        } else if (obj instanceof Boolean) {
                            cs.setBoolean(pos, (Boolean) obj);
                        } else if (obj instanceof java.sql.Array) {
                            cs.setArray(pos, (java.sql.Array) obj);
                        } else if (obj instanceof Calendar) {
                            cs.setTimestamp(pos, new Timestamp(((Calendar) obj).getTimeInMillis()));
                        } else {
                            cs.setObject(pos, obj);
                        }
                        pos++;
                    }
                }

                if (output != null) {
                    for (Integer outType : output) {
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Registrando parámetro OUT | pos = " + pos + " | tipo = " + outType + "]");
                        cs.registerOutParameter(pos, outType);
                        pos++;
                    }
                }

                cs.executeUpdate();
                logger.info(uid + " | [Database                       ][executeCallOutputs         ][RESPONSE| Procedimiento ejecutado correctamente]");

                long endTime = System.currentTimeMillis();
                logger.info(uid + " | [Database                       ][executeCallOutputs         ][TIME| OPERATION_NAME: executeCallOutputs | Duración: " + (endTime - startTime) + " ms]");
return cs;

            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Error SQL ejecutando procedimiento " + this.call + "]", e);
            } catch (Exception e) {
                logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Error inesperado ejecutando procedimiento " + this.call + "]", e);
            }
        } else {
            logger.warn(uid + " | [Database                       ][executeCallOutputs         ][WARN| this.call es null. No se ejecuta el procedimiento]");
        }

        logger.info(uid + " | [Database                       ][executeCallOutputs         ][RESPONSE| Resultado: N/A]");
        return null;
    }

    /**
     * ejecuta procedimiento almancenado con salida
     *
     * @param output Arreiglo de tipos de Salidas
     * @param parameter Arreiglo de parametro de entrada
     * @return
     */
    public OracleCallableStatement executeCallOutputs(Connection con, List<Integer> output, List<Object> parameter,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| OPERATION_NAME: executeCallOutputs | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Parámetros de entrada: " + (parameter != null ? parameter.toString() : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][REQUEST| Tipos de salida: " + (output != null ? output.toString() : "N/A") + "]");

        if (this.call != null) {
            OracleCallableStatement cs = null;
            try {
                cs = (OracleCallableStatement) con.prepareCall(this.call);
                int pos = 1;

                if (parameter != null) {
                    for (Object obj : parameter) {
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Set IN param | pos = " + pos + " | valor = " + obj + "]");
                        if (obj instanceof String) {
                            cs.setString(pos, (String) obj);
                        } else if (obj instanceof Integer) {
                            cs.setInt(pos, (Integer) obj);
                        } else if (obj instanceof Date) {
                            cs.setDate(pos, (Date) obj);
                        } else if (obj instanceof BigDecimal) {
                            cs.setBigDecimal(pos, (BigDecimal) obj);
                        } else if (obj instanceof Float) {
                            cs.setFloat(pos, (Float) obj);
                        } else if (obj instanceof Long) {
                            cs.setLong(pos, (Long) obj);
                        } else if (obj instanceof Double) {
                            cs.setDouble(pos, (Double) obj);
                        } else if (obj instanceof Boolean) {
                            cs.setBoolean(pos, (Boolean) obj);
                        } else if (obj instanceof java.sql.Array) {
                            cs.setArray(pos, (java.sql.Array) obj);
                        } else {
                            cs.setObject(pos, obj);
                        }
                        pos++;
                    }
                }

                if (output != null) {
                    for (Integer outType : output) {
                        logger.info(uid + " | [Database                       ][executeCallOutputs         ][INFO| Register OUT param | pos = " + pos + " | tipo SQL = " + outType + "]");
                        cs.registerOutParameter(pos, outType);
                        pos++;
                    }
                }

                cs.executeUpdate();

                logger.info(uid + " | [Database                       ][executeCallOutputs         ][RESPONSE| Procedimiento ejecutado correctamente]");

                return cs;

            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| SQL Exception ejecutando procedimiento: " + e.getMessage() + "]", e);
                if (e.getCause() != null) {
                    logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Causa raíz: " + e.getCause().getMessage() + "]", e.getCause());
                }
            } catch (Exception e) {
                logger.error(uid + " | [Database                       ][executeCallOutputs         ][ERROR| Error inesperado ejecutando procedimiento: " + e.getMessage() + "]", e);
            }
        } else {
            logger.warn(uid + " | [Database                       ][executeCallOutputs         ][WARN| this.call es null. No se ejecuta ningún procedimiento]");
        }
        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][RESPONSE| Resultado: N/A]");
        logger.info(uid + " | [Database                       ][executeCallOutputs         ][TIME| OPERATION_NAME: executeCallOutputs | Duración: " + (endTime - startTime) + " ms]");
return null;
    }

    /**
     * ejecuta procedimiento almancenado con salida
     *
     * @param output Arreiglo de tipos de Salidas
     * @param parameter Arreiglo de parametro de entrada
     * @return
     */
    public OracleCallableStatement executeCallOutputsV3(Connection con, List<Integer> output, List<Object> parameter,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][REQUEST| OPERATION_NAME: executeCallOutputsV3 | call = " + (this.call != null ? this.call : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][REQUEST| Tipos de salida: " + (output != null ? output.toString() : "N/A") + "]");
        logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][REQUEST| Parámetros de entrada: " + (parameter != null ? parameter.toString() : "N/A") + "]");

        if (this.call != null) {
            OracleCallableStatement cs = null;
            try {
                cs = (OracleCallableStatement) con.prepareCall(this.call);
                int pos = 1;

                if (output != null) {
                    for (Integer obj : output) {
                        logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][INFO| Register OUT param | pos = " + pos + " | tipo SQL = " + obj + "]");
                        cs.registerOutParameter(pos, obj);
                        pos++;
                    }
                }

                if (parameter != null) {
                    for (Object obj : parameter) {
                        logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][INFO| Set IN param | pos = " + pos + " | valor = " + obj + "]");
                        if (obj instanceof String) {
                            cs.setString(pos, (String) obj);
                        } else if (obj instanceof Integer) {
                            cs.setInt(pos, (Integer) obj);
                        } else if (obj instanceof Date) {
                            cs.setDate(pos, (Date) obj);
                        } else if (obj instanceof BigDecimal) {
                            cs.setBigDecimal(pos, (BigDecimal) obj);
                        } else if (obj instanceof Float) {
                            cs.setFloat(pos, (Float) obj);
                        } else if (obj instanceof Long) {
                            cs.setLong(pos, (Long) obj);
                        } else if (obj instanceof Double) {
                            cs.setDouble(pos, (Double) obj);
                        } else if (obj instanceof Boolean) {
                            cs.setBoolean(pos, (Boolean) obj);
                        } else if (obj instanceof java.sql.Array) {
                            cs.setArray(pos, (java.sql.Array) obj);
                        } else {
                            cs.setObject(pos, obj);
                        }
                        pos++;
                    }
                }

                cs.executeUpdate();
                long endTime = System.currentTimeMillis();
                logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][RESPONSE| Procedimiento ejecutado correctamente]");
                logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][TIME| OPERATION_NAME: executeCallOutputsV3 | Duración: " + (endTime - startTime) + " ms]");
return cs;

            } catch (SQLException e) {
                logger.error(uid + " | [Database                       ][executeCallOutputsV3       ][ERROR| SQL Exception ejecutando procedimiento: " + e.getMessage() + "]", e);
                if (e.getCause() != null) {
                    logger.error(uid + " | [Database                       ][executeCallOutputsV3       ][ERROR| Causa raíz SQL: " + e.getCause().getMessage() + "]", e.getCause());
                }
            } catch (Exception e) {
                logger.error(uid + " | [Database                       ][executeCallOutputsV3       ][ERROR| Error inesperado ejecutando procedimiento: " + e.getMessage() + "]", e);
            }
        } else {
            logger.warn(uid + " | [Database                       ][executeCallOutputsV3       ][WARN| this.call es null. No se ejecutó el procedimiento]");
        }

        logger.info(uid + " | [Database                       ][executeCallOutputsV3       ][RESPONSE| Resultado: null]");
        return null;
    }

    /**
     * ejecuta procedimiento almancenado con salida
     *
     * @param output Arreiglo de tipos de Salidas
     * @param parameter Arreiglo de parametro de entrada
     * @return
     */
    public OracleCallableStatement executeCallOutputsV2(Connection con, List<Integer> output, List<Object> parameter,String uid) {
        long startTime = System.currentTimeMillis();
        logger.info(uid + " | Iniciando método: executeCallOutputsV2");
        logger.info(uid + " | Request - this.call: " + (this.call != null ? this.call : "N/A"));
        logger.info(uid + " | Request - Tipos de salida: " + (output != null ? output.toString() : "N/A"));
        logger.info(uid + " | Request - Parámetros de entrada: " + (parameter != null ? parameter.toString() : "N/A"));

        if (this.call != null) {
            OracleCallableStatement cs = null;
            try {
                cs = (OracleCallableStatement) con.prepareCall(this.call);
                int pos = 1;

                // Parámetros de entrada
                if (parameter != null) {
                    for (Object obj : parameter) {
                        logger.info(uid + " | Asignando parámetro IN - pos: " + pos + " valor: " + obj);
                        if (obj instanceof String) {
                            cs.setString(pos, (String) obj);
                        } else if (obj instanceof Integer) {
                            cs.setInt(pos, (Integer) obj);
                        } else if (obj instanceof Date) {
                            cs.setDate(pos, (Date) obj);
                        } else if (obj instanceof BigDecimal) {
                            cs.setBigDecimal(pos, (BigDecimal) obj);
                        } else if (obj instanceof Float) {
                            cs.setFloat(pos, (Float) obj);
                        } else if (obj instanceof Long) {
                            cs.setLong(pos, (Long) obj);
                        } else if (obj instanceof Double) {
                            cs.setDouble(pos, (Double) obj);
                        } else if (obj instanceof Boolean) {
                            cs.setBoolean(pos, (Boolean) obj);
                        } else if (obj instanceof java.sql.Array) {
                            cs.setArray(pos, (java.sql.Array) obj);
                        } else {
                            cs.setObject(pos, obj);
                        }
                        pos++;
                    }
                }

                // Parámetros de salida simples
                if (output != null) {
                    for (Integer obj : output) {
                        logger.info(uid + " | Registrando parámetro OUT - pos: " + pos + " tipo SQL: " + obj);
                        cs.registerOutParameter(pos, obj);
                        pos++;
                    }
                }

                // Parámetros de salida tipo ARRAY P_ERROR_TYPE
                logger.debug(uid + " | Registrando P_ERROR_TYPE en pos: " + pos);
                cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");
                pos++;

                logger.debug(uid + " | Registrando P_ERROR_TYPE en pos: " + pos);
                cs.registerOutParameter(pos, OracleTypes.ARRAY, "P_ERROR_TYPE");

                // Ejecución
                cs.executeUpdate();

                long endTime = System.currentTimeMillis();
                logger.info(uid + " | Procedimiento ejecutado correctamente. Duración: " + (endTime - startTime) + " ms");
                logger.info(uid + " | Response - OracleCallableStatement retornado.");
                return cs;

            } catch (SQLException e) {
                logger.error(uid + " | Error SQL ejecutando procedimiento: " + e.getMessage(), e);
                if (e.getCause() != null) {
                    logger.error(uid + " | Causa raíz SQL: " + e.getCause().getMessage(), e.getCause());
                }
            } catch (Exception e) {
                logger.error(uid + " | Error general ejecutando procedimiento: " + e.getMessage(), e);
            }
        } else {
            logger.warn(uid + " | this.call es null. No se ejecutó el procedimiento.");
        }

        logger.info(uid + " | Response: null");
        return null;
    }

    /**
     * ejecuta procedimiento almancenado con salida
     *
     * @param output Arreiglo de tipos de Salidas
     * @param parameter Arreiglo de parametro de entrada
     * @return
     */
    /*public Boolean executeCallOutputs(List<Integer> output,List<Object> parameter){
		logger.info(uid + " | Execute call .." + this.call);
		if (this.call != null) {
			try {
				cs = ((OracleCallableStatement) getConn()
						.prepareCall(this.call));
				//Se recorre elementos
				int pos =1;
				for(Object obj : parameter){
					logger.info(uid + " | pos:"+pos  +" value :"+obj+" ");
					if (obj instanceof String){
						//logger.info(uid + " | Obj String:"+obj);
						cs.setString(pos,(String) obj);
					}
					else if (obj instanceof Integer){
						//logger.info(uid + " | Obj Integer:"+obj);
						cs.setInt(pos,(Integer) obj);
					}
					else if (obj instanceof Date){
						//logger.info(uid + " | Obj Date:"+obj);
						cs.setDate(pos,(Date) obj);
					}
					else if (obj instanceof BigDecimal){
						//logger.info(uid + " | Obj BigDecimal:"+obj);
						cs.setBigDecimal(pos,(BigDecimal) obj);
					}
					else{
						//logger.info(uid + " | Obj No Object Type:"+obj);
						cs.setObject(pos, obj);
					}
					pos++;
				}
				for (Integer obj : output){
					logger.info(uid + " | pos:"+pos  + " value :"+obj+" ");
					cs.registerOutParameter(pos, obj);					
					pos++;
				}
				cs.executeUpdate();
				return true;
			} catch (SQLException e) {
				logger.error(uid + " | Error ejecutando procedimiento " + e.getMessage(),e);
			}
			catch (Exception e) {
				logger.error(uid + " | Error ejecutando procedimiento " + e.getMessage(),e);
			}
		}
		return false;
	}*/
    /**
     * Se desconecta la conexi?n de la base de datos
     */
    public void disconnet(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][disconnet                 ][REQUEST| OPERATION_NAME: disconnet | conn: " + (this.conn != null ? "activa" : "null") + "]");

        try {
            if (this.conn != null && !this.conn.isClosed()) {
                this.conn.close();
                logger.info(uid + " | [Database                       ][disconnet                 ][RESPONSE| Conexión cerrada correctamente]");
            } else {
                logger.warn(uid + " | [Database                       ][disconnet                 ][RESPONSE| No hay conexión activa o ya estaba cerrada]");
            }
        } catch (SQLException e) {
            logger.error(uid + " | [Database                       ][disconnet                 ][ERROR| Error al cerrar la conexión (SQLException): " + e.getMessage() + "]", e);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][disconnet                 ][ERROR| Error inesperado al cerrar la conexión: " + e.getMessage() + "]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][disconnet                 ][TIME| OPERATION_NAME: disconnet | Duración: " + (endTime - startTime) + " ms]");
}

    /**
     * Ejecuta un query y tiene un resulset
     *
     * @param query
     * @return
     */
    public Connection getConn(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][getConn                   ][REQUEST| OPERATION_NAME: getConn | Estado conexión actual: "
                + (conn == null ? "null" : (isConnectionOpen(conn,uid) ? "abierta" : "cerrada")) + "]");

        try {
            if (conn != null && !conn.isClosed()) {
                logger.debug(uid + " | [Database                       ][getConn                   ][INFO| La conexión estaba abierta. Procediendo a cerrarla.]");
                this.disconnet(uid);
            }

            logger.debug(uid + " | [Database                       ][getConn                   ][INFO| Solicitando nueva conexión...]");
            this._connection(uid);

        } catch (SQLException e) {
            logger.error(uid + " | [Database                       ][getConn                   ][ERROR| SQLException revisando o creando conexión: " + e.getMessage() + "]", e);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][getConn                   ][ERROR| Error inesperado: " + e.getMessage() + "]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][getConn                   ][RESPONSE| Conexión retornada: " + (conn != null ? "establecida" : "null") + "]");
        logger.info(uid + " | [Database                       ][getConn                   ][TIME| OPERATION_NAME: getConn | Duración: " + (endTime - startTime) + " ms]");
return conn;
    }

// Método auxiliar para verificar si la conexión está abierta, opcional
    private boolean isConnectionOpen(Connection connection,String uid) {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            logger.warn(uid + " | [Database                       ][isConnectionOpen          ][ERROR| Fallo al verificar estado de la conexión: " + e.getMessage() + "]", e);
            return false;
        }
    }

    /**
     * Inserta en bloque en un objeto CachedRowSet
     *
     * @param cr
     */
    public void insertQueryReintento(CachedRowSet cr, String sql,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][insertQueryReintento      ][REQUEST| SQL: " + (sql != null ? sql : "N/A") + "]");
        logger.info(uid + " | [Database                       ][insertQueryReintento      ][REQUEST| CachedRowSet: " + (cr != null ? "con datos" : "null") + "]");

        if (cr == null) {
            logger.warn(uid + " | [Database                       ][insertQueryReintento      ][WARNING| El CachedRowSet recibido es null. Se omite el procesamiento.]");
            logger.info(uid + " | [Database                       ][insertQueryReintento      ][RESPONSE| Resultado: N/A]");
return;
        }

        int registrosProcesados = 0;
        int registrosErroneos = 0;

        try {
            while (cr.next()) {
                Connection conn = null;
                PreparedStatement ps = null;
                try {
                    conn = this.getConn(uid);
                    ps = conn.prepareStatement(sql);

                    ps.setInt(1, cr.getInt("GRUPO_AFINIDAD"));
                    ps.setInt(2, cr.getInt("PLAZO"));
                    ps.setBigDecimal(3, cr.getBigDecimal("SALDO_FINANCIAR"));
                    ps.setString(4, cr.getString("NOMBRES"));
                    ps.setString(5, cr.getString("APELLIDOS"));
                    ps.setInt(6, cr.getInt("TIP_IDENTIFICACION"));
                    ps.setString(7, cr.getString("NRO_IDENTIFICACION"));
                    ps.setString(8, cr.getString("REF_EQUIPO"));
                    ps.setString(9, cr.getString("IMEI"));
                    ps.setLong(10, cr.getLong("MSISDN"));
                    ps.setString(11, cr.getString("CUSTCODE_SER"));
                    ps.setLong(12, cr.getLong("CUSTOMER_ID_SER"));
                    ps.setLong(13, cr.getLong("CO_ID"));
                    ps.setString(14, cr.getString("COD_CICLO_FACTURACION"));
                    ps.setString(15, cr.getString("CUSTCODE_RES_PAGO"));
                    ps.setString(16, cr.getString("REGION"));
                    ps.setString(17, cr.getString("CODIGO_DISTRIBUIDOR"));
                    ps.setString(18, cr.getString("NOMBRE_DISTRIBUIDOR"));
                    ps.setLong(19, cr.getLong("EXENTO_IVA"));
                    ps.setString(20, cr.getString("PROCESO"));
                    ps.setString(21, cr.getString("CODIGO_SALUDO"));
                    ps.setString(22, cr.getString("DIR_COMPLETA"));
                    ps.setString(23, cr.getString("CIUDAD_DEPT"));
                    ps.setString(24, cr.getString("CENTRO_COSTO"));
                    ps.setString(25, cr.getString("MEDIO_ENVIO"));
                    ps.setString(26, cr.getString("EMAIL"));
                    ps.setString(27, cr.getString("USUARIO"));

                    ps.executeUpdate();
                    registrosProcesados++;
                    logger.debug(uid + " | [Database                       ][insertQueryReintento      ][INFO| Registro insertado correctamente. Total acumulado: " + registrosProcesados + "]");
                } catch (SQLException e) {
                    registrosErroneos++;
                    logger.error(uid + " | [Database                       ][insertQueryReintento      ][ERROR| Fallo al insertar registro. Detalle: " + e.getMessage() + "]", e);
                } finally {
                    try {
                        if (ps != null && !ps.isClosed()) {
                            ps.close();
                        }
                        if (conn != null && !conn.isClosed()) {
                            conn.close();
                        }
                    } catch (SQLException e) {
                        logger.warn(uid + " | [Database                       ][insertQueryReintento      ][WARNING| Error cerrando recursos: " + e.getMessage() + "]", e);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error(uid + " | [Database                       ][insertQueryReintento      ][ERROR| Error recorriendo CachedRowSet: " + e.getMessage() + "]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][insertQueryReintento      ][RESPONSE| Registros procesados: " + registrosProcesados + " | Registros con error: " + registrosErroneos + "]");
        logger.info(uid + " | [Database                       ][insertQueryReintento      ][TIME| Duración total: " + (endTime - startTime) + " ms]");
}

    /**
     * Update query
     *
     * @param sql
     */
    public void updateQuery(String sql,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][updateQuery               ][REQUEST| SQL: " + (sql != null ? sql : "N/A") + "]");

        Connection conn = null;
        PreparedStatement preparedStatement = null;

        try {
            conn = this.getConn(uid);
            preparedStatement = conn.prepareStatement(sql);
            int rowsAffected = preparedStatement.executeUpdate();
            logger.info(uid + " | [Database                       ][updateQuery               ][RESPONSE| Filas afectadas: " + rowsAffected + "]");
        } catch (SQLException e) {
            logger.error(uid + " | [Database                       ][updateQuery               ][ERROR| Error ejecutando updateQuery: " + e.getMessage() + "]", e);
        } finally {
            try {
                if (preparedStatement != null && !preparedStatement.isClosed()) {
                    preparedStatement.close();
                    logger.debug(uid + " | [Database                       ][updateQuery               ][DEBUG| PreparedStatement cerrado correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][updateQuery               ][WARNING| Error cerrando PreparedStatement: " + e.getMessage() + "]", e);
            }

            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    logger.debug(uid + " | [Database                       ][updateQuery               ][DEBUG| Conexión cerrada correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][updateQuery               ][WARNING| Error cerrando conexión: " + e.getMessage() + "]", e);
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][updateQuery               ][TIME| Duración: " + (endTime - startTime) + " ms]");
}

    public void disconnetCs(String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][disconnetCs               ][REQUEST| N/A]");

        try {
            if (cs != null) {
                this.cs.close();
                logger.info(uid + " | [Database                       ][disconnetCs               ][RESPONSE| CallableStatement cerrado correctamente]");
            } else {
                logger.info(uid + " | [Database                       ][disconnetCs               ][RESPONSE| CallableStatement ya es null]");
            }
        } catch (SQLException e) {
            logger.error(uid + " | [Database                       ][disconnetCs               ][ERROR| SQLException cerrando CallableStatement: " + e.getMessage() + "]", e);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][disconnetCs               ][ERROR| Error general cerrando CallableStatement: " + e.getMessage() + "]", e);
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][disconnetCs               ][TIME| Duración: " + (endTime - startTime) + " ms]");
}

    /**
     * actualiza call de archivo
     *
     * @param id
     * @param estado
     * @param descripcion
     */
    public void executeCallArchivo(int id, int estado, String descripcion,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][executeCallArchivo        ][REQUEST| id=" + id + ", estado=" + estado + ", descripcion=" + (descripcion != null ? descripcion : "N/A") + "]");

        OracleCallableStatement cs = null;
        Connection conn = null;

        try {
            conn = this.getConn(uid);
            cs = (OracleCallableStatement) conn.prepareCall(this.call);

            cs.setInt(1, id);
            cs.setInt(2, estado);
            cs.setString(3, descripcion);
            cs.registerOutParameter(4, OracleTypes.NUMBER);

            cs.execute();
            int registrosActualizados = cs.getInt(4);
            logger.info(uid + " | [Database                       ][executeCallArchivo        ][RESPONSE| registros actualizados=" + registrosActualizados + "]");
        } catch (SQLException e) {
            logger.error(uid + " | [Database                       ][executeCallArchivo        ][ERROR| SQLException ejecutando procedimiento: " + e.getMessage() + "]", e);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][executeCallArchivo        ][ERROR| Excepción inesperada: " + e.getMessage() + "]", e);
        } finally {
            try {
                if (cs != null && !cs.isClosed()) {
                    cs.close();
                    logger.debug(uid + " | [Database                       ][executeCallArchivo        ][INFO| CallableStatement cerrado correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][executeCallArchivo        ][WARN| Error cerrando CallableStatement: " + e.getMessage() + "]", e);
            }

            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    logger.debug(uid + " | [Database                       ][executeCallArchivo        ][INFO| Conexión cerrada correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][executeCallArchivo        ][WARN| Error cerrando conexión: " + e.getMessage() + "]", e);
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][executeCallArchivo        ][TIME| Duración: " + (endTime - startTime) + " ms]");
}

    public CachedRowSet execQuery(String query,String uid) {
        long startTime = System.currentTimeMillis();
logger.info(uid + " | [Database                       ][execQuery                 ][REQUEST| query=" + (query != null ? query : "N/A") + "]");

        Statement stmt = null;
        Connection conn = null;
        ResultSet resultSet = null;
        CachedRowSetImpl crs = null;

        try {
            conn = this.getConn(uid);
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(query);

            crs = new CachedRowSetImpl();
            crs.populate(resultSet);

            logger.info(uid + " | [Database                       ][execQuery                 ][RESPONSE| CachedRowSet generado correctamente]");
            return crs;
        } catch (SQLException se) {
            logger.error(uid + " | [Database                       ][execQuery                 ][ERROR| SQLException ejecutando consulta: " + se.getMessage() + "]", se);
        } catch (Exception e) {
            logger.error(uid + " | [Database                       ][execQuery                 ][ERROR| Excepción inesperada ejecutando consulta: " + e.getMessage() + "]", e);
        } finally {
            try {
                if (resultSet != null && !resultSet.isClosed()) {
                    resultSet.close();
                    logger.debug(uid + " | [Database                       ][execQuery                 ][INFO| ResultSet cerrado correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][execQuery                 ][WARN| Error cerrando ResultSet: " + e.getMessage() + "]", e);
            }

            try {
                if (stmt != null && !stmt.isClosed()) {
                    stmt.close();
                    logger.debug(uid + " | [Database                       ][execQuery                 ][INFO| Statement cerrado correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][execQuery                 ][WARN| Error cerrando Statement: " + e.getMessage() + "]", e);
            }

            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    logger.debug(uid + " | [Database                       ][execQuery                 ][INFO| Conexión cerrada correctamente]");
                }
            } catch (SQLException e) {
                logger.warn(uid + " | [Database                       ][execQuery                 ][WARN| Error cerrando conexión: " + e.getMessage() + "]", e);
            }
        }

        logger.info(uid + " | [Database                       ][execQuery                 ][RESPONSE| null]");
        long endTime = System.currentTimeMillis();
        logger.info(uid + " | [Database                       ][execQuery                 ][TIME| Duración: " + (endTime - startTime) + " ms]");
return null;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getCall() {
        return call;
    }

    public void setCall(String call) {
        this.call = call;
    }

    public OracleCallableStatement getCs() {
        return cs;
    }

}
