package net.oegupm.wicus.isa.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConfigValues {
	
	public static String configPath ="";
	private static final String fileErrorMsg = "CONFIG FILE ERROR: we couldn't open the config file at ";
	
	ArrayList<String> cloudProviders;
	public Map<String,String> cloudProvidersNames;
	public Map<String,String> keys;
	private  final String currentDir = "/Users/isantana/Dropbox/DOCTORADO/DBXworkspace/workspace/WicusInfrastructureGenerator/";
	
	
	public ConfigValues(String path)
	{
		
		
		configPath = currentDir+path;		
		System.out.println("CV ISA path:" + configPath);

		//load the default values of the config simple properties
		keys = new HashMap<String, String>();
		cloudProvidersNames = new HashMap<String, String>();
		cloudProviders = new ArrayList<String>();
		keys.put("WF_URI","");
		keys.put("WFI_DATASET_PATH","");
		keys.put("SWC_DATASET_PATH","");
		keys.put("SVA_DATASET_PATH","");
		keys.put("WF_EXEC_FILE_PRECIP","");
		keys.put("WF_EXEC_FILE_VAGRANT","");
		keys.put("CLOUD_PROV_FILE","");
		keys.put("OUT_PRECIP_FILE","false");	
		keys.put("OUT_VAGRANT_FILE","false");	
		keys.put("WICUS_USER","");	
		keys.put("SSH_TYPE","");
		keys.put("AVOID_STACK_DUPLICATED","true");

	}

	public void readConfigValues()
	{
		//open the file and read it line by line
		try {
			
			
			FileInputStream fstream = new FileInputStream(configPath);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) 
			{
				//strLine=strLine.replaceAll("\\s+","");				
				//dbg(strLine);
				processLine(strLine, br);
			}


			//load the available providers from the cloud providers file
			cloudProviders = loadCloudProviders();
			
			// Close the input stream
			in.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println(fileErrorMsg + configPath + "\n" + e.getMessage());
			System.err.println("Stack Trace: " + e.getStackTrace().toString());
		}
		
	}
	
	private ArrayList<String> loadCloudProviders() {
		
		ArrayList<String> res = new ArrayList<String>();
		
		//read the providers file line by line (each line represents a provider)
		try {
			
			FileInputStream fstream = new FileInputStream(keys.get("CLOUD_PROV_FILE"));
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) 
			{
				int hashIndex = strLine.indexOf(";"); 
				String provUri = strLine.substring(0,hashIndex);
				String provName = strLine.substring(hashIndex+1);
				cloudProvidersNames.put(provUri, provName);
				
				res.add(provUri);
			}
			
			
			// Close the input stream
			in.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println(fileErrorMsg + configPath + "\n" + e.getMessage());
		}
		
		return res;
	}

	private void processLine(String strLine, BufferedReader br) throws IOException {
		if((strLine.startsWith("#"))||(strLine.equals("")))
			return; //skip comment and empty lines
				
		String k=strLine.substring(0, strLine.indexOf("="));
		if(keys.containsKey(k))
		{
			String v = strLine.substring(strLine.indexOf("=")+1,strLine.indexOf(";"));
			if((k.equals("WF_EXEC_FILE_PRECIP"))||(k.equals("WF_EXEC_FILE_VAGRANT"))||(k.equals("CLOUD_PROV_FILE")))
			{
					v = currentDir + v;
			}
			keys.put(k, v);
		}
		
	}


	public String value(String key)
	{
		if(keys.containsKey(key))
			return keys.get(key);
		else
			return "CONFIG_VALUE_NOT_FOUND("+key+")";
	}
	
	public ArrayList<String> getCloudProviders()
	{
		return cloudProviders;
	}
	
	public String toString()
	{
		String res = "Configuration Values:\n";
		res += "-------------------------------\n";
		Iterator it = keys.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry kv = (Map.Entry)it.next();
	        res+=kv.getKey()+":["+kv.getValue()+"]\n";
	    }
		
	    res+="CloudProviders:" + cloudProviders.toString() + "\n";
		res += "-------------------------------\n";
	    
		return res;
	}
	
	public String getCloudProviderName(String uri)
	{
		return cloudProvidersNames.get(uri);
	}

}
