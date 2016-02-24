package com.accuity.xmldocumentmerge;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DocumentNodeMerger {

    /**
     * recursively merges the children from a node, to a destination node
     *
     * @param destinationNode
     * @param sourceNode
     */
    public void mergeNodeChildren(Node destinationNode, Node sourceNode) {
        NodeList sourceChildren = sourceNode.getChildNodes();
        Set<String> newNodeNames = new HashSet<>();
        Map<String, Integer> nameCount = new HashMap<>();
        for (int i = 0; i < sourceChildren.getLength(); i++) {
            Node childNode = sourceChildren.item(i);
            int nameOrdinal = nameCount.containsKey(childNode.getNodeName()) ? nameCount.get(childNode.getNodeName()) : 0;
            Node destinationChildNode = getChildByName(destinationNode, childNode.getNodeName(), nameOrdinal);
            if (destinationChildNode == null) {
                // if nameOrdinal > 0 and the node name existed in the original form of the destination document,
                // that means this is an extra instance of this type of node. ignore it
                if (nameOrdinal == 0 || newNodeNames.contains(childNode.getNodeName())) {
                    // this is our first node by this name. append it
                    Node importedNode = destinationNode.getOwnerDocument().importNode(childNode, true);
                    destinationNode.appendChild(importedNode);
                    newNodeNames.add(importedNode.getNodeName());
                }
            } else {
                mergeNodeChildren(destinationChildNode, childNode);
            }
            nameCount.put(childNode.getNodeName(), nameOrdinal + 1);
        }
    }

    private Node getChildByName(Node node, String name, int ordinal) {
        Node childNode = null;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals(name) && childNode == null) {
                ordinal--;
                if (ordinal < 0) {
                    childNode = children.item(i);
                }
            }
        }
        return childNode;
    }

}
