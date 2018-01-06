package org.adorsys.resource.server.service;

import com.nimbusds.jose.jwk.JWKSet;
import de.adorsys.resource.server.keyservice.SecretKeyGenerator;
import org.adorsys.encobject.domain.ContentMetaInfo;
import org.adorsys.encobject.params.EncryptionParams;
import org.adorsys.jjwk.keystore.JwkExport;
import org.adorsys.jjwk.serverkey.KeyAndJwk;
import org.adorsys.jjwk.serverkey.ServerKeyMap;
import org.adorsys.jkeygen.keystore.SecretKeyData;
import org.adorsys.resource.server.persistence.complextypes.DocumentGuardLocation;
import org.adorsys.resource.server.basetypes.DocumentKey;
import org.adorsys.resource.server.basetypes.DocumentKeyID;
import org.adorsys.resource.server.basetypes.GuardKeyID;
import org.adorsys.resource.server.complextypes.DocumentGuard;
import org.adorsys.resource.server.exceptions.BaseExceptionHandler;
import org.adorsys.resource.server.persistence.ExtendedKeystorePersistence;
import org.adorsys.resource.server.persistence.ExtendedObjectPersistence;
import org.adorsys.resource.server.persistence.KeySource;
import org.adorsys.resource.server.persistence.KeyStoreBasedKeySourceImpl;
import org.adorsys.resource.server.persistence.PersistentObjectWrapper;
import org.adorsys.resource.server.persistence.basetypes.KeyID;
import org.adorsys.resource.server.persistence.complextypes.KeyStoreAuth;
import org.adorsys.resource.server.persistence.complextypes.KeyStoreLocation;
import org.adorsys.resource.server.serializer.DocumentGuardSerializer;
import org.adorsys.resource.server.serializer.DocumentGuardSerializer01;
import org.adorsys.resource.server.serializer.DocumentGuardSerializerRegistery;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

public class DocumentGuardService {

    private ExtendedKeystorePersistence keystorePersistence;
    private SecretKeyGenerator secretKeyGenerator;
    private ExtendedObjectPersistence objectPersistence;

    private DocumentGuardSerializerRegistery serializerRegistry = DocumentGuardSerializerRegistery.getInstance();

    public DocumentGuardService(ExtendedKeystorePersistence keystorePersistence, ExtendedObjectPersistence objectPersistence) {
        this.keystorePersistence = keystorePersistence;
        this.objectPersistence = objectPersistence;
        this.secretKeyGenerator = new SecretKeyGenerator("AES", 256);
    }

    public DocumentGuardLocation createDocumentGuard(KeyStoreLocation keyStoreLocation, KeyStoreAuth keyStoreAuth) {
        try {
            // KeyStore laden
            KeyStore userKeystore = keystorePersistence.loadKeystore(keyStoreLocation, keyStoreAuth.getUserpass());
            KeySource keySource = new KeyStoreBasedKeySourceImpl(userKeystore, keyStoreAuth.getKeypass());

            // Willkürlich einen SecretKey aus dem KeyStore nehmen für die Verschlüsselung des Guards
            JWKSet jwkSet = JwkExport.exportKeys(userKeystore, keyStoreAuth.getKeypass());
            ServerKeyMap serverKeyMap = new ServerKeyMap(jwkSet);
            KeyAndJwk randomSecretKey = serverKeyMap.randomSecretKey();
            GuardKeyID guardKeyID = new GuardKeyID(randomSecretKey.jwk.getKeyID());

            // Eine zufällige DocumentKeyID erzeugen
            DocumentKeyID documentKeyID = new DocumentKeyID(RandomStringUtils.randomAlphanumeric(20));

            // Zielpfad für den DocumentGuard bestimmen
            DocumentGuardLocation documentGuardLocation = new DocumentGuardLocation(keyStoreLocation, documentKeyID);
//            ObjectHandle location = new ObjectHandle(keyStoreLocation.getKeyStoreBucketName().getValue(), documentGuardLocation.getValue());

            EncryptionParams encParams = null;

            // Für die DocumentKeyID einen DocumentKey erzeugen
            SecretKeyData secretKeyData = secretKeyGenerator.generate(documentKeyID.getValue(), keyStoreAuth.getKeypass());
            DocumentKey documentKey = new DocumentKey(secretKeyData.getSecretKey());

            // Den DocumentKey serialisieren, in der MetaInfo die SerializerID vermerken
            ContentMetaInfo metaInfo = new ContentMetaInfo();
            metaInfo.setAddInfos(new HashMap<>());
            metaInfo.getAddInfos().put(serializerRegistry.SERIALIZER_HEADER_KEY, DocumentGuardSerializer01.SERIALIZER_ID);
            byte[] serializedSecretKeyBytes = serializerRegistry.defaultSerializer().serializeSecretKey(documentKey);

            objectPersistence.storeObject(serializedSecretKeyBytes, metaInfo, documentGuardLocation.getLocationHandle(), keySource, new KeyID(guardKeyID.getValue()), encParams);
            return documentGuardLocation;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }


    /**
     * Loading the secret key from the guard.
     */
    public DocumentGuard loadDocumentGuard(DocumentGuardLocation documentGuardLocation, KeyStoreAuth keyStoreAuth) {

        try {

        	KeyStore userKeystore = keystorePersistence.loadKeystore(documentGuardLocation.getKeyStoreLocation(), keyStoreAuth.getUserpass());

            // load guard file
            KeySource keySource = new KeyStoreBasedKeySourceImpl(userKeystore, keyStoreAuth.getKeypass());
            PersistentObjectWrapper wrapper = objectPersistence.loadObject(documentGuardLocation.getLocationHandle(), keySource);

            ContentMetaInfo metaIno = wrapper.getMetaIno();
            Map<String, Object> addInfos = metaIno.getAddInfos();
            String serializerId = (String) addInfos.get(serializerRegistry.SERIALIZER_HEADER_KEY);
            serializerRegistry.getSerializer(serializerId);
            DocumentGuardSerializer serializer = serializerRegistry.getSerializer(serializerId);
            DocumentKey documentKey = serializer.deserializeSecretKey(wrapper.getData());

            return new DocumentGuard(documentGuardLocation, documentKey);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }
}
