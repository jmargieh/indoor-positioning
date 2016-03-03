package main.java.com.indoor.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties({"content"})
public class FileMeta {
	
	

	private String fileName;
	private String fileSize;
	private String fileType;
	private String twitter;
	private String jsonContent;
	
	private InputStream content;
	
	
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileSize() {
		return fileSize;
	}
	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public InputStream getContent(){
		return this.content;
	}
	public void setContent(InputStream content){
		this.content = content;
	}
	public String getTwitter(){
		return this.twitter;
	}
	public void setTwitter(String twitter){
		this.twitter = twitter;
	}
	
	public String getJsonContentFromInputStream(){
		return this.jsonContent;
	}
	
	// convert InputStream to String
	public void setJsonContentFromInputStream() {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(this.getContent()));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		this.jsonContent = sb.toString();

	}
	
	@Override
	public String toString() {
		return "FileMeta [fileName=" + fileName + ", fileSize=" + fileSize
				+ ", fileType=" + fileType + "]";
	}
	
	
	
}

