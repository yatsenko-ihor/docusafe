package org.adorsys.docusafe.transactional;

import org.adorsys.cryptoutils.storeconnectionfactory.ExtendedStoreConnectionFactory;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.business.types.UserID;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DSDocumentMetaInfo;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.business.types.complex.UserIDAuth;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.docusafe.transactional.impl.TransactionalFileStorageImpl;
import org.adorsys.docusafe.transactional.types.TxID;
import org.adorsys.encobject.domain.ReadKeyPassword;
import org.adorsys.encobject.service.api.ExtendedStoreConnection;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

/**
 * Created by peter on 12.06.18 at 08:44.
 */
public class TransactionalTransactionalFileStorageTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionalTransactionalFileStorageTest.class);
    private ExtendedStoreConnection esc = ExtendedStoreConnectionFactory.get();
    private DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(esc);
    private TransactionalFileStorage transactionalFileStorage = new TransactionalFileStorageImpl(new DocumentSafeServiceImpl(esc));
    private UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("password"));

    @Before
    public void preTest() {
        Security.addProvider(new BouncyCastleProvider());
        if (documentSafeService.userExists(userIDAuth.getUserID())) {
            documentSafeService.destroyUser(userIDAuth);
        }
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void test() {
        documentSafeService.createUser(userIDAuth);
        DocumentFQN documentFQN = new DocumentFQN("peter/first.txt");
        DocumentContent documentContent1 = new DocumentContent("very first".getBytes());
        DocumentContent documentContent2 = new DocumentContent("second".getBytes());
        DSDocumentMetaInfo documentMetaInfo = new DSDocumentMetaInfo();
        DSDocument document = new DSDocument(documentFQN, documentContent1, documentMetaInfo);

        // Lege erste Version von first.txt an
        {
            TxID txid = transactionalFileStorage.beginTransaction(userIDAuth);
            LOGGER.debug("FIRST TXID " + txid);
            Assert.assertFalse(transactionalFileStorage.documentExists(txid, userIDAuth, documentFQN));
            transactionalFileStorage.storeDocument(txid, userIDAuth, document);
            Assert.assertTrue(transactionalFileStorage.documentExists(txid, userIDAuth, documentFQN));
            transactionalFileStorage.endTransaction(txid, userIDAuth);
        }

        TxID thirdTx = null;
        TxID fourthTx = null;
        // Beginne neue Transaction
        {
            // Überschreibe erste version mit zweiter Version
            TxID txid = transactionalFileStorage.beginTransaction(userIDAuth);
            LOGGER.debug("SECOND TXID " + txid);
            DSDocument dsDocument = transactionalFileStorage.readDocument(txid, userIDAuth, documentFQN);
            Assert.assertEquals(new String(documentContent1.getValue()), new String(dsDocument.getDocumentContent().getValue()));
            DSDocument document2 = new DSDocument(documentFQN, documentContent2, documentMetaInfo);
            transactionalFileStorage.storeDocument(txid, userIDAuth, document2);
            // Beginne dritte Transaktion VOR Ende der zweiten
            thirdTx = transactionalFileStorage.beginTransaction(userIDAuth);
            LOGGER.debug("THIRD TXID " + thirdTx);
            transactionalFileStorage.endTransaction(txid, userIDAuth);
            // Beginne vierte Transaktion NACH Ende der zweiten
            fourthTx = transactionalFileStorage.beginTransaction(userIDAuth);
            LOGGER.debug("FOURTH TXID " + fourthTx);
        }

        {
            // dritte Tx muss noch ersten Inhalt lesen
            DSDocument dsDocument = transactionalFileStorage.readDocument(thirdTx, userIDAuth, documentFQN);
            Assert.assertEquals(new String(documentContent1.getValue()), new String(dsDocument.getDocumentContent().getValue()));
        }

        {
            // vierte Tx muss schon zweiten Inhalt lesen
            DSDocument dsDocument = transactionalFileStorage.readDocument(fourthTx, userIDAuth, documentFQN);
            Assert.assertEquals(new String(documentContent2.getValue()), new String(dsDocument.getDocumentContent().getValue()));
        }

    }
}