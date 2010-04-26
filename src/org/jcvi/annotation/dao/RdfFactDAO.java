package org.jcvi.annotation.dao;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.jcvi.annotation.facts.FeatureProperty;
import org.jcvi.annotation.facts.GenomeProperty;
import org.jcvi.annotation.facts.PropertyRelationship;
import org.jcvi.annotation.facts.RelationshipType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class RdfFactDAO implements Iterable<Object> {

	// Ontology and instance prefixes
	private String ontologyNs = "urn:genome_properties:ontology:";
	private String instanceNs = "urn:genome_properties:instances:";
	private String rdfNs = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private Model model;
	private ArrayList<PropertyRelationship> relationships = new ArrayList<PropertyRelationship>();
	private HashSet<FeatureProperty> featureProperties = new HashSet<FeatureProperty>();
	private HashSet<GenomeProperty> genomeProperties = new HashSet<GenomeProperty>();

	// Used to test if our predicate is a relationship
	private ArrayList<String> relations = new ArrayList<String>(Arrays.asList("sufficient_for", "required_by", "part_of"));

	public RdfFactDAO(URL inUrl) {
		this(new RdfFileDAO(inUrl));
	}
	public RdfFactDAO(URL inUrl, String lang) {
		this(new RdfFileDAO(inUrl, lang));
	}

	public RdfFactDAO(String inFile) {
		this(new RdfFileDAO(inFile));
	}
	public RdfFactDAO(String inFile, String lang) {
		this(new RdfFileDAO(inFile, lang));
	}

	public RdfFactDAO(RdfFileDAO dao) {
		super();
		this.addDao(dao);
	}

	public void addRdf(URL inUrl) {
		this.addDao(new RdfFileDAO(inUrl));
	}
	public void addRdf(String inFile) {
		this.addDao(new RdfFileDAO(inFile));
	}

	public void addRdf(URL inUrl, String lang) {
		this.addDao(new RdfFileDAO(inUrl, lang));
	}
	public void addRdf(String inFile, String lang) {
		this.addDao(new RdfFileDAO(inFile, lang));
	}

	/*
	public HashSet<Property> getProperties() {
		return properties;
	}
	public void setProperties(HashSet<Property> properties) {
		this.properties = properties;
	}
	 */
	public HashSet<FeatureProperty> getFeatureProperties() {
		return featureProperties;
	}
	public void setProperties(HashSet<FeatureProperty> properties) {
		this.featureProperties = properties;
	}

	public ArrayList<PropertyRelationship> getRelationships() {
		return relationships;
	}
	public void setRelationships(ArrayList<PropertyRelationship> relationships) {
		this.relationships = relationships;
	}

	public int addDao(RdfFileDAO dao) {

		model = dao.getModel();
		System.out.println("Loading facts from RDF...");

		// Returns a count of all facts
		int total = 0;

		// Load Genome Properties
		int numGenomePropertiesBefore = this.getNumGenomeProperties();

		if (this.addGenomeProperties()) {
			int numGenomePropertiesAdded = this.getNumGenomeProperties() - numGenomePropertiesBefore;
			System.out.println(" " + numGenomePropertiesAdded + " genome properties");

			// Load Feature Properties
			int numFeaturePropertiesBefore = this.getNumFeatureProperties();
			if (this.addFeatureProperties()) {
				int numFeaturePropertiesAdded = this.getNumFeatureProperties() - numFeaturePropertiesBefore;
				System.out.println(" " + numFeaturePropertiesAdded + " feature properties");
				total = numGenomePropertiesAdded + numFeaturePropertiesAdded;
			}
		}
		System.out.println(" " + this.getRelationships().size() + " relationships");
		System.out.println(" " + this.getTotalFacts() + " total facts");

		return total;
	}

	
	private boolean addGenomeProperties() {

		boolean isSuccess = true;

		// Get our FeatureProperty node as a resource
		Resource resource = model.getResource(ontologyNs + "GenomeProperty");

		// This basically refers to our ":a" syntax
		Property propTypeOf = model.getProperty(rdfNs, "type");

		// Properties of a Genome Property resource
		Property propName 	= model.getProperty(ontologyNs + "name");

		// fetch all subjects that are a type of Genome Property
		Selector propSelector = new SimpleSelector(null, propTypeOf, resource);

		// Returns an iterator of statements about Genome Properties
		StmtIterator iter = model.listStatements(propSelector);

		if (iter.hasNext()) {
			while (iter.hasNext()) {
				Statement stmt = iter.nextStatement();

				// Get our subject resource from this rdf statement
				Resource subject = stmt.getSubject();

				// Get the name statement for our subject resource
				Statement sName = subject.getProperty(propName);

				if (sName == null) {
					System.err.println("A name is required for a Property.");
					isSuccess = false;

				} else {
					// Get the string identifying the resource of the subject of our name statement
					String propResourceId = sName.getSubject().toString();
					String name = sName.getString();
					
					// Create our GenomeProperty, and add it to our properties list
					GenomeProperty genomeProperty = GenomeProperty.create(name);
					genomeProperties.add(genomeProperty);

					// Set the rest of the properties
					StmtIterator propIter = subject.listProperties();
					while (propIter.hasNext()) {
						Statement propStmt = propIter.next();

						// Add all key-value pairs, except name, and
						// type urn:genome_properties:ontology:GenomeProperty statement
						String key = propStmt.getPredicate().getLocalName();
						RDFNode propNode = propStmt.getObject();
						String value = propNode.toString();
						if (key != null) {

							// Handle property relationships
							if (this.relations.contains(key)) {

								// Get the property that the object of this statement refers to
								String[] parentPropertyInfo = this.getPropertyInfo(propNode);

								if (parentPropertyInfo != null) {

									String propClass = parentPropertyInfo[0];
									String propId = parentPropertyInfo[1];
									Property parentProperty;
									
									if (propClass.equals("FeatureProperty") || propClass.equals("GenomeProperty")) {
										
										// Get our PropertyRelationship
										RelationshipType relation = getRelationshipType(key);
										if (relation != null) {
											if (propClass.equals("FeatureProperty")) {
												FeatureProperty parentFeatureProperty = FeatureProperty.create(propId);
												featureProperties.add(parentFeatureProperty);
												this.relationships.add(new PropertyRelationship(genomeProperty, relation, parentFeatureProperty));
	
											} else if (propClass.equals("GenomeProperty")) {
												GenomeProperty parentGenomeProperty = GenomeProperty.create(propId);
												genomeProperties.add(parentGenomeProperty);
												this.relationships.add(new PropertyRelationship(genomeProperty, relation, parentGenomeProperty));
											}
										}
									}
								} else {
									System.out.println("Unable to getProperty( " + propNode.toString() +" )");
								}
							}


							// TODO: use propNode, and figure out a more robust way 
							// to determine its value
							else if (key.equals("threshold")) {
								genomeProperty.setThreshold(value);
							}
							else if (!(key.equals("name") || (key.equals("type") && value.equals(ontologyNs + "GenomeProperty")))) {
								genomeProperty.put(key, value);
							}
						}
					}
				}
			}			

		} else {
			System.out.println("No statements were found in the database");
		}  
		return isSuccess;
	}

	private boolean addFeatureProperties() {

		boolean isSuccess = true;

		// Get our FeatureProperty node as a resource
		Resource resource = model.getResource(ontologyNs + "FeatureProperty");

		// This refers to our ":a" predicate in N3 syntax
		Property propTypeOf = model.getProperty(rdfNs, "type");

		// FeatureProperty requires a unique name 
		Property propName 	= model.getProperty(ontologyNs + "name");

		// Query the model for all Feature Property instances
		Selector propSelector = new SimpleSelector(null, propTypeOf, resource);

		// Returns an iterator of statements about Feature Properties
		StmtIterator iter = model.listStatements(propSelector);

		if (iter.hasNext()) {
			while (iter.hasNext()) {
				Statement stmt = iter.nextStatement();

				// Get our subject resource from this rdf statement
				Resource subject = stmt.getSubject();

				// Get the name statement for our subject resource
				Statement sName = subject.getProperty(propName);

				// Throw an error
				if (sName == null) {
					System.err.println("A name is required for a Property.");
					isSuccess = false;

				} else {
					// Get the string identifying the resource of the subject of our name statement
					// Eg. urn:genome_properties:instances:FeatureProperty_TIGR03006 urn:genome_properties:ontology:name "TIGR03006" .
					String propResourceId = sName.getSubject().toString();
					String name = sName.getString();

					// Create our FeatureProperty, and add it to our properties list
					FeatureProperty featureProperty = FeatureProperty.create(name);
					featureProperties.add(featureProperty);

					// Handle all of the statements about this FeatureProperty
					StmtIterator propIter = subject.listProperties();
					while (propIter.hasNext()) {
						Statement propStmt = propIter.next();

						// Add all key-value pairs, except name, and
						// type urn:genome_properties:ontology:GenomeProperty statement
						String key = propStmt.getPredicate().getLocalName();
						RDFNode propNode = propStmt.getObject();
						String value = propNode.toString();
						if (key != null) {

							// Handle property relationships
							if (this.relations.contains(key)) {

								// Get the property that the object of this statement refers to
								String[] parentPropertyInfo = this.getPropertyInfo(propNode);

								if (parentPropertyInfo != null) {

									String propClass = parentPropertyInfo[0];
									String propId = parentPropertyInfo[1];
									Property parentProperty;
									
									if (propClass.equals("FeatureProperty") || propClass.equals("GenomeProperty")) {
										
										// Get our PropertyRelationship
										RelationshipType relation = getRelationshipType(key);
										if (relation != null) {
											if (propClass.equals("FeatureProperty")) {
												FeatureProperty parentFeatureProperty = FeatureProperty.create(propId);
												featureProperties.add(parentFeatureProperty);
												this.relationships.add(new PropertyRelationship(featureProperty, relation, parentFeatureProperty));
	
											} else if (propClass.equals("GenomeProperty")) {
												GenomeProperty parentGenomeProperty = GenomeProperty.create(propId);
												genomeProperties.add(parentGenomeProperty);
												this.relationships.add(new PropertyRelationship(featureProperty, relation, parentGenomeProperty));
											}
										}
									}
								} else {
									System.out.println("Unable to getProperty( " + propNode.toString() +" )");
								}
							}

							// this requires thresholds to be defined as a String
							else if (key.equals("threshold")) {
								System.out.println("threshold " + value);
								featureProperty.setThreshold(value);
							}
							else if (!(key.equals("name") || (key.equals("type") && value.equals(ontologyNs + "GenomeProperty")))) {
								featureProperty.put(key, value);
							}
						}
					}
				}
			}			

		} else {
			System.out.println("No statements were found in the database");
		}  
		return isSuccess;
	}

	private RelationshipType getRelationshipType(String key) {
		if (key.equals("sufficient_for")) {
			return RelationshipType.SUFFICIENT_FOR;
		}
		else if (key.equals("required_by")) {
			return RelationshipType.REQUIRED_BY;
		}
		else if (key.equals("part_of")) {
			return RelationshipType.PART_OF;
		}
		System.out.println("ERROR: " + key + " relation not found");
		return null;
	}
	
	private String[] getPropertyInfo (RDFNode propNode) {

		// Get the id and class (FeatureProperty or GenomeProperty) 
		// that this resource refers to
		String propInfo[] = new String[2];

		// type and name properties
		Property propTypeOf = model.getProperty(rdfNs, "type");
		Property propName = model.getProperty(ontologyNs + "name");

		String propNodeId = propNode.toString();
		if (model.containsResource(propNode)) {

			// Get the resource for this property
			// urn:genome_properties:instances:*Property_63239, or
			// gp:GenomeProperty_66644
			Resource propResource = model.getResource(propNodeId);

			// We expect that our Property Resource has a type statement
			if (model.contains(propResource, propTypeOf)) {

				// List the values of the type property of a resource
				NodeIterator propTypeNodes = model.listObjectsOfProperty(propResource, propTypeOf);
				while (propTypeNodes.hasNext()) {
					RDFNode propTypeNode = propTypeNodes.next();

					// Get the name statement for our subject resource
					Statement nameStmt = propResource.getProperty(propName);

					// Get the id for this property
					propInfo[1] = nameStmt.getString();

					// Get the class (FeatureProperty or GenomeProperty)
					if (propTypeNode.toString().equals(ontologyNs + "FeatureProperty")) {
						propInfo[0] = "FeatureProperty";
					}
					else if (propTypeNode.toString().equals(ontologyNs + "GenomeProperty"))
					{
						propInfo[0] = "GenomeProperty";
					} 
					else
					{
						System.out.println("Error: Unrecognized resource class " + propTypeNode.toString());
					}
				}

			} else {
				System.out.println("Error: Type property is undefined for resource " + propResource.toString());
			}
		}

		return propInfo;
	}

	// Testing purposes only
	public void testListStatements(Resource resource) {
		System.out.println("  testListStatements( " + resource.toString() + " )");
		Selector s = new SimpleSelector(resource, null, (RDFNode) null);
		StmtIterator iter = model.listStatements(s);
		while (iter.hasNext()) {
			Statement stmt = iter.next();
			System.out.println("   statement: " + stmt.toString());

		}
	}

	public ArrayList<Object> facts() {
		ArrayList<Object> facts = new ArrayList<Object>();
		facts.addAll(featureProperties);
		facts.addAll(genomeProperties);
		facts.addAll(relationships);
		return facts;
	}

	public int getTotalFacts() {
		return featureProperties.size() + genomeProperties.size() + relationships.size();
	}

	public int getNumFeatureProperties() {
		return featureProperties.size();
	}

	public int getNumGenomeProperties() {
		return genomeProperties.size();
	}
	public Iterator<Object> iterator() {
		return this.facts().iterator();
	}
}
