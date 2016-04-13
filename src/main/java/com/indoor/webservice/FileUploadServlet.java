package main.java.com.indoor.webservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.java.com.indoor.helpers.FileMeta;
import main.java.com.indoor.helpers.MultipartRequestHandler;

//this to be used with Java Servlet 3.0 API
@MultipartConfig 
public class FileUploadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    
	// location to store file uploaded
    private static final String UPLOAD_DIRECTORY = "upload";
 
    // upload settings
    private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB
	
	
	// this will store uploaded files
	private static List<FileMeta> files = new LinkedList<FileMeta>();
	/***************************************************
	 * URL: /upload
	 * doPost(): upload the files and other parameters
	 ****************************************************/
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException{
	    
		 // checks if the request actually contains upload file
        if (!ServletFileUpload.isMultipartContent(request)) {
            // if not, we stop here
            PrintWriter writer = response.getWriter();
            writer.println("Error: Form must has enctype=multipart/form-data.");
            writer.flush();
            return;
        }
 
        // configures upload settings
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // sets memory threshold - beyond which files are stored in disk
        factory.setSizeThreshold(MEMORY_THRESHOLD);
        // sets temporary location to store files
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
 
        ServletFileUpload upload = new ServletFileUpload(factory);
         
        // sets maximum size of upload file
        upload.setFileSizeMax(MAX_FILE_SIZE);
         
        // sets maximum size of request (include file + form data)
        upload.setSizeMax(MAX_REQUEST_SIZE);
 
        // constructs the directory path to store upload file
        // this path is relative to application's directory
        String uploadPath = getServletContext().getRealPath("/")
                + File.separator + UPLOAD_DIRECTORY;
        // creates the directory if it does not exist
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
 
        try {
            // parses the request's content to extract file data
            @SuppressWarnings("unchecked")
            List<FileItem> formItems = upload.parseRequest(request);
 
            if (formItems != null && formItems.size() > 0) {
                // iterates over form's fields
                for (FileItem item : formItems) {
                    // processes only fields that are not form fields
                    if (!item.isFormField()) {
                        String fileName = new File(item.getName()).getName();
                        String filePath = uploadPath + File.separator + fileName;
                        File storeFile = new File(filePath);
 
                        // saves the file on disk
                        item.write(storeFile);
                        request.setAttribute("message",
                            "Upload has been done successfully!");
                    }
                }
            }
            
    		// 1. update files List<FileMeta> File Using Apache FileUpload
    		files.addAll(MultipartRequestHandler.uploadByApacheFileUpload(formItems));
    		
    		// Remove some files if more than 20
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
            
            
        } catch (Exception ex) {
            request.setAttribute("message",
                    "There was an error: " + ex.getMessage());
        }
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
		 //String fileName = request.getParameter("filename");
		 

		 if(getfiles != null && getfiles.compareTo("true") == 0){
			// 2. Set response type to json
				response.setContentType("application/json");
				
				// 3. Convert List<FileMeta> into JSON format
		    	ObjectMapper mapper = new ObjectMapper();
		    	
		    	// 4. Send result to client
		    	mapper.writeValue(response.getOutputStream(), files);
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
	/*
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
*/	

	
}
