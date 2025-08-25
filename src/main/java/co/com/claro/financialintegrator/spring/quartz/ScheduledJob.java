package co.com.claro.financialintegrator.spring.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import co.com.claro.financialintegrator.interfaces.GenericProccess;

/**
 * Clase generica que ejecuta la clase que Ejecuta el proceso del Batch
 * 
 * @author Oracle
 *
 */
@DisallowConcurrentExecution
public class ScheduledJob extends QuartzJobBean {

	/**
	 * Clase Generica. que tiene un metodo de proceso
	 */
	private GenericProccess genericProccess;

	/**
	 * Ejecuta le metodo process que tiene cada clase que implementa una clase
	 * GenericProccess
	 */
	@Override
	protected void executeInternal(JobExecutionContext arg0)
			throws JobExecutionException {
		genericProccess.process();

	}
	
	public void setGenericProccess(GenericProccess genericProccess) {
		this.genericProccess = genericProccess;
	}
}
