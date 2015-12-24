package net.oegupm.wicus.isa.queries;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;

public class QueryExecute {
	
	public static ResultSet execute(OntModel mod, String qs, String paramVar, String paramVal)
	{
		//load, parameterize and create the query 
		ParameterizedSparqlString queryStr = new ParameterizedSparqlString(qs);
		queryStr.setIri(paramVar, paramVal);
		Query query = QueryFactory.create(queryStr.toString());
		
		
		//execute the query
		QueryExecution qe = QueryExecutionFactory.create(query, mod);
	    com.hp.hpl.jena.query.ResultSet results =  qe.execSelect();
		
	    // Output query results    
	    //ResultSetFormatter.out(System.out, results, query);
	    
	    
//	    //close the query execution connection
//	    qe.close(); //if we close it we will not be able to access the results...
	    
		return results;
	}
	

}
