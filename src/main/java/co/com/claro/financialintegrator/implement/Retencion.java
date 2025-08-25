package co.com.claro.financialintegrator.implement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.FinancialIntegratorException.FinancialIntegratorException;
import co.com.claro.financialintegrator.interfaces.GenericProccess;

public class Retencion extends GenericProccess {
	private Logger logger = Logger.getLogger(Retencion.class);

	/**
	 * 
	 * @return
	 */
	public Properties readProperties() {
		Properties propiedades = new Properties();
		try {
			propiedades.load(new FileInputStream(this.getPros().getProperty(
					"pathProperties")));
			return propiedades;
		} catch (FileNotFoundException e) {
			logger.error("Error Leyendo Propiedades ", e);
		} catch (IOException e) {
			logger.error("Error Leyendo Propiedades ", e);
		}
		return null;
	}

	@Override
	public void process() {

		localRetencion();
		remoteRetencion();
	}

	/**
	 * realiza proceso de retencion de archivos
	 */
	public void localRetencion() {
		logger.info("Retencion Archivo "
				+ this.getPros().getProperty("pathProperties"));
		logger.info("List Batch Names "
				+ this.getPros().getProperty("ListBatchName"));
		Properties prop = readProperties();
		// Se procesa los batchs
		String ListBatchsName[] = this.getPros().getProperty("ListBatchName")
				.split(",");
		for (String batchName : ListBatchsName) {
			logger.info("Procesando Retencion Archivo " + batchName);
			String path = prop.getProperty("file.path_" + batchName);
			String time = prop.getProperty("file.timeretention_" + batchName);
			String type = prop.getProperty("file.type_" + batchName);
			if (path != null && !path.equals("") && time != null
					&& !time.equals("") && type != null && !type.equals("")) {
				logger.info("path :" + path + " time :" + time + " type :"
						+ type);
				retencion(path, time, type);
			} else {
				logger.error("Error procesando Retencion para ARCHIVOS "
						+ batchName + ", Faltan propiedades");
			}
		}
	}
	/**
	 * realiza proceso de retencion de archivos
	 */
	public void remoteRetencion() {
		logger.info("Retencion Archivo "
				+ this.getPros().getProperty("pathProperties"));
		logger.info("List Batch Names "
				+ this.getPros().getProperty("ListBatchNameRemote"));
		Properties prop = readProperties();
		// Se procesa los batchs
		String ListBatchsName[] = this.getPros().getProperty("ListBatchNameRemote")
				.split(",");
		for (String batchName : ListBatchsName) {
			logger.info("Procesando Retencion Archivo " + batchName);
			String path = prop.getProperty("fileRemote.path_" + batchName);
			Long time = Long.parseLong( prop.getProperty("fileRemote.timeretention_" + batchName));
			String type = prop.getProperty("fileRemote.type_" + batchName);
			if (path != null && !path.equals("")  && type != null && !type.equals("")) {
				logger.info("path :" + path + " time :" + time + " type :"
						+ type);
				retencionFTP(path, time, type);
			} else {
				logger.error("Error procesando Retencion para ARCHIVOS "
						+ batchName + ", Faltan propiedades");
			}
		}
	}

	/**
	 * realiza el proceso de retencion de un archivo
	 * 
	 * @param path
	 * @param time
	 * @param type
	 */
	public void retencion(String path, String time, String pattern) {
		List<File> fileProcessList = null;
		try {
			fileProcessList = FileUtil.findListFileName(path, pattern);
			logger.info("Lista File a Delete " + fileProcessList.size());

			// Se ciclan los arhivos a eliminar
			deleteFiles(fileProcessList, Long.parseLong(time));
		} catch (Exception e) {
			logger.error("Error Buscando Archivos " + e.getMessage(), e);
		}
	}

	/**
	 * proceso que eliminar archivos dependiendo la duración
	 * 
	 * @param fileProcessList
	 * @param time
	 */
	public void deleteFiles(List<File> fileProcessList, long time) {
		for (File dFile : fileProcessList) {
			BasicFileAttributes attr;
			try {
				attr = Files.readAttributes(Paths.get(dFile.getAbsolutePath()),
						BasicFileAttributes.class);

				// Long time = System.currentTimeMillis();
				Long diff = (System.currentTimeMillis() - attr.creationTime()
						.toMillis());
				long days = TimeUnit.MILLISECONDS.toDays(diff);
				logger.info(dFile.getName() + " Time "
						+ attr.creationTime().toMillis() + " Días de creacion "
						+ days);
				if (days > time) {
					dFile.delete();
				}

			} catch (IOException e) {
				logger.error("Delete Files Error " + e.getMessage(), e);
			}

		}
	}

	/**
	 * Se eliminan los archivos remotamente
	 * 
	 * @param path
	 * @param time
	 * @param pattern
	 */
	public void retencionFTP(String path, long time, String pattern) {
		Session session = null;
		Channel channel = null;
		String SFTPWORKINGDIR = path;
		JSch ssh = new JSch();
		try {
			session = ssh.getSession(this.getPros().getProperty("username"), this.getPros().getProperty("host"), Integer.parseInt(this.getPros().getProperty("port")));
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(this.getPros().getProperty("password"));
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp sftpChannel = (ChannelSftp) channel;
			System.out.println(channel);
			sftpChannel.cd(SFTPWORKINGDIR);
			Vector filelist = sftpChannel.ls(SFTPWORKINGDIR);
			for (int i = 0; i < filelist.size(); i++) {
				LsEntry entry = (LsEntry) filelist.get(i);
				//Se verifica si archivo cumple expresion regular
				if (entry.getFilename().matches(pattern)) {
					SftpATTRS attrs = entry.getAttrs();
					Long diff = (TimeUnit.MILLISECONDS.toSeconds(System
							.currentTimeMillis()) - attrs.getATime());
					long days = TimeUnit.SECONDS.toDays(diff);
					logger.info("File Remote " + entry.getFilename() + " Days "
							+ days + " Time " + time);
					if (days >= time) {
						try {
							sftpChannel
									.rm(SFTPWORKINGDIR + entry.getFilename());
						} catch (Exception ex) {
							logger.error("Error Eliminando Archivo "
									+ ex.getMessage());
						}

					}
				}
			}
			session.disconnect();
			sftpChannel.disconnect();
			// return channel;
		} catch (JSchException e) {
			logger.error("error Conecntando al FTP ", e);
		} catch (Exception ex) {
			logger.error("error Conecntando al FTP ", ex);
		}

	}

}
