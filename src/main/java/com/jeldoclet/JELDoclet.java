package com.jeldoclet;

import java.util.Date;

/*
 * File: JELDoclet.java
 * Purpose: A Doclet to be used with JavaDoc which will output XML with all of the information
 *    from the JavaDoc.
 * Date: Mar 2nd, 2003
 * 
 * History:
 * 		Sep 14th, 2005 - Updated by TP to allow multiple file output.
 * 					   - Added support for exceptions.
 * 					   - Added support for a few missing method modifiers (final, etc).
 *                     - Added support for xml namespaces.
 *                     
 *    Dec 8/9th, 2005 - updated by T.Zwolsky (all extensions marked thz.../thz):
 *      - added cmdline parameter -outputEncoding
 *      - bugfix: implements/interface node(s) were created but not inserted into class node
 *      - added exception comment (both here and in the xsd as optional element)
 *      - added cmdline parameter -filename
 *      - added output directory check
 *      - added some comments to readme
 *      - added test target in build.xml
 *      - added nested class in test classes (test/MyInterClass.java)
 *      - added xsl transformation jel2html.xsl
 *      
 *      Dec 16th, 2005 - updated by T.Zwolsky (all extensions marked thz.../thz):
 *      - added version here and in xsd
 *      - added admin stuff in xsd
 * 
 * Author: Jack D. Herrington <jack_d_herrington@codegeneration.net>
 * 		   Toby Patke 		  <toby_patke _?_ hotmail.com>
 * 
 * This source is covered by the Open Software Licenses (1.1)
 */
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.Type;

/**
 * The core JELDoclet class.
 *
 * @author Jack D. Herrington, Toby Patke
 */
public class JELDoclet
{
//    private final static Logger log = LoggerFactory.getLogger(JELDoclet.class);

    private static final String JEL_TYPE = "jelclass";

    /**
     * (thz) program version
     */
    private static String programversion = "1.0.0";

    /**
     * (thz) this must be the same as the xsdversion element in JelSchema - Admin
     */
    private static String xsdversion = "1.0.0";

    /**
     * Determines whether the output is a single file or multiple files.
     * Populated from the command line via the "-multiple" flag.
     */
    private static boolean multipleFiles = false;

    /**
     * Determines the directory where output is placed.
     * Populated from the command line via the "-d [directory]" flag.
     */
    private static String outputDirectory = "./";

    /**
     * Determines whether the generated output is part of a namespace.
     * Populated from the command line via the "-includeNamespace" flag.
     */
    private static boolean includeNamespace = false;

    /**
     * (thz) determines standard / adjustable encoding
     */
    private static String outputEncoding = "UTF-8";

    /**
     * (thz) single output: this is the filename, multiple output: this is prepended
     * to the filename created; default is empty (single: "jel.xml", multiple: package.class.xml) !
     */
    private static String filenameBase = "";

    public static void main(String[] args) {
        com.sun.tools.javadoc.Main.execute(new String[]{"-verbose",
        		"-doclet",JELDoclet.class.getName(),
        		"-sourcepath","C:/dev/newspace/pcc/PCCAPI/src",//com/pointclickcare/api/common/paging/model/dto
        		"com.pointclickcare.api.common.paging.model.dto"});
    }
    
    /**
     * Processes the JavaDoc documentation.
     *
     * @param root The root of the documentation tree.
     * @return True if processing was succesful.
     * @see com.sun.java.Doclet
     */
    public static boolean start( RootDoc root )
    {
        //Get program options
        getOptions(root);

        // Create the root node.
        XMLNode[] nodes = buildXmlFromDoc(root);

        // Save the output XML
        save(nodes);

        return true;
    }

    /**
     * NOTE: Without this method present and returning LanguageVersion.JAVA_1_5,
     *       Javadoc will not process generics because it assumes LanguageVersion.JAVA_1_1
     * @return language version (hard coded to LanguageVersion.JAVA_1_5)
     * @see http://stackoverflow.com/questions/5731619/doclet-get-generics-of-a-list
     */
    public static LanguageVersion languageVersion() {
       return LanguageVersion.JAVA_1_5;
    }
    

    /**
     * A JavaDoc option parsing handler.  This one returns the number of arguments required
     * for the given option.
     *
     * @param option The name of the option.
     * @return The number of arguments.
     * @see com.sun.java.Doclet
     */
    public static int optionLength( String option )
    {	//This method is required by the doclet spec.

        //Check for our flags.  Return the number of parameters expected after a given flag (including the flag).

        if ( option.compareToIgnoreCase( "-multiple" ) == 0 )
            return 1;

        if ( option.compareToIgnoreCase( "-includeNamespace" ) == 0 )
            return 1;

        if ( option.compareToIgnoreCase( "-d" ) == 0 )
            return 2;

        // thz
        if ( option.compareToIgnoreCase( "-outputEncoding" ) == 0 )
            return 2;
        if ( option.compareToIgnoreCase( "-filename" ) == 0 )
            return 2;
        // /thz

        return 0;
    }

    /**
     * A JavaDoc option parsing handler. This one checks the validity of the options.
     *
     * @param options The two dimensional array of options.
     * @param reporter The error reporter.
     * @return True if the options are valid.
     * @see com.sun.java.Doclet
     */
    public static boolean validOptions(String options[][], DocErrorReporter reporter )
    {	//This method is required by the doclet spec.  If we had any options to validate,
        //	we would do it here.
        return true;
    }


    /**
     * Retrieve the expected options from the given array of options.
     *
     * @param root The root object which contains the options to be retrieved.
     */
    private static void getOptions( RootDoc root )
    {
        // Get the file name and determine if multiple files should be built.

        String[][] options = root.options();
        for ( int opt = 0; opt < options.length; opt++ )
        {
            if( options[opt][0].compareToIgnoreCase( "-d" ) == 0 )
            {
                outputDirectory = options[opt][1];
// thz: make sure output directory ends with a path separator (the default does)
                String fs = System.getProperty("file.separator");
                if(outputDirectory.endsWith(fs) == false)
                    outputDirectory += fs;
// /thz
                continue;
            }

            if( options[opt][0].compareToIgnoreCase( "-multiple" ) == 0 )
            {
                multipleFiles = true;
                continue;
            }

            if( options[opt][0].compareToIgnoreCase( "-includeNamespace" ) == 0 )
            {
                includeNamespace = true;
                continue;
            }

            // thz
            if( options[opt][0].compareToIgnoreCase( "-outputEncoding" ) == 0 )
            {
                outputEncoding = options[opt][1];
                continue;
            }
            if( options[opt][0].compareToIgnoreCase( "-filename" ) == 0 )
            {
                filenameBase = options[opt][1];
                continue;
            }
            // /thz
        }

        // thz
//        log.debug("jeldoclet V" + programversion);
//        log.debug( "Using output directory '" + outputDirectory + "'.");
//        log.debug( "output encoding '" + outputEncoding + "'.");
//        log.debug("Saving as " + (multipleFiles ? "multiple files." : "a single file."));
//
//        if(!filenameBase.equals("")){
//            log.debug("filename " + (multipleFiles ? " base " : "") + ": '" + filenameBase + "')");
//        }
    }


    /**
     * Save the given array of nodes.  Will either save the files individually, or as a single file
     * depending on the existence of the "-multiple" flag.
     *
     * @param nodes The array of nodes to be saved.
     */
    private static void save( XMLNode[] nodes )
    {
// thz: moved outside if
        //Wrap the XML in a "jel" root node.
        XMLNode rootXml = new XMLNode("jel");
// add admin node
        XMLNode adminNode = new XMLNode("admin");
        adminNode.addAttribute("version", programversion);
        adminNode.addAttribute("xsdversion", xsdversion);
        adminNode.addAttribute("creation", new Date().toString());
        rootXml.addNode(adminNode);
// /thz

        if(multipleFiles)
        {
            for(int index=0; index<nodes.length; index++)
            {
                String fileName = nodes[index].getAttribute("fulltype") + ".xml";

                rootXml.addNode( nodes[index] );
                // thz: prepend filename base
                if(filenameBase.equals("") == false)
                    fileName = filenameBase + fileName;
                // thz: call with encoding
                rootXml.save(outputDirectory, fileName, includeNamespace, outputEncoding);
                // /thz
            }
        }
        else
        {
            for(int index=0; index<nodes.length; index++)
                rootXml.addNode(nodes[index]);

            // thz: use filename
            String fileName = "jel.xml";
            if(filenameBase.equals("") == false)
                fileName = filenameBase;
            // thz: call with outputencoding
            rootXml.save(outputDirectory, fileName, includeNamespace, outputEncoding);
            // /thz
        }
    }

    /**
     * Builds the XML nodes from a given com.sun.javadoc.RootDoc.
     *
     * @param root The RootDoc from which the XML should be built.
     * @return The array of XML nodes which represents the RootDoc.
     */
    private static XMLNode[] buildXmlFromDoc ( RootDoc root )
    {
        // Iterate through all of the classes and generate a node for each class.
        // NOTE:  Nodes may contain subnodes if classes contain subclasses.

        ClassDoc[] classes = root.classes();
        XMLNode[] retval = new XMLNode[classes.length];

        for ( int index = 0; index < classes.length; index++ )
        {
            retval[index] = transformClass( classes[index], null ,JEL_TYPE);
        }

        return retval;
    }

    /**
     * Transforms comments on the Doc object into XML.
     *
     * @param doc The Doc object.
     * @param node The node to add the comment nodes to.
     */
    private static void transformComment( Doc doc, XMLNode node )
    {
        // Creat the comment node

        XMLNode commentNode = new XMLNode( "comment" );
        boolean addNode = false;

        // Handle the basic comment

        if ( doc.commentText() != null && doc.commentText().length() > 0 )
        {
            commentNode.addText( doc.commentText() );
            addNode = true;
        }

        // Handle the tags

        Tag[] tags = doc.tags();
        for( int tag = 0; tag < tags.length; tag++ )
        {
            XMLNode paramNode = new XMLNode( "attribute" );
            paramNode.addAttribute( "name", tags[tag].name() );
            paramNode.addText( tags[tag].text() );

            commentNode.addNode( paramNode );

            addNode = true;
        }

        // Add the node to the host

        if ( addNode )
            node.addNode( commentNode );
    }

    /**
     * Creates a <fields> node from a set of fields.
     *
     * @param fields The set of fields.
     * @param node The node to add the <field> nodes to.
     */
    private static void transformFields( FieldDoc[] fields, XMLNode node )
    {
        if ( fields.length < 1 )
            return;

        // Create the <fields> node and iterate through the fields

        XMLNode fieldsNode = new XMLNode( "fields");
        for( int index = 0; index < fields.length; index++ )
        {
            // Create the <field> node and populate it.

            XMLNode fieldNode = new XMLNode( "field");

            transformAnnotations(fields[index].annotations(), fieldNode);
            Type type = fields[index].type();
            fieldNode.addAttribute( "name", fields[index].name() );
            fieldNode.addAttribute( "type", type.typeName() );
            if (type.asParameterizedType()==null) {
              fieldNode.addAttribute( "fulltype", type.toString() );
            } else {
              //for Fields with Generic - use this method instead of toString()
              fieldNode.addAttribute( "fulltype", type.qualifiedTypeName());
            }
            populateGenericType(type,fieldNode);
                

            if ( fields[index].constantValue() != null && fields[index].constantValue().toString().length() > 0 )
                fieldNode.addAttribute( "const", fields[index].constantValue().toString() );

            if ( fields[index].constantValueExpression() != null && fields[index].constantValueExpression().length() > 0 )
                fieldNode.addAttribute( "constexpr", fields[index].constantValueExpression() );

            setVisibility(fields[index], fieldNode);

            if ( fields[index].isStatic() )
                fieldNode.addAttribute( "static", "true" );

            if ( fields[index].isFinal() )
                fieldNode.addAttribute( "final", "true" );

            if ( fields[index].isTransient() )
                fieldNode.addAttribute( "transient", "true" );

            if ( fields[index].isVolatile() )
                fieldNode.addAttribute( "volatile", "true" );

            // Add comments attached to the field.

            transformComment( fields[index], fieldNode );

            // Add the <field> node to the <fields> node.

            fieldsNode.addNode( fieldNode );
        }

        // Add the <fields> node to the host.
        node.addNode( fieldsNode );
    }


    /**
     * Sets the visibility for the class, method or field.
     * @param member The member for which the visibility needs to be set (class, method, or field).
     * @param node The node to which the visibility should be set.
     */
    private static void setVisibility( ProgramElementDoc member, XMLNode node )
    {
        if ( member.isPrivate() )
            node.addAttribute( "visibility", "private" );

        else if ( member.isProtected() )
            node.addAttribute( "visibility", "protected" );

        else if ( member.isPublic() )
            node.addAttribute( "visibility", "public" );

        else if ( member.isPackagePrivate() )
            node.addAttribute( "visibility", "package-private" );
    }

    /**
     * Populates the interior of a <method> node from information from an ExecutableMemberDoc
     * object.
     * @param method The method documentation.
     * @param node The node to add the XML to.
     */
    private static void populateMethodNode( ExecutableMemberDoc method, XMLNode node )
    {
        // Add any comments associated with the method

        transformComment( method, node );

        transformAnnotations(method.annotations(),node);
        
        // Add the basic values

        node.addAttribute( "name", method.name() );

        setVisibility(method, node);

        if ( method.isStatic() )
            node.addAttribute( "static", "true" );

        if ( method.isInterface() )
            node.addAttribute( "interface", "true" );

        if ( method.isFinal() )
            node.addAttribute( "final", "true" );

        if ( method instanceof MethodDoc )
            if ( ((MethodDoc) method).isAbstract() )
                node.addAttribute( "abstract", "true" );

        if ( method.isSynchronized() )
            node.addAttribute( "synchronized", "true" );

        if ( method.isSynthetic() )
            node.addAttribute( "synthetic", "true" );


        // Iterate through the parameters and add them

        Parameter[] params = method.parameters();

        if ( params.length > 0 )
        {
            ParamTag[] paramTags = method.paramTags();
            XMLNode paramsNode = new XMLNode( "params" );

            for( int param = 0; param < params.length; param++ )
            {
                XMLNode paramNode = new XMLNode( "param" );

                paramNode.addAttribute( "name", params[param].name() );
                paramNode.addAttribute( "type", params[param].type().typeName() );
                transformAnnotations(params[param].annotations(),paramNode);
                populateGenericType(params[param].type(), paramNode);

                for( int paramTag = 0; paramTag < paramTags.length; paramTag++ )
                {
                    if( paramTags[ paramTag ].parameterName().compareToIgnoreCase( params[param].name() ) == 0 )
                    {
                        paramNode.addAttribute( "comment", paramTags[ paramTag ].parameterComment() );
                    }
                }

                paramsNode.addNode( paramNode );
            }

            node.addNode( paramsNode );
        }


        //  Iterate through the exceptions and add them

        ClassDoc[] exceptions = method.thrownExceptions();

// thz: @throws/@exception tags
        ThrowsTag[] exceptionTags = method.throwsTags();

        if ( exceptions != null &&
                exceptions.length > 0 )
        {
            XMLNode exceptionsNode = new XMLNode( "exceptions" );

            for( int except = 0; except < exceptions.length; except++ )
            {
                XMLNode exceptNode = new XMLNode( "exception" );

                exceptNode.addAttribute( "type", exceptions[except].typeName() );
                exceptNode.addAttribute( "fulltype", exceptions[except].qualifiedTypeName() );

// thz
                for(int exceptionTag = 0; exceptionTag < exceptionTags.length; exceptionTag++)
                {
                    if(exceptionTags[exceptionTag].exceptionName().compareToIgnoreCase(exceptions[except].typeName()) == 0)
                        exceptNode.addAttribute("comment", exceptionTags[exceptionTag].exceptionComment());
                }
// /thz

                exceptionsNode.addNode( exceptNode );
            }

            node.addNode( exceptionsNode );
        }

        //if (method.thrownExceptions())
    }

    /**
     * Transforms an array of methods and an array of constructor methods into XML and adds those to the host
     * node.
     * @param methods The methods.
     * @param constructors The constructors.
     * @param node The node to add the XML to.
     */
    private static void transformMethods( MethodDoc[] methods, ConstructorDoc[] constructors, XMLNode node )
    {
        if ( methods.length < 1 && constructors.length < 1 )
            return;

        // Create the <methods> node

        XMLNode methodsNode = new XMLNode( "methods");

        // Add the <constructor> nodes

        for( int index = 0; index < constructors.length; index++ )
        {
            XMLNode constNode = new XMLNode( "constructor");

            populateMethodNode( constructors[index], constNode );

            methodsNode.addNode( constNode );
        }

        // Add the <method> nodes

        for( int index = 0; index < methods.length; index++ )
        {
            XMLNode methodNode = new XMLNode( "method");

            populateMethodNode( methods[index], methodNode );

            methodNode.addAttribute( "type", methods[index].returnType().typeName() );
            populateGenericType( methods[index].returnType(), methodNode );
            Tag[] returnTags = methods[ index ].tags( "@return" );
            if ( returnTags.length > 0 )
            {
                methodNode.addAttribute( "returncomment", returnTags[ 0 ].text() );
            }

            methodsNode.addNode( methodNode );
        }

        // Add the <methods> node to the host

        node.addNode( methodsNode );
    }

    private static void populateGenericType(Type returnType, XMLNode rootNode)
    {
      
      String fullType = returnType.toString();
      int i = fullType.indexOf("<");
      if (i>0) {
        rootNode.addAttribute( "fulltype",  fullType.substring(0, i));
        ParameterizedType parameterizedType = returnType.asParameterizedType();
        if (parameterizedType != null) {
          Type[] typeArguments = parameterizedType.typeArguments();
          if (typeArguments.length>0) {
            XMLNode genericNode = new XMLNode( "genericTypes");
            for (Type type : typeArguments)
            {
              XMLNode node = new XMLNode( "type");
              populateGenericType(type,node);
              genericNode.addNode(node);
            }
            rootNode.addNode(genericNode);
          }
        }
      } else {
        rootNode.addAttribute( "fulltype",  fullType);
      }
    }

    /**
     * Transforms a ClassDoc class into XML and adds it to the root XML node.
     *
     * @param classDoc The class to transform.
     * @param root The XML node to add the class XML to.
     */
    private static XMLNode transformClass( ClassDoc classDoc, XMLNode root, String type )
    {
        XMLNode classNode = new XMLNode( type );  //"class" needs a prefix for output to work with JAXB.

        // Handle basic class attributes

        setVisibility(classDoc, classNode);
        transformAnnotations(classDoc.annotations(),classNode);
        
        classNode.addAttribute( "type", classDoc.name() );
        classNode.addAttribute( "fulltype", classDoc.qualifiedName() );
        classNode.addAttribute( "package", classDoc.containingPackage().name() );
    	
        if (classDoc.typeParameters() != null && classDoc.typeParameters().length>0) {
        	//class is generic
        	Type[] typeArguments = classDoc.typeParameters();
	        if (typeArguments.length>0) {
	          XMLNode genericNode = new XMLNode( "typeParameters");
	          for (Type type2 : typeArguments)
	          {
	            XMLNode node = new XMLNode( "type");
	            populateGenericType(type2,node);
	            genericNode.addNode(node);
	          }
	          classNode.addNode(genericNode);
	        }
        }
    	
        ClassDoc[] extendClasses = classDoc.interfaces();
        if(extendClasses.length > 0)
        {
            XMLNode implement = new XMLNode( "implements" );
            for( int extendIndex = 0; extendIndex < extendClasses.length; extendIndex++ )
            {
                XMLNode interfce = new XMLNode( "interface" );
                interfce.addAttribute( "type", extendClasses[extendIndex].name() );
                interfce.addAttribute( "fulltype", extendClasses[extendIndex].qualifiedName() );
                implement.addNode( interfce );
            }

// thz: implements-node should be inserted, too
            classNode.addNode(implement);
// /thz
        }

        if ( classDoc.superclass() != null )
        {
        	classNode.addAttribute( "superclass", classDoc.superclass().name() );
            classNode.addAttribute( "superclassfulltype", classDoc.superclass().qualifiedName() );
        }

        //collect all enumeration constants
        if ( classDoc.isEnum()) {
            XMLNode enums = new XMLNode( "enumeration" );
            classNode.addNode( enums );
        	for (FieldDoc enumValue : classDoc.enumConstants()) {
        		XMLNode value = new XMLNode( "value" );
        		value.addAttribute( "name", enumValue.name());
        		if (enumValue.constantValue() instanceof String) {
        			value.addAttribute( "value", (String) enumValue.constantValue());
				} else if (enumValue.constantValue() != null) {
        			value.addAttribute( "value", String.valueOf(enumValue.constantValue()));
				}
        		if (enumValue.constantValueExpression() != null) {
        			value.addAttribute( "expression", enumValue.constantValueExpression());
        		}
        		if (enumValue.commentText() != null && enumValue.commentText().length()>0) {
        			value.addAttribute( "description", enumValue.commentText());
        		}
        		enums.addNode( value );
			}
        }
		
        
        if ( classDoc.isInterface() )
            classNode.addAttribute( "interface", "true" );

        if ( classDoc.isFinal() )
            classNode.addAttribute( "final", "true" );

        if ( classDoc.isAbstract() )
            classNode.addAttribute( "abstract", "true" );

        if ( classDoc.isSerializable() )
            classNode.addAttribute( "serializable", "true" );

        // Handle the comments on the class

        transformComment( classDoc, classNode );

        // Handle the fields

        transformFields( classDoc.fields(), classNode );

        // Handle the methods

        transformMethods( classDoc.methods(), classDoc.constructors(), classNode );

        // Handle inner classes

        ClassDoc[] innerClasses = classDoc.innerClasses();
        for( int classIndex = 0; classIndex < innerClasses.length; classIndex++ )
            classNode.addNode ( transformClass( innerClasses[classIndex], classNode , JEL_TYPE) );

        return classNode;
    }


    private static void transformAnnotations(AnnotationDesc[] annotationDescs, XMLNode classNode)
    {
      if (annotationDescs.length>0) {
        XMLNode annotationsNode = new XMLNode( "annotations" ); 
        for (AnnotationDesc annotationDesc : annotationDescs)
        {
          XMLNode node = new XMLNode( "annotation" );  //"class" needs a prefix for output to work with JAXB.
          AnnotationTypeDoc doc = annotationDesc.annotationType();
          node.addAttribute( "type", doc.name() );
          node.addAttribute( "fulltype", doc.qualifiedName() );
          node.addAttribute( "package", doc.containingPackage().name() );
          ElementValuePair[] valuePairs = annotationDesc.elementValues();
          if (valuePairs.length>0) {
            XMLNode values = new XMLNode( "values" );  
            for (ElementValuePair valuePair : valuePairs)
            {
              XMLNode value = new XMLNode( "value" );  
              value.addAttribute("name", valuePair.element().name());
              String strVal = valuePair.value().toString();
              if (strVal.length()>1 && strVal.startsWith("\"") && strVal.endsWith("\"")) {
                value.addAttribute("value", strVal.substring(1, strVal.length()-1));
              } else {
                value.addAttribute("value", strVal);
              }
              values.addNode(value);
            }
            node.addNode(values);
          }
          annotationsNode.addNode(node);
        }
        classNode.addNode(annotationsNode);
      }
    }
    
    
}
