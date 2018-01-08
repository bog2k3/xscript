import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;

import XScripter.Functor;
import xGUI.Window;
import xGUI.WindowDesc;
import xGUI.XGUI;
import xGUI.controls.Button;
import xGUI.controls.Button.ButtonLayout;
import xGUI.controls.CheckBox;
import xGUI.controls.EditBox;
import xGUI.controls.RichEdit;
import xGUI.controls.ScrollableView;
import xGUI.controls.ScrollBar;
import xapp.XApp;

/**
 * @author Bogdan.Ionita
 *
 */
public class CustomApp1 extends XApp 
{
	/**
	 * @param kernel
	 * @param appName
	 */
	public CustomApp1(XGUI xgui, String appName) {
		super(xgui, appName);
	}
	
	private RichEdit re = null;
	private EditBox eb_scroll = null;
	private ScrollBar sb = null;
	private ScrollableView sv;
	
	@Override
	public boolean init() {
		Window wnd1 = createWindow(new WindowDesc("Fereastra #1", 400, 260));
		wnd1.show();
		/*wnd1.addControl(sb = new ScrollBar(10,10,200,20,ScrollBar.SB_LAYOUT_HORIZONTAL));
		wnd1.addControl(new ScrollBar(240,10,20,200,ScrollBar.SB_LAYOUT_VERTICAL));
		sb.onScroll.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				eb_scroll.setText("Pos : "+sb.getPosition().toString());				
			}});
		wnd1.addControl(eb_scroll = new EditBox(10,100,100));
		eb_scroll.setReadonly(true);*/
		
		//wnd1.addControl(sv = new ScrollableView(10, 10, 300, 200));
		//sv.forceScrollbars(true);
		//sv.addControl(new Button(0,0,300,30,"0,0 -> 300,30"));
		//sv.addControl(new Button(260,260,100,30,"260,260 -> 360,290"));
		
		wnd1.addControl(new Button(20,20,60,30));
		Button btn;
		wnd1.addControl(btn = new Button(90,20,60,30));
		btn.setEnabled(false);
		btn.setText("Disabled");
		wnd1.addControl(new CheckBox(160, 20, "checkbox :D"));
		wnd1.addControl(new CheckBox(160, 40, "checkbox :D"));
		wnd1.addControl(btn = new CheckBox(160, 60, "checkbox disabled :P"));
		btn.setEnabled(false);
		wnd1.addControl(btn = new CheckBox(160, 80, "checkbox disabled & checked :P"));
		btn.setEnabled(false);
		((CheckBox)btn).setChecked(true);
		wnd1.addControl(new EditBox(20,100,200));
		wnd1.addControl(new EditBox(20,125,200));
		Button btnImagine = new Button(240, 100, 120, 60);
		wnd1.addControl(btnImagine);
		btnImagine.setImage("themes/default/icons/check_mark.png").setLayout(ButtonLayout.imageLeft);
		btnImagine.setText("Load theme!");
		
		btnImagine.onClick.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				xgui.loadTheme("themes/default/default_theme.xml");
			}
		});
		
		EditBox e = new EditBox(20,150,200);
		wnd1.addControl(e);
		e.setText("EditBox disabled.");
		e.setEnabled(false);
		Window wnd2 = createWindow(new WindowDesc("Fereastra #2", 350, 270));
		re = new RichEdit(10, 10, 200, 100);
		wnd2.addControl(re);
		re.enableWrap(true);
		Button b = new Button(230, 10, 0, 0, "Rosu");
		wnd2.addControl(b);
		b.onClick.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				re.setAttribute(TextAttribute.FOREGROUND, Color.red);
			}
		});
		
		b = new Button(230, 35, 0, 0, "Verde");
		wnd2.addControl(b);
		b.onClick.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				re.setAttribute(TextAttribute.FOREGROUND, Color.green);
			}
		});
		
		b = new Button(230, 60, 0, 0, "Bold");
		wnd2.addControl(b);
		b.onClick.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				re.setAttribute(TextAttribute.FONT, new Font("Arial",Font.BOLD,12));
			}
		});
		
		wnd2.show();
		
		Window wnd3 = createWindow(new WindowDesc("Fereastra #3", 420, 230));
		wnd3.show();
		
		return true;
	}

}
