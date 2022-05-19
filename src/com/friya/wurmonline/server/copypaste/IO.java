package com.friya.wurmonline.server.copypaste;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class IO
{
	private static Logger logger = Logger.getLogger(IO.class.getName());
	
	static private final String saveDir = System.getProperty("user.dir") + File.separator + "Friyas-Clipboard";


	static private boolean isSecure(String fileName)
	{
		String normalizedSaveDir = Paths.get(saveDir).normalize().toString();
		String pathAndFileName = getNormalizedFilePath(fileName);

		if(pathAndFileName.startsWith(normalizedSaveDir)) {
			return true;
		} else {
			return false;
		}
	}


	static private String getNormalizedFilePath(String fileName)
	{
		return Paths.get(saveDir + File.separator + fileName).normalize().toString();
	}


	static boolean exists(String fileName)
	{
		if(fileName.endsWith(".friyaclip") == false) {
			fileName += ".friyaclip";
		}

		if(!isSecure(fileName)) {
			return false;
		}
		
		if((new File(getNormalizedFilePath(fileName)).exists()) == false) {
			return false;
		}
		
		return true;
	}
	
	static String[] dir()
	{
		ArrayList<String> tmp = new ArrayList<String>();
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(saveDir), "*.{friyaclip}")) {
		    for (Path entry: stream) {
		    	tmp.add(entry.getFileName().toString());
		    }
		} catch (IOException x) {
		    System.err.println(x);
		    return null;
		}
		
		return tmp.toArray(new String[tmp.size()]);
	}


	static boolean save(Buffer b)
	{
		if((new File(saveDir).exists()) == false) {
			if(!(new File(saveDir).mkdir())) {
				logger.log(Level.SEVERE, "Failed to create folder " + saveDir);
				return false;
			}
		}
		
		if(b.name.endsWith(".friyaclip")) {
			b.name = b.name.substring(0, b.name.length() - ".friyaclip".length());
		}
		
		String safeFileName = b.name.replaceAll("[^A-Za-z0-9]", "_");
		
		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(
					new FileOutputStream(saveDir + File.separator + b.owner + "-" + safeFileName  + "-" + System.currentTimeMillis() + ".friyaclip"), "utf-8")
				)
			) {
			writer.write(b.area);

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to save buffer", e);
			return false;
		}

		return true;
	}

	
	static boolean saveRaw(String fn, String s)
	{
		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(
					new FileOutputStream(saveDir + File.separator + fn + ".friyaclip"), "utf-8")
				)
			) {
			writer.write(s);

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to save string", e);
			return false;
		}

		return true;
	}

	/**
	 * Load a 'remote' file into specified copy buffer
	 * 
	 * @param owner
	 * @param fileName
	 * @param toBuffer
	 * @return
	 */
	static boolean load(String owner, String fileName, int toBuffer)
	{
		if(fileName.endsWith(".friyaclip") == false) {
			fileName += ".friyaclip";
		}

		if(!isSecure(fileName)) {
			return false;
		}

		File file = new File(getNormalizedFilePath(fileName));
		
		StringBuffer sb = new StringBuffer();

		try (FileInputStream fis = new FileInputStream(file)) {
			int content;
			
			while ((content = fis.read()) != -1) {
				sb.append((char)content);
			}

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to load into buffer", e);
			return false;
		}
		
		if(sb.length() == 0) {
			return false;
		}

		CopyBuffers.getInstance().setJsonToBuffer(owner, fileName, sb, toBuffer);

		return true;
	}
}
