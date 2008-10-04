package net.sf.okapi.applications.rainbow.utilities.alignment;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

public class DbStore {

	//TODO
	public static final int  GROUP_PK       = 0;
	public static final int  GROUP_NAME     = 0;
	public static final int  GROUP_TYPE     = 0;
	public static final int  GROUP_PREWS    = 0;
	public static final int  GROUP_TRANS    = 0;

	public static final String TBLNAME_SOURCE    = "Source";

	public static final int  SOURCE_KEY          = 0;
	public static final String SOURCE_NKEY       = "Key";
	public static final int  SOURCE_GKEY         = 1;
	public static final String SOURCE_NTMP       = "Tmp";
	public static final int  SOURCE_TMP          = 2;
	public static final String SOURCE_NGKEY      = "GKey";
	public static final int  SOURCE_XKEY         = 3;
	public static final String SOURCE_NXKEY      = "XKey";
	public static final int  SOURCE_SEGKEY       = 4;
	public static final String SOURCE_NSEGKEY    = "SegKey";
	public static final int  SOURCE_NAME         = 5;
	public static final String SOURCE_NNAME      = "Name";
	public static final int  SOURCE_TYPE         = 6;
	public static final String SOURCE_NTYPE      = "Type";
	public static final int  SOURCE_TEXT         = 7;
	public static final String SOURCE_NTEXT      = "Text";
	public static final int  SOURCE_CODES        = 8;
	public static final String SOURCE_NCODES     = "Codes";
	public static final int  SOURCE_PREWS        = 9;
	public static final String SOURCE_NPREWS     = "PreWS";
	public static final int  SOURCE_TRANS        = 10;
	public static final String SOURCE_NTRANS     = "Trans";
	
	//TODO
	public static final int  TARGET_PK      = 0;
	public static final int  TARGET_XKEY    = 0;
	public static final int  TARGET_SKEY    = 0;
	public static final int  TARGET_TEXT    = 0;
	public static final int  TARGET_CODES   = 0;
	
	private static final String DATAFILE_EXT = ".data.db";

	private Connection  conn = null;


	public DbStore () {
		try {
			// Initialize the driver
			Class.forName("org.h2.Driver");
		}
		catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void close () {
		try {
			if ( conn != null ) {
				conn.close();
				conn = null;
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void create (String folder,
		String dbName,
		boolean deleteExistingDB)
	{
		Statement stm = null;
		try {
			close();
			String path = folder+File.separatorChar+dbName;
			if ( (new File(path+DATAFILE_EXT)).exists() ) {
				if ( !deleteExistingDB ) return;
				// Else: delete the directory content
				Util.deleteDirectory(folder, false);
			}
			else Util.createDirectories(path);
			
			// Open the connection, this creates the DB if none exists
			conn = DriverManager.getConnection("jdbc:h2:"+path, "sa", "");
	
			// Create the source table
			stm = conn.createStatement();
			stm.execute("CREATE TABLE " + TBLNAME_SOURCE + " ("
				+ SOURCE_NKEY + " INTEGER IDENTITY PRIMARY KEY,"
				+ SOURCE_NTMP + " INTEGER,"
				+ SOURCE_NGKEY + " INTEGER,"
				+ SOURCE_NXKEY + " VARCHAR,"
				+ SOURCE_NSEGKEY + " INTEGER,"
				+ SOURCE_NNAME + " VARCHAR,"
				+ SOURCE_NTYPE + " VARCHAR,"
				+ SOURCE_NTEXT + " VARCHAR,"
				+ SOURCE_NCODES + " VARCHAR,"
				+ SOURCE_NPREWS + " BOOLEAN,"
				+ SOURCE_NTRANS + " BOOLEAN"
				+ ")");
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void open (String path) {
		try {
			close();
			if ( !(new File(path+DATAFILE_EXT)).exists() ) return;
			conn = DriverManager.getConnection("jdbc:h2:"+path, "sa", "");
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public int getTextUnitCount () {
		Statement stm = null;
		try {
			stm = conn.createStatement();
			ResultSet result = stm.executeQuery("SELECT COUNT(" + SOURCE_NKEY + ") FROM " + TBLNAME_SOURCE);
			if ( !result.first() ) return 0;
			return result.getInt(1);
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( stm != null ) {
					stm.close();
					stm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public int addSourceTextUnit (TextUnit tu,
		int gKey)
	{
		int count = 0;
		PreparedStatement pstm = null;
		try {
			TextContainer tc = tu.getSourceContent();
			//TODO: make this pstm class-level
			pstm = conn.prepareStatement(String.format("INSERT INTO %s (%s,%s,%s,%s,%s,%s,%s,%s) VALUES(?,?,?,?,?,?,?,?);",
				TBLNAME_SOURCE, SOURCE_NTMP, SOURCE_NGKEY, SOURCE_NXKEY, SOURCE_NSEGKEY, SOURCE_NNAME,
				SOURCE_NTYPE, SOURCE_NTEXT, SOURCE_NCODES));
			
			// Store the main content
			pstm.setInt(1, 0);
			pstm.setInt(2, gKey);
			pstm.setString(3, tu.getID());
			pstm.setInt(4, 0);
			pstm.setString(5, tu.getName());
			pstm.setString(6, tu.getType());
			pstm.setString(7, tc.getCodedText());
			pstm.setString(8, Code.codesToString(tc.getCodes()));
			pstm.execute();
			count++;
			
			// Store the segments if needed
			if ( tc.isSegmented() ) {
				int i = 1;
				for ( TextFragment tf : tc.getSegments() ) {
					pstm.setInt(1, 0);
					pstm.setInt(2, gKey);
					pstm.setString(3, tu.getID());
					pstm.setInt(4, i);
					pstm.setString(5, tu.getName());
					pstm.setString(6, tu.getType());
					pstm.setString(7, tf.getCodedText());
					pstm.setString(8, Code.codesToString(tf.getCodes()));
					pstm.execute();
					count++;
					i++;
				}
			}
			return count;
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( pstm != null ) {
					pstm.close();
					pstm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public TextContainer findEntry (String name,
		boolean includeSegments)
	{
		PreparedStatement pstm = null;
		try {
			//TODO: make pstm class-level objects, reset for each connection
			pstm = conn.prepareStatement(String.format("SELECT %s,%s FROM %s WHERE %s=? ORDER BY %s",
				SOURCE_NTEXT, SOURCE_NCODES, TBLNAME_SOURCE, SOURCE_NNAME, SOURCE_NSEGKEY));
			pstm.setString(1, name);
			ResultSet result = pstm.executeQuery();
			if ( !result.first() ) return null;
			TextContainer tc = new TextContainer();
			tc.setCodedText(result.getString(1),
				Code.stringToCodes(result.getString(2)), false);
			// Return now if the segments are not requested
			if ( !includeSegments ) return tc;
			
			// Build the segments
			if ( !tc.hasCode() || !result.next() ) return tc; // No segments
			// Create the new list
			ArrayList<TextFragment> list = new ArrayList<TextFragment>();
			tc.setSegments(list);

			// Add the first segment to it
			list.add(new TextFragment(result.getString(1),
				Code.stringToCodes(result.getString(2))));
			// Add the other segments
			while ( result.next() ) {
				list.add(new TextFragment(result.getString(1),
					Code.stringToCodes(result.getString(2))));
			}
			return tc;
		}
		catch ( SQLException e ) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if ( pstm != null ) {
					pstm.close();
					pstm = null;
				}
			}
			catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
