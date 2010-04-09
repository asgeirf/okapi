/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it 
  under the terms of the GNU Lesser General Public License as published by 
  the Free Software Foundation; either version 2.1 of the License, or (at 
  your option) any later version.

  This library is distributed in the hope that it will be useful, but 
  WITHOUT ANY WARRANTY; without even the implied warranty of 
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License 
  along with this library; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package net.sf.okapi.filters.xml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.its.ITSEngine;
import org.w3c.its.NSContextManager;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sf.okapi.common.DefaultEntityResolver;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.InlineCodeFinder;

public class Parameters implements IParameters {
	
	private static final String ESCAPEGT = "escapeGt";
	private static final String ESCAPENBSP = "escapeNbsp";
	private static final String ESCAPELINEBREAK = "escapeLineBreak";
	private static final String PROTECTENTITYREF = "protectEntityRef";
	private static final String LINEBREAKASCODE = "lineBreakAsCode";
	private static final String USECODEFINDER = "useCodeFinder";

	private static final String OKP_NS_PREFIX = "okp";
	private static final String OKP_NS_URI = "okapi-framework:xmlfilter-options";
	
	private final static String DEFAULTS = "<?xml version='1.0' ?>\n"
		+ "<its:rules version='1.0'\n"
		+ " xmlns:its='"+ITSEngine.ITS_NS_URI+"'\n"
		+ " xmlns:"+ITSEngine.XLINK_NS_PREFIX+"='"+ITSEngine.XLINK_NS_URI+"'\n"
		+ " xmlns:"+ITSEngine.ITSX_NS_PREFIX+"='"+ITSEngine.ITSX_NS_URI+"'\n"
		+ " xmlns:"+OKP_NS_PREFIX+"='"+OKP_NS_URI+"'\n"
		+ ">\n"
		+ "<!-- See ITS specification at: http://www.w3.org/TR/its/ -->\n"
		+ "</its:rules>\n";
	
	private String path;
	private URI docURI;
	private Document doc;
	private DocumentBuilder docBuilder;
	private XPath xpath;

	public boolean useCodeFinder;
	public InlineCodeFinder codeFinder;
	public boolean escapeGt;
	public boolean escapeNbsp;
	public boolean protectEntityRef;
	public boolean escapeLineBreak;
	public boolean lineBreakAsCode;
	
	public Parameters () {
		reset();
		// Create the document builder factory
		DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		fact.setNamespaceAware(true);
		fact.setValidating(false);
		// Create the document builder
		try {
			docBuilder = fact.newDocumentBuilder();
		}
		catch ( ParserConfigurationException e ) {
			throw new OkapiIOException(e);
		}
		
		docBuilder.setEntityResolver(new DefaultEntityResolver());

		// Macintosh work-around
		// When you use -XstartOnFirstThread as a java -Xarg on Leopard, your ContextClassloader gets set to null.
		// That is not the case on 10.4 or with Windows or Linux flavors
		// This allows XPathFactory.newInstance() to have a non-null context
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		// end work-around
		XPathFactory xpFact = XPathFactory.newInstance();
		xpath = xpFact.newXPath();
		NSContextManager nsContext = new NSContextManager();
		nsContext.addNamespace(OKP_NS_PREFIX, OKP_NS_URI);
		xpath.setNamespaceContext(nsContext);
	}

	@Override
	public String toString () {
		if ( doc == null ) {
			return DEFAULTS;
		}
		StringWriter sw = new StringWriter();
		Result result = new StreamResult(sw);
		// Write the DOM document to the file
		Transformer trans;
		try {
			trans = TransformerFactory.newInstance().newTransformer();
			trans.transform(new DOMSource(doc), result);
		}
		catch ( TransformerConfigurationException e ) {
			throw new OkapiIOException(e);
        }
		catch ( TransformerException e ) {
			throw new OkapiIOException(e);
		}
		return sw.toString();
	}
	
	@Override
	public void fromString (String data) {
		docURI = null;
		path = null;
		try {
			doc = docBuilder.parse(new InputSource(new StringReader(data)));
			getFilterOptions();
		}
		catch ( SAXException e ) {
			throw new OkapiIOException(e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
		catch ( XPathExpressionException e ) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public String getPath () {
		return path;
	}
	
	@Override
	public void setPath (String filePath) {
		path = filePath;
	}

	@Override
	public void load (URI inputURI,
		boolean ignoreErrors)
	{
		Reader sr = null;
		try {
			String tmp = inputURI.toString();
			if ( tmp.startsWith("jar:") ) {
				URL url = inputURI.toURL();
				doc = docBuilder.parse(url.openStream());
			}
			else {
				doc = docBuilder.parse(inputURI.toString());
			}
			path = inputURI.getPath();
			docURI = inputURI;
			getFilterOptions();
		}
		catch ( MalformedURLException e ) {
			throw new OkapiIOException(e);
		}
		catch ( UnsupportedEncodingException e ) {
			throw new OkapiIOException(e);
		}
		catch ( SAXException e ) {
			throw new OkapiIOException(e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
		catch ( XPathExpressionException e ) {
			throw new OkapiIOException(e);
		}
		finally {
			if ( sr != null ) {
				try {
					sr.close();
				} 
				catch ( IOException e ) {
					throw new OkapiIOException(e);
				}
			}
		}
	}

	@Override
	public void reset () {
		doc = null;
		docURI = null;
		codeFinder = new InlineCodeFinder();
		useCodeFinder = false;
		codeFinder.reset();
		escapeGt = true;
		escapeNbsp = true;
		protectEntityRef = true;
		escapeLineBreak = false;
		lineBreakAsCode = false;
	}

	@Override
	public void save (String filePath) {
		Result result = null;
		Transformer trans = null;
		try {
			if ( doc == null ) {
				fromString(DEFAULTS);
			}
			// Prepare the output file
			File f = new File(filePath);
			result = new StreamResult(f);
			// Write the DOM document to the file
			try {
				trans = TransformerFactory.newInstance().newTransformer();
				trans.transform(new DOMSource(doc), result);
			}
			catch ( TransformerConfigurationException e ) {
				throw new OkapiIOException(e);
	        }
			catch ( TransformerException e ) {
				throw new OkapiIOException(e);
			}
			// Update path and URI
			path = filePath;
			docURI = f.toURI();
		}
		finally {
			// Try to make sure the file is not locked
			trans = null;
			result = null;
			System.gc();
		}
	}

	public Document getDocument () {
		return doc;
	}
	
	public URI getURI () {
		return docURI;
	}

	@Override
	public boolean getBoolean (String name) {
		if ( name.equals(ESCAPEGT) ) return escapeGt;
		if ( name.equals(ESCAPENBSP) ) return escapeNbsp;
		if ( name.equals(PROTECTENTITYREF) ) return protectEntityRef;
		if ( name.equals(ESCAPELINEBREAK) ) return escapeLineBreak;
		if ( name.equals(LINEBREAKASCODE) ) return lineBreakAsCode;
		return false;
	}

	@Override
	public String getString (String name) {
		return null;
	}

	@Override
	public int getInteger (String name) {
		return 0;
	}

	@Override
	public ParametersDescription getParametersDescription () {
		return null;
	}

	private void getFilterOptions () throws XPathExpressionException {
		// Read the options element
		NodeList nl = (NodeList)xpath.evaluate("//"+OKP_NS_PREFIX+":options", doc, XPathConstants.NODESET);
		if ( nl.getLength() > 0 ) {
		// One element only
			Element elem = (Element)nl.item(0);
			String tmp = elem.getAttribute(ESCAPELINEBREAK);
			if ( !Util.isEmpty(tmp) ) {
				escapeLineBreak = tmp.equals("yes");
			}
			tmp = elem.getAttribute(ESCAPENBSP);
			if ( !Util.isEmpty(tmp) ) {
				escapeNbsp = tmp.equals("yes");
			}
			tmp = elem.getAttribute(LINEBREAKASCODE);
			if ( !Util.isEmpty(tmp) ) {
				lineBreakAsCode = tmp.equals("yes");
			}
			tmp = elem.getAttribute(ESCAPEGT);
			if ( !Util.isEmpty(tmp) ) {
				escapeGt = tmp.equals("yes");
			}
			tmp = elem.getAttribute(PROTECTENTITYREF);
			if ( !Util.isEmpty(tmp) ) {
				protectEntityRef = tmp.equals("yes");
			}
		}
		// Get the code finder data
		nl = (NodeList)xpath.evaluate("//"+OKP_NS_PREFIX+":codeFinder", doc, XPathConstants.NODESET);
		if ( nl.getLength() > 0 ) {
			// One element only
			Element elem = (Element)nl.item(0);
			String tmp = elem.getAttribute(USECODEFINDER);
			if ( !Util.isEmpty(tmp) ) {
				useCodeFinder = tmp.equals("yes");
			}
			tmp = Util.getTextContent(elem);
			if ( tmp == null ) tmp = "";
			codeFinder.fromString(tmp);
		}
	}

}
