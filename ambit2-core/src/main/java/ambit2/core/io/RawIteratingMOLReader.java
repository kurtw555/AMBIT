package ambit2.core.io;

import java.io.Reader;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.formats.IResourceFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;

import ambit2.base.data.StructureRecord;
import ambit2.base.interfaces.IStructureRecord;

public class RawIteratingMOLReader extends RawIteratingReader<IStructureRecord> {
	protected final StructureRecord r = new StructureRecord();
	public RawIteratingMOLReader(Reader in) throws CDKException {
		super(in);
	}

	@Override
	public boolean isEndOfRecord(String line) {
		return (line == null) || ("M  END".equals(line.trim()));
	}
	@Override
	protected boolean acceptLastRecord() {
		return recordBuffer!=null;
	}
	@Override
	public IResourceFormat getFormat() {
	    return MDLV2000Format.getInstance();
	}
    @Override
    public IStructureRecord nextRecord() {
        Object o = next();
        if (o instanceof IStructureRecord) return (IStructureRecord)o;
        else {
        	StructureRecord r = new StructureRecord(-1,-1,o.toString(),"SDF");
        	r.setReference(reference);
        	return r;
        }
    }
    @Override
    public Object next() {
    	Object o = super.next();
		r.setIdchemical(-1);
		r.setIdstructure(-1);
		r.setFormat("SDF");
		r.setContent(o.toString());
		r.setReference(getReference());
		return r;    	
    }
}
