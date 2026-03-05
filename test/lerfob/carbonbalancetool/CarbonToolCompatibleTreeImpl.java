package lerfob.carbonbalancetool;

import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;

class CarbonToolCompatibleTreeImpl implements CATCompatibleTree, Cloneable {

	
	private final double number;
	private final double volM3;
	private final String speciesName;
	private final Species species;
	private final SpeciesLocale locale;
	
	protected CarbonToolCompatibleTreeImpl(double number, double volM3, Species species, SpeciesLocale locale) {
		this.number = number;
		this.volM3 = volM3;
		this.speciesName = species.getLatinName();
		this.species = species;
		this.locale = locale;
	}

	protected CarbonToolCompatibleTreeImpl(double volM3, Species species, SpeciesLocale locale) {
		this(1d, volM3, species, locale);
	}

	@Override
	public double getNumber() {return number;}

	@Override
	public double getCommercialVolumeM3() {return volM3;}

	@Override
	public String getSpeciesName() {return speciesName;}

	@Override
	public CATCompatibleTree clone() {
		return new CarbonToolCompatibleTreeImpl(number, volM3, species, locale);
	}

	@Override
	public Species getCATSpecies() {return species;}

	@Override
	public boolean isCommercialVolumeOverbark() {
		return true;
	}

	@Override
	public SpeciesLocale getSpeciesLocale() {return locale;}

}
