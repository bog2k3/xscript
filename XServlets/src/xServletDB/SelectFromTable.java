package xServletDB;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SelectFromTable
 */
public class SelectFromTable extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SelectFromTable() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();		
		try {
			// TODO citit din requestul servletului numele tabelei
			// coloanele pe care tre sa le interoghez
			// trebuie gandit cum returnez informatia
			// poate fac un servlet care returneaza datele de conectare la baza de date sau nu?? 
			
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/rca_ieftin","root","admin");
			statement = connect.createStatement();
			
			resultSet = statement.executeQuery("select * from RCA_USERS");
			

			
			while (resultSet.next())
			{
				out.println(resultSet.getInt("idUSERS")+" "+resultSet.getString("UserName")+" "+resultSet.getString("UserEmail"));
			}
		} catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		} finally {
			close();
		}
		out.println("gigi");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	private void close()
	{
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}

}
