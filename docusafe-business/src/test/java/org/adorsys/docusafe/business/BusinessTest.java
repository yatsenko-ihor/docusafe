package org.adorsys.docusafe.business;

import com.googlecode.catchexception.CatchException;
import lombok.SneakyThrows;
import org.adorsys.cryptoutils.exceptions.BaseException;
import org.adorsys.docusafe.business.exceptions.UserIDDoesNotExistException;
import org.adorsys.docusafe.business.exceptions.WrongPasswordException;
import org.adorsys.docusafe.business.types.UserID;
import org.adorsys.docusafe.business.types.complex.*;
import org.adorsys.docusafe.service.impl.DocumentPersistenceServiceImpl;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.encobject.domain.ReadKeyPassword;
import org.adorsys.encobject.domain.UserMetaData;
import org.adorsys.encobject.service.impl.KeyStoreServiceImpl;
import org.adorsys.encobject.types.ListRecursiveFlag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Created by peter on 19.01.18 at 16:25.
 */
@SuppressWarnings("Duplicates")
@RunWith(PowerMockRunner.class)
@PrepareForTest({DocumentPersistenceServiceImpl.class})
@PowerMockIgnore("javax.*")
public class BusinessTest extends BusinessTestBase {
    private final static Logger LOGGER = LoggerFactory.getLogger(BusinessTest.class);

    private KeyStoreServiceImpl keyStoreService;

    @Override
    @Before
    @SneakyThrows
    public void before() {
        keyStoreService = spy(new KeyStoreServiceImpl(extendedStoreConnection));
        PowerMockito.whenNew(KeyStoreServiceImpl.class).withAnyArguments().thenReturn(keyStoreService);
        super.before();
    }

    // Demo for Maksym how path and filename encryption blows up the path length
    // @Test
    public void blowUpPathLengthByEncryption() {

        UserIDAuth userIDAuth = createUser();

        DocumentFQN documentFQN = new DocumentFQN("a/b/c/file1.txt");
        DSDocument dsDocument1 = createDocument(userIDAuth, documentFQN);
    }


    @Test
    public void testCache_DOC_86() {
        // avoid reading the keystore twice
        UserIDAuth userIDAuth = createUser(new UserID("UserPeter"), new ReadKeyPassword("peterkey"));
        DocumentFQN documentFQN = new DocumentFQN("first/next/document.txt");
        DocumentContent documentContent = new DocumentContent(("Einfach nur a bisserl Text").getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent, new DSDocumentMetaInfo());
        service.storeDocument(userIDAuth, dsDocument);

        service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
        verify(keyStoreService).loadKeystore(any(), any());
        // Maksym try to prrove  with mock
        service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
        // should be read from cache, hence 1 invocation
        verify(keyStoreService).loadKeystore(any(), any());
    }

    @Test
    public void documentListMetadata_DOC_77() {
        UserIDAuth userIDAuth = createUser(new UserID("UserPeter"), new ReadKeyPassword("peterkey"));

        DocumentFQN documentFQN = new DocumentFQN("first/document.txt");
        Assert.assertFalse(service.documentExists(userIDAuth, documentFQN));
        DocumentContent documentContent = new DocumentContent(("Einfach nur a bisserl Text").getBytes());
        UserMetaData userMetaData = new UserMetaData();
        userMetaData.put("first", "1");
        userMetaData.put("second", "2");
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent, new DSDocumentMetaInfo(userMetaData));
        service.storeDocument(userIDAuth, dsDocument);
        Assert.assertTrue(service.documentExists(userIDAuth, documentFQN));
        BucketContentFQNWithUserMetaData list = service.list(userIDAuth, new DocumentDirectoryFQN(""), ListRecursiveFlag.TRUE);
        final Boolean found[] = new Boolean[1];
        found[0] = false;
        list.getFiles().stream().filter(file -> file.equals(documentFQN)).forEach(file -> {
            found[0] = true;
            LOGGER.info("found:" + file);
            list.getUserMetaData(file).keySet().forEach(key -> LOGGER.info("UserMetaData: " + key + " " + list.getUserMetaData(file).get(key)));
            for (String key : userMetaData.keySet()) {
                Assert.assertEquals(userMetaData.get(key), list.getUserMetaData(file).get(key));
            }
        });
        Assert.assertTrue(found.length == 1);
        Assert.assertTrue(found[0].equals(Boolean.TRUE));
    }

    @Test
    public void documentExistsTest_DOC_36() {
        UserIDAuth userIDAuth = createUser(new UserID("UserPeter"), new ReadKeyPassword("peterkey"));
        DocumentFQN documentFQNReadme1 = new DocumentFQN("README.txt");
        DocumentFQN documentFQNReadme2 = new DocumentFQN("README2.txt");
        DocumentFQN documentFQNnewDir = new DocumentFQN("affe/README2.txt");
        Assert.assertTrue(service.documentExists(userIDAuth, documentFQNReadme1));
        Assert.assertFalse(service.documentExists(userIDAuth, documentFQNReadme2));
        Assert.assertFalse(service.documentExists(userIDAuth, documentFQNnewDir));
        UserIDAuth userIDAuth2 = new UserIDAuth(new UserID("UserPeter2"), new ReadKeyPassword("peterkey"));
        CatchException.catchException(() -> service.documentExists(userIDAuth2, documentFQNReadme1));
        Assert.assertNotNull(CatchException.caughtException());
        Assert.assertTrue(CatchException.caughtException() instanceof UserIDDoesNotExistException);
    }

    @Test
    public void performanceTest_DOC_29() {

        int REPEATS = 1;
        int i = 0;

        UserIDAuth userIDAuth = createUser();

        while (i > 0) {
            LOGGER.info("wait for visualVM profiler " + i);
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
            }
            i--;
        }

        for (int j = 0; j < REPEATS; j++) {
            DocumentFQN documentFQN = new DocumentFQN("first/next/document" + j + ".txt");
            Assert.assertFalse(service.documentExists(userIDAuth, documentFQN));
            DocumentContent documentContent = new DocumentContent(("Einfach nur a bisserl Text" + j).getBytes());
            DSDocument dsDocument = new DSDocument(documentFQN, documentContent, new DSDocumentMetaInfo());
            service.storeDocument(userIDAuth, dsDocument);
            Assert.assertTrue(service.documentExists(userIDAuth, documentFQN));
            DSDocument dsDocumentResult = service.readDocument(userIDAuth, documentFQN);
            LOGGER.debug("original  document:" + new String(documentContent.getValue()));
            LOGGER.debug("retrieved document:" + new String(dsDocumentResult.getDocumentContent().getValue()));
            Assert.assertEquals("document content ok", documentContent, dsDocumentResult.getDocumentContent());
        }
    }

    @Test
    public void sequenceDiagramTest() {

        UserIDAuth userIDAuth = new UserIDAuth(new UserID("user1"), new ReadKeyPassword("password1"));
        users.add(userIDAuth);
        service.createUser(userIDAuth);
    }

    @Test
    public void testCreateUser() {
        UserIDAuth userIDAuth = createUser();
        Assert.assertEquals("Anzahl der guards muss 0 betragen", 0, getNumberOfGuards(userIDAuth.getUserID()));
    }


    @Test
    public void loadDSDocument() {

        UserIDAuth userIDAuth = createUser(new UserID("affe"), new ReadKeyPassword("ab_irgendwas_cd"));
        DocumentFQN fqn = new DocumentFQN("README.txt");
        // Dieser Read muss ok sein
        service.readDocument(userIDAuth, fqn);
        userIDAuth = new UserIDAuth(new UserID("affe"), new ReadKeyPassword("ab_123456789_cd"));
        Boolean catched = false;
        try {
            // Dieser Read muss fehlschlagen. Es gab einen Bug im Cache, wo statt des ReadKeyPassword der toString Text benutzt wurde.
            // Dieser ist aber mit **** ausgegraut, so dass alle Passworte bis auf Anfang und Ende gleich sind !!!
            service.readDocument(userIDAuth, fqn);
        } catch (BaseException e) {
            catched = true;
        }
        Assert.assertTrue(catched);
    }

    @Test
    public void deleteDocumentTest() {

        UserID userID = new UserID("DelPeter");
        Assert.assertFalse(service.userExists(userID));
        UserIDAuth userIDAuth = createUser(userID);
        Assert.assertTrue(service.userExists(userID));
        DocumentFQN fqn = new DocumentFQN("README.txt");
        service.readDocument(userIDAuth, fqn);
        Assert.assertTrue(service.documentExists(userIDAuth, fqn));
        service.deleteDocument(userIDAuth, fqn);
        Assert.assertFalse(service.documentExists(userIDAuth, fqn));
        CatchException.catchException(() -> service.readDocument(userIDAuth, fqn));
        Assert.assertNotNull(CatchException.caughtException());
    }

    // DOC-45, now the number guards still should be 1
    @Test
    public void storeDSDocumentInANewFolder() {

        UserIDAuth userIDAuth = createUser();
        DocumentFQN documentFQN = new DocumentFQN("first/next/a-new-document.txt");
        DSDocument dsDocument1 = createDocument(userIDAuth, documentFQN);
        DSDocument dsDocument1R = readDocument(userIDAuth, documentFQN, dsDocument1.getDocumentContent());
        Assert.assertArrayEquals(dsDocument1.getDocumentContent().getValue(), dsDocument1R.getDocumentContent().getValue());

        DSDocument dsDocument2 = createDocument(userIDAuth, new DocumentFQN("first/next/another-new-document.txt"));
        DSDocument dsDocument2R = readDocument(userIDAuth, dsDocument2.getDocumentFQN(), dsDocument2.getDocumentContent());
        Assert.assertArrayEquals(dsDocument2.getDocumentContent().getValue(), dsDocument2R.getDocumentContent().getValue());
    }

    @Test
    public void tryToDeleteUserWithWrongPassword() {

        UserIDAuth userIDAuthPeter = createUser(new UserID("peter"), new ReadKeyPassword("keyPasswordForPeter"));
        UserIDAuth wrongUserIDAuthPeter = new UserIDAuth(userIDAuthPeter.getUserID(), new ReadKeyPassword("WRONGPASSWORD"));
        boolean exceptionRaised = false;
        try {
            service.destroyUser(wrongUserIDAuthPeter);
        } catch (WrongPasswordException e) {
            LOGGER.debug("THIS EXCEPTION WAS EXPECTED");
            LOGGER.info("caught exception");
            exceptionRaised = true;
        }
        Assert.assertTrue(exceptionRaised);
        service.destroyUser(userIDAuthPeter);
        users.clear();

    }

    @Test
    public void checkDirectoryListings() {
        int DEPTH = 4;
        int SUBDIRS = 4;
        int FILES = 4;
        // TODO actually a performance teste
        DEPTH = 2;
        SUBDIRS = 2;
        FILES = 1;
        UserIDAuth userIDAuth = createUser();
        DocumentDirectoryFQN dir = new DocumentDirectoryFQN("/");
        createDirectoryWithSubdirectories(DEPTH, userIDAuth, dir, SUBDIRS, FILES);
        int expectedFilesInRootNonRecursive = FILES + 1; // README.txt is created in every root
        int expectedDirectoriesInRootNonRecursive = SUBDIRS;
        int dirs = 0;
        for (int i = 1; i < DEPTH; i++) {
            dirs = dirs + (int) Math.pow(SUBDIRS, i);
        }
        int expectedDirsRecursive = dirs;
        int expectedFilesRecursive = dirs * FILES + expectedFilesInRootNonRecursive;

        LOGGER.info("expectedFilesInRootNonRecursive:       " + expectedDirectoriesInRootNonRecursive);
        LOGGER.info("expectedDirectoriesInRootNonRecursive: " + expectedFilesInRootNonRecursive);
        LOGGER.info("expectedDirsRecursive:                 " + expectedDirsRecursive);
        LOGGER.info("expectedFilesRecursive:                " + expectedFilesRecursive);
        {
            BucketContentFQN list = service.list(userIDAuth, dir, ListRecursiveFlag.FALSE);
            list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
            list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
            Assert.assertEquals(expectedDirectoriesInRootNonRecursive, list.getDirectories().size());
            Assert.assertEquals(expectedFilesInRootNonRecursive, list.getFiles().size());
        }
        {
            BucketContentFQN list = service.list(userIDAuth, dir, ListRecursiveFlag.TRUE);
            list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
            list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
            Assert.assertEquals(expectedDirsRecursive, list.getDirectories().size());
            Assert.assertEquals(expectedFilesRecursive, list.getFiles().size());
        }
    }

    @Test
    public void checkRootDirectoryListings() {

        UserIDAuth userIDAuth = createUser();
        DocumentDirectoryFQN dir = new DocumentDirectoryFQN("/");
        createDirectoryWithSubdirectories(2, userIDAuth, dir, 1, 1);
        {
            BucketContentFQN list = service.list(userIDAuth, dir, ListRecursiveFlag.FALSE);
            list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
            list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
            Assert.assertEquals(1, list.getDirectories().size());
            Assert.assertEquals(2, list.getFiles().size());
        }
        {
            BucketContentFQN list = service.list(userIDAuth, dir, ListRecursiveFlag.TRUE);
            list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
            list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
            Assert.assertEquals(1, list.getDirectories().size());
            Assert.assertEquals(3, list.getFiles().size());
        }
    }

    @Test
    public void checkRootDirectoryListingVerySimple() {

        UserIDAuth userIDAuth = createUser();
        DocumentDirectoryFQN dir = new DocumentDirectoryFQN("/");
        DocumentFQN documentFQN = new DocumentFQN("/affe.txt");
        createDocument(userIDAuth, documentFQN);

        BucketContentFQN list = service.list(userIDAuth, dir, ListRecursiveFlag.FALSE);
        list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
        list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
        Assert.assertEquals(0, list.getDirectories().size());
        Assert.assertEquals(2, list.getFiles().size());

        list = service.list(userIDAuth, dir, ListRecursiveFlag.TRUE);
        list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
        list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
        Assert.assertEquals(0, list.getDirectories().size());
        Assert.assertEquals(2, list.getFiles().size());
    }

    @Test
    public void checkRootDirectoryListingSimple() {

        UserIDAuth userIDAuth = createUser();
        DocumentDirectoryFQN dir = new DocumentDirectoryFQN("/anyfolder");
        DocumentFQN documentFQN = new DocumentFQN("/anyfolder/affe.txt");
        createDocument(userIDAuth, documentFQN);

        BucketContentFQN list = service.list(userIDAuth, dir, ListRecursiveFlag.FALSE);
        list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
        list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
        Assert.assertEquals(0, list.getDirectories().size());
        Assert.assertEquals(1, list.getFiles().size());

        list = service.list(userIDAuth, dir, ListRecursiveFlag.TRUE);
        list.getDirectories().forEach(sdir -> LOGGER.debug("found dir " + sdir));
        list.getFiles().forEach(file -> LOGGER.debug("found file " + file));
        Assert.assertEquals(0, list.getDirectories().size());
        Assert.assertEquals(1, list.getFiles().size());
    }


}
