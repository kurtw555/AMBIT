/* UpdateDataset.java
 * Author: nina
 * Date: Mar 28, 2009
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

package ambit2.db.update.bundle;

import java.util.ArrayList;
import java.util.List;

import net.idea.modbcum.i.exceptions.AmbitException;
import net.idea.modbcum.i.query.QueryParam;
import ambit2.base.data.substance.SubstanceEndpointsBundle;
import ambit2.db.update.AbstractObjectUpdate;
import ambit2.db.update.dataset.UpdateDataset;

public class UpdateBundle extends AbstractObjectUpdate<SubstanceEndpointsBundle> {
	private static final String update_sql = 

		"update bundle b, catalog_references cr set %s where b.idreference=cr.idreference and idbundle=?"
	;
	
	private static final String _license = "b.licenseURI=?";
	
	private static final String _name = "b.name=?";
	
	private static final String _rightsHolder = "b.rightsHolder=?";
	
	private static final String _maintainer = "b.maintainer=?";
	
	private static final String _description = "b.description=?";
	
	private static final String _status = "b.published_status=?";
	
	private static final String _seeAlso = "cr.url=?";
	
	private static final String _source = "cr.title=?";
	
	public enum _published_status  {draft,archived,published};
	
	public UpdateBundle(SubstanceEndpointsBundle dataset) {
		super(dataset);
	}
	public UpdateBundle() {
		this(null);
	}	
	
	public List<QueryParam> getParameters(int index) throws AmbitException {
		List<QueryParam> params3 = new ArrayList<QueryParam>();
		if (getObject().getName()!=null)
			params3.add(new QueryParam<String>(String.class, truncate(getObject().getName(),255)));

		if (getObject().getLicenseURI()!=null)
			params3.add(new QueryParam<String>(String.class, truncate(getObject().getLicenseURI(),128)));
		
		if (getObject().getrightsHolder()!=null)
			params3.add(new QueryParam<String>(String.class, truncate(getObject().getrightsHolder(),128)));

		if (getObject().getMaintainer()!=null)
			params3.add(new QueryParam<String>(String.class, truncate(getObject().getMaintainer(),128)));
		
		if (getObject().getDescription()!=null)
			params3.add(new QueryParam<String>(String.class, getObject().getDescription()));
		
		if (getObject().getTitle()!=null)
			params3.add(new QueryParam<String>(String.class,getObject().getTitle()));		
		
		if (getObject().getURL()!=null)
			params3.add(new QueryParam<String>(String.class,getObject().getURL()));	
		
		if (getObject().getStatus()!=null) {
		    try {
			params3.add(new QueryParam<String>(String.class, _published_status.valueOf(getObject().getStatus().trim()).name()));
		    } catch (Exception x) {
			
		    }
		}	
		
		if (params3.size()==0)
			throw new AmbitException(UpdateDataset.MSG_EMPTY);
		params3.add(new QueryParam<Integer>(Integer.class, getObject().getID()));
		
		return params3;
		
	}

	public String[] getSQL() throws AmbitException {
		StringBuilder b = new StringBuilder();
		int i=0;
		if (getObject().getName()!=null) {
			b.append(_name.toString());
			i++;
		}
		if (getObject().getLicenseURI()!=null) {
			b.append(i>0?",":"");
			b.append(_license);
			i++;
		}

		if (getObject().getrightsHolder()!=null) {
			b.append(i>0?",":"");
			b.append(_rightsHolder);
			i++;
		}
		
		if (getObject().getMaintainer()!=null) {
			b.append(i>0?",":"");
			b.append(_maintainer);
			i++;
		}
			
		if (getObject().getDescription()!=null) {
			b.append(i>0?",":"");
			b.append(_description);
			i++;
		}
				
		if (getObject().getTitle()!=null) {
			b.append(i>0?",":"");
			b.append(_source);
			i++;
		}
		
		if (getObject().getURL()!=null) {
			b.append(i>0?",":"");
			b.append(_seeAlso);
			i++;
		}
		if (getObject().getStatus()!=null) {
		    try {
			_published_status.valueOf(getObject().getStatus().trim());
			b.append(i>0?",":"");
			b.append(_status);
			i++;
		    } catch (Exception x) {
		    }
		}			
		if (i>0)
			return new String[] {String.format(update_sql,b.toString())};
		else
			throw new AmbitException(UpdateDataset.MSG_EMPTY);
	}
	public void setID(int index, int id) {
		
	}
}
