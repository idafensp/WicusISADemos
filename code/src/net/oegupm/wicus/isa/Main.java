package net.oegupm.wicus.isa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.oegupm.wicus.isa.queries.QueryExecute;
import net.oegupm.wicus.isa.queries.QueryStrings;
import net.oegupm.wicus.isa.utils.ConfigValues;
import net.oegupm.wicus.isa.utils.Digraph;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class Main {
	
	private static ConfigValues cv;
	 

	private static OntModel wfiModel;
	private static OntModel swcModel;
	private static OntModel svaModel;
	


	private static String NONE_SVA_FOUND_MSG = "NOT_SVA_FOUND for: ";


	private static String DEFAULT_INSTANCE_TYPE = "m1.large";


	private static String PAR_NOT_FOUND_ERR_MSG = "Paramater from Requirement not found on Plan: ";

	private static boolean DEBUG = true;

	public static void main(String[] args) {
		
		
		cv = new ConfigValues(args[0]);
		cv.readConfigValues();
		System.out.println(cv);
		
		PrintStream originalStream = System.out;
		PrintStream dummyStream = originalStream;

		if(!DEBUG)
		{
			    dummyStream = new PrintStream(new OutputStream(){
			    public void write(int b) {
			        //NO-Output
			    }
			});
		}

		System.setOut(dummyStream);
		
		
		//load the model containing the dataset
		wfiModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
		wfiModel.read(cv.value("WFI_DATASET_PATH"), null);
		

		swcModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
		swcModel.read(cv.value("SWC_DATASET_PATH"), null);		
		

		svaModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
		svaModel.read(cv.value("SVA_DATASET_PATH"), null);		
		
		
		ResultSet getWfConcreteWfReqsQueryResults = QueryExecute.execute(wfiModel, QueryStrings.getWfConcreteWfReqsQS, "wf_uri", cv.value("WF_URI"));
	        
		Map<String,ArrayList<String>> concWfReqs = new HashMap<String,ArrayList<String>>();
		
	    while(getWfConcreteWfReqsQueryResults.hasNext())
	    {
	    	QuerySolution qs = getWfConcreteWfReqsQueryResults.next();
	    	String cw = qs.get("?cw").toString();
	    	String swrq = qs.get("?swrq").toString();
//	    	System.out.println("Read:" + cw + " requires " + swrq);
	    	
	    	if(concWfReqs.containsKey(cw))
	    	{
	    		//this CW has already a REQ stored, therefore we add the REQ to its list
	    		concWfReqs.get(cw).add(swrq);
	    	}
	    	else
	    	{
	    		//there is no annotation for this CW yet. We create a new list with the REQ and add it to a new CW entry
	    		ArrayList<String> swrqList = new ArrayList<String>();
	    		swrqList.add(swrq);
	    		concWfReqs.put(cw, swrqList);	    		
	    	}
	    }
	    
	    
	    //at this point we have a list of <CW,List<REQ>> ONLY FOR THE SUB WORKFLOWS
	    //we need to add the requirements of the top level wf (if any), usually the WMS info
	    ResultSet getTopWfReqsQueryResults = QueryExecute.execute(wfiModel, QueryStrings.getTopWfReqsQS, "wf_uri", cv.value("WF_URI"));
	    while(getTopWfReqsQueryResults.hasNext())
	    {
	    	QuerySolution qs = getTopWfReqsQueryResults.next();
	    	String wf_uri = cv.value("WF_URI");
	    	String swrq = qs.get("?swrq").toString();
	    	
	    	if(concWfReqs.containsKey(wf_uri))
	    	{
	    		//the WF has already a REQ stored, therefore we add the REQ to its list
	    		concWfReqs.get(wf_uri).add(swrq);
	    	}
	    	else
	    	{
	    		//there is no annotation for the WF yet. We create a new list with the REQ and add it to the WF entry
	    		ArrayList<String> swrqList = new ArrayList<String>();
	    		swrqList.add(swrq);
	    		concWfReqs.put(wf_uri, swrqList);	    		
	    	}
	    	
	    }
		
	

	     System.out.println("*********** concWfReqs **********");
	     System.out.println("* " + printMapList(concWfReqs));
	     System.out.println("***************************************\n");
	     
		//at this point we have a list of <WF,List<REQ>> with all the reqs (including WMS)
	    //we propagate the requirements down on the wf-tree
	     Map<String, ArrayList<String>> propagatedWfReqs = propagateWfReqs(cv.value("WF_URI"),concWfReqs);
	     
	     System.out.println("*********** propagatedWfReqs **********");
	     System.out.println("* " + printMapList(propagatedWfReqs));
	     System.out.println("***************************************\n");

    
	    //we create a list for storing <REQ,List<STACK>>, relating a REQ with the set of Stacks that compose it
	    Map<String,ArrayList<String>> reqSwStack = new HashMap<String,ArrayList<String>>();
	    	    
	    

	    //without propagation
	    //Iterator<Entry<String, ArrayList<String>>> cwit = concWfReqs.entrySet().iterator();
	    
	    //with propagation
	    Iterator<Entry<String, ArrayList<String>>> cwit = propagatedWfReqs.entrySet().iterator();

	    
	    
	    while (cwit.hasNext()) 
	    {
	    	Entry<String, ArrayList<String>> pairs = cwit.next();
	    	ArrayList<String> swrqList = (ArrayList<String>) pairs.getValue();
	    	//System.out.println(pairs.getKey() + ":");
	    	for(String swrq : swrqList)
	    	{
	    		//System.out.println("Retrieving stacks of " + swrq);
	    		
	    		//query about swrq to 
	    		ResultSet getReqSwStackQR = QueryExecute.execute(wfiModel, QueryStrings.getReqSwStackQS, "req_uri", swrq);
	    		ArrayList<String> reqSwtList = new ArrayList<String>();
	    		while(getReqSwStackQR.hasNext())
	    		{
	    			QuerySolution qs = getReqSwStackQR.next();
	    			String swt2 = qs.get("?swst").toString();
	    			reqSwtList.add(swt2);
	    		}
	    		
	    		//we add the list of stacks to the requirement
	    		reqSwStack.put(swrq, reqSwtList);
	    	}
	    }
	    
	    //at this point we have in reqSwStack a list of <REQ, List<Stack>>, relating a REQ with the set of Stacks that compose it
	    
	    //System.out.println("reqSwStack="+reqSwStack.toString());
	    
	    
	    //iterate the list of REQ to obtain Directed Graph with their required stacks calculated recursively
	    Map<String,Digraph<String>> reqStackDiagraph = new HashMap<String,Digraph<String>>();
	    Iterator<Entry<String, ArrayList<String>>> reqit = reqSwStack.entrySet().iterator();
	    while (reqit.hasNext()) 
	    {
	    	Digraph<String> dg = new Digraph<String>();
	     	
	    	Entry<String, ArrayList<String>> pairs = reqit.next();
	    	String req = pairs.getKey().toString();
	    	ArrayList<String> swtList = (ArrayList<String>) pairs.getValue();
	    	
	    	getStacksDependencies(dg,swtList);
	    	
	    	reqStackDiagraph.put(req, dg);
	    	 	
	    }
	    
	    //at this point we have in reqStackDiagraph a list of <REQ,DIGRAPH> relating a requirement with a graph including the direct and derived dependencies

	     
	    System.out.println("*********** reqStackDiagraph **********");
	    System.out.println("* " + printReqDg(reqStackDiagraph));
	    System.out.println("***************************************\n");
	    
	    
	    //load the SVAs and their stacks based on the available providers
	    Map<String,HashSet<String>> svaList = new HashMap<String,HashSet<String>>();
	    for(String prov : cv.getCloudProviders())
	    {
		
	    	ResultSet getSvaAndStackProviderQR = QueryExecute.execute(svaModel, QueryStrings.getSvaAndStackProviderQS, "prov_uri", prov);
	    	while(getSvaAndStackProviderQR.hasNext())
    		{
    			QuerySolution qs = getSvaAndStackProviderQR.next();
    			String sva = qs.get("?sva").toString();
    			String stack = qs.get("?stack").toString();
    			if(svaList.containsKey(sva))
    			{
    				svaList.get(sva).add(stack); //if there is an entry for the sva we add a new stack
    			}
    			else
    			{
    				HashSet<String> sl = new HashSet<String>();
    				sl.add(stack);
    				svaList.put(sva, sl);
    			}
    		}
	    }
	    
	    
	    //at this point we have a list of SVAs each one of them with its list of stacks.
	    System.out.println("************** svaList *************");
	    System.out.println("* " +svaList);
	    System.out.println("***************************************\n");
	    
	    //we have at this point:
	    //reqStackDiagraph: a list of <REQ,DIGRAPH> relating a requirement with a graph including the direct and derived dependencies
	    //svaList: a list of <SVA,List<Stack>> each one of them with its list of stacks.
	    
	    //get how a SVAs match each REQ, based on the stacks@SVA and the diagram@REQ
	    //we obtain a list Map<String, Map<String, Integer>> that relates the number of stack that are common
	    //to both, the req and the sva
	    Map<String, Map<String, Integer>> reqSvaList = getCompatibleSva(reqStackDiagraph,svaList);
	    
	    System.out.println("************** reqSvaList *************");
	    System.out.println("* " + printMapMap(reqSvaList));
	    System.out.println("***************************************\n");
	    
	    
	    //with reqs propagation
	    Map<String, Map<String, Integer>> wfSvaList = getCompatibleSvaPropagated(propagatedWfReqs,reqStackDiagraph,svaList);

	    System.out.println("************** wfSvaList *************");
	    System.out.println("* " + printMapMap(wfSvaList));
	    System.out.println("***************************************\n");
	    
	    
	    
	    
	    
	    //we obtain the list of <REQ,SVA> containing the most suitable SVA (max cadinality)
	    //for each REQ. We ask for a threshold representing the minimun acceptable value.
	    Map<String,String> reqMaxSvaList = new HashMap<String,String>();
	    
	    Iterator<Entry<String, Map<String, Integer>>> reqSvaIt = reqSvaList.entrySet().iterator();
	    while (reqSvaIt.hasNext()) 
	    {     	
	    	Entry<String, Map<String, Integer>> pairs = reqSvaIt.next();
	    	String req = pairs.getKey().toString();
	    	Map<String, Integer> svaIntList = (Map<String, Integer>) pairs.getValue();
	    	

	    	System.out.println("REQ: " + req);
	    	String maxSva = getMaxSva(svaIntList,0);
	    	if(maxSva.equals(NONE_SVA_FOUND_MSG))
	    	{
	    		System.out.println(NONE_SVA_FOUND_MSG + req);
	    		return;
	    	}
	    	
	    	reqMaxSvaList.put(req, maxSva);
	    }
	    
	    
	    //we obtain the list of <WF,SVA> containing the most suitable SVA (max cadinality)
	    //for each WF/subWF. We ask for a threshold representing the minimun acceptable value.
	    Map<String,String> wfMaxSvaList = new HashMap<String,String>();
	    
	    Iterator<Entry<String, Map<String, Integer>>> wfSvaIt = wfSvaList.entrySet().iterator();
	    while (wfSvaIt.hasNext()) 
	    {     	
	    	Entry<String, Map<String, Integer>> pairs = wfSvaIt.next();
	    	String wf = pairs.getKey().toString();
	    	Map<String, Integer> svaIntList = (Map<String, Integer>) pairs.getValue();
	    	

	    	System.out.println("WF: " + wf);
	    	String maxSva = getMaxSva(svaIntList,0);
	    	if(maxSva.equals(NONE_SVA_FOUND_MSG))
	    	{
	    		System.err.println(NONE_SVA_FOUND_MSG + wf);
	    		return;
	    	}
	    	
	    	wfMaxSvaList.put(wf, maxSva);
	    }
	    	
//		System.out.println("*********** wfMaxSvaList **********");
//		System.out.println("* " + printMap(wfMaxSvaList));
//		System.out.println("***************************************\n");
		
		
		
	   
	   //we now calculate what stacks must be deployed on the SVA
	   //to do this we calculate DG-SVA_STACKS, being DG the direct graph of a requirement
	   //and SVA_STACKS the set of stacks available on an SVA.
	   //We will obtain a new Map<String,Digraph<String>> specifying the DG to be deployed for each REQ
	   
		Map<String,Digraph<String>> deployReqDg = substractStackFromReqDg(reqMaxSvaList,reqStackDiagraph,svaList);
		   
		//we now calculate what stacks must be deployed on the SVA
	   //to do this we calculate DG-SVA_STACKS, being DG the direct graph of a WF/subWF
	   //and SVA_STACKS the set of stacks available on an SVA.
	   //We will obtain a new map<WF,map<REQ,DIGRAPH> specifying the set of reqs and their DGs to be deployed for each WF/subWF
	  
		Map<String, LinkedHashMap<String, Digraph<String>>> deployWfDg = substractStackFromWfReqDg(wfMaxSvaList,propagatedWfReqs,reqStackDiagraph,svaList);
		   
//		System.out.println("*********** deployWfDg **********");
//		System.out.println("* " + printMapMapDigraph(deployWfDg));
//		System.out.println("***************************************\n");
	   
		
		
		
	   //at this point we have:
	   //reqMaxSvaList: a list relating each REQ with its most suitable SVA
	   //deployReqDg: a list defining for each REQ with stacks must be deployed (as they are not included already in the SVA stacks)
	   //deployWfDg: we will get a map<WF,map<REQ,DIGRAPH> relating each WF with a list of <REQS,DIGRAPH>
	   
	   
	   
	   
//	   System.out.println("*********** deployReqDg **********");
//	   System.out.println(deployReqDg);
//	   System.out.println("************************************\n");
		   

	   
//	   String infConf = generateInfrastructureConfiguration(reqMaxSvaList,deployReqDg);
	   
//	   System.out.println("*********** infConf **********");
//	   System.out.println(infConf);
//	   System.out.println("************************************\n");
		 
	   

	   
//	   String infConfWrangler = generateInfrastructureConfigurationWrangler(reqMaxSvaList,deployReqDg);
//	   
//	   System.out.println("*********** infConfWrangler **********");
//	   System.out.println(infConfWrangler);
//	   System.out.println("************************************\n");
	   
	   
//	   String mergedSvasWrangler = mergeMaxSVAandGenerateInfConfigurationWrangler(reqMaxSvaList,deployReqDg);
//	   
//	   System.out.println("*********** mergedSvas **********");
//	   System.out.println(mergedSvasWrangler);
//	   System.out.println("*********************************\n");
		

		String wfExecPrecipPath = cv.value("WF_EXEC_FILE_PRECIP");
		String wfExecVagrantPath = cv.value("WF_EXEC_FILE_VAGRANT");


//	   String mergedSvasPrecip = precipGen.mergeMaxSVAandGenerateInfConfigurationPrecip(reqMaxSvaList,deployReqDg,cv.getCloudProviders(),wfExecPath);
	   
//	   System.out.println("*********** mergedSvasPrecip **********");
//	   System.out.println(mergedSvasPrecip);
//	   System.out.println("*********************************\n");
	   

	   System.setOut(originalStream);
	   
	   //flag for avoiding repetitions of stacks
	   boolean dup = cv.value("AVOID_STACK_DUPLICATED").equals("true");
	   
	   if(!cv.value("OUT_PRECIP_FILE").equals("false"))
	   {
		   
		   PrecipSpecGenerator precipGen = new  PrecipSpecGenerator(cv.value("WF_URI"), wfiModel, swcModel, svaModel, cv.value("WICUS_USER"),cv.value("SSH_TYPE"),dup);
		   
		   ArrayList<String> filteredPrecipProviders = filterPrecipProviders(cv.getCloudProviders());
		   if(filteredPrecipProviders.isEmpty())
		   {
			   System.err.println("*********** ERROR **********");
			   System.err.println("* NO PRECIP PROVIDERS HAVE BEEN SPECIFIED");
			   System.err.println("*********************************\n");
		   }
		   else
		   {
			   String mergedSvasPropagatedPrecip = precipGen.mergeMaxSVAandGenerateInfConfigurationPropagatedPrecip(wfMaxSvaList,deployWfDg,deployReqDg,filteredPrecipProviders, wfExecPrecipPath);	   
//			   System.out.println("*********** mergedSvasPropagatedPrecip **********");
//			   System.out.println(mergedSvasPropagatedPrecip);
//			   System.out.println("*********************************\n");
			   try {
					PrintWriter out = new PrintWriter(cv.value("OUT_PRECIP_FILE"));
					out.println(mergedSvasPropagatedPrecip);
					out.close();
					
					System.out.println("Output precip file generated at " + cv.value("OUT_PRECIP_FILE"));
				} catch (IOException e) {
					System.err.println("Output precip file not generated. Path=" + cv.value("OUT_PRECIP_FILE"));
					e.printStackTrace();
				}
		   }
	   }
	   
	   if(!cv.value("OUT_VAGRANT_FILE").equals("false"))
	   {
		   VagrantSpecGenerator vagrantGen = new  VagrantSpecGenerator(cv.value("WF_URI"), wfiModel, swcModel, svaModel, cv.value("WICUS_USER"),cv.value("SSH_TYPE"),dup);
		  
		   ArrayList<String> filteredVagrantProviders = filterVagrantProviders(cv.getCloudProviders());
		   if(filteredVagrantProviders.isEmpty())
		   {
			   System.err.println("*********** ERROR **********");
			   System.err.println("* NO VAGRANT PROVIDERS HAVE BEEN SPECIFIED");
			   System.err.println("*********************************\n");
		   }
		   else
		   {
			   String mergedSvasPropagatedVagrant = vagrantGen.mergeMaxSVAandGenerateInfConfigurationPropagatedVagrant(wfMaxSvaList,deployWfDg,deployReqDg,filteredVagrantProviders, wfExecVagrantPath);

//			   System.out.println("*********** mergedSvasPropagatedVagrant **********");
//			   System.out.println(mergedSvasPropagatedVagrant);
//			   System.out.println("*********************************\n");
			   
			   try {
				PrintWriter out = new PrintWriter(cv.value("OUT_VAGRANT_FILE"));
				out.println(mergedSvasPropagatedVagrant);
				out.close();
				
				System.out.println("Output vagrant file generated at " + cv.value("OUT_VAGRANT_FILE"));
			} catch (IOException e) {
				System.err.println("Output vagrant file not generated. Path=" + cv.value("OUT_VAGRANT_FILE"));
				e.printStackTrace();
			}
		   }
		   
	   }
	   
	   
	   
	   
	   	
	    
	} //end of main




	private static ArrayList<String> filterVagrantProviders(ArrayList<String> cloudProviders) 
	{
		ArrayList<String> res = new ArrayList<String>();
		
		for(String prov : cloudProviders)
		{
			if(prov.contains("Vagrant"))
			{
				res.add(prov);
			}
		}
		
		return res;
	}




	private static ArrayList<String> filterPrecipProviders(ArrayList<String> cloudProviders) 
	{
		ArrayList<String> res = new ArrayList<String>();
		
		for(String prov : cloudProviders)
		{
			if(!prov.contains("Vagrant"))
			{
				res.add(prov);
			}
		}
		
		return res;
	}




	//wfUri: URI of the top level workflow
	//wfReqs: a map Map<WF,List<REQS>> relating each wf/subworkflow with their requirements
	//we obtains a Map<WF,List<REQS>> realiting each leav node of the workflow with ther accumulated requirements
	private static Map<String, ArrayList<String>> propagateWfReqs(String wfUri, Map<String, ArrayList<String>> wfReqs) {
		Map<String, ArrayList<String>> res = new HashMap<String, ArrayList<String>>();
		ArrayList<String> accumulatedReqs = new ArrayList<String>();
		
		wfReqDepthPropagation(res, accumulatedReqs, wfUri, wfReqs);
		
		return res;
	}





	private static void wfReqDepthPropagation(Map<String, ArrayList<String>> res, ArrayList<String> accumulatedReqs, String wfUri, Map<String, ArrayList<String>> wfReqs) 
	{
		boolean isLeave=true;
		ArrayList<String> aux = new ArrayList<String>(accumulatedReqs);
		
		//check if the new reqs are compatible with the ones
		//obtained so far
		if(checkCompatibilityList(aux,wfReqs.get(wfUri)))
		{
			//add the reqs of the given WF
			System.out.println("wfReqs:"+wfReqs);
			System.out.println("wfUri:"+wfUri);

			if(wfReqs.get(wfUri)!=null)
				aux.addAll(wfReqs.get(wfUri));
		}
		else
		{
			System.err.println("Accumulated Requirements are not compatible with the requirements of  " + wfUri);
		}
		
		
	
    	//get its subworkflows
	    ResultSet getWfConcreteSubWfQueryResults = QueryExecute.execute(wfiModel, QueryStrings.getWfConcreteSubWfQS, "wf_uri", wfUri);
	    while(getWfConcreteSubWfQueryResults.hasNext())
	    {
	    	isLeave = false;
	    	QuerySolution qs = getWfConcreteSubWfQueryResults.next();
	    	String swf = qs.get("?swf").toString();
	    	
	    	//recursive calls
	    	wfReqDepthPropagation(res, aux, swf, wfReqs);
	    }
	    
	    if(isLeave)
	    {
	    	res.put(wfUri, aux);
	    }
	}





	private static String printMergedSvas(HashMap<String, HashMap<String, ArrayList<String>>> mergedSvas) 
	{
		String res = "";
		Iterator<Entry<String, HashMap<String, ArrayList<String>>>> svaIt = mergedSvas.entrySet().iterator();
		while(svaIt.hasNext())
		{
			Entry<String, HashMap<String, ArrayList<String>>> svaIdPair = svaIt.next();
			String sva = svaIdPair.getKey();
			
			System.out.println("SVA="+sva);
			
			HashMap<String, ArrayList<String>> svaIdMap = svaIdPair.getValue();
			Iterator<Entry<String, ArrayList<String>>> svaIdIt = svaIdMap.entrySet().iterator();
			while(svaIdIt.hasNext())
			{
				Entry<String, ArrayList<String>> reqsPair = svaIdIt.next();
				String svaId = reqsPair.getKey();

				System.out.println("--SVA_id="+svaId+":");
				ArrayList<String> reqList = reqsPair.getValue();
				for(String r : reqList)
				{
					System.out.println("----"+r);
				}
			}
		}
		
		
		return res;
	}




	private static String generateInfrastructureConfiguration(Map<String, String> reqMaxSvaList,Map<String, Digraph<String>> deployReqDg) 
	{
		String conf = "";
		
		//iterate the reqs list
		Iterator<Entry<String, String>> reqIt = reqMaxSvaList.entrySet().iterator();
	    while (reqIt.hasNext()) 
	    {     	
	    	Entry<String, String> pairs = reqIt.next();
	    	String req = (String) pairs.getKey();
	    	String sva = (String) pairs.getValue();
	    	
	    	//get the image appliance that best supports the SVA
	    	ResultSet getSvaInfoQR = QueryExecute.execute(svaModel, QueryStrings.getSvaInfoQS, "sva_uri", sva);
	    	QuerySolution iapp = getSvaImageAppliance(getSvaInfoQR);

	    	String imageApp  = iapp.get("?iapp").toString();
	    	String vmImage  = iapp.get("?vmimg").toString();
	    	String vmImageId  = iapp.get("?vmid").toString();
	    	
	    	conf+="//Deployment Conf for Req " + req + "\n";
	    	conf+="//SVA=" + sva + "\n";
	    	conf+="//Image appliance=" + imageApp + "\n";
	    	conf+="//VmImage=" + vmImage + "\n";
	    	conf+="VM Id=" + vmImageId + "\n";
	    	
	    	//retrieve the Sw Components from SW catalog of each stack of the DGraph and add it to the conf.
	    	Digraph dg = deployReqDg.get(req);
	    	
	    	//iterate the list of stacks and get the SW comp for each one
	    	Iterator<String> stackIt = dg.getVertices().iterator();
	    	while(stackIt.hasNext())
	    	{
	    		String stack = stackIt.next();
	    		ResultSet getSwCompSwStackQR = QueryExecute.execute(swcModel, QueryStrings.getSwCompSwStackQS, "stack_uri", stack);
	    		
	    		conf+="//SW Stack=" + stack + "\n";
	    		
	    		//for each SW comp we get its dep. plan and its configuration info.
	    		while(getSwCompSwStackQR.hasNext())
	    		{
	    			QuerySolution qs = getSwCompSwStackQR.next();
	    			String swc = qs.get("?swc").toString();
	    			
	    			conf+="//--SW Comp=" + swc + "\n";
	    			String depPlan ="DEP_PLAN_STRING";
	    			
	    			ResultSet getDepStepsSwcCompQR = QueryExecute.execute(swcModel, QueryStrings.getDepStepsSwcCompQS, "swc_uri", swc);
	    			while(getDepStepsSwcCompQR.hasNext())
		    		{
	    				QuerySolution qsp = getDepStepsSwcCompQR.next();
		    			depPlan = qsp.get("?depplan").toString();
	    				
	    				conf+="//----Dep Plan=" + depPlan + "\n";

		    			String depStep = qsp.get("?depstep").toString();

	    				conf+="//------Dep Step=" + depStep + "\n";
	    				
	    				String script = qsp.get("?script").asLiteral().getString();
		    			
	    				conf+="Script=" + script + "\n";
		    			
		    		}
	    			
	    			
	    			ResultSet getConfParSwCompQR = QueryExecute.execute(swcModel, QueryStrings.getConfParDepPlanQS, "dp_uri", depPlan);
	    			while(getConfParSwCompQR.hasNext())
		    		{
	    				QuerySolution qsp = getConfParSwCompQR.next();
		    			String confPar = qsp.get("?confpar").toString();
		    			
		    			conf+="//----Conf Par=" + confPar + "\n";
		    			
		    			String parName = qsp.get("?parname").asLiteral().getString();
		    			String parValue = qsp.get("?parvalue").asLiteral().getString();
		    			
		    			conf+=parName +"=" + parValue + "\n";
		    			
		    		}	    			
	    		}
	    		
	    	}
	    	
	    	
	    	conf+="\n//End of Dep. Info\n\n";
	    }
	    
		
		
		return conf;
	}

	
	private static String generateInfrastructureConfigurationWrangler(Map<String, String> reqMaxSvaList,Map<String, Digraph<String>> deployReqDg) 
	{
		String conf = "<deployment>\n";
		
		//iterate the reqs list
		Iterator<Entry<String, String>> reqIt = reqMaxSvaList.entrySet().iterator();
	    while (reqIt.hasNext()) 
	    {     	
	    	Map.Entry<String, String> pairs = reqIt.next();
	    	String req = (String) pairs.getKey();
	    	String sva = (String) pairs.getValue();
	    	

	    	conf += getSvaWranglerInfo(sva);
	    	
	    	conf += getReqWranglerInfo(req,deployReqDg);
	    }
	    
		conf+="</deployment>\n";
		
		return conf;
	}

    //reqMaxSvaList<ReqUri,MaxSVAUri>
	//deployReqDg<ReqUri,SwStakDependGraph>
//	private static String mergeMaxSVAandGenerateInfConfigurationWrangler(Map<String, String> reqMaxSvaList,Map<String, Digraph<String>> deployReqDg) 
//	{
//		String conf = "";
//			
//		HashMap<String, HashMap<String, ArrayList<String>>> mergedSvas = mergeMaxSVA(reqMaxSvaList,deployReqDg);
//		
//		conf = generateWranglerConf(mergedSvas, deployReqDg);
//		
//		return conf;
//	}
	
	
	private static String generateWranglerConf(HashMap<String, HashMap<String, ArrayList<String>>> mergedSvas, Map<String, Digraph<String>> deployReqDg) 
	{
		String conf = "<deployment>\n";
		
		
		Iterator<Entry<String, HashMap<String, ArrayList<String>>>> svaIt = mergedSvas.entrySet().iterator();
		while(svaIt.hasNext())
		{
			Entry<String, HashMap<String, ArrayList<String>>> svaIdPair = svaIt.next();
			String sva = svaIdPair.getKey();
			
			//System.out.println("SVA="+sva);
			
			
			HashMap<String, ArrayList<String>> svaIdMap = svaIdPair.getValue();
			Iterator<Entry<String, ArrayList<String>>> svaIdIt = svaIdMap.entrySet().iterator();
			while(svaIdIt.hasNext())
			{
				Entry<String, ArrayList<String>> reqsPair = svaIdIt.next();
				String svaId = reqsPair.getKey();

				//System.out.println("--SVA_id="+svaId+":");
				
				//for each svaID we need to generate an SVA
				conf += getSvaWranglerInfo(sva);
				
				
				ArrayList<String> reqList = reqsPair.getValue();
				for(String req : reqList)
				{
					//System.out.println("----"+r);
			    	conf += getReqWranglerInfo(req,deployReqDg);
				}
				
				conf += getCloseSvaWranglerInfo();
			}
		}
		conf+="</deployment>\n";
		return conf;
	}


	

	private static boolean checkCompatibilityList(ArrayList<String> reqList1, ArrayList<String> reqList2) 
	{
		boolean res = true;
		return res;
	}




	//returns the image appliance (along with VM image and VM image id) that best suits the SVA
	//TODO up to now it only returns the first entry, we have to add the logic for selecting the best one
	private static QuerySolution getSvaImageAppliance(ResultSet qr) 
	{
		return qr.next();
	}


	public static String getSvaWranglerInfo(String sva)
	{
		//get the image appliance that best supports the SVA
    	ResultSet getSvaInfoQR = QueryExecute.execute(svaModel, QueryStrings.getSvaInfoQS, "sva_uri", sva);
    	QuerySolution iapp = getSvaImageAppliance(getSvaInfoQR);

    	String imageApp  = iapp.get("?iapp").toString();
    	String vmImage  = iapp.get("?vmimg").toString();
    	String vmImageId  = iapp.get("?vmid").toString();
    	String vmAppProvUri  = iapp.get("?prov").toString();
    	String vmAppProv = cv.getCloudProviderName(vmAppProvUri);
    	
    	//conf+="//Deployment Conf for Req " + req + "\n";
    	//conf+="//SVA=" + sva + "\n";
    	
    	String conf = "\t<node name=\""+svaModel.getResource(sva).getLocalName()+"\">\n";
    	
    	//conf+="//Image appliance=" + imageApp + "\n";
    	//conf+="//VmImage=" + vmImage + "\n";
    	//conf+="VM Id=" + vmImageId + "\n";
    	conf+="\t\t<provider name=\""+vmAppProv+"\">\n";
    	conf+="\t\t\t<image>"+vmImageId+"</image>\n";
    	conf+="\t\t\t<instance-type>"+DEFAULT_INSTANCE_TYPE+"</instance-type>\n";
    	conf+="\t\t</provider>\n";
    	
    	
    	return conf;
    	
    	
	}
	
	public static String getCloseSvaWranglerInfo()
	{
		String conf ="";
    	conf+="\t</node>\n";
		return conf;
	}
	
	public static String getReqWranglerInfo(String req, Map<String, Digraph<String>> deployReqDg)
	{
		String conf = "";
		//retrieve the Sw Components from SW catalog of each stack of the DGraph and add it to the conf.
    	Digraph<String> dg = deployReqDg.get(req);
    	
    	//iterate the list of stacks and get the SW comp for each one
    	Iterator<String> stackIt = dg.topSort().iterator();
    	while(stackIt.hasNext())
    	{
    		String stack = stackIt.next();
    		ResultSet getSwCompSwStackQR = QueryExecute.execute(swcModel, QueryStrings.getSwCompSwStackQS, "stack_uri", stack);
    		
    		//conf+="//SW Stack=" + stack + "\n";
    		
    		//for each SW comp we get its dep. plan and its configuration info.
    		while(getSwCompSwStackQR.hasNext())
    		{
    			QuerySolution qs = getSwCompSwStackQR.next();
    			String swc = qs.get("?swc").toString();
    			
    			//conf+="//--SW Comp=" + swc + "\n";
    			
    			
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
    			
    			String depPlan ="DEP_PLAN_STRING";
    			
    			ResultSet getDepStepsSwcCompQR = QueryExecute.execute(swcModel, QueryStrings.getDepStepsSwcCompQS, "swc_uri", swc);
    			while(getDepStepsSwcCompQR.hasNext())
	    		{
    				QuerySolution qsp = getDepStepsSwcCompQR.next();
	    			depPlan = qsp.get("?depplan").toString();
    				    				
    				String script = qsp.get("?script").asLiteral().getString();
	    			
    				//conf+="Script=" + script + "\n";
    				conf+="\t\t<plugin script=\""+script+"\">\n"; 
	    			
	    		}
    			
    			
    			//Get Dep Plan Conf Info (Deployment Info)
    			ResultSet getConfParDepPlanQR = QueryExecute.execute(swcModel, QueryStrings.getConfParDepPlanQS, "dp_uri", depPlan);
    			while(getConfParDepPlanQR.hasNext())
	    		{
    				QuerySolution qsp = getConfParDepPlanQR.next();
	    			//String confPar = qsp.get("?confpar").toString();
	    			
	    			
	    			String parName = qsp.get("?parname").asLiteral().getString();
	    			String parValue = qsp.get("?parvalue").asLiteral().getString(); //get the default value from de SWC
	    			
	    			//we get the value of the parName expressed in the requirements
	    			//of not found, we assume that the defaul value expressed in the
	    			//SWC catalogue must be used
	    			if(reqParNameValues.containsKey(parName))
	    			{
	    				parValue = reqParNameValues.get(parName);
	    			}
	    			else
	    			{
	    				System.err.println(PAR_NOT_FOUND_ERR_MSG + parName);
	    				System.err.println("from: " + reqParNameValues);
	    			}
	    			conf+="\t\t\t<param name=\""+parName+"\">"+parValue+"</param>\n"; 
	    			
	    		}
    			
    			
    			conf+="\t\t</plugin>\n";
    			
    			
    		}
    		
    	}
    	
    	
    	
    	return conf;
	}

	
	private static String getMaxSva(Map<String, Integer> svaIntList, int min)
	{
		String maxSva = NONE_SVA_FOUND_MSG;
		int max = min;
		
		Iterator svaIt = svaIntList.entrySet().iterator();
	    while (svaIt.hasNext()) 
	    {     	
	    	Map.Entry pairs = (Map.Entry)svaIt.next();
	    	String sva = pairs.getKey().toString();
	    	int i = ((Integer) pairs.getValue()).intValue();
	    	
	    	if(i>max)
	    	{
	    		max=i;
	    		maxSva=sva;
	    	}
	    	
	    }
		

    	System.out.println("\nSVA: " + maxSva + "\nINT: " + max + "\n-----------------");
		return maxSva;
	}




	private static String printReqDg(Map<String, Digraph<String>> r) 
	{
		String res ="";
		Iterator it = r.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Map.Entry pairs = (Map.Entry)it.next();
	    	String req = pairs.getKey().toString();
	    	Digraph<String> dg = (Digraph<String>) pairs.getValue();
	    	

	    	res+="* REQ: " + req + "\n";
	    	res+="* DG:" + dg + "\n";
	    	res+="* ----------------------------------------------------\n";
	    	
	    }	
	    return res;
	}



	private static String printMapList(	Map<String, ArrayList<String>> map) 
	{
		String res="";
		Iterator<Entry<String, ArrayList<String>>> it = map.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Map.Entry<String, ArrayList<String>> pairs = it.next();
	    	String req = pairs.getKey().toString();
	    	ArrayList<String> list = pairs.getValue();
	    	

	    	res+="\n";
	    	res+="* Key: " + req + "\n";
	    	res+="* List:" + "\n";
	    	for(String s : list)
	    	{
	    		res+="*\t-" + s.toString() + "\n";
	    	}   		
	    	res+="* ----------------------------------------------------\n";
	    	
	    }		
		return res;
	}
	


	private static String printMap(Map<String, String> map) 
	{
		String res="";
		Iterator<Entry<String, String>> it = map.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Map.Entry<String, String> pairs = it.next();
	    	String key = pairs.getKey().toString();
	    	String value = pairs.getValue();
	    	

	    	res+="\n";
	    	res+="* Key: " + key + "\n";
	    	res+="* Value:" + value + "\n";
	    	res+="* ----------------------------------------------------\n";
	    	
	    }		
		return res;
	}
	

	private static String printMapMap(	Map<String, Map<String,Integer>> map) 
	{
		String res="";
		Iterator<Entry<String, Map<String,Integer>>> it = map.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Map.Entry<String, Map<String,Integer>> pairs = it.next();
	    	String string1 = pairs.getKey().toString();
	    	Map<String,Integer> map2 = pairs.getValue();
	    	

	    	res+="\n";
	    	res+="* Key: " + string1 + "\n";
	    	res+="* List:" + "\n";
	    	
	    	Iterator<Entry<String,Integer>> it2 = map2.entrySet().iterator();
		    while (it2.hasNext()) 
		    {
		    	Map.Entry<String, Integer> pairs2 = it2.next();
		    	String string2 = pairs2.getKey().toString();
		    	Integer intValue = pairs2.getValue();
		    	res+="*\tKEY:" + string2.toString() + "\n";
		    	res+="*\tVALUE:" + intValue.toString() + "\n";
		    }
		
	    	res+="* ----------------------------------------------------\n";
	    	
	    }		
		return res;
	}
	
	
	public static void getStacksDependencies(Digraph<String> dg, ArrayList<String> stacks)
	{
		for(String swt : stacks) //iterate each stack to obtain its dependencies
		{
			//we add a vertex for the current stack (if it is already included nothing happens)
			dg.add(swt);
			System.out.println("Retrieving deps for: " + swt);
	    	
			
			//get all the dependencies of the current Stack
			ResultSet getSwStackDependenciesRS = QueryExecute.execute(swcModel, QueryStrings.getSwStackDependenciesQS, "swt", swt);
			ArrayList<String> swtDepList = new ArrayList<String>();
			
			while(getSwStackDependenciesRS.hasNext())//iterate all the obtained dependencies
		    {
				QuerySolution qs = getSwStackDependenciesRS.next();
				String swt2 = qs.get("?swt2").toString();

				System.out.println("-- Got deps for: " + swt2);
				
				//we only add a stack to the list of neighbors if it has NOT been considered yet
				//this is necessary to avoid infinite loops over circular dependencies
				if(!dg.contains(swt2))
				{
					swtDepList.add(swt2);
				}
				
				//add a edge for each dependecy
				dg.add(swt, swt2);
		    }
			getStacksDependencies(dg,swtDepList);
		}
	}
	

	private static Map<String, Map<String, Integer>> getCompatibleSva(Map<String, Digraph<String>> reqStackDiagraph,	Map<String, HashSet<String>> svaList) 
	{
		
		Map<String,Map<String,Integer>> reqSvaIntersection = new HashMap<String,Map<String,Integer>>();
		Iterator<Entry<String, Digraph<String>>> itReq = reqStackDiagraph.entrySet().iterator();
	    while (itReq.hasNext()) 
	    {
	    	Entry<String, Digraph<String>> pairsReq = itReq.next();
	    	String req = pairsReq.getKey().toString();
	    	Digraph<String> dg = (Digraph<String>) pairsReq.getValue();
			
	    	Map<String,Integer> svaIntList = new HashMap<String,Integer>();
	    	
	    	Iterator<Entry<String, HashSet<String>>> itSva = svaList.entrySet().iterator();
		    while (itSva.hasNext()) 
		    {
		    	Entry<String, HashSet<String>> pairsSva = itSva.next();
		    	String sva = pairsSva.getKey().toString();
		    	HashSet<String> stackList = (HashSet<String>) pairsSva.getValue();
		    	
		    	int intCount = getDigraphAndStackIntersection(dg,stackList);
		    	svaIntList.put(sva, new Integer(intCount));
		    }
	    	
		    reqSvaIntersection.put(req, svaIntList);
	    }
		
	    return reqSvaIntersection;
	}
	

	

	
    //we obtain a list Map<WF, Map<SVA, Integer>> that relates the number of stack that are common
    //to both, the WF's reqs and the sva
	private static Map<String, Map<String, Integer>> getCompatibleSvaPropagated(Map<String, ArrayList<String>> wfPropReqs, Map<String, Digraph<String>> reqStackDiagraph,Map<String, HashSet<String>> svaList) 
	{
		
		//get the compatibility between each req and sva
		// list<REQ,list<SVA,int>>
		Map<String, Map<String, Integer>> reqSvaIntersection = getCompatibleSva(reqStackDiagraph,	svaList);
		
		Map<String,Map<String,Integer>> wfSvaIntersection = new HashMap<String,Map<String,Integer>>();
		
		//we traverse the list of WFs to obtain their requirements
		Iterator<Entry<String, ArrayList<String>>> itWf = wfPropReqs.entrySet().iterator();
		 while (itWf.hasNext()) 
	    {
	    	Entry<String, ArrayList<String>> pairsWf = itWf.next();
	    	String wf = pairsWf.getKey().toString();
	    	List<String> reqList = (List<String>) pairsWf.getValue();
	    	
	    	Map<String, Integer> svaIntMap = new HashMap<String, Integer>(); 
			wfSvaIntersection.put(wf, svaIntMap);
	    	
	    	for(String req : reqList)
	    	{
	    		Map<String, Integer> svaReqIntMap = reqSvaIntersection.get(req);
	    		//we traverse the list of WFs to obtain their requirements
	    		Iterator<Entry<String, Integer>> svaIt = svaReqIntMap.entrySet().iterator();
	    		while(svaIt.hasNext())
	    		{
	    			Entry<String, Integer> pairsSva = svaIt.next();
	    	    	String sva = pairsSva.getKey().toString();
	    	    	Integer intValue =  pairsSva.getValue();
	    	    	
	    			if(wfSvaIntersection.get(wf).containsKey(sva))
	    			{
	    				//add the value to the intersection
	    				Integer oldIntValue = wfSvaIntersection.get(wf).get(sva);
	    				wfSvaIntersection.get(wf).put(sva,intValue+oldIntValue);
	    			}
	    			else
	    			{
	    				//create a new entry
	    				wfSvaIntersection.get(wf).put(sva,intValue);
	    			}
	    		}
	    		
	    	}
	    	
	    	
	    }
		
	    return wfSvaIntersection;
	}




	private static int getDigraphAndStackIntersection(Digraph<String> dg,HashSet<String> stackList) 
	{
		int intCount = 0;
		for(String stack : stackList)
		{
			if(dg.contains(stack))
			{
				intCount++;
			}
		}
		return intCount;
	}
	

	//remove the stacks already available in an SVA from the Digraph associated to each req
	private static Map<String, Digraph<String>> substractStackFromReqDg(Map<String, String> reqMaxSvaList, Map<String, Digraph<String>> reqStackDigraph,	Map<String, HashSet<String>> svaList) 
	{
		Map<String, Digraph<String>> res = new 	HashMap<String, Digraph<String>>();
		
		Iterator reqMaxSvaListIt = reqMaxSvaList.entrySet().iterator();
	    while (reqMaxSvaListIt.hasNext()) 
	    {
	    	Map.Entry pairs = (Map.Entry)reqMaxSvaListIt.next();
	    	
	    	//get the req and the sva
	    	String req = (String)pairs.getKey();
	    	String maxSva = (String)pairs.getValue();
		
	    	//get the DG related to req
	    	Digraph<String> reqDg = reqStackDigraph.get(req);
	    	
	    	//get the stack associated to the sva
	    	HashSet<String> svaStacks = svaList.get(maxSva);
	    	
	    	//iterate the svaStacks list to remove their individuals from the graph
	    	for(String stack : svaStacks)
	    	{
	    		removeStackFromDg(stack, reqDg);
	    	}
	    	
	    	res.put(req, reqDg);
	    	
	    }
		
		
		return res;
	}
	

	

	//remove the stacks already available in an SVA from the Digraphs associated to each req associated with each WF
	//we will get a map<WF,map<REQ,DIGRAPH> relating each WF with a list of <REQS,DIGRAPH>
	//each <REQ,DIGRAPH> pair will contain the stack graph for a requirement without stacks the already available in the selected SVA
	//notice that two requirements can appear twice (e.g. PEGASUS_WMS), but have different graphs, as they are being deployed in different SVAs
	private static Map<String, LinkedHashMap<String, Digraph<String>>> substractStackFromWfReqDg(Map<String, String> wfMaxSvaList, Map<String, ArrayList<String>> propagatedWfReqs, Map<String, Digraph<String>> reqStackDigraph,	Map<String, HashSet<String>> svaList) 
	{
		//map<WF,map<REQ,DIGRAPH>
		Map<String, LinkedHashMap<String, Digraph<String>>> res = new HashMap<String, LinkedHashMap<String,Digraph<String>>>();
		
		Iterator wfMaxSvaListIt = wfMaxSvaList.entrySet().iterator();
	    while (wfMaxSvaListIt.hasNext()) 
	    {
	    	Map.Entry pairs = (Map.Entry)wfMaxSvaListIt.next();
	    	
	    	//get the req and the sva
	    	String wf = (String)pairs.getKey();
	    	String maxSva = (String)pairs.getValue();
		
	    	//get the reqs realted to the WF
	    	ArrayList<String> wfReqs = propagatedWfReqs.get(wf);
	    	System.out.println("--->>>GETTING:" + wfReqs);
	    	
			//create an entry for the WF
	    	res.put(wf, new LinkedHashMap<String, Digraph<String>>());
	    	
	    	for(String req : wfReqs)
	    	{
		    	//get the DG related to req
		    	Digraph<String> reqDg = reqStackDigraph.get(req);
		    	
		    	//get the stack associated to the sva
		    	HashSet<String> svaStacks = svaList.get(maxSva);
		    	
		    	//iterate the svaStacks list to remove their individuals from the graph
		    	for(String stack : svaStacks)
		    	{
		    		removeStackFromDg(stack, reqDg);
		    	}	
		    	

		    	System.out.println("----->>>ADDING req:" + req + " \n to wf:" + wf + "\ndigraph:" + reqDg + "\n\n");
		    	res.get(wf).put(req, reqDg);
	    	}
	    	
	    }
		
		return res;
	}




	private static void removeStackFromDg(String stack, Digraph<String> reqDg) 
	{
		//if stack is not in reqDg there is nothing to do, bye!
		if(!reqDg.contains(stack))
			return;
		
		System.out.println("~~~~~Removing " + stack);
		
		//remove income edges
		reqDg.removeInEdges(stack);
		
		//get neighbors
		List<String> l = reqDg.getNeighbors(stack);
		
		//remove vertex and out edges
		reqDg.removeVertex(stack);
		
		//check for neighbors
		//we only remove those that have no income edges
		//as no other component depends on them
		for(String i : l)
		{
			//System.out.println("inDegree of " + i + "=" + reqDg.inDegree(i));
			if(reqDg.inDegree(i)==0)
			{
				removeStackFromDg(i,reqDg);
			}
		}
	}
	
	private static String printMapMapDigraph(Map<String, LinkedHashMap<String, Digraph<String>>> map) 
	{
		String res="";
		Iterator<Entry<String, LinkedHashMap<String, Digraph<String>>>> it = map.entrySet().iterator();
	    while (it.hasNext()) 
	    {
	    	Entry<String, LinkedHashMap<String, Digraph<String>>> pairs = it.next();
	    	String string1 = pairs.getKey().toString();
	    	Map<String, Digraph<String>> map2 = pairs.getValue();
	    	

	    	res+="\n";
	    	res+="* Key: " + string1 + "\n";
	    	res+="* List:" + "\n";
	    	
	    	Iterator<Entry<String, Digraph<String>>> it2 = map2.entrySet().iterator();
		    while (it2.hasNext()) 
		    {
		    	Entry<String, Digraph<String>> pairs2 = it2.next();
		    	String string2 = pairs2.getKey().toString();
		    	Digraph<String> graph = pairs2.getValue();
		    	res+="*\tKEY:" + string2.toString() + "\n";
		    	res+="*\tVALUES:\n";
		    	
		    	//res+="*\t-" + graph.toString() + "\n";
		    	  		
		    }
		
	    	res+="* ----------------------------------------------------\n";
	    	
	    }		
		return res;
	}
	
	
}
