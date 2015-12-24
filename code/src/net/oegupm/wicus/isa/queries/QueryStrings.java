package net.oegupm.wicus.isa.queries;

public class QueryStrings {

	//get the set of pairs <CW,REQ> relating each concrete WF with its requirements
	public static final String getWfConcreteWfReqsQS = "select distinct ?cw ?swrq where{"+
			"?cw <http://purl.org/net/wicus-reqs#requiresExecutionEnvironment> ?swrq."+
			"?swrq a <http://purl.org/net/wicus-reqs#SoftwareRequirements>."+
			"{"+
			"  ?wf_uri <http://purl.org/net/wicus-reqs#hasSubworkflow> ?cw."+
			"}"+
			"UNION"+
			"{"+  
			"   ?wf_uri <http://purl.org/net/wicus-reqs#hasSubworkflow> ?aw."+
			"   ?aw <http://purl.org/net/wicus-reqs#hasConcreteWorkflow> ?cw."+
			"}"+
			"}";

	//get the concrete subworkflows of a given wf (wf_uri) 
	public static final String getWfConcreteSubWfQS = "select distinct ?swf where{"+
			"{"+
			"  ?wf_uri <http://purl.org/net/wicus-reqs#hasSubworkflow> ?swf."+
			"}"+
			"UNION"+
			"{"+  
			"   ?wf_uri <http://purl.org/net/wicus-reqs#hasSubworkflow> ?aw."+
			"   ?aw <http://purl.org/net/wicus-reqs#hasConcreteWorkflow> ?swf."+
			"}"+
			"}";


	//get the set of pairs <WF,REQ> relating the top level WF with its requirements (usually WMS)
	public static final String getTopWfReqsQS = "select distinct ?swrq where{"+
			"?wf_uri <http://purl.org/net/wicus-reqs#requiresExecutionEnvironment> ?swrq."+
			"?swrq a <http://purl.org/net/wicus-reqs#SoftwareRequirements>."+
			"}";
	
	//get the SW stack that compose a requirement	
	public static final String getReqSwStackQS = "select distinct ?swst where{"+
			"?req_uri <http://purl.org/net/wicus#composedBySoftwareStack> ?swst."+
			"}";


	//get all the dependencies between SW stacks
	public static final String getSwStackDependenciesQS = "select distinct ?swt2 where{"+
			" ?swt <http://purl.org/net/wicus-stack#dependsOn> ?swt2."+
			"}";
	
	//get all the SVAs and their stack from a given provider (?prov_uri)
	public static final String getSvaAndStackProviderQS = "select distinct ?sva ?stack where{ "+
			"  ?sva a <http://purl.org/net/wicus-sva#ScientificVirtualAppliance>."+
			"  ?sva <http://purl.org/net/wicus-sva#isSupportedBy> ?iapp. "+
			"  ?iapp <http://purl.org/net/wicus-sva#providedBy> ?prov_uri. "+
			"  ?sva <http://purl.org/net/wicus#hasSoftwareStack> ?stack." +
			"}";

	
	//get the software components of a given sotf. stack (?soft_stack)
	public static final String getSwCompSwStackQS = "select distinct ?swc where{ "+
			" ?swc a <http://purl.org/net/wicus-stack#SoftwareComponent>."+
			" ?stack_uri <http://purl.org/net/wicus-stack#hasSoftwareComponent> ?swc."+
			"}";
	


	
	//get the version of a given software component (?swc_uri)
	public static final String getVersionSwCompQS = "select distinct ?vn where{ " +
			" ?swc_uri <http://purl.org/net/wicus-stack#hasVersion> ?version ." +
			"?version <http://purl.org/net/wicus-stack#versionNumber> ?vn ." +
			"}";
	
	//get the image appliance, WM image, and VM image id of a given SVA (?sva_uri)
	public static final String getSvaInfoQS = " select distinct  ?iapp ?vmimg ?vmid ?prov ?hwcpu ?hwmem ?hwstorage  where{ " +
			"    ?sva_uri a <http://purl.org/net/wicus-sva#ScientificVirtualAppliance>. " +
			"    ?sva_uri <http://purl.org/net/wicus-sva#isSupportedBy> ?iapp. " +
			"    ?iapp <http://purl.org/net/wicus-sva#providedBy> ?prov. " +
			"    ?iapp <http://purl.org/net/wicus-sva#hasVMImage> ?vmimg. " +
			"    ?vmimg <http://purl.org/net/wicus-sva#vmId> ?vmid. " +
			"    ?iapp <http://purl.org/net/wicus#hasHardwareSpecs> ?hwspec . " +
			"    ?hwspec <http://purl.org/net/wicus-hwspecs#hasHardwareComponent> ?hwcpu . " +
			"    ?hwcpu a <http://purl.org/net/wicus-hwspecs#CPU> . " +
			"    ?hwspec <http://purl.org/net/wicus-hwspecs#hasHardwareComponent> ?hwmem . " +
			"    ?hwmem a <http://purl.org/net/wicus-hwspecs#Memory> . " +
			"    ?hwspec <http://purl.org/net/wicus-hwspecs#hasHardwareComponent> ?hwstorage . " +
			"    ?hwstorage a <http://purl.org/net/wicus-hwspecs#Storage> . " +
			"  }";
	
	//get the configuration parameteres of a given SW comp (?swc_uri)
	public static final String getConfParSwCompQS = "select distinct ?confpar ?parname ?parvalue where{"+
			" ?swc_uri <http://purl.org/net/wicus-stack#hasConfigurationInfo> ?conf."+
			" ?confpar <http://purl.org/net/wicus-stack#isConfigurationParameterOf> ?conf."+
			" ?confpar <http://purl.org/net/wicus-stack#parameterName> ?parname."+
			" ?confpar <http://purl.org/net/wicus-stack#parameterValue> ?parvalue."+ 
			"}";
	
	//get the configuration parameteres of a given Dep Plan (?dp_uri)
	public static final String getConfParDepPlanQS = "select distinct ?confpar ?parname ?parvalue where{"+
			" ?dp_uri <http://purl.org/net/wicus-stack#hasDeploymentInfo> ?conf."+
			" ?confpar <http://purl.org/net/wicus-stack#isConfigurationParameterOf> ?conf."+
			" ?confpar <http://purl.org/net/wicus-stack#parameterName> ?parname."+
			" ?confpar <http://purl.org/net/wicus-stack#parameterValue> ?parvalue."+ 
			"}";
	
	//get the deployment plan and its steps of a given SW comp (?swc_uri)
	public static final String getDepStepsSwcCompQS = "select distinct ?script ?depplan ?depstep where{"+
			" ?depscript <http://purl.org/net/wicus-stack#script> ?script."+
			" ?depstep <http://purl.org/net/wicus-stack#hasDeploymentScript> ?depscript."+
			" ?depstep <http://purl.org/net/wicus-stack#isDeploymentStepOf> ?depplan."+
			" ?depplan <http://purl.org/net/wicus-stack#isDeploymentPlanOf> ?swc_uri."+
			"}";
	
	//get the deployment steps of a given plan(?dp_uri)
	public static final String getDepStepsDepPlanQS = "select distinct ?depstep where{"+
			" ?depstep <http://purl.org/net/wicus-stack#isDeploymentStepOf> ?dp_uri."+
			"}";
	
	//get the deployment steps linked by a nextStep
	public static final String getNextDepStepsDePlanQS = "select distinct ?step1 ?step2 where{"+
			" ?step1 <http://purl.org/net/wicus-stack#nextStep> ?step2."+
			" ?step1 <http://purl.org/net/wicus-stack#isDeploymentStepOf> ?dp_uri."+
			" ?step2 <http://purl.org/net/wicus-stack#isDeploymentStepOf> ?dp_uri."+
			"}";
	
	//get the deployment script of a given step(?ds_uri)
	public static final String getScriptDepStepQS = "select distinct ?script where{"+
			" ?depscript <http://purl.org/net/wicus-stack#script> ?script."+
			" ?ds_uri <http://purl.org/net/wicus-stack#hasDeploymentScript> ?depscript."+
			"}";
	
	//get the deployment script and dep info of a given step(?ds_uri)
	public static final String getScriptConfInfoDepStepQS = "select distinct ?script ?conf where{"+
			" ?depscript <http://purl.org/net/wicus-stack#script> ?script."+
			" ?ds_uri <http://purl.org/net/wicus-stack#hasDeploymentScript> ?depscript."+
			" ?ds_uri <http://purl.org/net/wicus-stack#hasDeploymentInfo> ?conf."+
			"}";
	
	//get the configuration parameteres of a given conf info (?ci_uri)
	public static final String getConfParConfInfoQS = "select distinct ?confpar ?parname ?parvalue where{"+
			" ?confpar <http://purl.org/net/wicus-stack#isConfigurationParameterOf> ?ci_uri."+
			"}";
	
	
	//get the configuration parameteres values of a given parameter (?cp_uri)
	public static final String getParsConfParQS = "select distinct ?parname ?parvalue where{"+
			" ?cp_uri <http://purl.org/net/wicus-stack#parameterName> ?parname."+
			" ?cp_uri <http://purl.org/net/wicus-stack#parameterValue> ?parvalue."+ 
			"}";

	
	//get the deployment plan of a given SW comp (?swc_uri)
	public static final String getDepPlanSwcCompQS = "select distinct ?depplan where{"+
			" ?depplan <http://purl.org/net/wicus-stack#isDeploymentPlanOf> ?swc_uri."+
			"}";
	
	//get the hw requirements' components of a given wf (?wf_uri)
	public static final String getWfHwReqQS = "select distinct ?hwcpu ?hwmem ?hwstorage where { "+
			"  ?wf_uri <http://purl.org/net/wicus-reqs#requiresExecutionEnvironment> ?hwreq ."+
			"  ?hwreq a <http://purl.org/net/wicus-reqs#HardwareRequirements> ."+
			"  ?hwreq <http://purl.org/net/wicus#composedByHardwareSpec> ?hwspec ."+
			"  ?hwspec <http://purl.org/net/wicus-hwspecs#hasHardwareComponent> ?hwcpu ."+
			"  ?hwcpu a <http://purl.org/net/wicus-hwspecs#CPU> ."+
			"  ?hwspec <http://purl.org/net/wicus-hwspecs#hasHardwareComponent> ?hwmem ."+
			"  ?hwmem a <http://purl.org/net/wicus-hwspecs#Memory> ."+
			"  ?hwspec <http://purl.org/net/wicus-hwspecs#hasHardwareComponent> ?hwstorage ."+
			"  ?hwstorage a <http://purl.org/net/wicus-hwspecs#Storage> ."+
			"}";
	
	//get the feature values requirements of a given hw component (?hw_uri)
	public static final String getHwFeatValueQS = "select distinct ?value ?unit where { "+
			"  ?hw_uri <http://purl.org/net/wicus-hwspecs#hasFeature> ?feat ."+
			"  ?feat <http://purl.org/net/wicus-hwspecs#value> ?value ."+
			"  ?feat <http://purl.org/net/wicus-hwspecs#unit> ?unit . "+
			"}";
	   
	
}
