/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sparqlbuilder.data;

/**
 *
 * @author atsuko
 */
import java.util.*;
import java.io.Serializable;

public class OWLClassGraph extends LabeledMultiDigraph implements Serializable{
    int nsteps = 4;
    //int limit = 100;
    
    List<String> nodeType;
    String sparqlEndpoint;
    Set<Integer> visited;
    List<Map<Integer, Integer>> edgeweight;
    List<Integer> nodeweight;
    Map<String, Boolean> checkedpaths;
    
    public class LinkAndPath{
        String originalClassURI; // originalClasssURI -classLink.propertyURI-> classLink.linkedClassURL
        ClassLink classLink;
        List<ClassLink> path;
        Set<String> classURIs; // apearing class URIs in the path
        
        
        public LinkAndPath(ClassLink classLink, List<ClassLink> path){
           this.classLink = classLink;
           this.path = path;
        }
        
        public LinkAndPath(ClassLink classLink, List<ClassLink> path, String originalClassURI){
           this.classLink = classLink;
           this.path = path;
           this.originalClassURI = originalClassURI;
        }

        public LinkAndPath(ClassLink classLink, List<ClassLink> path, String originalClassURI, Set<String> classURIs){
           this.classLink = classLink;
           this.path = path;
           this.originalClassURI = originalClassURI;
           this.classURIs = classURIs;
        }
    }

    public OWLClassGraph(){ // not used
        super();
        nodeType = new LinkedList<String>();
    }
    
    public OWLClassGraph(String ep, Set<String> cl, Set<String> cr){ // used
        super();
        sparqlEndpoint = ep;
        setClassGraph(cl, cr);
    }

    public int getNumberOfEdge(String url){
        Integer node = labelednodes.get(url);
        if (node == null){
            return 0;
        }
        return adjlist.get(node).size();
    }
    
    public boolean visitedNode(String classURI){
        if ( visited.contains(labelednodes.get(classURI)) ){
            return true;
        }
        return false;
    }
    
    /*
    private boolean checkPath(String startClass, String endClass){
        Integer snode = labelednodes.get(startClass);
        Integer enode = labelednodes.get(endClass);
        return checkSimplePath(snode, enode);
    }
    */
    /*
    private boolean checkSimplePath(Integer snode, Integer enode){
        List<List<Integer>> simplePaths = new LinkedList<>();
        List<List<Integer>> lp = new LinkedList<>();
        List<Integer> ini = new LinkedList<Integer>(); // initial path
        ini.add(snode);
        lp.add(ini);
        for (int i = 0; i < nsteps; i++ ){
            ListIterator<List<Integer>> lit = lp.listIterator();
            List<List<Integer>> nextlp = new LinkedList<>();
            while ( lit.hasNext() ){ 
                List<Integer> crrpath = lit.next();
                Integer crrnode = crrpath.get(crrpath.size()-1);
                Set<Integer> nexts = gadjlist.get(crrnode).keySet();
                Iterator<Integer> nit = nexts.iterator();
                while( nit.hasNext() ){
                    Integer nextnode = nit.next();
                    if ( crrpath.contains(nextnode) ){ continue; }
                    List<Integer> nextpath = new LinkedList<Integer>(crrpath); // copy
                    nextpath.add(nextnode);
                    if ( nextnode.equals(enode) ){
                        return true;
                        //simplePaths.add(nextpath);
                        //continue;
                    }
                    nextlp.add(nextpath);
                }
	    }
            lp = nextlp;
        }        
        return false;
    }*/
    
    private void setClassGraph(Set<String> cl, Set<String> crel){
        Iterator<String> cit = cl.iterator();
        while( cit.hasNext()){
            String c = cit.next();
            addNode(c);
            //nodeType.add("class");
            //nodeweight.add(10); // tmp           
        }
        Iterator<String> crit = crel.iterator();
        while( crit.hasNext() ){
            String cr = crit.next();
            String[] data = cr.split("\t");
            if (data.length < 3 ){ 
                continue;
            }
            Integer n1 = labelednodes.get(data[0]);
            Integer n2 = labelednodes.get(data[1]);
            addEdge(n1, n2, new ClassLink(data[2], data[1], null, Direction.forward, 
				10, 10, 10,
				10, 10,
				false, false));
            addEdge(n2, n1, new ClassLink(data[2], data[0], null, Direction.reverse, 
				10, 10, 10,
				10, 10,
				false, false));
        }
    }
/*    
    private void setClassGraph(RDFSchemaAnalyzer rdfsa){
        // setNodes
        SClass[] classes = null;
        try{
            classes = rdfsa.getOWLClasses(null, null, null, true);
        }catch(Exception e){
            System.err.println(e); return;
        }
        for (int i = 0 ; i < classes.length; i++){
            addNode(classes[i].getClassURI());
            nodeType.add("class");
            nodeweight.add(classes[i].getNumOfInstances());           
        }
        // setEdges
        for (int i = 0 ; i < classes.length; i++ ){
            try{
                ClassLink[] classLinks = rdfsa.getNextClass(null, classes[i].getClassURI(), 10000, true);
                for (int j = 0 ; j < classLinks.length; j++){
                    Integer n = labelednodes.get(classLinks[j].getLinkedClassURI());
                    if ( n != null ){
                        addEdge(i, n, classLinks[j]);
                    }else{
                        n = labelednodes.get(classLinks[j].getLinkedLiteralDatatypeURI());
                        if ( n == null ){
                           addNode(classLinks[j].getLinkedLiteralDatatypeURI());
                           n = nodeType.size();
                           nodeType.add("literal");
                        }
                        addEdge(i, n, classLinks[j]);
                    }
                }
            }catch(Exception e){
                System.err.println(e);
            }
        }
    }

    public void setPartClassGraph(RDFSchemaAnalyzer rdfsa, String sparqlEndpoint, String startClass){
        // set endpoint
        this.sparqlEndpoint = sparqlEndpoint;
        visited = new HashSet<Integer>();
        nodeweight = new LinkedList<Integer>();
        // setNodes for all classes
        SClass[] classes = null;
        try{
           classes = rdfsa.getOWLClasses(null, null, null, true);
        }catch(Exception e){
           System.err.println(e); return;
        }
        for (int i = 0 ; i < classes.length; i++){
           addNode(classes[i].getClassURI());
           nodeType.add("class");
           nodeweight.add(classes[i].getNumOfInstances());
        }
        // setEdges
        Integer snode = labelednodes.get(startClass);
        Set<Integer> nodes = new HashSet<Integer>();
        nodes.add(snode);
        visited.add(snode);
        for (int i = 0 ; i < nsteps; i++ ){
            Iterator<Integer> nit = nodes.iterator();
            Set<Integer> nextnodes = new HashSet<Integer>();
            while ( nit.hasNext() ){
                Integer crr = nit.next();
                try{
                    ClassLink[] classLinks = rdfsa.getNextClass(null, labels.get(crr), 10000, true);
                    for (int j = 0 ; j < classLinks.length; j++){
                        Integer nn = labelednodes.get(classLinks[j].getLinkedClassURI());
                        if ( nn == null ){
                            continue;
                        }
                        if ( !visited.contains(nn) ){
                            nextnodes.add(nn);
                            //visited.add(nn);
                        }
                        addEdge(crr, nn, classLinks[j]);
                        //updateWeight(crr, nn, classLinks[j]);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            nodes = nextnodes;
            visited.addAll(nodes);
            if ( visited.size() > labelednodes.size()){
                System.out.println();
            }
        }
    }
*/

    public List<String> getReachableClasses(String st){
        List<String> clURIs = new LinkedList<String>();
        Integer snode = labelednodes.get(st);
        Set<Integer> vnodes = new HashSet<Integer>();
        Set<Integer> cnodes = new HashSet<Integer>();
        vnodes.add(snode);
        cnodes.add(snode);
        for (int i = 0 ; i < nsteps; i++ ){
            Iterator<Integer> nit = cnodes.iterator();
            Set<Integer> nextnodes = new HashSet<Integer>();
            while ( nit.hasNext() ){
                Integer crr = nit.next();
                nextnodes = this.gadjlist.get(crr).keySet();
            }
            cnodes = nextnodes;
            vnodes.addAll(cnodes);
        }
        // vnode->urls
        Iterator<Integer> vit = vnodes.iterator();
        while(vit.hasNext()){
            Integer vn = vit.next();
            if (vn.equals(snode)){continue;}
            clURIs.add(this.labels.get(vn));
        }
        return clURIs;
    }
}
