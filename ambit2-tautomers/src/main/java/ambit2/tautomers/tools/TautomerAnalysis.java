package ambit2.tautomers.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.IChemObjectReader.Mode;
import org.openscience.cdk.io.IChemObjectReaderErrorHandler;
import org.openscience.cdk.io.iterator.IIteratingChemObjectReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import ambit2.base.exceptions.AmbitIOException;
import ambit2.core.filter.MoleculeFilter;
import ambit2.core.helper.CDKHueckelAromaticityDetector;
import ambit2.core.io.FileInputState;
import ambit2.core.io.InteractiveIteratingMDLReader;
import ambit2.smarts.SmartsHelper;
import ambit2.tautomers.Rule;
import ambit2.tautomers.TautomerConst;
import ambit2.tautomers.TautomerManager;
import ambit2.tautomers.TautomerUtils;
import ambit2.tautomers.TautomerUtils.TautomerPair;



public class TautomerAnalysis 
{
	public static enum Task {
		PRINT_SMILES, GEN_RULE_PAIR, CALC_TAUTOMERS_ALL_RANKS;
	}
	
	private final static Logger logger = Logger.getLogger(TautomerAnalysis.class.getName());
	
	//Configuration variables
	public String filePath = "";
	public String outFilePath = null; //If null it is treated as filePath
	public String inputFileName = null;
	public String outputFileName = null;
	public String outFilePrefix = "out";
	public String outFileType = "csv";
	public String molFilterString = null;
	public Task task = Task.PRINT_SMILES; 
	public boolean FlagFilter = true;
	
	
	//Work variables
	protected TautomerManager tman = new TautomerManager();

	protected MoleculeFilter molecularFilter = null;
	protected int records_read = 0;
	protected int records_processed = 0;
	protected int records_error = 0;
	protected FileWriter outWriters[] = null;
	protected File outFiles[] = null;
	
	
	protected ArrayList<String> fileRuleNames = new ArrayList<String>();
	public String sep = ",";
	
	/**
	 * 
	 * @param molFilterString expression which defines the filter e.g. "#Mol=[1,100];NA=[1,10]"
	 * @throws Exception
	 */
	public void setMoleculeFilter(String molFilterString) throws Exception
	{
		this.molFilterString = molFilterString;
		try {
			MoleculeFilter filter = MoleculeFilter.parseFromCommandLineString(molFilterString);
			molecularFilter = filter;
		} catch (Exception x) {
			throw new Exception("Incorrect molecule filter: " + x.getMessage());
		}
	}
	
	/**
	 * 
	 * @return number of read structure records
	 * @throws Exception
	 */
	public int process() throws Exception 
	{	
		records_read = 0;
		records_processed = 0;
		records_error = 0;
		
		File file = new File(filePath + inputFileName);
		if (!file.exists()) 
			throw new FileNotFoundException(file.getAbsolutePath());
		InputStream in = new FileInputStream(file);
		IIteratingChemObjectReader<IAtomContainer> reader = null;
		
		try 
		{
			reader = getReader(in,file.getName());
			logger.log(Level.INFO, String.format("Reading %s",file.getAbsoluteFile()));
			//LOGGER.log(Level.INFO, String.format("Writing %s tautomer(s)",all?"all":"best"));
			
			createWriters();
			
			while (reader.hasNext()) 
			{	
				IAtomContainer molecule  = reader.next();
				records_read++;
				if (molecule==null) {
					records_error++;
					continue;
				}
				
				
				boolean FlagUseMolFilterAfterProcessing = false; 
				
				if (molecularFilter != null)
				{
					if (molecularFilter.isAromaticityInfoNeeded())
						FlagUseMolFilterAfterProcessing = true;
					else
					{	
						if (!molecularFilter.useMolecule(molecule, records_read))
							continue;
					}	
				}
				
				
				try {
					
					try {
						AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
						CDKHueckelAromaticityDetector.detectAromaticity(molecule);
						//implicit H count is NULL if read from InChI ...
						molecule = AtomContainerManipulator.removeHydrogens(molecule);
						CDKHydrogenAdder.getInstance(molecule.getBuilder()).addImplicitHydrogens(molecule);
						boolean aromatic = false;
						for (IBond bond : molecule.bonds()) if (bond.getFlag(CDKConstants.ISAROMATIC)) {aromatic = true; break;}
						if (aromatic)
							Kekulization.kekulize(molecule);
					} catch (Exception x) {
						logger.log(Level.WARNING, String.format("[Record %d] Error %s\t%s", records_read, file.getAbsoluteFile(), x.getMessage()));
					}
					
					
					if (FlagUseMolFilterAfterProcessing)
						if (!molecularFilter.useMolecule(molecule, records_read))
							continue;
					
					
					doTask(molecule);
					
					
					records_processed++;
				} 
				catch (Exception x) {
					records_error++;
					logger.log(Level.SEVERE, String.format("[Record %d] Error %s\n", records_read, file.getAbsoluteFile()), x);
				}
			}//while
		} catch (Exception x1) {
			logger.log(Level.SEVERE, String.format("[Record %d] Error %s\n", records_read, file.getAbsoluteFile()), x1);
			
		} finally {
			try { reader.close(); } catch (Exception x) {}
			
			closeWriters();

		}
		
		logger.log(Level.INFO, String.format("[Records read/processed/error %d/%d/%d] %s", 
						records_read,records_processed,records_error,file.getAbsoluteFile()));
		
		return records_read;
	}
	
	
	protected IIteratingChemObjectReader<IAtomContainer> getReader(InputStream in, String extension) throws CDKException, AmbitIOException {
		FileInputState instate = new FileInputState();
		IIteratingChemObjectReader<IAtomContainer> reader ;
		if (extension.endsWith(FileInputState._FILE_TYPE.SDF_INDEX.getExtension())) {
			reader = new InteractiveIteratingMDLReader(in,SilentChemObjectBuilder.getInstance());
			((InteractiveIteratingMDLReader) reader).setSkip(true);
		} else reader = instate.getReader(in,extension);
		
		reader.setReaderMode(Mode.RELAXED);
		reader.setErrorHandler(new IChemObjectReaderErrorHandler() {
			
			@Override
			public void handleError(String message, int row, int colStart, int colEnd,
					Exception exception) {
				exception.printStackTrace();
			}
			
			@Override
			public void handleError(String message, int row, int colStart, int colEnd) {
				System.out.println(message);
			}
			
			@Override
			public void handleError(String message, Exception exception) {
				exception.printStackTrace();				
			}
			
			@Override
			public void handleError(String message) {
				System.out.println(message);
			}
		});
		return reader;
	}	
	
	
	protected void createWriters() throws Exception
	{
		switch (task)
		{
		case PRINT_SMILES:
			//no writer;
			outWriters = null;
			break;
			
		case GEN_RULE_PAIR:
			createRulePairsWriters();
			break;
			
		case CALC_TAUTOMERS_ALL_RANKS:
			createOutWriter();
			break;
		}
	}
	
	protected FileWriter createWriter(String fname) throws Exception
	{
		FileWriter writer = null;
		try {
			writer = new FileWriter(fname);
			
		}catch (Exception x) {
			//in case smth's wrong with the writer file, close it and throw an error
			try {writer.close(); } catch (Exception xx) {}
			throw x;
		} finally { }
		
		return writer;
	}
	
	protected void createRulePairsWriters() throws Exception
	{
		String path = outFilePath;
		if (path == null)
			path = filePath;
		
		int nRules = tman.getKnowledgeBase().rules.size();
		outWriters = new FileWriter[nRules];
		outFiles = new File[nRules];
		for (int i = 0; i < nRules; i++)
		{
			Rule rule = tman.getKnowledgeBase().rules.get(i);
			//System.out.println(rule.name + "  --> " + getFileRuleName(rule.name));
			outFiles[i] = new File(path + outFilePrefix + "rule" + (i+1) +"-"+ getFileRuleName(rule.name)+"." + outFileType);
			outWriters[i] = new FileWriter(outFiles[i]);
		}
	}
	
	protected void createOutWriter() throws Exception
	{
		String path = outFilePath;
		if (path == null)
			path = filePath;
		
		outWriters = new FileWriter[1];
		outFiles = new File[1];
		
		outFiles[0] = new File(path + outputFileName);
		outWriters[0] = createWriter(outFiles[0].getAbsolutePath());
	}
	
	
	protected void closeWriters()
	{
		if (outWriters != null)
			for (int i = 0; i < outWriters.length; i++)
			{
				try { 
					outWriters[i].close();
					//logger.info("closed: " + outFiles[i].getAbsolutePath());
				} catch (Exception x) {
					logger.info("Error on clossing: " + outFiles[i].getAbsolutePath() + "\n" + x.getMessage());
				}
			}
	}
	
	
		
	protected void doTask(IAtomContainer mol) throws Exception
	{
		switch (task)
		{
		case PRINT_SMILES:
			printSmiles(mol);
			break;
			
		case GEN_RULE_PAIR:
			generateRulePairs(mol);
			break;
			
		case CALC_TAUTOMERS_ALL_RANKS:
			calcAllTautomersAndAllRanks(mol);
			break;
		}
	}
	
	protected void printSmiles(IAtomContainer mol) throws Exception
	{
		String smiles = SmartsHelper.moleculeToSMILES(mol, false);
		System.out.println(smiles);		
	
	}
	
	protected void generateRulePairs(IAtomContainer mol) throws Exception
	{
		String smiles = SmartsHelper.moleculeToSMILES(mol, false);
		ArrayList<TautomerPair> tpairs =  TautomerUtils.generatePairForEachRuleInstance(tman, mol, FlagFilter);
		for (int i = 0; i < tpairs.size(); i++)
		{
			outputTautomerPair(tpairs.get(i));
		}
		
		System.out.println("#" + records_read + "found " + tpairs.size() + " instances  " + smiles);
	}
	
	protected void outputTautomerPair(TautomerPair tp) 
	{
		FileWriter fw = outWriters[tp.ruleKBIndex];
		
		try {
		
			StringBuffer atInd = new StringBuffer();
			if (tp.riAtomIndices != null)
				for (int i = 0; i < tp.riAtomIndices.length; i++)
				{	
					atInd.append(sep);
					atInd.append(tp.riAtomIndices[i]);
				}
			
			String s0 = SmartsHelper.moleculeToSMILES(tp.mol0, false);
			String s1 = SmartsHelper.moleculeToSMILES(tp.mol1, false);
			String s = String.format("%s%s%s%s\n",s0, sep, s1, atInd.toString());
			//System.out.println(s);
			
			fw.write(s);
			
			fw.flush();
		}
		catch (Exception x){
			System.out.println("outputTautomerPair Error: " + x.getMessage());
		};
	}
	
	protected void calcAllTautomersAndAllRanks(IAtomContainer mol) throws Exception
	{
		String smiles = SmartsHelper.moleculeToSMILES(mol, false);
		tman.setStructure(mol);			
		List<IAtomContainer> resultTautomers = tman.generateTautomersIncrementaly();
		System.out.println("#" + records_read + "   " + smiles + "   nTautomers = " + resultTautomers.size());
		
		String s = String.format("%d%s%s%s%d%s%f\n",records_read, sep, smiles, sep, resultTautomers.size(), sep, 0.0);
		outWriters[0].write(s);
		outWriters[0].flush();
		
		if (resultTautomers.size() > 1)  
			for (int i = 0; i < resultTautomers.size(); i++)
			{
				IAtomContainer tautomer = resultTautomers.get(i);
				double old_rank = ((Double)tautomer.getProperty(TautomerConst.TAUTOMER_RANK)).doubleValue();
				double new_rank = tman.getEnergyRanking().calculateRank(tautomer, tman.getKnowledgeBase());
				String tautomer_smiles = SmartsHelper.moleculeToSMILES(tautomer, false);
				
				s = String.format("%d%s%s%s%f%s%f\n",records_read, sep, tautomer_smiles, sep, old_rank, sep, new_rank);
				System.out.println(s);
				outWriters[0].write(s);
				outWriters[0].flush();
			}
	}
	
	protected String getFileRuleName(String rname)
	{	
		String fname = new String (rname);
		fname = fname.replace('/', '_');
		//fname = fname.replace("#", "b3");
		return fname;
	}
	
	/**
	 * This is a measure for the tautomer ranking based on a comparison with
	 * tautomers energies
	 * @param ranks
	 * @param energies
	 * @return
	 */
	public static double calcTautomerRankingMeasure(double ranks[], double energies[])
	{
		if (ranks == null)
			return 0.0;
		if (energies == null)
			return 0.0;
		int n = ranks.length;
		if (n != energies.length)
			return 0.0;
		if (n == 0)
			return 0.0;
		
		double minRank = ranks[0];
		double maxRank = ranks[0];
		double minEnergy = energies[0];
		double maxEnergy = energies[0];
		
		for (int i = 1; i < n; i++)
		{
			if (minRank > ranks[i])
				minRank = ranks[i];
			if (maxRank < ranks[i])
				maxRank = ranks[i];
			if (minEnergy > energies[i])
				minEnergy = energies[i];
			if (maxEnergy < energies[i])
				maxEnergy = energies[i];
		}
		
		double deltaRank = maxRank - minRank;
		double deltaEnergy = maxEnergy - minEnergy;
		double measure = 0.0;
		
		for (int i = 0; i < n; i++)
		{
			measure += Math.abs(
					((deltaRank > 0)?(ranks[i]-minRank)/deltaRank:0.0) -
					((deltaEnergy > 0)?(energies[i]-minEnergy)/deltaEnergy:0.0)		
					);
		}
		
		measure = measure / n;
		
		return measure;
	}
	
	public static double calcTautomerRankCorrCoeff(double ranks[], double energies[])
	{
		if (ranks == null)
			return 0.0;
		if (energies == null)
			return 0.0;
		int n = ranks.length;
		if (n != energies.length)
			return 0.0;
		if (n == 0)
			return 0.0;
		
		double m_ranks = 0.0;
		double m_energies = 0.0;
		
		for (int i = 0; i < n; i++)
		{
			m_ranks += ranks[i];
			m_energies += energies[i];
		}
		
		m_ranks = m_ranks / n;
		m_energies = m_energies / n;
		
		double cov = 0.0;
		double sr = 0;
		double se = 0;
		
		for (int i = 0; i < n; i++)
		{
			cov += (ranks[i] - m_ranks)*(energies[i] - m_energies);
			sr += (ranks[i] - m_ranks)*(ranks[i] - m_ranks);
			se += (energies[i] - m_energies)*(energies[i] - m_energies);
		}
		
		if ((sr == 0.0) || (se == 0.0) )
			return 0.0;
		
		return cov/Math.sqrt(sr*se);
	}
	
	public static double calcTautomerRankSpearmanCorrCoeff(double ranks[], double energies[], boolean isFractional)
	{
		if (ranks == null)
			return 0.0;
		if (energies == null)
			return 0.0;
		int n = ranks.length;
		if (n != energies.length)
			return 0.0;
		if (n == 0)
			return 0.0;
		if (n == 1)
			return 1.0;
		
		double r_ranks[] = getRanks(ranks, isFractional);
		double r_energies[] = getRanks(energies, isFractional);
		double sumdiff = 0.0;
		
		for (int i = 0; i < n; i++)
		{
			double d = r_ranks[i] - r_energies[i]; 
			sumdiff += d*d;
		}
		
		return 1.0 - 6.0 * sumdiff / (n*(n*n-1));
	}
	
	public static double [] getRanks(double values[], boolean isFractionalRanking)
	{
		int n = values.length;
		int index[] = new int[n];
		double v[] = values.clone();
		
		for (int i = 0; i < n; i++)
			index[i] = i;
		
		
		for (int i = 1; i < n; i++)
			for (int k = 0; k < n-i; k++)
			{
				if (v[k] >  v[k+1])
				{
					//swap values
					double tmp = v[k+1];
					v[k+1] = v[k];
					v[k] = tmp;
					
					//swap original indices
					int tmp_i = index[k+1];
					index[k+1] = index[k];
					index[k] = tmp_i;
				}
			}
		
		
		double ranks[] = new double[n];
		
		if (isFractionalRanking)
		{
			//Get fractional ranking
			//each element of a sequence of several equal values with indices k, k+1,..., k+s-1
			//takes a fractional ranking r_fraq = (k + k+1 + ... + k+s-1)/s
			//r_fraq is also equal to k+(s-1)/2
			
			int startIndex = 0;
			double startValue = v[0];
			double rankSum = 0.0;
			
			for (int i = 1; i < n; i++)
			{
				if (v[i] == startValue)
				{
					rankSum += i;
				}
				else
				{
					//v[i] != startValue
					double r_fraq = rankSum / (i-startIndex);
					for (int k = startIndex; k < i; k++)
						ranks[index[k]] = r_fraq;
					startIndex = i;
					startValue = v[i];
					rankSum = i;
				}
			}
			
			//Handle last sequence of values
			double r_fraq = rankSum / (n-startIndex);
			for (int k = startIndex; k < n; k++)
				ranks[index[k]] = r_fraq;
		}
		else
		{	
			//Get order ranking
			for (int i = 0; i < n; i++)
			{
				//value with original index[i] has rank i 
				ranks[index[i]] = i;
			}
		}
			
		return ranks;
	}
	
	public static int getNumberOfDistinctValues(double values[])
	{
		HashSet<Double> set = new HashSet<Double>();
		for (int i = 0; i < values.length; i++ )
			set.add(values[i]);
		return set.size();
	}
}
