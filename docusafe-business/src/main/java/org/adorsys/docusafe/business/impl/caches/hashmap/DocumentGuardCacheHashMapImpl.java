package org.adorsys.docusafe.business.impl.caches.hashmap;

import org.adorsys.docusafe.business.impl.caches.DocumentGuardCache;
import org.adorsys.docusafe.service.impl.PasswordAndDocumentKeyIDWithKey;

/**
 * Created by peter on 14.08.18 at 14:52.
 */
public class DocumentGuardCacheHashMapImpl
        extends DocusafeCacheTemplateHashMapImpl<String, PasswordAndDocumentKeyIDWithKey>
        implements DocumentGuardCache {
}
