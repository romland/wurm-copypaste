package com.friya.wurmonline.server.copypaste;

abstract class EventOnce
{
	private long invokeAt;
	private long originalMilliSecondDelay;
	
	private Object[] args;
	
	public static enum Unit {
		MILLISECONDS,
		SECONDS,
		MINUTES,
		HOURS
	};
	
	public EventOnce(long invokeAt, Object[] args)
	{
		this.invokeAt = invokeAt;
		this.args = args;
		this.originalMilliSecondDelay = invokeAt - System.currentTimeMillis();
	}
	
	public EventOnce(int fromNow, Unit unit)
	{
		this(fromNow, unit, new Object[]{});
	}

	public EventOnce(int fromNow, Unit unit, Object[] args)
	{
		this.args = args;
		
		switch(unit) {
		case HOURS:
			invokeAt = System.currentTimeMillis() + (fromNow * 60 * 60 * 1000);
			break;
		case MINUTES:
			invokeAt = System.currentTimeMillis() + (fromNow * 60 * 1000);
			break;
		case SECONDS:
			invokeAt = System.currentTimeMillis() + (fromNow * 1000);
			break;
		case MILLISECONDS:
			invokeAt = System.currentTimeMillis() + (fromNow);
			break;
		}

		this.originalMilliSecondDelay = fromNow;
	}
	
	public boolean isInvokable()
	{
		return invokeAt > 0 && System.currentTimeMillis() > invokeAt;
	}
	
	/**
	 * 
	 * @return true if this event should be deleted, false if we should retry
	 */
	abstract public boolean invoke();
	
	public long getInvokeAt() {
		return invokeAt;
	}

	public void setInvokeAt(long invokeAt) {
		this.invokeAt = invokeAt;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public long getOriginalMilliSecondDelay()
	{
		return originalMilliSecondDelay;
	}
}
