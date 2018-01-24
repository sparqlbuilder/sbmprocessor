/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sparqlbuilder.data;

import java.io.*;
import java.util.*;
import org.apache.jena.rdf.model.*;

/**
 *
 * @author atsuko
 */
public class SBLDMain {

    /**
     * @param args the command line arguments
     */
    static String workdir = "./cc/";
    static String inputdir = "./ci";
    static String ontdir = "./ont"; 

    static String clfile = "cl.txt"; // cl url \tab cl label \tab ep
    static String epfile = "ep.txt";
        
    public static void main(String[] args) {
        boolean addmode = false;
        if (args[0].equals("a") || args[0].equals("add")){
            addmode = true;
        }
        
        File ifile = new File(inputdir);
        File[] ifiles = null;
        List<String> cl = new LinkedList<>(); 

        if (ifile.isDirectory()){
            ifiles = ifile.listFiles();
        }else if (ifile.isFile()){
            ifiles = new File[1];
            ifiles[0] = ifile;
        }else{
            System.err.println("Directory may be wrong.");
        }

        Map<String, String> cltmp = new HashMap<>();
        Set<String> eps = new HashSet<>();
        Set<String> bc = getBlackClasses();
        //Map<String, String> epmap = getEPMap();// should be removed
        File fcl = new File(workdir.concat(clfile));

        for (File efile : ifiles) {
            System.out.println(efile.getName());
            // For each file
            Model m = getModel(efile);
            Set<String> nc = new HashSet<String>(); // #ent <= 1
            // get ep 
            Property epp = m.getProperty(SBMURLs.EP);   
            NodeIterator niep = m.listObjectsOfProperty(epp);
            String ep = "";
            while (niep.hasNext()){
                RDFNode node = niep.next();
                if (node.isResource()){
                    ep = node.asResource().getURI();
                }else if (node.isLiteral()){
                    ep = node.asLiteral().getString();
                }
                if ( niep.hasNext() ){
                    System.err.println("Two or more endpoints for one file");
                }
            }
            if ( ep.length() == 0 ){
                System.err.println("endpoint is null");
                continue; 
            }
            
            ep = checkEPMap(ep);
            
            String epc = ep.split("//")[1].replace("/", "_").replace("#", "-");
            File epcrfile = new File(workdir.concat(epc).concat(".cr")); // cl-reachable-cl
            File epclfile = new File(workdir.concat(epc).concat(".cl")); // cl-relation-cl
            eps.add(ep);
            // get cl
            Property cpp = m.getProperty(SBMURLs.CP); 
            Property vc = m.getProperty(SBMURLs.VOID_CLASS);
            Property en = m.getProperty(SBMURLs.VOID_ENTITIES);
            Property la = m.getProperty(SBMURLs.RDFS_LABEL);
            NodeIterator cpep = m.listObjectsOfProperty(cpp); // class partitions
            while(cpep.hasNext()){
                Resource cres = cpep.next().asResource();
                // get cl resouce
                NodeIterator cit = m.listObjectsOfProperty(cres, vc);
                if (!cit.hasNext()){continue;}
                Resource cr = cit.next().asResource();
                // get the number of entities
                NodeIterator eit = m.listObjectsOfProperty(cres, en);
                String ent = "0";
                if (eit.hasNext()){
                  ent = eit.next().asLiteral().getString();
                }
                if (Integer.parseInt(ent) <= 1 ){
                    nc.add(cr.getURI());
                    continue;
                }
                // get labels
                NodeIterator lit = m.listObjectsOfProperty(cr, la);
                String label = "";
                while (lit.hasNext()){ 
                   RDFNode ln = lit.next();
                   if (ln.asLiteral().getLanguage() == null || ln.asLiteral().getLanguage().equals("en")){
                     label = ln.asLiteral().getString();
                   }
                }
                String cinfo = cr.getURI().concat("\t").concat(label).concat("\t")
                        .concat(ep).concat("\t").concat(ent);
                //cl.add(cinfo);
                cltmp.put(cr.getURI(), cinfo);
                //epcl.add(cinfo);
            }
            // get cgraph
            Property ppp = m.getProperty(SBMURLs.VOID_PP);
            Property pp = m.getProperty(SBMURLs.VOID_P);
            Property crp = m.getProperty(SBMURLs.SBM_CREL);
            Property dsp = m.getProperty(SBMURLs.VOID_DS);
            Property dop = m.getProperty(SBMURLs.VOID_DO);
            Property trp = m.getProperty(SBMURLs.VOID_TR);
            Property scp = m.getProperty(SBMURLs.SBM_SC);
            Property ocp = m.getProperty(SBMURLs.SBM_OC);
            NodeIterator ppit = m.listObjectsOfProperty(ppp); // property partitions
            Set<String> curls = new HashSet<String>();
            Set<String> crels = new HashSet<String>();
            //Set<String> crelsep = new HashSet<String>();
            while(ppit.hasNext()){ // for each property
                Resource pres = ppit.next().asResource();
                // get prop url
                NodeIterator pit = m.listObjectsOfProperty(pres, pp);
                if (!pit.hasNext()){ continue; }
                String prurl = pit.next().asResource().getURI();
                // get crel
                NodeIterator crit = m.listObjectsOfProperty(pres, crp);
                while(crit.hasNext()){ //for each class relation
                    Resource cr = crit.next().asResource();
                    // Check OC first 
                    String oc = null;
                    String sc = null;
                    NodeIterator ocit = m.listObjectsOfProperty(cr, ocp); // oc 
                    if (ocit.hasNext()){
                        oc = ocit.next().asResource().getURI();
                    }else{
                        continue;
                    }
                    NodeIterator scit = m.listObjectsOfProperty(cr, scp); // sc 
                    if (scit.hasNext()){
                        sc = scit.next().asResource().getURI();
                    }else{
                        continue;
                    }
                    if (bc.contains(oc) || bc.contains(sc) || nc.contains(oc) || nc.contains(sc)){
                        continue;
                    }
                    
                    String dsn = "0";
                    String don = "0";
                    String trn = "0";                    
                    NodeIterator dsit = m.listObjectsOfProperty(cr, dsp); // dsn
                    while(dsit.hasNext()){
                       dsn = dsit.next().asLiteral().getString();                       
                       break;
                    }
                    NodeIterator doit = m.listObjectsOfProperty(cr, dop); // don
                    while(doit.hasNext()){
                       don = doit.next().asLiteral().getString();
                       break;
                    }
                    NodeIterator trit = m.listObjectsOfProperty(cr, trp); // trn
                    while(trit.hasNext()){
                       trn = trit.next().asLiteral().getString();
                       break;
                    }
                    // sc oc 
                    curls.add(oc); curls.add(sc); 
                    //if (sc.equals("http://togodb.biosciencedbc.jp/ontology/ArchiveAcestBlasthitsTr")){
                    //    System.out.println("here");
                    //}
                    String crelep = sc.concat("\t").concat(oc).concat("\t").concat(prurl).concat("\t")
                            .concat(dsn).concat("\t").concat(don).concat("\t").concat(trn);
                    crels.add(crelep);
                    try {
                        BufferedWriter bwe = new BufferedWriter(new FileWriter(epclfile, true));
                        bwe.write(crelep);
                        bwe.newLine();
                        bwe.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
            OWLClassGraph g = new OWLClassGraph(ep, curls, crels);
            Iterator<String> cit1 = curls.iterator();
            while(cit1.hasNext()){
                String cl1 = cit1.next();
                // cl addition
                cl.add(cltmp.get(cl1));
                
                List<String> rc = g.getReachableClasses(cl1);
                // crel addition
                ListIterator<String> rcit = rc.listIterator();
                StringBuilder sb = new StringBuilder(cl1.concat("\t"));
                while(rcit.hasNext()){
                    sb.append(rcit.next()); sb.append(",");
                }
                try {
                    BufferedWriter bwe = new BufferedWriter(new FileWriter(epcrfile, true));
                    bwe.write(sb.substring(0,sb.length()-1));
                    bwe.newLine();
                    bwe.close();
                }catch(IOException e){
                    e.printStackTrace();
                }

            }
        }
        try{
            BufferedWriter bw1 = new BufferedWriter(new FileWriter(fcl, true));
            ListIterator<String> cit = cl.listIterator();
            while(cit.hasNext()){
                bw1.write(cit.next());
                bw1.newLine();
            }
            bw1.close();
            File fep = new File(workdir.concat(epfile));
            BufferedWriter bw3 = new BufferedWriter(new FileWriter(fep, true));
            Iterator<String> eit = eps.iterator();
            while(eit.hasNext()){
                bw3.write(eit.next());
                bw3.newLine();
            }
            bw3.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public static Model getModel(File file){
        Model model = ModelFactory.createDefaultModel();
        try{
          InputStream in = new FileInputStream(file);
          model.read(in, null, "TURTLE");
        }catch(IOException e){
          e.printStackTrace();
        }
        return model;
    }
        
    private static Set<String> getBlackClasses(){
        Set<String> bc = new HashSet<String>();
        bc.add("http://www.w3.org/2002/07/owl#Class");
        bc.add("http://www.w3.org/2002/07/owl#ObjectProperty");
        bc.add("http://www.w3.org/2002/07/owl#Ontology");
        bc.add("http://www.w3.org/2002/07/owl#Thing");
        bc.add("http://www.w3.org/2000/01/rdf-schema#Class");
        bc.add("http://www.w3.org/2002/07/owl#NamedIndividual");
        //bc.add("");
        return bc;
    }
    
    private static String checkEPMap(String epurl){ // From a part of URL->EP URL
        if (epurl.contains("ebi.ac.uk")){
            return "https://www.ebi.ac.uk/rdf/services/sparql";
        }else if (epurl.contains("bio2rdf")){
            return "http://bio2rdf.org/sparql";
        }
        return epurl;
    }
}
