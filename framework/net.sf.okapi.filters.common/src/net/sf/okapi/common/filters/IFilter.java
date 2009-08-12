/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.filters;

import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

/**
 * Common set of methods to extract translatable text and its associated data.
 * <p>The following example shows a typical use of IFilter:
 * <pre>
 * MyUtlity myUtility = new MyUtility(); // Some object that do things with filter events
 * IFilter filter = new MyFilter(); // A filter implementation
 * filter.open(new RawDocument(URI("myFile.ext"), "UTF-8", "en");
 * while ( filter.hasNext() ) {
 *    myUtility.handleEvent(filter.next());
 * }
 * filter.close();
 * </pre>
 */
public interface IFilter {	

	/**
	 * Gets the name of this filter.
	 * @return The name of the filter.
	 */
	public String getName ();

	/**
	 * Opens the input document described in a give RawDocument object.
	 * Skeleton information is always created when you use this method.
	 * @param input The RawDocument object to use to open the document.
	 */
	public void open (RawDocument input);

	/**
	 * Opens the input document described in a give RawDocument object, and
	 * optionally creates skeleton information.
	 * @param input The RawDocument object to use to open the document.
	 */
	public void open (RawDocument input,
		boolean generateSkeleton);

	/**
	 * Closes the input document. Developers should call this method from within their code
	 * before sending the last event: This can allow writer objects to overwrite the input file
	 * when they receive the last event. This method must also be safe to call even
	 * if the input document is not opened.
	 */
	public void close ();

	/**
	 * Indicates if there is an event to process.
	 * @return True if there is at least one event to process, false if not.
	 */
	public boolean hasNext ();

	/**
	 * Gets the next event available.
	 * @return The next event available or null if there are no events.
	 */
	public Event next ();	

	/**
	 * Cancels the current process.
	 */
	public void cancel ();

	/**
	 * Gets the current parameters for this filter.
	 * @return The current parameters for this filter.
	 */
	public IParameters getParameters ();

	/**
	 * Sets new parameters for this filter.
	 * @param params The new parameters to use.
	 */
	public void setParameters (IParameters params);

	/**
	 * Creates a new ISkeletonWriter object that corresponds to the type of skeleton 
	 * this filter uses.
	 * @return A new instance of ISkeletonWriter for the type of skeleton this filter
	 * uses.
	 */
	public ISkeletonWriter createSkeletonWriter ();
	
	/**
	 * Creates a new IFilterWriter object from the most appropriate class to
	 * use with this filter.
	 * @return A new instance of IFilterWriter for the preferred implementation
	 * for this filter.
	 */
	public IFilterWriter createFilterWriter ();

	/**
	 * Gets the MIME type of the format supported by this filter.
	 * @return The MIME type of the format supported by this filter.
	 */
	public String getMimeType ();

	/**
	 * Gets the list of all predefined configurations for this filter. 
	 * @return a list of the all predefined configurations for this filter.
	 */
	public List<FilterConfiguration> getConfigurations();
	
}
