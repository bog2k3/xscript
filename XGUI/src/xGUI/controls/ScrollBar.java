package xGUI.controls;

import kernel.Event;
import XScripter.Functor;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;
import xGUI.Container;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.XColors;

public class ScrollBar extends Container 
{
	class SBArrow extends Button
	{
		SBArrow(Integer x, Integer y, Integer w, Integer h) {
			super(x,y,w,h,"");
		}
		
		private Timer timerScroll;
		private int step_value;
		
		@Override
		protected void init() {
			super.init();
			timerScroll = new Timer(50);
			addControl(timerScroll);
			timerScroll.onTick.addListener(new Functor() {				
				@Override
				public void Execute(Object sender, Object... params) {
					step(step_value);
				}});
			
			setLayout(ButtonLayout.imageUnder);
			//setImage("hehe_from_theme_here");
		}
		
		@Override
		protected void handleMousePressed(MouseArgs arg0) {
			super.handleMousePressed(arg0);
			timerScroll.setEnabled(true);
		}
		
		@Override
		protected void handleMouseReleased(MouseArgs arg0) {
			super.handleMouseReleased(arg0);
			timerScroll.setEnabled(false);
		}
		
		@Override
		protected void handleMouseEntered(MouseArgs arg0) {
			super.handleMouseEntered(arg0);
			if (getButtonState() == 2)
				timerScroll.setEnabled(true);
		}
		
		@Override
		protected void handleMouseExit(MouseArgs arg0) {
			super.handleMouseExit(arg0);
			timerScroll.setEnabled(false);
		}
	}
	
	class SBTrack extends Button
	{
		private int m_mouseDX;
		private int m_mouseDY;
		private boolean m_bMoving;
		private int m_MoveDir;
		private int m_min, m_max;

		SBTrack(Integer x, Integer y, Integer w, Integer h) {
			super(x,y,w,h,"");
		}
		
		@Override
		protected void init() {
			super.init();
			//TODO override style from button
			setLayout(ButtonLayout.imageUnder);
			//setImage("hehe_from_theme_here");
		}
		
		@Override
		protected void handleMousePressed(MouseArgs arg0) {
			super.handleMousePressed(arg0);
			if (arg0.button == 1) {
				Location ml = screenToLocal(arg0.x, arg0.y);
				m_mouseDX = ml.x;
				m_mouseDY = ml.y;
				m_bMoving = true;
			}
		}
		
		@Override
		protected void handleMouseReleased(MouseArgs arg0) {
			super.handleMouseReleased(arg0);
			if (arg0.button == 1)
				m_bMoving = false;
		}
		
		@Override
		protected void handleMouseDragged(MouseArgs arg0) {
			if (m_bMoving) {
				Location pos = screenToParent(arg0.x-m_mouseDX, arg0.y-m_mouseDY);
				int x=-100;
				int y=-100;
				double tp = -100;
				if (m_MoveDir==SB_LAYOUT_HORIZONTAL) {
					x = Math.max(Math.min(pos.x, m_max), m_min);
					y = m_loc.y;
					tp = ((double)x - m_min) / (m_max-m_min); 
				}
				else
				if (m_MoveDir==SB_LAYOUT_VERTICAL) {
					x = m_loc.x;
					y = Math.max(Math.min(pos.y, m_max), m_min);
					tp = ((double)y - m_min) / (m_max-m_min);
				}
				setPosition(x, y);
				trackMoved(tp);
			}
		}

		protected void setScrollDirection(int dir, int min, int max) {
			m_MoveDir = dir;
			m_min = min;
			m_max = max;
		}
	}
	
	public static final Integer SB_LAYOUT_HORIZONTAL	= 0x1001;
	public static final Integer SB_LAYOUT_VERTICAL		= 0x1002;
	
	private int m_layout;
	public Integer getLayout() { return m_layout; }
	public void setLayout(Integer layout) {
		checkAttachedToParent();
		suspendPaint();
		if (m_layout == SB_LAYOUT_HORIZONTAL) {
			Location lc = getClientArea();
			int arrW = Math.min(20, lc.w/2);
			arrDec.setLocation(0,0,arrW, lc.h);
			arrDec.setText("\u25C4");
			arrInc.setLocation(lc.w-arrW, 0, arrW, lc.h);
			arrInc.setText("\u25BA");
			updateTrack();
		}
		else
		if (m_layout == SB_LAYOUT_VERTICAL) {
			Location lc = getClientArea();
			int arrH = Math.min(20, lc.h/2);
			arrDec.setLocation(0,0,lc.w,arrH);
			arrDec.setText("\u25B2");
			arrInc.setLocation(0, lc.h-arrH, lc.w, arrH);
			arrInc.setText("\u25BC");
			updateTrack();
		}
		resumePaint(true);
	}
	
	private void updateTrack()
	{
		int tx=-100, ty=-100, tw=0, th=0;
		Location lc = getClientArea();
		int min=-100, max=-100;
		
		if (m_layout == SB_LAYOUT_HORIZONTAL) {
			int arrW = arrDec.getLocation().w;
			tw = (int)((lc.w - 2*arrW) * coverage);
			tx = arrW + (lc.w - 2*arrW - tw) * m_pos / m_range;
			ty = 0;
			th = lc.h;
			min = arrW;
			max = lc.w - arrW-tw;
		}
		else
		if (m_layout == SB_LAYOUT_VERTICAL) {
			int arrH = arrDec.getLocation().h;
			th = (int)((lc.h - 2*arrH) * coverage);
			ty = arrH + (lc.h - 2*arrH - th) * m_pos / m_range;
			tx = 0;
			tw = lc.w;
			min = arrH;
			max = lc.h - arrH-th;
		}
		
		track.setLocation(tx, ty, tw, th);
		track.setScrollDirection(m_layout, min, max);
	}
	
	protected SBArrow arrInc, arrDec;
	protected SBTrack track;
	private int m_range = 100;
	private int m_pos = 0;
	private int m_smallInc = 1;
	private int m_largeInc = 20;
	private double coverage = 0.2f;
	
	public Double getCoverage() { return coverage; }
	public void setCoverage(Double d) {
		coverage = Math.min(Math.max(d, 0.1), 1);
		updateTrack();
	}
	
	public Integer getSmallStep() { return m_smallInc; }
	public void setSmallStep(Integer st) { 
		m_smallInc = st; 
		arrInc.step_value = +m_smallInc;
		arrDec.step_value = -m_smallInc; 
	}
	public Integer getLargeStep() { return m_largeInc; }
	public void setLargeStep(Integer st) { m_largeInc = st; }
	
	public Integer getRange() { return m_range; }
	public void setRange(Integer range) {
		m_range = Math.max(range, 1);
		if (m_pos > m_range) {
			m_pos = m_range;
		}
		updateTrack();
	}
	public Integer getScroll() { return m_pos; }
	public void setScroll(Integer scroll) {
		m_pos = Math.min(m_range, Math.max(0, scroll));
		updateTrack();
	}
	
	/**
	 * triggers when the position of the scrollbar is changed, either by dragging, clicking, 
	 * or by a call to setPosition.
	 * @param ScrollBar sender - the ScrollBar who triggered the event.
	 */
	public final Event onScroll;
	
	private void step(int step_value) 
	{
		m_pos = Math.max(0, Math.min(m_range, m_pos+step_value));
		
		updateTrack();
		onScroll.fire();
	}
	
	private void trackMoved(double newPos)
	{
		m_pos = (int)(newPos * m_range);
		onScroll.fire();
	}
	
	public ScrollBar(Integer x, Integer y, Integer w, Integer h, Integer layout)
	{
		init_loc = new Location(x,y,w,h);
		m_layout = layout;
		m_visible = true;
		m_BB = m_BL = m_BR = m_BT = 1;
		
		m_Events.add(onScroll = new Event(this));
	}
	
	@Override
	protected void init() {
		setLocation(init_loc);
		addControl(track = new SBTrack(0, 0, 5, 5));
		addControl(arrInc = new SBArrow(0, 0, 5, 5));
		arrInc.step_value = +m_smallInc;
		addControl(arrDec = new SBArrow(0, 0, 5, 5));
		arrDec.step_value = -m_smallInc;
		
		setLayout(m_layout);
		setBackgroundColor(XColors.scrollBarBackground);
		setBorderColor(XColors.frameDark1);
	}
	
	@Override
	protected void handleMousePressed(MouseArgs arg0) 
	{
		super.handleMousePressed(arg0);
		
		if (arg0.button == 1) {
			Location ml = screenToLocal(arg0.x, arg0.y);
			if (m_layout==SB_LAYOUT_HORIZONTAL) {
				if (ml.x < track.getLocation().x)
					step(-m_largeInc);
				else
					step(+m_largeInc);
			}
			else
			if (m_layout==SB_LAYOUT_VERTICAL) {
				if (ml.y < track.getLocation().y)
					step(-m_largeInc);
				else
					step(+m_largeInc);
			}
		}
	}
	
	@Override
	protected void handleSizeChanged() {
		if (track != null)
			setLayout(m_layout);
	}
	
	@Override
	protected void updateFromStyle() {
		// TODO Auto-generated method stub
		
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
