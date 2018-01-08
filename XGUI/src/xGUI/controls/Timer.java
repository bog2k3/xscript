package xGUI.controls;

import java.awt.Graphics2D;

import XScripter.Functor;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import kernel.Event;

import xGUI.VisualComponent;

public class Timer extends VisualComponent
{
	private int m_Interval;
	private int m_TimerID;
	
	public void setInterval(Integer interv) {
		m_Interval = interv;
		getKernel().setTimer(m_TimerID, m_Interval);
	}
	public Integer getInterval() {
		return m_Interval;
	}
	
	public Timer(Integer interval) {
		m_Interval = interval;
		m_Events.add(onTick = new Event(this));
	}
	
	public final Event onTick;

	@Override
	protected void init() {
		m_TimerID = getKernel().createTimer(m_Interval, new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				if (isEnabled())
					onTick.fire(); //TODO implement enable/disable timer in kernel for optimization
			}
		});
		setLocation(-1,-1,0,0);
		setEnabled(false);
	}
	
	@Override
	protected void paint(Graphics2D gfx) {
		// me no paint, me invisible.
	}

	@Override
	protected void updateFromStyle() {
		// I has no style >:)
	}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
	
	@Override
	public void destroy() {
		getKernel().removeTimer(m_TimerID);
		onTick.removeAllListeners();
	}
}
