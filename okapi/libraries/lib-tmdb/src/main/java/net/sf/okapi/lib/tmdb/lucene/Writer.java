/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.tmdb.lucene;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.lib.search.lucene.analysis.NgramAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class Writer {
	
	private static final Logger LOGGER = Logger.getLogger(Writer.class.getName());

	private IndexWriter indexWriter;

	/**
	 * Creates a OWriter object
	 * 
	 * @param indexDirectory
	 *            - the Lucene Directory implementation of choice.
	 * @param createNewTmIndex
	 *            Set to false to append to the existing TM index file. Set to true to overwrite.
	 * @throws IOException
	 *             if the indexDirectory can not load
	 */
	public Writer (Directory indexDirectory,
		boolean createNewTmIndex)
		throws IOException
	{
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, new NgramAnalyzer(Locale.ENGLISH, 4));
		iwc.setOpenMode(createNewTmIndex ? OpenMode.CREATE: OpenMode.CREATE_OR_APPEND);
		indexWriter = new IndexWriter(indexDirectory, iwc);
	}

	/**
	 * Commits and closes (for now) the transaction.
	 * 
	 * @throws OkapiIOException
	 *             if the commit cannot happen.
	 */
	public void close () {
		try {
			indexWriter.commit();		
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e); // To change body of catch statement use File | Settings | File Templates.
		}
		catch ( AlreadyClosedException ignored ) {
			// Ignore this exception
		}
		finally {
			try {
				indexWriter.close();
			}
			catch ( IOException ignored ) {
				LOGGER.log(Level.WARNING, "Exception closing index.", ignored); //$NON-NLS-1$
			}
		}
	}
	
	public void commit () {
		try {
			indexWriter.commit();
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error when committing.", e);
		}
	}
	

	/**
	 * Gets a handle on the IndexWriter so that commits and rollbacks can happen outside. For now, this is a convenience
	 * method. In other words, don't depend on it working for you.
	 * 
	 * @return a handle on the IndexWriter used to Create, Update or Delete the index.
	 */
	public IndexWriter getIndexWriter () {
		return indexWriter;
	}

	public void index (TmEntry entry) {
		Document doc = createDocument(entry);
		if ( doc != null ) { // Skip empty/invalid entries
			try {
				indexWriter.addDocument(doc);
			}
			catch ( CorruptIndexException e ) {
				throw new OkapiIOException(
					"Error adding an entry to the TM index. Corrupted index.", e);
			}
			catch ( IOException e ) {
				throw new OkapiIOException("Error adding an entry to the TM index.", e);
			}
		}
	}

	public void index (TmEntry entry,
		boolean overwrite)
	{
		/*try {
			if ( overwrite ) {
				TextFragment srcFrag = tu.getSource().getContent();
				if ( srcFrag.hasCode() ) {
					BooleanQuery bq = new BooleanQuery();
					bq.add(
						new TermQuery(
							new Term(TranslationUnitField.SOURCE_EXACT.name(),
								srcFrag.getCodedText())
						),
						BooleanClause.Occur.MUST);
					bq.add(
						new TermQuery(
							new Term(TranslationUnitField.SOURCE_CODES.name(),
								Code.codesToString(srcFrag.getCodes(), true))
						), BooleanClause.Occur.MUST);			
					indexWriter.deleteDocuments(bq);
				}
				else {
					indexWriter.deleteDocuments(new Term(TranslationUnitField.SOURCE_EXACT.name(),
						srcFrag.getCodedText()));
				}
			}
		}
		catch (CorruptIndexException e) {
			throw new OkapiIOException("Error deleting a translationUnit from the TM. Corrupted index.", e);
		}
		catch (IOException e) {
			throw new OkapiIOException("Error deleting a translationUnit from the TM.", e);
		}*/
		index(entry);
	}

	/**
	 * Deletes an entry from the index.
	 * @param id the ID of the enrty to be deleted.
	 */
	public void delete (String entryId) {
		if ( Util.isEmpty(entryId) ) {
			throw new IllegalArgumentException("id is a required field for delete to happen");
		}
		try {
			indexWriter.deleteDocuments(new Term(TmEntry.ID_FIELDNAME, entryId));
		}
		catch ( CorruptIndexException e ) {
			throw new OkapiIOException("Error deleting an index entry from the TM. Corrupted index.", e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error deleting an index entry from the TM.", e);
		}
	}
	
	public void delete (String fieldName,
		String fieldValue)
	{
		try {
			indexWriter.deleteDocuments(new Term(fieldName, fieldValue));
		}
		catch ( CorruptIndexException e ) {
			throw new OkapiIOException("Error deleting an index entry from the TM. Corrupted index.", e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error deleting an index entry from the TM.", e);
		}
	}
	
// TODO: same as index with overwrite???
	public void update (TmEntry entry) {
		delete(entry.getIdField().stringValue());
		index(entry);
	}

	Document createDocument (TmEntry entry) {
		Document doc = new Document();

		// Set the document Id
		doc.add(entry.getIdField());
		doc.add(entry.getTmIdField());
		doc.add(entry.getSegKeyField());
		
		// Set variants
		int count = 0;
		for ( String locale : entry.getVariants().keySet() ) {
			Variant variant = entry.getVariant(locale);
			if ( Util.isEmpty(variant.getGenericTextField().stringValue()) ) continue;
			doc.add(variant.getGenericTextField());
			Field codesField = variant.getCodesField();
			if ( codesField != null ) {
				doc.add(codesField);
			}
			count++;
		}
		if ( count == 0 ) return null;
		
		// Set the metadata fields
		if ( entry.hasAttribute() ) {
			for ( Field field : entry.getAttributes().values() ) {
				doc.add(field);
			}
		}
		
		return doc;
	}

}
