package ambit2.db.reporters.test;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ambit2.base.data.SubstanceRecord;
import ambit2.base.data.study.IParams;
import ambit2.base.data.study.Params;
import ambit2.base.data.study.Protocol;
import ambit2.base.data.study.ProtocolEffectRecordMatrix;
import ambit2.base.data.study.Value;
import ambit2.base.data.substance.SubstanceProperty;
import ambit2.core.io.study.ProtocolEffectRecord2SubstanceProperty;
import ambit2.core.io.study.StudyFormatter;
import junit.framework.Assert;

public class TestStudyExportFormatting {
	protected static final StudyFormatter f = new StudyFormatter();
	protected static final ProtocolEffectRecord2SubstanceProperty convertor = new ProtocolEffectRecord2SubstanceProperty();
	protected Logger logger = Logger.getLogger(getClass().getName());

	protected IParams initConditions(Protocol._categories c) throws Exception {
		Params conditions = new Params();
		ObjectNode conditionsConfig = f.getConditionsConfig(f.getCategoryConfig(c));
		if (conditionsConfig == null) {
			logger.log(Level.FINE, String.format("%s : missing conditions config", c.name()));
		} else {
			Iterator<String> names = conditionsConfig.fieldNames();
			while (names.hasNext()) {
				String name = names.next();
				JsonNode node = conditionsConfig.get(name);
				Value t = new Value(20);
				t.setUnits("UNIT");
				conditions.put(name, t);
				logger.log(Level.FINE, String.format("%s : added conditions %s name", c.name(), name));
			}
		}

		Params params = new Params();
		ObjectNode paramsConfig = f.getParametersConfig(f.getCategoryConfig(c));
		if (paramsConfig == null) {
			logger.log(Level.FINE, String.format("%s : missing parameters config", c.name()));
		} else {
			Iterator<String> names = paramsConfig.fieldNames();
			while (names.hasNext()) {
				String name = names.next();
				JsonNode node = paramsConfig.get(name);
				Value t = new Value("paramvalue");
				t.setUnits("UNIT");
				conditions.put(name, t);
				logger.log(Level.FINE, String.format("%s : added parameters %s name", c.name(), name));
			}
		}
		return conditions;
	}

	protected ProtocolEffectRecordMatrix<String, String, String> initDetail(Protocol._categories c) throws Exception {
		ProtocolEffectRecordMatrix<String, String, String> detail = new ProtocolEffectRecordMatrix<String, String, String>();

		Protocol protocol = new Protocol(null);
		protocol.setCategory(c.name());
		protocol.setTopCategory(c.getTopCategory());
		protocol.addGuideline("Guideline");
		detail.setProtocol(protocol);
		detail.setInterpretationResult("interpretationResult");
		return detail;
	}

	protected String format(Protocol._categories c, ProtocolEffectRecordMatrix<String, String, String> detail,
			IParams conditions, boolean istextvalue) throws Exception {
		ProtocolEffectRecordMatrix<String, IParams, String> effect = new ProtocolEffectRecordMatrix<String, IParams, String>();
		ProtocolEffectRecord2SubstanceProperty.copyEffectRecordValues(detail, effect);
		effect.setProtocol(detail.getProtocol());
		effect.setConditions(conditions);
		SubstanceProperty key = convertor.process(effect);

		SubstanceRecord master = new SubstanceRecord();
		Value value1 = ProtocolEffectRecord2SubstanceProperty.processValue(detail, istextvalue);
		ProtocolEffectRecord2SubstanceProperty.addValues(master, key, value1, master.getRecordProperty(key));
		return f.format(key, master.getRecordProperty(key));
	}

	@Test
	public void testInterpretationResult_skinirritation() throws Exception {
		Protocol._categories c = Protocol._categories.TO_SKIN_IRRITATION_SECTION;
		IParams conditions = initConditions(c);
		ProtocolEffectRecordMatrix<String, String, String> detail = initDetail(c);
		detail.setInterpretationResult("interpretationResult");
		detail.setUnit(null);
		detail.setTextValue(null);
		detail.setEndpoint(null);
		detail.setConditions(conditions.toString());
		String result = format(c, detail, conditions, true);
		Assert.assertEquals("interpretationResult (paramvalue)", result);

	}

	@Test
	public void testInterpretationResult() throws Exception {
		for (Protocol._categories c : Protocol._categories.values()) {
			IParams conditions = initConditions(c);
			ProtocolEffectRecordMatrix<String, String, String> detail = initDetail(c);
			detail.setInterpretationResult("interpretationResult");
			detail.setUnit(null);
			detail.setTextValue(null);
			detail.setEndpoint(null);
			detail.setConditions(conditions.toString());
			try {
				String result = format(c, detail, conditions, true);

				logger.log(Level.INFO,
						String.format("case %s: { Assert.assertEquals(\"%s\",result); break; }", c.name(), result));

				switch (c) {
				case GI_GENERAL_INFORM_SECTION: {
					Assert.assertEquals("interpretation_result interpretationResult", result);
					break;
				}
				case TO_BIODEG_WATER_SCREEN_SECTION: {
					Assert.assertEquals("interpretationResult [Guideline]", result);
					break;
				}

				case TO_SKIN_IRRITATION_SECTION: {
					Assert.assertEquals("interpretationResult (paramvalue)", result);
					break;
				}
				case TO_EYE_IRRITATION_SECTION: {
					Assert.assertEquals("interpretationResult (paramvalue)", result);
					break;
				}
				case TO_SENSITIZATION_SECTION: {
					Assert.assertEquals("interpretationResult (paramvalue)", result);
					break;
				}
				case TO_GENETIC_IN_VITRO_SECTION: {
					Assert.assertEquals("interpretationResult [Guideline] (paramvalue)", result);
					break;
				}
				case TO_GENETIC_IN_VIVO_SECTION: {
					Assert.assertEquals("interpretationResult [Guideline] (paramvalue)", result);
					break;
				}
				case BAO_0003009_SECTION: {
					Assert.assertEquals("interpretationResult [Guideline]", result);
					break;
				}
				case BAO_0010001_SECTION: {
					Assert.assertEquals("interpretationResult", result);
					break;
				}
				case NPO_1911_SECTION: {
					Assert.assertEquals("interpretationResult", result);
					break;
				}
				case TO_SENSITIZATION_INSILICO_SECTION: {
					Assert.assertEquals("interpretationResult [Guideline] (type of study=paramvalue)", result);
					break;
				}
				case TO_SENSITIZATION_HUMANDB_SECTION: {
					Assert.assertEquals("NOEL =interpretationResult", result);
					break;
				}
				default: {
					Assert.assertEquals("interpretationResult [Guideline]", result);

				}
				}
			} catch (Exception x) {
				if (f.inMatrix(f.getInterpretationResultConfig(f.getCategoryConfig(c))))
					throw x;
			}

		}
	}

	@Test
	public void testTextValues() throws Exception {
		for (Protocol._categories c : Protocol._categories.values()) {
			IParams conditions = initConditions(c);
			ProtocolEffectRecordMatrix<String, String, String> detail = initDetail(c);

			detail.setUnit(null);
			detail.setTextValue("textValue");
			detail.setEndpoint("endpoint");
			detail.setConditions(conditions.toString());
			String result = null;
			try {
				result = format(c, detail, conditions, true);

				logger.log(Level.INFO,
						String.format("case %s: { Assert.assertEquals(\"%s\",result); break; }", c.name(), result));
				switch (c) {
				case GI_GENERAL_INFORM_SECTION: {
					Assert.assertEquals("endpoint textValue", result);
					break;
				}
				case PC_MELTING_SECTION: {
					Assert.assertEquals(c.name(), "textValue", result);
					break;
				}

				case PC_BOILING_SECTION: {
					Assert.assertEquals(c.name(), "textValue (atm. pressure=  20  UNIT)", result);
					break;
				}
				case PC_GRANULOMETRY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (medium=  20  UNIT)", result);
					break;
				}

				case PC_PARTITION_SECTION: {
					Assert.assertEquals(c.name(), "textValue (ph=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}
				case PC_WATER_SOL_SECTION: {
					Assert.assertEquals(c.name(), "textValue (ph=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}
				case PC_DISSOCIATION_SECTION: {
					Assert.assertEquals(c.name(), "textValue (temperature=  20  UNIT)", result);
					break;
				}

				case PC_NON_SATURATED_PH_SECTION: {
					Assert.assertEquals(c.name(), "textValue (temperature=  20  UNIT)", result);
					break;
				}
				case PC_VAPOUR_SECTION: {
					Assert.assertEquals(c.name(), "textValue (temperature=  20  UNIT)", result);
					break;
				}

				case PC_SOL_ORGANIC_SECTION: {
					Assert.assertEquals(c.name(), "textValue (solvent=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}

				case TO_PHOTOTRANS_AIR_SECTION: {
					Assert.assertEquals(c.name(), "textValue (reactant=paramvalue ,test condition=  20  UNIT)",

							result);
					break;
				}
				case TO_HYDROLYSIS_SECTION: {
					Assert.assertEquals(c.name(), "textValue (ph=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}

				case TO_BIODEG_WATER_SIM_SECTION: {
					Assert.assertEquals(c.name(), "Half-life =textValue (paramvalue)", result);
					break;
				}
				case EN_STABILITY_IN_SOIL_SECTION: {
					Assert.assertEquals(c.name(), "Half-life =textValue (paramvalue)", result);
					break;
				}
				case EN_BIOACCUMULATION_SECTION: {
					Assert.assertEquals(c.name(), "Bioacc. type =textValue (route=paramvalue)", result);
					break;
				}
				case EN_BIOACCU_TERR_SECTION: {
					Assert.assertEquals("Bioacc. type =textValue", result);
					break;
				}
				case EN_ADSORPTION_SECTION: {
					Assert.assertEquals(c.name(), "Kp type =textValue", result);
					break;
				}
				case EN_HENRY_LAW_SECTION: {
					Assert.assertEquals(c.name(), "textValue", result);
					break;
				}
				case TO_ACUTE_ORAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (paramvalue)", result);
					break;
				}
				case TO_ACUTE_INHAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (paramvalue)", result);
					break;
				}
				case TO_ACUTE_DERMAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (paramvalue)", result);
					break;
				}
				case TO_SKIN_IRRITATION_SECTION: {
					Assert.assertEquals("endpoint =textValue (paramvalue)", result);
					break;
				}
				case TO_EYE_IRRITATION_SECTION: {
					Assert.assertEquals("endpoint =textValue (paramvalue)", result);
					break;
				}
				case TO_SENSITIZATION_SECTION: {
					Assert.assertEquals("endpoint =textValue (paramvalue)", result);
					break;
				}
				case TO_REPEATED_ORAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (paramvalue ,paramvalue)", result);
					break;
				}
				case TO_REPEATED_INHAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (paramvalue ,paramvalue)", result);
					break;
				}
				case TO_REPEATED_DERMAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (paramvalue ,paramvalue)", result);
					break;
				}
				case TO_CARCINOGENICITY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect type=  20  UNIT ,paramvalue)", result);
					break;
				}
				case TO_REPRODUCTION_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (generation=  20  UNIT ,paramvalue)",
							result);
					break;
				}
				case TO_DEVELOPMENTAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (  20  UNIT ,paramvalue)", result);
					break;
				}
				case EC_FISHTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (exposure=  20  UNIT)", result);
					break;
				}
				case EC_CHRONFISHTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (exposure=  20  UNIT)", result);
					break;
				}

				case EC_DAPHNIATOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (exposure=  20  UNIT)", result);
					break;
				}

				case EC_CHRONDAPHNIATOX_SECTION: {
					Assert.assertEquals("endpoint =textValue", result);
					break;
				}
				case EC_ALGAETOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (  20  UNIT ,exposure=  20  UNIT)", result);
					break;
				}
				case EC_BACTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_SEDIMENTDWELLINGTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_SOILDWELLINGTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_HONEYBEESTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_PLANTTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}

				case EC_SOIL_MICRO_TOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case AGGLOMERATION_AGGREGATION_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (medium=  20  UNIT)", result);
					break;
				}
				case CRYSTALLINE_PHASE_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (crystal system=  20  UNIT)",
							result);
					break;
				}
				case CRYSTALLITE_AND_GRAIN_SIZE_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (medium=  20  UNIT)", result);
					break;
				}
				case ASPECT_RATIO_SHAPE_SECTION: {
					Assert.assertEquals(c.name(),
							"endpoint =textValue [Guideline] (shape_descriptive=  20  UNIT ,x=  20  UNIT ,y=  20  UNIT ,z=  20  UNIT)",
							result);
					break;
				}
				case ZETA_POTENTIAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline] (medium=  20  UNIT ,ph=  20  UNIT)",
							result);
					break;
				}
				case SURFACE_CHEMISTRY_SECTION: {
					Assert.assertEquals(c.name(),
							"textValue [Guideline] (coating_description=  20  UNIT ,description=  20  UNIT ,element_or_group=  20  UNIT)",
							result);
					break;
				}

				case BAO_0003009_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case NPO_1709_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT ,time point=  20  UNIT)",
							result);
					break;
				}
				case NPO_1911_SECTION: {
					Assert.assertEquals("endpoint =textValue", result);
					break;
				}
				case TO_GENETIC_IN_VITRO_SECTION: {
					Assert.assertEquals(c.name(), "textValue [Guideline] (paramvalue)", result);
					break;
				}
				case TO_GENETIC_IN_VIVO_SECTION: {
					Assert.assertEquals(c.name(), "textValue [Guideline] (paramvalue)", result);
					break;
				}
				case TO_BIODEG_WATER_SCREEN_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =textValue [Guideline]", result);
					break;
				}
				case TO_SENSITIZATION_INSILICO_SECTION: {
					Assert.assertEquals(" [Guideline] (type of study=paramvalue)", result);
					break;
				}
				case TO_SENSITIZATION_INVITRO_SECTION: {
					Assert.assertEquals("Parameter =textValue [Guideline] (target gene=  20  UNIT)", result);
					break;
				}
				case TO_SENSITIZATION_INCHEMICO_SECTION: {
					Assert.assertEquals("Parameter =textValue [Guideline] (c_endpoint=  20  UNIT)", result);
					break;
				}
				case TO_SENSITIZATION_HUMANDB_SECTION: {
					Assert.assertEquals("NOEL =textValue", result);
					break;
				}
				case TO_SENSITIZATION_LLNA_SECTION: {
					Assert.assertEquals("EC3 =textValue (max dose tested=  20  UNIT) (p_vehicle_abbr=paramvalue)",
							result);
					break;
				}
				case PC_UNKNOWN_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (data_gathering_instruments=  20  UNIT) (paramvalue)",
							result);
					break;
				}
				case UNKNOWN_TOXICITY_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case ENM_0000044_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case NPO_1339_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case ENM_0000068_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case ENM_0000037_SECTION: {
					Assert.assertEquals(
							"endpoint =textValue [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				default: {

					if (c.name().startsWith("BAO_"))
						continue;
					if (c.name().startsWith("PUBCHEM_"))
						continue;
					if (c.name().startsWith("CELL_"))
						continue;
					if (c.name().startsWith("MITOCHONDRIAL_"))
						continue;
					if (c.name().startsWith("OXIDATIVE"))
						continue;
					if (c.name().startsWith("PROTEIN"))
						continue;
					if (c.name().startsWith("RECEPTOR"))
						continue;
					if (c.name().startsWith("REGULATION"))
						continue;
					if (c.name().startsWith("AUTOFLUORESCENCE"))
						continue;
					Assert.assertEquals("endpoint =textValue [Guideline]", result);
				}
				}
			} catch (Exception x) {

				if (f.inMatrix(f.getResultConfig(f.getCategoryConfig(c))))
					throw x;

			}
		}
	}

	@Test
	public void test_TO_GENETIC_IN_VITRO_SECTION() throws Exception {
		Protocol._categories c = Protocol._categories.TO_GENETIC_IN_VITRO_SECTION;
		IParams conditions = initConditions(c);
		ProtocolEffectRecordMatrix<String, String, String> detail = initDetail(c);

		detail.setInterpretationResult("negative");
		detail.setTextValue("negative");
		detail.setEndpoint("genotoxicity");
		detail.setConditions(conditions.toString());

		String result = null;

		result = format(c, detail, conditions, true);
		logger.log(Level.INFO,
				String.format("case %s: { Assert.assertEquals(\"%s\",result); break; }", c.name(), result));
		Assert.assertEquals("negative [Guideline] (paramvalue)", result);
	}

	@Test
	public void testNumericValues() throws Exception {
		for (Protocol._categories c : Protocol._categories.values()) {
			IParams conditions = initConditions(c);
			ProtocolEffectRecordMatrix<String, String, String> detail = initDetail(c);

			detail.setUnit("mg");
			detail.setLoValue(3.14);
			detail.setLoQualifier("ca.");
			detail.setEndpoint("endpoint");
			detail.setConditions(conditions.toString());

			String result = null;
			try {
				result = format(c, detail, conditions, false);
				logger.log(Level.INFO,
						String.format("case %s: { Assert.assertEquals(\"%s\",result); break; }", c.name(), result));

				switch (c) {
				case GI_GENERAL_INFORM_SECTION: {
					// check if "Physical state =" is ok
					break;
				}
				case PC_BOILING_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (atm. pressure=  20  UNIT)", result);
					break;
				}
				case PC_GRANULOMETRY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline] (medium=  20  UNIT)", result);
					break;
				}
				case AGGLOMERATION_AGGREGATION_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline] (medium=  20  UNIT)", result);
					break;
				}
				case PC_VAPOUR_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (temperature=  20  UNIT)", result);
					break;
				}
				case PC_PARTITION_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (ph=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}
				case PC_WATER_SOL_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (ph=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}
				case PC_SOL_ORGANIC_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (solvent=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}
				case PC_NON_SATURATED_PH_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (temperature=  20  UNIT)", result);
					break;
				}
				case PC_DISSOCIATION_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (temperature=  20  UNIT)", result);
					break;
				}
				case PC_UNKNOWN_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (data_gathering_instruments=  20  UNIT) (paramvalue)",
							result);
					break;
				}

				case TO_PHOTOTRANS_AIR_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (reactant=paramvalue ,test condition=  20  UNIT)",
							result);
					break;
				}
				case TO_HYDROLYSIS_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg (ph=  20  UNIT ,temperature=  20  UNIT)", result);
					break;
				}
				case TO_BIODEG_WATER_SCREEN_SECTION: {
					// OK, it assumes interpretation result is displayed only
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}

				case TO_BIODEG_WATER_SIM_SECTION: {
					Assert.assertEquals(c.name(), "Half-life =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case EN_STABILITY_IN_SOIL_SECTION: {
					Assert.assertEquals(c.name(), "Half-life =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case EN_BIOACCUMULATION_SECTION: {
					Assert.assertEquals(c.name(), "Bioacc. type =ca. 3.14  mg (route=paramvalue)", result);
					break;
				}
				case EN_BIOACCU_TERR_SECTION: {
					Assert.assertEquals(c.name(), "Bioacc. type =ca. 3.14  mg", result);
					break;
				}
				case EN_ADSORPTION_SECTION: {
					Assert.assertEquals(c.name(), "Kp type =ca. 3.14  mg", result);
					break;
				}
				case TO_ACUTE_ORAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case TO_ACUTE_INHAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case TO_ACUTE_DERMAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case TO_SKIN_IRRITATION_SECTION: {
					// OK, it assumes interpretation result is displayed only
					// and not even the guideline
					// added endpoint
					Assert.assertEquals("endpoint =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case TO_EYE_IRRITATION_SECTION: {
					// OK, it assumes interpretation result is displayed only
					// added endpoint
					Assert.assertEquals("endpoint =ca. 3.14  mg (paramvalue)", result);
					break;
				}
				case TO_SENSITIZATION_SECTION: {
					// OK, it assumes interpretation result is displayed only
					// added endpoint
					Assert.assertEquals("endpoint =ca. 3.14  mg (paramvalue)", result);
					break;

				}

				case TO_SENSITIZATION_INCHEMICO_SECTION: {
					Assert.assertEquals("Parameter =ca. 3.14  mg [Guideline] (c_endpoint=  20  UNIT)", result);
					break;
				}

				case TO_SENSITIZATION_HUMANDB_SECTION: {
					Assert.assertEquals("NOEL =ca. 3.14  mg", result);
					break;
				}
				case TO_SENSITIZATION_LLNA_SECTION: {
					Assert.assertEquals("EC3 =ca. 3.14  mg (max dose tested=  20  UNIT) (p_vehicle_abbr=paramvalue)",
							result);
					break;
				}
				case TO_SENSITIZATION_INVITRO_SECTION: {
					Assert.assertEquals("Parameter =ca. 3.14  mg [Guideline] (target gene=  20  UNIT)", result);
					break;
				}
				case TO_REPEATED_ORAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (paramvalue ,paramvalue)", result);
					break;
				}
				case TO_REPEATED_INHAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (paramvalue ,paramvalue)", result);
					break;
				}
				case TO_REPEATED_DERMAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (paramvalue ,paramvalue)", result);
					break;
				}
				case TO_CARCINOGENICITY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect type=  20  UNIT ,paramvalue)",
							result);
					break;
				}
				case TO_REPRODUCTION_SECTION: {
					Assert.assertEquals(c.name(),
							"endpoint =ca. 3.14  mg [Guideline] (generation=  20  UNIT ,paramvalue)", result);
					break;
				}
				case TO_DEVELOPMENTAL_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline] (  20  UNIT ,paramvalue)",
							result);
					break;
				}
				case EC_FISHTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (exposure=  20  UNIT)", result);
					break;
				}
				case EC_CHRONFISHTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (exposure=  20  UNIT)", result);
					break;
				}
				case EC_DAPHNIATOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (exposure=  20  UNIT)", result);
					break;
				}
				case EC_CHRONDAPHNIATOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg", result);
					break;
				}
				case EC_ALGAETOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (  20  UNIT ,exposure=  20  UNIT)", result);
					break;
				}
				case EC_BACTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_SEDIMENTDWELLINGTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_SOILDWELLINGTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_HONEYBEESTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_PLANTTOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}
				case EC_SOIL_MICRO_TOX_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg (effect=  20  UNIT ,exposure=  20  UNIT)",
							result);
					break;
				}

				case CRYSTALLINE_PHASE_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline] (crystal system=  20  UNIT)",
							result);
					break;
				}
				case CRYSTALLITE_AND_GRAIN_SIZE_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline] (medium=  20  UNIT)", result);
					break;
				}
				case ASPECT_RATIO_SHAPE_SECTION: {
					Assert.assertEquals(c.name(),
							"endpoint =ca. 3.14  mg [Guideline] (shape_descriptive=  20  UNIT ,x=  20  UNIT ,y=  20  UNIT ,z=  20  UNIT)",
							result);
					break;
				}
				case SPECIFIC_SURFACE_AREA_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}
				case ZETA_POTENTIAL_SECTION: {
					Assert.assertEquals(c.name(),
							"endpoint =ca. 3.14  mg [Guideline] (medium=  20  UNIT ,ph=  20  UNIT)", result);
					break;
				}
				case SURFACE_CHEMISTRY_SECTION: {
					Assert.assertEquals(c.name(),
							"ca. 3.14  mg [Guideline] (coating_description=  20  UNIT ,description=  20  UNIT ,element_or_group=  20  UNIT)",
							result);
					break;
				}
				case DUSTINESS_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}
				case POROSITY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}

				case POUR_DENSITY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}
				case PHOTOCATALYTIC_ACTIVITY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}
				case CATALYTIC_ACTIVITY_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}
				case UNKNOWN_TOXICITY_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case SUPPORTING_INFO_SECTION: {
					Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);
					break;
				}
				case TO_GENETIC_IN_VITRO_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  [Guideline] (paramvalue)", result);
					break;
				}
				case TO_GENETIC_IN_VIVO_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  [Guideline] (paramvalue)", result);
					break;
				}
				case PC_MELTING_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg", result);
					break;
				}
				case EN_HENRY_LAW_SECTION: {
					Assert.assertEquals(c.name(), "ca. 3.14  mg", result);
					break;
				}
				case NPO_1709_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT ,time point=  20  UNIT)",
							result);
					break;
				}
				case NPO_1911_SECTION: {
					Assert.assertEquals("endpoint =ca. 3.14  mg", result);
					break;
				}
				case ENM_0000044_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case NPO_1339_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case ENM_0000068_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				case ENM_0000037_SECTION: {
					Assert.assertEquals(
							"endpoint =ca. 3.14  mg [Guideline] (concentration=  20  UNIT ,replicate=  20  UNIT) (paramvalue ,paramvalue ,paramvalue ,paramvalue)",
							result);
					break;
				}
				default: {
					if (c.name().startsWith("BAO_"))
						continue;
					if (c.name().startsWith("PUBCHEM_"))
						continue;
					if (c.name().startsWith("CELL_"))
						continue;
					if (c.name().startsWith("MITOCHONDRIAL_"))
						continue;
					if (c.name().startsWith("OXIDATIVE"))
						continue;
					if (c.name().startsWith("PROTEIN"))
						continue;
					if (c.name().startsWith("RECEPTOR"))
						continue;
					if (c.name().startsWith("REGULATION"))
						continue;
					if (c.name().startsWith("AUTOFLUORESCENCE"))
						continue;
					if (c.name().startsWith("PROTEOMICS"))
						continue;

					if (f.inMatrix(f.getInterpretationResultConfig(f.getCategoryConfig(c)))
							&& !f.inMatrix(f.getResultConfig((f.getCategoryConfig(c))))) {
						Assert.assertEquals(c.name(), " [Guideline] (type of study=paramvalue)", result);
					} else
						Assert.assertEquals(c.name(), "endpoint =ca. 3.14  mg [Guideline]", result);

				}
				}
			} catch (Exception x) {

				if (f.inMatrix(f.getResultConfig(f.getCategoryConfig(c))))
					throw x;

			}

		}
	}
}
