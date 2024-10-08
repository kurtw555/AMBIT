package ambit2.rest.structure.dataset;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

import com.hp.hpl.jena.ontology.OntModel;

import ambit2.base.data.ISourceDataset;
import ambit2.base.data.SourceDataset;
import ambit2.base.data.StructureRecord;
import ambit2.base.interfaces.IStructureRecord;
import ambit2.db.readers.RetrieveDatasets;
import ambit2.rest.OpenTox;
import ambit2.rest.RDFJenaConvertor;
import ambit2.rest.dataset.DatasetURIReporter;
import ambit2.rest.dataset.MetadataRDFReporter;
import ambit2.rest.dataset.MetadatasetJSONReporter;
import ambit2.rest.query.QueryResource;
import net.idea.modbcum.i.IQueryRetrieval;
import net.idea.modbcum.i.exceptions.AmbitException;
import net.idea.modbcum.i.processors.IProcessor;
import net.idea.modbcum.r.QueryReporter;
import net.idea.restnet.c.ChemicalMediaType;
import net.idea.restnet.c.StringConvertor;
import net.idea.restnet.db.convertors.OutputWriterConvertor;

public class DatasetsByStructureResource extends
		QueryResource<IQueryRetrieval<SourceDataset>, SourceDataset> {

	@Override
	public IProcessor<IQueryRetrieval<SourceDataset>, Representation> createConvertor(
			Variant variant) throws AmbitException, ResourceException {

		String filenamePrefix = getRequest().getResourceRef().getPath();
		if (variant.getMediaType().equals(MediaType.TEXT_URI_LIST)) {
			return new StringConvertor(
					new DatasetURIReporter<IQueryRetrieval<ISourceDataset>, ISourceDataset>(
							getRequest()), MediaType.TEXT_URI_LIST,
					filenamePrefix);
		} else if (variant.getMediaType().equals(MediaType.APPLICATION_JSON)) {
			return new OutputWriterConvertor(
					new MetadatasetJSONReporter<IQueryRetrieval<ISourceDataset>, ISourceDataset>(
							getRequest()), MediaType.APPLICATION_JSON);

		} else if (variant.getMediaType().equals(MediaType.APPLICATION_RDF_XML)
				|| variant.getMediaType().equals(
						MediaType.APPLICATION_RDF_TURTLE)
				|| variant.getMediaType().equals(MediaType.TEXT_RDF_N3)
				|| variant.getMediaType().equals(MediaType.TEXT_RDF_NTRIPLES)
				|| variant.getMediaType()
						.equals(MediaType.APPLICATION_RDF_TRIG)
				|| variant.getMediaType()
						.equals(MediaType.APPLICATION_RDF_TRIX)
				|| variant.getMediaType().equals(
						ChemicalMediaType.APPLICATION_JSONLD)) {
			QueryReporter<SourceDataset, IQueryRetrieval<SourceDataset>, OntModel> reporter = new MetadataRDFReporter<SourceDataset, IQueryRetrieval<SourceDataset>>(
					getRequest(), variant.getMediaType());
			return new RDFJenaConvertor<SourceDataset, IQueryRetrieval<SourceDataset>>(
					reporter, variant.getMediaType(), filenamePrefix);

		} else
			return new OutputWriterConvertor(
					new MetadatasetJSONReporter<IQueryRetrieval<ISourceDataset>, ISourceDataset>(
							getRequest()), MediaType.APPLICATION_JSON);
	}

	@Override
	protected IQueryRetrieval<SourceDataset> createQuery(Context context,
			Request request, Response response) throws ResourceException {
		try {
			Form form = request.getResourceRef().getQueryAsForm();
			headless = Boolean.parseBoolean(form.getFirstValue("headless"));
		} catch (Exception x) {
			headless = false;
		}
		int idcompound = -1;
		int idstructure = -1;
		try {
			idcompound = OpenTox.URI.compound.getIntValue(getRequest());
			try {
				idstructure = OpenTox.URI.conformer.getIntValue(getRequest());
			} catch (Exception x) {
				idstructure = -1;
			}
		} catch (Exception x) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					OpenTox.URI.compound.getKey());
		}
		IStructureRecord record = new StructureRecord(idcompound, idstructure,
				null, null);

		RetrieveDatasets q = new RetrieveDatasets();
		q.setFieldname(record);
		return q;
	}
}
