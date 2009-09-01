package net.sf.okapi.tm.pensieve.common;

import net.sf.okapi.common.resource.TextFragment;

/**
 * User: Christian Hargraves
 * Date: Aug 19, 2009
 * Time: 6:53:34 AM
 */
public class TranslationUnit {
    private TextFragment source;
    private TextFragment target;

    public TranslationUnit(TextFragment source, TextFragment target) {
        this.source = source;
        this.target = target;
    }

    public TextFragment getSource() {
        return source;
    }

    public TextFragment getTarget() {
        return target;
    }
}
