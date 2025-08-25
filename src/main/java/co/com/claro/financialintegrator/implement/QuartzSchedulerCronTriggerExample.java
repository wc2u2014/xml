package co.com.claro.financialintegrator.implement;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.quartz.*;

import co.com.claro.financialintegrator.interfaces.ILatch;
import co.com.claro.financialintegrator.spring.quartz.MySchedulerListener;
import co.com.claro.financialintegrator.thread.ReporteArchivoThread;

public class QuartzSchedulerCronTriggerExample implements ILatch {
	
    private CountDownLatch latch = new CountDownLatch(1);
    
    public static void main(String[] args) throws Exception {   
        QuartzSchedulerCronTriggerExample quartzSchedulerExample = new QuartzSchedulerCronTriggerExample();
        quartzSchedulerExample.fireJob();
    }
     
    public void fireJob() throws SchedulerException, InterruptedException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
        Scheduler scheduler = schedFact.getScheduler();
        scheduler.getListenerManager().addSchedulerListener(new MySchedulerListener(scheduler));
        scheduler.start();
         
        // define the job and tie it to our HelloJob class
        JobBuilder jobBuilder = JobBuilder.newJob(ReporteArchivoThread.class);
        JobDataMap data = new JobDataMap();
        data.put("latch", this);
         
        JobDetail jobDetail = jobBuilder.usingJobData("example", "com.javacodegeeks.quartz.QuartzSchedulerListenerExample") 
                .usingJobData(data)
                .withIdentity("myJob", "group1")
                .build();
         
        java.util.Calendar rightNow = java.util.Calendar.getInstance();
        int hour = rightNow.get(java.util.Calendar.HOUR_OF_DAY);
        int min = rightNow.get(java.util.Calendar.MINUTE);
         
        System.out.println("Current time: " + new Date());
         
        // Fire at curent time + 1 min every day
        Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity("myTrigger", "group1")
        .startAt(DateBuilder.todayAt(10, 20, 20))
        .withSchedule(CronScheduleBuilder.cronSchedule("0 " + (min + 1) + " " + hour + " * * ? *"))     
        .build();
         
        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(jobDetail, trigger);
        latch.await();
        System.out.println("All triggers executed. Shutdown scheduler");
        scheduler.shutdown();
    }
     
    public void countDown() {
        latch.countDown();
    }
}
