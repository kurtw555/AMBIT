package ambit2.rest;

import java.io.IOException;
import java.io.OutputStream;

import net.idea.modbcum.i.IQueryRetrieval;
import net.idea.modbcum.i.exceptions.AmbitException;
import net.idea.modbcum.r.QueryReporter;
import net.idea.restnet.c.ChemicalMediaType;
import net.idea.restnet.db.convertors.AbstractObjectConvertor;
import net.idea.restnet.rdf.ns.OT;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFWriter;

/**
 * TODO refactor to work with default model
 * Jena model convertor
 * @author nina
 *
 * @param <T>
 * @param <Q>
 * @param <R>
 */
public class RDFJenaConvertor<T,Q extends IQueryRetrieval<T>>  extends AbstractObjectConvertor<T,Q,OntModel>  {
	
	protected boolean xml_abbreviation = true;
	public boolean isXml_abbreviation() {
		return xml_abbreviation;
	}
	public void setXml_abbreviation(boolean xml_abbreviation) {
		this.xml_abbreviation = xml_abbreviation;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6566828643076743577L;

	
	public RDFJenaConvertor(QueryReporter<T,Q,OntModel> reporter) {
		this(reporter,MediaType.APPLICATION_RDF_XML);
		if (this.reporter != null) ((QueryReporter<T,Q,OntModel>)this.reporter).setMaxRecords(5000);
	}
	public RDFJenaConvertor(QueryReporter<T,Q,OntModel> reporter,MediaType media) {
		this(reporter,media,null);
	}

	public RDFJenaConvertor(QueryReporter<T,Q,OntModel> reporter,MediaType media,String fileNamePrefix) {
		super(reporter,media,fileNamePrefix);
	}

	@Override
	protected OntModel createOutput(Q query) throws AmbitException {
		try {
			return OT.createModel();
		} catch (Exception x) {
			throw new AmbitException(x);
		}
	}

	@Override
	public Representation processDoc(final OntModel jenaModel) throws AmbitException {
		/*
		To optimise the speed of writing RDF/XML it is suggested that all URI processing is turned off. 
		Also do not use RDF/XML-ABBREV. 
		It is unclear whether the longId attribute is faster or slower; 
		the short IDs have to be generated on the fly and a table maintained during writing. 
		The longer IDs are long, and hence take longer to write. The following creates a faster writer: 

	   RDFWriter fasterWriter = m.getWriter("RDF/XML");
	   fasterWriter.setProperty("allowBadURIs","true");
	   fasterWriter.setProperty("relativeURIs","");
	   fasterWriter.setProperty("tab","0");

		 */
		OutputRepresentation rep = new OutputRepresentation(mediaType) {
	            @Override
	            public void write(OutputStream output) throws IOException {
	            	try {
	            		RDFWriter fasterWriter = null;
	        			if (mediaType.equals(MediaType.APPLICATION_RDF_XML)) {
	        				if (isXml_abbreviation())
	        					fasterWriter = jenaModel.getWriter("RDF/XML-ABBREV");//lot smaller ... but could be slower
	        				else
	        					fasterWriter = jenaModel.getWriter("RDF/XML");
	        				fasterWriter.setProperty("xmlbase",jenaModel.getNsPrefixURI(""));
	        				fasterWriter.setProperty("showXmlDeclaration", Boolean.TRUE);
	        				fasterWriter.setProperty("showDoctypeDeclaration", Boolean.TRUE);	        				
	        			}
	        			else if (mediaType.equals(MediaType.APPLICATION_RDF_TURTLE))
	        				fasterWriter = jenaModel.getWriter("TURTLE");
	        			else if (mediaType.equals(MediaType.TEXT_RDF_N3))
	        				fasterWriter = jenaModel.getWriter("N3");
	        			else if (mediaType.equals(MediaType.TEXT_RDF_NTRIPLES))
	        				fasterWriter = jenaModel.getWriter("N-TRIPLE");
	        			else if (mediaType.equals(ChemicalMediaType.APPLICATION_JSONLD))
	        				fasterWriter = jenaModel.getWriter("JSON-LD");	        			
	        			else {
	        				fasterWriter = jenaModel.getWriter("RDF/XML-ABBREV");
	        				fasterWriter.setProperty("showXmlDeclaration", Boolean.TRUE);
	        				fasterWriter.setProperty("showDoctypeDeclaration", Boolean.TRUE);	//essential to get XSD prefixed
	        				fasterWriter.setProperty("xmlbase",jenaModel.getNsPrefixURI(""));	        				
	        			}
	        			
	        			fasterWriter.write(jenaModel,output,"http://opentox.org/api/1.1");
	            	} catch (Exception x) {
	            		Throwable ex = x;
	            		while (ex!=null) {
	            			if (ex instanceof IOException) 
	            				throw (IOException)ex;
	            			ex = ex.getCause();
	            		}
	            		Context.getCurrentLogger().warning(x.getMessage()==null?x.toString():x.getMessage());
	            	} finally {

	            		try {if (output !=null) output.flush(); } catch (Exception x) { x.printStackTrace();}
	            		try {getReporter().setOutput(null); } catch (Exception x) { x.printStackTrace();}
	            		try {getReporter().close(); } catch (Exception x) { x.printStackTrace();}
	            		try {if (jenaModel !=null) jenaModel.close(); } catch (Exception x) { x.printStackTrace();}
	            	}
	            }
	        };	
	        setDisposition(rep);
	        return rep;

	}
	
}
