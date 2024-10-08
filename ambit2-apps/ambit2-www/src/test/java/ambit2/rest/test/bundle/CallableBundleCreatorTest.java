package ambit2.rest.test.bundle;

import junit.framework.Assert;
import net.idea.modbcum.i.IQueryRetrieval;
import net.idea.restnet.db.CreateDatabaseProcessor;
import net.idea.restnet.db.test.DbUnitTest;
import net.idea.restnet.i.task.TaskResult;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.ITable;
import org.junit.Test;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.security.User;

import ambit2.base.data.ISourceDataset;
import ambit2.base.data.substance.SubstanceEndpointsBundle;
import ambit2.rest.bundle.CallableBundleCreator;
import ambit2.rest.bundle.CallableBundleVersionCreator;
import ambit2.rest.dataset.DatasetURIReporter;
import ambit2.rest.test.CreateAmbitDatabaseProcessor;

public class CallableBundleCreatorTest extends DbUnitTest {

    @Test
    public void testCreateBundle() throws Exception {
	setUpDatabase(dbFile);
	IDatabaseConnection c = getConnection();
	IDatabaseConnection c1 = getConnection();

	Form form = new Form();
	form.add(ISourceDataset.fields.title.name(), "title");
	form.add(ISourceDataset.fields.source.name(), "source");
	form.add(ISourceDataset.fields.license.name(), "license");
	form.add(ISourceDataset.fields.seeAlso.name(), "seeAlso");
	form.add(ISourceDataset.fields.maintainer.name(), "maintainer");
	form.add(ISourceDataset.fields.rightsHolder.name(), "rightsHolder");
	form.add("description", "description");
	form.add(ISourceDataset.fields.stars.name(), "9");

	try {
	    
	    DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle> reporter = new DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle>(
		    new Reference("http://localhost"));
	    CallableBundleCreator callable = new CallableBundleCreator(null, reporter, Method.POST, form,
		    c.getConnection(), new User("testuser"), null);
	    TaskResult task = callable.call();
	    ITable table = c1.createQueryTable("EXPECTED",
		    String.format("SELECT idbundle,description,user_name,title,url from bundle join catalog_references using(idreference) where name='title'"));
	    Assert.assertEquals(1, table.getRowCount());
	    Assert.assertEquals("description", table.getValue(0, "description"));
	    Assert.assertEquals("testuser", table.getValue(0, "user_name"));
	    Assert.assertEquals("source", table.getValue(0, "title"));
	    Assert.assertEquals("seeAlso", table.getValue(0, "url"));

	} catch (Exception x) {
	    throw x;
	} finally {
	    try {
		c.close();
	    } catch (Exception x) {
	    }
	    try {
		c1.close();
	    } catch (Exception x) {
	    }
	}
    }

    @Test
    public void testCreateBundleCopy() throws Exception {
	setUpDatabase(dbFile);
	IDatabaseConnection c = getConnection();
	IDatabaseConnection c1 = getConnection();

	Form form = new Form();
	form.add("bundle_uri","http://localhost/bundle/1");

	try {
	    DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle> reporter = new DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle>(
		    new Reference("http://localhost"));
	    CallableBundleCreator callable = new CallableBundleCreator(null, reporter, Method.POST, form,
		    c.getConnection(), new User("testuser"), null);
	    TaskResult task = callable.call();
	    ITable table = c1.createQueryTable("EXPECTED",
		    String.format("SELECT idbundle,licenseURI,user_name from bundle where name='Assessment' order by idbundle asc"));
	    Assert.assertEquals(2, table.getRowCount());
	    Assert.assertEquals( table.getValue(1, "licenseURI"), table.getValue(0, "licenseURI"));
	    Assert.assertEquals("testuser", table.getValue(1, "user_name"));

	} catch (Exception x) {
	    throw x;
	} finally {
	    try {
		c.close();
	    } catch (Exception x) {
	    }
	    try {
		c1.close();
	    } catch (Exception x) {
	    }
	}
    }
    @Test
    public void testDeleteBundle() throws Exception {
	setUpDatabase(dbFile);
	IDatabaseConnection c = getConnection();
	IDatabaseConnection c1 = getConnection();

	try {
	    ITable table = c1.createQueryTable("EXPECTED",
		    String.format("SELECT idbundle from bundle where idbundle=1"));
	    Assert.assertEquals(1, table.getRowCount());

	    SubstanceEndpointsBundle bundle = new SubstanceEndpointsBundle(1);
	    DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle> reporter = new DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle>(
		    new Reference("http://localhost"));
	    CallableBundleCreator callable = new CallableBundleCreator(bundle, reporter, Method.DELETE, new Form(),
		    c.getConnection(),new User("testuser"), null);
	    TaskResult task = callable.call();
	    table = c1.createQueryTable("EXPECTED", String.format("SELECT idbundle,user_name from bundle where idbundle=1"));
	    Assert.assertEquals(0, table.getRowCount());

	} catch (Exception x) {
	    throw x;
	} finally {
	    try {
		c.close();
	    } catch (Exception x) {
	    }
	    try {
		c1.close();
	    } catch (Exception x) {
	    }
	}
    }

    @Test
    public void testUpdateBundle() throws Exception {
	setUpDatabase(dbFile);
	IDatabaseConnection c = getConnection();
	IDatabaseConnection c1 = getConnection();

	Form form = new Form();
	form.add(ISourceDataset.fields.title.name(), "title");
	//form.add(ISourceDataset.fields.source.name(), "source");
	form.add(ISourceDataset.fields.license.name(), "license");
	form.add(ISourceDataset.fields.maintainer.name(), "maintainer");
	form.add(ISourceDataset.fields.rightsHolder.name(), "rightsHolder");
	form.add(ISourceDataset.fields.seeAlso.name(), "NEW VALUE");
	form.add("description", "description");
	form.add(ISourceDataset.fields.stars.name(), "9");

	try {
	    ITable table = c1.createQueryTable("EXPECTED",
		    String.format("SELECT idbundle,updated from bundle where idbundle=1"));
	    Assert.assertEquals(1, table.getRowCount());
	    Object updated = table.getValue(0, "updated");

	    SubstanceEndpointsBundle bundle = new SubstanceEndpointsBundle(1);
	    DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle> reporter = new DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle>(
		    new Reference("http://localhost"));
	    CallableBundleCreator callable = new CallableBundleCreator(bundle, reporter, Method.PUT, form,
		    c.getConnection(), new User("testuser"), null);
	    TaskResult task = callable.call();
	    table = c1
		    .createQueryTable(
			    "EXPECTED",
			    String.format("SELECT idbundle,name,licenseURI,rightsHolder,maintainer,stars,title,url,description,updated,user_name,url,title from bundle join catalog_references using(idreference) where idbundle=1"));
	    Assert.assertEquals(1, table.getRowCount());
	    Assert.assertEquals("title", table.getValue(0, "name"));
	    Assert.assertEquals("maintainer", table.getValue(0, "maintainer"));
	    Assert.assertEquals("rightsHolder", table.getValue(0, "rightsHolder"));
	    Assert.assertEquals("license", table.getValue(0, "licenseURI"));
	    Assert.assertEquals("description", table.getValue(0, "description"));
	    Assert.assertNotSame(updated,table.getValue(0, "updated"));
	    Assert.assertEquals("guest", table.getValue(0, "user_name"));
	    Assert.assertEquals("NEW VALUE",table.getValue(0,"url"));
	    //Assert.assertEquals("source",table.getValue(0,"title"));

	} catch (Exception x) {
	    throw x;
	} finally {
	    try {
		c.close();
	    } catch (Exception x) {
	    }
	    try {
		c1.close();
	    } catch (Exception x) {
	    }
	}
    }
    
    @Test
    public void testUpdateBundleStatus() throws Exception {
	setUpDatabase(dbFile);
	IDatabaseConnection c = getConnection();
	IDatabaseConnection c1 = getConnection();

	Form form = new Form();
	form.add(ISourceDataset.fields.status.name(), "published");

	try {
	    ITable table = c1.createQueryTable("EXPECTED",
		    String.format("SELECT idbundle,published_status,name,licenseURI,rightsHolder,maintainer,stars,title,url,description,updated from bundle join catalog_references using(idreference) where idbundle=1"));
	    Assert.assertEquals(1, table.getRowCount());
	    Assert.assertEquals("draft", table.getValue(0, "published_status"));
	    Assert.assertEquals("Assessment", table.getValue(0, "name"));
	    Assert.assertEquals("ABC", table.getValue(0, "maintainer"));
	    Assert.assertEquals("xyz", table.getValue(0, "rightsHolder"));
	    Assert.assertEquals("http://creativecommons.org/publicdomain/zero/1.0/", table.getValue(0, "licenseURI"));
	    Assert.assertNull(table.getValue(0, "description"));
	    Object updated = table.getValue(0, "updated");
	    SubstanceEndpointsBundle bundle = new SubstanceEndpointsBundle(1);
	    DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle> reporter = new DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle>(
		    new Reference("http://localhost"));
	    CallableBundleCreator callable = new CallableBundleCreator(bundle, reporter, Method.PUT, form,
		    c.getConnection(), new User("testuser"),null);
	    TaskResult task = callable.call();
	    table = c1
		    .createQueryTable(
			    "EXPECTED",
			    String.format("SELECT idbundle,published_status,name,licenseURI,rightsHolder,maintainer,stars,title,url,description,updated,user_name from bundle join catalog_references using(idreference) where idbundle=1"));
	    Assert.assertEquals(1, table.getRowCount());
	    Assert.assertEquals("published", table.getValue(0, "published_status"));
	    Assert.assertEquals("Assessment", table.getValue(0, "name"));
	    Assert.assertEquals("ABC", table.getValue(0, "maintainer"));
	    Assert.assertEquals("xyz", table.getValue(0, "rightsHolder"));
	    Assert.assertEquals("http://creativecommons.org/publicdomain/zero/1.0/", table.getValue(0, "licenseURI"));
	    Assert.assertNull(table.getValue(0, "description"));
	    Assert.assertNotSame(updated,table.getValue(0, "updated"));
	    Assert.assertEquals("guest", table.getValue(0, "user_name"));
	    // Assert.assertEquals("url",table.getValue(0,"url"));
	    // Assert.assertEquals("source",table.getValue(0,"title"));
	    
	} catch (Exception x) {
	    throw x;
	} finally {
	    try {
		c.close();
	    } catch (Exception x) {
	    }
	    try {
		c1.close();
	    } catch (Exception x) {
	    }
	}
    }    

    @Test
    public void testCreateBundleVersion() throws Exception {
	setUpDatabase(dbFile);
	IDatabaseConnection c = getConnection();
	IDatabaseConnection c1 = getConnection();

	Form form = new Form();

	try {
	    SubstanceEndpointsBundle bundle = new SubstanceEndpointsBundle(1);
	    DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle> reporter = new DatasetURIReporter<IQueryRetrieval<SubstanceEndpointsBundle>, SubstanceEndpointsBundle>(
		    new Reference("http://localhost"));
	    CallableBundleVersionCreator callable = new CallableBundleVersionCreator(bundle, reporter, Method.POST,
		    form, c.getConnection(), "");
	    TaskResult task = callable.call();
	    System.out.println(task);
	    ITable table = c1.createQueryTable("EXPECTED",
		    String.format("SELECT idbundle,version,hex(bundle_number) bn from bundle where bundle_number in (select bundle_number from bundle where idbundle=1) order by idbundle"));
	    Assert.assertEquals(2, table.getRowCount());

	} catch (Exception x) {
	    throw x;
	} finally {
	    try {
		c.close();
	    } catch (Exception x) {
	    }
	    try {
		c1.close();
	    } catch (Exception x) {
	    }
	}
    }

    protected String dbFile = "src/test/resources/descriptors-datasets.xml";

    @Override
    protected CreateDatabaseProcessor getDBCreateProcessor() {
	return new CreateAmbitDatabaseProcessor();
    }

    @Override
    public String getDBTables() {
	return "src/test/resources/tables.xml";
    }

    @Override
    protected String getConfig() {
	return "ambit2/rest/config/ambit2.pref";
    }

}
