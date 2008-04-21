package net.sf.okapi.Package.OmegaT;

import java.io.File;

import net.sf.okapi.Format.XML.XMLWriter;
import net.sf.okapi.Library.Base.ILog;
import net.sf.okapi.Library.Base.Utils;

public class Writer extends net.sf.okapi.Package.XLIFF.Writer {

	public Writer(ILog log) {
		super(log);
	}

	@Override
	public String getPackageType () {
		return "omegat";
	}
	
	@Override
	public void writeStartPackage ()
	{
		super.writeStartPackage();
		m_Mnf.setSourceLocation("source");
		m_Mnf.setTargetLocation("target");
		
		// Create the OmegaT directories
		Utils.createDirectories(m_Mnf.getRoot() + File.separator + "glossary" + File.separator);
		Utils.createDirectories(m_Mnf.getRoot() + File.separator + "omegat" + File.separator);
		Utils.createDirectories(m_Mnf.getRoot() + File.separator + "source" + File.separator);
		Utils.createDirectories(m_Mnf.getRoot() + File.separator + "target" + File.separator);
		Utils.createDirectories(m_Mnf.getRoot() + File.separator + "tm" + File.separator);
	}

	@Override
	public void writeEndPackage (boolean p_bCreateZip) {
		// Write the OmegaT project file
		createOmegaTProject();
		// And then, normal ending
		super.writeEndPackage(p_bCreateZip);
	}

	private void createOmegaTProject () {
		XMLWriter XR = null;
		try {
			XR = new XMLWriter();
			XR.create(m_Mnf.getRoot() + File.separator + "omegat.project");
			XR.writeStartDocument();
			XR.writeStartElement("omegat");
			XR.writeStartElement("project");
			XR.writeAttributeString("version", "1.0");

			XR.writeStartElement("source_dir");
			XR.writeRawXML("__DEFAULT__");
			XR.writeEndElement(); // source_dir
			
			XR.writeStartElement("target_dir");
			XR.writeRawXML("__DEFAULT__");
			XR.writeEndElement(); // target_dir
			
			XR.writeStartElement("tm_dir");
			XR.writeRawXML("__DEFAULT__");
			XR.writeEndElement(); // tm_dir
			
			XR.writeStartElement("glossary_dir");
			XR.writeRawXML("__DEFAULT__");
			XR.writeEndElement(); // glossary_dir
			
			XR.writeStartElement("source_lang");
			XR.writeRawXML(m_Mnf.getSourceLanguage());
			XR.writeEndElement(); // source_lang

			XR.writeStartElement("target_lang");
			XR.writeRawXML(m_Mnf.getTargetLanguage());
			XR.writeEndElement(); // target_lang

			XR.writeStartElement("sentence_seg");
			XR.writeRawXML("true");
			XR.writeEndElement(); // sentence_seg

			XR.writeEndElement(); // project
			XR.writeEndElement(); // omegat
		}
		catch ( Exception E ) {
			m_Log.error(E.getLocalizedMessage());
		}
		finally {
			if ( XR != null ) {
				XR.writeEndDocument();
				XR.close();
			}
		}
	}
}
