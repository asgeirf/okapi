package net.sf.okapi.lib.tmdb;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.lib.tmdb.DbUtil;
import net.sf.okapi.lib.tmdb.IRepository;
import net.sf.okapi.lib.tmdb.ITm;
import net.sf.okapi.lib.tmdb.DbUtil.PageMode;

public class ProcesswithAPI {
	
	public static void runMultipleTestsStep1 (IRepository repo)
		throws SQLException
	{
		// There should be no TM
		List<String> list = repo.getTmNames();
		assertEquals(0, list.size());

		// Create a first TM
		String tmName = "first";
		String localeCode = DbUtil.toOlifantLocaleCode(LocaleId.ENGLISH);
		ITm tm = repo.createTm(tmName, null, localeCode);
		assertEquals(tmName, tm.getName());
		list = repo.getTmNames();
		assertEquals(1, list.size());
		assertEquals(tmName, list.get(0));
		
		// Verify we can access that first TM
		tm = repo.openTm(tmName);
		assertEquals(tmName, tm.getName());
		assertNull(tm.getDescription());

		// Close the repository
		repo.close();
	}
	
	public static void runMultipleTestsStep2 (IRepository repo)
		throws SQLException
	{
		// Verify we can access that first TM
		String tmName = "first";
		ITm tm = repo.openTm(tmName);
		assertEquals(tmName, tm.getName());
		assertNull(tm.getDescription());
		
		// Get the list of available fields
		List<String> list = tm.getAvailableFields();
		
		// Get the first page for all fields (should be empty)
		list.remove(0);
		tm.setRecordFields(list);
		IRecordSet rs = tm.getFirstPage();
		assertNull(rs);
//		int i = 0;
//		while ( rs.next() ) {
//			i++;
//		}
//		assertEquals(0, i);
		
		// Add some records
		tm.startImport();
		LinkedHashMap<String, Object> segMap = new LinkedHashMap<String, Object>();
		segMap.put(DbUtil.TEXT_PREFIX+"EN", "Text EN 1");
		segMap.put(DbUtil.CODES_PREFIX+"EN", null);
		tm.addRecord(-1, null, segMap);
		tm.finishImport();

		// Test the result
		tm.setRecordFields(Arrays.asList(new String[]{DbUtil.TEXT_PREFIX+"EN"}));
		rs = tm.getFirstPage();
		int i = 0;
		while ( rs.next() ) {
			i++;
			assertEquals("Text EN 1", rs.getString(3));
			assertEquals("Text EN 1", rs.getString(DbUtil.TEXT_PREFIX+"EN"));
		}
		assertEquals(1, i);
		
		// Add another row
		tm.startImport();
		segMap.clear();
		segMap.put(DbUtil.TEXT_PREFIX+"EN", "Text EN 2");
		segMap.put(DbUtil.CODES_PREFIX+"EN", null);
		segMap.put("Quality"+DbUtil.LOC_SEP+"EN", 80);

		LinkedHashMap<String, Object> tuMap = new LinkedHashMap<String, Object>();
		tuMap.put("TUInfo", "TU-level info");
		tm.addRecord(-1, tuMap, segMap);
		tm.finishImport();

		// Check the two rows
		tm.setRecordFields(Arrays.asList(new String[]{DbUtil.TEXT_PREFIX+"EN","Quality"+DbUtil.LOC_SEP+"EN","TUInfo"}));
		rs = tm.getFirstPage();
		rs.next();
		assertEquals("Text EN 1", rs.getString(3));
		rs.next();
		assertEquals("Text EN 2", rs.getString(3));
		assertEquals(80, rs.getLong(4));
		assertEquals("TU-level info", rs.getString(5));

		// Add another row, this time create new fields
		tm.startImport();
		segMap.clear();
		segMap.put(DbUtil.TEXT_PREFIX+"EN", "Text EN 3");
		segMap.put(DbUtil.CODES_PREFIX+"EN", null);
		segMap.put(DbUtil.TEXT_PREFIX+"FR", "Text FR 3");
		segMap.put(DbUtil.CODES_PREFIX+"FR", null);
		segMap.put("Quality"+DbUtil.LOC_SEP+"FR", 33);
		tm.addRecord(-1, null, segMap);
		segMap.clear();
		segMap.put(DbUtil.TEXT_PREFIX+"EN", "Text EN 4");
		segMap.put(DbUtil.CODES_PREFIX+"EN", null);
		segMap.put(DbUtil.TEXT_PREFIX+"FR", "Text FR 4");
		segMap.put(DbUtil.CODES_PREFIX+"FR", null);
		tm.addRecord(-1, null, segMap);
		tm.finishImport();

		// Check the new row
		tm.setRecordFields(Arrays.asList(new String[]{DbUtil.TEXT_PREFIX+"EN",
			"Quality"+DbUtil.LOC_SEP+"EN",
			"Quality"+DbUtil.LOC_SEP+"FR",
			DbUtil.TEXT_PREFIX+"FR"}));
		rs = tm.getFirstPage();
		rs.next(); rs.next(); rs.next();
		assertEquals("Text EN 3", rs.getString(3));
//		assertEquals(0, rs.getInt(4));
//		assertEquals(33, rs.getInt(5));
		assertEquals("Text FR 3", rs.getString(6));
		rs.next();
		assertEquals("Text EN 4", rs.getString(3));
//		assertEquals(0, rs.getInt(4));
//		assertEquals(0, rs.getInt(5));
		assertEquals("Text FR 4", rs.getString(6));
		
		assertEquals(4, repo.getTotalSegmentCount(tmName));
		
		// test pages with the 4 entries
		tm.setPageSize(3);
		tm.setPageMode(PageMode.EDITOR);
		rs = tm.getFirstPage();
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last());
		assertEquals("Text EN 3", rs.getString(3));
		// Next page
		rs = tm.getNextPage();
		assertTrue(rs.next());
		assertEquals("Text EN 3", rs.getString(3)); // Last row of previous page is first of the next
		assertTrue(rs.last());
		assertEquals("Text EN 4", rs.getString(3));
		// Next should return empty set
		rs = tm.getNextPage();
		assertNull(rs);
		
		rs = tm.getPreviousPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last());
		assertEquals("Text EN 3", rs.getString(3));
		// Previous empty
		rs = tm.getPreviousPage();
		assertNull(rs);
		
		tm.setPageSize(3);
		rs = tm.getLastPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 3", rs.getString(3)); // was 2
		assertTrue(rs.last()); // Skip middle item
		assertEquals("Text EN 4", rs.getString(3));
		// Previous
		rs = tm.getPreviousPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last());
		assertEquals("Text EN 3", rs.getString(3));
		// Previous is null
		rs = tm.getPreviousPage();
		assertNull(rs);

		rs = tm.getFirstPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last());
		assertEquals("Text EN 3", rs.getString(3));
		// Next
		rs = tm.getNextPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 3", rs.getString(3));
		assertTrue(rs.last());
		assertEquals("Text EN 4", rs.getString(3));
		// Next is null
		rs = tm.getNextPage();
		assertNull(rs);

		tm.setPageSize(4);
		rs = tm.getLastPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last()); // Skip middle item
		assertEquals("Text EN 4", rs.getString(3));
		// Previous
		rs = tm.getPreviousPage();
		assertNull(rs);
		// First
		rs = tm.getFirstPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last()); // Skip middle item
		assertEquals("Text EN 4", rs.getString(3));
		// Previous
		rs = tm.getNextPage();
		assertNull(rs);

		tm.setPageSize(5);
		rs = tm.getLastPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 1", rs.getString(3));
		assertTrue(rs.last()); // Skip middle item
		assertEquals("Text EN 4", rs.getString(3));
		// Previous
		rs = tm.getPreviousPage();
		assertNull(rs);

		// Test with one entry
		ArrayList<Long> segKeys = new ArrayList<Long>();
		segKeys.add(1L); segKeys.add(2L); segKeys.add(3L);
		tm.deleteSegments(segKeys);
		assertEquals(1, tm.getTotalSegmentCount());
		
		tm.setPageMode(PageMode.EDITOR);
		tm.setPageSize(5);
		rs = tm.getLastPage();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("Text EN 4", rs.getString(3));
		
		
		//==== Check the locales
		
		list = tm.getLocales();
		assertEquals(2, list.size());
		assertEquals("EN", list.get(0));
		assertEquals("FR", list.get(1));
		
		String newTmName = "newName";
		tm.rename(newTmName);
		assertEquals(newTmName, tm.getName());
		assertEquals(newTmName, repo.getTmNames().get(0));
		
		newTmName = "NEWName"; // Different case
		tm.rename(newTmName);
		assertEquals(newTmName, tm.getName());
		assertEquals(newTmName, repo.getTmNames().get(0));

		//=== Deletion
		
		repo.deleteTm(newTmName);
		// There should be no TM
		list = repo.getTmNames();
		assertEquals(0, list.size());
		
		repo.close();
	}

	
	public static void runMultipleTestsStep3 (IRepository repo)
		throws SQLException
	{
		//String tmName = "\u0195\u0222b_ test";
		String tmName = "normalName";
		String localeCode = DbUtil.toOlifantLocaleCode(LocaleId.ENGLISH);
		ITm tm = repo.createTm(tmName, null, localeCode);
		assertEquals(tmName, tm.getName());
		
		Map<String, Object> tuFlds = new HashMap<String, Object>();
		Map<String, Object> segFlds = new HashMap<String, Object>();
		
		String srcFName = DbUtil.TEXT_PREFIX+"EN";
		
		tm.startImport();
		tuFlds.put("F1", "F1 1");
		segFlds.put(srcFName, "Src 1");
		tm.addRecord(-1, tuFlds, segFlds);
		tm.finishImport();
		
		tm.setRecordFields(tm.getAvailableFields());
		tm.setPageMode(PageMode.EDITOR);

		IRecordSet rs = tm.getFirstPage();
		rs.next();
		assertEquals("F1 1", rs.getString(3));
		assertEquals("Src 1", rs.getString(5));
		
		// second row
		tm = repo.openTm(tmName);
		tuFlds.clear();
		tuFlds.put("F1", "F1 2");
		segFlds.clear();
		segFlds.put(srcFName, "Src 2");
		tm.startImport();
		tm.addRecord(-1, tuFlds, segFlds);
		tm.finishImport();

		tm.setRecordFields(tm.getAvailableFields());
		rs = tm.getFirstPage();
		rs.next(); rs.next();
		assertEquals("F1 2", rs.getString(3));
		assertEquals("Src 2", rs.getString(5));
		

		//=== Test addition of locale
		
		tm.addLocale("BG");
		List<String> list = tm.getLocales();
		assertEquals(2, list.size());
		assertEquals("EN", list.get(0));
		assertEquals("BG", list.get(1));
		// Do it a second time: no change
		tm.addLocale("BG");
		list = tm.getLocales();
		assertEquals(2, list.size());
		assertEquals("EN", list.get(0));
		assertEquals("BG", list.get(1));
		
		//=== Test renaming of locale
		
		tm.renameLocale("BG", "ZU");
		list = tm.getLocales();
		assertEquals(2, list.size());
		assertEquals("EN", list.get(0));
		assertEquals("ZU", list.get(1));

		tm.renameLocale("BG", "ZZ"); // BG does not exists: nothing happens
		list = tm.getLocales();
		assertEquals(2, list.size());
		assertEquals("EN", list.get(0));
		assertEquals("ZU", list.get(1));
		
		tm.renameLocale("ZU", "EN"); // EN exists already: nothing happens
		list = tm.getLocales();
		assertEquals(2, list.size());
		assertEquals("EN", list.get(0));
		assertEquals("ZU", list.get(1));
		
		//=== Test deletion of locales
		
		tm.deleteLocale("ZU");
		list = tm.getLocales();
		assertEquals(1, list.size());
		assertEquals("EN", list.get(0));
		tm.deleteLocale("EN"); // Try to delete the last one
		list = tm.getLocales();
		assertEquals(1, list.size()); // It should not be deleted
		assertEquals("EN", list.get(0));
		
		
		repo.close();
	}

	public static void runMultipleTestsStep4 (IRepository repo)
		throws SQLException
	{
		String tmName = "myTm";
		String localeCode = DbUtil.toOlifantLocaleCode(LocaleId.ENGLISH);
		ITm tm = repo.createTm(tmName, null, localeCode);
		assertEquals(tmName, tm.getName());
		
		Map<String, Object> tuFlds = new HashMap<String, Object>();
		Map<String, Object> segFlds = new HashMap<String, Object>();
		String srcFName = DbUtil.TEXT_PREFIX+localeCode;
		tm.startImport();
		tuFlds.put("F1", "F1 1");
		segFlds.put(srcFName, "Src 1");
		tm.addRecord(-1, tuFlds, segFlds);
		tm.finishImport();

		tm.setRecordFields(tm.getAvailableFields());
		tm.setPageMode(PageMode.EDITOR);
		IRecordSet rs = tm.getFirstPage();
		rs.next();
		assertEquals("F1 1", rs.getString(3));
		assertEquals("Src 1", rs.getString(5));

		tm.addField("NEWTUField");
		assertTrue(tm.getAvailableFields().contains("NEWTUField"));
		
		tm.renameField("NEWTUField", "NewNamedField");
		assertFalse(tm.getAvailableFields().contains("NEWTUField"));
		assertTrue(tm.getAvailableFields().contains("NewNamedField"));

		String newFieldName = "NewSegField"+DbUtil.LOC_SEP+localeCode; 
		tm.addField(newFieldName);
		assertTrue(tm.getAvailableFields().contains(newFieldName));

		String newName = "NewName"+DbUtil.LOC_SEP+localeCode;
		tm.renameField(newFieldName, newName);
		assertFalse(tm.getAvailableFields().contains(newFieldName));
		assertTrue(tm.getAvailableFields().contains(newName));
		
		tm.deleteField(newName);
		assertFalse(tm.getAvailableFields().contains(newFieldName));
		
		repo.close();
	}
	
}
