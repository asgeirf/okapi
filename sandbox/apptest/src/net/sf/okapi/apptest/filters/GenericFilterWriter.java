package net.sf.okapi.apptest.filters;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Stack;

import net.sf.okapi.apptest.common.IParameters;
import net.sf.okapi.apptest.common.ISkeleton;
import net.sf.okapi.apptest.resource.BuilderData;
import net.sf.okapi.apptest.resource.Ending;
import net.sf.okapi.apptest.resource.GenericSkeleton;
import net.sf.okapi.apptest.resource.GenericSkeletonPart;
import net.sf.okapi.apptest.resource.Group;
import net.sf.okapi.apptest.resource.PropertiesUnit;
import net.sf.okapi.apptest.resource.TextContainer;
import net.sf.okapi.apptest.resource.TextUnit;
import net.sf.okapi.common.Util;

public class GenericFilterWriter implements IFilterWriter {

	protected OutputStream output;
	protected String language;
	protected String encoding;
	protected String outputPath;
	protected IParameters params;
	private OutputStreamWriter writer;
	private Stack<Group> groupStack;
	private BuilderData builderData;
	
	public GenericFilterWriter () {
		builderData = new BuilderData();
	}
	
	public void close () {
		try {
			if ( writer != null ) {
				writer.close();
				writer = null;
				//TODO: do we need to close the underlying stream???
			}
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public String getName () {
		return "GenericSkeletonWriter";
	}

	public IParameters getParameters () {
		return params;
	}

	public void setOutputTarget (boolean value) {
		builderData.outputTarget = value;
	}
	
	public void setEncoder (IEncoder encoder) {
		builderData.encoder = encoder;
	}
	
	public void handleEvent (FilterEvent event) {
		try {
			switch ( event.getEventType() ) {
			case START_DOCUMENT:
				createWriter();
				processSkeleton(event.getResource().getSkeleton());
				break;
			case END_DOCUMENT:
				processSkeleton(event.getResource().getSkeleton());
				close();
				break;
			case START_SUBDOCUMENT:
				processSkeleton(event.getResource().getSkeleton());
				break;
			case END_SUBDOCUMENT:
				processSkeleton(event.getResource().getSkeleton());
				break;
			case START_GROUP:
				processStartGroup((Group)event.getResource());
				processSkeleton(event.getResource().getSkeleton());
				break;
			case END_GROUP:
				processSkeleton(event.getResource().getSkeleton());
				processEndGroup((Ending)event.getResource());
				break;
			case TEXT_UNIT:
				writeTextUnit((TextUnit)event.getResource());
				processSkeleton(event.getResource().getSkeleton());
				break;
			case PROPERTIES_UNIT:
				processPropertiesUnit((PropertiesUnit)event.getResource());
				processSkeleton(event.getResource().getSkeleton());
				break;
			}
		}
		catch ( FileNotFoundException e ) {
			throw new RuntimeException(e);
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public void setOptions (String language,
		String defaultEncoding)
	{
		this.language = language;
		this.encoding = defaultEncoding;
	}

	public void setOutput (String path) {
		close();
		this.outputPath = path;
	}

	public void setOutput(OutputStream output) {
		close(); // make sure previous is closed
		this.output = output; // then assign the new stream
	}

	public void setParameters (IParameters params) {
		this.params = params;
	}

	private void processSkeleton (ISkeleton skeleton) throws IOException {
		if ( skeleton == null ) return; // Nothing to process
		GenericSkeleton skel = (GenericSkeleton)skeleton;
		for ( GenericSkeletonPart part : skel.getParts() ) {
			if ( part.isReference() ) {
				builderData.references.add(part);
			}
			if ( groupStack.size() > 0 ) {
				groupStack.peek().add(part);
			}
			else if ( !part.isReference() ) {
				writer.write(part.toString(builderData));
			}
		}
	}
	
	private void processPropertiesUnit (PropertiesUnit resource) {
		if ( resource.isReference() ) {
			builderData.references.add(resource);
		}
		else if ( groupStack.size() > 0 ) {
			groupStack.peek().add(resource);
		}
	}
	
	private void processStartGroup (Group resource) {
		if ( resource.isReference() ) {
			builderData.references.add(resource);
			groupStack.push(resource);
		}
		else if ( groupStack.size() > 0 ) {
			groupStack.peek().add(resource);
			groupStack.push(resource);
		}
	}
	
	private void processEndGroup (Ending resource) {
		if ( groupStack.size() > 0 ) {
			groupStack.pop();
		}
	}
	
	private void createWriter () throws FileNotFoundException, UnsupportedEncodingException {
		// Create the output writer from the provided stream
		if ( output == null ) {
			output = new BufferedOutputStream(new FileOutputStream(outputPath));
		}
		writer = new OutputStreamWriter(output, encoding);
		Util.writeBOMIfNeeded(writer, true, encoding);
		groupStack = new Stack<Group>();
	}
	
	private void writeTextUnit (TextUnit unit) throws IOException {
		if ( unit.isReference() ) {
			builderData.references.add(unit);
		}
		if ( groupStack.size() > 0 ) {
			groupStack.peek().add(unit);
		}
		else if ( !unit.isReference() ) {
			TextContainer tc;
			if ( unit.hasTarget() ) tc = unit.getTargetContent();
			else tc = unit.getSourceContent();
			writer.write(tc.toString(builderData));
		}
	}
}
