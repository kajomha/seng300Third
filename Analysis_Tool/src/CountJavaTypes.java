import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
public class CountJavaTypes {
    
     public static Hashtable<String, int[]> table = new Hashtable<String, int[]>();

     public static void main(String[] args) throws IOException{
         
         // Store command line arguments
         String pathname = args[0];
         
         //Boolean to check if directory has a .java file
         boolean no_file = true;
         List[] declaredClasses = new List[2];
         List<String> nested = new ArrayList<>();
         List<String> local = new ArrayList<>();
         
         //Print confirmations of receivedd input
         System.out.println("You have selected the following directory:\n\t" + pathname + "\n");
         
         //Open the directory to read files from
         File directory = new File(pathname);
         //Lists all files from directory
         File[] fileList = directory.listFiles();
         List<File>tempList = Arrays.asList(fileList);
         List<File>list = new ArrayList<>();
         for (int i = 0; i<tempList.size(); i++) {
             list.add(tempList.get(i));
         }
         
         for (int i = 0; i<list.size(); i++) {
             if (list.get(i).isDirectory()) {
                 String newpath = list.get(i).getAbsolutePath();
                 File subdir = new File(newpath);
                 fileList = subdir.listFiles();
                 
                 for (int j=0;j<fileList.length;j++) {
                     list.add(fileList[j]);
                 }
                 
             }
             
             if (list.get(i).getName().endsWith(".jar")) {
                 JarFile jar = new JarFile(list.get(i).getAbsolutePath());
                 Enumeration<JarEntry> e = jar.entries();
                 while (e.hasMoreElements()) {
                     JarEntry entry = (JarEntry)e.nextElement();
                     String jarfiles = entry.getName();
          
                     InputStream in = null;
                     
                     if (jarfiles.endsWith(".java")) {
                         
                         URL inputURL = new URL("jar:file:/" + list.get(i).getAbsolutePath()+"!/"+jarfiles);
                         JarURLConnection conn = (JarURLConnection)inputURL.openConnection();
                         in = conn.getInputStream();
                     
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                         String jartext;
                         String jarcode = "";
                         while ((jartext = reader.readLine()) != null) {
                        	 jarcode = jarcode + "\n" + jartext;
                         }
                                                 
                         no_file = false;
                         
                         declaredClasses = parse(jarcode,pathname,declaredClasses,nested,local);
                     
                         reader.close();
                         
                     }
                 }
           }
           
             
             
         }
         
         
         //Loop to iterate though files in the directory
         for (int i = 0; i<list.size(); i++) {
             if (list.get(i).isFile()) {
                 
                 //Look for files with .java extension
                 int temp = list.get(i).getName().lastIndexOf('.');
                 String filetype = list.get(i).getName().substring(temp+1);
                 
                 if (filetype.contentEquals("java")) {
                     //Set boolean to show .java file was found
                     no_file = false;
                     
                     
                     //The following commented out print line is used to find the pathname of a file
                     //incase that fath contains foreign symbols and scanner crashes due to it
                     //System.out.println(list.get(i).getAbsolutePath());
                     
                     //Use scanner to read code from file. Stores whole doc in a string
                     Scanner scanner = new Scanner(list.get(i));
                     String code = "";
                     
                     if (scanner.hasNextLine()) {
                     	 code = scanner.useDelimiter("\\A").next();
                     }
                     
 					 scanner.close();
                     // Parse the given code; this method will update the hashtable
                     declaredClasses = parse(code, pathname,declaredClasses,nested,local);

                 }
             }
         }
         
         //Print out results
         if (no_file == true) {
             System.out.println("This directory does not contian any files with the exptension: '.java'");
         }
         
         else {
             reportTable(declaredClasses);
         }
         
      }  
     
     //Parse method
     public static List[] parse(String str, String pathname, List[] declaredClasses, List<String> nested, List<String> local) {
         
         //Create AST Parser
            
            ASTParser parser = ASTParser.newParser(AST.JLS9);
            parser.setSource(str.toCharArray());
            
            Map<String, String> options = JavaCore.getOptions();
            JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
            parser.setCompilerOptions(options);
            
            //parser.setBindingsRecovery(true);
            parser.setResolveBindings(true);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            
            parser.setEnvironment(new String[] { System.getProperty("java.home") + "/lib/rt.jar" }, new String[] { pathname },
                    null, false);
            
            parser.setUnitName("");
            
            //Initialize counters to 0
            int[] counters = {0,0};
            
            Hashtable<String, int[]> collection = new Hashtable<String, int[]>();
            
            final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            
     
            //Visit string
            cu.accept(new ASTVisitor() {
                     
                //Visits Class and Interface declaration, i.e. the assign1 class.
                public boolean visit(TypeDeclaration node) {
                    
                    String nodename = node.resolveBinding().getQualifiedName();
                    
                    //System.out.println("Type Declaration: " + nodename);
                    
                    if (node.resolveBinding().isNested()) {
                    	nested.add(nodename);
                    }else {
                    	local.add(nodename);
                    }
                    
                    updateTable(nodename, "Declaration");
                    
                    return true;                
                }           
                
                //Visits annotations
                public boolean visit(AnnotationTypeDeclaration node) {
                    
                    String nodename = node.getName().getFullyQualifiedName();
                    
                    //System.out.println("Annotation: " + nodename);
                    
                    if (node.resolveBinding().isNested()) {
                    	nested.add(nodename);
                    }else {
                    	local.add(nodename);
                    }
                    
                    updateTable(nodename, "Declaration");
                    
                    return false;               
                }
                
                //Visits Enumerations
                public boolean visit(EnumDeclaration node) {
                    
                    String nodename = node.getName().getFullyQualifiedName();
                    
                    //System.out.println("Enumeration: " + nodename);
                    
                    if (node.resolveBinding().isNested()) {
                    	nested.add(nodename);
                    }else {
                    	local.add(nodename);
                    }
                    
                    updateTable(nodename, "Declaration");
                    
                    return false;               
                }
                
     
                //Count references for all primitives 
                public boolean visit(SimpleName node) {
                    
                    String nodename = node.getFullyQualifiedName();
                    if (node.resolveTypeBinding() != null && node.resolveTypeBinding().isPrimitive()) {
                    
                    		updateTable(nodename, "Reference");
                    }
                    
                    return true;
                }
                
                @Override
                public boolean visit(SimpleType node) {
                    
                    if (node.resolveBinding() != null) {
                        
                        String nodename = node.resolveBinding().getQualifiedName();
                        updateTable(nodename, "Reference");
                    }
                    
                    return false;
                }
                
                public boolean visit(AnonymousClassDeclaration node) {
                    
                    if (node.resolveBinding() != null) {

                        // MAY NEED TO BE CHANGED!
                        // anonymous classes don't have names, obviously, which complicates counting
                        // them in our table
                        
                        // at current I just have it count "Anonymous" as a separate declaration
                        // but I don't know if that's how Prof. Walker would want them counted
                        
                        updateTable("Anonymous", "Declaration");
                        //System.out.println("Anonymous");
        				
                        
                    }
                
                return false;
                }
                
                @Override
                public boolean visit(ArrayAccess node) {
                    
                    if (node.getArray().resolveTypeBinding() != null) {  
                    	String nodename = node.getArray().resolveTypeBinding().getQualifiedName();
                    	updateTable(nodename, "Reference");
                    }
                        
                    
                    return false;
                }
                
            });
            
            declaredClasses[0] = nested;
            declaredClasses[1] = local;
            
            return declaredClasses;
     }
     
     // takes the name we got from a node, and a string telling us whether to increment Reference or Declaration count
     
     public static void updateTable (String nodename, String type) {
         
        int index = 0;
         
        if (type.equals("Reference")) {
            index = 1;
        } else if (type.equals("Declaration")) {
            index = 0;
        }
        
        int i_array[] = {0,0};
            
        if (table.get(nodename) == null) {
                
            i_array[index] = 1;
            table.put(nodename, i_array);
        
        } else {
                
            i_array = table.get(nodename);
            i_array[index] += 1;
            table.put(nodename,  i_array);
            
        }
            
        //System.out.println("Count for \"" + nodename + "\" is " + table.get(nodename)[index]);
         
     }
     
     // displays all of the information stored in our table
     
     public static void reportTable (List[] declaredClasses) {
         
         // use an enumeration of keys to find the type names, and then use the keys to get the counts
         
         Enumeration<String> elements = table.keys();
         
         int nestedCount = 0;
         int localCount = 0;
         int anonymousCount = 0;
         int thisReferenceCount = 0;
         int otherReferenceCount = 0;
         
         while(elements.hasMoreElements()) {
             
             String s = elements.nextElement();
             int[] a = table.get(s);
             
             //Print Statement from Iteration 2
             //System.out.println(s +": Declarations found: " + a[0] + "; references found: " + a[1] + ".");
             
             
             //Use the following for analysis of repos

             //Get list of with names of Listed and Local Types
             List<String>nested = declaredClasses[0];
             List<String>local = declaredClasses[1];
             
             
             //Look for Nested Type Declarations
             if (nested.contains(s)) {
            	 nestedCount++;
            	 
             }
             
             //Look for Local Type Declarations
             if (local.contains(s)) {
            	 localCount++;
             }
             
             if (s == "Anonymous") {
            	 anonymousCount = a[0];
             }
             

             
             //Look for # of references to local/nested types
             if (a[0] != 0 && a[1] != 0) {
            	 thisReferenceCount = thisReferenceCount + a[1];
             }
             
             
             //Look for # of references to other types
             if (a[0] == 0 && a[1] != 0) {
            	 otherReferenceCount = otherReferenceCount + a[1];
             }
             
             
         }
         //Print Results
         System.out.println("Nested Types Declared: " + nestedCount);
         System.out.println("Local Types Declared: " + localCount);
         System.out.println("Anonymous Types Declared: " + anonymousCount);
         System.out.println("Local/Nested Types Referenced : " + thisReferenceCount);
         System.out.println("Other Types Referenced : " + otherReferenceCount);
     }
     
}