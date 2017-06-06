/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.common.core.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author LLT
 *
 */
public class OrgExecutionTimer extends ExecutionTimer {
	
	public OrgExecutionTimer(long timeout) {
		super(timeout);
	}
	
	public OrgExecutionTimer(int timeout, TimeUnit unit) {
		super(timeout, unit);
	}
	
	public boolean run(final Runnable target) {
		final ObjectWrapper<Boolean> success = new ObjectWrapper<Boolean>(true);
		final Thread thread = new Thread(target);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				if (thread.isAlive()) {
					success.setObj(false);
					/* we have to stop, cannot call interrupt,
					 * otherwise, infinitive loop will still be running 
					 */
					/* TODO: find another solution for this problem. 
					 * using stop() is unsafe method. 
					 * see more: http://docs.oracle.com/javase/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
					 * currently, this will cause jvm exception when a debugger is attached, and the thread stops 
					 * right at the time the debugger is executing, getting some data from virtualMachine (due to inconsistent state).
					 * how to reproduce: 
					 * run javaParser46, at instrumentation step (run program in debug mode and change data, which might create an infinitive loop)
					 * when it's timeout, jvm crash and we get empty jResult. This happens sometimes, depend on selected samples.    
					 *  */
					thread.stop();
//					thread.interrupt();
				}
			}
		}, timeout);
		thread.start();
		
		synchronized (thread) {
			try {
				/* make the current thread wait until target thread stops */
				thread.wait();
			} catch (Exception e) {
				// do nothing
			}
		}
		timer.cancel();
		return success.getObj();
	}
	
}
