package ambit2.rest.pubchem;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.logging.Level;

import net.idea.modbcum.i.exceptions.AmbitException;
import net.idea.modbcum.i.exceptions.NotFoundException;
import net.idea.restnet.c.ChemicalMediaType;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

import ambit2.base.interfaces.IStructureRecord;
import ambit2.pubchem.EntrezSearchProcessor;
import ambit2.rest.ProtectedResource;
import ambit2.rest.query.QueryResource;

/**
 * Retrieves structure in SDF format from pubchem <br>
 * URI /query/pubchem/{query_term} <br>
 * REST Operation: GET ; returns text/plain or chemical/x-mdl-sdfile
 * 
 * @author nina
 * 
 */
public class PubchemResource extends ProtectedResource {
	public static final String resource = "/pubchem";
	protected static final String resourceKey = "term";
	public static final String resourceID = String.format("/{%s}", resourceKey);
	protected String term = "";
	protected EntrezSearchProcessor entrezQuery;

	@Override
	protected void doInit() throws ResourceException {
		super.doInit();
		MediaType[] mimeTypes = new MediaType[] { MediaType.TEXT_PLAIN,
				ChemicalMediaType.CHEMICAL_MDLSDF, MediaType.TEXT_HTML,
				ChemicalMediaType.CHEMICAL_MDLSDF };
		// List<Variant> variants = new ArrayList<Variant>();
		for (MediaType mileType : mimeTypes)
			getVariants().add(new Variant(mileType));
		// getVariants().put(Method.GET, variants);

		try {
			this.term = Reference.decode(getRequest().getAttributes()
					.get(resourceKey).toString());
		} catch (Exception x) {
			Form form = getResourceRef(getRequest()).getQueryAsForm();
			Object key = form.getFirstValue(QueryResource.search_param);
			if (key != null) {
				term = Reference.decode(key.toString());
			} else
				this.term = null;
		}
		entrezQuery = new EntrezSearchProcessor();

	}

	public Representation get(Variant variant) {
		setFrameOptions("SAMEORIGIN");
		try {
			if (term != null) {
				List<IStructureRecord> records = entrezQuery.process(term);
				if (records == null) {
					getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					return null;
				}
				if (variant.getMediaType().equals(
						ChemicalMediaType.CHEMICAL_MDLSDF))
					return new OutputRepresentation(variant.getMediaType()) {
						@Override
						public void write(OutputStream stream)
								throws IOException {
							OutputStreamWriter writer = null;
							try {
								writer = new OutputStreamWriter(stream);
								List<IStructureRecord> records = entrezQuery
										.process(term);
								for (IStructureRecord record : records)
									if (record.getFormat().toLowerCase()
											.equals("sdf"))
										writer.write(record.getContent());
								writer.flush();
								stream.flush();
							} catch (AmbitException x) {
								getLogger().log(Level.WARNING, x.getMessage(),
										x);
								// throw new IOException(x.getMessage());
							} finally {
								try {
									if (writer != null)
										writer.flush();
								} catch (Exception x) {
									x.printStackTrace();
								}
								try {
									if (stream != null)
										stream.flush();
								} catch (Exception x) {
									x.printStackTrace();
								}
							}
						}
					};
				else {
					StringBuilder b = new StringBuilder();
					for (IStructureRecord record : records)
						if (record.getFormat().toLowerCase().equals("sdf"))
							b.append(record.getContent());

					return new StringRepresentation(b.toString(),
							ChemicalMediaType.CHEMICAL_MDLSDF);
				}
			} else
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
		} catch (NotFoundException x) {

			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"No results for query " + term);
			return null;
		} catch (ResourceException x) {
			throw x;
		} catch (Exception x) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
		}
	}

}
