/* RetrieveTest.java
 * Author: nina
 * Date: Jan 7, 2009
 * Revision: 0.1 
 * 
 * Copyright (C) 2005-2009  Ideaconsult Ltd.
 * 
 * Contact: nina
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package ambit2.db.readers.test;

import java.sql.ResultSet;

import net.idea.modbcum.i.IQueryRetrieval;
import net.idea.modbcum.i.IRetrieval;

import org.dbunit.database.IDatabaseConnection;
import org.junit.Before;
import org.junit.Test;

import ambit2.db.processors.test.DbUnitTest;
import ambit2.db.search.QueryExecutor;




public abstract class RetrieveTest<T> extends DbUnitTest {
	protected IRetrieval<T> query;
	protected abstract IQueryRetrieval<T> createQuery();
	
	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		query = createQuery();
	}
	
	protected abstract String getTestDatabase();

	
	@Test
	public void testRows() throws Exception {
		setUpDatabaseFromResource(getTestDatabase());
		IDatabaseConnection c = getConnection();
		QueryExecutor qx = new QueryExecutor();
		ResultSet rows = null;
		try {
			qx.setConnection(c.getConnection());
			IQueryRetrieval<T>  query = createQuery();
			rows = qx.process(query);
			verifyRows(query,rows);
		} catch (Exception x) {
			throw x;
		} finally {
			if (rows!=null) qx.closeResults(rows);
			qx.close();
			c.close();
		}
	}
	protected void verifyRows(IQueryRetrieval<T> query,ResultSet rs) throws Exception {
		throw new Exception("Not implemented");
	}

}
