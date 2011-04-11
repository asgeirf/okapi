/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class XLIFFWriter {

    private PrintWriter writer = null;
    private String lb = System.getProperty("line.separator");
    private boolean isIndented = false;
    private String indent;

    public void create (File file ) {
		try {
			// Create the directories if needed
			String path = file.getCanonicalPath();
			int n = path.lastIndexOf('\\');
			if ( n == -1 ) path.lastIndexOf('/');
			if ( n > -1 ) {
				File dir = new File(path.substring(0, n));
				dir.mkdirs();
			}
			// Create the file
			create(new OutputStreamWriter(
				new BufferedOutputStream(new FileOutputStream(file)), "UTF-8"));
		}
		catch ( FileNotFoundException e ) {
			throw new XLIFFWriterException("Cannote create document.", e);
		}
		catch ( UnsupportedEncodingException e ) {
			throw new XLIFFWriterException("Unsupported encoding.", e);
		}
		catch ( IOException e ) {
			throw new XLIFFWriterException("Cannote create document.", e);
		}
    }

    public void create (Writer output) {
		writer = new PrintWriter(output);
		indent = "";
	}
    
    public void setLineBreak (String lineBreak) {
    	lb = lineBreak;
    }
    
    public String getLineBreak () {
    	return lb;
    }
    
    public void setIsIndented (boolean isIndented) {
    	this.isIndented = isIndented;
    }
    
    public boolean getIsIndented () {
    	return isIndented;
    }
	
	public void close () {
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
	}
	
	public void writeUnit (Unit unit) {
		writer.print(indent+String.format("<unit id=\"%s\"", toXML(unit.getId(), true)));
		writer.print(">"+lb);
		if ( isIndented ) indent += " ";
		
		for ( Segment seg : unit ) {
			writer.print(indent+"<segment>"+lb);
			if ( isIndented ) indent += " ";
			// Leading parts to ignore
			for ( Fragment frag : seg.getLeadingParts() ) {
				writeFragment("ignorable", frag);
			}
			// Source
			writeFragment("source", seg.getSource());
			// Target
			if ( seg.hasTarget() ) {
				writeFragment("target", seg.getTarget());
			}
			// Trailing parts to ignore
			for ( Fragment frag : seg.getTrailingParts() ) {
				writeFragment("ignorable", frag);
			}
			
			if ( seg.getCandidates().size() > 0 ) {
				writer.print(indent+"<matches>"+lb);
				if ( isIndented ) indent += " ";
				
				for ( Alternate alt : seg.getCandidates() ) {
					writer.print(indent+"<match>"+lb);
					if ( isIndented ) indent += " ";
					writeFragment("source", alt.getSource());
					writeFragment("target", alt.getTarget());
					if ( isIndented ) indent = indent.substring(1);
					writer.print(indent+"</match>"+lb);
				}

				if ( isIndented ) indent = indent.substring(1);
				writer.print(indent+"</matches>"+lb);
			}
			
			if ( isIndented ) indent = indent.substring(1);
			writer.print(indent+"</segment>"+lb);
		}

		if ( isIndented ) indent = indent.substring(1);
		writer.print(indent+"</unit>"+lb);
	}

	public void writeStartDocument () {
		writer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lb);
		writer.print("<xliff version=\"2.0\">"+lb);
		
		writer.print("<!-- This output is EXPERIMENTAL only. -->"+lb);
		writer.print("<!-- XLIFF 2.0 is not defined yet. -->"+lb);
		writer.print("<!-- For feedback or more info, please see the XLIFF TC (http://www.oasis-open.org/committees/xliff) -->"+lb);
		if ( isIndented ) indent += " ";
	}
	
	public void writeEndDocument () {
		if ( isIndented ) indent = indent.substring(1);
		writer.print("</xliff>"+lb);
	}
	
	private void writeFragment (String name,
		Fragment fragment)
	{
		writer.print(indent+"<"+name+">");
		writer.print(fragment.toString());
		writer.print("</"+name+">"+lb);
	}

	private String toXML (String text,
		boolean attribute)
	{
		text = text.replace("&", "&amp;");
		text = text.replace("<", "&lt;");
		if ( attribute ) {
			text = text.replace("\"", "&quot;");
		}
		return text;
	}

}
