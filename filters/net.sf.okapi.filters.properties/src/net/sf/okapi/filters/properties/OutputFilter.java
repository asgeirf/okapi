package net.sf.okapi.filters.properties;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import net.sf.okapi.common.filters.IOutputFilter;
import net.sf.okapi.common.resource.IExtractionItem;
import net.sf.okapi.common.resource.IResource;
import net.sf.okapi.common.resource.IResourceContainer;

public class OutputFilter implements IOutputFilter {

	private Resource              res;
	private OutputStream          output;
	private String                encoding;
	private OutputStreamWriter    writer;
	private CharsetEncoder        outputEncoder;
	
	public void initialize (OutputStream output,
		String encoding) {
		this.output = output;
		this.encoding = encoding;
	}

	public void close ()
		throws Exception
	{
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
	}

	public void endContainer (IResourceContainer resourceCntainer) {
	}

	public void endExtractionItem (IExtractionItem extractionItem) {
		try {
			// Write the buffer
			writer.write(res.buffer.toString());
			// Then write the item content
			writer.write(escape(extractionItem.getContent().toString()));
			if ( res.endingLB ) writer.write(res.lineBreak);
		}
		catch ( Exception e ) {
			System.err.println(e.getLocalizedMessage());
		}
	}

	public void endResource (IResource resource) {
		try {
			writer.write(res.buffer.toString());
		}
		catch ( Exception e ) {
			System.err.println(e.getLocalizedMessage());
		}
		finally {
			try {
				close();
			}
			catch ( Exception e ) {
				System.err.println(e.getLocalizedMessage());
			}
		}
	}

	public void startContainer (IResourceContainer resourceContainer) {
	}

	public void startExtractionItem (IExtractionItem extractionItem) {
	}

	public void startResource (IResource resource) {
		try {
			// Save the resource for later use
			res = (Resource)resource;
			
			// Create the output writer from the provided stream
			writer = new OutputStreamWriter(
				new BufferedOutputStream(output), encoding);
			outputEncoder = Charset.forName(encoding).newEncoder(); 
			
			// Write the buffer
			writer.write(res.buffer.toString());
		}
		catch ( Exception e ) {
			System.err.println(e.getLocalizedMessage());
		}
	}

	private String escape (String text) {
		StringBuilder escaped = new StringBuilder();
		for ( int i=0; i<text.length(); i++ ) {
			if ( text.codePointAt(i) > 127 ) {
				if ( res.params.escapeExtendedChars ) {
					escaped.append(String.format("\\u%04x", text.codePointAt(i))); 
				}
				else {
					if ( outputEncoder.canEncode(text.charAt(i)) )
						escaped.append(text.charAt(i));
					else
						escaped.append(String.format("\\u%04x", text.codePointAt(i)));
				}
			}
			else {
				switch ( text.charAt(i) ) {
				case '\n':
					escaped.append("\\n");
					break;
				case '\t':
					escaped.append("\\t");
					break;
				default:
					escaped.append(text.charAt(i));
					break;
				}
			}
		}
		return escaped.toString();
	}

}
