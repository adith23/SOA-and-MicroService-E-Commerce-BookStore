package com.globalbooks.adapter.endpoint;

import com.globalbooks.adapter.client.OrdersRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Endpoint
public class OrdersEndpoint {

    private static final String NAMESPACE_URI = "http://globalbooks.com/order-contract/v1";

    @Autowired
    private OrdersRestClient ordersRestClient;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createOrderRequest")
    @ResponsePayload
    public Element handleCreateOrderRequest(@RequestPayload Element requestElement) throws Exception {
        System.out.println("\n[OrdersAdapterService] ======= INCOMING SOAP REQUEST TO ADAPTER =======");
        System.out.println("[OrdersAdapterService] Received createOrder HTTP POST from BPEL Engine");
        
        // 1. Convert XML DOM to Java Map (JSON representation)
        Map<String, Object> jsonPayload = new HashMap<>();
        
        String customerId = getChildElementText(requestElement, "customerId");
        jsonPayload.put("customerId", customerId);
        
        String totalAmount = getChildElementText(requestElement, "totalAmount");
        if (totalAmount != null) {
            jsonPayload.put("totalAmount", Double.parseDouble(totalAmount));
        }

        // Parse items
        List<Map<String, Object>> itemsList = new ArrayList<>();
        Node itemsNode = getChildNode(requestElement, "items");
        if (itemsNode != null) {
            NodeList itemNodes = ((Element) itemsNode).getElementsByTagNameNS(NAMESPACE_URI, "item");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemEl = (Element) itemNodes.item(i);
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("bookId", getChildElementText(itemEl, "bookId"));
                
                String qty = getChildElementText(itemEl, "quantity");
                if (qty != null) itemMap.put("quantity", Integer.parseInt(qty));
                
                String price = getChildElementText(itemEl, "unitPrice");
                if (price != null) itemMap.put("price", Double.parseDouble(price)); // Note: OrdersService REST actually expects 'price' or 'unitPrice' based on Jackson mappings
                
                itemsList.add(itemMap);
            }
        }
        jsonPayload.put("items", itemsList);

        // Parse shipping address
        Node addrNode = getChildNode(requestElement, "shippingAddress");
        if (addrNode != null) {
            Element addrEl = (Element) addrNode;
            Map<String, Object> addrMap = new HashMap<>();
            addrMap.put("street", getChildElementText(addrEl, "street"));
            addrMap.put("city", getChildElementText(addrEl, "city"));
            addrMap.put("country", getChildElementText(addrEl, "country"));
            addrMap.put("postalCode", getChildElementText(addrEl, "postalCode"));
            jsonPayload.put("shippingAddress", addrMap);
        }

        System.out.println("[OrdersAdapterService] Decoded Customer ID: " + customerId + ", Total: " + totalAmount);
        System.out.println("[OrdersAdapterService] Successfully extracted " + itemsList.size() + " items from BPEL SOAP request.");
        System.out.println("[OrdersAdapterService] Initiating REST OAuth2 Call to OrdersService on port 8081...");

        // 2. Invoke OrdersService via HTTP REST with OAuth2
        Map<String, Object> responseJson;
        try {
            responseJson = ordersRestClient.createOrder(jsonPayload);
            System.out.println("[OrdersAdapterService] SUCCESS: Received successful JSON response from OrdersService. OrderId=" + responseJson.get("orderId"));
        } catch (Exception e) {
            System.out.println("[OrdersAdapterService] ERROR: REST Call to OrdersService failed: " + e.getMessage());
            throw e;
        }

        // 3. Convert JSON response back to XML DOM using javax.xml.parsers
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element orderElement = doc.createElementNS(NAMESPACE_URI, "gbord:order");
        doc.appendChild(orderElement);

        appendElement(doc, orderElement, "orderId", (String) responseJson.get("orderId"));
        appendElement(doc, orderElement, "customerId", (String) responseJson.get("customerId"));
        
        Object total = responseJson.get("totalAmount");
        if (total != null) appendElement(doc, orderElement, "totalAmount", total.toString());
        
        appendElement(doc, orderElement, "status", (String) responseJson.get("status"));

        System.out.println("[OrdersAdapterService] Formatted pure XML response to return to BPEL Engine.");
        System.out.println("[OrdersAdapterService] ======================================================\n");
        return orderElement;
    }

    private void appendElement(Document doc, Element parent, String name, String value) {
        if (value != null) {
            Element el = doc.createElementNS(NAMESPACE_URI, "gbord:" + name);
            el.setTextContent(value);
            parent.appendChild(el);
        }
    }

    private Node getChildNode(Element element, String localName) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getLocalName().equals(localName)) {
                return n;
            }
        }
        return null;
    }

    private String getChildElementText(Element element, String localName) {
        Node n = getChildNode(element, localName);
        return (n != null) ? n.getTextContent() : null;
    }
}
