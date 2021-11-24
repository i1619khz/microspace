package io.microspace.server;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;

/**
 * Details of an annotated HTTP service method.
 * @author i1619kHz
 */
public class AnnotatedServiceElement {
    private final Route route;
    private final AnnotatedService service;

    AnnotatedServiceElement(Route route,
                            AnnotatedService service) {
        this.route = requireNonNull(route, "route");
        this.service = requireNonNull(service, "service");
    }

    /**
     * Returns the {@link Route}.
     */
    public Route route() {
        return route;
    }

    /**
     * Returns the {@link AnnotatedService} that will handle the request.
     */
    public AnnotatedService service() {
        return service;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
                          .add("route", route())
                          .add("service", service())
                          .toString();
    }
}
