package com.globalbooks.catalog.security;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WsSecurityHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOG = Logger.getLogger(WsSecurityHandler.class.getName());

    // WS-Security namespace
    private static final String WSSE_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    // Credentials
    private static final String VALID_USERNAME = "catalog-client";
    private static final String VALID_PASSWORD = "s3cur3P@ss";

    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        Boolean outbound = (Boolean) ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        // Only validate inbound (requests)
        if (Boolean.TRUE.equals(outbound)) {
            return true;
        }

        try {
            SOAPMessage message    = ctx.getMessage();
            SOAPEnvelope envelope  = message.getSOAPPart().getEnvelope();
            SOAPHeader header      = envelope.getHeader();

            if (header == null) {
                throwSecurityFault(ctx, "Missing SOAP Security header");
                return false;
            }

            // Find <wsse:Security> element
            Iterator<?> headerElements = header.examineAllHeaderElements();
            SOAPElement securityElement = null;

            while (headerElements.hasNext()) {
                SOAPElement el = (SOAPElement) headerElements.next();
                if ("Security".equals(el.getLocalName())
                        && WSSE_NS.equals(el.getNamespaceURI())) {
                    securityElement = el;
                    break;
                }
            }

            if (securityElement == null) {
                throwSecurityFault(ctx, "Missing wsse:Security element");
                return false;
            }

            // Find <wsse:UsernameToken>
            Iterator<?> children = securityElement.getChildElements(
                new QName(WSSE_NS, "UsernameToken"));

            if (!children.hasNext()) {
                throwSecurityFault(ctx, "Missing UsernameToken in Security header");
                return false;
            }

            SOAPElement usernameToken = (SOAPElement) children.next();

            String username = extractChildText(usernameToken, "Username");
            String password = extractChildText(usernameToken, "Password");

            if (!VALID_USERNAME.equals(username) || !VALID_PASSWORD.equals(password)) {
                LOG.warning("WS-Security: Invalid credentials for user: " + username);
                throwSecurityFault(ctx, "Invalid credentials");
                return false;
            }

            LOG.info("WS-Security: Authenticated user: " + username);
            return true;

        } catch (SOAPException e) {
            LOG.log(Level.SEVERE, "WS-Security handler error", e);
            return false;
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext ctx) {
        return true; // pass faults through
    }

    @Override
    public void close(MessageContext ctx) {}

    @Override
    public Set<QName> getHeaders() {
        return Collections.singleton(new QName(WSSE_NS, "Security"));
    }

    private String extractChildText(SOAPElement parent, String localName) {
        Iterator<?> it = parent.getChildElements(new QName(WSSE_NS, localName));
        if (it.hasNext()) {
            return ((SOAPElement) it.next()).getTextContent();
        }
        return null;
    }

    private void throwSecurityFault(SOAPMessageContext ctx, String message)
            throws SOAPException {
        SOAPMessage soapMessage = ctx.getMessage();
        SOAPBody body = soapMessage.getSOAPPart().getEnvelope().getBody();
        SOAPFault fault = body.addFault();
        fault.setFaultString("WS-Security violation: " + message);
        fault.setFaultCode("wsse:FailedAuthentication");
        throw new javax.xml.ws.soap.SOAPFaultException(fault);
    }
}
