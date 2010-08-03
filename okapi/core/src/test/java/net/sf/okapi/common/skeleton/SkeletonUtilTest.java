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

package net.sf.okapi.common.skeleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

import org.junit.Test;

public class SkeletonUtilTest {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private static final LocaleId ESES = new LocaleId("es", "es");
	private static final LocaleId FRFR = new LocaleId("fr", "fr");
	
	@Test
	public void testParts() {
		TextUnit tu1 = new TextUnit("tu1");
		TextUnit tu2 = new TextUnit("tu2");
		
		GenericSkeleton skel1 = new GenericSkeleton();
		tu1.setSkeleton(skel1);
		assertEquals(0, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text1");
		assertEquals(1, SkeletonUtil.getNumParts(skel1));
		
		skel1.addContentPlaceholder(tu1);
		assertEquals(2, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text2");
		assertEquals(3, SkeletonUtil.getNumParts(skel1));
		
		skel1.attachParent(tu1);
		assertEquals(3, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text3");
		assertEquals(4, SkeletonUtil.getNumParts(skel1));
		
		skel1.addContentPlaceholder(tu1, ESES); 
		assertEquals(5, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text4");
		assertEquals(6, SkeletonUtil.getNumParts(skel1));
		
		skel1.addValuePlaceholder(tu1, "prop", ESES); 
		assertEquals(7, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text5");
		assertEquals(8, SkeletonUtil.getNumParts(skel1));
		
		skel1.addValuePlaceholder(tu1, "prop", LocaleId.EMPTY);
		assertEquals(9, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text6");
		assertEquals(10, SkeletonUtil.getNumParts(skel1));
		
		skel1.addValuePlaceholder(tu1, "prop", null);
		assertEquals(11, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text7");
		assertEquals(12, SkeletonUtil.getNumParts(skel1));
		
		skel1.addReference(tu2);
		assertEquals(13, SkeletonUtil.getNumParts(skel1));
		
		skel1.add("text8");
		assertEquals(14, SkeletonUtil.getNumParts(skel1));
	}
	
	@Test
	public void testTypes() {
		TextUnit tu1 = new TextUnit("tu1");
		TextUnit tu2 = new TextUnit("tu2");
		GenericSkeleton skel1 = new GenericSkeleton();
		GenericSkeleton skel2 = new GenericSkeleton();
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		tu2.setSourceContent(new TextFragment("source"));
		tu2.setTargetContent(ESES, new TextFragment("target"));
		tu2.setProperty(new Property("res_prop", "res_prop_value"));
		tu2.setSourceProperty(new Property("src_prop", "src_prop_value"));
		tu2.setTargetProperty(ESES, new Property("trg_prop", "trg_prop_value"));
		
		assertEquals(0, SkeletonUtil.getNumParts(skel1));
		skel1.addContentPlaceholder(tu1); // @@@
		assertEquals(1, SkeletonUtil.getNumParts(skel1));
				
		GenericSkeletonPart p1 = SkeletonUtil.getPart(skel1, 0); 
		assertTrue(SkeletonUtil.isSourcePlaceholder(tu1, p1));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu1, p1));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu1, p1));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu1, p1));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu1, p1));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu1, p1));
		assertFalse(SkeletonUtil.isReference(p1));
		
		assertEquals(0, SkeletonUtil.getNumParts(skel2));
		
		skel2.addContentPlaceholder(tu2); // @@@ p1
		assertEquals(1, SkeletonUtil.getNumParts(skel2));
		
		skel2.addContentPlaceholder(tu2, ESES); // @@@ p2
		assertEquals(2, SkeletonUtil.getNumParts(skel2));
		
		skel2.addValuePlaceholder(tu2, "res_prop", null); // @@@ p3
		assertEquals(3, SkeletonUtil.getNumParts(skel2));
		
		skel2.addValuePlaceholder(tu2, "src_prop", LocaleId.EMPTY); // @@@ p4
		assertEquals(4, SkeletonUtil.getNumParts(skel2));
		
		skel2.addValuePlaceholder(tu2, "trg_prop", ESES); // @@@ p5
		assertEquals(5, SkeletonUtil.getNumParts(skel2));
		
		skel2.addReference(tu1); // @@@ p6
		assertEquals(6, SkeletonUtil.getNumParts(skel2));
		
		skel2.add("plain text"); // @@@ p7
		assertEquals(7, SkeletonUtil.getNumParts(skel2));
		
		p1 = SkeletonUtil.getPart(skel2, 0); 
		assertTrue(SkeletonUtil.isSourcePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isReference(p1));
		assertFalse(SkeletonUtil.isText(p1));
		
		GenericSkeletonPart p2 = SkeletonUtil.getPart(skel2, 1); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p2));
		assertTrue(SkeletonUtil.isTargetPlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isReference(p2));
		assertFalse(SkeletonUtil.isText(p2));
		
		GenericSkeletonPart p3 = SkeletonUtil.getPart(skel2, 2); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p3));
		assertTrue(SkeletonUtil.isValuePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isReference(p3));
		assertFalse(SkeletonUtil.isText(p3));
		
		GenericSkeletonPart p4 = SkeletonUtil.getPart(skel2, 3); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p4));
		assertTrue(SkeletonUtil.isValuePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isReference(p4));
		assertFalse(SkeletonUtil.isText(p4));
		
		GenericSkeletonPart p5 = SkeletonUtil.getPart(skel2, 4); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p5));
		assertTrue(SkeletonUtil.isValuePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isReference(p5));
		assertFalse(SkeletonUtil.isText(p5));
		
		GenericSkeletonPart p6 = SkeletonUtil.getPart(skel2, 5); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p6));
		assertTrue(SkeletonUtil.isReference(p6));
		assertFalse(SkeletonUtil.isText(p6));
		
		GenericSkeletonPart p7 = SkeletonUtil.getPart(skel2, 6); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isReference(p7));
		assertTrue(SkeletonUtil.isText(p7));
		
		skel2.changeSelfReferents(tu1); // @@@
		p1 = SkeletonUtil.getPart(skel2, 0); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p1));
		assertTrue(SkeletonUtil.isExtSourcePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p1));
		assertFalse(SkeletonUtil.isReference(p1));
		assertFalse(SkeletonUtil.isText(p1));
		
		p2 = SkeletonUtil.getPart(skel2, 1); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p2));
		assertTrue(SkeletonUtil.isExtTargetPlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p2));
		assertFalse(SkeletonUtil.isReference(p2));
		assertFalse(SkeletonUtil.isText(p2));
		
		p3 = SkeletonUtil.getPart(skel2, 2); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p3));
		assertTrue(SkeletonUtil.isExtValuePlaceholder(tu2, p3));
		assertFalse(SkeletonUtil.isReference(p3));
		assertFalse(SkeletonUtil.isText(p3));
		
		p4 = SkeletonUtil.getPart(skel2, 3); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p4));
		assertTrue(SkeletonUtil.isExtValuePlaceholder(tu2, p4));
		assertFalse(SkeletonUtil.isReference(p4));
		assertFalse(SkeletonUtil.isText(p4));
		
		p5 = SkeletonUtil.getPart(skel2, 4); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p5));
		assertTrue(SkeletonUtil.isExtValuePlaceholder(tu2, p5));
		assertFalse(SkeletonUtil.isReference(p5));
		assertFalse(SkeletonUtil.isText(p5));
		
		p6 = SkeletonUtil.getPart(skel2, 5); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p6));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p6));
		assertTrue(SkeletonUtil.isReference(p6));
		assertFalse(SkeletonUtil.isText(p6));
		
		p7 = SkeletonUtil.getPart(skel2, 6); 
		assertFalse(SkeletonUtil.isSourcePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isTargetPlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isValuePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isExtSourcePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isExtTargetPlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isExtValuePlaceholder(tu2, p7));
		assertFalse(SkeletonUtil.isReference(p7));
		assertTrue(SkeletonUtil.isText(p7));
	}
	
	@Test
	public void testGenericSkeletonWriter() {
		GenericSkeletonWriter writer = new GenericSkeletonWriter();
		
		TextUnit tu1 = new TextUnit("tu1");
		TextUnit tu2 = new TextUnit("tu2");
		
		GenericSkeleton skel1;
		GenericSkeleton skel2;
		
		tu1.setSourceContent(new TextFragment("source"));
		tu1.setTargetContent(ESES, new TextFragment("target"));
		
		tu2.setSourceContent(new TextFragment("source2"));
		tu2.setTargetContent(ESES, new TextFragment("target2"));
		
		tu1.setProperty(new Property("res_prop", "res_prop_value"));
		tu1.setSourceProperty(new Property("src_prop", "src_prop_value"));
		tu1.setTargetProperty(ESES, new Property("trg_prop", "trg_prop_value"));
		
		tu2.setProperty(new Property("res_prop", "res_prop_value2"));
		tu2.setSourceProperty(new Property("src_prop", "src_prop_value2"));
		tu2.setTargetProperty(ESES, new Property("trg_prop", "trg_prop_value2"));
		
		// Monolingual document
		StartDocument sd = new StartDocument("sd");
		sd.setMultilingual(false);
		sd.setLocale(ENUS);
		sd.setLineBreak("\n");
		
		// Test a skeleton-less TU
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("source", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("source", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("target", writer.processTextUnit(tu1));
		
		// Src
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1)); // !!! Diff
		
		// Trg 1
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, ENUS);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		// Trg 2
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, FRFR);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		// Trg 3
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, ESES);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1));
		
		// Trg 4
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, FRFR);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		// Ext src 1
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1);
		skel1.changeSelfReferents(tu2);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		// Ext src - tu1
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1)); //!!! Diff
		
		// Ext src - tu2
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_target2_text after2", writer.processTextUnit(tu2)); //!!! Diff
		
		// Ext src 2
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		// Ext src 2 - tu1
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1)); //!!! Diff
		
		// Ext src 2 - tu2
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_target2_text after2", writer.processTextUnit(tu2)); //!!! Diff
		
		// Ext trg 1
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2, ENUS);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		// Ext trg 2
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2, FRFR);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		// Ext trg 3
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2, ESES);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1));
		
		// Ext ref
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addReference(tu2);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		tu2.setIsReferent(true);		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("", writer.processTextUnit(tu2));		
		assertEquals("text before_text before2_source2_text after2_text after", writer.processTextUnit(tu1));				
		
		tu2.setIsReferent(true);		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("", writer.processTextUnit(tu2));		
		assertEquals("text before_text before2_target2_text after2_text after", writer.processTextUnit(tu1)); //!!! Diff
		
		// Res prop value 
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "res_prop", LocaleId.EMPTY);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value_text after", writer.processTextUnit(tu1));
		
		// Src prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "src_prop", null);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value_text after", writer.processTextUnit(tu1));
		
		// Trg prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "trg_prop", ESES);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value_text after", writer.processTextUnit(tu1));
		
		// Ext res prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "res_prop", LocaleId.EMPTY);
		skel1.changeSelfReferents(tu2);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value2_text after", writer.processTextUnit(tu1));
		
		// Ext src prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "src_prop", null);
		skel1.changeSelfReferents(tu2);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value2_text after", writer.processTextUnit(tu1));
		
		// Ext trg prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "trg_prop", ESES);		
		skel1.add("_text after");
		skel1.changeSelfReferents(tu2);
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value2_text after", writer.processTextUnit(tu1));
	
		// Text
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.add("text");
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_text_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_text_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_text_text after", writer.processTextUnit(tu1));		
	}
	
	@Test
	public void testGenericSkeletonWriter_Multilingual() {
		GenericSkeletonWriter writer = new GenericSkeletonWriter();
		
		TextUnit tu1 = new TextUnit("tu1");
		TextUnit tu2 = new TextUnit("tu2");
		
		GenericSkeleton skel1;
		GenericSkeleton skel2;
		
		tu1.setSourceContent(new TextFragment("source"));
		tu1.setTargetContent(ESES, new TextFragment("target"));
		
		tu2.setSourceContent(new TextFragment("source2"));
		tu2.setTargetContent(ESES, new TextFragment("target2"));
		
		tu1.setProperty(new Property("res_prop", "res_prop_value"));
		tu1.setSourceProperty(new Property("src_prop", "src_prop_value"));
		tu1.setTargetProperty(ESES, new Property("trg_prop", "trg_prop_value"));
		
		tu2.setProperty(new Property("res_prop", "res_prop_value2"));
		tu2.setSourceProperty(new Property("src_prop", "src_prop_value2"));
		tu2.setTargetProperty(ESES, new Property("trg_prop", "trg_prop_value2"));
		
		// Multi-lingual document
		StartDocument sd = new StartDocument("sd");
		sd.setMultilingual(true);
		sd.setLocale(ENUS);
		sd.setLineBreak("\n");
		
		// Test a skeleton-less TU
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("source", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("source", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("target", writer.processTextUnit(tu1));
		
		// Src
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1)); // !!! Diff
		
		// Trg 1
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, ENUS);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		// Trg 2
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, FRFR);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		// Trg 3
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, ESES);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target_text after", writer.processTextUnit(tu1));
		
		// Trg 4
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1, FRFR);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source_text after", writer.processTextUnit(tu1));
		
		// Ext src 1
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu1);
		skel1.changeSelfReferents(tu2);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		// Ext src - tu1
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1)); //!!! Diff
		
		// Ext src - tu2
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2)); //!!! Diff
		
		// Ext src 2
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		// Ext src 2 - tu1
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1)); //!!! Diff
		
		// Ext src 2 - tu2
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before2_source2_text after2", writer.processTextUnit(tu2)); //!!! Diff
		
		// Ext trg 1
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2, ENUS);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		// Ext trg 2
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2, FRFR);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_source2_text after", writer.processTextUnit(tu1));
		
		// Ext trg 3
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addContentPlaceholder(tu2, ESES);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_target2_text after", writer.processTextUnit(tu1));
		
		// Ext ref
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addReference(tu2);
		skel1.add("_text after");
		
		skel2.add("text before2_");
		skel2.addContentPlaceholder(tu2);
		skel2.add("_text after2");
		
		tu2.setIsReferent(true);		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("", writer.processTextUnit(tu2));		
		assertEquals("text before_text before2_source2_text after2_text after", writer.processTextUnit(tu1));				
		
		tu2.setIsReferent(true);		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("", writer.processTextUnit(tu2));		
		assertEquals("text before_text before2_source2_text after2_text after", writer.processTextUnit(tu1)); //!!! Diff
		
		// Res prop value 
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "res_prop", LocaleId.EMPTY);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value_text after", writer.processTextUnit(tu1));
		
		// Src prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "src_prop", null);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value_text after", writer.processTextUnit(tu1));
		
		// Trg prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "trg_prop", ESES);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value_text after", writer.processTextUnit(tu1));
		
		// Ext res prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "res_prop", LocaleId.EMPTY);
		skel1.changeSelfReferents(tu2);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_res_prop_value2_text after", writer.processTextUnit(tu1));
		
		// Ext src prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "src_prop", null);
		skel1.changeSelfReferents(tu2);
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_src_prop_value2_text after", writer.processTextUnit(tu1));
		
		// Ext trg prop value
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.addValuePlaceholder(tu1, "trg_prop", ESES);		
		skel1.add("_text after");
		skel1.changeSelfReferents(tu2);
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value2_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_trg_prop_value2_text after", writer.processTextUnit(tu1));
	
		// Text
		skel1 = new GenericSkeleton();
		skel2 = new GenericSkeleton();
		
		tu1.setSkeleton(skel1);
		tu2.setSkeleton(skel2);
		
		skel1.add("text before_");
		skel1.add("text");
		skel1.add("_text after");
		
		writer.processStartDocument(ENUS, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_text_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(FRFR, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_text_text after", writer.processTextUnit(tu1));
		
		writer.processStartDocument(ESES, "UTF-8", null, new EncoderManager(), sd);
		assertEquals("text before_text_text after", writer.processTextUnit(tu1));		
	}
}
