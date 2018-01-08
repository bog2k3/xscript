package xGUI.controls;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import wrappers.Color;
import wrappers.Cursor;
import xGUI.KeyArgs;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.VisualComponent;
import xGUI.XColors;

public class RichEdit extends VisualComponent 
{
	/** on which paragraph the caret is positioned */
	private int iCurrentParagraph = -1;
	/** caret offset into current paragraph */
	private int iCaretOffs = 0;
	/** absolute caret position in text */
	private int iCaretPos = 0;
	private Shape caret = null;
	/** caret translation */
	private int caretTransX=0, caretTransY=0;
	private boolean caretVisible = false;
	
	protected boolean m_readonly = false;
	
	/** the vertical distance, in pixels, between the start of two adjacent lines of text (this is the height of the line
	 * plus the distance between lines. */
	//private int iLineSpan = 0;
	
	/** white pixels between border and text */
	private int whiteBorderX = 5;
	/** white pixels between border and text */
	private int whiteBorderY = 3;
	
	/** horizontal scroll in pixels (always 0 or negative) */
	private int iScrollX = 0;
	/** vertical scroll in pixels (always 0 or negative) */
	private int iScrollY = 0;
	/** virtual caret X position (when moving the caret up/down */
	private int virtualCaretX = 0;
	
	private boolean bWrapEnabled = false;
	
	/** this defines and stores the data required to edit and display a paragraph.
	 * a paragraph is a length of text that ends with a line return, but can consist of multiple lines if the text
	 * is wrapped around by a width limit.
	 * @author bog
	 *
	 */
	private class Paragraph
	{
		Paragraph next = null;
		
		/** offset in the enclosing text where the paragraph starts */
		int offset;
		/** vertical offset from the start of the text */
		int yPos;
		int endY;
		
		/** initial attribute map */
		HashMap<Attribute, Object> initialAttribs;
		
		/** character gap-buffer */
		private int nFilled = 0;
		private final int gapInitialWidth = 16;
		private int gapStart = 0;
		private int gapWidth = gapInitialWidth;
		private char[] buf = new char[gapInitialWidth];
		
		class AttribModifier {
			AttribModifier(AttribModifier att) {
				attr = att.attr;
				newValue = att.newValue;
				iTextPos = att.iTextPos;
			}
			
			public AttribModifier(int pos, Attribute a, Object v) {
				iTextPos = pos;
				attr = a;
				newValue = v;
			}

			Attribute attr;			// attribute to be modified
			Object newValue;		// value to be assigned to the attribute
			int iTextPos;			// position in text where attribute change takes place
		}		
		/** attribute modifier buffer */
		ArrayList<AttribModifier> modifiers;
		
		class TextChunk {
			TextLayout layout;
			int offset;
			int length;
			int x, y;
			Rectangle rcBounds;
			int selStart = -1;
			int selEnd = -1;
			Shape selection = null;
			
			TextChunk(int offset, int length, int x, int y, TextLayout layout) {
				this.layout = layout;
				this.offset = offset;
				this.length = length;
				
				this.x = x;
				this.y = y;
				
				rcBounds = layout.getPixelBounds(null, x, y);
			}

			void rebuildSelection(int locSelStart, int locSelEnd) {
				selStart = locSelStart;
				selEnd = locSelEnd;
				if (layout != null)
					selection = layout.getVisualHighlightShape(TextHitInfo.beforeOffset(selStart), TextHitInfo.afterOffset(selEnd));
				else
					selection = null;
			}
		}
		/** the graphical representation of this paragraph*/
		ArrayList<TextChunk> textChunks;
		/** a LineBreakMeasurer breaks text into layouts based on wrapping width */
		LineBreakMeasurer lineBreaker = null;
		FontRenderContext frc = null;
		Font defaultFont = null;
		
		Rectangle rcBounds;
		
		/** maximum wrapping width for text. 
		 * if 0, text is not wrapped */
		private int wrapWidth = 0;
		void setWrappingWidth(int w) {
			wrapWidth = w;
			updateLayouts(false);
		}
		
		private void updateLayouts(boolean textChanged) {
			if (frc == null)
				return;
			
			if (textChanged)
				lineBreaker = nFilled > 0 ? new LineBreakMeasurer(it, frc) : null;
			
			textChunks.clear();
			if (lineBreaker != null) {
				rcBounds.y = Integer.MAX_VALUE;
				rcBounds.x = Integer.MAX_VALUE;
				rcBounds.width = 0;
				rcBounds.height = 0;
				
				lineBreaker.setPosition(0);
				int pos_before;
				int y = 0;
				while ((pos_before = lineBreaker.getPosition()) < nFilled) 
				{
					TextLayout tl = lineBreaker.nextLayout(wrapWidth > 0 ? wrapWidth : Float.MAX_VALUE);
					y += tl.getAscent();
					
					TextChunk c = new TextChunk(pos_before, lineBreaker.getPosition() - pos_before, 0, y, tl);
					textChunks.add(c);
					
					Rectangle tlb = tl.getPixelBounds(null, 0, y);
					rcBounds.x = Math.min(rcBounds.x, tlb.x);
					rcBounds.y = Math.min(rcBounds.y, tlb.y);
					rcBounds.width = -rcBounds.x + Math.max(rcBounds.width+rcBounds.x, tlb.width + tlb.x);
					rcBounds.height = -rcBounds.y + Math.max(rcBounds.height+rcBounds.y, tlb.height + tlb.y);
					
					y += tl.getDescent() + tl.getLeading();
				}
				
				int oldEndY = endY;
				endY = yPos + y; //use this endY when inserting a new paragraph after this one
				if (endY != oldEndY && next != null)
					next.push(endY-oldEndY);
			} else {
				//handle empty paragraph case, must still show caret
				TextLayout tl = new TextLayout(" ",defaultFont,frc);
				textChunks.add(new TextChunk(0,0,0,(int)tl.getAscent(),tl));
			}
		}
		
		void setFontRenderContext(FontRenderContext frc, Font defaultFont) {
			this.frc = frc;
			this.defaultFont = defaultFont;
			 //insert default font as attribute at the begining of text:
			initialAttribs.put(TextAttribute.FONT, defaultFont);
			initialAttribs.put(TextAttribute.FOREGROUND, Color.black);
			updateLayouts(true);
		}
		
		/** when creating new paragraph, pass the end set of attributes of the previous */
		Paragraph(int offs, int yPos, HashMap<Attribute, Object> hashMap)
		{
			this.offset = offs;
			this.yPos = yPos;
			this.initialAttribs = hashMap;
			rcBounds = new Rectangle(0,0,0,0);
			modifiers = new ArrayList<AttribModifier>();
			textChunks = new ArrayList<TextChunk>();
		}
		
		private void moveGap(int pos) {
			if (pos < 0 || pos > nFilled)
				return;
			while (gapStart < pos) {// move right 
				buf[gapStart] = (gapStart + gapWidth < buf.length) ? buf[gapStart + gapWidth] : 0;
				gapStart++;
			}
			while (gapStart > pos) // move left
				buf[--gapStart + gapWidth] = buf[gapStart];
		}
		
		private void checkGap() {
			if (gapWidth == 0) {
				// gap filled, need to realloc
				char[] nBuf = new char[nFilled + gapInitialWidth];
				for (int i=0; i<gapStart; i++)
					nBuf[i] = buf[i];
				for (int i=gapStart; i<nFilled; i++)
					nBuf[i+gapInitialWidth] = buf[i];
				gapWidth = gapInitialWidth;
				buf = nBuf;
			}
		}

		void insertChar(char keyChar, int pos) {
			if (pos < 0 || pos > nFilled)
				return;
			if (pos != gapStart)
				moveGap(pos);
			buf[gapStart++] = keyChar;
			gapWidth--;
			boolean needRecreate = nFilled == 0;
			nFilled++;
			checkGap();
			if (!needRecreate)
				lineBreaker.insertChar(it, pos);
			if (next != null)
				next.modifyOffset(+1);
			updateLayouts(needRecreate);
		}

		private void push(int dy) {
			yPos += dy;
			endY += dy;
			if (next != null)
				next.push(dy);
		}

		private void modifyOffset(int i) {
			offset += i;
			if (next != null)
				next.modifyOffset(i);
		}

		boolean deleteChar(int pos) {
			if (pos < 0 || pos >= nFilled)
				return false;
			
			if (pos == getLastValidPos()) {
				// special case, merge with the next paragraph
				if (next == null)
					return false;
				
				// copy chars:
				char[] newBuf = new char[nFilled-1 + next.nFilled + gapInitialWidth];
				moveGap(pos);
				for (int i=0; i<pos; i++)
					newBuf[i] = buf[i];
				next.moveGap(next.length());
				for (int i=0; i<next.nFilled; i++)
					newBuf[pos+i] = next.buf[i];
				nFilled += next.nFilled - 1;
				buf = newBuf;
				gapStart = newBuf.length - gapInitialWidth;
				gapWidth = gapInitialWidth;
				
				for (AttribModifier m : next.modifiers) {
					m.iTextPos += pos;
					modifiers.add(m);
				}
				it.setIndex(pos);
				HashMap<Attribute, Object> h = it.getAttributes();
				for (Attribute a : next.initialAttribs.keySet()) {
					Object v = next.initialAttribs.get(a);
					if (!h.containsKey(a) || !h.get(a).equals(v)) {
						modifiers.add(new AttribModifier(pos, a, v));
					}
				}
				Paragraph toBeDeleted = next;
				next = toBeDeleted.next;
				toBeDeleted.next = null;
				endY = toBeDeleted.endY;
				updateLayouts(true);
				
				if (next != null)
					next.modifyOffset(-1);
				
				return true;
			}
			
			//TODO daca raman modifiere dincolo de limita textului, sterge-le
			
			// just delete the character:
			if (pos != gapStart)
				moveGap(pos);
			gapWidth++;
			nFilled--;
			lineBreaker.deleteChar(it, pos);
			if (next != null)
				next.modifyOffset(-1);
			updateLayouts(false);
			return false;
		}

		int length() {
			return nFilled;
		}
		
		int getLastValidPos() {
			if (nFilled == 0)
				return 0;
			
			int pos;
			if (nFilled-1 < gapStart)
				pos = nFilled-1;
			else
				pos = nFilled-1 + gapWidth;
			
			if (buf[pos]=='\n')
				return nFilled-1;
			else
				return nFilled;
		}
		
		Paragraph split(int i) 
		{
			it.setIndex(i);
			Paragraph p = new Paragraph(offset+i,0,it.getAttributes());
			moveGap(i);
			
			// copy chars:
			p.buf = new char[p.gapWidth + length() - i];
			for (int k=i, n=length(); k<n; k++)
				p.buf[k-i] = buf[k+gapWidth];
			p.gapStart = length() - i;
			p.nFilled = p.gapStart;
			// copy attributes:
			int kS = 0, nS = modifiers.size();
			while (kS<nS && modifiers.get(kS).iTextPos <= i)
				kS++;
			for (int k=kS; k<nS; k++) {
				AttribModifier attr = new AttribModifier(modifiers.get(kS));
				modifiers.remove(kS);
				attr.iTextPos -= i;
				p.modifiers.add(attr);
			}
			p.next = next;
			
			next = null; // prevent update layout from pushing any other paragraphs, we do that manually later
			nFilled = i+1; // set the new character count
			buf[gapStart++] = '\n';
			gapWidth = buf.length - nFilled;
			updateLayouts(true);
			next = p;
			p.yPos = endY;
			p.endY = p.next != null ? p.next.yPos : 0;
			p.setFontRenderContext(frc, defaultFont);
			p.updateLayouts(true);
			p.modifyOffset(+1);
			
			return p;
		}
		
		class Iterator implements AttributedCharacterIterator, Cloneable

		{
			Paragraph p;
			private int pos = 0;
			
			Iterator(Paragraph p) {
				this.p = p;
			}
			
			@Override 
			public Object clone() { try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return null;
			} }
			
			@Override
			public Set<Attribute> getAllAttributeKeys() {
				Set<Attribute> set = new HashSet<Attribute>(p.initialAttribs.keySet());
				for (AttribModifier a : p.modifiers)
					if (!set.contains(a.attr))
						set.add(a.attr);
				return set;
			}

			@Override
			public Object getAttribute(Attribute attribute) {
				Object value = p.initialAttribs.get(attribute);
				AttribModifier m;
				for (int i=0, n =p.modifiers.size(); i<n && ((m = p.modifiers.get(i)).iTextPos <= pos); i++)
					if (m.attr.equals(attribute))
						value = m.newValue;
				return value;
			}

			@SuppressWarnings("unchecked")
			@Override
			public HashMap<Attribute, Object> getAttributes() {
				HashMap<Attribute, Object> map = (HashMap<Attribute, Object>) p.initialAttribs.clone();
				AttribModifier m;
				for (int i=0, n=p.modifiers.size(); i<n && ((m = p.modifiers.get(i)).iTextPos <= pos); i++)
					map.put(m.attr, m.newValue);
				return map;
			}

			@Override
			public int getRunLimit() {
				int i,n;
				AttribModifier m = null;
				for (i=0, n=p.modifiers.size(); i<n && ((m = p.modifiers.get(i)).iTextPos <= pos); i++)
					;
				if (i < n)
					return m.iTextPos;
				else
					return length();
			}

			@Override
			public int getRunLimit(Attribute attribute) {
				int i,n;
				AttribModifier m = null;
				for (i=0, n=p.modifiers.size(); i<n && (
					(m = p.modifiers.get(i)).iTextPos <= pos
					|| !m.attr.equals(attribute)
				); i++)
					;
				if (i < n)
					return m.iTextPos;
				else
					return length();
			}

			@Override
			public int getRunLimit(Set<? extends Attribute> attributes) {
				int i,n;
				AttribModifier m = null;
				for (i=0, n=p.modifiers.size(); i<n && (
					(m = p.modifiers.get(i)).iTextPos <= pos
					|| !attributes.contains(m.attr)
				); i++)
					;
				if (i < n)
					return m.iTextPos;
				else
					return length();
			}

			@Override
			public int getRunStart() {
				int i=-1,n=p.modifiers.size();
				AttribModifier m = null;
				while (i<n-1 && (m = p.modifiers.get(i+1)).iTextPos <= pos)
					i++;
				if (i >= 0)
					return m.iTextPos;
				else
					return 0;
			}

			@Override
			public int getRunStart(Attribute attribute) {
				int i=-1,n=p.modifiers.size();
				AttribModifier m = null;
				int start = 0;
				while (i<n-1 && (m = p.modifiers.get(i+1)).iTextPos <= pos)
				{
					if (m.attr.equals(attribute))
						start = m.iTextPos;
					i++;
				}
				return start;
			}

			@Override
			public int getRunStart(Set<? extends Attribute> attributes) {
				int i=-1,n=p.modifiers.size();
				AttribModifier m = null;
				int start = 0;
				while (i<n-1 && (m = p.modifiers.get(i+1)).iTextPos <= pos)
				{
					if (attributes.contains(m.attr))
						start = m.iTextPos;
					i++;
				}
				return start;
			}

			@Override
			public char current() {
				if (pos >= p.nFilled)
					return DONE;
				
				if (pos < p.gapStart)
					return p.buf[pos];
				else
					return p.buf[pos + gapWidth];
			}

			@Override
			public char first() {
				pos = getBeginIndex();
				return current();
			}

			@Override
			public int getBeginIndex() {
				return 0;
			}

			@Override
			public int getEndIndex() {
				return p.nFilled;
			}

			@Override
			public int getIndex() {
				return pos;
			}

			@Override
			public char last() {
				int end = getEndIndex();
				end = Math.max(0, end-1);
				pos = end;
				return current();
			}

			@Override
			public char next() {
				pos++;
				return current();
			}

			@Override
			public char previous() {
				if (pos == getBeginIndex())
					return DONE;
				else {
					pos--;
					return current();
				}
			}

			@Override
			public char setIndex(int position) {
				if (position >= getBeginIndex() && position <= getEndIndex()) {
					pos = position;
					return current();
				} else
					return DONE;
			}
		}
		
		Iterator it = new Iterator(this);
		public void sortModifiers() {
			for (int n=modifiers.size(), i=0; i<n-1; i++)
				for (int j=i+1; j<n; j++) {
					AttribModifier mi = modifiers.get(i);
					AttribModifier mj = modifiers.get(j);
					if (mj.iTextPos < mi.iTextPos) {
						modifiers.set(i, mj);
						modifiers.set(j, mi);
					}
				}
		}
		
	}
	
	private ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>(16);
	
	void addParagraph(int offset) {
		if (offset == getLength()) {
			Paragraph pLast = paragraphs.size() > 0 ? paragraphs.get(paragraphs.size()-1) : null;
			Paragraph p;
			if (pLast != null) {
				pLast.insertChar('\n', pLast.length());
				pLast.it.setIndex(pLast.length());
				p = new Paragraph(offset+1, pLast.endY, pLast.it.getAttributes());
				Graphics2D gfx = getGfx();
				if (gfx != null)
					p.setFontRenderContext(gfx.getFontRenderContext(), gfx.getFont());
			} else {
				p = new Paragraph(offset, 0, new HashMap<Attribute, Object>());
				Graphics2D gfx = getGfx();
				if (gfx != null)
					p.setFontRenderContext(gfx.getFontRenderContext(), gfx.getFont());
			}
			if (pLast != null) {
				pLast.next = p;
			}
			p.setWrappingWidth(bWrapEnabled ? m_loc.w - 2*whiteBorderX : 0);
			paragraphs.add(p);
			moveCaret(0, +1, false);
		} else {
			// find current paragraph
			int iPar = 0;
			Paragraph p = paragraphs.get(iPar); 
			while (p.offset + p.getLastValidPos() < offset)
				p = paragraphs.get(++iPar);
			
			// p is to be split
			Paragraph p1 = p.split(offset-p.offset);
			paragraphs.add(iPar+1,p1);
			moveCaret(0, +1, false);
		}
	}
	
	private int selStart = -1; //selection start (absolute offset)
	private int selEnd = -1; // selection end (absolute offset)
	private int selOrigin = -1; // selection origin (where it first started)
	
	@Override
	protected synchronized void paint(Graphics2D gfx)
	{
		Location lFrame = new Location(1, 1, m_loc.w-2, m_loc.h-2);
		getGUI().draw3DFrame(gfx, lFrame, isEnabled() ? XColors.editBackground : XColors.disabledLight, false);
		if (isFocused()) {
			gfx.setColor(Color.gray75.awtClr);
			gfx.drawRect(0, 0, m_loc.w-1, m_loc.h-1);
		}
		
		setClip(gfx, whiteBorderX, whiteBorderY, m_loc.w - 2*whiteBorderX, m_loc.h - 2*whiteBorderY);
		// set scroll translation:
		gfx.translate(whiteBorderX + iScrollX, whiteBorderY + iScrollY);
			//draw each paragraph of text that intersects the scrolled view's clip area
			for (int i=0, n=paragraphs.size(); i<n; i++) {
				Paragraph p = paragraphs.get(i);
				if (p.frc == null) {
					p.setFontRenderContext(gfx.getFontRenderContext(), gfx.getFont());
				}
				//TODO verify against clip region instead of window
				if (p.endY > -iScrollY && p.yPos < -iScrollY + m_loc.h - 2*whiteBorderY) {
					for (xGUI.controls.RichEdit.Paragraph.TextChunk c : p.textChunks) {
						if (selStart != selEnd) { // if selection is not empty
							int locSelStart = selStart - p.offset - c.offset;
							int locSelEnd = selEnd - p.offset-c.offset;
							// check if selection intersects this chunk:
							if (locSelEnd >= 0 && locSelStart <= c.length) {
								// clamp the coords to chunk:
								if (locSelStart < 0)
									locSelStart = 0;
								if (locSelEnd > c.length)
									locSelEnd = c.length;
								if (locSelStart != c.selStart || locSelEnd != c.selEnd)
									c.rebuildSelection(locSelStart, locSelEnd);
								gfx.setColor(isFocused() ? XColors.selection.awtClr : XColors.selectionUnfocused.awtClr);
								gfx.translate(c.x, p.yPos + c.y);
								gfx.fill(c.selection);
								gfx.translate(-c.x, -p.yPos - c.y);
							}
						}
						gfx.setColor(XColors.text.awtClr);
						c.layout.draw(gfx, c.x, p.yPos + c.y);
					}
				}
			}
			
			if (caretVisible && caret != null) {
				gfx.setColor(Color.red.awtClr);
				gfx.translate(caretTransX, caretTransY);
				//translate from layout-space to paragraph-space and to scrolled-view-space
				gfx.draw(caret);
				gfx.translate(-caretTransX, -caretTransY);
			}
		gfx.translate(-whiteBorderX - iScrollX, -whiteBorderY - iScrollY);
		restoreClip(gfx);
	}
	
	@Override
	protected void init() 
	{
		setLocation(init_loc);
		paragraphs = new ArrayList<Paragraph>(32);
		addParagraph(0);
		iCurrentParagraph = 0;
		setCaretPos(0, false);
	}
	
	@Override
	protected void updateFromStyle() { }
	
	public RichEdit(Integer x, Integer y, Integer w, Integer h) 
	{
		m_visible = true;
		setFocusable(true);
		init_loc = new Location(x,y,Math.max(w, 40),Math.max(h,22));
		cursor = Cursor.TEXT;
	}
	
	@Override
	protected void handleMousePressed(MouseArgs arg0) {
		suspendPaint();
			Location lClick = screenToLocal(arg0.x, arg0.y);
			setCaretPos(getHitOffset(lClick.x, lClick.y), arg0.checkModifier(MouseArgs.SHIFT_DOWN_MASK));
		resumePaint(true);
	}
	
	@Override
	protected void handleMouseDragged(MouseArgs arg0) {
		suspendPaint();
			Location lClick = screenToLocal(arg0.x, arg0.y);
			setCaretPos(getHitOffset(lClick.x, lClick.y), true);
		resumePaint(true);
	}
	
	@Override
	protected void handleMouseWheel(MouseArgs arg0) {
		iScrollY -= arg0.wheelSteps * 15;
		if (iScrollY > 0)
			iScrollY = 0;
		int iMaxScroll = paragraphs.get(paragraphs.size()-1).endY - m_loc.h + 2*whiteBorderY; 
		if (iScrollY < -iMaxScroll)
			iScrollY = -iMaxScroll;
		repaint();
	}
	
	@Override
	protected void handleGetFocus() {
		showCaret();
	}
	
	@Override
	protected void handleLostFocus() {
		hideCaret();
	}
	
	@Override
	protected boolean handleKeyTyped(KeyArgs arg) {
		if (!m_readonly && arg.isChar()) {
			suspendPaint();
				paragraphs.get(iCurrentParagraph).insertChar(arg.keyChar, iCaretOffs);
				moveCaret(0, +1, false);
			resumePaint(true); // TODO only repaint affected area (set clip)
			return true;
		} else
			return false;
	}
	
	@Override
	protected boolean handleKeyPressed(KeyArgs arg) {
		switch (arg.keyCode) {
		
		case KeyArgs.KEY_BACKSPACE:
			if (iCaretPos <= 0)
				return true;
			suspendPaint();
			moveCaret(0, -1, false);
			if (paragraphs.get(iCurrentParagraph).deleteChar(iCaretOffs))
				paragraphs.remove(iCurrentParagraph+1);
			resumePaint(true);
			return true;
		case KeyArgs.KEY_DELETE:
			if (iCaretPos == getLength())
				return true;
			suspendPaint();
			if (paragraphs.get(iCurrentParagraph).deleteChar(iCaretOffs))
				paragraphs.remove(iCurrentParagraph+1);
			resumePaint(true);
			return true;
		case KeyArgs.KEY_LEFT:
			moveCaret(0, -1, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			return true;
		case KeyArgs.KEY_RIGHT:
			moveCaret(0, +1, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			return true;
		case KeyArgs.KEY_UP:
			moveCaret(-1, 0, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			return true;
		case KeyArgs.KEY_DOWN:
			moveCaret(+1, 0, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			return true;
		case KeyArgs.KEY_HOME:
			if (arg.checkModifier(KeyArgs.CTRL_DOWN_MASK))
				setCaretPos(paragraphs.get(0).offset, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			else
				setCaretPos(paragraphs.get(iCurrentParagraph).offset, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			if (caret != null)
				virtualCaretX = caretTransX + caret.getBounds().x;
			return true;
		case KeyArgs.KEY_END:
			if (arg.checkModifier(KeyArgs.CTRL_DOWN_MASK))
				setCaretPos(paragraphs.get(paragraphs.size()-1).offset + paragraphs.get(paragraphs.size()-1).length(),
						arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			else
				setCaretPos(paragraphs.get(iCurrentParagraph).offset + paragraphs.get(iCurrentParagraph).getLastValidPos(),
						arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			if (caret != null)
				virtualCaretX = caretTransX + caret.getBounds().x;
			return true;
		case KeyArgs.KEY_ENTER:
			addParagraph(iCaretPos);
			return true;
		default: return false;
		}
	}
	
	@Override
	protected void handleSizeChanged() {
		for (Paragraph p : paragraphs)
			p.setWrappingWidth(bWrapEnabled ? (m_loc.w - 2*whiteBorderX) : 0);
	}
	
	// === Public methods ==============================================================
	
	public Integer getLength() {
		if (paragraphs.isEmpty())
			return 0;
		Paragraph pLast = paragraphs.get(paragraphs.size()-1);
		return pLast.offset + pLast.length();
	}

	public Integer getCaretPos() { return iCaretPos; }
	public Integer getActiveLine() { return iCurrentParagraph; }
	public void setCaretPos(Integer pos, Boolean shiftDown)
	{
		if (pos == iCaretPos && caret != null) {
			if (!shiftDown && selStart != selEnd) {
				selOrigin = selStart = selEnd = iCaretPos;
				repaint(); //TODO only caret area instead of whole box
			}
			return;
		}
		
		suspendPaint();
		
		hideCaret();
		iCaretPos = Math.max(0, Math.min(pos,getLength()));
		iCaretOffs = iCaretPos;
		iCurrentParagraph = 0;
		while (iCaretOffs >= paragraphs.get(iCurrentParagraph).length()) {
			if (iCurrentParagraph < paragraphs.size()-1) {
				iCaretOffs -= paragraphs.get(iCurrentParagraph).length();
				iCurrentParagraph++;
			} else
				break;
		}
		
		Paragraph p = paragraphs.get(iCurrentParagraph);
		caret = null;
		if (!p.textChunks.isEmpty())
			for (xGUI.controls.RichEdit.Paragraph.TextChunk c : p.textChunks)
				if (c.offset + c.length >= iCaretOffs) {
					if (c.layout == null)
						caret = null;
					else {
						caret = c.layout.getCaretShapes(iCaretOffs-c.offset)[0];
						caretTransX = c.x;
						caretTransY = c.y + p.yPos;
					}
					break;
				}
		if (caret != null) {
			Rectangle2D rb = caret.getBounds2D();
			// check for caret outside of edit box area:
			if (rb.getX() + caretTransX + iScrollX < whiteBorderX + 1)
				iScrollX = Math.min(0, (int) -rb.getX() - caretTransX + whiteBorderX + 1);
			if (rb.getMaxX() + caretTransX + iScrollX >= m_loc.w - 2*whiteBorderX - 1)
				iScrollX = m_loc.w - 2*whiteBorderX - 1 -(int)rb.getMaxX() - caretTransX;
			
			if (rb.getY() + caretTransY + iScrollY < whiteBorderY + 1)
				iScrollY = Math.min(0, (int) -rb.getY() - caretTransY + whiteBorderY + 1);
			if (rb.getMaxY() + caretTransY + iScrollY >= m_loc.h - 2*whiteBorderY - 1)
				iScrollY = m_loc.h - 2*whiteBorderY - 1 -(int)rb.getMaxY() - caretTransY;
			showCaret();
		}
		
		// check and modify selection range:
		if (!shiftDown) {
			boolean repaint = selStart!=selEnd;
			selOrigin = selStart = selEnd = iCaretPos;
			if (repaint)
				repaint();
		} else {
			selStart = Math.min(selOrigin, iCaretPos);
			selEnd = Math.max(selOrigin, iCaretPos);
			repaint();
		}
		
		resumePaint(true);
	}
	
	/** moves the insertion caret by the given amount in horizontal or
	 * vertical direction.
	 * @param lineDelta amount of lines to move (+/-)
	 * @param offsDelta amount of characters to move left or right (+/-)
	 * @param shiftDown this simulates holding down the shift key (to select text)
	 */
	public void moveCaret(Integer lineDelta, Integer offsDelta, Boolean shiftDown) 
	{
		suspendPaint();
		if (lineDelta != 0) {
			Paragraph p = paragraphs.get(iCurrentParagraph);
			int iChunk = 0;
			while (iCaretOffs >= p.textChunks.get(iChunk).offset + p.textChunks.get(iChunk).length) {
				if (iChunk < p.textChunks.size() - 1)
					iChunk++;
				else
					break;
			}
			xGUI.controls.RichEdit.Paragraph.TextChunk t = p.textChunks.get(iChunk);
			int ydelta = (int) (lineDelta * (t.layout.getAscent() + t.layout.getDescent() + t.layout.getLeading()));
			if (ydelta < 0)
				ydelta--;
			else
				ydelta++;
			
			if (ydelta < 0 || p.next != null || iChunk != p.textChunks.size() - 1)
				setCaretPos(getHitOffset(virtualCaretX + iScrollX + whiteBorderX, 
						ydelta + t.rcBounds.y + p.yPos + iScrollY + whiteBorderY),
						shiftDown);
		}
		
		if (offsDelta != 0) {
			setCaretPos(iCaretPos + offsDelta, shiftDown);
			if (caret != null)
				virtualCaretX = caretTransX + caret.getBounds().x;
		}
		resumePaint(true);
	}
	
	/** returns the offset in the text where the given point would hit.
	 * the coordinates are in local visible space (not absolute text space, because if the view is
	 * scrolled, the coordinates will have a different meaning). it's like clicking with the mouse.
	 */
	public int getHitOffset(Integer x, Integer y)
	{
		x -= iScrollX + whiteBorderX;
		y -= iScrollY + whiteBorderY;
		
		Paragraph pTarget = null;
		for (Paragraph p : paragraphs)
			if (p.endY > y) {
				pTarget = p;
				break;
			}
		
		if (pTarget == null) {
			if (paragraphs.size() == 0)
				return 0;
			return paragraphs.get(paragraphs.size()-1).offset + paragraphs.get(paragraphs.size()-1).getLastValidPos();
		}
		
		y -= pTarget.yPos;
		
		for (xGUI.controls.RichEdit.Paragraph.TextChunk t : pTarget.textChunks) {
			int chunkY2 = t.rcBounds.y + t.rcBounds.height;
			if (chunkY2>y || 
					pTarget.textChunks.get(pTarget.textChunks.size()-1) == t // if last one, use it
			) {
				TextHitInfo hit = t.layout.hitTestChar(x, y); 
				return Math.min(hit.getInsertionIndex() + t.offset + pTarget.offset, pTarget.offset + pTarget.getLastValidPos());
			}
		}
		
		if (paragraphs.size() == 0)
			return 0;
		return paragraphs.get(paragraphs.size()-1).offset + paragraphs.get(paragraphs.size()-1).getLastValidPos();
	}
	
	/** hides the caret. */
	public void hideCaret()	{
		if (caretVisible) {
			caretVisible = false;
			//TODO only clip on the carret area
			repaint();
		}
	}
	
	/** shows the caret */
	public void showCaret() {
		if (isFocused()) {
			caretVisible = true;
			//TODO only clip on the carret area
			repaint();
		}
	}
	
	/** enable word-wrapping, breaking lines that are too long to display in
	 * one screen
	 * @param bEnable
	 */
	public void enableWrap(Boolean bEnable) {
		bWrapEnabled = bEnable;
		handleSizeChanged();
	}
	
	/** selects a range of text */
	public void setSelection(Integer start, Integer end) {
		selStart = selOrigin = Math.min(start, end);
		selEnd = Math.max(start, end);
		repaint();
	}
	
	public Integer getSelectionStart() {
		return selStart;
	}
	
	public Integer getSelectionEnd() {
		return selEnd;
	}
	
	/** apply the attribute to the currently selected text (if any) */
	public void setAttribute(TextAttribute attr, Object value) {
		setAttribute(selStart, selEnd, attr, value);
	}
	
	/** apply the attribute value to the range of text between start and end */
	public void setAttribute(Integer start, Integer end, TextAttribute attr, Object value)
	{
		checkAttachedToParent();
		if (start >= end)
			return;
		
		suspendPaint();

		Paragraph p = paragraphs.get(0);
		while (p != null && p.offset < end) {
			if (p.offset + p.length() > start) {
				// the selection range intersects this paragraph
				int pstart = Math.max(0, start - p.offset);
				int pend = Math.min(p.length(), end - p.offset);
				boolean bEndRestored = false;
				for (int n=p.modifiers.size(), i=0; i<n; i++) {
					Paragraph.AttribModifier m = p.modifiers.get(i);
					if (m.iTextPos < pend && m.attr.equals(attr))
					{
						int endEffect = -1;
						for (int k=i+1; k<n; k++)
							if (p.modifiers.get(k).attr.equals(attr)) {
								endEffect = p.modifiers.get(k).iTextPos;
								break;
							}
						if (endEffect == -1)
							endEffect = p.length();
						
						if (endEffect <= pstart) // modifier not intersecting the selected area
							continue;
						
						if (endEffect > pend) {
							// the modifier effect area spans outside of selection
							if (m.iTextPos >= pstart) {
								// the modifier started inside the selection, just move it to the end
								m.iTextPos = pend;
								bEndRestored = true;
							} else {
								// the modifier started before the selection. add a new one to the end
								p.modifiers.add(p.new AttribModifier(pend,m.attr,m.newValue));
								bEndRestored = true;
							}
							p.sortModifiers();
						} else if (m.iTextPos >= pstart){
							p.modifiers.remove(i--);
							n--;
						}
					} else
						if (m.iTextPos == pend && m.attr.equals(attr))
							bEndRestored = true;
				}
				if (!bEndRestored && pend != p.length()) {
					p.modifiers.add(p.new AttribModifier(pend,attr,p.initialAttribs.get(attr)));
					p.sortModifiers();
				}
				if (pstart > 0) {
					p.modifiers.add(p.new AttribModifier(pstart,attr,value));
					p.sortModifiers();
				} else
					p.initialAttribs.put(attr, value);
				
				p.updateLayouts(true);
			}
			p = p.next;
		}
		
		repaint();
		resumePaint(true);
	}
	
	public void setReadonly(Boolean ro) {
		m_readonly = ro;
	}
	public Boolean getReadonly() { return m_readonly; }

	@Override
	public void destroy() {}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
