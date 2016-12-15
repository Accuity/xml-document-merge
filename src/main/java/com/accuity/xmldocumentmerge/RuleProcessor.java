package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Id;
import com.accuity.xmldocumentmerge.model.Rule;
import com.accuity.xmldocumentmerge.model.Source;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.*;
import java.util.logging.Logger;

public class RuleProcessor {
	private final static  Logger LOG = Logger.getLogger(RuleProcessor.class.getName());
	private final DocumentNodeMerger documentNodeMerger = new DocumentNodeMerger();
	private final XPathFactory xPathfactory = XPathFactory.newInstance();
	private final XPath xpath = xPathfactory.newXPath();

	public Document processRule(Rule rule, Document trustedDocument, Map<String, Document> sourceDocuments) {
		List<Source> ruleSources = calculateSourcesToUseForRule(rule);

		// docroot rule
		if (rule.getParentRule() == null) {
			// at the doc root
			for (Source source : ruleSources) {
				if (sourceDocuments.containsKey(source.getName())) {
					if (trustedDocument.getDocumentElement() == null) {
						Node newRootNode = trustedDocument.importNode(sourceDocuments.get(source.getName()).getDocumentElement(), true);
						trustedDocument.appendChild(newRootNode);
					} else {
						if (rule.getField() == null || !rule.getField().isStop()) {
							documentNodeMerger.mergeNodeChildren(trustedDocument.getDocumentElement(), sourceDocuments.get(source.getName()).getDocumentElement());
						}
					}
				}
			}
		} else {
			try {
				// simple field / sub-field / coalesce

				// xpath for rule
				LOG.fine("processing rule for xpath: " + rule.getFullContextXPath());
				XPathExpression ruleXpath = xpath.compile(rule.getFullContextXPath());

				// find the node in trusted doc
				NodeList nl = (NodeList) ruleXpath.evaluate(trustedDocument, XPathConstants.NODESET);
				LOG.fine(nl.getLength() + " existing matching nodes found in trusted document.");
				// remove nodes from trusted doc
				// this assumes all matches have the same parent
				Node trustedParentNode = null;
				for (int i = 0; i < nl.getLength(); i++) {
					trustedParentNode = nl.item(i).getParentNode();
					if (trustedParentNode != null) {
						trustedParentNode.removeChild(nl.item(i));
					}
				}

				if (trustedParentNode == null) {
					// remove filter and try to find nodes. if we do, use those nodes' parent
					if (rule.getFilter() != null && !rule.getFilter().isEmpty()) {
						String filterlessXpath = rule.getParentRule().getFullContextXPath() + "/" + rule.getContext();
						XPathExpression filterlessXpathExpression = xpath.compile(filterlessXpath);
						NodeList filterlessParentNodes = (NodeList) filterlessXpathExpression.evaluate(trustedDocument, XPathConstants.NODESET);
						LOG.fine(filterlessParentNodes.getLength() + " nodes found by removing filter: " + filterlessXpath);
						if (filterlessParentNodes.getLength() > 0) {
							trustedParentNode = filterlessParentNodes.item(0).getParentNode();
						}
					}
				}

				if (trustedParentNode == null) {
					// this should only happen when a rule is a child of a stop rule
					// OR when a subrule has a trusted source which contains data on a node, and that node does not exist in any of the sources in the parent rules
					LOG.info("no parent node found in trusted document for xpath " + rule.getFullContextXPath() + ". skipping rule.");
				} else {                	
					//pre-process coalesce trusted document rule
					Map<String, Node> coalesceMatches = new HashMap<>();
					if (rule.getField() != null && rule.getField().isCoalesce()) {
						if (rule.getField().getIds() != null) {
							// coalesce match rules
							for (int j = 0; j < nl.getLength(); j++) {
								String key = buildKeyForNode(nl.item(j), rule.getField().getIds());
								LOG.fine("found node with key: " + key);
								coalesceMatches.put(key, nl.item(j));
							}
						}
					}


					// process sources in order of trustworthyness
					for (Source source : ruleSources) {
						int startingNumberOfTrustedNodes = ((NodeList) ruleXpath.evaluate(trustedDocument, XPathConstants.NODESET)).getLength();
						String trustedSourceName = source.getName();
						if (sourceDocuments.containsKey(trustedSourceName)) {
							Document sourceDocument = sourceDocuments.get(trustedSourceName);
							// get nodes from this source matching the rule xpath
							NodeList sourceNodes = (NodeList) ruleXpath.evaluate(sourceDocument, XPathConstants.NODESET);
							LOG.fine(sourceNodes.getLength() + " matching nodes found in " + source.getName() + " document.");
							if (sourceNodes.getLength() > 0) {
								// there might be multiple matches
								for (int i = 0; i < sourceNodes.getLength(); i++) {
									boolean ignore = false;
									String newKey = null;

									Node sourceNode = sourceNodes.item(i);
									Node matchingTrustedNode = null;
									if (rule.getField() != null && rule.getField().isCoalesce()) {
										if (rule.getField().getIds() != null) {
											// coalesce match rules
											newKey = buildKeyForNode(sourceNode, rule.getField().getIds());
											LOG.fine("Key for foreign node is " + newKey);

											if (coalesceMatches.containsKey(newKey)) {
												LOG.fine("matching key found. should merge instead of coalesce");
												matchingTrustedNode = coalesceMatches.get(newKey);
											}
										}
									} else {
										if (startingNumberOfTrustedNodes > 0 && startingNumberOfTrustedNodes < i + 1) {
											// if there are more copies of this node in the source than in trusted, ignore the extras
											ignore = true;
										} else {
											// fall back on matching based on xpath match and position
											NodeList trustedNodes = (NodeList) ruleXpath.evaluate(trustedDocument, XPathConstants.NODESET);
											matchingTrustedNode = trustedNodes.item(i);
										}

									}

									if (matchingTrustedNode == null) {
										if (!ignore) {
											LOG.fine("importing " + sourceNode.getNodeName() + " node from " + source.getName());
											Node newNode = trustedDocument.importNode(sourceNode, true);
											newNode = trustedParentNode.appendChild(newNode);
											//add new trusted node to coalesce map
											if (newKey != null) {
												coalesceMatches.put(newKey, newNode);
											}
										}
									} else {
										if (rule.getField() == null || !rule.getField().isStop()) {
											LOG.fine("merging " + sourceNode.getNodeName() + " node from " + source.getName());
											documentNodeMerger.mergeNodeChildren(matchingTrustedNode, sourceNode);
										}
									}
								}
							}
						}
					}
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}


		// process child rules
		boolean processChildRules = rule.getRules() != null;
		// if there are no trusted documents according to the trust levels of this rule, don't bother processing child rules.
		boolean hasTrustedDocument = false;
		for (Source source : ruleSources) {
			if (sourceDocuments.containsKey(source.getName())) {
				hasTrustedDocument = true;
			}
		}
		if (!hasTrustedDocument) {
			processChildRules = false;
		}
		if (processChildRules) {
			// according to ben, stop rules should never have sub rules, so to avoid unexpected behavior, we should not process them.
			if (rule.getField() == null || !rule.getField().isStop()) {
				for (Rule subRule : rule.getRules()) {
					processRule(subRule, trustedDocument, sourceDocuments);
				}
			} else {
				LOG.warning("stop rule at " + rule.getFullContextXPath() + " has children. Ignoring these.");
			}
		}
		return trustedDocument;
	}

	/**
	 * builds a key for a node. This is used for matching nodes of the same name
	 * @param node
	 * @param ids
	 * @return
	 * @throws XPathExpressionException
	 */
	String buildKeyForNode(Node node, List<Id> ids) throws XPathExpressionException {
		StringBuilder compoundKey = new StringBuilder();
		for (Id id : ids) {
			String idXPath = id.getPath();
			XPathExpression idPathExpression = xpath.compile(idXPath);
			NodeList keyNodes = (NodeList) idPathExpression.evaluate(node, XPathConstants.NODESET);
			Node firstKeyNode = keyNodes.item(0);
			if (firstKeyNode != null) {
				if (firstKeyNode.getNodeType() == Node.ATTRIBUTE_NODE) {
					compoundKey.append(firstKeyNode.getNodeValue());
				} else {
					compoundKey.append(firstKeyNode.getTextContent());
				}
			}
			compoundKey.append(";");
		}
		return compoundKey.toString();
	}

	List<Source> calculateSourcesToUseForRule(Rule rule) {
		List<Source> sources = null;
		Rule ruleToUse = rule;
		while (sources == null && ruleToUse != null) {
			if (ruleToUse.getWeighting() != null && ruleToUse.getWeighting().getSources() != null) {
				sources = ruleToUse.getWeighting().getSources();
			} else {
				if (ruleToUse.getParentRule() != null) {
					ruleToUse = ruleToUse.getParentRule();
				} else {
					sources = new ArrayList<>();
				}
			}
		}

		List<Source> trustedSources = new ArrayList<>();

		for (Source source : sources) {
			if (source.getTrust() > 0) {
				trustedSources.add(source);
			}
		}

		Collections.sort(trustedSources);
		return trustedSources;

	}
}
