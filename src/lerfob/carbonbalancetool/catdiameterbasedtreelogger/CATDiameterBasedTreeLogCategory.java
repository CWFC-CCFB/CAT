package lerfob.carbonbalancetool.catdiameterbasedtreelogger;

import java.util.List;

import lerfob.carbonbalancetool.CATSettings.CATSpecies;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLogCategory;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedWoodPiece;
import repicea.serial.PostUnmarshalling;
import repicea.simulation.treelogger.LoggableTree;

@SuppressWarnings({ "serial", "deprecation" })
public class CATDiameterBasedTreeLogCategory extends DiameterBasedTreeLogCategory implements PostUnmarshalling {

	private transient CATDiameterBasedTreeLogCategoryPanel guiInterface;

	public CATDiameterBasedTreeLogCategory(Enum<?> logGrade, Enum<?> species, double minimumDbhCm,
			double conversionFactor, double downgradingFactor, boolean isFromStump,
			DiameterBasedTreeLogCategory subCategory) {
		super(logGrade, species.name(), minimumDbhCm, conversionFactor, downgradingFactor, isFromStump, subCategory);
	}
	
//	/*
//	 * For extended visibility
//	 */
//	@Override
//	public void setSpecies(Object species) {
//		super.setSpecies(species);
//	}
	
	@Override
	public CATDiameterBasedTreeLogCategoryPanel getUI() {
		if (guiInterface == null) {
			guiInterface = new CATDiameterBasedTreeLogCategoryPanel(this);
		}
		return guiInterface;
	}

	/*
	 * Just for extended visibility (non-Javadoc)
	 * @see lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLogCategory#extractFromTree(repicea.simulation.treelogger.LoggableTree, java.lang.Object[])
	 */
	@Override
	protected List<DiameterBasedWoodPiece> extractFromTree(LoggableTree tree, Object... parms) {
		return super.extractFromTree(tree, parms);
	}

	@Override
	public void postUnmarshallingAction() {
		if (this.getSpecies() instanceof CATSpecies) {
			this.setSpecies(((CATSpecies) getSpecies()).species);
		}
	}

}
