package net.sf.okapi.common.filters;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment.TagType;

public class InlineCodeFinder {

	private ArrayList<String>     rules;
	private String                sample;
	private Pattern               pattern;

	
	public InlineCodeFinder () {
		rules = new ArrayList<String>();
	}

	@Override
	public InlineCodeFinder clone () {
		InlineCodeFinder tmp = new InlineCodeFinder();
		tmp.setSample(sample);
		tmp.getRules().addAll(getRules());
		return tmp;
	}

	public ArrayList<String> getRules () {
		return rules;
	}
	
	public String getSample () {
		return sample;
	}
	
	public void setSample (String value) {
		sample = value;
	}
	
	public void compile () {
		StringBuilder tmp = new StringBuilder();
		for ( String rule : rules ) {
			if ( tmp.length() > 0 ) tmp.append("|");
			tmp.append("("+rule+")");
		}
		pattern = Pattern.compile(tmp.toString(), Pattern.MULTILINE);
	}

	public void process (TextContainer fragment) {
		String tmp = fragment.getCodedText();
		Matcher m = pattern.matcher(tmp);
		int start = 0;
		int diff = 0;
		while ( m.find(start) ) {
			diff += fragment.changeToCode(m.start()+diff, m.end()+diff, TagType.PLACEHOLDER, null);
			start = m.end();
			if ( start >= tmp.length() ) break;
		}
	}

}
