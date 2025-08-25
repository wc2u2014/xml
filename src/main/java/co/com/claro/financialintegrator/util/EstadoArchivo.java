package co.com.claro.financialintegrator.util;

public enum EstadoArchivo {
ERRORCOPIADO(3,"Error copiando archivos  para el proceso"),
NOCONSOLIDADO(4,"No se ha consolidado el archivo"),
NOCONSOLIDADOPENDIENTE(5,"No consolidado, pendiente para el día siguiente"),
CONSOLIDADO(6,"Se ha consolidado el archivo");

private final int estado;   
private final String descripcion; 

EstadoArchivo (int estado, String descripcion){
	this.estado = estado;
	this.descripcion = descripcion;
}

public int getEstado() {
	return estado;
}

public String getDescripcion() {
	return descripcion;
}
}
