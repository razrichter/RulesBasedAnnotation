package org.jcvi.annotation.facts;

import java.util.ArrayList;
import java.util.List;

import org.jcvi.annotation.facts.Annotation;

public class Feature {
	private String featureId;
	private Taxon taxon;
	private Feature source;
	private String name;
	private int start;
	private int end;
	private int strand;
	private String type; // Feature type
	private Annotation assignedAnnotation;
	private List<Annotation> assertedAnnotations = new ArrayList<Annotation>();
	// private List<Rule> firedRules = new ArrayList<String>();
		
	public Feature(String featureId, String type) {
		super();
		this.featureId = featureId;
		this.type = type;
	}
	
	// Constructor with coordinates
	public Feature(String featureId, String type, int start, int end, int strand) {
		super();
		this.featureId = featureId;
		this.type = type;
		this.start = start;
		this.end = end;
		this.strand = strand;
	}

	// Constructor with source molecule
	public Feature(String featureId, String type, int start, int end, int strand,
			String name) {
		this(featureId, type, start, end, strand);
		this.setName(name);
	}
	
	public Taxon getTaxon() {
		return taxon;
	}
	public void setTaxon(Taxon taxon) {
		this.taxon = taxon;
	}

	public Annotation getAssignedAnnotation() {
		return assignedAnnotation;
	}

	public void setAssignedAnnotation(Annotation assignedAnnotation) {
		this.assignedAnnotation = assignedAnnotation;
	}

	public List<Annotation> getAssertedAnnotations() {
		return assertedAnnotations;
	}

	public void addAssertedAnnotation(Annotation annotation) {
		this.assertedAnnotations.add(annotation);
	}

	public void removeAssertedAnnotation(Annotation annotation) {
		this.assertedAnnotations.remove(annotation);
	}
	
	public String getFeatureId() {
		return featureId;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Feature getSource() {
		return source;
	}

	public void setSource(Feature source) {
		this.source = source;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public int getStrand() {
		return strand;
	}

	public int getLength() {
		return end - start;
	}
	public void setStrand(int strand) {
		this.strand = strand;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String toString() {
		return this.type + "|" + this.featureId;
	}

	// @Override equals method
	public boolean equals(Object o) {
		
		if (o instanceof Feature) {
			
			Feature f = (Feature) o;
			if (f.getFeatureId().equals(featureId) &&
					f.getType().equals(type)) {
				return true;
			}
		}
		
		return false;
	}

}
