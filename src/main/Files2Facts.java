
package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import industryStandard.AML;
import uni.bonn.krextor.Krextor;
import util.ConfigManager;
import util.StringUtil;

/**
 * Reads the RDF files and convert them to Datalog facts
 * 
 * @author Irlan 28.06.2016
 */
public class Files2Facts {
	public RDFNode object;
	public RDFNode predicate;
	public RDFNode subject;
	public ArrayList<File> files;
	public Model model;
	private LinkedHashSet<String> subjectsToWrite;

	/**
	 * Converts the file to turtle format based on Krextor
	 * 
	 * @param input
	 * @param output
	 */
	public void convertRdf() {
		int i = 0;
		for (File file : files) {
			if (file.getName().endsWith(".aml")) {
				Krextor krextor = new Krextor();
				krextor.convertRdf(file.getAbsolutePath(), "aml", "turtle",
						ConfigManager.getFilePath() + "plfile" + i + ".ttl");
			} else {
				RDFTransformer convert = new RDFTransformer();

				// give input and output
				convert.transform(file.getAbsolutePath(),
						ConfigManager.getFilePath() + "plfile" + i + ".ttl");

			}

			i++;
		}
	}

	/**
	 * Read the RDF files of a given path
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public ArrayList<File> readFiles(String path, String type, String type2, String type3)
			throws Exception {
		files = new ArrayList<File>();
		File originalFilesFolder = new File(path);
		if (originalFilesFolder.isDirectory()) {
			for (File amlFile : originalFilesFolder.listFiles()) {
				if (amlFile.isFile()
						&& (amlFile.getName().endsWith(type) || amlFile.getName().endsWith(type2)
								|| amlFile.getName().endsWith(type3))) {
					if (amlFile.getName().endsWith(".aml")) {
						String name = amlFile.getName().replace(".aml", "");
						if (name.endsWith("0") || name.endsWith("1")) {
							files.add(amlFile);
						}
					}

					else if (amlFile.getName().endsWith(".opcua")) {
						String name = amlFile.getName().replace(".opcua", "");
						if (name.endsWith("0") || name.endsWith("1")) {
							files.add(amlFile);
						}
					}

					else if (amlFile.getName().endsWith(".xml")) {
						String name = amlFile.getName().replace(".xml", "");
						if (name.endsWith("0") || name.endsWith("1")) {
							files.add(amlFile);
						}
					}

					else {
						files.add(amlFile);
					}
				}
			}
		} else {
			System.out.println("Error in the directory that you provided");
			System.exit(0);
		}
		return files;
	}

	/**
	 * This function reads the RDF files and extract their contents for creating
	 * PSL predicates.
	 * 
	 * @param file
	 * @param number
	 * @param standard
	 * @return
	 * @throws Exception
	 */
	public String createModel(File file, int number, String standard, AML aml) throws Exception {

		InputStream inputStream = FileManager.get().open(file.getAbsolutePath());
		Model model = ModelFactory.createDefaultModel();
		model.read(new InputStreamReader(inputStream), null, "TURTLE");
		subjectsToWrite = new LinkedHashSet<String>();
		switch (standard) {

		case "aml":
			aml.setModel(model);
			aml.setNumber(number);
			aml.addsDataforAML(); // process required data for AML

			writeData(aml);
		}
		return "";
	}

	/**
	 * This function create AML model files. Files are created based on key in
	 * the hashMap. *
	 * 
	 * @param aml
	 * @throws FileNotFoundException
	 */
	private void writeData(AML aml) throws FileNotFoundException {
		try {
			Set<String> predicates = aml.generic.keySet();
			// gets predicates to name the data files
			for (String i : predicates) {
				// name files as predicates
				PrintWriter documentwriter = new PrintWriter(
						ConfigManager.getFilePath() + "Alligator/model/" + i + ".txt");
				Collection<String> values = aml.generic.get(i);
				// for every predicate get its value
				for (String val : values) {
					documentwriter.println(val);
				}

				documentwriter.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the turtle format RDF files and extract the contents for data log
	 * conversion.
	 * 
	 * @param file
	 * @param number
	 * @return
	 * @throws Exception
	 */
	public String factsFromFiles(File file, int number) throws Exception {

		StringBuilder buf = new StringBuilder();

		InputStream inputStream = FileManager.get().open(file.getAbsolutePath());

		Model model = null;
		model = ModelFactory.createDefaultModel();

		// parses in turtle format
		model.read(new InputStreamReader(inputStream), null, "TURTLE");

		StmtIterator iterator = model.listStatements();

		while (iterator.hasNext()) {
			Statement stmt = iterator.nextStatement();
			subject = stmt.getSubject();
			predicate = stmt.getPredicate();
			object = stmt.getObject();

			buf.append("clause1(")
					.append(StringUtil.lowerCaseFirstChar(predicate.asNode().getLocalName()))
					.append("(")
					.append(StringUtil.lowerCaseFirstChar(subject.asNode().getLocalName()) + number)
					.append(",");
			if (object.isURIResource()) {
				object = model.getResource(object.as(Resource.class).getURI());
				String objectStr = object.asNode().getLocalName();
				if (predicate.asNode().getLocalName().toString().equals("type")) {
					buf.append(StringUtil.lowerCaseFirstChar(objectStr));
				} else {
					buf.append(StringUtil.lowerCaseFirstChar(objectStr) + number);
				}

			} else {
				if (object.isLiteral()) {
					buf.append("'" + object.asLiteral().getLexicalForm() + "'");

				} else {
					buf.append(object);

				}
			}

			buf.append("),true).");
			buf.append(System.getProperty("line.separator"));

		}

		return buf.toString();
	}

	/**
	 * Adds aml Values
	 * 
	 * @param amlList
	 * @param amlValue
	 * @param aml
	 * @return
	 */
	HashMap<String, String> addAmlNegValues(ArrayList<?> amlList, HashMap<String, String> amlValue,
			String aml, String predicate, ArrayList<?> type, HashMap<String, String> pred) {
		for (int i = 0; i < amlList.size(); i++) {
			StmtIterator iterator = model.listStatements();
			while (iterator.hasNext()) {
				Statement stmt = iterator.nextStatement();
				subject = stmt.getSubject();

				if (subject.asResource().getLocalName().equals(amlList.get(i))) {
					String value = getValue(subject, predicate);
					if (value != null && !value.contains("eClassIRDI")
							&& !value.contains("eClassClassificationClass")
							&& !value.contains("eClassVersion")) {
						amlValue.put(aml + value, type.get(i).toString());
						pred.put(aml + value, predicate);

						iterator.close();
						break;
					}
				}
			}
		}
		return amlValue;
	}

	/**
	 * get predicate Value
	 * 
	 * @param name
	 * @return
	 */
	String getValue(RDFNode name, String predicate) {
		String type = null;
		StmtIterator stmts = model.listStatements(name.asResource(), null, (RDFNode) null);
		while (stmts.hasNext()) {
			Statement stmte = stmts.nextStatement();

			if (stmte.getPredicate().asNode().getLocalName().toString().equals(predicate)) {
				type = stmte.getObject().asLiteral().getLexicalForm();
			}

		}
		return type;
	}

	/**
	 * Generate all the files of a given folder
	 * 
	 * @throws Exception
	 */
	public void generateExtensionalDB(String path) throws Exception {
		int i = 1;
		StringBuilder buf = new StringBuilder();
		for (File file : files) {
			buf.append(factsFromFiles(file, i++));
		}
		PrintWriter prologWriter = new PrintWriter(new File(path + "Alligator/model/edb.pl"));
		prologWriter.println(buf.toString().replaceAll("\\\\", ""));
		prologWriter.close();
	}

	/**
	 * Creates temporary files which holds the path for edb.pl and output.txt
	 * These files are necessary for evalAML.pl so that the path is
	 * automatically set from config.ttl
	 * 
	 * @throws FileNotFoundException
	 */
	public void prologFilePath() throws FileNotFoundException {
		new File(ConfigManager.getFilePath() + "Alligator/Precision").mkdirs();

		new File(ConfigManager.getFilePath() + "Alligator/model").mkdirs();
		PrintWriter prologWriter = new PrintWriter(
				new File(System.getProperty("user.dir") + "/resources/files/edb.txt"));

		prologWriter.println("'" + ConfigManager.getFilePath() + "Alligator/model/edb.pl" + "'.");
		prologWriter.close();

		prologWriter = new PrintWriter(
				new File(System.getProperty("user.dir") + "/resources/files/output.txt"));
		prologWriter.println("'" + ConfigManager.getFilePath() + "Alligator/output.txt" + "'.");
		prologWriter.close();

	}

	/**
	 * This function Generates Alligator Evaluation model.
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void generateModel(String path) throws Exception {
		int i = 1;
		AML aml = new AML();
		for (File file : files) {
			// pass in the writers
			if (!file.getName().equals("seed.ttl")) {
				createModel(file, i++, "aml", aml);
			}
		}
	}

}