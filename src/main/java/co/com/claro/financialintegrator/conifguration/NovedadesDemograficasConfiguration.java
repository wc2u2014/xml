package co.com.claro.financialintegrator.conifguration;

import co.com.claro.FileUtilAPI.Type;
import co.com.claro.FileUtilAPI.FileConfiguration;
import co.com.claro.FileUtilAPI.FileOuput;
import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FileUtilAPI.ObjectType;
import co.com.claro.FinancialIntegratorsUtils.ObjectUtils;
import co.com.claro.financialintegrator.database.Database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Clase que contiene los templates de los archivos de Novedades demograficas de
 * entradas y salidas
 * 
 * @author Oracle
 *
 */
public class NovedadesDemograficasConfiguration {
    private static Logger logger = Logger.getLogger(NovedadesDemograficasConfiguration.class);
	/**
	 * Contiene el template para crear archivo de salida de novedades
	 * demograficas
	 * 
	 * @param nombreDelCampo
	 * @return
	 */
	public static List<Type> typesTemplateNovedadesNoMonetarias(
			String nombreDelCampo) {
 long startTime = System.currentTimeMillis();
        logger.info("Iniciando m�todo: typesTemplateNovedadesNoMonetarias");
        logger.info("Request - nombreDelCampo: " + (nombreDelCampo != null ? nombreDelCampo : "N/A"));

		List<Type> _types = new ArrayList<Type>();
        try {
		Type type = new Type();
		type.setLength(9);
		type.setSeparator("");
		type.setName("BIN");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("");
		type.setValueString("");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("NumeroProducto");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("C�digodeTransacci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("004");
		type.setValueString("004");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("EstadodelCr�dito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("TipodeCr�dito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("Nemot�cnico");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("  ");
		type.setValueString("  ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("GrupodeAfinidad");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("L�mitedeCr�ditoenPesos");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("LimiteCr�ditoenD�lares");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("N�merodeCr�ditoCompa��a");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("N�merodeCr�ditoAmparada");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("C�digodelBanco");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("N�merodeCuentaCorriente");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("LimiteAvancesenPesos");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("LimiteAvancesenD�lares");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000000000000");
		type.setValueString("000000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("CiclodeFacturaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("GrupodeSeguro");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("016");
		type.setValueString("016");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("GrupodeManejo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechadeProceso");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechadeRecepci�nPla");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("C�d.Reexpedici�n Cr�dito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("C�digoRealcedeCr�dito");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("IndicadorP/CargoaCuenta");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("C�digodeOficina");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(10);
		type.setSeparator("");
		type.setName("Usuario");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("MASIVO    ");
		type.setValueString("MASIVO    ");
		type.setComplement(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("OrigendelaTransacci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("006");
		type.setValueString("006");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("EstadodelaTransacci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("TipodeProceso");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(13);
		type.setSeparator("");
		type.setName("Concectivo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0000000000001");
		type.setValueString("0000000000001");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico5");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico6");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico7");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico8");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("EspacioNum�rico9");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000000");
		type.setValueString("00000000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechaDisponible1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechaDisponible2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechaDisponible3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechaDisponible4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechaDisponible5");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("Nombrepararealce");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(ObjectUtils.complement(" ", " ", 30, true));
		type.setValueString(ObjectUtils.complement(" ", " ", 30, true));
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("NombrepararealceEmpresa");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(ObjectUtils.complement(" ", " ", 30, true));
		type.setValueString(ObjectUtils.complement(" ", " ", 30, true));
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("C�digoCiudadImpuesto");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(11);
		type.setSeparator("");
		type.setName("N�merodeIdentificaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000000");
		type.setValueString("00000000000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("TipodeIdentificaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(3);
		type.setSeparator("");
		type.setName("TipodeId.Amparador");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000");
		type.setValueString("000");
		type.setComplement(true);
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("nombreDelCampo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(nombreDelCampo);
		type.setValueString(nombreDelCampo);
		type.setComplement(true);
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement("");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nIndUno");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("IndDisponible2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Dos");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("IndDisponible3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Tres");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Cuatro");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible5");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Cinco");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setLeftOrientation(false);
		type.setComplement(true);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible6");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Seis");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible7");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Siete");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible8");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Ocho");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible9");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Nueve");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Ind.Disponible10");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Descripci�nInd.Diez");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("C�d.Observaci�nBloqueo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("TasadeVentas0");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("TasadeVentas1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("TasadeVentas2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("TasadeVentas3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("TasadeVentas4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("CodigoAbogadoFormat");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(10);
		type.setSeparator("");
		type.setName("UsuarioActualizaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("FechadeActualizaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000");
		type.setValueString("00000000");
		type.setComplement(true);
		type.setStringcomplement("00000000");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("HoradeActualizaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000");
		type.setValueString("000000");
		type.setComplement(true);
		type.setStringcomplement("000000");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(10);
		type.setSeparator("");
		type.setName("DispositivodeActualizaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("Oficina Radicaci�n");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setStringcomplement("00000");
		type.setLeftOrientation(true);
		_types.add(type);
		 logger.info("Response - total types generados: " + _types.size());
		return _types;
	
        } catch (Exception e) {
            logger.error("Error en el m�todo typesTemplateNovedadesNoMonetarias", e);
            return _types;
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info("Tiempo de ejecuci�n del m�todo typesTemplateNovedadesNoMonetarias: " + (endTime - startTime) + " ms");
	}
    }


	/**
	 * Configuraci�n archivo de salida de Novedades monetarias
	 * 
	 * @param numeroProducto
	 * @param nombreDelCampo
	 * @return
	 */
	public static List<Type> typesTemplateNovedadesDemograficas() {
 long startTime = System.currentTimeMillis();
        logger.info("Iniciando m�todo: typesTemplateNovedadesDemograficas");
        logger.info("Request: N/A");

		List<Type> _types = new ArrayList<Type>();
        try {
		Type type = new Type();
		type.setLength(14);
		type.setSeparator("");
		type.setName("BIN");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000000987654");
		type.setValueString("00000000987654");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(16);
		type.setSeparator("");
		type.setName("NumeroProducto");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("IndicadorActualizacionMasiva");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("TipoIdentificacionAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("NumeroIdentificacionAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setStringcomplement("0");
		type.setLeftOrientation(true);
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("CodigoOficinaAC");	//UsoFuturo1
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("UsoFuturo2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("UsoFuturo3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("UsoFuturo4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("CorreoElectronico1AC");	//UsoFuturo5
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(26);
		type.setSeparator("");
		type.setName("UsoFuturo6");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(8);
		type.setSeparator("");
		type.setName("TipoPersonaAC");	//UsoFuturo7
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Sexo");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("9");
		type.setValueString("9");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("Estado Civil");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("9");
		type.setValueString("9");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("CorreoElectronico2AC");	//UsoFuturo8
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("CiudadDepartamentoAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(2);
		type.setSeparator("");
		type.setName("CodigoDepartamento");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("CodigoCiudadDeResidencia");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("UsoFuturo9");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("000000");
		type.setValueString("000000");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		/*
		 * type = new Type(); type.setLength(5); type.setSeparator("");
		 * type.setName("UsoFuturo10"); type.setTypeData(new
		 * ObjectType(String.class.getName(), "")); type.setPosicion(0);
		 * type.setValue(" "); type.setValueString(" ");
		 * type.setComplement(true); type.setLeftOrientation(false);
		 * type.setStringcomplement(" "); _types.add(type);
		 */
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("DireccionCorrespondencia1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("DireccionCorrespondencia2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("CodigoDeCiudadCorrespondencia");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("00000");
		type.setValueString("00000");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("UsoFuturo10");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(5);
		type.setSeparator("");
		type.setName("UsoFuturo11");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(14);
		type.setSeparator("");
		type.setName("TelefonoCasa");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(14);
		type.setSeparator("");
		type.setName("TelefonoOficina");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("CodigoUnicoCliente");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(true);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("TipoReferencia1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("NombredeRef.1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("Direcci�ndeRef.1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Tel�fonodeRef.1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("Descripci�nRef.1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("CiudadRef.1");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("TipoReferencia2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("NombredeRef.2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("Direcci�ndeRef.2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Tel�fonodeRef.2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("Descripci�nRef.2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("CiudadRef.2");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("TipoReferencia3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("NombredeRef.3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("Direcci�ndeRef.3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Tel�fonodeRef.3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("Descripci�nRef.3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("CiudadRef.3");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(15);
		type.setSeparator("");
		type.setName("TipoReferencia4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("NombredeRef.4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("Direcci�ndeRef.4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(20);
		type.setSeparator("");
		type.setName("Tel�fonodeRef.4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(30);
		type.setSeparator("");
		type.setName("Descripci�nRef.4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement(" ");
		_types.add(type);
		//
		type = new Type();
		type.setLength(6);
		type.setSeparator("");
		type.setName("CiudadRef.4");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue("0");
		type.setValueString("0");
		type.setComplement(true);
		type.setLeftOrientation(false);
		type.setStringcomplement("0");
		_types.add(type);
		//
		type = new Type();
		type.setLength(230);
		type.setSeparator("");
		type.setName("Direcci�ndeResidencia");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(50);
		type.setSeparator("");
		type.setName("CustcodedeservicioAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(50);
		type.setSeparator("");
		type.setName("CustomerIDdeservicioAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(1);
		type.setSeparator("");
		type.setName("C�digoSaludoAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);

		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("NombresAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		type = new Type();
		type.setLength(40);
		type.setSeparator("");
		type.setName("ApellidosAC");
		type.setTypeData(new ObjectType(String.class.getName(), ""));
		type.setPosicion(0);
		type.setValue(" ");
		type.setValueString(" ");
		type.setComplement(true);
		type.setStringcomplement(" ");
		type.setLeftOrientation(false);
		_types.add(type);
		//
		/*
		 * type = new Type(); type.setLength(80); type.setSeparator("");
		 * type.setName("NombresCortoAC"); type.setTypeData(new
		 * ObjectType(String.class.getName(), "")); type.setPosicion(0);
		 * type.setValue(" "); type.setValueString(" ");
		 * type.setComplement(true); type.setStringcomplement(" ");
		 * type.setLeftOrientation(false); _types.add(type);
		 */
		//
                
            logger.info("Response - total types generados: " + _types.size());
            return _types;
                
        } catch (Exception e) {
            logger.error("Error en el m�todo typesTemplateNovedadesDemograficas", e);
		return _types;
        } finally {
            long endTime = System.currentTimeMillis();
            logger.info("Tiempo de ejecuci�n del m�todo typesTemplateNovedadesDemograficas: " + (endTime - startTime) + " ms");
        }
	}
}
