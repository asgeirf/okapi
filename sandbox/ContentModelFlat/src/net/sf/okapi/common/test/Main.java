package net.sf.okapi.common.test;

import java.util.List;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.Document;
import net.sf.okapi.common.resource.Group;
import net.sf.okapi.common.resource.IContainable;
import net.sf.okapi.common.resource.IResourceContainer;
import net.sf.okapi.common.resource.SkeletonUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextFragment.TagType;

public class Main {

	public static void main(String[] args) {

		Document doc = new Document();
		doc.add(new SkeletonUnit("s1", "<doc>"));

		TextUnit tu = new TextUnit("t1", "text unit 1");
		tu.setSkeletonBefore(new SkeletonUnit("s2", "<p>"));
		tu.setSkeletonAfter(new SkeletonUnit("s3", "</p>"));
		doc.add(tu);
		
		tu = new TextUnit("t2", "a");
		tu.setSkeletonBefore(new SkeletonUnit("s4", "<p>"));
		tu.setSkeletonAfter(new SkeletonUnit("s5", "</p>"));
		TextContainer src = tu.getSource().getContent();
		src.append('b');
		src.append("c []");
		TextFragment tf = new TextFragment("INSERT");
		src.insert(5, tf);
		src.append(TagType.OPENING, "b", "<b1>");
		src.append("bold");
		src.append(TagType.OPENING, "b", "<b2>");
		src.append("rebold.");
		src.append(TagType.CLOSING, "b", "</b2>");
		src.append(TagType.CLOSING, "b", "</b1>");
		src.append(TagType.PLACEHOLDER, "br", "<br/>");
		src.append("After break.");
		doc.add(tu);

		TextUnit tu2 = new TextUnit("t3", "Image1: ");
		tu2.setSkeletonBefore(new SkeletonUnit("s6", "<p>"));
		tu2.setSkeletonAfter(new SkeletonUnit("s7", "</p>"));
		src = tu2.getSourceContent();
		src.append(TagType.PLACEHOLDER, "image",
			String.format("<img alt='%sSF1%s' title='%sSF2%s'/>",
			TextFragment.SFMARKER_START, TextFragment.SFMARKER_END,
			TextFragment.SFMARKER_START, TextFragment.SFMARKER_END)
		).setHasSubflow(true);
		Group grp = new Group();
		grp.setID("g1");
		grp.add(new TextUnit("SF1", "Text of SF1 in image1"));
		grp.add(new TextUnit("SF2", "Text of SF2 in image1"));
		tu2.addChild(grp);
		src.append(", img2: ");
		src.append(TagType.PLACEHOLDER, "image",
				String.format("<img2 alt='%sSF3%s' title='%sSF4%s'/>",
				TextFragment.SFMARKER_START, TextFragment.SFMARKER_END,
				TextFragment.SFMARKER_START, TextFragment.SFMARKER_END)
			).setHasSubflow(true);
			grp = new Group();
			grp.setID("g2");
			grp.add(new TextUnit("SF3", "Text of SF3 in image2"));
			grp.add(new TextUnit("SF4", "Text of SF4 in image2"));
			tu2.addChild(grp);
		doc.add(tu2);
		 
		tu = new TextUnit("t3", "Image2: ");
		tu.setSkeletonBefore(new SkeletonUnit("s8", "<p>"));
		tu.setSkeletonAfter(new SkeletonUnit("s9", "</p>"));
		src = tu.getSourceContent();
		src.append(TagType.PLACEHOLDER, "image",
			String.format("%sSF1%s",
			TextFragment.SFMARKER_START, TextFragment.SFMARKER_END)
		).setHasSubflow(true);
		TextUnit tu3 = new TextUnit("SF1", "Text of SF1 in image2");
		tu3.setSkeletonBefore(new SkeletonUnit("s1", "<img alt='"));
		tu3.setSkeletonAfter(new SkeletonUnit("s2", "'/>"));
		tu.addChild(tu3);
		doc.add(tu);
		
		doc.add(new SkeletonUnit("sLast", "</doc>"));
		show(doc, 0);
		
		System.out.println("\nTest for iteration:\n");
		
		System.out.println("\nTest type 1:");
		processTU(tu2);
		if ( tu2.hasChild() ) {
			for ( TextUnit item : tu2.childTextUnitIterator() ) {
				processTU(item);
			}
		}
		
		System.out.println("\nTest for content:\n");
		
		tu = new TextUnit("t1", "ABCDEF ");
		src = tu.getSourceContent();
		src.append(TagType.OPENING, "b", "<b1>");
		src.append("bold");
		src.append(TagType.OPENING, "b", "<b2>");
		src.append("rebold.");
		src.append(TagType.CLOSING, "b", "</b2>");
		src.append(TagType.PLACEHOLDER, "br", "<br/>");
		src.append(" more");
		System.out.println("---Initial:");
		System.out.println(tu.toString());
		
		src.append(TagType.CLOSING, "b", "</b1>");
		System.out.println("-After adding </b1>:");
		System.out.println(src.toString());
		
		TextFragment src2 = src.subSequence(9, 15);
		System.out.println("---SubSequence(9, 15):");
		System.out.println("src2: "+src2.toString());
		
		src.getCode(1).setData("<B2>");
		System.out.println("---Modify <b2> to <B2> in original:");
		System.out.println("src1: "+src.toString());
		System.out.println("src2: "+src2.toString());
		
		TextFragment src3 = src.subSequence(15, 26);
		System.out.println("---SubSequence(15, 26):");
		System.out.println("src3: "+src3.toString());

		src2.append(src3);
		System.out.println("-After appending src3 to src2:");
		System.out.println("src2: "+src2.toString());
		
		TextFragment src4 = src.subSequence(9, 26);
		System.out.println("---SubSequence(9, 26):");
		System.out.println("src4: "+src4.toString());

		System.out.println("src4 compared to src2 as string = "+src4.toString().compareTo(src2.toString()));
		System.out.println("src4 compared to src2 as TextContainer = "+src4.compareTo(src2));
		System.out.println("src4 equals to src2 = "+src4.equals(src2));
		
		src4 = src.subSequence(0, -1);
		System.out.println("src1 before: "+src.toString());
		src.remove(9, 26);
		System.out.println("---Remove(9, 26):");
		System.out.println("src1 after : "+src.toString());

		System.out.println("src4 before: "+src4.toString());
		int diff = src4.changeToCode(9, 26, TagType.PLACEHOLDER, null);
		System.out.println("---ChangeToCode(9, 26):");
		System.out.println("src4 after : "+src4.toString());
		System.out.println("difference : "+diff);
		diff = src4.changeToCode(0, 2, TagType.OPENING, "special");
		System.out.println("---ChangeToCode(0, 2):");
		System.out.println("src4 after : "+src4.toString());
		System.out.println("difference : "+diff);
		diff = src4.changeToCode(3+diff, 4+diff, TagType.CLOSING, "special");
		System.out.println("---ChangeToCode(3+diff, 4+diff):");
		System.out.println("src4 after : "+src4.toString());
		System.out.println("difference : "+diff);

		System.out.println("---remove(8, 10)");
		src4.remove(8, 10);
		System.out.println("src4 after : "+src4.toString());
		
		System.out.println("---renumberCodes");
		src4.renumberCodes();
		System.out.println("src4 after : "+src4.toString());
		
		System.out.println("---setCodedText(ct, c)");
		System.out.println("src2 before: "+src2.toString());
		List<Code> codes = src2.getCodes();
		StringBuilder tmp = new StringBuilder(src2.getCodedText());
		src2.clear();
		src2.setCodedText(tmp.toString(), codes);
		System.out.println("src2 after : "+src2.toString());
		
		System.out.println("---setCodedText(ct) after modification");
		for ( int i=0; i<tmp.length(); i++ ) {
			switch ( tmp.charAt(i) ) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
				i++;
				break;
			default:
				if ( Character.isLowerCase(tmp.charAt(i)) )
					tmp.setCharAt(i, Character.toUpperCase(tmp.charAt(i)));
				else if ( Character.isUpperCase(tmp.charAt(i)) )
					tmp.setCharAt(i, Character.toLowerCase(tmp.charAt(i)));
				break;
			}
		}
		src2.setCodedText(tmp.toString());
		System.out.println("src2 after : "+src2.toString());
		
		System.out.println("\nTest target:");
		
		tu = new TextUnit("tu1", "Source text");
		tu.setTarget(tu.getSource());
		System.out.println("src : "+tu.getSource().toString());
		System.out.println("trg : "+tu.getTarget().toString());
		
		//=== Test Segmenter
		
		System.out.println("\n=== Segmenter Tests");
		TextContainer tc = new TextContainer();
		tc.append("Seg1. Seg2. ");
		tc.append(TagType.OPENING, "b", "<b>");
		tc.append("Bold seg3.");
		tc.append(TagType.CLOSING, "b", "</b>");
		System.out.println(tc.toString());

		
		
		
	}

	private static void processTU (TextUnit tu) {
		System.out.println("tu.toString(): '"+ tu.toString() + "'");
		if ( tu.hasTarget() ) {
			System.out.println("tu.getTarget(): '"+ tu.getTarget().toString() + "'");
			System.out.println("tu.getTargetContent(): '"+ tu.getTargetContent().toString() + "'");
		}
	}
	
	private static void show (IResourceContainer container,
		int level)
	{
		if ( container instanceof Document ) {
			for ( int i=0; i<level; i++ ) System.out.print('-'); 
			System.out.println("doc: ");
		}
		else if ( container instanceof Group ) {
			for ( int i=0; i<level; i++ ) System.out.print('-'); 
			System.out.println("grp: ");
		}

		for ( IContainable unit : container ) {
			if ( unit instanceof Group ) {
				show((IResourceContainer)unit, level+1);
			}
			else if ( unit instanceof TextUnit ) {
				for ( int i=0; i<level; i++ ) System.out.print('-'); 
				System.out.println("txt: "+unit.toString());
			}
			else if ( unit instanceof SkeletonUnit ) {
				for ( int i=0; i<level; i++ ) System.out.print('-'); 
				System.out.println("skl: "+unit.toString());
			}
		}
	}
}
