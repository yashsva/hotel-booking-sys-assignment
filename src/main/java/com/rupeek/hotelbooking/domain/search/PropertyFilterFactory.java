package com.rupeek.hotelbooking.domain.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds one kind of {@link PropertyFilter} from raw search parameters, if the parameters it
 * understands are present. Adding a filter = adding one factory; the search code is untouched.
 */
public interface PropertyFilterFactory {

    Optional<PropertyFilter> create(Map<String, List<String>> parameters);
}
