/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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


package net.sf.okapi.common.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.sf.okapi.common.Range;
import net.sf.okapi.common.Util;

public class Segments implements ISegments {
    private AlignmentStatus alignmentStatus = AlignmentStatus.NOT_ALIGNED;

    //TODO currently this relies on a few protected methods and fields in the
    // parent text container. Consider whether the implementation can be improved
    // by providing more encapsulated access to these
    private TextContainer parent;

    public Segments(TextContainer parent) {
        this.parent = parent;
    }

    public Iterator<Segment> iterator() {
            return new Iterator<Segment>() {
                    int current = foundNext(-1);
                    private int foundNext (int start) {
                            for ( int i=start+1; i<parent.parts.size(); i++ ) {
                                    if ( parent.parts.get(i).isSegment() ) {
                                            return i;
                                    }
                            }
                            return -1;
                    }

                    @Override
                    public void remove () {
                            throw new UnsupportedOperationException("The method remove() not supported.");
                    }

                    @Override
                    public Segment next () {
                            if ( current == -1 ) {
                                    throw new NoSuchElementException("No more content parts.");
                            }
                            int n = current;
                            // Get next here because hasNext() could be called several times
                            current = foundNext(current);
                            // Return 'previous' current
                            return (Segment)parent.parts.get(n);
                    }

                    @Override
                    public boolean hasNext () {
                            return (current != -1);
                    }
            };
    };

    @Override
    public List<Segment> asList () {
            ArrayList<Segment> segments = new ArrayList<Segment>();
            for ( TextPart part : parent.parts ) {
                    if ( part.isSegment() ) {
                            segments.add((Segment)part);
                    }
            }
            return segments;
    }

    @Override
    public void swap (int segIndex1,
            int segIndex2)
    {
            int partIndex1 = getPartIndex(segIndex1);
            int partIndex2 = getPartIndex(segIndex2);
            if (( partIndex1 == -1 ) || ( partIndex2 == -1 )) {
                    return; // At least one index is wrong: do nothing
            }
            TextPart tmp = parent.parts.get(partIndex1);
            parent.parts.set(partIndex1, parent.parts.get(partIndex2));
            parent.parts.set(partIndex2, tmp);
    }


    @Override
    public void append (Segment segment,
            boolean collapseIfPreviousEmpty)
    {
            append(segment, null, collapseIfPreviousEmpty);
    }

    @Override
    public void append (Segment segment) {
            append(segment, true);
    }

    @Override
    public void append (Segment segment,
            String textBefore,
            boolean collapseIfPreviousEmpty)
    {
            // Add the text before if needed
            if ( !Util.isEmpty(textBefore) ) {
                    if (( parent.parts.get(parent.parts.size()-1).getContent().isEmpty() )
                            && !parent.parts.get(parent.parts.size()-1).isSegment() )
                    {
                            parent.parts.set(parent.parts.size()-1, new TextPart(textBefore));
                    }
                    else {
                            parent.parts.add(new TextPart(textBefore));
                    }
            }

            // If the last segment is empty and at the end of the content: re-use it
            if ( collapseIfPreviousEmpty ) {
                    if (( parent.parts.get(parent.parts.size()-1).getContent().isEmpty() )
                            && parent.parts.get(parent.parts.size()-1).isSegment() )
                    {
                            parent.parts.set(parent.parts.size()-1, segment);
                    }
                    else {
                            parent.parts.add(segment);
                    }
            }
            else {
                    parent.parts.add(segment);
            }

            parent.validateSegmentId(segment);
            parent.segApplied = true;
    }

    @Override
    public void append (Segment segment,
            String textBefore)
    {
            append(segment, textBefore, true);
    }

    @Override
    public void append (TextFragment fragment,
            boolean collapseIfPreviousEmpty)
    {
            append(new Segment(null, fragment), collapseIfPreviousEmpty);
    }

    @Override
    public void append (TextFragment fragment) {
            append(fragment, true);
    }

    @Override
    public void set (int index,
            Segment seg)
    {
            int n = getPartIndex(index);
            if ( n < -1 ) {
                    throw new IndexOutOfBoundsException("Invalid segment index: "+index);
            }
            parent.parts.set(n, seg);
            parent.validateSegmentId(seg);
    }

    @Override
    public void insert (int index,
            Segment seg)
    {
            // If the index is the one after the last segment: we append
            if ( index == count() ) {
                    append(seg, true);
                    return;
            }
            // Otherwise it has to exist
            int n = getPartIndex(index);
            if ( n < -1 ) {
                    throw new IndexOutOfBoundsException("Invalid segment index: "+index);
            }
            parent.parts.add(n, seg);
            parent.validateSegmentId(seg);
    }

    @Override
    public int create (List<Range> ranges) {
            // Do nothing if null or empty
            if (( ranges == null ) || ranges.isEmpty() ) return 0;

            // If the current content is a single segment we start from it
            TextFragment holder;
            if ( parent.parts.size() == 1  ) {
                    holder = parent.parts.get(0).getContent();
            }
            else {
                    holder = parent.createJoinedContent(null);
            }

            // Reset the segments
            parent.parts = new ArrayList<TextPart>();

            // Extract the segments using the ranges
            int start = 0;
            int id = 0;
            for ( Range range : ranges ) {
                    if ( range.end == -1 ) {
                            range.end = holder.text.length();
                    }
                    // Check boundaries
                    if ( range.end < range.start ) {
                            throw new InvalidPositionException(String.format(
                                    "Invalid segment boundaries: start=%d, end=%d.", range.start, range.end));
                    }
                    if ( start > range.start ) {
                            throw new InvalidPositionException("Invalid range order.");
                    }
                    if ( range.end == range.start ) {
                            // Empty range, skip it
                            continue;
                    }
                    // If there is an interstice: creates the corresponding part
                    if ( start < range.start ) {
                            parent.parts.add(new TextPart(holder.subSequence(start, range.start)));
                    }
                    // Create the part for the segment
                    // Use existing id if possible, otherwise use local counter
                    Segment seg = new Segment(((range.id == null) ? String.valueOf(id++) : range.id),
                            holder.subSequence(range.start, range.end));
                    parent.parts.add(seg);
                    parent.validateSegmentId(seg);
                    start = range.end;
                    parent.segApplied = true;
            }

            // Check if we have remaining text after the last segment
            if ( start < holder.text.length() ) {
                    if ( start == 0 ) { // If the remain is the whole content: make it a segment
                            parent.parts.add(new Segment(String.valueOf(id), holder));
                            // That is the only segment: no need to validate the id
                    }
                    else { // Otherwise: make it an interstice
                            parent.parts.add(new TextPart(holder.subSequence(start, -1)));
                    }
            }

            return parent.parts.size();
    }

    @Override
    public int create (int start,
            int end)
    {
            ArrayList<Range> range = new ArrayList<Range>();
            range.add(new Range(start, end));
            return create(range);
    }

    @Override
    public int count () {
            int count = 0;
            for ( TextPart part : parent.parts ) {
                    if ( part.isSegment() ) {
                            count++;
                    }
            }
            return count;
    }

    @Override
    public TextFragment getFirstContent () {
            for ( TextPart part : parent.parts ) {
                    if ( part.isSegment() ) {
                            return part.getContent();
                    }
            }
            // Should never occur
            return null;
    }

    @Override
    public TextFragment getLastContent () {
            for ( int i=parent.parts.size()-1; i>=0; i-- ) {
                    if ( parent.parts.get(i).isSegment() ) {
                            return parent.parts.get(i).getContent();
                    }
            }
            // Should never occur
            return null;
    }

    @Override
    public Segment getLast () {
            for ( int i=parent.parts.size()-1; i>=0; i-- ) {
                    if ( parent.parts.get(i).isSegment() ) {
                            return (Segment)parent.parts.get(i);
                    }
            }
            // Should never occur
            return null;
    }

    @Override
    public Segment get (String id) {
            for ( TextPart part : parent.parts ) {
                    if ( part.isSegment() ) {
                            if ( ((Segment)part).id.equals(id) ) return (Segment)part;
                    }
            }
            // Should never occur
            return null;
    }

    @Override
    public Segment get (int index) {
            int tmp = -1;
            for ( TextPart part : parent.parts ) {
                    if ( part.isSegment() ) {
                            if ( ++tmp == index ) {
                                    return (Segment)part;
                            }
                    }
            }
            // Should never occur
            return null;
    }

    @Override
    public void joinAll () {
            // Merge but don't remember the ranges
            parent.setContent(parent.createJoinedContent(null));
    }

    @Override
    public void joinAll (List<Range> ranges) {
            parent.setContent(parent.createJoinedContent(ranges));
    }

    @Override
    public List<Range> getRanges () {
            List<Range> ranges = new ArrayList<Range>();
            parent.createJoinedContent(ranges);
            return ranges;
    }

    @Override
    public int joinWithNext (int segmentIndex) {
            // Check if we have something to join to
            if ( parent.parts.size() == 1 ) {
                    return 0; // Nothing to do
            }

            // Find the part for the segment index
            int start = getPartIndex(segmentIndex);
            // Check if we have a segment at such index
            if ( start == -1 ) {
                    return 0; // Not found
            }

            // Find the next segment
            int end = -1;
            for ( int i=start+1; i<parent.parts.size(); i++ ) {
                    if ( parent.parts.get(i).isSegment() ) {
                            end = i;
                            break;
                    }
            }

            // Check if we have a next segment
            if ( end == -1 ) {
                    // No more segment to join
                    return 0;
            }

            TextFragment tf = parent.parts.get(start).getContent();
            int count = (end-start);
            int i = 0;
            while ( i < count ) {
                    tf.append(parent.parts.get(start+1).getContent());
                    parent.parts.remove(start+1);
                    i++;
            }

            // Do not reset segApplied if one part only: keep the info that is was segmented
            return count;
    }

    @Override
    public int getPartIndex (int segIndex) {
            int n = -1;
            for ( int i=0; i<parent.parts.size(); i++ ) {
                    if ( parent.parts.get(i).isSegment() ) {
                            n++;
                            if ( n == segIndex ) return i;
                    }
            }
            return -1; // Not found
    }

    @Override
    public int getIndex (String segId) {
            int n = 0;
            for ( int i=0; i<parent.parts.size(); i++ ) {
                    if ( parent.parts.get(i).isSegment() ) {
                            if ( segId.equals(((Segment)parent.parts.get(i)).id) ) return n;
                            // Else, move to the next
                            n++;
                    }
            }
            return -1; // Not found
    }

    @Override
    public AlignmentStatus getAlignmentStatus() {
            return alignmentStatus;
    }

    @Override
    public void setAlignmentStatus(AlignmentStatus alignmentStatus) {
            this.alignmentStatus = alignmentStatus;
    }
}