package co.com.claro.financialintegrator.conifguration;

public class FileProccessConfiguration {
	private String keyFile;
	private String secretKeyFile;
	private String passphrase;
	private String localDirectory;
	private String fileNameProcess;
	
	public String getKeyFile() {
		return keyFile;
	}
	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}
	public String getSecretKeyFile() {
		return secretKeyFile;
	}
	public void setSecretKeyFile(String secretKeyFile) {
		this.secretKeyFile = secretKeyFile;
	}
	public String getPassphrase() {
		return passphrase;
	}
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	public String getLocalDirectory() {
		return localDirectory;
	}
	public void setLocalDirectory(String localDirectory) {
		this.localDirectory = localDirectory;
	}
	
	
}
