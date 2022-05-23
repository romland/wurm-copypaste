package com.friya.wurmonline.server.copypaste;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class ShortEventDispatcher
{
	private static Logger logger = Logger.getLogger(ShortEventDispatcher.class.getName());
	private static ArrayList<EventOnce> events = new ArrayList<EventOnce>();
    private static Timer timer = null;
    private static boolean running = false;

	
	public ShortEventDispatcher()
    {
    	// start "polling" when an event is added
    	// stop "polling" when last event is gone
    	// do not allow events to exceed more than 10 second delay
	}

	static private void startPolling()
    {
    	if(timer != null) {
    		logger.info("startPolling(): Poller is already running");
    		return;
    	}
    	
    	timer = new Timer();

    	timer.schedule(
    		new TimerTask() {
    			public void run() {
    				poll();
    			}
    		}, 50L, 1L			// 50 = 1/20th second
    	);

		logger.info("stopPolling(): started");
    }
    
	static private void stopPolling()
    {
    	if(timer != null) {
    		logger.info("stopPolling(): stopped");
    		timer.cancel();
    		timer = null;
    		return;
    	}

		logger.severe("stopPolling(): Poller was not running. Why call this, eh?");
    }

	static public void add(EventOnce event)
	{
		if(event.getOriginalMilliSecondDelay() > 10000) {
			throw new RuntimeException("Short events cannot exceed 10 seconds");
		}
		
		events.add(event);

		if(timer == null) {
			startPolling();
		}
	}

	static private void poll()
	{
		if(running) {
			return;
		}

		if(events.size() == 0) {
			stopPolling();
			return;
		}
		
		running = true;
		
		ArrayList<EventOnce> executed = new ArrayList<EventOnce>();
		long ts = System.currentTimeMillis();
		
		EventOnce[] eventsCopy = events.toArray(new EventOnce[]{});

		for(EventOnce event : eventsCopy){
			if(event.getInvokeAt() < ts) {
				if(event.invoke()) {
					executed.add(event);
				}
			}
		}

		if(executed.size() > 0) {
			events.removeAll(executed);
		}

		running = false;
	}

}
