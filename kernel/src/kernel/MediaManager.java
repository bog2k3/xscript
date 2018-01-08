package kernel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import wrappers.XMLDocument;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;
import XScripter.Script;


/**
 * Use this class' static methods to retrieve media files.
 * @author Bogdan.Ionita
 *
 */
public class MediaManager extends IScriptable
{
	private static final long serialVersionUID = 0xfa65b98e;
	
	public static enum ResourceState {
		Loaded,
		InProgress,
		Errored,
		Unknown,
	}
	
	public static class WebImage extends IScriptable 
	{
		public static final Integer STATE_LOADED = ResourceState.Loaded.ordinal();
		public static final Integer STATE_INPROGRESS = ResourceState.InProgress.ordinal();
		public static final Integer STATE_ERRORED = ResourceState.Errored.ordinal();
		
		Image img;
		ResourceState state;
		private final WebImageObserver imgObs;
		
		WebImage(Image img, WebImageObserver imgObs) {
			this.img = img;
			state = ResourceState.InProgress;
			this.imgObs = imgObs;
		}
		
		public ResourceState getState() {
			return state;
		}
		
		public Integer getLoadState() {
			return getState().ordinal();
		}
		
		public Image getImage() {
			return img;
		}

		/**
		 * querries the image for its width. if the image is not yet loaded,
		 * the ImageObserver whoRequested will be notified when the image is ready. 
		 * @param whoRequested object to notify when the image loads.
		 * @return
		 */
		public Integer getWidth(ImageObserver whoRequested) 
		{
			if (whoRequested == null)
				whoRequested = imgObs;
			if (state != ResourceState.Errored)
			{
				int w = img.getWidth(whoRequested);
				if (state == ResourceState.Loaded)
					return w;
				else
					return 16;
			} else
				return 16;
		}
		
		public Integer getWidth() {
			return getWidth(null);
		}

		/**
		 * querries the image for its height. if the image is not yet loaded,
		 * the ImageObserver whoRequested will be notified when the image is ready. 
		 * @param whoRequested object to notify when the image loads.
		 * @return
		 */
		public Integer getHeight(ImageObserver whoRequested) 
		{
			if (whoRequested == null)
				whoRequested = imgObs;
			
			if (state != ResourceState.Errored) {
				int h = img.getHeight(whoRequested);
				if (state == ResourceState.Loaded)
					return h;
				else
					return 16;
			} else
				return 16;
		}
		
		public static boolean ValidateInterface(PropertyDesc[] Properties,
				MethodDesc[] Methods, String BaseClassName, boolean isAbstract) {
			return true; //TODO check
		}
	}
	
	class WebImageObserver implements ImageObserver
	{
		@Override
		public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) 
		{
			//XKernel.log("received image update (no owner) for "+img);
			int errFlags = ERROR | ABORT;
			if ((infoflags & errFlags) != 0) {
				kernel.log(mediaManLogChannel, LogLevel.Default, "Error loading image (no owner): "+img);
				setImageState(img, ResourceState.Errored);
				return false;
			}
			if ((infoflags & ALLBITS) == ALLBITS) {
				kernel.log(mediaManLogChannel, LogLevel.Info, "Image loaded (no owner): "+img);
				setImageState(img, ResourceState.Loaded);
				return false;
			}
			
			return true;
		}
	}
	
	private String httpRoot = "";
	public synchronized void setHttpRoot(String url) {
		httpRoot = url;
		if (httpRoot.charAt(httpRoot.length()-1) != '/')
			httpRoot += '/';
	}
	
	private Toolkit tk;
	private HashMap<String, WebImage> hashImages;
	private HashMap<Image, WebImage> hashWebImage;
	private BufferedImage imgPHLoading;
	private BufferedImage imgPHErrored;
	private DocumentBuilder docBuilder;
	private DOMImplementation domImpl;
    Transformer xmlTransformer;
	private boolean initialized = false;
	private XKernel kernel;
	private WebImageObserver imgObs = null;
	
	private int mediaManLogChannel;
	
	MediaManager(XKernel kernel) throws 
		ParserConfigurationException, 
		TransformerConfigurationException, 
		TransformerFactoryConfigurationError 
	{
		this.kernel = kernel;
		
		if (initialized)
			return;
		
		mediaManLogChannel = kernel.registerLogChannel("MEDIAMANAGER");
		kernel.log(mediaManLogChannel, LogLevel.Default, "initializing MediaManager...");
		tk = Toolkit.getDefaultToolkit();
		hashImages = new HashMap<String, WebImage>();
		hashWebImage = new HashMap<Image, MediaManager.WebImage>();
		
		// create the "loading image..." placeholder image:
		imgPHLoading = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) imgPHLoading.getGraphics();
		g.setBackground(Color.black);
		g.clearRect(0, 0, 16, 16);
		g.setColor(Color.yellow);
		g.fillRect(1, 5, 3, 5);
		g.fillRect(6, 5, 3, 5);
		g.fillRect(11, 5, 3, 5);
		
		// create the "errored" placeholder image
		imgPHErrored = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		g = (Graphics2D) imgPHErrored.getGraphics();
		g.setBackground(Color.black);
		g.clearRect(0, 0, 16, 16);
		g.setColor(Color.red);
		g.drawRect(1, 1, 13, 13);
		g.drawLine(1, 1, 14, 14);
		g.drawLine(14, 1, 1, 14);
		
		docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		domImpl = docBuilder.getDOMImplementation();
		xmlTransformer = TransformerFactory.newInstance().newTransformer();
		xmlTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
        xmlTransformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
        xmlTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
		
		imgObs = new WebImageObserver();
		
		initialized = true;
		
		kernel.log(mediaManLogChannel, LogLevel.Default, "MediaManager initialized.");
	}
	
	public Document createDocument()
	{
		return domImpl.createDocument(null, null, null);
	}
	
	public XMLDocument createXMLDocument()
	{
		return new XMLDocument(createDocument());
	}
	
	public Boolean saveXMLDocument(XMLDocument doc, String url)
	{
		doc.prepareForSave();
		if (doc.rootNode == null)
			return false;
		
		// transform the Document into a String
        DOMSource domSource = new DOMSource(doc.doc);
        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult sr = new StreamResult(sw);
        try {
			xmlTransformer.transform(domSource, sr);
		} catch (TransformerException e) {
			kernel.log(mediaManLogChannel, LogLevel.Default, "Failed saving document "+url+"\n"+XKernel.getStackTrace(e));
			return false;
		}
        String xml = sw.toString();
        
        return kernel.saveTextFile(xml, url);
	}
	
	/**
	 * Request an image from a given url. if the url is relative, the default
	 * http root is added. The function returns immediately, but the image is loaded
	 * asynchronously in the background. 
	 */
	public synchronized WebImage requestImage(String url) {
		return requestImage(url, null);
	}
	
	/**
	 * Request an image from a given url. if the url is relative, the default
	 * http root is added. The function returns immediately, but the image is loaded
	 * asynchronously in the background. when the image is completely loaded,
	 * a repaint request is sent to the VisualComponent associated with this image.
	 * @param url
	 * @param customer VisualComponent to be notified (repainted) when the image is loaded
	 * @return
	 */
	public synchronized WebImage requestImage(String url, ImageObserver customer) 
	{
		kernel.log(mediaManLogChannel, LogLevel.Info, "requested image "+url+"...");
		URL u;
		try {
			url = getAbsoluteURL(url);
			u = new URL(url);
		} catch (MalformedURLException e) {
			kernel.log(mediaManLogChannel, LogLevel.Error, "Error parsing image URL :" + url + "\n"+XKernel.getStackTrace(e));
			return null;
		}
		kernel.log(mediaManLogChannel, LogLevel.Info, "URL resolved to "+url);
		if (hashImages.containsKey(url)) {
			kernel.log(mediaManLogChannel, LogLevel.Info, "Image found in database.");
			return hashImages.get(url);
		}
		
		WebImage img = new WebImage(tk.createImage(u), imgObs);
		tk.prepareImage(img.img, 1, 1, customer != null ? customer : imgObs);
		hashImages.put(url, img);
		hashWebImage.put(img.img, img);
		kernel.log(mediaManLogChannel, LogLevel.Debug, "started loading image "+img.img+"...");
		return img;
	}
	
	/**
	 * Loads and parses an xml file.
	 * @param url location of the xml document
	 * @return Returns a Document in case of success, or null if an error occurs.
	 */
	public XMLDocument parseXML(String url) {
		kernel.log(mediaManLogChannel, LogLevel.Info, "loading document "+url);
		url = getAbsoluteURL(url);
		kernel.log(mediaManLogChannel, LogLevel.Info, "URL resolved to "+url);
		try {
			Document d = docBuilder.parse(url);
			d.normalizeDocument();
			kernel.log(mediaManLogChannel, LogLevel.Info, "document loaded successfully.");
			return new XMLDocument(d);
		} catch (Exception e) {
			kernel.log(mediaManLogChannel, LogLevel.Error, "failed loaing document\n"+XKernel.getStackTrace(e));
			return null;
		}
	}
	
	
	/** checks and returns the state of an image */
	public synchronized ResourceState getImageState(Image img)
	{
		if (hashWebImage.containsKey(img))
			return hashWebImage.get(img).state;
		else
			return ResourceState.Unknown;
	}
	
	/** modifies the state of an image. the caller is responsible for 
	 * keeping the state synchronized with the image, and is expected to 
	 * only modify the state flag if a state change has occured on the image.
	 * @param img
	 * @param state
	 */
	public synchronized void setImageState(Image img, ResourceState state)
	{
		if (hashWebImage.containsKey(img)) {
			WebImage w = hashWebImage.get(img);
			w.state = state;
			kernel.log(mediaManLogChannel, LogLevel.Debug, "image state for "+img+" set to "+state);
		}
	}

	/** returns the absolute url. if the passed url is already absolute, it is 
	 * returned unaltered.
	 * if it is a relative url to the default root path, these two parts are
	 * combined and a new url is returned.
	 * @param url
	 * @return
	 * @author bogdan.ionita
	 */
	public String getAbsoluteURL(String url) {
		//Replace all spaces with %20
		url = url.replaceAll(" ", "%20");
		if (!url.contains("http://") && !url.contains("www.") && !url.contains("ftp."))
			return httpRoot + url;
		else
			return url;
	}
	
	/** returns a 16x16 "loading" placeholder image to be used for images that 
	 * have not yet finished loading
	 * @return
	 */
	public synchronized Image getLoadingIcon() {
		return imgPHLoading;
	}
	
	/** returns a 16x16 "errored" placeholder image to be used for images that 
	 * have failed loading
	 * @return
	 */
	public synchronized Image getErroredIcon() {
		return imgPHErrored;
	}
	
	private int avoidCheckRecursion = 0;
	
	public boolean checkAllImagesLoaded(ImageObserver caller, WebImage... images)
	{
		avoidCheckRecursion++;
		boolean incomplete = false;
		for (int i=0; i<images.length; i++) {
			if (images[i] == null)
				incomplete = true;
			else
				if (images[i].getState() == ResourceState.InProgress) {
					incomplete = true;
					tk.prepareImage(images[i].getImage(), 1, 1, avoidCheckRecursion == 1 ? caller : null);
				}
		}
		avoidCheckRecursion--;
		return !incomplete;
	}

	/**
	 * reads a text document from the specified URL and returns the string representation
	 * of the document.
	 * Returns null if document failed to load.
	 * @param url_addr
	 * @return
	 */
	public String readTextDoc(String url_addr) 
	{
		url_addr = getAbsoluteURL(url_addr);
		try {
			URL url = new URL(url_addr);
			InputStream is = url.openStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			StringBuffer sBuf = new StringBuffer() ;
			String line;
			while ( (line = br.readLine()) != null)
				sBuf.append (line + "\n");

			is.close();
			return sBuf.toString();
		} catch (Exception e) {
			kernel.log(mediaManLogChannel, LogLevel.Error, "failed loaing document \""+url_addr+"\"\n"+XKernel.getStackTrace(e));
			return null;
		}
	}

	/**
	 * reads and returns a script from the given URL, assuming the url points to the
	 * source of the script (in text form).
	 * @param address
	 * @return
	 */
	public Script readScript(String address) 
	{
		String src = readTextDoc(address);
		if (src == null)
			return null;
		
		Script script = new Script();
		if (script.LoadFromString(src)) {
			script.FileName = address;
			return script;
		} else
			return null;
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties,
			MethodDesc[] Methods, String BaseClassName, boolean isAbstract) {
		return true; //TODO check
	}
}
