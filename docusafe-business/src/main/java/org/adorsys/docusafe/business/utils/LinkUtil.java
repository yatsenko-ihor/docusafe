package org.adorsys.docusafe.business.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.DocumentLink;
import org.adorsys.docusafe.business.types.complex.DocumentLinkAsDSDocument;

/**
 * Created by peter on 23.01.18 at 17:27.
 */
public class LinkUtil {
    public static DocumentLinkAsDSDocument createDSDocument(DocumentLink documentLink, DocumentFQN documentFQN) {
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(documentLink);
        DocumentContent documentContent = new DocumentContent(jsonString.getBytes());
        return new DocumentLinkAsDSDocument(documentFQN, documentContent);
    }

    public static DocumentLink getDocumentLink(byte[] bytes) {
        Gson gson = new GsonBuilder().create();
        String jsonString = new String(bytes);
        DocumentLink documentLink = gson.fromJson(jsonString, DocumentLink.class);
        return documentLink;
    }
}