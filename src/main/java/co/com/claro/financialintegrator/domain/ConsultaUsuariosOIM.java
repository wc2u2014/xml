package co.com.claro.financialintegrator.domain;

import java.util.Calendar;

public class ConsultaUsuariosOIM {
	private String usuario;
	private String nombre;
	private String marcaEspecial;
	private String perfil;
	private String email;
	private String estado;
	private Long sesiones;
	private Long codEntidad;
	private String numIdentificacion;
	private String area;
	private String telefono;
	private Long codOficina;
	private String entidad;
	private String creador;
	private java.sql.Date fechaCreacion;
	private java.sql.Date fechaUltimoAcceso;	
	private java.sql.Date fechaExpiracionPassword;		
	private String Observaciones;			
	
	public Object[] getArray() {
		Object[] object = new Object[19];
		object[0]=usuario;
		object[1]=nombre;
		object[2]=marcaEspecial;
		object[3]=perfil;
		object[4]=email;
		object[5]=estado;
		object[6]=sesiones;
		object[7]=codEntidad;
		object[8]=numIdentificacion;
		object[9]=area;
		object[10]=telefono;
		object[11]=codOficina;
		object[12]=new java.sql.Timestamp(Calendar.getInstance()
				.getTimeInMillis());
		object[13]=entidad;
		object[14]=creador;		
		object[15]=fechaCreacion;
		object[16]=fechaUltimoAcceso;
		object[17]=fechaExpiracionPassword;
		if(Observaciones.length()>200)
		object[18]=Observaciones.substring(0, 199);
		else
		object[18]=Observaciones;	
		
		return object;
	}
	
	@Override
	public String toString() {
		return "usuario:"+usuario+ "--nombre:"+nombre+"--marcaEspecial:"+marcaEspecial+ "--perfil:"+perfil+ "--email:" +email+"--estado:"+estado+"--sesiones:"+sesiones+ "--codEntidad:"+codEntidad+ "--numIdentificacion:" +numIdentificacion+"--area:"+area+ "--telefono:" +telefono+"--codOficina:"+codOficina+"--entidad:"+entidad+"--creador:"+creador+"--";
				
	}
	
	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getMarcaEspecial() {
		return marcaEspecial;
	}
	public void setMarcaEspecial(String marcaEspecial) {
		this.marcaEspecial = marcaEspecial;
	}
	public String getPerfil() {
		return perfil;
	}
	public void setPerfil(String perfil) {
		this.perfil = perfil;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public Long getSesiones() {
		return sesiones;
	}
	public void setSesiones(Long sesiones) {
		this.sesiones = sesiones;
	}
	public Long getCodEntidad() {
		return codEntidad;
	}
	public void setCodEntidad(Long codEntidad) {
		this.codEntidad = codEntidad;
	}
	public String getNumIdentificacion() {
		return numIdentificacion;
	}
	public void setNumIdentificacion(String numIdentificacion) {
		this.numIdentificacion = numIdentificacion;
	}
	public String getArea() {
		return area;
	}
	public void setArea(String area) {
		this.area = area;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public Long getCodOficina() {
		return codOficina;
	}
	public void setCodOficina(Long codOficina) {
		this.codOficina = codOficina;
	}
	public String getEntidad() {
		return entidad;
	}

	public void setEntidad(String entidad) {
		this.entidad = entidad;
	}

	public String getCreador() {
		return creador;
	}

	public void setCreador(String creador) {
		this.creador = creador;
	}

	public java.sql.Date getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(java.sql.Date fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public java.sql.Date getFechaUltimoAcceso() {
		return fechaUltimoAcceso;
	}

	public void setFechaUltimoAcceso(java.sql.Date fechaUltimoAcceso) {
		this.fechaUltimoAcceso = fechaUltimoAcceso;
	}

	public java.sql.Date getFechaExpiracionPassword() {
		return fechaExpiracionPassword;
	}

	public void setFechaExpiracionPassword(java.sql.Date fechaExpiracionPassword) {
		this.fechaExpiracionPassword = fechaExpiracionPassword;
	}

	public String getObservaciones() {
		return Observaciones;
	}

	public void setObservaciones(String observaciones) {
		Observaciones = observaciones;
	}
}
