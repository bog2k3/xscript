package xServlet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetFile
 * @author dan
 * 
 */
public class GetFile extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
 
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetFile() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		PrintWriter out = response.getWriter();
		
		out.println("Access denied");
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String ROOT = getInitParameter("ROOT");
		String AdminPassword = getInitParameter("NULL");
		
		String requestFile = ROOT.concat(request.getParameter("file"));
		String requestUsr = request.getParameter("usr");
		String requestPass = request.getParameter("pass");
		
		OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
        
        writer.write("gigi");
        writer.flush();
        writer.close();
		
	}
	
	private static byte[] computeHash(String x) throws Exception {
		
		java.security.MessageDigest d =null;
		
		d = java.security.MessageDigest.getInstance("SHA-1");
		d.reset();
		d.update(x.getBytes());
		
		return  d.digest();
	}
	
	private static String byteArrayToHexString(byte[] b) {
		
		StringBuffer sb = new StringBuffer(b.length * 2);
		
	    for (int i = 0; i < b.length; i++) {
	    	
	    	int v = b[i] & 0xff;
	    	
	    	if (v < 16) {
	    		sb.append('0');
	    	}
	    	
	    sb.append(Integer.toHexString(v));
	    }
	    
	    return sb.toString().toUpperCase();
	}

}
