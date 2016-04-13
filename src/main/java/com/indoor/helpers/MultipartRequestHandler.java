package main.java.com.indoor.helpers;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.FileItem;


public class MultipartRequestHandler {
	
	public static List<FileMeta> uploadByApacheFileUpload(List<FileItem> items) throws IOException, ServletException{
				
		List<FileMeta> files = new LinkedList<FileMeta>();

		FileMeta temp = null;
			// 2.2 Parse the request
			try {
				String twitter = "";
				
				// 2.4 Go over each FileItem
				for(FileItem item:items){
					
					// 2.5 if FileItem is not of type "file"
				    if (item.isFormField()) {

				    	// 2.6 Search for "twitter" parameter
				        if(item.getFieldName().equals("twitter"))
				        	twitter = item.getString();
				        
				    } else {
				       
				    	// 2.7 Create FileMeta object
				    	temp = new FileMeta();
						temp.setFileName(item.getName());
						temp.setContent(item.getInputStream());
						temp.setFileType(item.getContentType());
						temp.setFileSize(item.getSize()/1024+ "Kb");
						//temp.setJsonContentFromInputStream();
						
				    	// 2.7 Add created FileMeta object to List<FileMeta> files
						files.add(temp);
				       
				    }
				}
				
				// 2.8 Set "twitter" parameter 
				for(FileMeta fm:files){
					fm.setTwitter(twitter);
				}
				
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		return files;
	}
}

