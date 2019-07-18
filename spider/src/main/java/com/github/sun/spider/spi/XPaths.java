package com.github.sun.spider.spi;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

public class XPaths {
  private final static XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
  private final Node node;
  private final String xpath;

  private XPaths(Node node, String xpath) {
    this.node = node;
    this.xpath = xpath;
  }

  public static XPaths of(Node node, String xpath) {
    return new XPaths(node, xpath);
  }

  public String asText() {
    try {
      return xPath.evaluate(xpath, node);
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + xpath, ex);
    }
  }

  public int asInt() {
    try {
      return ((Double) xPath.evaluate(xpath, node, XPathConstants.NUMBER)).intValue();
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + xpath, ex);
    }
  }

  public double asDouble() {
    try {
      return (Double) xPath.evaluate(xpath, node, XPathConstants.NUMBER);
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + xpath, ex);
    }
  }

  public long asLong() {
    try {
      return ((Double) xPath.evaluate(xpath, node, XPathConstants.NUMBER)).longValue();
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + xpath, ex);
    }
  }

  public boolean asBoolean() {
    try {
      return (Boolean) xPath.evaluate(xpath, node, XPathConstants.BOOLEAN);
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + xpath, ex);
    }
  }

  public Node as() {
    try {
      return (Node) xPath.evaluate(this.xpath, node, XPathConstants.NODE);
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + this.xpath, ex);
    }
  }

  public List<Node> asArray() {
    try {
      NodeList nodes = (NodeList) xPath.evaluate(this.xpath, node, XPathConstants.NODESET);
      List<Node> list = new ArrayList<>();
      for (int i = 0; i < nodes.getLength(); i++) {
        list.add(nodes.item(i));
      }
      return list;
    } catch (XPathExpressionException ex) {
      throw new IllegalArgumentException("incorrect xpath: " + this.xpath, ex);
    }
  }
}
