/*===========================================================================
Copyright (C) 2008-2009 by the Okapi Framework contributors
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.okapi.tm.pensieve.analyzers;

import com.ibm.icu.lang.UCharacter;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 *
 * @author HaslamJD
 */
public class AlphabeticNgramTokenizer extends Tokenizer {

    private static final Locale ARMENIAN = new Locale("hy");
    private static final int NO_CHAR = -1;
    private static final Locale SINHALA = new Locale("si");
    private int ngramLength;
    private String ngramType;
    private int offset;
    private TermAttribute termAttribute;
    private OffsetAttribute offsetAttribute;
    private TypeAttribute typeAttribute;
    private char[] ngram;
    private Locale locale;

    public AlphabeticNgramTokenizer(Reader reader, int ngramLength, Locale locale) {
        super(reader);
        if (ngramLength <= 0) {
            throw new IllegalArgumentException("ngramLength must be greater than 0");
        }
        this.ngramLength = ngramLength;
        this.termAttribute = (TermAttribute) addAttribute(TermAttribute.class);
        this.offsetAttribute = (OffsetAttribute) addAttribute(OffsetAttribute.class);
        this.typeAttribute = (TypeAttribute) addAttribute(TypeAttribute.class);
        this.locale = locale;
        this.ngram = new char[ngramLength];
        this.ngramType = "ngram(" + getNgramLength() + ")";
        this.offset = 0;
    }

    public Locale getLocale() {
        return locale;
    }

    public int getNgramLength() {
        return ngramLength;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        int c;
        for (int i = 0; i < ngramLength; i++) {
            c = input.read();
            if (c == NO_CHAR) {
                offset = 0;
                return false;
            }
            ngram[i] = (char) c;
        }

        //Populate Attributes
        termAttribute.setTermBuffer(toLowerCase(ngram));
        offsetAttribute.setOffset(offset, offset + termAttribute.termLength());
        typeAttribute.setType(ngramType);

        //Reset to marker and then advance marker and offset;
        input.reset();
        input.skip(1);
        input.mark(ngramLength);
        offset++;
        return true;
    }

    @Override
    public void reset(Reader input) throws IOException {
        super.reset(input);
        offset = 0;
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException("reset is not supported please create a new reader and use reset(Reader r)");
    }

    private String toLowerCase(char[] ngram) {
        String termValue = new String(ngram);
        //Don't we want to always lowercase things?  Not only if a locale is specified
        if (locale != null) {
            if (!(locale.equals(ARMENIAN) || locale.equals(SINHALA))) // FIXME:
            // we can't lowercase Armenain yet
            {
                // use ICU4J to lower case - should be more accurate
                //TODO:  why are we not using default mappings??
                //termValue = UCharacter.foldCase(termValue, false);
                //http://icu-project.org/apiref/icu4j/com/ibm/icu/lang/UCharacter.html#foldCase%28int,%20boolean%29
                termValue = UCharacter.foldCase(termValue, true);
            }
        }
        return termValue;
    }
}
