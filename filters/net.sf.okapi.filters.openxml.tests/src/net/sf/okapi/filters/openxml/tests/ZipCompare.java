/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml.tests;

//import java.io.File;
import java.io.File;
import java.io.InputStream;
//import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.okapi.common.exceptions.OkapiFileNotFoundException;

public class ZipCompare {

	private FileCompare fc=null;
	public ZipCompare()
	{
		fc = new FileCompare();
	}
	
	public boolean zipsExactlyTheSame(String out, String gold)
	{
		boolean bRslt=true,bFoundit=false;
		ZipFile oZipFile,gZipFile;
		ZipEntry oZipEntry,gZipEntry;
		String sOEntryName,sGEntryName;
		InputStream ois,gis;
		Enumeration<? extends ZipEntry> oEntries;
		Enumeration<? extends ZipEntry> gEntries;
		try
		{
			File oZip = new File(out);
			oZipFile = new ZipFile(oZip);
			oEntries = oZipFile.entries();
		}
		catch(Exception e)
		{
			throw new OkapiFileNotFoundException("Output file "+out+" not found.");
		}
		try
		{
			File gZip = new File(gold);
			gZipFile = new ZipFile(gZip);
			gEntries = gZipFile.entries();
		}
		catch(Exception e)
		{
			throw new OkapiFileNotFoundException("Gold file "+gold+" not found.");
		}
		while( oEntries.hasMoreElements() )
		{
			oZipEntry = oEntries.nextElement();
			sOEntryName = oZipEntry.getName();
			bFoundit = false;
			while(gEntries.hasMoreElements())
			{
				gZipEntry = gEntries.nextElement();
				sGEntryName = gZipEntry.getName();
				if (sOEntryName.equals(sGEntryName))
				{
					bFoundit = true;
					try
					{
						ois = oZipFile.getInputStream(oZipEntry);
						gis = gZipFile.getInputStream(gZipEntry);
					}
					catch(Exception e)
					{
						return false;
					}
					bRslt = fc.filesExactlyTheSame(ois,gis);
					if (!bRslt)
						return false;
					break;
				}
			}
			if (!bFoundit)
				return false;
		}
		while( gEntries.hasMoreElements() )
		{
			gZipEntry = gEntries.nextElement();
			sGEntryName = gZipEntry.getName();
			bFoundit = false;
			while(oEntries.hasMoreElements())
			{
				oZipEntry = oEntries.nextElement();
				sOEntryName = oZipEntry.getName();
				if (sGEntryName.equals(sOEntryName))
				{
					bFoundit = true;
					break;
				}
			}
			if (!bFoundit)
				return false;
		}
		return bRslt;
	}
}
