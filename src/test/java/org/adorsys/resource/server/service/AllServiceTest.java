package org.adorsys.resource.server.service;

import org.adorsys.resource.server.basetypes.DocumentContent;
import org.adorsys.resource.server.basetypes.DocumentID;
import org.adorsys.resource.server.exceptions.BaseException;
import org.adorsys.resource.server.exceptions.BaseExceptionHandler;
import org.adorsys.resource.server.persistence.basetypes.BucketName;
import org.adorsys.resource.server.utils.HexUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by peter on 04.01.18.
 */
public class AllServiceTest {
    @BeforeClass
    public static void before() {
        KeyStoreServiceTest.beforeTest();
        DocumentGuardServiceTest.beforeClass();
        DocumentPersistenceServiceTest.beforeClass();

    }

    @AfterClass
    public static void after() {
        DocumentPersistenceServiceTest.afterClass();
        DocumentGuardServiceTest.afterClass();
        KeyStoreServiceTest.afterTest();

    }

    @Test
    public void testCreateKeyStore() {
        try {
            KeyStoreServiceTest.KeyStoreStuff keyStoreStuff = new KeyStoreServiceTest().createKeyStore();
            Assert.assertEquals("Number of Entries", 15, keyStoreStuff.keyStore.size());
        } catch (Exception e) {
            BaseExceptionHandler.handle(e);
        }
    }

    @Test
    public void testCreateKeyStoreAndDocumentGuard() {
        try {
            KeyStoreServiceTest.KeyStoreStuff keyStoreStuff = new KeyStoreServiceTest().createKeyStore();
            new DocumentGuardServiceTest().testCreateDocumentGuard(
                    keyStoreStuff.userKeyStoreHandler,
                    keyStoreStuff.keyPassHandler,
                    keyStoreStuff.keystorePersistence,
                    keyStoreStuff.keyStoreBucketName,
                    keyStoreStuff.keyStoreID);
        } catch (Exception e) {
            BaseExceptionHandler.handle(e);
        }
    }

    @Test
    public void testCreateKeyStoreAndDocumentGuardAndLoadDocumentGuard() {
        try {
            KeyStoreServiceTest.KeyStoreStuff keyStoreStuff = new KeyStoreServiceTest().createKeyStore();
            DocumentGuardServiceTest.DocumentGuardStuff documentGuardStuff = new DocumentGuardServiceTest().testCreateAndLoadDocumentGuard(
                    keyStoreStuff.userKeyStoreHandler,
                    keyStoreStuff.keyPassHandler,
                    keyStoreStuff.keystorePersistence,
                    keyStoreStuff.keyStoreBucketName,
                    keyStoreStuff.keyStoreID);
            System.out.println("DocumentKey is " + HexUtil.conventBytesToHexString(documentGuardStuff.documentGuard.getDocumentKey().getSecretKey().getEncoded()));
        } catch (Exception e) {
            BaseExceptionHandler.handle(e);
        }
    }

    // TODO
    @Test (expected = BaseException.class)
    public void testCreateDocument() {
        try {
            BucketName documentBucketName = new BucketName("document-bucket");
            DocumentID documentID = new DocumentID("document-id-123");
            DocumentContent documentContent = new DocumentContent("Der Inhalt ist ein Affe".getBytes());
            KeyStoreServiceTest.KeyStoreStuff keyStoreStuff = new KeyStoreServiceTest().createKeyStore();
            DocumentGuardServiceTest.DocumentGuardStuff documentGuardStuff = new DocumentGuardServiceTest().testCreateAndLoadDocumentGuard(
                    keyStoreStuff.userKeyStoreHandler,
                    keyStoreStuff.keyPassHandler,
                    keyStoreStuff.keystorePersistence,
                    keyStoreStuff.keyStoreBucketName,
                    keyStoreStuff.keyStoreID);
            new DocumentPersistenceServiceTest().testPersistDocument(
                    documentGuardStuff.documentGuardService,
                    keyStoreStuff.userKeyStoreHandler,
                    keyStoreStuff.keyPassHandler,
                    documentGuardStuff.documentGuardName,
                    documentBucketName,
                    documentID,
                    documentContent);

            System.out.println("DocumentKey is " + HexUtil.conventBytesToHexString(documentGuardStuff.documentGuard.getDocumentKey().getSecretKey().getEncoded()));
        } catch (Exception e) {
            BaseExceptionHandler.handle(e);
        }
    }

}
