/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.filters.drupal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

public class Manual {

	public static void main (String[] args) {
		//testFilter();
		testConnector();
	}
	
	private static  void testConnector () {
		String root = TestUtil.getParentDir(Manual.class, "/test.drp");
		File file = new File(root+"/test.drp");
		Project prj = new Project();
		DrupalConnector cli = null;
		try {
			prj.read(new BufferedReader(new FileReader(file)), LocaleId.ENGLISH, LocaleId.FRENCH);
			cli = new DrupalConnector(prj.getHost());
			cli.setCredentials(prj.getUser(), prj.getPassword());
			cli.login();
			
			Node node = cli.getNode("10", prj.getSourceLocale().toString(), false);
			
			boolean hasTarget = node.hasLanguageForBody(prj.getTargetLocale().toString());
			
			node.setTitle(prj.getTargetLocale().toString(), "New title text");
			cli.updateNode(node);
		}
		catch (Throwable e ) {
			e.printStackTrace();
		}
		finally {
			if ( cli != null ) {
				cli.logout();
			}
		}
	}
	
	private static void testFilter () {
		LocaleId locEN = LocaleId.ENGLISH;
		DrupalFilter filter = null;
		boolean merge = true;
		IFilterWriter writer = null;

		try {
			String root = TestUtil.getParentDir(Manual.class, "/test.drp");

			filter = new DrupalFilter();
			
			File file = new File(root+"/test.drp");
			RawDocument rd = new RawDocument(file.toURI(), "UTF-8", locEN);
			
			Parameters params = (Parameters)filter.getParameters();
			params.setOpenProject(false);

			filter.open(rd);
			if ( merge ) {
				writer = filter.createFilterWriter();
				writer.setOptions(locEN, "UTF-8");
				// writer.setOutput() not needed
			}

			while ( filter.hasNext() ) {
				Event event = filter.next();
				System.out.println(event.toString()+" ("+event.getResource().getId()+")");
				if ( event.isTextUnit() ) {
					ITextUnit tu = event.getTextUnit();
					System.out.println(" ="+tu.getType());
					System.out.println(" ="+tu.getSource().toString());
					if ( merge ) {
						// Change content to verify the merge
						TextContainer tc = tu.createTarget(locEN, true, IResource.COPY_ALL);
						String ctext = tc.getFirstContent().getCodedText();
						if (( ctext.indexOf('\u0114') != -1 ) || ( ctext.indexOf('\u00d8') != -1 )) {
							ctext = ctext.replaceAll("\u0114", "e");
							ctext = ctext.replaceAll("\u00d8", "o");
						}
						else {
							ctext = ctext.replaceAll("e", "\u0114");
							ctext = ctext.replaceAll("o", "\u00d8");
						}
						tc.getFirstContent().setCodedText(ctext);
					}
				}
				else if ( event.isStartGroup() ) {
					System.out.println(" grp-type="+event.getStartGroup().getType());
				}
				
				if ( merge ) {
					writer.handleEvent(event);
				}
			}
			
		}
		catch ( Throwable e ) {
			e.printStackTrace();
		}
		finally {
			if ( filter != null ) {
				filter.close();
			}
			if ( writer != null ) {
				writer.close();
			}
		}
	}

}
