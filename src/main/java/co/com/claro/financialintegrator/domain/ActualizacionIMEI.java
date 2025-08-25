package co.com.claro.financialintegrator.domain;

public class ActualizacionIMEI {

	private Long numeroCredito;
	private String imei;
	private String referenciaEquipo;
	private String usuario;
	private String archivo;
	private String imeiAntiguo;
	private String referenciaEquipoAntiguo;
	private String numeroDocumento;
	private int tipoIdentificacion;
	private Long min;
	private String estado;

	public ActualizacionIMEI() {
	}

	public ActualizacionIMEI(Long numeroCredito, String imei, String referenciaEquipo, String usuario, String archivo,
			String imeiAntiguo, String referenciaEquipoAntiguo, String numeroDocumento, int tipoIdentificacion,
			Long min) {
		super();
		this.numeroCredito = numeroCredito;
		this.imei = imei;
		this.referenciaEquipo = referenciaEquipo;
		this.usuario = usuario;
		this.archivo = archivo;
		this.imeiAntiguo = imeiAntiguo;
		this.referenciaEquipoAntiguo = referenciaEquipoAntiguo;
		this.numeroDocumento = numeroDocumento;
		this.tipoIdentificacion = tipoIdentificacion;
		this.min = min;
	}

	public ActualizacionIMEI(Long numeroCredito, String imei, String referenciaEquipo, String usuario, String archivo) {
		super();
		this.numeroCredito = numeroCredito;
		this.imei = imei;
		this.referenciaEquipo = referenciaEquipo;
		this.usuario = usuario;
		this.archivo = archivo;
	}

	public Long getNumeroCredito() {
		return numeroCredito;
	}

	public String getImei() {
		return imei;
	}

	public String getReferenciaEquipo() {
		return referenciaEquipo;
	}

	public String getUsuario() {
		return usuario;
	}

	public String getArchivo() {
		return archivo;
	}

	public String getImeiAntiguo() {
		return imeiAntiguo;
	}

	public String getReferenciaEquipoAntiguo() {
		return referenciaEquipoAntiguo;
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public int getTipoIdentificacion() {
		return tipoIdentificacion;
	}

	public Long getMin() {
		return min;
	}

	public String getEstado() {
		return estado;
	}

	public void setNumeroCredito(Long numeroCredito) {
		this.numeroCredito = numeroCredito;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public void setReferenciaEquipo(String referenciaEquipo) {
		this.referenciaEquipo = referenciaEquipo;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public void setArchivo(String archivo) {
		this.archivo = archivo;
	}

	public void setImeiAntiguo(String imeiAntiguo) {
		this.imeiAntiguo = imeiAntiguo;
	}

	public void setReferenciaEquipoAntiguo(String referenciaEquipoAntiguo) {
		this.referenciaEquipoAntiguo = referenciaEquipoAntiguo;
	}

	public void setNumeroDocumento(String numeroDocumento) {
		this.numeroDocumento = numeroDocumento;
	}

	public void setTipoIdentificacion(int tipoIdentificacion) {
		this.tipoIdentificacion = tipoIdentificacion;
	}

	public void setMin(Long min) {
		this.min = min;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

}
