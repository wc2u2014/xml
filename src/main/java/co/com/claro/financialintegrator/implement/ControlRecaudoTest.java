package co.com.claro.financialintegrator.implement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.net.aso.f;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FileUtilAPI.TemplateRecaudoBancosConsolidado;
import co.com.claro.FileUtilAPI.TemplateRecaudoSicacom;
import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FileUtilAPI.TypeRegister;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.FinancialIntegratorsUtils.NumberUtils;
import co.com.claro.financialintegrator.conifguration.MetadataConf;
import co.com.claro.financialintegrator.conifguration.ProccesingAutomaticConf;

import java.sql.Struct;
import java.sql.Array;

import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;

public class ControlRecaudoTest {

	public Connection getConnection() {

		System.out.println("-------- Oracle JDBC Connection Testing ------");
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.out.println("Where is your Oracle JDBC Driver?");
			e.printStackTrace();
			return null;
		}

		System.out.println("Oracle JDBC Driver Registered!");
		Connection connection = null;
		try {

			connection = DriverManager.getConnection(
					"jdbc:oracle:thin:@localhost:9991:INTEGQA", "INTEGTEST",
					"1Nt3_201608");

		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return null;
		}

		if (connection != null) {
			System.out.println("You made it, take control your database now!");
		} else {
			System.out.println("Failed to make connection!");
		}

		return connection;
	}

	public static FileConfiguration config_Asobancaria_2001(String file) {
		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		List<Type> _types = new ArrayList<Type>();
		// ///////////////////// HEADER ////////////////////////
		// Tipo_registro
		Type type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("Tipo_registro");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// RNIT Empresa facturadora
		type = new Type();
		type.setLength(10);
		type.setSeparator("");
		type.setName("NIT Empresa facturadora");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(2);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// Fecha de pago
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.FECCOP);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// Código entidad recaudadora
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.OFCCAP);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString("0"); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Número de cuenta
		type = new Type();
		type.setLength(17);
		type.setSeparator("");
		type.setName("Número de cuenta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// Fecha del archivo
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("Fecha del archivo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// Hora del archivo
		type = new Type();
		type.setLength(4);
		type.setSeparator("");
		type.setName("Hora_del_archivo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// Modificador de archivo
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Modificador_de_archivo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		// Tipo de cuenta
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("Tipo_de_cuenta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		type = new Type();
		type.setLength(107);
		type.setSeparator("");
		type.setName("Reservado");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);

		_fileConfiguration.setTypesHeader(_types);

		// ///////////////////// BODY ////////////////////////

		_types = new ArrayList<Type>();

		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("Tipo_registro");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Referencia de pago principal
		type = new Type();
		type.setLength(48);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.NUMTAR);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(2);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Valor pagado
		type = new Type();
		type.setLength(14);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.VALTRA);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Procedencia de pago
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("Procedencia_de _pago");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Medios de pago
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("Secuencia");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// No. de Operación
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("No_de_Operacion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// No. de Autorización
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.CODAUT);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//

		// Código de la entidad financiera debitada
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("Código de la entidad financiera debitada");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Código de sucursal
		type = new Type();
		type.setLength(4);
		type.setSeparator("");
		type.setName("Código de sucursal");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Secuencia
		type = new Type();
		type.setLength(7);
		type.setSeparator("");
		type.setName("Código de sucursal");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Causal de Devolución
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("Causal_de_Devolucion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// Reservado
		type = new Type();
		type.setLength(65);
		type.setSeparator("");
		type.setName("Causal_de_Devolucion");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(false);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		// / //Por defecto para archivo de salida

		// ORIGEN
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.ORIGEN);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString("10"); // Default
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//

		// PLZTRN
		type = new Type();
		type.setLength(1000);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.PLZTRN);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString("01"); // Default
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		type = new Type();
		type.setLength(17);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.VALPRO);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString("00000000000000000"); // Default
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		type = new Type();
		type.setLength(17);
		type.setSeparator("");
		type.setName(TemplateRecaudoBancosConsolidado.VALTOT);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(1);
		type.setValueString(""); // Default
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);

		_fileConfiguration.setTypes(_types);
		_fileConfiguration.setTypesFooter(_types);
		return _fileConfiguration;
	}

	public FileConfiguration configurationRecaudoSicacom(String file) {

		FileConfiguration _fileConfiguration = new FileConfiguration();
		_fileConfiguration.setFileName(file);
		// header
		List<Type> _typesHeader = new ArrayList<Type>();
		//
		Type typeHeader = new Type();
		typeHeader.setLength(3);
		typeHeader.setSeparator("");
		typeHeader.setName("TIPOREGISTRO");
		typeHeader.setTypeData(new ObjectType(String.class.getName(), ""));
		_typesHeader.add(typeHeader);

		typeHeader = new Type();
		typeHeader.setLength(13);
		typeHeader.setSeparator("");
		typeHeader.setName("NIT Claro");
		typeHeader.setTypeData(new ObjectType(String.class.getName(), ""));
		_typesHeader.add(typeHeader);

		typeHeader = new Type();
		typeHeader.setLength(8);
		typeHeader.setSeparator("");
		typeHeader.setName("Fecha del proceso");
		typeHeader.setTypeData(new ObjectType(String.class.getName(), ""));
		_typesHeader.add(typeHeader);
		_fileConfiguration.setTypesHeader(_typesHeader);
		// Body
		List<Type> _types = new ArrayList<Type>();
		//
		Type type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("TIPOREGISTRO");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(25);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.NUMTAR);
		type.setTypeData(new ObjectType(Long.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(14);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.USUARIO);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(31);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.NOMBRECENTRO);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.TIPOCENTRO);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.VALTOT);
		type.setTypeData(new ObjectType(Float.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(7);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.CODIGOCENTRO);
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		_types.add(type);
		//
		type = new Type();
		type.setLength(14);
		type.setSeparator("");
		type.setName(TemplateRecaudoSicacom.FECCOP);
		type.setTypeData(new ObjectType("java.sql.Date", "yyyyMMddHHmmss"));
		// type.setTypeData(new ObjectType(Calendar.class.getName(),
		// "yyyMMddHHmmss"));
		_types.add(type);
		//
		_fileConfiguration.setTypes(_types);

		return _fileConfiguration;
	}

	/**
	 * Consulta procedimiento para obtener metadata y parametros para lectura de
	 * archivo y procesamiento
	 * 
	 * @param fileName
	 * @param path
	 * @return
	 */
	private ProccesingAutomaticConf getConf(String fileName, String path) {
		ProccesingAutomaticConf conf = new ProccesingAutomaticConf();
		conf.setFileConfiguration(this.getConfigurationByFile(fileName, path,
				conf));
		conf.setTypeStruct("TYP_TBL_SICACOM");
		conf.setTypeArray("TYP_CARGUE_SICACOM");
		System.out.println("Conf : " + conf.getFields().size());
		return conf;
	}

	/**
	 * Contruye objeto de configuración
	 * 
	 * @param fileName
	 * @param path
	 * @return
	 */
	private FileConfiguration getConfigurationByFile(String fileName,
			String path, ProccesingAutomaticConf conf) {
		conf.setFields(new ArrayList<MetadataConf>());
		MetadataConf mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.NUMTAR);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.USUARIO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.CODIGOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.NOMBRECENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.VALTOT);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.FECCOP);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		mf = new MetadataConf();
		mf.setName(TemplateRecaudoSicacom.TIPOCENTRO);
		mf.setAplicaCargue(true);
		mf.setType("BODY");
		conf.getFields().add(mf);
		//
		//
		conf.setNumFieldsHeader(0);
		return this.configurationRecaudoSicacom(path);

	}

	/**
	 * Se procesa el archivo en bloque y se va ejecutando en base de datos
	 * 
	 * @param conf
	 * @return
	 */
	private Boolean read_file_block(ProccesingAutomaticConf conf) {
		// Configuración de archivo
		FileConfiguration inputFile = conf.getFileConfiguration();
		//
		Long limit_block = Long.parseLong("10");
		Long limitCount = 0L;
		Long sizeFile = 0L;
		//
		System.out.println("READ FILE BLOCK FILE BLOCK");
		List<FileOuput> lines = new ArrayList<FileOuput>();
		//
		File f = null;
		BufferedReader b = null;
		String nameFile = "";
		// Result process
		Boolean result = true;
		try {
			f = new File(inputFile.getFileName());
			nameFile = f.getName();
			b = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "ISO-8859-1"));
			String line = "";
			String lastLine = "";
			FileOuput headerLine = null;
			int index = 0;
			int header = (inputFile.getTypesHeader() == null ? 0 : 1);
			while ((line = b.readLine()) != null) {
				System.out.println(line);
				if (header == 0) {
					if (!line.trim().equals("")) {
						try {
							FileOuput _FileOuput = FileUtil.readLine(
									inputFile.getTypes(), line);
							lines.add(_FileOuput);
							lastLine = line;
							index++;
						} catch (Exception ex) {
							// logger.error("Error leyendo linea " + line, ex);
							System.out.println("Error leyendo linea:: " + line);
							ex.printStackTrace();
						}

					}
				} else {
					headerLine = FileUtil.readLine(inputFile.getTypesHeader(),
							line);
				}
				header = 0;
				// Se revisa el limite para la creacion en invocacion del
				// proceso
				if (limitCount >= limit_block) {
					System.out.println("-- PROCESS BLOCK ... ");
					result = _proccess_block(lines, headerLine, conf);
					lines.clear();
					limitCount = 0L;
					// logger.debug("Lines new size " + lines.size());
				}
				limitCount++;
				sizeFile++;
			}
			// se verifica que no hayan lineas para procesae
			if (lines.size() > 0) {
				result = _proccess_block(lines, headerLine, conf);
			}
		} catch (Exception ex) {
			System.err.println("Error en proceso :" + ex.getMessage());
			ex.printStackTrace();
		}
		return true;
	}

	/**
	 * Se procesan las lineas del archivo
	 * 
	 * @param lines
	 * @param headerLine
	 */
	public Boolean _proccess_block(List<FileOuput> lines, FileOuput headerLine,
			ProccesingAutomaticConf conf) {
		List<Object[]> roles = new LinkedList<Object[]>();
		Object[] objecHeader = new Object[conf.getNumFieldsHeader()];
		int headerPos = 0;
		if (headerLine != null) {
			for (MetadataConf f : conf.getFields()) {
				System.out.println(f.getName() + " - " + f.getType());
				if (f.getType().equals("HEADER")) {
					try {
						objecHeader[headerPos] = headerLine
								.getType(f.getName()).getValueString();
					} catch (FinancialIntegratorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				headerPos++;
			}
			objecHeader = null;
		}

		for (FileOuput fo : lines) {
			if (fo.getTypes() != null) {
				Object[] objecs = new Object[conf.getFields().size() + 3];
				int pos = 0;
				if (objecHeader != null) {
					for (Object obj : objecHeader) {
						objecs[pos] = obj;
						pos++;
					}
				}
				System.out.println("Header ... " + Arrays.toString(objecHeader)
						+ "Size " + conf.getFields().size() + "-" + pos);
				for (MetadataConf f : conf.getFields()) {
					if (f.getType().equals("BODY")) {
						try {
							System.out.println("pos "
									+ pos
									+ " - "
									+ fo.getType(f.getName()).getValue()
									+ fo.getType(f.getName()).getValue()
											.getClass());
							objecs[pos] = fo.getType(f.getName()).getValue();

							System.out.println("BODY:" + f.getName() + " - "
									+ fo.getType(f.getName()).getValue());
						} catch (FinancialIntegratorException e) {
							System.out.println("eror agregando obj :-> "
									+ e.getMessage() + " name: " + f.getName());
							e.printStackTrace();
						} catch (Exception e) {
							System.out.println("eror agregando obj :-> "
									+ e.getMessage() + " name: " + f.getName()
									+ " fo " + fo.getTypes());
							e.printStackTrace();
						}
						pos++;
					}

				}
				objecs[pos] = "ASCARD_COM_20181217210508.txt";
				pos++;
				objecs[pos] = "";
				pos++;
				objecs[pos] = new java.sql.Date(Calendar.getInstance()
						.getTimeInMillis());
				System.out.println("Add " + Arrays.toString(objecs));
				roles.add(objecs);
			}
		}
		execute_prod(roles, conf);
		return true;
	}

	private Boolean execute_prod2() {

		Connection conn = null;
		try {

			List<Object[]> roles = new LinkedList<Object[]>();
			String value = "1000000089";
			value = value.substring(0, value.length() - 2) + "."
					+ value.substring(value.length() - 2);
			Object[] rol1 = new Object[] {
					95,
					new java.sql.Date(Calendar.getInstance().getTime()
							.getTime()),
					new BigDecimal("47100.00"),
					3,
					14,
					0,
					4015925907L,
					new java.sql.Timestamp(Calendar.getInstance().getTime()
							.getTime()),
					987654,
					16023761,
					"C",
					new java.sql.Date(Calendar.getInstance().getTime()
							.getTime()), 1, 0, 110429, 0, null, 0, "ECM4292",
					" 8.22422616", "CRMVD181217.TXT" };
			roles.add(rol1);
			Object[] rol2 = new Object[] {
					"95a",
					new java.sql.Date(Calendar.getInstance().getTime()
							.getTime()),
					new BigDecimal("47100.00"),
					3,
					14,
					0,
					4015925907L,
					new java.sql.Timestamp(Calendar.getInstance().getTime()
							.getTime()),
					987654,
					16023761,
					"C",
					new java.sql.Date(Calendar.getInstance().getTime()
							.getTime()), 1, 0, 110429, 0, null, 0, "ECM4292",
					" 8.22422616", "CRMVD181217.TXT" };
			roles.add(rol2);
			Struct[] struct = new Struct[roles.size()];
			System.out.println(Arrays.toString(rol1));
			conn = getConnection();
			int i = 0;
			int error = 0;
			for (Object[] rol : roles) {
				try {
					System.out.println("rol " + i + ": " + rol[0] + "-"
							+ rol[1]);
					struct[i] = conn.createStruct("TYPE_TBL_MOVIMIENTOS", rol);
					i++;
				} catch (SQLException e) {
					System.out.println("Error creando struct " + e.getMessage()
							+ " - " + e.getErrorCode() + Arrays.toString(rol));
					error++;
				}catch (Exception e) {
					System.out.println("Error creando struct " + e.getMessage()
							+ " - "  + Arrays.toString(rol));
					error++;
				}
			}
			/**
			 * si existe error creando structos
			 */
			System.out.println("No se han creado los siguientes Structs "+error);
			if (error>0){
				System.out.println("No se han creado los siguientes Structs "+error);
				List<Struct> structWthNull = new ArrayList<Struct>();

				for (java.sql.Struct s : struct) {
					if (s!=null){
						structWthNull.add(s);
					}					
				}
				struct = structWthNull.toArray(new java.sql.Struct[structWthNull.size()]);
			}
			try {
				Array array = ((OracleConnection) conn).createOracleArray(
						"TYP_CARGUE_MOVIMIENTOS", struct);
				// PreparedStatement ps = (OracleCallableStatement) conn
				// .prepareCall("begin call PKG_CONTROL_RECAUDO.prc_cargue_archivos_bancos(?,?,?); end;");
				OracleCallableStatement cs = (OracleCallableStatement) conn
						.prepareCall("call PKG_CONTROL_RECAUDO.prc_cargue_arch_movimentos(?,?,?)");
				cs.setArray(1, array);
				cs.registerOutParameter(2, java.sql.Types.INTEGER);
				cs.registerOutParameter(3, java.sql.Types.VARCHAR);
				cs.executeUpdate();
				conn.close();
				cs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * Se ejecuta el procesdimiento
	 * 
	 * @param roles
	 * @return
	 */
	private Boolean execute_prod(List<Object[]> roles,
			ProccesingAutomaticConf conf) {

		Connection conn = null;
		try {
			// se crea objeto struct
			Struct[] struct = new Struct[roles.size()];
			int i = 0;
			conn = getConnection();
			for (Object[] rol : roles) {
				System.out.println("Role " + i + " : " + Arrays.toString(rol));
				try {
					struct[i] = conn.createStruct(conf.getTypeStruct(), rol);
				} catch (SQLException e) {
					System.out
							.println("Error creando struct " + e.getMessage());
				}
				i++;
			}
			try {
				Array array = ((OracleConnection) conn).createOracleArray(
						conf.getTypeArray(), struct);
				PreparedStatement ps = (OracleCallableStatement) conn
						.prepareCall("begin pkg_control_recaudo.prc_cargue_archivos_sicacom(:1,:2); end;");
				ps.setString(1, "ASCARD_COM_20181217210508.txt");
				ps.setArray(2, array);
				ps.execute();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

	@Deprecated
	public void insertarRegistros() throws SQLException {

		List<Object[]> roles = new LinkedList<Object[]>();

		// Object[] rol1 = new Object[] { 4042940135L, "ECM2506","1035"
		// ,"Cvs Smartmobile Tintal",new Float("59946.0"),new
		// java.sql.Date(Calendar.getInstance().getTimeInMillis(),)};

		// roles.add(rol1);
		// roles.add(rol2);

		Struct[] struct = new Struct[roles.size()];

		Connection conn = getConnection();
		int i = 0;
		for (Object[] rol : roles) {
			System.out.println("rol " + i + ": " + rol[0] + "-" + rol[1]);
			struct[i] = conn.createStruct("ROL_TYPE", rol);
			i++;
		}
	}

	public static void main(String... arg) {
		ControlRecaudoTest con = new ControlRecaudoTest();
		System.out.println("Control Recaudo Test");
		/*
		 * File fileProcess = new File(
		 * "D:\\OracleSources\\Files\\ControlRecaudos\\ASCARD_COM_20181217210508.txt"
		 * ); try { ProccesingAutomaticConf conf =
		 * con.getConf(fileProcess.getName(), fileProcess.getAbsolutePath());
		 * System.out.println(conf.getFields()); con.read_file_block(conf); }
		 * catch (Exception e) { e.printStackTrace(); }
		 */
		con.execute_prod2();
	}

}
