package co.com.claro.financialintegrator.thread;

import org.apache.log4j.Logger;

public class Thread implements Runnable{
	private Logger logger = Logger.getLogger(Thread.class);
	@Override
	public void run() {
		logger.info("****************** EXECUTE THREAD POOL ***********************");
		
	}

}
