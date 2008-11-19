package net.sf.okapi.apptest.dummyfilter;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import net.sf.okapi.apptest.common.IParameters;
import net.sf.okapi.apptest.common.IResource;
import net.sf.okapi.apptest.filters.FilterEvent;
import net.sf.okapi.apptest.filters.IEncoder;
import net.sf.okapi.apptest.filters.IFilter;
import net.sf.okapi.apptest.filters.FilterEvent.FilterEventType;
import net.sf.okapi.apptest.resource.Code;
import net.sf.okapi.apptest.resource.Property;
import net.sf.okapi.apptest.resource.StartDocument;
import net.sf.okapi.apptest.resource.DocumentPart;
import net.sf.okapi.apptest.resource.Ending;
import net.sf.okapi.apptest.resource.StartGroup;
import net.sf.okapi.apptest.resource.TextFragment;
import net.sf.okapi.apptest.resource.TextUnit;
import net.sf.okapi.apptest.resource.TextFragment.TagType;
import net.sf.okapi.apptest.skeleton.GenericSkeleton;
import net.sf.okapi.apptest.skeleton.GenericSkeletonProvider;

public class DummyFilter implements IFilter {

	ArrayList<FilterEvent> list;
	private int current;
	private boolean canceled;
	private String language;
	private IEncoder encoder;
	private GenericSkeletonProvider skelProv;
	
	public DummyFilter () {
		encoder = new DummyEncoder();
	}
	
	public String getName () {
		return "DummyFilter";
	}
	
	public void setOptions (String language,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		System.out.println("DummyParser: setOptions() called");
		this.language = language;
		System.out.println(" -output language = " + language);
		System.out.println(" -output default encoding = " + defaultEncoding);
	}
	
	public void open (InputStream input) {
		System.out.println("DummyParser: open(stream) called");
		resetResources();
	}
	
	public void open (CharSequence inputText) {
		System.out.println("DummyParser: open(charseq) called");
		resetResources();
	}

	public void open (URL inputURL) {
		System.out.println("DummyParser: open(URL) called");
		resetResources();
	}

	public void close () {
		System.out.println("DummyParser: close() called");
		list.clear();
		list = null;
	}
	
	public boolean hasNext () {
		if ( list == null ) return false;
		return (current<list.size()-1);
	}
	
	public FilterEvent next () {
		if ( canceled ) {
			if ( current < list.size()-1 ) {
				current = list.size()-2;
			}
		}
		current++;
		return list.get(current);
	}
	
	public IResource getResource () {
		return list.get(current).getResource();
	}

	private void makeCase001 () {
		// <p>Before <img href='img.png' alt='text'/> after.</p>

		DocumentPart dp = new DocumentPart("dp1", true);
		dp.setProperty(new Property("href", "img.png", true));
		list.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp));
		
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT,
			new TextUnit("t1", "text", true)));

		TextUnit tu = new TextUnit();
		tu.setId("t2");
		tu.setIsReferent(true);
		TextFragment tf = new TextFragment();
		tf.append("Before ");
		Code code = tf.append(TagType.PLACEHOLDER, "image",
			"<img href='" + TextFragment.makeRefMarker("dp1","href") +
			"' alt='" + TextFragment.makeRefMarker("t1") + "'/>");
		code.setHasReference(true);
		tf.append(" after.");
		tu.setContent(tf);
		GenericSkeleton skel = skelProv.createSkeleton();
		skel.add("<p>");
		skel.addRef("t2");
		skel.add("</p>");
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu, skel));

	}
	
	private void makeCase002 () {
		// <p>Before <a href='link.htm'/> after.</p>
		StartGroup grp1 = new StartGroup(null, "g1");
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp1,
			skelProv.createSkeleton("<p>")));

		DocumentPart dp = new DocumentPart("dp1", true);
		dp.setProperty(new Property("href", "link.htm", true));
		list.add(new FilterEvent(FilterEventType.DOCUMENT_PART, dp));

		TextUnit tu = new TextUnit();
		tu.setId("t1");
		TextFragment tf = new TextFragment();
		tf.append("Before ");
		Code code = tf.append(TagType.PLACEHOLDER, "link",
			"<a href='" + TextFragment.makeRefMarker("dp1","href") + "'/>");
		code.setHasReference(true);
		tf.append(" after.");
		tu.setContent(tf);
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu));

		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g1"),
			skelProv.createSkeleton("</p>\n")));
	}
	
	private void makeCase003 () {
		// <table id=100> <tr><td>text</td></tr><table>
		StartGroup grp1 = new StartGroup(null, "g1");
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp1,
			skelProv.createSkeleton("<table id=100>\n ")));

		StartGroup grp2 = new StartGroup("g1", "g2");
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp2,
			skelProv.createSkeleton("<tr>")));
		
		StartGroup grp3 = new StartGroup("g2", "g3");
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp3,
			skelProv.createSkeleton("<td>")));
		
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT,
			new TextUnit("t1", "text")));
		
		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g3"),
			skelProv.createSkeleton("</td>")));

		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g2"),
			skelProv.createSkeleton("</tr>\n")));
		
		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g1"),
			skelProv.createSkeleton("</table>\n")));
	}

	private void makeCase004 () {
		StartGroup grp1 = new StartGroup(null, "g1", true);
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp1,
				skelProv.createSkeleton("<ul>", true)));
		
		StartGroup grp2 = new StartGroup(null, "g2", false);
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp2,
				skelProv.createSkeleton("\n  <li>")));
		
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT,
			new TextUnit("t1", "Text of item 1 with special char < and &.")));
		
		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g2"),
			skelProv.createSkeleton("</li>")));
		
		StartGroup grp3 = new StartGroup(null, "g3", false);
		list.add(new FilterEvent(FilterEventType.START_GROUP, grp3,
				skelProv.createSkeleton("\n  <li>")));
		
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT,
			new TextUnit("t2", "Text of item 2")));
		
		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g3"),
			skelProv.createSkeleton("</li>")));
		
		list.add(new FilterEvent(FilterEventType.END_GROUP,
			new Ending("g1"),
			skelProv.createSkeleton("\n </ul>")));

		TextUnit tu = new TextUnit();
		tu.setId("t3");
		TextFragment tf = new TextFragment();
		tf.append("Text before list: \n ");
		Code code = tf.append(TagType.PLACEHOLDER, "list", TextFragment.makeRefMarker("g1"));
		code.setHasReference(true);
		tf.append("\n and text after the list.");
		tu.setContent(tf);
		GenericSkeleton skel = skelProv.createSkeleton("<p>");
		skel.addRef("t3");
		skel.add("</p>");
		list.add(new FilterEvent(FilterEventType.TEXT_UNIT, tu, skel));

	}
	
	private void resetResources () {
		canceled = false;
		current = -1;
		
		skelProv = new GenericSkeletonProvider();
		list = new ArrayList<FilterEvent>();
		
		StartDocument docRes = new StartDocument();
		docRes.setId("d1");
		docRes.setEncoding("UTF-8"); // Always
		docRes.setLanguage(language);
		list.add(new FilterEvent(FilterEventType.START_DOCUMENT, docRes));

		makeCase001();
		//makeCase002();
		//makeCase003();
		//makeCase004();
	
		list.add(new FilterEvent(FilterEventType.END_DOCUMENT,
			new Ending("d1")));
	}

	public void cancel () {
		canceled = true;
	}

	public IParameters getParameters () {
		return null;
	}

	public void setParameters (IParameters params) {
		// No parameters used for this filter.
	}

	public IEncoder getEncoder() {
		return encoder;
	}

}
