package co.com.claro.financialintegrator.implement;

import java.io.InputStream;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import co.com.claro.FileUtilAPI.FileUtil;
import co.com.claro.financialintegrator.interfaces.GenericProccess;

public class MoveFile extends GenericProccess {
	private Logger logger = Logger.getLogger(MoveFile.class);
	/*public void copyCRM(String SFTPHOST,int SFTPPORT,String SFTPUSER,String SFTPPASS,String SFTProcessFile){
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		try {
			// TODO Auto-generated method stub
						JSch jsch = new JSch();
						session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
						session.setPassword(SFTPPASS);
						java.util.Properties config = new java.util.Properties();
						config.put("StrictHostKeyChecking", "no");
						session.setConfig(config);
						session.connect();
						channel = session.openChannel("sftp");
						channel.connect();
						channelSftp = (ChannelSftp) channel;
						channelSftp.cd(SFTPWORKINGDIRSRC);
						Vector filelist = channelSftp.ls(SFTPWORKINGDIRSRC);
		}catch (Exception ex) {
			logger.error("Error GENERAR ARCHIVOS : "+ex.getMessage());
		}
	}*/
	@Override
	public void process() {

		String SFTPHOST = this.getPros().getProperty("SFTPHOST");
		int SFTPPORT = Integer.parseInt(this.getPros().getProperty("SFTPPORT"));
		String SFTPUSER = this.getPros().getProperty("SFTPUSER");
		String SFTPPASS = this.getPros().getProperty("SFTPPASS");
		String SFTPWORKINGDIRSRC =this.getPros().getProperty("SFTPWORKINGDIRSRC");
		String SFTPWORKINGDIRDEST = this.getPros().getProperty("SFTPWORKINGDIRDEST");
		String SFTPWORKINGDIRlocalTmp = this.getPros().getProperty("SFTPWORKINGDIRLOCALTMP");
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		try {
			// TODO Auto-generated method stub
			JSch jsch = new JSch();
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			session.setPassword(SFTPPASS);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIRSRC);
			Vector filelist = channelSftp.ls(SFTPWORKINGDIRSRC);
			logger.info("Searc files "+SFTPWORKINGDIRSRC);
			logger.info("File found "+filelist.size());
			for (int i = 0; i < filelist.size(); i++) {
				try {
					LsEntry entry = (LsEntry) filelist.get(i);
					logger.info("File Found "+entry.getFilename());
					if (entry.getFilename().endsWith(".txt") || entry.getFilename().endsWith(".TXT")){
						String src = SFTPWORKINGDIRSRC+ entry.getFilename();
						String dest = SFTPWORKINGDIRDEST + entry.getFilename();
						String localTmp=SFTPWORKINGDIRlocalTmp+entry.getFilename();
						logger.info("src " + src);
						logger.info("dest " + dest);
						channelSftp.get(src,localTmp);
						logger.info("localTmp " + localTmp);
						channelSftp.put(localTmp, dest);						
						channelSftp.rm(src);			
						FileUtil.delete(localTmp);
					}
				} catch (Exception ex) {
					logger.error("Error pasando archivos : "+ex.getMessage());
				}
				
			}
			channelSftp.disconnect();
			channel.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			logger.error("Error GENERAR ARCHIVOS : "+ex.getMessage());
		}

	}

}
