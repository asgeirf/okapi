/*===========================================================================*/
/* Copyright (C) 2008 Asgeir Frimannsson, Jim Hargrave, Yves Savourel        */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA               */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.pipeline;

import net.sf.okapi.common.resource.Document;
import net.sf.okapi.common.resource.Group;
import net.sf.okapi.common.resource.SkeletonUnit;
import net.sf.okapi.common.resource.TextUnit;

public abstract class ThrougputPipeBase implements IResourceBuilder, IOutputPipe {

    private IResourceBuilder outputPipe;
    
    
    public void startResource(Document resource) {
    	if ( outputPipe != null ) outputPipe.startResource(resource);
    }

    public void endResource(Document resource) {
    	if ( outputPipe != null ) outputPipe.endResource(resource);        
    }

    public void startContainer(Group resource) {
    	if ( outputPipe != null ) outputPipe.startContainer(resource);
    }

    public void endContainer(Group resource) {
    	if ( outputPipe != null ) outputPipe.endContainer(resource);
    }

    public void startExtractionItem (TextUnit item) {
    	if ( outputPipe != null ) outputPipe.startExtractionItem(item);
    }

    public void endExtractionItem(TextUnit item) {
    	if ( outputPipe != null ) outputPipe.endExtractionItem(item);
    }

    public void skeletonContainer (SkeletonUnit resource) {
    	if ( outputPipe != null ) outputPipe.skeletonContainer(resource);
    }
    
    public void setOutput(IResourceBuilder inputBuilder) {
    	this.outputPipe = inputBuilder;
    }

}
