package ambit2.smarts;

import java.util.List;

import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IRingSet;

/**
 * SMARTSProp is a per atom property &
 * 
 * hvnNrrrrrrrrRRRRRRRR extractSMARTSProperties(IAtomContainer mol)
 * setCMLSMARTSProperties(IAtomContainer mol)
 * mol.getAtom(i).setProperty("SMARTSProp", prop);
 * 
 * if already set,
 * 
 * 
 * @author nina
 * 
 */
public class CMLUtilities {
	public static final String SMARTSProp = "SMARTSProp";
	public static final String RingData = "RingData";
	public static final String RingData2 = "RingData2";
	public static final String ExplicitH = "ExplicitH";
	public static final int MAXRINGS = 4; // The maximal number of rings
	int expH[];
	List<int[]> rd;
	List<int[]> rd2;

	/**
	 * Generates attributes and sets atom & bond attributes for every atom as
	 * atom properties
	 * 
	 * @param mol
	 */
	public void setCMLSMARTSProperties(IAtomContainer mol) {

		IRingSet ringSet = Cycles.sssr(mol).toRingSet();

		SmartsParser.setNeighbourData(mol);
		SmartsParser.setValenceData(mol);
		expH = SmartsParser.getExplicitHAtomData(mol);
		rd = SmartsParser.getRindData(mol, ringSet);
		rd2 = SmartsParser.getRindData2(mol, ringSet);
		for (int i = 0; i < mol.getAtomCount(); i++) {
			String prop = getAtomCMLSMARTSProperty(i, mol);
			mol.getAtom(i).setProperty(SMARTSProp, prop);
		}
	}

	/*
	 * The SMARTS atom data is stored as a string in the follwing format:
	 * hvnNrrrrrrrrRRRRRRRR h - unmber of explicit H atoms v - valency n -
	 * neighbours N - number of rings rrrrrrrr - ring data RRRRRRRR - ring data
	 * 2
	 */
	String getAtomCMLSMARTSProperty(int n, IAtomContainer mol) {
		StringBuffer sb = new StringBuffer();
		IAtom atom = mol.getAtom(n);
		sb.append(expH[n]);
		sb.append(atom.getValency());
		sb.append(atom.getFormalNeighbourCount());
		if (rd.get(n) == null)
			sb.append("0");
		else {
			sb.append(rd.get(n).length);
			addData(sb, rd.get(n));
			addData(sb, rd2.get(n));
		}
		return (sb.toString());
	}

	void addZeros(StringBuffer sb, int n) {
		for (int i = 0; i < n; i++)
			sb.append("0");
	}

	void addData(StringBuffer sb, int r[]) {
		if (r == null)
			return;
		// addZeros(sb,MAXRINGS);
		else {
			for (int i = 0; i < r.length; i++) {
				if (r[i] <= 9)
					sb.append("0");
				sb.append(r[i]);
			}
			// addZeros(sb, 2*(MAXRINGS-r.length));
		}
	}

	/**
	 * Retrieves properties (set by {@link setCMLSmartsProperties} Expects
	 * SMARTSProp to be set for each atom !
	 * 
	 * @param mol
	 */
	public void extractSMARTSProperties(IAtomContainer mol) {
		for (int i = 0; i < mol.getAtomCount(); i++) {
			IAtom atom = mol.getAtom(i);
			Object o = atom.getProperty(SMARTSProp);
			if (o != null)
				extractCMLAtomProperties(atom, o.toString());
		}
	}

	/**
	 * Converts per-atom string property into internal structure
	 * 
	 * @param atom
	 * @param CMLProperty
	 */
	public void extractCMLAtomProperties(IAtom atom, String CMLProperty) {
		// hvnNrrrrrrrrRRRRRRRR
		if (CMLProperty.length() < 4)
			return;

		atom.setProperty(CMLUtilities.ExplicitH,
				new Integer(extractInteger(CMLProperty, 0, 1)));
		atom.setValency(extractInteger(CMLProperty, 1, 1));
		atom.setFormalNeighbourCount(extractInteger(CMLProperty, 2, 1));
		int numRings = extractInteger(CMLProperty, 3, 1);
		if (CMLProperty.length() < (4 + 4 * numRings))
			return;
		int r[] = new int[numRings];
		int r2[] = new int[numRings];
		int pos = 4;
		for (int i = 0; i < numRings; i++) {
			r[i] = extractInteger(CMLProperty, pos, 2);
			pos += 2;
		}
		for (int i = 0; i < numRings; i++) {
			r2[i] = extractInteger(CMLProperty, pos, 2);
			pos += 2;
		}
		atom.setProperty(CMLUtilities.RingData, r);
		atom.setProperty(CMLUtilities.RingData2, r2);
	}

	int extractInteger(String s, int pos, int lenght) {
		Integer i = new Integer(s.substring(pos, pos + lenght));
		return (i.intValue());
	}

}
