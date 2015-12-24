package net.oegupm.wicus.isa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.oegupm.wicus.isa.queries.QueryExecute;
import net.oegupm.wicus.isa.queries.QueryStrings;
import net.oegupm.wicus.isa.utils.Digraph;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class VagrantSpecGenerator 
{
	

	private static OntModel wfiModel;
	private static OntModel swcModel;
	private static OntModel svaModel;
	


	private static String VAGRANT_USER ="root";
	private static String WICUS_USER ="vagrant";


	private static String DEST_COMMAND_PATH = "";
	private static CharSequence SSH_TYPE;
	
	private static List<String> installedStacks;
	private static boolean AVOID_STACK_DUPLICATED;
	private static String wfUri;  
	
	public VagrantSpecGenerator(String wfUri, OntModel wfiM, OntModel swcM, OntModel svaM, String wu, String ssht, boolean dup)
	{
		this.wfUri = wfUri;
		
		this.wfiModel = wfiM;
		this.swcModel = swcM;
		this.svaModel = svaM;
		

		this.SSH_TYPE=ssht;
		
		//if there is no user specified, we use de default one
		if(!wu.equals(""))
			this.WICUS_USER=wu;
		
		this.DEST_COMMAND_PATH = "/home/"+WICUS_USER+"/"; 

		this.installedStacks = new ArrayList<String>();
		AVOID_STACK_DUPLICATED = dup;
	}
	
//	public String mergeMaxSVAandGenerateInfConfigurationVagrant(Map<String, String> reqMaxSvaList,Map<String, Digraph<String>> deployReqDg, ArrayList<String> providersList, String wfExecPath) 
//	{
//		
//		String conf = "";
//		
//		HashMap<String, HashMap<String, ArrayList<String>>> mergedSvas = mergeMaxSVA(reqMaxSvaList);
//		
//		conf = generateVagrantConf(mergedSvas, deployReqDg, providersList, wfExecPath);
//		
//
//		conf.replace("@@USER@@", WICUS_USER);
//	
//		return conf;
//		
//	}
	
	public String mergeMaxSVAandGenerateInfConfigurationPropagatedVagrant(Map<String, String> wfMaxSvaList,Map<String, LinkedHashMap<String, Digraph<String>>> deployWfDg,Map<String, Digraph<String>> deployReqDg, ArrayList<String> providersList, String wfExecPath) 
	{
		String conf = "";
		
		//merge SVAs
		//TODO: get map<SVA,map<SVA_ID,linkedhashset<REQ>>> (or this?)
		HashMap<String, HashMap<String, LinkedHashSet<String>>> mergedPropagatedSvas = mergeWfMaxSVA(wfMaxSvaList,deployWfDg);
		

	     System.out.println("*********** mergedPropagatedSvas **********");
	     System.out.println("* " + printMapKeys(mergedPropagatedSvas));
	     System.out.println("***************************************\n");
		
		//TODO: modify this method or use the same as above
		conf = generateVagrantPropagatedConf(mergedPropagatedSvas, deployWfDg,deployReqDg, providersList, wfExecPath);	
		
		

		conf = conf.replace("@@USER@@", WICUS_USER);

		return conf;
	}
	


	private String printMergedPropagatedSvas(HashMap<String, HashMap<String, LinkedHashSet<String>>> mergedPropagatedSvas, Map<String, Digraph<String>> deployReqDg) {
		String res="";
		Iterator<Entry<String, HashMap<String, LinkedHashSet<String>>>> it = mergedPropagatedSvas.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Entry<String, HashMap<String, LinkedHashSet<String>>> pairs = it.next();
	    	String string1 = pairs.getKey().toString();
	    	Map<String, LinkedHashSet<String>> map2 = pairs.getValue();
	    	

	    	res+="\n";
	    	res+="* Key: " + string1 + "\n";
	    	res+="* List:" + "\n";
	    	
	    	Iterator<Entry<String, LinkedHashSet<String>>> it2 = map2.entrySet().iterator();
		    while (it2.hasNext()) 
		    {
		    	Entry<String, LinkedHashSet<String>> pairs2 = it2.next();
		    	String string2 = pairs2.getKey().toString();
		    	HashSet<String> list = pairs2.getValue();
		    	res+="*\tKEY:" + string2.toString() + "\n";
		    	res+="*\tVALUES:\n";
		    	for(String s : list)
		    	{
		    		res+="*\t-" + s.toString() + "\n";
//		    		res+="*\t\t" +deployReqDg.get(s) + "\n";
		    	}   		
		    }
		
	    	res+="* ----------------------------------------------------\n";
	    	
	    }		
		return res;
	}

	private String generateVagrantPropagatedConf(HashMap<String, HashMap<String, LinkedHashSet<String>>> mergedPropagatedSvas,	Map<String, LinkedHashMap<String, Digraph<String>>> deployWfDg, Map<String, Digraph<String>> deployReqDg, ArrayList<String> providersList, String wfExecPath) 
	{
		String conf = "# -*- mode: ruby -*-\n" +
					"# vi: set ft=ruby :\n\n" +
					"# All Vagrant configuration is done below. The \"2\" in Vagrant.configure\n" +
					"# configures the configuration version (we support older styles for\n" +
					"# backwards compatibility). Please don't change it unless you know what\n" +
					"# you're doing.\n";
				
		
		Iterator<Entry<String, HashMap<String, LinkedHashSet<String>>>> svaIt = mergedPropagatedSvas.entrySet().iterator();
		while(svaIt.hasNext())
		{
			Entry<String, HashMap<String, LinkedHashSet<String>>> svaIdPair = svaIt.next();
			String sva = svaIdPair.getKey();

			//System.out.println("SVA="+sva);
			
			
			HashMap<String, LinkedHashSet<String>> svaIdMap = svaIdPair.getValue();
			Iterator<Entry<String, LinkedHashSet<String>>> svaIdIt = svaIdMap.entrySet().iterator();

			
			while(svaIdIt.hasNext())
			{
				Entry<String, LinkedHashSet<String>> wfPair = svaIdIt.next();
				
				
				//for each svaID we need to generate an SVA
				conf += getSvaVagrantInfo(sva,providersList);
				
				

				conf += generateUserSetup(WICUS_USER);
				
				//we traverse all the WFs
				HashSet<String> reqList = wfPair.getValue();
				for(String req : reqList)
				{
			    	conf += getReqVagrantInfo(req,deployReqDg);
				}
				
				conf += generateUserGiveaway(WICUS_USER);

				
				try {
					conf += readWfExecFile(wfExecPath,WICUS_USER) + "\n";
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		
		
		conf+="end\n";
		
		return conf;
	}

	private static String generateUserSetup(String user)
	{
		String res = "\t# USER SETUP\n"+
				"\t# executing the user_setup.sh script\n\n"+
				"\t#REMOVE?: config.vm.provision :shell, path: \"user_setup.sh\", args: [\"-USER_NAME\", \"%USER%\", \"-SSH_KEYS_PATH\", \"~\\ \"]\n\n" +
				"\t#set hostnname properly\n"+
				"\tconfig.vm.provision :shell, path: \"config_hostname.sh\"\n\n";

	    res = res.replace("%USER%", user);
		
		return res;
	}
	


	private static String generateUserGiveaway( String user) 
	{
		String res =  "\n\t# USER SETUP\n"+
				"\t# Give away %VAGRANT_USER%'s kingdom to %USER%\n"+
				"\tconfig.vm.provision \"shell\", inline: \"chown -R %USER%: /home/%USER%/\"\n" + 
				"\t# SSH localhost setup in case the %USER% needs it\n"+
				"\tconfig.vm.provision \"shell\", inline: \"ssh-keygen -f ~/.ssh/id_%SSH_TYPE% -t %SSH_TYPE% -N ''\", privileged: false\n"+
				"\tconfig.vm.provision \"shell\", inline: \"cat ~/.ssh/id_%SSH_TYPE%.pub >> ~/.ssh/authorized_keys\", privileged: false\n"+
				"\tconfig.vm.provision \"shell\", inline: \"chmod og-wx ~/.ssh/authorized_keys\", privileged: false\n\n\n";

	    res = res.replace("%VAGRANT_USER%", VAGRANT_USER);
	    res = res.replace("%USER%", user);
	    res = res.replace("%SSH_TYPE%", SSH_TYPE);
		
		return res;
	}



	//this method merges requirements into SVAs that are equal if those requirements are compatible.
	//it returns a map<SVA,map<SVA_ID,hashset<REQ>>>
	//SVA is the base appliance, where SVA_ID is an instantiation of the appliance for a set of requirementes
	//this is like this because it could be the case that 2 WF are associated to the same base SVA,
	//but they are incompatible, so we need two instantiate 2 different SVA for them
	private HashMap<String, HashMap<String, LinkedHashSet<String>>> mergeWfMaxSVA(Map<String, String> wfMaxSvaList, Map<String, LinkedHashMap<String, Digraph<String>>> deployWfDg) 
	{
				
		HashMap<String,HashMap<String,LinkedHashSet<String>>> res = new HashMap<String,HashMap<String,LinkedHashSet<String>>>();
		
		Iterator<Entry<String, String>> wfIt = wfMaxSvaList.entrySet().iterator();
		while(wfIt.hasNext())
		{
			Entry<String, String> pair = wfIt.next();
			String wf = pair.getKey();
			String sva = pair.getValue();
			
			if(res.containsKey(sva))
			{
				//we iterate all the svaIds for an SVA
				HashMap<String, LinkedHashSet<String>> svaMap = res.get(sva);
				Iterator<Entry<String, LinkedHashSet<String>>> svaIter = svaMap.entrySet().iterator();
				while (svaIter.hasNext())
				{
					Entry<String, LinkedHashSet<String>> svaIdPair = svaIter.next();
					String svaId = svaIdPair.getKey();
					HashSet<String> svaIdReqs = svaIdPair.getValue();
					
					if(checkWfCompatibility(wf,svaIdReqs,deployWfDg))
					{
						//if the wfs is compatible with the others
						//we add its requirements (no duplicates) to the list of the sva
						Set<String> l = deployWfDg.get(wf).keySet();
						System.out.println("--------ADDING:" + l);
						res.get(sva).get(svaId).addAll(l);
					}
					else
					{
						HashMap<String, LinkedHashSet<String>> map = res.get(sva);
						
						//we generate a new identifier for the sva
						int size = map.entrySet().size();
						String newSvaId = sva+size;
						map.put(newSvaId, new LinkedHashSet<String>());

						//we add its requirements (no duplicates) to the list of the sva
						Set<String> l = deployWfDg.get(wf).keySet();
						map.get(newSvaId).addAll(l);
					}
					
				}
				
			}
			else
			{
				res.put(sva, new HashMap<String,LinkedHashSet<String>>());
				HashMap<String, LinkedHashSet<String>> map = res.get(sva);
				
				//we generate a new identifier for the sva
				int size = map.entrySet().size();
				String newSvaId = sva+size;
				map.put(newSvaId, new LinkedHashSet<String>());
				
				//we add its requirements (no duplicates) to the list of the sva
				Set<String> l = deployWfDg.get(wf).keySet();

				System.out.println("--------ADDING:" + l);
				map.get(newSvaId).addAll(l);
				
				//at this point we have
				//<SVA,<SVA0,<REQS>>>
				
			}
			
		}
		
		return res;
	}

//	private static String generateVagrantConf( HashMap<String, HashMap<String, ArrayList<String>>> mergedSvas, Map<String, Digraph<String>> deployReqDg, ArrayList<String> providersList, String wfExecPath) 
//	{
//		String conf = "# -*- mode: ruby -*-\n" +
//				"# vi: set ft=ruby :\n\n" +
//				"# All Vagrant configuration is done below. The \"2\" in Vagrant.configure\n" +
//				"# configures the configuration version (we support older styles for\n" +
//				"# backwards compatibility). Please don't change it unless you know what\n" +
//				"# you're doing.\n";
//		
//		
//		Iterator<Entry<String, HashMap<String, ArrayList<String>>>> svaIt = mergedSvas.entrySet().iterator();
//		while(svaIt.hasNext())
//		{
//			Entry<String, HashMap<String, ArrayList<String>>> svaIdPair = svaIt.next();
//			String sva = svaIdPair.getKey();
//			
//			//System.out.println("SVA="+sva);
//			
//			
//			HashMap<String, ArrayList<String>> svaIdMap = svaIdPair.getValue();
//			Iterator<Entry<String, ArrayList<String>>> svaIdIt = svaIdMap.entrySet().iterator();
//			int idCounter = 0;
//			while(svaIdIt.hasNext())
//			{
//				Entry<String, ArrayList<String>> reqsPair = svaIdIt.next();
//				
//				String instId = INT_BASE_ID +idCounter;
//				idCounter++;
//				
//				//for each svaID we need to generate an SVA
//				conf += getSvaVagrantInfo(sva,instId, providersList);
//				
//				
//				conf+="    # Wait for all instances to boot and become accessible. The provision\n" +
//					  "    # method only starts the provisioning, and can be used to start a large\n" +
//					  "    # number of instances at the same time. The wait method provides a\n" +
//					  "    # barrier to when it is safe to start the actual experiment.\n";
//
//				conf+="    exp.wait()\n\n";
//				
//
//				conf += generateUserSetup(instId,WICUS_USER);
//				
//				ArrayList<String> reqList = reqsPair.getValue();
//				for(String req : reqList)
//				{
//					//System.out.println("----"+r);
//			    	conf += getReqVagrantInfo(req,deployReqDg,instId);
//				}
//				
//				conf += generateUserGiveaway(instId, WICUS_USER);
//				
//				try {
//					conf += readWfExecFile(wfExecPath,instId,WICUS_USER) + "\n";
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		
//		
//		conf+="except ExperimentException as e:\n"+
//				"    # This is the default exception for most errors in the api\n"+
//				"    print \"ERROR: %s\" % e\n\n"+
//
//				"finally:\n"+
//				"    # Be sure to always deprovision the instances we have started. Putting\n"+
//				"    # the deprovision call under finally: make the deprovisioning happening\n"+
//				"    # even in the case of failure.\n"+
//				"    if exp is not None:\n"+
//				"        exp.deprovision()\n";
//		
//		return conf;
//	}

	private static String getSvaVagrantInfo(String sva, ArrayList<String> providersList) 
	{
		String conf ="";
		
		
		//get the image appliance that best supports the SVA
    	ResultSet getSvaInfoQR = QueryExecute.execute(svaModel, QueryStrings.getSvaInfoQS, "sva_uri", sva);
    	QuerySolution iapp = getSvaImageAppliance(getSvaInfoQR, providersList);

    	//String imageApp  = iapp.get("?iapp").toString();
    	//String vmImage  = iapp.get("?vmimg").toString();
    	String vmAppProvUri  = iapp.get("?prov").toString();
    	
    	//String vmAppProv = cv.getCloudProviderName(vmAppProvUri);
    	String vmImageId  = iapp.get("?vmid").toString();
    	
    	//get RAM mem of the VM
		 String hwmem = iapp.get("?hwmem").toString();
	     //MEM calculation
	     ResultSet getHwMemFeatQR = QueryExecute.execute(svaModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwmem);
	     int iappHwMem = 0;
	     if(getHwMemFeatQR.hasNext())
	     {
	    	 QuerySolution memqs = getHwMemFeatQR.next();
	     
	    	 String mv = memqs.get("?value").toString();
	     	String mu = memqs.get("?unit").toString();
	     	iappHwMem = (int) (Float.valueOf(mv) * getUnitFactor(mu));
	     }


    	conf += "Vagrant.configure(2) do |config|\n\n";
    	conf += "\tconfig.vm.boot_timeout = 60000\n\n";
    	conf += "\tconfig.vm.box = \""+vmImageId+"\"\n\n";
    	conf += "\tconfig.vm.provider \"virtualbox\" do |v|\n";
    	conf += "\t\tv.memory = "+iappHwMem+"\n";
    	conf += "\t\tv.cpus = 2\n";
    	conf += "\tend\n\n";
    	
    	
    	 
    	
    	
    	return conf;
	}
	

	//returns the image appliance (along with VM image and VM image id) that best suits the SVA
	//TODO up to now it only returns the first entry the providers list
	//we have to add the logic for selecting the best one
    
	private static QuerySolution getSvaImageAppliance(ResultSet qr, ArrayList<String> providersList) 
	{
		 QuerySolution res = null;
		 
		 ArrayList<QuerySolution> validIapps = new ArrayList<QuerySolution>();
		
	     System.out.println("*********** providersList **********");
	     System.out.println("* " + providersList.toString());
	     System.out.println("***************************************\n");	     

	     
	     //get wfUri HW reqs (in MB and Hz)
	     float wfHwCpu = 0;
	     float wfHwMem = 0;
	     float wfHwStorage = 0;
	     String archValue = "";
	     
	     //get the image appliance that best supports the SVA
	     ResultSet getWfHwReqQR = QueryExecute.execute(wfiModel, QueryStrings.getWfHwReqQS, "wf_uri", wfUri);

	     if(!getWfHwReqQR.hasNext())
	    	 return null;
	     
		 QuerySolution wfQs = getWfHwReqQR.next();
		 String hwcpu = wfQs.get("?hwcpu").toString();
		 String hwmem = wfQs.get("?hwmem").toString();
		 String hwstorage = wfQs.get("?hwstorage").toString();
		 
		 
		 //CPU calculation
	     ResultSet getHwCPUFeatQR = QueryExecute.execute(wfiModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwcpu);
	     
	     while(getHwCPUFeatQR.hasNext())
	     {
	    	 QuerySolution cpuqs = getHwCPUFeatQR.next();
	    	 String v = cpuqs.get("?value").toString();
	    	 String u = cpuqs.get("?unit").toString();
	    	 
	    	 if(u.equals("bits")) //arch
	    	 {
	    		 archValue = v;
	    	 }
	    	 else //freq
	    	 {
	    		 wfHwCpu = Float.valueOf(v) * getUnitFactor(u);
	    	 }
	    	 
	     }
	     
	     
	     //MEM calculation
	     ResultSet getHwMemFeatQR = QueryExecute.execute(wfiModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwmem);
	     QuerySolution memqs = getHwMemFeatQR.next();
	     
    	 String mv = memqs.get("?value").toString();
    	 String mu = memqs.get("?unit").toString();
	     wfHwMem = Float.valueOf(mv) * getUnitFactor(mu);
	     
	     //Storage calculation
	     ResultSet getHwStorageFeatQR = QueryExecute.execute(wfiModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwstorage);
	     QuerySolution stqs = getHwStorageFeatQR.next();
	     
    	 String sv = stqs.get("?value").toString();
    	 String su = stqs.get("?unit").toString();
	  
	     wfHwStorage = Float.valueOf(sv) * getUnitFactor(su);
	     
	     
	     while(qr.hasNext())
	     {
	    	 QuerySolution qs = qr.next();
	     	 String vmAppProvUri  = qs.get("?prov").toString();
	     	 
	     	 //check that provider is available
	     	 if(!providersList.contains(vmAppProvUri))
	     		 continue;
	     	 
	     	 if(checkHwReqs(wfHwCpu, wfHwMem, wfHwStorage, archValue, qs))
	     	 {
	     		  validIapps.add(qs);
	     	 }
	     	 
	     } 

	     System.out.println("\n\n<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
	     System.out.println("<><><><><><><><><><><><><>     VALID IAPP       <><><><><><><><><><><><><><><>");
	     System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
	     
	     for(QuerySolution vsvaqs : validIapps)
	     {
	    	 System.out.println("<>" + vsvaqs.get("?iapp"));
	     }
	     
	     
	     System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
	     
	     res = selectIapp(validIapps);
	     
	     return res;
	}
	
	private static QuerySolution selectIapp(ArrayList<QuerySolution> validIapps) {
	     //TODO: get the best, not the first
		return validIapps.get(0);
	}
	
	private static float getUnitFactor(String unit) 
	{
		//byte scale
		if(unit.equals("KB"))
			return 1/1024;
		if(unit.equals("MB"))
			return 1;
		if(unit.equals("GB"))
			return 1024;
		if(unit.equals("TB"))
			return 1024 * 1024 ;
		
		//freq scale
		if(unit.equals("Hz"))
			return 1;
		if(unit.equals("KHz"))
			return 1000;
		if(unit.equals("MHz"))
			return 1000*1000;
		if(unit.equals("GHz"))
			return 1000*1000*1000;
		
		
		
		return 1f;
	}

	private static boolean checkHwReqs(float wfHwCpu, float wfHwMem,float wfHwStorage, String archValue, QuerySolution qs) 
	{
		//iapp HW specs (in MB)
	    float iappHwCpu = 0;
	    float iappHwMem = 0;
	    float iappHwStorage = 0;
	    String iappArch = "";
	    
		 String hwcpu = qs.get("?hwcpu").toString();
		 String hwmem = qs.get("?hwmem").toString();
		 String hwstorage = qs.get("?hwstorage").toString();
		 
		 
		 //CPU calculation
	     ResultSet getHwCPUFeatQR = QueryExecute.execute(svaModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwcpu);
	     
	     if(getHwCPUFeatQR.hasNext())
	     {
		     while(getHwCPUFeatQR.hasNext())
		     {
		    	 QuerySolution cpuqs = getHwCPUFeatQR.next();
		    	 String v = cpuqs.get("?value").toString();
		    	 String u = cpuqs.get("?unit").toString();
		    	 
		    	 if(u.equals("bits")) //arch
		    	 {
		    		 iappArch = v;
		    	 }
		    	 else //freq
		    	 {
		    		 iappHwCpu = Float.valueOf(v) * getUnitFactor(u);
		    	 }
		    	 
		     }
	     }
	     else
	     {
		     System.err.println("Not found iapp CPU for:"+hwcpu);
	     }
	     
	     //MEM calculation
	     ResultSet getHwMemFeatQR = QueryExecute.execute(svaModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwmem);
	     if(getHwMemFeatQR.hasNext())
	     {
	    	 QuerySolution memqs = getHwMemFeatQR.next();
	     
	    	 String mv = memqs.get("?value").toString();
	     	String mu = memqs.get("?unit").toString();
	     	iappHwMem = Float.valueOf(mv) * getUnitFactor(mu);
	     }
	     else
	     {
		     System.err.println("Not found iapp mem for:"+hwmem);
	     }
	     
	     //Storage calculation
	     ResultSet getHwStorageFeatQR = QueryExecute.execute(svaModel, QueryStrings.getHwFeatValueQS, "hw_uri", hwstorage);
	     if(getHwStorageFeatQR.hasNext())
	     {
		     QuerySolution stqs = getHwStorageFeatQR.next();
		     
		     String sv = stqs.get("?value").toString();
		     String su = stqs.get("?unit").toString();
		  
		     iappHwStorage = Float.valueOf(sv) * getUnitFactor(su); 
	     }
	     else
	     {
		     System.err.println("Not found iapp storage for:"+hwstorage);
	     }
		

	     System.out.println("<><><><><><><><><><><> checkHwReqs <><><><><><><><><><>");
	     System.out.println("<> wfHwCpu="+wfHwCpu);
	     System.out.println("<> wfHwMem="+wfHwMem);
	     System.out.println("<> wfHwStorage="+wfHwStorage);
	     System.out.println("<> archValue="+archValue);
	     System.out.println("<><> " +qs.get("?iapp"));
	     System.out.println("<> iappHwCpu="+iappHwCpu);
	     System.out.println("<> iappHwMem="+iappHwMem);
	     System.out.println("<> iappHwStorage="+iappHwStorage);
	     System.out.println("<> iappArch="+iappArch);
	     System.out.println("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
	     
	     if ((iappHwCpu>=wfHwCpu) && (iappHwMem>=wfHwMem) && (iappHwStorage>=wfHwStorage) && (archValue.equals(iappArch)))
	     {
		     System.out.println("<><><><><><><><><><><><><>    IT'S A MATCH      <><><><><><><><><><><><><><><>\n");
	    	 return true;
	     }
	     
	     
		//TODO
		return false;
	}
	private static String getReqVagrantInfo(String req, Map<String, Digraph<String>> deployReqDg) 
	{
		String conf = "";
		//retrieve the Sw Components from SW catalog of each stack of the DGraph and add it to the conf.
    	Digraph<String> dg = deployReqDg.get(req);
    	
	     
	     System.out.println("*********** DG_TOP_SORT **********");
	     System.out.println("* " + dg.topSort());
	     System.out.println("***************************************\n");
	     

	     System.out.println("*********** DG_REVERSE_SORT **********");
	     System.out.println("* " + dg.reverseTopSort());
	     System.out.println("***************************************\n");
	     
	     
    	//iterate the list of stacks and get the SW comp for each one
    	Iterator<String> stackIt = dg.reverseTopSort().iterator();
    	while(stackIt.hasNext())
    	{
			
    		String stackUri = stackIt.next();
    		ResultSet getSwCompSwStackQR = QueryExecute.execute(swcModel, QueryStrings.getSwCompSwStackQS, "stack_uri", stackUri);
    		
    		
    		
    		if(AVOID_STACK_DUPLICATED)
    		{
	    		if(installedStacks.contains(stackUri))
	    		{
	    			//avoid deploying an stack several times
	    			continue;
	    		}
	    		else
	    		{
	    			installedStacks.add(stackUri);
	    		}
    		}
    		
			conf += "\t# [STACK] Deployment of "+stackUri+" stack\n\n";

    		
    		//for each SW comp we get its dep. plan and its configuration info.
    		while(getSwCompSwStackQR.hasNext())
    		{
    			QuerySolution qs = getSwCompSwStackQR.next();
    			String swc = qs.get("?swc").toString();
    			
    			ResultSet getVersionSwCompQR = QueryExecute.execute(swcModel, QueryStrings.getVersionSwCompQS, "swc_uri", swc);
    			String versionInfo = "\n\n";
    			while(getVersionSwCompQR.hasNext())
        		{
    				QuerySolution qv = getVersionSwCompQR.next();
    				String vn = qv.get("?vn").asLiteral().getString();
    				versionInfo = " (version: "+ vn + ")\n\n";
        		}

    			conf += "\t# [COMPONENT] Deployment of "+swc+" component" + versionInfo;
    			
    			//get SWC Conf Info, and store on a HashMap<PARNAME,PARVALUE> 
    			//representing the value for each par as expressed in the reqs
    			HashMap<String,String> reqParNameValues = new HashMap<String,String>();
    			ResultSet getConfParSwCompQR = QueryExecute.execute(wfiModel, QueryStrings.getConfParSwCompQS, "swc_uri", swc);
    			while(getConfParSwCompQR.hasNext())
	    		{
    				QuerySolution qsp = getConfParSwCompQR.next();
	    			//String confPar = qsp.get("?confpar").toString();
	    			
	    			String parName = qsp.get("?parname").asLiteral().getString();
	    			String parValue = qsp.get("?parvalue").asLiteral().getString();
	    			reqParNameValues.put(parName, parValue);
	    			
	    		}
    			
    			//TODO: REMOVE
    		     System.out.println("reqParNameValues:" + reqParNameValues);
    			
    			ResultSet getDepPlanSwcCompQR = QueryExecute.execute(swcModel, QueryStrings.getDepPlanSwcCompQS, "swc_uri", swc);
    			while(getDepPlanSwcCompQR.hasNext())
	    		{
    				QuerySolution qsp = getDepPlanSwcCompQR.next();
	    			String depPlan = qsp.get("?depplan").toString();
    				    

	    			//Get Dep Steps of a Dep Plan ()
        			ResultSet getDepStepsDepPlanQR = QueryExecute.execute(swcModel, QueryStrings.getDepStepsDepPlanQS, "dp_uri", depPlan);
        			
        			//Get Dep Steps linked by nextStep
        			ResultSet getNextDepStepsDePlanQR = QueryExecute.execute(swcModel, QueryStrings.getNextDepStepsDePlanQS, "dp_uri", depPlan);
        			

        			//get all the steps and add them to a graph
        			Digraph<String> nextStepsGraph = new Digraph<String>();
        			
        			while(getDepStepsDepPlanQR.hasNext())
        			{
        				QuerySolution qsDs = getDepStepsDepPlanQR.next();
        				String depStep = qsDs.get("?depstep").toString();
        				nextStepsGraph.add(depStep);
        			}
        			

        			//link them
        			while(getNextDepStepsDePlanQR.hasNext())
        			{
        				QuerySolution qsDs = getNextDepStepsDePlanQR.next();
        				String depStep1 = qsDs.get("?step1").toString();
        				String depStep2 = qsDs.get("?step2").toString();
        				nextStepsGraph.add(depStep1, depStep2);
        			}
        			
        			
        			//sort them
        			List<String> nextStepsList = nextStepsGraph.topSort();

        		    System.out.println("*********** nextStepsList **********");
        		    System.out.println("* " + nextStepsList);
        		    System.out.println("***************************************\n");
        			

            		for(String depStep : nextStepsList)
    	    		{

    	    			//TODO: REMOVE
    	    		     System.out.println(">depStep:" + depStep);
    	    		     
        				//Get Script of and conf info of a Dep Step
            			ResultSet getScriptConfInfoDepStepQR = QueryExecute.execute(swcModel, QueryStrings.getScriptConfInfoDepStepQS, "ds_uri", depStep);
            			while(getScriptConfInfoDepStepQR.hasNext())
        	    		{
            				
            				QuerySolution qsSCI= getScriptConfInfoDepStepQR.next();
            				String script = qsSCI.get("?script").asLiteral().getString();
            				String confInfo = qsSCI.get("?conf").toString();

        	    		     
            				String argsString ="";

                			
            				//Get Conf Par of a given conf info
                			ResultSet getConfParConfInfoQR = QueryExecute.execute(swcModel, QueryStrings.getConfParConfInfoQS, "ci_uri", confInfo);
                			while(getConfParConfInfoQR.hasNext())
            	    		{
                				QuerySolution qspCP = getConfParConfInfoQR.next();
            	    			String confPar = qspCP.get("?confpar").toString();

            	    			//get the literal values of a given conf par 
            	    			ResultSet getParsConfParQR = QueryExecute.execute(swcModel, QueryStrings.getParsConfParQS, "cp_uri", confPar);
                    			while(getParsConfParQR.hasNext())
                	    		{
                    				QuerySolution qspPar = getParsConfParQR.next();
	            	    			String parName = qspPar.get("?parname").asLiteral().getString();
	            	    			String parValue = qspPar.get("?parvalue").asLiteral().getString(); 

	            	    			
	            	    			//we get the value of the parName expressed in the requirements
	            	    			//of not found, we assume that the default value expressed in the
	            	    			//SWC catalogue must be used
	            	    			if(reqParNameValues.containsKey(parName))
	            	    			{
	            	    				parValue = reqParNameValues.get(parName);
	            	    			}
	            	    			else
	            	    			{
	            	    				//System.err.println(PAR_NOT_FOUND_ERR_MSG + parName + " of " + parValue);
	            	    				//System.err.println(stackUri + "|" + script +  "|" + script + " from: " + reqParNameValues);
	            	    			}	
	            	    			
	            	    			//add the parameter (either default or specific value) to the args line
            	    				argsString += "\"" + parName + "\",\"" + parValue + "\",";
                	    		}  

            	    		}
                			
                			if(!argsString.isEmpty())
                			{
                				argsString =  ", args: [" + argsString;
	                			//remove last comma 
	                			argsString = argsString.substring(0, argsString.length()-1);
	                			argsString += "]\n\n";
                			}
                			
                			//now we have the SCRIPT and the args string

                			conf += "\t# [STEP] Excution of "+depStep+" step\n\n";
                			
                			
                			//execute the string with the parameters
                			conf += "\t# executing the "+script+" script\n";
                			//conf += "    print \"executing the "+script+" script\"\n";
                			conf += "\tconfig.vm.provision :shell, path: \""+script+"\""+argsString;
                			
        	    		}
        				
    	    		}
	    			
	    		}
    			
    		}
    	}
		return conf;
	}

	//this method merges requirements into SVAs that are equal if those requirements are compatible.
	//it returns a map<SVA,map<SVA_ID,list<REQ>>>
	//SVA is the base appliance, where SVA_ID is an instantiation of the appliance for a set of requirementes
	//this is like this because it could be the case that 2 REQS are associated to the same base SVA,
	//but they are incompatible, so we need two instantiate 2 different SVA for them
	private static HashMap<String, HashMap<String, ArrayList<String>>> mergeMaxSVA(Map<String, String> reqMaxSvaList) 
	{
		
		HashMap<String,HashMap<String,ArrayList<String>>> res = new HashMap<String,HashMap<String,ArrayList<String>>>();
		
		Iterator<Entry<String, String>> reqIt = reqMaxSvaList.entrySet().iterator();
		while(reqIt.hasNext())
		{
			Entry<String, String> pair = reqIt.next();
			String req = pair.getKey();
			String sva = pair.getValue();
			
			if(res.containsKey(sva))
			{
				//we iterate all the svaIds for an SVA
				HashMap<String, ArrayList<String>> svaMap = res.get(sva);
				Iterator<Entry<String, ArrayList<String>>> svaIter = svaMap.entrySet().iterator();
				while (svaIter.hasNext())
				{
					Entry<String, ArrayList<String>> svaIdPair = svaIter.next();
					String svaId = svaIdPair.getKey();
					ArrayList<String> svaIdReqs = svaIdPair.getValue();
					
					if(checkReqsCompatibility(req,svaIdReqs))
					{
						//if the req is compatible with the others
						//we add it to the list of the sva
						res.get(sva).get(svaId).add(req);
					}
					else
					{
						HashMap<String, ArrayList<String>> map = res.get(sva);
						
						//we generate a new identifier for the sva
						int size = map.entrySet().size();
						String newSvaId = sva+size;
						map.put(newSvaId, new ArrayList<String>());
						map.get(newSvaId).add(req);
					}
					
				}
				
			}
			else
			{
				res.put(sva, new HashMap<String,ArrayList<String>>());
				HashMap<String, ArrayList<String>> map = res.get(sva);
				
				//we generate a new identifier for the sva
				int size = map.entrySet().size();
				String newSvaId = sva+size;
				map.put(newSvaId, new ArrayList<String>());
				map.get(newSvaId).add(req);
				
				//at this point we have
				//<SVA,<SVA0,<REQ>>>
				
			}
			
		}
		
		return res;
		
	}

	//This function checks whether a requirement req is compatible with all the requiments
	//already merged in the SVA (svaReqList)
	//TODO add the logic for checking compatibility
	//up to know we are assuming that unless the user says that 2 requirements are INCOMPATIBLE
	//they are always COMPATIBLE 
	private static boolean checkReqsCompatibility(String req, ArrayList<String> svaResList) 
	{
		boolean res = true;
		return res;
	}
	

	//This function checks whether the requirements of a wf workflow wf is compatible with all the requiments
	//already merged in the SVA (svaReqList)
	//TODO add the logic for checking compatibility
	//up to know we are assuming that unless the user says that 2 requirements are INCOMPATIBLE
	//they are always COMPATIBLE 
	//TODO: add a parameter for getting the WF requirements
	private static boolean checkWfCompatibility(String wf, HashSet<String> svaIdReqs, Map<String, LinkedHashMap<String, Digraph<String>>> deployWfDg) 
	{
		boolean res = true;
		return res;
	}

	
	private static String readWfExecFile( String file, String user) throws IOException {
	    BufferedReader reader = new BufferedReader( new FileReader (file));
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    while( ( line = reader.readLine() ) != null ) {
	        stringBuilder.append( line );
	        stringBuilder.append( ls );
	    }

	    String res = stringBuilder.toString();
	    res = res.replace("%USER%", user);
	    
	    
	    
	    return res;
	}


	private static String printMapKeys(	HashMap<String, HashMap<String, LinkedHashSet<String>>> mergedPropagatedSvas) 
	{
		String res="";
		Iterator<Entry<String, HashMap<String, LinkedHashSet<String>>>> it = mergedPropagatedSvas.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Map.Entry<String, HashMap<String, LinkedHashSet<String>>> pairs = it.next();
	    	String req = pairs.getKey().toString();
	    	Object list = pairs.getValue();
	    	

	    	res+="\n";
	    	res+="* Key: " + req + "\n";
	    	res+="* Value:" + list.toString() + "\n";
		
	    	res+="* ----------------------------------------------------\n";
	    	
	    }		
		return res;
	}
	
}
