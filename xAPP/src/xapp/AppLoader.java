package xapp;

import java.util.ArrayList;
import java.util.HashMap;

import xGUI.XGUI;

import XScripter.ILibraryFetcher;
import XScripter.IScriptable;
import XScripter.IWarningHandler;
import XScripter.Script;
import XScripter.ScriptClass;
import XScripter.ScriptError;
import XScripter.TypeID;
import XScripter.Variable;
import kernel.LogLevel;
import kernel.MediaManager;
import kernel.XKernel;

public class AppLoader implements ILibraryFetcher, IWarningHandler
{
	private class library 
	{
		Script lib;
		String url;
		
		library(String url) {
			this.url = url;
		}
		
		boolean load() {
			if (lib != null)
				return false;
			lib = mediaMan.readScript(url);
			if (lib == null) {
				kernel.log(m_logChannel, LogLevel.Error, "Couldn't load \""+url+"\"");
				return false;
			}
			return true;
		}
	}
	
	private HashMap<String, library> hashLibs = new HashMap<String, AppLoader.library>();
	private final XKernel kernel;
	private final int m_logChannel;
	private final MediaManager mediaMan;
	private final XGUI xgui;
	private HashMap<String, Variable> hashGlobals = new HashMap<String, Variable>();
	private HashMap<String, Class<? extends IScriptable>> hashClasses = new HashMap<String, Class<? extends IScriptable>>();
	
	public AppLoader(XGUI gui) 
	{
		this.xgui = gui;
		this.kernel = xgui.getKernel();
		mediaMan = kernel.getMediaManager();
		m_logChannel = kernel.registerLogChannel("AppLoader");
	}
	
	public XApp loadApp(String url)
	{
		kernel.log(m_logChannel, LogLevel.Default, "Loading application \""+url+"\"...");
		url = mediaMan.getAbsoluteURL(url);
		Script sApp = mediaMan.readScript(url);
		if (sApp == null) {
			kernel.log(m_logChannel, LogLevel.Error, "Error loading app \""+url+"\": could not find or read file.");
			return null;
		}
		sApp.SetExtGlobals(hashGlobals);
		ScriptError err = sApp.Compile(this, this);
		if (err != null) {
			kernel.log(m_logChannel, LogLevel.Error, "Error compiling application " + url + "\n"+err);
			return null;
		}
		
		// register classes:
		ArrayList<ScriptClass> cls = sApp.GetClassList();
		for (ScriptClass c : cls) {
			String name = c.getName();
			Class<? extends IScriptable> clsImp = hashClasses.get(name);
			if (clsImp != null) {
				err = sApp.RegisterClass(name, clsImp, true);
				if (err != null) {
					kernel.log(m_logChannel, LogLevel.Error, "Error registering class " + clsImp.getCanonicalName() + " while loading application \""+url+"\"\n"+err);
					return null;
				}
			}
		}
		
		kernel.log(m_logChannel, LogLevel.Default, "Application loaded successfuly! :-)");
		
		return new XApp(sApp, xgui);
	}

	@Override
	public Script getLibrary(String importURL) {
		importURL = mediaMan.getAbsoluteURL(importURL);
		library lib = hashLibs.get(importURL);
		if (lib == null) {
			lib = new library(importURL);
			if (!lib.load())
				return null;
			hashLibs.put(importURL, lib);
			
			lib.lib.SetExtGlobals(hashGlobals);
			ScriptError err = lib.lib.Compile(this, this);
			if (err != null) {
				kernel.log(m_logChannel, LogLevel.Error, "Error compiling library " + importURL + "\n"+err);
				hashLibs.remove(lib);
				return null;
			}
		}
		if (!lib.lib.IsCompiled()) {
			kernel.log(m_logChannel, LogLevel.Error, "Cyclic reference detected in library " + importURL);
			hashLibs.remove(lib);
			return null;
		}
		return lib.lib;
	}

	/**
	 * registers an external global variable to be used in the scripts :D
	 * @param string
	 * @param var
	 */
	public void addGlobal(String name, Object var, boolean readonly) 
	{
		TypeID type = TypeID.fromClass(var.getClass());
		String clsID = null;
		if (type == TypeID.Object || type == TypeID.ObjectArray)
			clsID = var.getClass().getSimpleName();
		
		Variable v = new Variable(name, type, clsID, var);
		v.readonly = readonly;
		hashGlobals.put(name, v);
	}
	
	public void addClass(Class<? extends IScriptable> cls)
	{
		hashClasses.put(cls.getSimpleName(), cls);
	}

	@Override
	public void handleScriptWarning(ScriptError warn) 
	{
		kernel.log(m_logChannel, LogLevel.Default, "!WARNING! (compiling) "+warn);
	}
}
