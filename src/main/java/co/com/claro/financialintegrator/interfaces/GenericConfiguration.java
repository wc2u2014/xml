package co.com.claro.financialintegrator.interfaces;

import co.com.claro.financialintegrator.conifguration.FTPConfiguration;
import co.com.claro.financialintegrator.conifguration.FileProccessConfiguration;
import co.com.claro.financialintegrator.conifguration.MailConfiguration;
import co.com.claro.financialintegrator.conifguration.QuartzConfiguration;
import co.com.claro.financialintegrator.domain.UidServiceResponse;
import co.com.claro.financialintegrator.util.UidService;

import org.apache.log4j.Logger;

public abstract class GenericConfiguration {

    private static final Logger logger = Logger.getLogger(GenericConfiguration.class);

    public FileProccessConfiguration fileProcessConfiguration;
    public FTPConfiguration ftpConfiguration;
    public MailConfiguration mailConfiguration;
    public QuartzConfiguration quartzConfiguration;

    public FileProccessConfiguration getFileProcessConfiguration() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][getFileProcessConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][getFileProcessConfiguration][REQUEST| request: N/A]");
            FileProccessConfiguration response = fileProcessConfiguration;
            logger.info(uid + "[GenericConfiguration][getFileProcessConfiguration][RESPONSE| " + response + "]");
            return response;
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][getFileProcessConfiguration][ERROR| Excepción capturada]", e);
            return null;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][getFileProcessConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][getFileProcessConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public void setFileProcessConfiguration(FileProccessConfiguration fileProcessConfiguration) {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][setFileProcessConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][setFileProcessConfiguration][REQUEST| " + fileProcessConfiguration + "]");
            this.fileProcessConfiguration = fileProcessConfiguration;
            logger.info(uid + "[GenericConfiguration][setFileProcessConfiguration][RESPONSE| response: N/A]");
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][setFileProcessConfiguration][ERROR| Excepción capturada]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][setFileProcessConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][setFileProcessConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public FTPConfiguration getFtpConfiguration() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][getFtpConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][getFtpConfiguration][REQUEST| request: N/A]");
            FTPConfiguration response = ftpConfiguration;
            logger.info(uid + "[GenericConfiguration][getFtpConfiguration][RESPONSE| " + response + "]");
            return response;
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][getFtpConfiguration][ERROR| Excepción capturada]", e);
            return null;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][getFtpConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][getFtpConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public void setFtpConfiguration(FTPConfiguration ftpConfiguration) {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][setFtpConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][setFtpConfiguration][REQUEST| " + ftpConfiguration + "]");
            this.ftpConfiguration = ftpConfiguration;
            logger.info(uid + "[GenericConfiguration][setFtpConfiguration][RESPONSE| response: N/A]");
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][setFtpConfiguration][ERROR| Excepción capturada]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][setFtpConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][setFtpConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public MailConfiguration getMailConfiguration() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][getMailConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][getMailConfiguration][REQUEST| request: N/A]");
            MailConfiguration response = mailConfiguration;
            logger.info(uid + "[GenericConfiguration][getMailConfiguration][RESPONSE| " + response + "]");
            return response;
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][getMailConfiguration][ERROR| Excepción capturada]", e);
            return null;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][getMailConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][getMailConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public void setMailConfiguration(MailConfiguration mailConfiguration) {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][setMailConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][setMailConfiguration][REQUEST| " + mailConfiguration + "]");
            this.mailConfiguration = mailConfiguration;
            logger.info(uid + "[GenericConfiguration][setMailConfiguration][RESPONSE| response: N/A]");
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][setMailConfiguration][ERROR| Excepción capturada]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][setMailConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][setMailConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public QuartzConfiguration getQuartzConfiguration() {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][getQuartzConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][getQuartzConfiguration][REQUEST| request: N/A]");
            QuartzConfiguration response = quartzConfiguration;
            logger.info(uid + "[GenericConfiguration][getQuartzConfiguration][RESPONSE| " + response + "]");
            return response;
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][getQuartzConfiguration][ERROR| Excepción capturada]", e);
            return null;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][getQuartzConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][getQuartzConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public void setQuartzConfiguration(QuartzConfiguration quartzConfiguration) {
        UidServiceResponse uidResponse = UidService.generateUid();
        String uid = uidResponse.getUid();
        long startTime = System.currentTimeMillis();
        logger.info(uid + "[GenericConfiguration][setQuartzConfiguration][--------------------------------------------START TRANSACTION--------------------------------------------]");
        try {
            logger.info(uid + "[GenericConfiguration][setQuartzConfiguration][REQUEST| " + quartzConfiguration + "]");
            this.quartzConfiguration = quartzConfiguration;
            logger.info(uid + "[GenericConfiguration][setQuartzConfiguration][RESPONSE| response: N/A]");
        } catch (Exception e) {
            logger.error(uid + "[GenericConfiguration][setQuartzConfiguration][ERROR| Excepción capturada]", e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info(uid + "[GenericConfiguration][setQuartzConfiguration][TIME| " + elapsedTime + " ms]");
            logger.info(uid + "[GenericConfiguration][setQuartzConfiguration][---------------------------------------------END TRANSACTION---------------------------------------------]");
        }
    }

    public abstract void loadConfiguration();
}
