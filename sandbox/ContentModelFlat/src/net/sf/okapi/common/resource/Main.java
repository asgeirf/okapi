package net.sf.okapi.common.resource;

import net.sf.okapi.common.resource.TextContainer.TagType;

public class Main {

	public static void main(String[] args) {

		Document doc = new Document();
		doc.add(new SkeletonUnit("s1", "<doc>"));

		TextUnit tu = new TextUnit("t1", "text unit 1");
		tu.setSkeletonBefore(new SkeletonUnit("s2", "<p>"));
		tu.setSkeletonAfter(new SkeletonUnit("s3", "</p>"));
		doc.add(tu);
		
		tu = new TextUnit("t2", "a");
		TextRootContainer src = tu.getSource();
		src.append('b');
		src.append("c ");
		src.append(TagType.OPENING, "b", "<b1>");
		src.append("bold");
		src.append(TagType.OPENING, "b", "<b2>");
		src.append("rebold.");
		src.append(TagType.CLOSING, "b", "</b2>");
		src.append(TagType.CLOSING, "b", "</b1>");
		src.append(TagType.PLACEHOLDER, "br", "<br/>");
		src.append("After break.");
		doc.add(tu);

		tu = new TextUnit("t3", "Image1: ");
		src = tu.getSource();
		src.append(TagType.PLACEHOLDER, "image",
			String.format("<img alt='%sSF1%s' title='%sSF2%s'/>",
			TextContainer.SFMARKER_START, TextContainer.SFMARKER_END,
			TextContainer.SFMARKER_START, TextContainer.SFMARKER_END)
		).setHasSubflow(true);
		Group grp = new Group();
		grp.setID("g1");
		grp.add(new TextUnit("SF1", "Text of SF1 in image1"));
		grp.add(new TextUnit("SF2", "Text of SF2 in image1"));
		tu.addChild(grp);
		doc.add(tu);
		
		tu = new TextUnit("t3", "Image2: ");
		src = tu.getSource();
		src.append(TagType.PLACEHOLDER, "image",
			String.format("%sSF1%s",
			TextContainer.SFMARKER_START, TextContainer.SFMARKER_END)
		).setHasSubflow(true);
		TextUnit tu2 = new TextUnit("SF1", "Text of SF1 in image2");
		tu2.setSkeletonBefore(new SkeletonUnit("s1", "<img alt='"));
		tu2.setSkeletonAfter(new SkeletonUnit("s2", "'/>"));
		tu.addChild(tu2);
		doc.add(tu);
		
		doc.add(new SkeletonUnit("sLast", "</doc>"));
		show(doc, 0);
		
		System.out.println("\nTest for content:\n");
		
		tu = new TextUnit("t1", "Before ");
		src = tu.getSource();
		src.append(TagType.OPENING, "b", "<b1>");
		src.append("bold");
		src.append(TagType.OPENING, "b", "<b2>");
		src.append("rebold.");
		src.append(TagType.CLOSING, "b", "</b2>");
		src.append(TagType.PLACEHOLDER, "br", "<br/>");
		src.append(" more");
		System.out.println("-Initial:");
		System.out.println(tu.toString());
		
		src.append(TagType.CLOSING, "b", "</b1>");
		System.out.println("-After adding </b1>:");
		System.out.println(src.toString());
		
		TextContainer src2 = src.subSequence(9, 15);
		System.out.println("-SubSequence(9, 15):");
		System.out.println("src2: "+src2.toString());
		
		src.codes.get(1).data = "<B2>";
		System.out.println("-Modify <b2> to <B2> in original:");
		System.out.println("src1: "+src.toString());
		System.out.println("src2: "+src2.toString());
		
		TextContainer src3 = src.subSequence(15, 26);
		System.out.println("-SubSequence(15, 26):");
		System.out.println("src3: "+src3.toString());

		src2.append(src3);
		System.out.println("-After appending src3 to src2:");
		System.out.println("src2: "+src2.toString());
		
		TextContainer src4 = src.subSequence(9, 26);
		System.out.println("-SubSequence(9, 26):");
		System.out.println("src4: "+src4.toString());

		System.out.println("src4 compared to src2 as string = "+src4.toString().compareTo(src2.toString()));
		System.out.println("src4 compared to src2 as TextContainer = "+src4.compareTo(src2));
		System.out.println("src4 equals to src2 = "+src4.equals(src2));
		
	}

	private static void show (IResourceContainer container,
		int level)
	{
		if ( container instanceof Document ) {
			for ( int i=0; i<level; i++ ) System.out.print('-'); 
			System.out.println("document");
		}
		else if ( container instanceof Group ) {
			for ( int i=0; i<level; i++ ) System.out.print('-'); 
			System.out.println("group");
		}

		for ( IContainable unit : container ) {
			if ( unit instanceof Group ) {
				show((IResourceContainer)unit, level+1);
			}
			else if ( unit instanceof TextUnit ) {
				for ( int i=0; i<level; i++ ) System.out.print('-'); 
				System.out.println("text-unit: "+unit.toString());
			}
			else if ( unit instanceof SkeletonUnit ) {
				for ( int i=0; i<level; i++ ) System.out.print('-'); 
				System.out.println("skeleton-unit: "+unit.toString());
			}
		}
	}
}
