package net.sf.okapi.lib.tmdb.h2;

import java.sql.SQLException;

import net.sf.okapi.common.Util;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ProcesswithAPI;

import org.junit.Test;

public class RepositoryTest {
	
	@Test
	public void testSimpleTest ()
		throws SQLException
	{
		String repoPath = Util.ensureSeparator(Util.getTempDirectory(), true) + "testTMRepository";
		// Make sure the repository is not there
		Repository.delete(repoPath);

		// Create repository (step 1)
		IRepository repo = new Repository(repoPath);
		ProcesswithAPI.runMultipleTestsStep1(repo);

		// Run the second step with the existing repository (step 2)
		repo = new Repository(repoPath);
		ProcesswithAPI.runMultipleTestsStep2(repo);
	}

	@Test
	public void testMultiFieldsAdd ()
		throws SQLException
	{
		// Create memory repository
		IRepository repo = new Repository(null);
		ProcesswithAPI.runMultipleTestsStep3(repo);
		
		repo = new Repository(null);
//		ProcesswithAPI.runMultipleTestsStep4(repo);
	}
	
}
