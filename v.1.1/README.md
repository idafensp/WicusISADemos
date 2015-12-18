WicusISADemos
=============

Demos of the WICUS Infrastructure Specification Algorithm

Version 1.1

- Usage:

> java -jar WicusISA.jar conf.txt


Modify your conf.txt file to include ALL the following parameters:

- WF_URI: the URI of the individual defininf the workflow to study (e.g. http://purl.org/net/wicus-reqs/resource/Workflow/soykb_WF);

- WFI_DATASET_PATH: Path to the RDF file containing the annotations about the workflow infrastructure requirements.

- SWC_DATASET_PATH: Path to the RDF file containing the annotations about the software components involved in the execution of the annotated workflows.

- SVA_DATASET_PATH: Path to the RDF file containing the annotations about the available virtual appliances.

- CLOUD_PROV_FILE: Path to the text file containing a list of URIs defining the available Cloud Providers.

- WF_EXEC_FILE_PRECIP: Path to the text file containing the PRECIP commands for workflow execution (only when working with Precip)

- WF_EXEC_FILE_VAGRANT: Path to the text file containing the PRECIP commands for workflow execution (only necessary when working with Vagrant)

- OUT_PRECIP_FILE: Path to the output Vagrant file (only necessary when working with Precip)

- OUT_VAGRANT_FILE: Path to the output Vagrant file (only necessary when working with Vagrant)

- SSH_TYPE: either "dsa" or "rsa", depending on the workflow.

Sample values for each parameters are provided in this demo. Comments are allowed by introducing a '#' at the beggining of the line.