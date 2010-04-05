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

package net.sf.okapi.steps.xliffkit.common.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextUnitUtil;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import net.sf.okapi.steps.xliffkit.common.persistence.beans.EventBean;
import net.sf.okapi.steps.xliffkit.common.persistence.beans.InputStreamBean;
import net.sf.okapi.steps.xliffkit.common.persistence.beans.TextUnitBean;
import net.sf.okapi.steps.xliffkit.common.persistence.beans.ZipSkeletonBean;

import org.apache.commons.io.input.CountingInputStream;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Before;
import org.junit.Test;

public class TestJackson {

//	private static final String fileName = "test3.txt";
	private ObjectMapper mapper;
	
	@Before
	public void setUp() {
		mapper = new ObjectMapper();
		
		mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true); 
		mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		//mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
		mapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
	}
			
	// DEBUG @Test
	public void testTextUnit() throws JsonGenerationException, JsonMappingException, IOException, URISyntaxException {
		Event event = new Event(EventType.TEXT_UNIT);
		//TextUnit tu = TextUnitUtil.buildTU("source", "skeleton");
		TextUnit tu = TextUnitUtil.buildTU("source-text" + (char) 2 + '"' + " : " + '"' + 
				'{' + '"' + "ssssss " + ':' + '"' + "ddddd" + "}:" + '<' + '>' + "sssddd: <>dsdd");
		tu.setSkeleton(new ZipSkeleton(new ZipEntry("")));
		event.setResource(tu);
		tu.setTarget(LocaleId.FRENCH, new TextContainer("french-text"));
		tu.setTarget(LocaleId.TAIWAN_CHINESE, new TextContainer("chinese-text"));

		//FileOutputStream output = new FileOutputStream(new File(this.getClass().getResource(fileName).toURI()));
//		mapper.writeValue(output, event);
//		output.close();
		
//		// Use JAXB annotations
//		AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
//		mapper.getSerializationConfig().setAnnotationIntrospector(introspector);
//		
//		GenericSkeleton skel = (GenericSkeleton) tu.getSkeleton();
//		System.out.println(mapper.writeValueAsString(skel));
		TextUnitBean tub = new TextUnitBean();
		tub.set(tu);
		
		EventBean evb = new EventBean();
		evb.set(event);
		//mapper.getDeserializationConfig().addHandler(new TestResolver());
		
		//String st = mapper.writeValueAsString(tub);
		String st = mapper.writeValueAsString(evb);
		System.out.println(st);
		
//		String st2 = mapper.writeValueAsString(evb);
//		System.out.println(st2);
		
		st = mapper.writeValueAsString(tub);
		tub = mapper.readValue(st, TextUnitBean.class);
		tu = tub.get(new TextUnit(""));
//		System.out.println(tu.getSource().getCodedText());
		System.out.println(((TextContainer)tub.getSource().get(new TextContainer())).getCodedText());
//		ISkeleton skel = tub.getSkeleton().read(ISkeleton.class);
//		if (skel != null)
//			System.out.println(skel.getClass());
		//System.out.println(tub.getSkeleton().getContent().getClass().getName());
		
//		ZipSkeletonBean zsb = new ZipSkeletonBean(); 
//		st = mapper.writeValueAsString(zsb);
//		zsb = mapper.readValue(st, ZipSkeletonBean.class);
		
		//ZipSkeletonBean zsb = mapper.readValue(st, ZipSkeletonBean.class);
	}
	
	// DEBUG @Test
	public void testTextUnitWrite() throws IOException {
	
		Event event1 = new Event(EventType.TEXT_UNIT);
		TextUnit tu1 = TextUnitUtil.buildTU("source-text1" + (char) 2 + '"' + " : " + '"' + 
				'{' + '"' + "ssssss " + ':' + '"' + "ddddd" + "}:" + '<' + '>' + "sssddd: <>dsdd");
		String zipName = this.getClass().getResource("sample1.en.fr.zip").getFile();
		tu1.setSkeleton(new ZipSkeleton(new ZipFile(new File(zipName))));
		event1.setResource(tu1);
		tu1.setTarget(LocaleId.FRENCH, new TextContainer("french-text1"));
		tu1.setTarget(LocaleId.TAIWAN_CHINESE, new TextContainer("chinese-text1"));
				
		Event event2 = new Event(EventType.TEXT_UNIT);
		TextUnit tu2 = TextUnitUtil.buildTU("source-text2" + (char) 2 + '"' + " : " + '"' + 
				'{' + '"' + "ssssss " + ':' + '"' + "ddddd" + "}:" + '<' + '>' + "sssddd: <>dsdd");
		//tu2.setSkeleton(new ZipSkeleton(new ZipEntry("aa1/content/content.gmx")));
		event2.setResource(tu2);
		tu2.setTarget(LocaleId.FRENCH, new TextContainer("french-text2"));
		tu2.setTarget(LocaleId.TAIWAN_CHINESE, new TextContainer("chinese-text2"));
		
		tu1.getSource().appendPart("part1");
		tu1.getSource().appendSegment(new Segment("segId1", new TextFragment("seg1")));
		tu1.getSource().appendPart("part2");
		tu1.getSource().appendSegment(new Segment("segId2", new TextFragment("seg2")));
				
		//JSONPersistenceSession skelSession = new JSONPersistenceSession(Event.class);		
		JSONPersistenceSession skelSession = new JSONPersistenceSession(Events.class);
		
		File tempSkeleton = null;
		tempSkeleton = File.createTempFile("~aaa", ".txt");
		tempSkeleton.deleteOnExit();
		
		skelSession.start(new FileOutputStream(tempSkeleton));
		
		Events events = new Events();
		events.add(event1);
		events.add(event2);
		
//		skelSession.serialize(event1);
//		skelSession.serialize(event2);
		skelSession.serialize(events);
		skelSession.end();
		
		FileInputStream fis = new FileInputStream(tempSkeleton);
		skelSession.start(fis);
		
		assertTrue(events instanceof Events);
		assertTrue(events instanceof Object);
		assertTrue(events instanceof ArrayList<?>);
		
		ArrayList.class.cast(new ArrayList<Event>()); 
		
		Events events2 = skelSession.deserialize(Events.class);
		
//		System.out.println(fis.available());
//		Event event11 = (Event) skelSession.deserialize();
//		System.out.println(fis.available());
//		Event event22 = (Event) skelSession.deserialize();
		skelSession.end();
		
		Event event11 = events2.get(0);
		Event event22 = events2.get(1);
		
		assertEquals(event1.getEventType(), event11.getEventType());
		assertEquals(event2.getEventType(), event22.getEventType());
	}
	
	// DEBUG @Test
	public void testRawDocument() throws JsonGenerationException, JsonMappingException, IOException {
		Event event = new Event(EventType.RAW_DOCUMENT);
		event.setResource(new RawDocument("raw doc", LocaleId.ENGLISH));
		EventBean evb = new EventBean();
		evb.set(event);
		//mapper.getDeserializationConfig().addHandler(new TestResolver());
		
		//String st = mapper.writeValueAsString(tub);
		String st = mapper.writeValueAsString(evb);
		System.out.println(st);
	}
	
	// DEBUG 
	@Test
	public void testMultipleRead1() throws IOException {
		JSONPersistenceSession skelSession = new JSONPersistenceSession(String.class);
		
		File tempSkeleton = null;
		tempSkeleton = File.createTempFile("~aaa", ".txt");
		tempSkeleton.deleteOnExit();
		
		skelSession.start(new FileOutputStream(tempSkeleton));
		String st1 = "string1";
		String st2 = "string2";
		skelSession.serialize(st1);
		skelSession.serialize(st2);
		skelSession.end();
		
		FileInputStream fis = new FileInputStream(tempSkeleton);
		skelSession.start(fis);
		
		System.out.println(fis.available());
		String st3 = skelSession.deserialize(String.class);
		System.out.println(fis.available());
		String st4 = skelSession.deserialize(String.class);
		skelSession.end();
	}
	
	// DEBUG 
	@Test
	public void testMultipleRead2() throws IOException {
		JSONPersistenceSession skelSession = new JSONPersistenceSession(Object.class);
		
		File tempSkeleton = null;
		tempSkeleton = File.createTempFile("~aaa", ".txt");
		tempSkeleton.deleteOnExit();
		
		skelSession.start(new FileOutputStream(tempSkeleton));
		Object st1 = new Object();
		Object st2 = new Object();
		
		List<Object> list = new ArrayList<Object> ();
		list.add(st1);
		list.add(st2);
		
//		skelSession.serialize(st1);
//		skelSession.serialize(st2);
		skelSession.serialize(list);
		skelSession.end();
		
		CountingInputStream fis = new CountingInputStream(new FileInputStream(tempSkeleton));
		skelSession.start(fis);
		
		System.out.println(fis.available());
		Object st3 = (Object) skelSession.deserialize(Object.class);
		System.out.println(fis.available());
		Object st4 = (Object) skelSession.deserialize(Object.class);
		skelSession.end();
	}
	
	// DEBUG @Test
	public void testInputStream() {
		InputStream is = this.getClass().getResourceAsStream("test3.txt");
		System.out.println(is.markSupported());
//		is.mark(Integer.MAX_VALUE);
		
		System.out.println();
		//InputStream is2 = is.clone();
		
		String st = "";
		try {
			st = mapper.writeValueAsString(is);
		} catch (JsonGenerationException e) {
			// TODO Handle exception
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Handle exception
			e.printStackTrace();
		}
		System.out.println(st);
	}
	
	
	@Test
	public void testZipSkeleton() throws URISyntaxException, IOException {
		ZipFile zf = null;
			//String name = this.getClass().getResource("sample1.en.fr.zip").toString();
//			URI uri = this.getClass().getResource("test3.txt").toURI();
//			String name = this.getClass().getResource("test3.txt").toString();
			
			//name = Util.getFilename(name, true);
				zf = new ZipFile(new File(this.getClass().getResource("sample1.en.fr.zip").toURI()));
		ZipSkeleton zs = new ZipSkeleton(zf);
		ZipSkeletonBean zsb = new ZipSkeletonBean();
		zsb.set(zs);
		String st = mapper.writeValueAsString(zsb);
		System.out.println(st);
		zf.close();
	}

	@Test
	public void testInputStreamBean() throws URISyntaxException, JsonGenerationException, JsonMappingException, IOException {
		FileInputStream fis = new FileInputStream(new File(this.getClass().getResource("test3.txt").toURI()));
		InputStreamBean isb = new InputStreamBean();
		isb.set(fis);
		String st = mapper.writeValueAsString(isb);
		System.out.println(st);
	}
}
