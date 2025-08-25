
package co.com.claro.financialintegrator.implement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import co.com.claro.FileUtilAPI.DateUtil;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.database.Database;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.interfaces.GenericProccess;
import co.com.claro.financialintegrator.util.UidService;

public class ReporteActivacionesFallidas extends GenericProccess {
	private Logger logger = Logger.getLogger(ReporteActivacionesFallidas.class);
	
	public String nameFile(String fileOutputPrefix) {
		try {
			String fechaName = this.getPros().getProperty("fileOutputFecha")
					.trim();
			String dateFormat = DateUtil.getDateFormFormat(fechaName);
			String extName = this.getPros().getProperty("fileOutputExtExcel").trim();
			String prefix =	fileOutputPrefix.trim();	
			String nameFile = prefix + dateFormat + extName;
			return nameFile;
		} catch (Exception ex) {
			logger.error(
					"Error generando nombre de archivo " + ex.getMessage(), ex);
			;
			return null;
		}

	}

	private void creaReporteActivacionesFallidasExcel(String fileNameReporteActivacionesFallidasExcel, List<String> fileName) {
		
		String excelFileName = fileNameReporteActivacionesFallidasExcel;

		// Create a Workbook and a sheet in it
		XSSFWorkbook workbook=null;
		XSSFSheet sheet=null;

		workbook = new XSSFWorkbook();
		logger.info("Creando archivo Excel");
		sheet=workbook.createSheet("Sheet1");
		
		// Read your input file and make cells into the workbook
		try {
		    String line;
		    Row row;
		    Cell cell;
		    int rowIndex = sheet.getLastRowNum();
		    if(rowIndex>0)rowIndex++;
		    if(fileName!=null) {
			    for (int i = 0; i < fileName.size(); i++) {
			    	line = fileName.get(i);
			        row = sheet.createRow(rowIndex);
			        String[] tokens = line.split("[|]");
			        for(int iToken = 0; iToken < tokens.length; iToken++) {
			            
			        	cell = row.createCell(iToken);
			        	cell.setCellType(Cell.CELL_TYPE_STRING);
			            cell.setCellValue(tokens[iToken]);
			          
			        }
			        rowIndex++;
			    }
		    }
		} catch(Exception e) {
			logger.error("Error en la conversion a EXCEL", e);
		    
		}

		// Write your xlsx file
		try (FileOutputStream outputStream = new FileOutputStream(excelFileName)) {
		    workbook.write(outputStream);		    
		    outputStream.close();
		} catch (IOException e) {
			logger.error("Error en la conversion a EXCEL", e);
		    
		}			
		//Finaliza conversion a EXCEL
	}

	private List<String> consultarActivacionesFallidas(String uid){
		String NOMBRE_PROCESO = "NOMBRE_PROCESO";
		String FECTRANSACCIONREQ = "FECTRANSACCIONREQ";
		String TIPODEIDENTIFICACION = "TIPODEIDENTIFICACION";
		String NOIDENTIFICACION = "NOIDENTIFICACION";
		String NOMBRES = "NOMBRES";
		String APELLIDOS = "APELLIDOS";
		String DIRECCIONRESDCAMPO1 = "DIRECCIONRESDCAMPO1";
		String CIUDADDEPARTAMENTO = "CIUDADDEPARTAMENTO";
		String CODSALUDO = "CODSALUDO";
		String NOTELEFONO1 = "NOTELEFONO1";
		String NOTELEFONO2 = "NOTELEFONO2";
		String VALCUPOTOTALAPROBADO = "VALCUPOTOTALAPROBADO";
		String VALPLAZOINICIAL = "VALPLAZOINICIAL";
		String CODAFINIDAD = "CODAFINIDAD";
		String MARCAEXENCION = "MARCAEXENCION";
		String CUSTCODESERVICIO = "CUSTCODESERVICIO";
		String REFERENCIAEQUIPO = "REFERENCIAEQUIPO";
		String VALIMEI = "VALIMEI";
		String CODMSISDN = "CODMSISDN";
		String CUSTOMERID = "CUSTOMERID";
		String COID = "COID";
		String CODCICLOFACTURACION = "CODCICLOFACTURACION";
		String CUSTCODEMAESTRO = "CUSTCODEMAESTRO";
		String CODREGION = "CODREGION";
		String CODDISTRIBUIDOR = "CODDISTRIBUIDOR";
		String NOMDISTRIBUIDOR = "NOMDISTRIBUIDOR";
		String PROCESO = "PROCESO";
		String CENTROCOSTOS = "CENTROCOSTOS";
		String CODMEDIOENVIOFACTURA = "CODMEDIOENVIOFACTURA";
		String VALEMAIL = "VALEMAIL";
		String NOFACTURA = "NOFACTURA";
		String FECTRANSACCIONRES = "FECTRANSACCIONRES";
		String CODTIPORESPUESTA = "CODTIPORESPUESTA";
		String VALDESCRIPCIONRESPUESTA = "VALDESCRIPCIONRESPUESTA";
		String VALNUMEROCREDITO = "VALNUMEROCREDITO";
		String REFPAGO = "REFPAGO";
		String CODERROR = "CODERROR";
		String DESERROR = "DESERROR";
		String FECHAREGISTRO = "FECHAREGISTRO";
		List<String> lines = new ArrayList<String>();
		String line = null;
		Database _database = null;

		line ="NOMBRE_PROCESO"
				+ "|" + "FECTRANSACCIONREQ"
				+ "|" + "TIPODEIDENTIFICACION"
				+ "|" + "NOIDENTIFICACION"
				+ "|" + "NOMBRES"
				+ "|" + "APELLIDOS"
				+ "|" + "DIRECCIONRESDCAMPO1"
				+ "|" + "CIUDADDEPARTAMENTO"
				+ "|" + "CODSALUDO"
				+ "|" + "NOTELEFONO1"
				+ "|" + "NOTELEFONO2"
				+ "|" + "VALCUPOTOTALAPROBADO"
				+ "|" + "VALPLAZOINICIAL"
				+ "|" + "CODAFINIDAD"
				+ "|" + "MARCAEXENCION"
				+ "|" + "CUSTCODESERVICIO"
				+ "|" + "REFERENCIAEQUIPO"
				+ "|" + "VALIMEI"
				+ "|" + "CODMSISDN"
				+ "|" + "CUSTOMERID"
				+ "|" + "COID"
				+ "|" + "CODCICLOFACTURACION"
				+ "|" + "CUSTCODEMAESTRO"
				+ "|" + "CODREGION"
				+ "|" + "CODDISTRIBUIDOR"
				+ "|" + "NOMDISTRIBUIDOR"
				+ "|" + "PROCESO"
				+ "|" + "CENTROCOSTOS"
				+ "|" + "CODMEDIOENVIOFACTURA"
				+ "|" + "VALEMAIL"
				+ "|" + "NOFACTURA"
				+ "|" + "FECTRANSACCIONRES"
				+ "|" + "CODTIPORESPUESTA"
				+ "|" + "VALDESCRIPCIONRESPUESTA"
				+ "|" + "VALNUMEROCREDITO"
				+ "|" + "REFPAGO"
				+ "|" + "CODERROR"
				+ "|" + "DESERROR"
				+ "|" + "FECHA_REGISTRO";
		lines.add(line);
		try {
			String dataSource = this.getPros()
					.getProperty("DatabaseDataSource").trim();
			// urlWeblogic = null;

			_database = Database.getSingletonInstance(dataSource, null,uid);
			logger.debug("dataSource " + dataSource);
		} catch (Exception ex) {
			logger.error(
					"Error obteniendo información de  configuracion "
							+ ex.getMessage(), ex);
		}
		Statement stmt = null;
		Date date = Calendar.getInstance().getTime(); 
		int dia = date.getDate();
		date.setDate(dia-1);
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");  
        String strDate = dateFormat.format(date);  
		String query = "SELECT P.NOMBRE_PROCESO,"
				+ " AD.FECTRANSACCION_REQ,"
				+ " AD.TIPODEIDENTIFICACION,"
				+ " AD.NOIDENTIFICACION,"
				+ " AD.NOMBRES,"
				+ " AD.APELLIDOS,"
				+ " AD.DIRECCIONRESDCAMPO1,"
				+ " AD.CIUDADDEPARTAMENTO,"
				+ " AD.CODSALUDO,"
				+ " AD.NOTELEFONO1,"
				+ " AD.NOTELEFONO2,"
				+ " AD.VALCUPOTOTALAPROBADO,"
				+ " AD.VALPLAZOINICIAL,"
				+ " AD.CODAFINIDAD,"
				+ " AD.MARCAEXENCION,"
				+ " AD.CUSTCODESERVICIO,"
				+ " AD.REFERENCIAEQUIPO,"
				+ " AD.VALIMEI,"
				+ " AD.CODMSISDN,"
				+ " AD.CUSTOMERID,"
				+ " AD.COID,"
				+ " AD.CODCICLOFACTURACION,"
				+ " AD.CUSTCODEMAESTRO,"
				+ " AD.CODREGION,"
				+ " AD.CODDISTRIBUIDOR,"
				+ " AD.NOMDISTRIBUIDOR,"
				+ " AD.PROCESO,"
				+ " AD.CENTROCOSTOS,"
				+ " AD.CODMEDIOENVIOFACTURA,"
				+ " AD.VALEMAIL,"
				+ " AD.NOFACTURA,"
				+ " AD.FECTRANSACCION_RES,"
				+ " AD.CODTIPORESPUESTA,"
				+ " AD.VALDESCRIPCIONRESPUESTA,"
				+ " AD.VALNUMEROCREDITO,"
				+ " AD.REFPAGO,"
				+ " AD.CODERROR,"
				+ " AD.DESERROR,"
				+ " AD.FECHA_REGISTRO"
				+ " FROM INT_REG_ASCARD_AUD_DETALLADA AD, INT_PROCESO P"
				+ " WHERE AD.CODTIPORESPUESTA <> '00'"
				+ " and P.ID_PROCESO = AD.PROCESO"
				+ " AND AD.FECTRANSACCION_REQ BETWEEN"
				+ " TO_DATE('" + strDate + " 00:00:00', 'DD/MM/YYYY HH24:MI:SS') AND"
				+ " TO_DATE('" + strDate + " 23:59:59', 'DD/MM/YYYY HH24:MI:SS')"
				+ " ORDER BY 2";
		try {
			// Se invoca procedimiento
			stmt=_database.getConn(uid).createStatement();
			ResultSet result = stmt.executeQuery(query);			
			while(result.next()) {
				NOMBRE_PROCESO = result.getString("NOMBRE_PROCESO");
				FECTRANSACCIONREQ = String.valueOf(result.getTimestamp("FECTRANSACCION_REQ"));
				TIPODEIDENTIFICACION = result.getString("TIPODEIDENTIFICACION");
				NOIDENTIFICACION = result.getString("NOIDENTIFICACION");
				NOMBRES = result.getString("NOMBRES");
				APELLIDOS = result.getString("APELLIDOS");
				DIRECCIONRESDCAMPO1 = result.getString("DIRECCIONRESDCAMPO1");
				CIUDADDEPARTAMENTO = result.getString("CIUDADDEPARTAMENTO");
				CODSALUDO = result.getString("CODSALUDO");
				NOTELEFONO1 = result.getString("NOTELEFONO1");
				NOTELEFONO2 = result.getString("NOTELEFONO2");
				VALCUPOTOTALAPROBADO = result.getString("VALCUPOTOTALAPROBADO");
				VALPLAZOINICIAL = result.getString("VALPLAZOINICIAL");
				CODAFINIDAD = result.getString("CODAFINIDAD");
				MARCAEXENCION = result.getString("MARCAEXENCION");
				CUSTCODESERVICIO = result.getString("CUSTCODESERVICIO");
				REFERENCIAEQUIPO = result.getString("REFERENCIAEQUIPO");
				VALIMEI = result.getString("VALIMEI");
				CODMSISDN = result.getString("CODMSISDN");
				CUSTOMERID = result.getString("CUSTOMERID");
				COID = result.getString("COID");
				CODCICLOFACTURACION = result.getString("CODCICLOFACTURACION");
				CUSTCODEMAESTRO = result.getString("CUSTCODEMAESTRO");
				CODREGION = result.getString("CODREGION");
				CODDISTRIBUIDOR = result.getString("CODDISTRIBUIDOR");
				NOMDISTRIBUIDOR = result.getString("NOMDISTRIBUIDOR");
				PROCESO = result.getString("PROCESO");
				CENTROCOSTOS = result.getString("CENTROCOSTOS");
				CODMEDIOENVIOFACTURA = result.getString("CODMEDIOENVIOFACTURA");
				VALEMAIL = result.getString("VALEMAIL");
				NOFACTURA = result.getString("NOFACTURA");
				FECTRANSACCIONRES = String.valueOf(result.getTimestamp("FECTRANSACCION_RES"));
				CODTIPORESPUESTA = result.getString("CODTIPORESPUESTA");
				VALDESCRIPCIONRESPUESTA = result.getString("VALDESCRIPCIONRESPUESTA");
				VALNUMEROCREDITO = result.getString("VALNUMEROCREDITO");
				REFPAGO = result.getString("REFPAGO");
				CODERROR = result.getString("CODERROR");
				DESERROR = result.getString("DESERROR");
				FECHAREGISTRO = String.valueOf(result.getTimestamp("FECHA_REGISTRO"));
				
				line=NOMBRE_PROCESO
				+ "|" + FECTRANSACCIONREQ
				+ "|" + TIPODEIDENTIFICACION
				+ "|" + NOIDENTIFICACION
				+ "|" + NOMBRES
				+ "|" + APELLIDOS
				+ "|" + DIRECCIONRESDCAMPO1
				+ "|" + CIUDADDEPARTAMENTO
				+ "|" + CODSALUDO
				+ "|" + NOTELEFONO1
				+ "|" + NOTELEFONO2
				+ "|" + VALCUPOTOTALAPROBADO
				+ "|" + VALPLAZOINICIAL
				+ "|" + CODAFINIDAD
				+ "|" + MARCAEXENCION
				+ "|" + CUSTCODESERVICIO
				+ "|" + REFERENCIAEQUIPO
				+ "|" + VALIMEI
				+ "|" + CODMSISDN
				+ "|" + CUSTOMERID
				+ "|" + COID
				+ "|" + CODCICLOFACTURACION
				+ "|" + CUSTCODEMAESTRO
				+ "|" + CODREGION
				+ "|" + CODDISTRIBUIDOR
				+ "|" + NOMDISTRIBUIDOR
				+ "|" + PROCESO
				+ "|" + CENTROCOSTOS
				+ "|" + CODMEDIOENVIOFACTURA
				+ "|" + VALEMAIL
				+ "|" + NOFACTURA
				+ "|" + FECTRANSACCIONRES
				+ "|" + CODTIPORESPUESTA
				+ "|" + VALDESCRIPCIONRESPUESTA
				+ "|" + VALNUMEROCREDITO
				+ "|" + REFPAGO
				+ "|" + CODERROR
				+ "|" + DESERROR
				+ "|" + FECHAREGISTRO;
			lines.add(line);
			}

		} catch (SQLException e) {
			logger.error(
					"ERROR consultando : Activaciones Fallidas : " + e.getMessage(),
					e);
		} catch (Exception e) {
			logger.error("ERROR consultando : Activaciones Fallidas : : " + e.getMessage(),	e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(
							"Error cerrando Stament BSCS "
									+ e.getMessage(), e);
				}
			}
		}
		_database.disconnet(uid);
		_database.disconnetCs(uid);
		return lines;
	}
	
	public void sendMail(String path,String uid){
		try {
			this.initPropertiesMails(uid);
			this.getMail().sendMail(path);
		} catch (FinancialIntegratorException e) {
			logger.error("error enviando reporte de activaciones fallidas "+e.getMessage(),e );
		}catch (Exception e) {
			logger.error("error enviando reporte de activaciones fallidas "+e.getMessage(),e );
		}
	}
	
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
		String path_processExcel = this.getPros().getProperty("path_processExcel");
		String pathBSCS = this.getPros().getProperty("pathBSCS");
		String fileOutputPrefix = this.getPros().getProperty("fileOutputPrefix").trim();
		try {
		FileUtil.createDirectory(this.getPros().getProperty("path").trim());
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ path_processExcel);
		FileUtil.createDirectory(this.getPros().getProperty("path").trim()
				+ pathBSCS);
		} catch (FinancialIntegratorException e) {
			logger.error("Error creando directorio"
			+ e.getMessage());
		}
		
		List<String> lineFileCreate = consultarActivacionesFallidas(uid);
		
		String fileNameOutput =this.nameFile(fileOutputPrefix);
		
		String fileName =path + path_processExcel + fileNameOutput;
		
		String fileNameBSCS =path + pathBSCS + fileNameOutput;

		creaReporteActivacionesFallidasExcel(fileName, lineFileCreate);
		
		try { 
			FileUtil.move(fileName, fileNameBSCS);
		} catch (FinancialIntegratorException e) {
			logger.error("Error moviendo archivo"
					+ e.getMessage());
		}
		logger.info("Archivo Procesado Exitosamente");
		String observacion = "Archivo Procesado Exitosamente";
		registrar_auditoriaV2(fileNameOutput, observacion,uid);
	
	}
	
}
