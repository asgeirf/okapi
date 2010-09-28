package net.sf.okapi.steps.idaligner;

import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.uidescription.CheckboxPart;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.PathInputPart;

@EditorFor(Parameters.class)
public class Parameters extends BaseParameters implements IEditorDescriptionProvider {

	private static final String GENERATETMX = "generateTMX";
	private static final String TMXOUTPUTPATH = "tmxOutputPath";
	private static final String REPLACEWITHSOURCE = "replaceWithSource";

	private boolean generateTMX;
	private String tmxOutputPath;
	private boolean replaceWithSource;

	public Parameters() {
		reset();
	}

	public boolean getGenerateTMX() {
		return generateTMX;
	}

	public void setGenerateTMX(boolean generateTMX) {
		this.generateTMX = generateTMX;
	}

	public String getTmxOutputPath() {
		return tmxOutputPath;
	}

	public void setTmxOutputPath(String tmxOutputPath) {
		this.tmxOutputPath = tmxOutputPath;
	}
	
	public boolean getReplaceWithSource() {
		return replaceWithSource;
	}
	
	public void setReplaceWithSource(boolean replaceWithSource) {
		this.replaceWithSource = replaceWithSource;
	}

	@Override
	public void reset() {
		tmxOutputPath = "aligned.tmx";
		generateTMX = true;
		replaceWithSource = true;
	}

	@Override
	public void fromString(String data) {
		reset();
		buffer.fromString(data);
		generateTMX = buffer.getBoolean(GENERATETMX, generateTMX);
		tmxOutputPath = buffer.getString(TMXOUTPUTPATH, tmxOutputPath);
		replaceWithSource = buffer.getBoolean(REPLACEWITHSOURCE, replaceWithSource);
	}

	@Override
	public String toString() {
		buffer.reset();
		buffer.setBoolean(GENERATETMX, generateTMX);
		buffer.setParameter(TMXOUTPUTPATH, tmxOutputPath);
		buffer.setBoolean(REPLACEWITHSOURCE, replaceWithSource);

		return buffer.toString();
	}

	@Override
	public ParametersDescription getParametersDescription() {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(GENERATETMX, "Generate a TMX file:",
				"If generateTMX is false generate bilingual TextUnits, otherwise (true) output a TMX file");
		desc.add(TMXOUTPUTPATH, "TMX output path", "Full path of the output TMX file");
		desc.add(REPLACEWITHSOURCE, "Replace with source", "If no target text available, use the source text");
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription(ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("Id-based Aligner", true, false);
		CheckboxPart cbp = desc.addCheckboxPart(paramsDesc.get(GENERATETMX));
		PathInputPart pip = desc.addPathInputPart(paramsDesc.get(TMXOUTPUTPATH),
				"TMX Document to Generate", true);
		pip.setBrowseFilters("TMX Documents (*.tmx)\tAll Files (*.*)", "*.tmx\t*.*");
		pip.setWithLabel(false);
		pip.setMasterPart(cbp, true);

		desc.addSeparatorPart();
		desc.addCheckboxPart(paramsDesc.get(REPLACEWITHSOURCE));

		return desc;
	}
}
