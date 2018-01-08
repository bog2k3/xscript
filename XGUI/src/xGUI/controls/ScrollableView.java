package xGUI.controls;

import java.awt.Graphics2D;

import XScripter.Functor;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import xGUI.Container;
import xGUI.Location;
import xGUI.VisualComponent;

/**
 * @author Bogdan.Ionita
 *
 */
public class ScrollableView extends Container {

	/**
	 * 
	 */
	public ScrollableView(Integer x, Integer y, Integer w, Integer h) {
		this(new Location(x,y,w,h));
	}
	
	public ScrollableView(Location loc) {
		init_loc = loc;
		m_visible = true;
		m_BL = m_BT = m_BR = m_BB = 2;
	}

	private ScrollBar sbH, sbV;
	private Label lCorner;
	private boolean m_forceScrollbars = false;
	
	private int m_VLead = 0;
	private int m_VTail = 0;
	private int m_HLead = 0;
	private int m_HTail = 0;

	@Override
	protected void init() 
	{
		setLocation(init_loc);
		addControl(sbH = new ScrollBar(0, 0, 0, 20, ScrollBar.SB_LAYOUT_HORIZONTAL));
		sbH.hide(); sbH.setEnabled(false);
		sbH.onScroll.addListener(new Functor() {			
			@Override
			public void Execute(Object sender, Object... params) {
				refresh();
			}});
		addControl(sbV = new ScrollBar(0, 0, 20, 0, ScrollBar.SB_LAYOUT_VERTICAL));
		sbV.hide(); sbV.setEnabled(false);
		sbV.onScroll.addListener(new Functor() {			
			@Override
			public void Execute(Object sender, Object... params) {
				refresh();
			}});
		
		sbV.setSmallStep(5);
		sbV.setLargeStep(60);
		sbH.setSmallStep(5);
		sbH.setLargeStep(60);
		
		addControl(lCorner = new Label("", 0, 0, 5, 5));
		lCorner.hide();
		
		refresh();
	}
	
	public void forceScrollbars(Boolean force) {
		m_forceScrollbars = force;
		refresh();
	}
	
	public void setHScrollbarPadding(Integer lead, Integer trail) {
		m_HLead = lead;
		m_HTail = trail;
		if (isInitialized())
			refresh();
	}
	
	public void setVScrollbarPadding(Integer lead, Integer trail) {
		m_VLead = lead;
		m_VTail = trail;
		if (isInitialized())
			refresh();
	}

	/**
	 * refreshes the layout of the container, adjusting scrollbars in order to cover all
	 * the displayable area.
	 */
	protected void refresh()
	{
		suspendPaint();
		
		// determine the virtual client area size:
		int x1=0, y1=0, x2=0, y2=0;
		
		for (VisualComponent c : m_Controls) 
		{
			if (c == sbH || c == sbV || c == lCorner)
				continue;
			Location l = c.getLocation();
			if (l.x < x1)
				x1 = l.x;
			if (l.y < y1)
				y1 = l.y;
			if (l.x+l.w > x2)
				x2 = l.x+l.w;
			if (l.y+l.h > y2)
				y2 = l.y+l.h;
		}
		Location cl = getClientArea();
		int cw = cl.w;
		int ch = cl.h;
		int vw = x2-x1, vh = y2-y1; // virtual w, h
		
		boolean sbve = vh > ch;
		if (sbve || m_forceScrollbars)
			cw -= sbV.getLocation().w;
		boolean sbhe = vw > cw;
		if (sbhe || m_forceScrollbars)
			ch -= sbH.getLocation().h;
		sbve = vh > ch;
		if (sbve && cw == cl.w)
			cw -= sbV.getLocation().w;
		
		if (vw < cw)
			vw = cw;
		if (vh < ch)
			vh = ch;
		
		sbV.setCoverage((double)ch / vh);
		sbH.setCoverage((double)cw / vw);
		
		if (sbve || m_forceScrollbars) {
			sbV.show();
			sbV.setEnabled(sbve);
			sbve = true;
		} else
			sbV.hide();
		
		if (sbhe || m_forceScrollbars) {
			sbH.show();
			sbH.setEnabled(sbhe);
			sbhe = true;
		} else
			sbH.hide();
		
		int cltx = sbH.getScroll(); 
		int clty = sbV.getScroll();
		
		if (cltx > vw-cw)
			cltx = vw-cw;
		if (cltx < 0)
			cltx = 0;
		
		if (clty > vh-ch)
			clty = vh-ch;
		if (clty < 0)
			clty = 0;
		
		if (sbve) {
			sbV.setLocation(cw+cltx, clty + m_VLead, sbV.getLocation().w, ch-m_VLead - m_VTail);
			sbV.setRange(vh-ch);
			sbV.setScroll(clty);
		}
		if (sbhe) {
			sbH.setLocation(cltx+m_HLead, ch+clty, cw - m_HLead - m_HTail, sbH.getLocation().h);
			sbH.setRange(vw-cw);
			sbH.setScroll(cltx);
		}
		lCorner.setLocation(cltx+cw,clty+ch,sbV.getLocation().w,sbH.getLocation().h);
		if (sbhe && sbve && m_HTail == 0 && m_VTail == 0)
			lCorner.show();
		else
			lCorner.hide();
		
		setClientTranslation(-cltx, -clty);
		
		resumePaint(true);
	}
	
	@Override
	public void addControl(VisualComponent c) 
	{
		super.addControl(c);
		if (isInitialized()) {
			bringScrollbarsToTop();
			refresh();
		}
	}
	
	protected void bringScrollbarsToTop() {
		bringControlToTop(lCorner);
		bringControlToTop(sbH);
		bringControlToTop(sbV);
	}

	@Override
	protected void handleSizeChanged() {
		if (isInitialized())
			refresh();
	}
	
	@Override
	protected void paintBorder(Graphics2D gfx) {
		getGUI().draw3DFrame(gfx, m_loc.untranslate(), clBackground, false);
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
