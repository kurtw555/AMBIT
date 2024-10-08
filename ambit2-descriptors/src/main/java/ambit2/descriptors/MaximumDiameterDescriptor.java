/*
Copyright (C) 2005-2006  

Contact: nina@acad.bg

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation; either version 2.1
of the License, or (at your option) any later version.
All we ask is that proper credit is given for our work, which includes
- but is not limited to - adding the above copyright notice to the beginning
of your source code files, and to any copyright notice that you may distribute
with programs based on this work.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA
*/

package ambit2.descriptors;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;

import ambit2.base.data.Property;

/**
 * Maximum cross sectional diameter.  Returns {@link org.openscience.cdk.qsar.result.DoubleResult}[0]. 
 * @author Nina Jeliazkova nina@acad.bg
 * <b>Modified</b> Aug 31, 2006
 */
public class MaximumDiameterDescriptor extends SpherosityDescriptor {

	public DescriptorSpecification getSpecification() {
        return new DescriptorSpecification(
        	String.format(Property.AMBIT_DESCRIPTORS_ONTOLOGY,"MoleculeLength"),
		    this.getClass().getName(),
		    "$Id: MaximumDiameterDescriptor.java,v 0.2 2006/06/29 14:09:00 Nina Jeliazkova Exp $",
		    "http://ambit.sourceforge.net");
    };    	
    public DescriptorValue calculate(IAtomContainer container)  {
    	try {
	        DescriptorValue value =  getSizeDescriptorResult(container);
	        IDescriptorResult result = value.getValue();
	        //[0] is max length, [1] is max diameter [2] is min diameter - =0 if planar 
	        DoubleArrayResult eval = ((DoubleArrayResult) result);
	        
	        return new DescriptorValue(getSpecification(), getParameterNames(), 
	                getParameters(), new DoubleResult( eval.get(0)),
	                getDescriptorNames());    
    	} catch (Exception x) {
	        return new DescriptorValue(getSpecification(), getParameterNames(), 
	                getParameters(), null,
	                getDescriptorNames(),x);
    	}
    }
    @Override
    public String[] getDescriptorNames() {
    	 return new String[] {CrossSectionalDiameterDescriptor.MAX_DIAMETER};
    }
    public String toString() {
    	return "Molecule Length";
    }
}


