package co.com.claro.financialintegrator.domain;

public class CargueMasivo {

	private String codigoOperacion;
	private String aplicacion;
	private String bin;
	private String origen;
	private String transaccion;
	private String concepto;
	private String secuencia;
	private String cuentaContable;
	private String tipoImputacion;
	private String detalleAbreviado;
	private String centroBeneficio;
	private String codigoBanco;
	private String claseDocumentoContable;
	private String tipoDeReferencia;
	private String tipoDescripcionComprobante;
	private String descripcionComprobante;
	private String indicadorFechaTransaccion;
	private String cuentaDivergente;
	private String claveContabilizar;
	private String claveTraslado;
	private String archivoCargue;

	private String codigoContable;
	private String descripcionCodigoContable;
	private String producto;
	private String transaccionArchivo;
	private String transaccionCredito;
	private String conceptoVisualizaCredito;
	private String claseDocumento;
	private String codHomologacionOpFija;

	private String codigoError;
	private String descripcionError;
	
	public CargueMasivo(String codigoOperacion, String aplicacion, String bin, String origen, String transaccion,
			String concepto, String secuencia, String cuentaContable, String tipoImputacion, String detalleAbreviado,
			String centroBeneficio, String codigoBanco, String claseDocumentoContable, String tipoDeReferencia,
			String tipoDescripcionComprobante, String descripcionComprobante, String indicadorFechaTransaccion,
			String cuentaDivergente, String claveContabilizar, String claveTraslado, String codigoError,
			String descripcionError) {
		super();
		this.codigoOperacion = codigoOperacion;
		this.aplicacion = aplicacion;
		this.bin = bin;
		this.origen = origen;
		this.transaccion = transaccion;
		this.concepto = concepto;
		this.secuencia = secuencia;
		this.cuentaContable = cuentaContable;
		this.tipoImputacion = tipoImputacion;
		this.detalleAbreviado = detalleAbreviado;
		this.centroBeneficio = centroBeneficio;
		this.codigoBanco = codigoBanco;
		this.claseDocumentoContable = claseDocumentoContable;
		this.tipoDeReferencia = tipoDeReferencia;
		this.tipoDescripcionComprobante = tipoDescripcionComprobante;
		this.descripcionComprobante = descripcionComprobante;
		this.indicadorFechaTransaccion = indicadorFechaTransaccion;
		this.cuentaDivergente = cuentaDivergente;
		this.claveContabilizar = claveContabilizar;
		this.claveTraslado = claveTraslado;
		this.codigoError = codigoError;
		this.descripcionError = descripcionError;
	}


	public CargueMasivo(String codigoOperacion, String origen, String codigoBanco, String codigoContable,
			String descripcionCodigoContable, String producto, String transaccionArchivo, String transaccionCredito,
			String conceptoVisualizaCredito, String claseDocumento, String codHomologacionOpFija, String codigoError,
			String descripcionError) {
		super();
		this.codigoOperacion = codigoOperacion;
		this.origen = origen;
		this.codigoBanco = codigoBanco;
		this.codigoContable = codigoContable;
		this.descripcionCodigoContable = descripcionCodigoContable;
		this.producto = producto;
		this.transaccionArchivo = transaccionArchivo;
		this.transaccionCredito = transaccionCredito;
		this.conceptoVisualizaCredito = conceptoVisualizaCredito;
		this.claseDocumento = claseDocumento;
		this.codHomologacionOpFija = codHomologacionOpFija;
		this.codigoError = codigoError;
		this.descripcionError = descripcionError;
	}
	
	public String getCodigoOperacion() {
		return codigoOperacion;
	}

	public String getAplicacion() {
		return aplicacion;
	}

	public String getBin() {
		return bin;
	}

	public String getOrigen() {
		return origen;
	}

	public String getTransaccion() {
		return transaccion;
	}

	public String getConcepto() {
		return concepto;
	}

	public String getSecuencia() {
		return secuencia;
	}

	public String getCuentaContable() {
		return cuentaContable;
	}

	public String getTipoImputacion() {
		return tipoImputacion;
	}

	public String getDetalleAbreviado() {
		return detalleAbreviado;
	}

	public String getCentroBeneficio() {
		return centroBeneficio;
	}

	public String getCodigoBanco() {
		return codigoBanco;
	}

	public String getClaseDocumentoContable() {
		return claseDocumentoContable;
	}

	public String getTipoDeReferencia() {
		return tipoDeReferencia;
	}

	public String getTipoDescripcionComprobante() {
		return tipoDescripcionComprobante;
	}

	public String getDescripcionComprobante() {
		return descripcionComprobante;
	}

	public String getIndicadorFechaTransaccion() {
		return indicadorFechaTransaccion;
	}

	public String getCuentaDivergente() {
		return cuentaDivergente;
	}

	public String getClaveContabilizar() {
		return claveContabilizar;
	}

	public String getClaveTraslado() {
		return claveTraslado;
	}

	public String getArchivoCargue() {
		return archivoCargue;
	}

	public String getCodigoContable() {
		return codigoContable;
	}

	public String getDescripcionCodigoContable() {
		return descripcionCodigoContable;
	}

	public String getProducto() {
		return producto;
	}

	public String getTransaccionArchivo() {
		return transaccionArchivo;
	}

	public String getTransaccionCredito() {
		return transaccionCredito;
	}

	public String getConceptoVisualizaCredito() {
		return conceptoVisualizaCredito;
	}

	public String getClaseDocumento() {
		return claseDocumento;
	}

	public String getCodHomologacionOpFija() {
		return codHomologacionOpFija;
	}

	public String getCodigoError() {
		return codigoError;
	}

	public String getDescripcionError() {
		return descripcionError;
	}

	public void setCodigoOperacion(String codigoOperacion) {
		this.codigoOperacion = codigoOperacion;
	}

	public void setAplicacion(String aplicacion) {
		this.aplicacion = aplicacion;
	}

	public void setBin(String bin) {
		this.bin = bin;
	}

	public void setOrigen(String origen) {
		this.origen = origen;
	}

	public void setTransaccion(String transaccion) {
		this.transaccion = transaccion;
	}

	public void setConcepto(String concepto) {
		this.concepto = concepto;
	}

	public void setSecuencia(String secuencia) {
		this.secuencia = secuencia;
	}

	public void setCuentaContable(String cuentaContable) {
		this.cuentaContable = cuentaContable;
	}

	public void setTipoImputacion(String tipoImputacion) {
		this.tipoImputacion = tipoImputacion;
	}

	public void setDetalleAbreviado(String detalleAbreviado) {
		this.detalleAbreviado = detalleAbreviado;
	}

	public void setCentroBeneficio(String centroBeneficio) {
		this.centroBeneficio = centroBeneficio;
	}

	public void setCodigoBanco(String codigoBanco) {
		this.codigoBanco = codigoBanco;
	}

	public void setClaseDocumentoContable(String claseDocumentoContable) {
		this.claseDocumentoContable = claseDocumentoContable;
	}

	public void setTipoDeReferencia(String tipoDeReferencia) {
		this.tipoDeReferencia = tipoDeReferencia;
	}

	public void setTipoDescripcionComprobante(String tipoDescripcionComprobante) {
		this.tipoDescripcionComprobante = tipoDescripcionComprobante;
	}

	public void setDescripcionComprobante(String descripcionComprobante) {
		this.descripcionComprobante = descripcionComprobante;
	}

	public void setIndicadorFechaTransaccion(String indicadorFechaTransaccion) {
		this.indicadorFechaTransaccion = indicadorFechaTransaccion;
	}

	public void setCuentaDivergente(String cuentaDivergente) {
		this.cuentaDivergente = cuentaDivergente;
	}

	public void setClaveContabilizar(String claveContabilizar) {
		this.claveContabilizar = claveContabilizar;
	}

	public void setClaveTraslado(String claveTraslado) {
		this.claveTraslado = claveTraslado;
	}

	public void setArchivoCargue(String archivoCargue) {
		this.archivoCargue = archivoCargue;
	}

	public void setCodigoContable(String codigoContable) {
		this.codigoContable = codigoContable;
	}

	public void setDescripcionCodigoContable(String descripcionCodigoContable) {
		this.descripcionCodigoContable = descripcionCodigoContable;
	}

	public void setProducto(String producto) {
		this.producto = producto;
	}

	public void setTransaccionArchivo(String transaccionArchivo) {
		this.transaccionArchivo = transaccionArchivo;
	}

	public void setTransaccionCredito(String transaccionCredito) {
		this.transaccionCredito = transaccionCredito;
	}

	public void setConceptoVisualizaCredito(String conceptoVisualizaCredito) {
		this.conceptoVisualizaCredito = conceptoVisualizaCredito;
	}

	public void setClaseDocumento(String claseDocumento) {
		this.claseDocumento = claseDocumento;
	}

	public void setCodHomologacionOpFija(String codHomologacionOpFija) {
		this.codHomologacionOpFija = codHomologacionOpFija;
	}

	public void setCodigoError(String codigoError) {
		this.codigoError = codigoError;
	}

	public void setDescripcionError(String descripcionError) {
		this.descripcionError = descripcionError;
	}

}
