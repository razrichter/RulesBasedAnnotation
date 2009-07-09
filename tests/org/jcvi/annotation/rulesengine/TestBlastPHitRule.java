package org.jcvi.annotation.rulesengine;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.drools.builder.ResourceType;
import org.jcvi.annotation.facts.Annotation;
import org.jcvi.annotation.facts.BlastHit;
import org.jcvi.annotation.facts.Feature;
import org.junit.After;
import org.junit.Before;

public class TestBlastPHitRule extends TestCase {

	private RulesEngine engine;
	private Feature orf;
	private Annotation ann;
	
	@Before
	public void setUp() throws Exception {
		
		engine = new RulesEngine();
		URL url = engine.getClass().getResource("../rules/SampleBlastHit.drl");
		engine.addResource(url.toString(), ResourceType.DRL);

		orf = new Feature("testorf", "ORF", 0, 110, 1);
		
		// Only hit1 should evaluate true according to our SampleBlastHit rule
		BlastHit hit1 = new BlastHit("blastp","testorf", "RF|NP_844922.1", 0.001, 170, 170, 0.002,
				100, 200, 1, 110, 100, 205, 1, 95.0, 82.0);
		hit1.setQueryLength(orf.getLength());
		BlastHit hit2 = new BlastHit("blastp","testorf", "RF|NOT_IT.1", 0.001, 170, 170, 0.002,
				100, 200, 1, 110, 100, 205, 1, 95.0, 82.0);
        hit2.setQueryLength(orf.getLength());
		BlastHit hit3 = new BlastHit("blastp","testorf", "RF|NOT_IT.1", 0.001, 170, 170, 0.002,
				100, 200, 1, 1000, 100, 205, 1, 95.0, 82.0);
        hit2.setQueryLength(orf.getLength());
		
		engine.addFact(orf);
		engine.addFact(hit1);
		engine.addFact(hit2);
		engine.addFact(hit3);
		engine.fireAllRules();
		this.ann = orf.getAssertedAnnotations().get(0);
	}
	
	public void testSampleBlastCommonName() {
		assertEquals(ann.getCommonName(), "exosporium protein K");
	}
	public void testSampleBlastGeneSymbol() {
		assertEquals(ann.getGeneSymbol(), "exsK");
	}
	
	public void testSampleBlastGeneSymbolFails2() {
		assertFalse(ann.getGeneSymbol() == "not_gene_symbol");
	}

	public void testSampleBlastEcNumber() {
		assertEquals(ann.getEcNumber(), null);
	}
	public void testSampleBlastGoIds() {
		ArrayList<String> goIds = new ArrayList<String>();
		goIds.add("GO:0043592"); 
		goIds.add("GO:0003674");
		goIds.add("GO:0008150");
		assertEquals(goIds, ann.getGoIds());
	}
	public void testSampleBlastRoleIds() {
		List<String> roleIds = ann.getRoleIds();
		assertTrue(roleIds.contains("705"));
	}
	
	public void testSampleBlastGeneSymbolFails() {
		assertFalse(ann.getGeneSymbol() == "not_gene_symbol");
	}
	
	public void testSampleBlastSpecificity() {
		 assertEquals(ann.getSpecificity(), Annotation.EQUIVALOG);
	}

	public void testSampleBlastAssertionType() {
		assertEquals(ann.getAssertionType(), Annotation.EXACT);
	}
	
	public void testSampleBlastConfidence() {
		assertEquals(ann.getConfidence(), 80.0);
	}
	
	@After
	public void tearDown() throws Exception {
		engine = null;
	}

}
