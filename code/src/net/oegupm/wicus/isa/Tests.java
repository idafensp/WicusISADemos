package net.oegupm.wicus.isa;

import java.util.List;

import net.oegupm.wicus.isa.utils.Digraph;

public class Tests {

	public static void main(String[] args) {

      // Create a Graph with Integer nodes
      Digraph<Integer> graph = new Digraph<Integer>();
      graph.add(0, 1); graph.add(0, 2); graph.add(0, 3);
      graph.add(1, 2); graph.add(1, 3); graph.add(2, 3);
      graph.add(2, 4); graph.add(4, 5); graph.add(5, 6);    // Tetrahedron with tail
      graph.add(1,7);

      System.out.println("ORIGINAL:\n---------------------------\n"+graph);
      
      
      removeStackFromDg(1,graph);

      System.out.println("AFTER:\n---------------------------\n"+graph);

	}
	
	private static void removeStackFromDg(Integer stack, Digraph reqDg) 
	{
		//remove income edges
		reqDg.removeInEdges(stack);
		
		//get neighbors
		List<Integer> l = reqDg.getNeighbors(stack);
		
		//remove vertex and out edges
		reqDg.removeVertex(stack);
		
		//check for neighbors
		//we only remove those that have no income edges
		//as no other component depends on them
		for(Integer i : l)
		{
			System.out.println("inDegree of " + i + "=" + reqDg.inDegree(i));
			if(reqDg.inDegree(i)==0)
			{
				removeStackFromDg(i,reqDg);
			}
		}
		
	}

}
