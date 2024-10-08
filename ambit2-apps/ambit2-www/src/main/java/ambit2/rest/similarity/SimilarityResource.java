package ambit2.rest.similarity;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import net.idea.modbcum.i.IQueryRetrieval;
import net.idea.modbcum.i.exceptions.AmbitException;
import net.idea.modbcum.q.conditions.NumberCondition;

import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Template;

import ambit2.base.data.ISourceDataset;
import ambit2.base.data.Property;
import ambit2.base.data.SourceDataset;
import ambit2.base.data.substance.SubstanceEndpointsBundle;
import ambit2.base.interfaces.IStructureRecord;
import ambit2.core.data.HashIntDescriptorResult;
import ambit2.core.processors.structure.FingerprintGenerator;
import ambit2.db.reporters.CSVReporter;
import ambit2.db.search.StoredQuery;
import ambit2.db.search.structure.ChemicalByQueryFolder;
import ambit2.db.search.structure.QueryAtomEnvironment;
import ambit2.db.search.structure.QueryCombinedStructure;
import ambit2.db.search.structure.QuerySimilarity;
import ambit2.db.search.structure.QuerySimilarityBitset;
import ambit2.db.substance.relation.ChemicalBySubstanceRelation;
import ambit2.db.update.structure.ChemicalByDataset;
import ambit2.descriptors.AtomEnvironentMatrix;
import ambit2.rest.AmbitApplication;
import ambit2.rest.OpenTox;
import ambit2.rest.dataset.DatasetResource;
import ambit2.rest.dataset.DatasetStructuresResource;
import ambit2.rest.error.InvalidResourceIDException;
import ambit2.rest.property.PropertyResource;
import ambit2.rest.query.StructureQueryResource;

/**
 * Retrieve similar compounds, given a smiles
 * /similarity?search="smiles"&threshold=0.9
 * 
 * @author nina
 * 
 */
public class SimilarityResource<Q extends IQueryRetrieval<IStructureRecord>>
		extends StructureQueryResource<Q> {

	public final static String resource = "/similarity";
	public final static String smiles = "smiles";
	public final static String resourceID = String.format("/{%s}", smiles);
	protected double threshold = 0.9;
	protected String dataset_id;
	protected boolean filterBySubstance = false;
	protected IAtomContainer mol;
	protected transient AtomEnvironentMatrix matrix = null;

	public SimilarityResource() {
		super();
	}

	@Override
	public String getCompoundInDatasetPrefix() {
		if (dataset_prefixed_compound_uri)
			return dataset_id != null ? String.format("%s/%s",
					OpenTox.URI.dataset.getURI(), dataset_id) : "";

		else
			return "";
	}

	protected String getDefaultTemplateURI(Context context, Request request,
			Response response) {
		return (dataset_id == null) ? null : String.format("%s%s/%s%s",
				getRequest().getRootRef(), OpenTox.URI.dataset.getURI(),
				dataset_id, PropertyResource.featuredef);

		// String.format("riap://application/dataset/%s%s",dataset_id,PropertyResource.featuredef);
	}

	@Override
	protected CSVReporter createCSVReporter() {
		CSVReporter csvReporter = super.createCSVReporter();
		csvReporter.setSimilarityColumn(Property.getInstance("metric",
				queryObject == null ? "" : queryObject.toString(),
				"http://ambit.sourceforge.net"));
		return csvReporter;
	}

	@Override
	protected Q createQuery(Context context, Request request, Response response)
			throws ResourceException {
		Form form = getResourceRef(getRequest()).getQueryAsForm();

		try {
			Object bundleURI = OpenTox.params.bundle_uri.getFirstValue(form);
			Integer idbundle = bundleURI == null ? null : getIdBundle(
					bundleURI, request);
			SubstanceEndpointsBundle bundle = new SubstanceEndpointsBundle(
					idbundle);
			bundles = new SubstanceEndpointsBundle[1];
			bundles[0] = bundle;
		} catch (Exception x) {
			bundles = null;
		}

		try {
			includeMol = "true".equals(form.getFirstValue("mol"));
		} catch (Exception x) {
			includeMol = false;
		}
		folders = form.getValuesArray("folder");

		filterBySubstance = false;
		try {
			String filter = form.getFirstValue("filterBySubstance");
			if (filter != null) {
				filter = filter.toLowerCase();
				filterBySubstance = "yes".equals(filter) || "on".equals(filter)
						|| "true".equals(filter);
			}
		} catch (Exception x) {
			filterBySubstance = false;
		}

		QueryAtomEnvironment.q_modifier ae = null;
		try {
			String filter = form.getFirstValue("mode");
			if (filter != null) {
				filter = filter.toLowerCase();
				ae = QueryAtomEnvironment.q_modifier.valueOf(filter.replace("ae",""));
			}
		} catch (Exception x) {
			ae = null;
		}

		mol = getMolecule(form);
		if ((mol == null) || (mol.getAtomCount() == 0))
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"Empty molecule");
		threshold = 0.0;
		try {
			threshold = new Double(Reference.decode(form
					.getFirstValue("threshold")));
		} catch (Exception x) {
			threshold = 0.9;
		}

		QuerySimilarity q = null;
		if (ae!=null) {
			q = new QueryAtomEnvironment(ae);
			try {
				q.setValue(getAE(mol));
			} catch (Exception x) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, x);
			}
		} else
			try {
				q = new QuerySimilarityBitset();
				q.setValue(getBitset(mol));
			} catch (Exception x) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, x);
			}
		q.setChemicalsOnly(true);
		q.setThreshold(threshold);
		q.setCondition(NumberCondition.getInstance(">"));
		q.setName("Similarity");

		try {
			q.setForceOrdering(((AmbitApplication) getApplication())
					.isSimilarityOrder());
		} catch (Exception x) {
			q.setForceOrdering(true);
		}

		try {

			if (filterBySubstance) {
				ChemicalBySubstanceRelation qa = new ChemicalBySubstanceRelation();
				QueryCombinedStructure qc = new QueryCombinedStructure();
				qc.add(q);
				qc.setChemicalsOnly(true);
				qc.setScope(qa);
				setTemplate(createTemplate(context, request, response));
				setGroupProperties(context, request, response);
				return (Q) qc;
			} else if ((folders != null) && (folders.length > 0)) {
				ChemicalByQueryFolder qa = new ChemicalByQueryFolder(folders);
				QueryCombinedStructure qc = new QueryCombinedStructure();
				qc.add(q);
				qc.setChemicalsOnly(true);
				qc.setScope(qa);
				setTemplate(createTemplate(context, request, response));
				setGroupProperties(context, request, response);
				return (Q) qc;
			} else {
				Object datasetURI = OpenTox.params.dataset_uri
						.getFirstValue(form);
				ISourceDataset srcdataset = null;
				if (datasetURI != null)
					try {
						srcdataset = getDataset(datasetURI.toString());
						QueryCombinedStructure qc = new QueryCombinedStructure();
						qc.add(q);
						qc.setChemicalsOnly(true);
						if (srcdataset instanceof SourceDataset) {
							ChemicalByDataset cd = new ChemicalByDataset(
									new Integer(srcdataset.getID()));
							qc.setScope(cd);
							setTemplate(createTemplate(context, request,
									response));
							setGroupProperties(context, request, response);
							return (Q) qc;
						} else {
							// TODO, resort to all db
						}
					} catch (Exception x) {
						srcdataset = null;
					}

				QueryCombinedStructure qc = null;
				try {

					this.dataset_id = Reference.decode(getRequest()
							.getAttributes().get(DatasetResource.datasetKey)
							.toString());
					qc = new QueryCombinedStructure();
					qc.add(q);
					qc.setChemicalsOnly(true);
					ChemicalByDataset cd = new ChemicalByDataset(new Integer(
							dataset_id));
					qc.setScope(cd);
					setTemplate(createTemplate(context, request, response));
					setGroupProperties(context, request, response);
					return (Q) qc;
				} catch (Exception x) {
					setTemplate(createTemplate(context, request, response));
					setGroupProperties(context, request, response);
					return (Q) q;
				}
			}
		} catch (Exception x) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, x);
		}

	}

	public BitSet getBitset(IAtomContainer molecule) throws AmbitException {
		FingerprintGenerator gen = new FingerprintGenerator(new Fingerprinter());
		return gen.process(molecule);
	}

	public HashIntDescriptorResult getAE(IAtomContainer molecule)
			throws Exception {
		if (matrix == null)
			matrix = new AtomEnvironentMatrix(
					CDKAtomTypeMatcher.getInstance(SilentChemObjectBuilder
							.getInstance()),
					"org/openscience/cdk/dict/data/cdk-atom-types.owl", 7);
		return matrix.doCalculation(molecule);
	}

	protected ISourceDataset getDataset(String uri)
			throws InvalidResourceIDException {
		Map<String, Object> vars = new HashMap<String, Object>();
		Template template = OpenTox.URI.dataset.getTemplate(getRequest()
				.getRootRef());
		String id = null;
		try {
			template.parse(uri, vars);
			id = vars.get(OpenTox.URI.dataset.getKey()).toString();
		} catch (Exception x) {
			return null;
		}

		if (id != null)
			try {
				Integer idnum = new Integer(Reference.decode(id.toString()));
				SourceDataset dataset = new SourceDataset();
				dataset.setID(idnum);
				return dataset;
			} catch (NumberFormatException x) {
				if (id.toString().startsWith(
						DatasetStructuresResource.QR_PREFIX)) {
					String key = id.toString().substring(
							DatasetStructuresResource.QR_PREFIX.length());
					try {
						ISourceDataset dataset = new StoredQuery();
						dataset.setID(Integer.parseInt(key.toString()));
						return dataset;
					} catch (NumberFormatException xx) {
						throw new InvalidResourceIDException(id);
					}
				}
			} catch (Exception x) {
				throw new InvalidResourceIDException(id);
			}
		return null;
	}

	@Override
	protected String[] getDefaultGroupProperties() {
		return new String[] { Property.opentox_CAS, Property.opentox_EC,
				Property.opentox_Name, Property.opentox_TradeName,
				Property.opentox_IupacName, Property.opentox_IUCLID5_UUID,
				Property.opentox_SMILES, Property.opentox_InChI_std,
				Property.opentox_InChIKey_std };
	}
}
