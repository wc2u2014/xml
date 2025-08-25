package co.com.claro.financialintegrator.conifguration;
/**
 * Clase de configuración de conexión a mails 
 * @author Oracle
 *
 */
public class MailConfiguration {
	
	private String host;
	private String port;
	private String userName;
	private String password;
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
}
