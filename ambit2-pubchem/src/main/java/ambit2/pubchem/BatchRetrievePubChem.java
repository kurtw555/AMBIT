/*
Copyright (C) 2005-2008  

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

package ambit2.pubchem;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import net.idea.modbcum.i.exceptions.AmbitException;
import net.idea.modbcum.p.DefaultAmbitProcessor;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import ambit2.base.interfaces.IStructureRecord;
import ambit2.base.processors.ProcessorException;
import ambit2.core.data.MoleculeTools;

public class BatchRetrievePubChem extends DefaultAmbitProcessor<String,Integer> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3658928227952234163L;
	protected String resultDir;
	
	protected String logfile="/entrez_pubchem.log";
	public String getResultDir() {
		return resultDir;
	}

	public void setResultDir(String resultDir) {
		this.resultDir = resultDir;
	}

	public Integer process(String dirname) throws AmbitException {
		int processed = 0;		
		try {

			if (resultDir == null) resultDir = dirname;
			File logfile = new File(resultDir+getLogfile());
			if (logfile.exists()) logfile.delete();
			
			FileWriter logwriter = new FileWriter(resultDir+getLogfile());
			IAtomContainer m = MoleculeTools.newMolecule(SilentChemObjectBuilder.getInstance());
			File dir = new File(dirname);

		    
		    
		    // This filter only returns directories
		    FileFilter fileFilter = new FileFilter() {
		        public boolean accept(File file) {
		        	
		            return !file.isDirectory() &&
		            	file.getName().endsWith(".xml");	

		        }
		    };
		    
		    File[] files = dir.listFiles(fileFilter);
		    
		    EntrezESearchParser entrezParser = new EntrezESearchParser();
		    PUGProcessor pug = new PUGProcessor();
		    
		    
		    for (File file: files) {
	    		File sdffile = getResultFile(resultDir, file);
	    		if (sdffile.exists()) continue;
	    		processed++;
		    	long now = System.currentTimeMillis();	    	
	    		
		    	try{
		
			    	FileInputStream in = new FileInputStream(file);
			    	List<IStructureRecord> records = entrezParser.process(in);
			    	in.close();
			    	
			    	List<IStructureRecord> cid_records = new ArrayList<IStructureRecord>();
			    	for (IStructureRecord record : records) {
			    		if (PUGProcessor.PUBCHEM_CID.equals(record.getFormat()))
			    			cid_records.add(record);
			    	}
			    	
			    	if (cid_records.size()>0) {
			    		try {
				    		records = pug.process(cid_records);
				    		
				    		FileWriter writer = new FileWriter(sdffile);
					    	for (IStructureRecord record : records) 
					    		if ("sdf".equals(record.getFormat().toLowerCase())) {
					    			writer.write(record.getContent());
					    		}
					    	writer.flush();
					    	writer.close();
					    	logger.fine(file.getName());	    	
					    	
					    	logwriter.write(getMessage(file.getName(),sdffile.getName(),"success",System.currentTimeMillis() - now));
					    	logwriter.flush();
			    		} catch (Exception x) {
			    			x.printStackTrace();
					    	logwriter.write(getMessage(file.getName(),sdffile.getName(),x.getMessage(),System.currentTimeMillis() - now));
					    	logwriter.flush();
			    		}
			    	} else {
		    			SDFWriter mdlWriter = new SDFWriter(new FileWriter(sdffile));
		    			mdlWriter.write(m);
		    			mdlWriter.close();
				    	logwriter.write(getMessage(file.getName(),sdffile.getName(),"CID not available",System.currentTimeMillis() - now));
				    	logwriter.flush();    			
			    	}
		    	} catch (Exception x) {
		    		x.printStackTrace();
		    		try {
				    	logwriter.write(getMessage(file.getName(),sdffile.getName(),"CID not available",System.currentTimeMillis() - now));
				    	logwriter.flush();
		    		} catch (IOException xx) {
		    			xx.printStackTrace();
		    		}
		    	}

		    }
		    try {
		    	logwriter.close();
		    } catch (IOException x) {
		    	throw new ProcessorException(this,x);
		    }			    

		} catch (Exception x) {
			throw new ProcessorException(this,x);
		}
			return new Integer(processed);
		}
	
		
		protected String getMessage(String filename,String outfile, String status, long time) {
			StringBuilder b = new StringBuilder();
			b.append("now=");
			b.append(new SimpleDateFormat("M/d/y,H:m",Locale.US).format(Calendar.getInstance(TimeZone.getDefault()).getTime()));
			b.append("\tclass=");
			b.append(getClass().getName());
			b.append("\tinput=");
			b.append(filename);
			b.append("\toutput=");
			b.append(outfile);
			b.append("\tstatus=");
			b.append(status);
			b.append("\telapsed(ms)=");
			b.append(Long.toString(time));
			b.append('\n');
			return b.toString();
		}
		
		protected File getResultFile(String resultdir, File file) {
			String newname = "";
		    int p = file.getName().lastIndexOf(".");
		    if (p != -1) {
		       newname = file.getName().substring(0, p) + ".sdf";
		    } else {
		       newname = file.getName() + ".sdf";
		    }
		    
			return new File(resultdir+"/"+newname);
		}

		public String getLogfile() {
			return logfile;
		}

		public void setLogfile(String logfile) {
			this.logfile = logfile;
		}

}


