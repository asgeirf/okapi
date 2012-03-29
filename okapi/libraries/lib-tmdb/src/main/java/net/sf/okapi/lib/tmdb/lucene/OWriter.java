/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.search.lucene.analysis.NgramAnalyzer;
import net.sf.okapi.lib.tmdb.DbUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
 * Used to write, delete and update the index.
 * All files in this package are based on the files by @author HaslamJD and @author HARGRAVEJE 
 * in the okapi-tm-pensieve project and in most cases there are only minor changes.
 */
public class OWriter {
	
	private static final Logger LOGGER = Logger.getLogger(OWriter.class.getName());

	private IndexWriter indexWriter;

	/**
	 * Creates a PensieveWriter
	 * 
	 * @param indexDirectory
	 *            - the Lucene Directory implementation of choice.
	 * @param createNewTmIndex
	 *            Set to false to append to the existing TM index file. Set to true to overwrite.
	 * @throws IOException
	 *             if the indexDirectory can not load
	 */
	public OWriter (Directory indexDirectory,
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

	/**
	 * Adds a TranslationUnit to the index
	 * 
	 * @param tu
	 *            The TranslationUnit to index
	 * @throws IOException
	 *             if the TU can not be indexed.
	 * @throws IllegalArgumentException
	 *             if tu is null
	 */
	public void index (OTranslationUnitInput tu) {
		if ( tu == null ) {
			throw new NullPointerException("TextUnit can not be null");
		}
		Document doc = createDocument(tu);
		if ( doc != null ) {
			try {
				indexWriter.addDocument(doc);
			}
			catch ( CorruptIndexException e ) {
				throw new OkapiIOException(
					"Error adding a translationUnit to the TM. Corrupted index.", e);
			}
			catch ( IOException e ) {
				throw new OkapiIOException("Error adding a translationUnit to the TM.", e);
			}
		}
	}

	public void index (OTranslationUnitInput tu,
		boolean overwrite)
	{
		if ( tu == null ) {
			throw new NullPointerException("TextUnit can not be null.");
		}
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
		
		index(tu);
	}

	/**
	 * Deletes a TranslationUnit based on the id.
	 * @param id the unique ID of the TU to delete
	 * @throws OkapiIOException if the delete can not happen
	 * @throws IllegalArgumentException if the id is invalid
	 */
	public void delete (String id) {
		if (Util.isEmpty(id)) {
			throw new IllegalArgumentException("id is a required field for delete to happen");
		}
		try {
			indexWriter.deleteDocuments(new Term(OTranslationUnitInput.DEFAULT_ID_NAME, id));
		} catch (CorruptIndexException e) {
			throw new OkapiIOException("Error deleting a translationUnit from the TM. Corrupted index.", e);
		} catch (IOException e) {
			throw new OkapiIOException("Error deleting a translationUnit from the TM.", e);
		}
	}
	
	/**
	 * Deletes a TranslationUnit based on the id.
	 * @param id the unique ID of the TU to delete
	 * @throws OkapiIOException if the delete can not happen
	 * @throws IllegalArgumentException if the id is invalid
	 */
	public void delete (OField id) {
		if (( id == null ) || Util.isEmpty(id.getName()) || Util.isEmpty(id.getValue())) {
			throw new IllegalArgumentException("Id is a required field for delete to happen");
		}
		try {
			indexWriter.deleteDocuments(new Term(id.getName(), id.getValue()));
		}
		catch ( CorruptIndexException e ) {
			throw new OkapiIOException("Error deleting a translationUnit from the TM. Corrupted index.", e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error deleting a translationUnit from the TM.", e);
		}
	}
	

	/**
	 * Updates a TranslationUnit.
	 * @param tu The TranslationUnit to update
	 * @throws IOException if the update can not happen
	 * @throws IllegalArgumentException if the tu or MetadataType.ID is null
	 */
	public void update (OTranslationUnitInput tu) {
		if (( tu == null ) || ( tu.getId() == null )) {
			throw new IllegalArgumentException("tu must be set and have its ID set");
		}
		delete(tu.getId());
		index(tu);
	}

	/**
	 * Add the fields...all other fields..
	 * @param doc
	 * @param fields
	 */
	void addFieldsToDocument (Document doc,
		OFields fields)
	{
		for ( OField field : fields.values() ) {
			doc.add(new Field(field.getName(), field.getValue(), field.getStore(), field.getIndex()));
		}
	}

	/**
	 *Create a Lucene document 
	 * @param tu
	 * @return
	 */
	Document createDocument(OTranslationUnitInput tu) {
		if (tu == null) {
			throw new NullPointerException("no content");
		}

		Document doc = new Document();

		//SET ID
		doc.add(new Field(tu.getIdName(), tu.getIdValue(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//SET TARGETS
		for (OTranslationUnitVariant variant : tu.getVariants()) {
			
			String locale = variant.getLanguage();
			TextFragment content = variant.getContent();

			//--skipt empty-- and log--
			// an empty source is not fatal just skip it			
			if(locale == null || content == null || content.isEmpty()){
				continue;
			} 
			
			//TODO check locale string--
			String keyIndexField = DbUtil.TEXT_PREFIX+locale;
			String keyExactField = "EXACT_"+DbUtil.TEXT_PREFIX+locale;
			String keyCodesField = DbUtil.CODES_PREFIX+locale;

			doc.add(createRawCodedTextField(keyExactField, content, Field.Store.YES, Field.Index.NOT_ANALYZED));
			// ANALYZED_NO_NORMS: We don't need Lucene to manage this for us, we will implement our own scoring and
			// normalization.
			doc.add(createIndexedTextField(keyIndexField, content, Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
			doc.add(createCodesField(keyCodesField, content, Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		}
		
		//SET REMAINING FIELDS
		addFieldsToDocument(doc, tu.getFields());
	
		return doc;
	}

	/**
	 * Use this create method for fields that need to be indexed for fuzzy search
	 * @param key
	 * @param frag
	 * @param store
	 * @param index
	 * @return
	 */
	private Field createIndexedTextField(String key, TextFragment frag,
			Field.Store store, Field.Index index) {
		return new Field(key, frag.getText(), store, index, TermVector.YES);
	}

	/**
	 * Use this create method to save the content
	 * @param key
	 * @param frag
	 * @param store
	 * @param index
	 * @return
	 */
	private Field createRawCodedTextField(String key, TextFragment frag,
			Field.Store store, Field.Index index) {
		return new Field(key, frag.getCodedText(), store, index);
	}
	
	/**
	 * Use this create method for codes field that relates the indexed field.
	 * @param field
	 * @param frag
	 * @param store
	 * @param index
	 * @return
	 */
	private Field createCodesField(String key, TextFragment frag, Field.Store store,
		Field.Index index)
	{
		// We don't keep the outerData in the codes
		return new Field(key, Code.codesToString(frag.getCodes(), true), store, index);
	}
}
