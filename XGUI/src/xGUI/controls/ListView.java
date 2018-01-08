package xGUI.controls;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;

import kernel.Event;
import kernel.MediaManager.ResourceState;
import kernel.MediaManager.WebImage;

import XScripter.Functor;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import wrappers.Color;
import wrappers.Cursor;
import wrappers.XMLNode;
import xGUI.IStyleLoader;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.TextAlign;
import xGUI.XColors;
import xGUI.XGUI;
import xGUI.XMLFontInfo;

public class ListView extends ScrollableView 
{
	class Record
	{
		Integer customID;
		String[] data;
		Label[] m_Labels;
		
		Record(Integer customID, String[] data, Label[] rLabels) {
			this.customID = customID;
			this.data = data;
			this.m_Labels = rLabels;
		}
	}
	
	public static class HeaderButton extends Button
	{
		public static class HeaderBtnStyle extends IStyleLoader
		{
			/** default metrics */
			private final Color DEFAULT_TEXT_COLOR = XColors.text;
			
			/** metrics (set by theme) */
			protected TextAlign TEXT_ALIGN_X = TextAlign.Left;
			protected Color TEXT_COLOR = DEFAULT_TEXT_COLOR;
			protected Font FONT = null;
			protected SkinState skinStateNormal = null;
			protected SkinState skinStateHover = null;
			protected SkinState skinStatePressed = null;
			protected SkinState skinStateDisabled = null;
			
			private XGUI xgui;
			
			public HeaderBtnStyle(XGUI xgui) 
			{
				this.xgui = xgui;
			}
			
			@Override
			public void loadStyle(XMLNode root, String themeRootURL) 
			{
				XMLNode eFont = root.getSubnode( "font"); 
				XMLFontInfo xf = new XMLFontInfo(eFont, xgui.getDefaultFont(), xgui.getDefaultTextColor());
				FONT = xf.font;
				TEXT_COLOR = xf.color;
				
				XMLNode eSkin = root.getSubnode("normal");
				if (eSkin != null)
					skinStateNormal = new SkinState(eSkin, themeRootURL, xgui);
				eSkin = root.getSubnode("hover");
				if (eSkin != null)
					skinStateHover = new SkinState(eSkin, themeRootURL, xgui);
				eSkin = root.getSubnode("pressed");
				if (eSkin != null)
					skinStatePressed = new SkinState(eSkin, themeRootURL, xgui);
				eSkin = root.getSubnode("disabled");
				if (eSkin != null)
					skinStateDisabled = new SkinState(eSkin, themeRootURL, xgui);
				
				notifyUpdate();
			}
			
		}
		
		HeaderButton(int x, int y, int w, int h, String title) {
			super(x,y,w,h,title);
		}
		
		private HeaderBtnStyle style = null;
		private static final String styleID = "ColumnHeader";
		
		private int m_resAreaRightID;
		private int m_resAreaLeftID;
		private int m_allowResize = 0;
		private int m_resizing = 0;
		private int m_size_dx;
		
		void setAllowResize(boolean allowLeft, boolean allowRight)
		{
			m_allowResize = (allowLeft ? 1 : 0) + (allowRight ? 2 : 0);
			handleSizeChanged();
		}
		
		@Override
		protected void init() 
		{
			style = (HeaderBtnStyle) getGUI().getStyle(styleID, HeaderBtnStyle.class);
			style.addSubscriber(this);
			
			m_resAreaRightID = addCursorArea(init_loc, Cursor.RESIZE_E);
			m_resAreaLeftID = addCursorArea(init_loc, Cursor.RESIZE_W);
			
			suspendPaint();
			setLocation(init_loc);
			updateFromStyle();
			resumePaint(false);
		}
		
		@Override
		protected void handleMousePressed(MouseArgs arg0) {
			if (arg0.button == 1)
			{
				Location m = screenToLocal(arg0.x, arg0.y);
				m_size_dx = m.x;
				
				if (m.x >= m_loc.w-5 && (m_allowResize&2) != 0)
					m_resizing = 2;
				else
				if (m.x < 5 && (m_allowResize&1) != 0)
					m_resizing = 1;
				else
					super.handleMousePressed(arg0);
			}
			else
				super.handleMousePressed(arg0);
		}
		
		@Override
		protected void handleMouseReleased(MouseArgs arg0) {
			if (m_resizing!=0 && arg0.button == 1) {
				m_resizing = 0;
			}
			else
				super.handleMouseReleased(arg0);
		}
		
		@Override
		protected void handleMouseDragged(MouseArgs arg0) 
		{
			Location m = screenToLocal(arg0.x, arg0.y);
			switch (m_resizing) {
			case 1:
				setLocation(new Location(m_loc.x + m.x - m_size_dx, m_loc.y, Math.max(20, m_loc.w - m.x + m_size_dx), m_loc.h));
				onResize.fire(1);
				break;
			case 2:
				setLocation(new Location(m_loc.x, m_loc.y, Math.max(20, m_loc.w + m.x - m_size_dx), m_loc.h));
				m_size_dx = m.x;
				onResize.fire(2);
				break;
			}
		}
		
		@Override
		protected void handleMouseEntered(MouseArgs arg0) {
			if (m_resizing == 0)
				super.handleMouseEntered(arg0);
		}
		
		@Override
		protected void handleMouseExit(MouseArgs arg0) {
			if (m_resizing == 0)
				super.handleMouseExit(arg0);
		}
		
		@Override
		protected void handleSizeChanged() 
		{
			updateCursorArea(m_resAreaLeftID, new Location(0, 0, (m_allowResize&1)!=0?5:0, m_loc.h), null);
			updateCursorArea(m_resAreaRightID, new Location(m_loc.w-5, 0, (m_allowResize&2)!=0?5:0, m_loc.h), null);
		}
		
		@Override
		protected void updateFromStyle() {
			setFont(style.FONT);
		}
		
		@Override
		protected void paint(Graphics2D gfx) 
		{
			boolean skin_success = false;
			SkinState ss = null;
			if (!isEnabled())
				ss = style.skinStateDisabled;
			else
				switch (getButtonState()) {
				case 0: ss = style.skinStateNormal; break;
				case 1: ss = style.skinStateHover; break;
				case 2: ss = style.skinStatePressed; break;
				}
			
			if (ss != null) {
				WebImage skin = ss.getElement("skin");
				if (skin != null && skin.getState() == ResourceState.Loaded) {
					drawImage(skin, 0, 0, m_loc.w, m_loc.h);
					skin_success = true;
				}
				gfx.setColor(isEnabled() ? ss.getTextColor().awtClr : XColors.disabledDark.awtClr);
			}
			
			if (!skin_success) {
				// no skin available for current state, so use default drawing:
				Color fillClr = getButtonFillColor();
				getGUI().draw3DFrame(gfx, m_loc.untranslate(), fillClr, getButtonState()!=2);
				
				gfx.setColor(isEnabled() ? (ss!=null ? ss.getTextColor().awtClr : XColors.text.awtClr) : XColors.disabledDark.awtClr);
			}

			setClip(gfx, 2, 2, m_loc.w-4, m_loc.h-4);
			
			ButtonMetrics met = getButtonMetrics();
			
			//TODO if disabled, gray out image using a filter
			if (image != null)
				drawImage(image, (int)met.imgOffX, (int)met.imgOffY);
			
			met.textOffX += (getButtonState()==2 ? 1:0);
			met.textOffY += (getButtonState()==2 ? 1:0);
			m_textLayout.draw(gfx, met.textOffX, met.textOffY);
			
			restoreClip(gfx);
		}
	}
	
	public ListView(Integer x, Integer y, Integer w, Integer h) 
	{
		super(x,y,w,h);
		m_BB = m_BT = m_BL = m_BR = 2;
		clBackground = Color.white;
		focusable = true;
		
		m_Events.add(onSelectionChanged = new Event(this));
	}

	@Override
	public void paintBorder(Graphics2D gfx) {
		getGUI().draw3DFrame(gfx, m_loc.untranslate(), null, false);
	}
	
	@Override
	protected void init() 
	{
		super.init();
		updateFromStyle();
		m_RowHeight = 20;
		
		onColumnResize = new Functor() {			
			@Override
			public void Execute(Object sender, Object... params) {
				resizeColumns((HeaderButton)sender, (Integer)params[0]);
			}
		};
		
		onClientScroll.addListener(new Functor() {			
			@Override
			public void Execute(Object sender, Object... params) {
				int x = 0;
				int y = -m_clTranslateY;
				for (int i=0, n=m_Headers.size(); i<n; i++) {
					HeaderButton btn = m_Headers.get(i);
					btn.setPosition(x,y);
					x += btn.getLocation().w;
				}
			}});
		
		setVScrollbarPadding(25, 0);
	}
	
	public final Event onSelectionChanged;

	@Override
	protected void updateFromStyle() {
		// we use style?

	}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
	
	private ArrayList<String> m_Columns = new ArrayList<String>();
	private ArrayList<Integer> m_ColumnWidths = new ArrayList<Integer>();
	private ArrayList<HeaderButton> m_Headers = new ArrayList<HeaderButton>();
	private ArrayList<Record> m_Records = new ArrayList<Record>();
	private int m_RowHeight;
	private Functor onColumnResize;

	public void addColumn(String title, Integer width) 
	{
		int x = 0;
		for (int i : m_ColumnWidths)
			x += i;
		
		HeaderButton b = new HeaderButton(x, 0, width, 25, title);
		addControl(b);
		b.setAllowResize(!m_Headers.isEmpty(), true);
		b.onResize.addListener(onColumnResize);
		m_Headers.add(b);
		
		m_Columns.add(title);
		m_ColumnWidths.add(width);
	}
	
	protected void resizeColumns(HeaderButton sender, int side)
	{
		suspendPaint();
		
		for (int i=0, n=m_Headers.size(); i<n; i++) {
			HeaderButton btn = m_Headers.get(i);
			if (btn != sender)
				continue;
			
			if (side == 1) {
				HeaderButton btnleft = m_Headers.get(i-1);
				Location lloc = btnleft.getLocation();
				int minX = lloc.x + 20;
				Location bloc = btn.getLocation();
				if (bloc.x < minX) {
					int w = bloc.w + bloc.x - minX;
					btn.setLocation(minX,bloc.y,w,bloc.h);
					bloc = btn.getLocation();
					m_ColumnWidths.set(i, w);
				}
				int leftW = bloc.x-lloc.x;
				btnleft.setLocation(lloc.x, lloc.y,leftW,lloc.h);
				m_ColumnWidths.set(i-1, leftW);
				
				for (int j=0, m=m_Records.size(); j<m; j++) {
					Record r = m_Records.get(j);
					Location labLeftLoc = r.m_Labels[i-1].getLocation();
					r.m_Labels[i-1].setLocation(lloc.x, labLeftLoc.y, leftW, labLeftLoc.h);
					Location labLoc = r.m_Labels[i].getLocation();
					r.m_Labels[i].setLocation(bloc.x, labLoc.y, bloc.w, labLoc.h);
				}
			}
			
			if (side == 2) {
				if (i == n-1) {
					Location bloc = btn.getLocation();
					for (int j=0, m=m_Records.size(); j<m; j++) {
						Record r = m_Records.get(j);
						Location labLoc = r.m_Labels[i].getLocation();
						r.m_Labels[i].setLocation(labLoc.x, labLoc.y, bloc.w, labLoc.h);
					}
				}
				
				m_ColumnWidths.set(i, btn.getLocation().w);

				while (i<n-1){
					HeaderButton btnRight = m_Headers.get(i+1);
					Location bloc = btn.getLocation();
					btnRight.setPosition(bloc.x + bloc.w, btnRight.getLocation().y);
					
					for (int j=0, m=m_Records.size(); j<m; j++) {
						Record r = m_Records.get(j);
						Location labLoc = r.m_Labels[i].getLocation();
						r.m_Labels[i].setLocation(labLoc.x, labLoc.y, bloc.w, labLoc.h);
						r.m_Labels[i+1].setPosition(bloc.x+bloc.w, labLoc.y);
					}
					
					btn = btnRight;
					i++;
				}
			}
			
			break;
		}
		
		resumePaint(true);
	}
	
	public void addRecord(Integer customID, String[] data)
	{
		Label[] rLabels = new Label[data.length];
		int x = 0, y=25+m_RowHeight*m_Records.size();
		int lineID = m_Records.size();
		for (int i=0; i<data.length; i++) {
			rLabels[i] = new Label(data[i],x,y,m_ColumnWidths.get(i),m_RowHeight);
			addControl(rLabels[i]);
			rLabels[i].setCustomProperty("LineID", lineID);
			rLabels[i].setBackgroundColor(Color.transparent);
			rLabels[i].setBorderStyle(Label.BORDER_SIMPLE);
			rLabels[i].setBorderColor(Color.decode("#E8E8E8"));
			x += m_ColumnWidths.get(i);
			
			rLabels[i].onMouseClicked.addListener(new Functor() {				
				@Override
				public void Execute(Object sender, Object... params) {
					contentLabelClicked((Label)sender, (MouseArgs)params[0]);					
				}});
		}
		m_Records.add(new Record(customID, data, rLabels));
		for (int i=0, n=m_Headers.size(); i<n; i++)
			bringControlToTop(m_Headers.get(i));
		bringScrollbarsToTop();
		repaint();
	}
	
	protected ArrayList<Integer> selectedLines = new ArrayList<Integer>();
	
	protected void contentLabelClicked(Label sender, MouseArgs params) 
	{
		int lineID = (Integer)sender.getCustomProperty("LineID");
		
		if (params.checkModifier(MouseArgs.CTRL_DOWN_MASK)) {
			// ctrl is down, toggle the line clicked
			boolean alreadSel = selectedLines.contains(lineID);
			setLineSelected(lineID, !alreadSel);
			if (alreadSel)
				selectedLines.remove((Object)lineID);
			else
				selectedLines.add(lineID);
		}
		else
		if (params.checkModifier(MouseArgs.SHIFT_DOWN_MASK)) {
			// shift is down, select range
		}
		else
		{
			for (int i : selectedLines)
				setLineSelected(i, false);
			selectedLines.clear();
			setLineSelected(lineID, true);
			selectedLines.add(lineID);
		}
		
		onSelectionChanged.fire();
	}
	
	@Override
	protected void handleGetFocus() {
		// we do this to update the color to selectedFocused / selectedUnfocused
		for (int i : selectedLines)
			setLineSelected(i, true);
	}
	
	@Override
	protected void handleLostFocus() {
		// we do this to update the color to selectedFocused / selectedUnfocused
		for (int i : selectedLines)
			setLineSelected(i, true);
	}

	private void setLineSelected(int lineID, boolean bSelected) {
		Label[] labs = m_Records.get(lineID).m_Labels;
		Color bkc = bSelected?(isFocused()?XColors.selection:XColors.selectionUnfocused):Color.white;
		for (Label l : labs)
			l.setBackgroundColor(bkc);
	}

	public void setRowHeight(Integer height) {
		m_RowHeight = height;
		//TODO resize all labels?
		repaint();
	}

	public void clear() 
	{
		for (Record r : m_Records)
			for (Label l : r.m_Labels)
				removeControl(l);
		
		m_Records.clear();
		repaint();
	}

	/**
	 * @return an Integer array with the indices of the selected lines, or an empty array
	 * if no lines are selected.
	 */
	public Integer[] getSelectedLines() {
		Integer[] lines = new Integer[selectedLines.size()];
		return selectedLines.toArray(lines);
	}
	
	/**
	 * @return an array with the user-provided IDs associated with the currently selected lines,
	 * or an empty array if no lines are selected.
	 */
	public Integer[] getSelectedIDs() {
		Integer[] ids = new Integer[selectedLines.size()];
		for (int i=0; i<ids.length; i++)
			ids[i] = m_Records.get(selectedLines.get(i)).customID;
		return ids;
	}

	/**
	 * returns the custom ID (provided by user) of the line indicated by index (note that
	 * lines can change indexes when user sorts the list), or null if idx is not valid.
	 * @param idx
	 * @return
	 */
	public Integer getLineID(Integer idx) {
		if (idx < 0 || idx >= m_Records.size())
			return null;
		return m_Records.get(idx).customID;
	}

	public void selectLines(Integer[] lineIDs) 
	{
		if (lineIDs.length == 0)
			return;
		suspendPaint();
		for (int k=0; k<lineIDs.length; k++)
			for (int i=0; i<m_Records.size(); i++)
			{
				Record r = m_Records.get(i);
				if (r.customID.equals(lineIDs[k]))
					setLineSelected(i, true);
			}
		resumePaint(true);
	}
}
