package lerfob;
import org.junit.Assert;
import org.junit.Test;

import lerfob.carbonbalancetool.Starter;


public class ProductionEnvironmentTest {
	@Test 
	
	/**
	 * This test will fail when used with a local class folder, but will succeed when running on a JAR file.
	 * This test is excluded from the normal local tests and executed only with the integration tests from the JAR file
	 */
	public void makeSurePackageIsRunningFromJAR() {
		String resourceURL = Starter.class.getResource("Starter.class").toString();
		Assert.assertTrue(resourceURL.startsWith("jar:"));
	}
}
