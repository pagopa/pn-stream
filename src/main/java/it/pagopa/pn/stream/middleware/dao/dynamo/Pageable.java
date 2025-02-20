package it.pagopa.pn.stream.middleware.dao.dynamo;

public interface Pageable {
    boolean isPage();
    boolean hasLimit();
}