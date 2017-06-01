package com.jeldoclet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * Represents an XML node
 * 
 * History:
 * 		Sep 14th, 2005 - Updated by TP to for better xml output formatting.  
 *                     - Added save method.
 *                     - Added support for a namespace (http://xml.jeldoclet.com)
 * 
 * Author: Jack D. Herrington <jack_d_herrington@codegeneration.net>
 * 		   Toby Patke 		  <toby_patke _?_ hotmail.com>
 */
public class XMLNode
{

	/**
	 * Used in the toString method to provide a carriage-return + line-feed. 
	 */
	private static final String crlf = System.getProperty("line.separator");
	
	/**
	 * The type of the node
	 */
	private String _type;
	
	/**
	 * Sets the processing instruction to be written when the object is serialized.
	 */
	private static String _processingInstructions = "";
	
	/**
	 * Sets the namespace to be written when the object is serialized.
	 */
	private static final String _namespace = "http://xml.jeldoclet.com";
	
	/**
	 * Sets the namespace prefix to be written when the object is serialized.
	 */
	private static String _namespacePrefix = "";

	/**
	 * The attributes
	 */
	private HashMap _attributes;

	/**
	 * The interior nodes
	 */
	private Vector _nodes;

	/**
	 * The interior text
	 */
	private StringBuffer _text;
	
	/**
	 * Constructs the XMLNode.
	 * 
	 * @param type The type name of the node
	 */
	public XMLNode( String type )
	{
		_type = type;
		_attributes = new HashMap();
		_nodes = new Vector();
		_text = new StringBuffer();
	}
	
	/**
	 * Adds an attribute to the node
	 * 
	 * @param name The name of the attribute.
	 * @param value The value for the attribute
	 */
	public void addAttribute( String name, String value )
	{
		_attributes.put( name, value );
	}
	
	/**
	 * Returns the specified attributed.
	 * 
	 * @param name The key for the value to be retrieved.
	 * @return The value stored in the attribute hash for the given key.
	 */
	public String getAttribute( String name )
	{
		return (String) _attributes.get( name );
	}

	/**
	 * Adds an interior node to the XMLNode.
	 * 
	 * @param node The node
	 */
	public void addNode( XMLNode node )
	{
		_nodes.add( node );
	}

	/**
	 * Adds text to the interior of the node.
	 * 
	 * @param text The node
	 */
	public void addText( String text )
	{
		_text.append( text );
	}
	

	/**
	 * thz: compatibility: original call w/o output encoding
	 */
	public void save(String dir, String fileName, boolean includeNamespace)
	{
		this.save(dir, fileName, includeNamespace, "");
	}

	/**
	 * Saves this XML node to the directory specified.
	 * 
	 * @param dir The directory to save this node to.
	 *
	 * thz: added output encoding
	 */
	public void save(String dir, String fileName, boolean includeNamespace, String outputEncoding)
	{
		try 
		{

			if(includeNamespace)
			{
				// thz
				if(outputEncoding.equals("") == true)
					outputEncoding = "UTF-8";
				// /thz

				_processingInstructions = "<?xml version=\"1.0\" encoding=\"" + outputEncoding + "\" standalone=\"yes\"?>" + crlf;
				_namespacePrefix = "xs";
				this.addAttribute("xmlns:" + _namespacePrefix, _namespace);
				_namespacePrefix = _namespacePrefix + ":";
			}
			// thz
			else
			{
				if(outputEncoding.equals("") == false)
					_processingInstructions = "<?xml version=\"1.0\" encoding=\"" + outputEncoding + "\"?>" + crlf;
			}
			// /thz
			
			//FileWriter cannot write international characters properly, see http://stackoverflow.com/a/9853008/4123249
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(dir + fileName ), outputEncoding);
			out.write( _processingInstructions );
			out.write( this.toString("") );
			out.close();
		}
		catch( IOException e )
		{
            System.err.println("Could not create '" + dir + fileName + "'");
            e.printStackTrace();
		}
	}

	/** 
	 * Converts the XML node to a String.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString(String tabs)
	{
	  StringBuilder out = new StringBuilder();
		
		out.append( tabs + "<" + _namespacePrefix + _type );
		Iterator attrIterator = _attributes.keySet().iterator();
		while( attrIterator.hasNext() )
		{
			String key = (String)attrIterator.next();
			out.append( " " + key + "=\"" + encode( (String)_attributes.get( key ) ) + "\"" );
		}

		Iterator nodeIterator = _nodes.iterator();
		
		if( _text.length() <= 0 && 
			! nodeIterator.hasNext() )
		{
			out.append( " />" + crlf); 
			return out.toString();
		}
		
		out.append( ">" + crlf);  

		if( _text.length() > 0 )
		{
			//Wrapping text in a seperate node allows for good presentation of data with out adding extra data.
			out.append( tabs + "\t<" + _namespacePrefix + "description>" + encode( _text.toString() ) + 
								"</" + _namespacePrefix + "description>" + crlf ); 
		}

		while( nodeIterator.hasNext() )
		{
			XMLNode node = (XMLNode)nodeIterator.next();
			out.append( node.toString(tabs + "\t") );
		}

		out.append( tabs + "</" + _namespacePrefix + _type + ">" + crlf  + 
				( "class".equalsIgnoreCase( _type ) ? crlf : "" ));
		
		return out.toString();
	}
	
//	/** 
//	 * Encodes strings as XML. Check for <, >, ', ", &.
//	 * 
//	 * @param in The input string
//	 * @return The encoded string.
//	 */
//	static protected String encode( String in )
//	{
//		Pattern ampPat = Pattern.compile( "&" );
//		Pattern ltPat = Pattern.compile( "<" );
//		Pattern gtPat = Pattern.compile( ">" );
//		Pattern aposPat = Pattern.compile( "\'" );
//		Pattern quotPat = Pattern.compile( "\"" );
//
//		String out = new String( in );
//
//		out = (ampPat.matcher(out)).replaceAll("&amp;");
//		out = (ltPat.matcher(out)).replaceAll("&lt;");
//		out = (gtPat.matcher(out)).replaceAll("&gt;");
//		out = (aposPat.matcher(out)).replaceAll("&apos;");
//		out = (quotPat.matcher(out)).replaceAll("&quot;");
//
//		return out;
//	}

	/**
	 * Returns the string where all non-ascii and <, &, > are encoded as numeric entities. I.e. "&lt;A &amp; B &gt;"
	 * .... (insert result here). The result is safe to include anywhere in a text field in an XML-string. If there was
	 * no characters to protect, the original string is returned.
	 * 
	 * @param originalUnprotectedString
	 *            original string which may contain characters either reserved in XML or with different representation
	 *            in different encodings (like 8859-1 and UFT-8)
	 * @see https://stackoverflow.com/questions/439298/best-way-to-encode-text-data-for-xml-in-java
	 * @return
	 */
	 static String encode(String originalUnprotectedString) {
	    if (originalUnprotectedString == null) {
	        return null;
	    }
	    boolean anyCharactersProtected = false;

	    StringBuilder stringBuffer = new StringBuilder(originalUnprotectedString.length());
	    for (int i = 0; i < originalUnprotectedString.length(); i++) {
	        char ch = originalUnprotectedString.charAt(i);

	        if (ch<32 || ch>126) {
	          // control characters or unicode but not Ascii
            stringBuffer.append("&#" + (int) ch + ";");
            anyCharactersProtected = true;
	        } else
	           switch (ch)
            {
            case '<':
              stringBuffer.append("&lt;");
              anyCharactersProtected = true;
              break;
            case '>':
              stringBuffer.append("&gt;");
              anyCharactersProtected = true;
              break;
            case '&':
              stringBuffer.append("&amp;");
              anyCharactersProtected = true;
              break;
            case '\'':
              stringBuffer.append("&apos;");
              anyCharactersProtected = true;
              break;
            case '"':
              stringBuffer.append("&quot;");
              anyCharactersProtected = true;
              break;
            default:
              stringBuffer.append(ch);
              anyCharactersProtected = true;
            }
	    }
	    if (anyCharactersProtected == false) {
	        return originalUnprotectedString;
	    }

	    return stringBuffer.toString();
	}
}
