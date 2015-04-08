WicusISADemos
=============

Demos of the WICUS Infrastructure Specification Algorithm

Version 0.3

- Usage:

> java -jar WicusISA.jar conf.txt


Modify your conf.txt file to include ALL the following parameters:

- WFI_DATASET_PATH: Path to the RDF file containing the annotations about the workflow infrastructure requirements.

- SWC_DATASET_PATH: Path to the RDF file containing the annotations about the software components involved in the execution of the annotated workflows.

- SVA_DATASET_PATH: Path to the RDF file containing the annotations about the available virtual appliances.

- CLOUD_PROV_FILE: Path to the text file containing a list of URIs defining the available Cloud Providers.

- WF_EXEC_FILE: Path to the text file containing the PRECIP commands for workflow execution (if available)

Sample values for each parameters are provided in this demo. Comments are allowed by introducing a '#' at the beggining of the line.