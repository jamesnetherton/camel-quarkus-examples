package org.acme.observability;

import java.net.URISyntaxException;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.tracing.decorators.HttpSpanDecorator;
import org.apache.camel.util.URISupport;

public class CustomHttpSpanDecorator extends HttpSpanDecorator {
    @Override
    public String getOperationName(Exchange exchange, Endpoint endpoint) {
        try {
            return super.getOperationName(exchange, endpoint) + " "
                    + URISupport.normalizeUriAsURI(endpoint.getEndpointBaseUri()).getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
