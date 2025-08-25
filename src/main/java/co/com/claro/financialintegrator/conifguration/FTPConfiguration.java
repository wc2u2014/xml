package co.com.claro.financialintegrator.conifguration;

public class FTPConfiguration {
	
	private String host;
	private String port;
	private String username;
	private String password;
	private String remoteFile;
	private String fixedRate;
	
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRemoteFile() {
		return remoteFile;
	}
	public void setRemoteFile(String remoteFile) {
		this.remoteFile = remoteFile;
	}
	public String getFixedRate() {
		return fixedRate;
	}
	public void setFixedRate(String fixedRate) {
		this.fixedRate = fixedRate;
	}
	
	
	
}
