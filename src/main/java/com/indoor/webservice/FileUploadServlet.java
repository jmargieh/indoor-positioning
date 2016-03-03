package main.java.com.indoor.webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.java.com.indoor.helpers.FileMeta;
import main.java.com.indoor.helpers.MultipartRequestHandler;

//this to be used with Java Servlet 3.0 API
@MultipartConfig 
public class FileUploadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	// this will store uploaded files
	private static List<FileMeta> files = new LinkedList<FileMeta>();
	/***************************************************
	 * URL: /upload
	 * doPost(): upload the files and other parameters
	 ****************************************************/
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException{
	    
		// 1. Upload File Using Java Servlet API
		//files.addAll(MultipartRequestHandler.uploadByJavaServletAPI(request));			
		
		// 1. Upload File Using Apache FileUpload
		files.addAll(MultipartRequestHandler.uploadByApacheFileUpload(request));
		
		// Remove some files
		while(files.size() > 20)
		{
			files.remove(0);
		}
		
		// 2. Set response type to json
		response.setContentType("application/json");
		
		// 3. Convert List<FileMeta> into JSON format
    	ObjectMapper mapper = new ObjectMapper();
    	
    	// 4. Send result to client
    	mapper.writeValue(response.getOutputStream(), files);
	
	}
	/***************************************************
	 * URL: /upload?f=value
	 * URL: /upload?getfiles=true
	 * URL: /upload?filename=value
	 * doGet(): get file of index "f" from List<FileMeta> as an attachment
	 * doGet(): get all files if getfiles=true
	 * doGet(): get file by value from List<FileMeta> as an outputstream
	 ****************************************************/
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException{
		
		 // 1. Get f from URL upload?f="?"
		 String value = request.getParameter("f");
		 String getfiles = request.getParameter("getfiles");
		 String fileName = request.getParameter("filename");
		 

		 if(getfiles != null && getfiles.compareTo("true") == 0){
			// 2. Set response type to json
				response.setContentType("application/json");
				
				// 3. Convert List<FileMeta> into JSON format
		    	ObjectMapper mapper = new ObjectMapper();
		    	
		    	// 4. Send result to client
		    	mapper.writeValue(response.getOutputStream(), files);
		 }
		 else if (fileName != null){
			 
			 	FileMeta getFile = getFileContentByName(fileName);
			 	if( getFile != null) {
				 	response.setContentType("application/json; charset=UTF-8");
				 	PrintWriter out = response.getWriter();
			        	        
			        String s = getFile.getJsonContentFromInputStream();
			        
			        out.println(s);
			        out.close();	
			 	}
		   
		 }
		 else {
		 // 2. Get the file of index "f" from the list "files"
		 FileMeta getFile = files.get(Integer.parseInt(value));
		 
		 try {		
			 	// 3. Set the response content type = file content type 
			 	response.setContentType(getFile.getFileType());
			 	
			 	// 4. Set header Content-disposition
			 	response.setHeader("Content-disposition", "attachment; filename=\""+getFile.getFileName()+"\"");
			 	
			 	// 5. Copy file inputstream to response outputstream
		        InputStream input = getFile.getContent();
		        OutputStream output = response.getOutputStream();
		        byte[] buffer = new byte[1024*10];
		        
		        for (int length = 0; (length = input.read(buffer)) > 0;) {
		            output.write(buffer, 0, length);
		        }
		        
		        output.close();
		        input.close();
		 }catch (IOException e) {
				e.printStackTrace();
		 }
		 }
	}
	
	private FileMeta getFileContentByName(String filename){

		ListIterator<FileMeta> listIterator = files.listIterator();
		while (listIterator.hasNext()) {
        	FileMeta file = listIterator.next();
        	if(file.getFileName().compareTo(filename) == 0){
        		return file;
            	
        	}
        }
		
		FileMeta file = null;
		return file;
			}
	

	
}
