package lerfob.carbonbalancetool;

import repicea.simulation.species.REpiceaSpecies.Species;

class CarbonToolCompatibleTreeImpl implements CATCompatibleTree, Cloneable {

	
	private final double number;
	private final double volM3;
	private final String speciesName;
	private final Species species;
	
	protected CarbonToolCompatibleTreeImpl(double number, double volM3, Species species) {
		this.number = number;
		this.volM3 = volM3;
		this.speciesName = species.getLatinName();
		this.species = species;
	}

	protected CarbonToolCompatibleTreeImpl(double volM3, Species species) {
		this(1d, volM3, species);
	}

	@Override
	public double getNumber() {return number;}

	@Override
	public double getCommercialVolumeM3() {return volM3;}

	@Override
	public String getSpeciesName() {return speciesName;}

	@Override
	public CATCompatibleTree clone() {
		return new CarbonToolCompatibleTreeImpl(number, volM3, species);
	}

	@Override
	public Species getCATSpecies() {return species;}

	@Override
	public boolean isCommercialVolumeOverbark() {
		return true;
	}

}
