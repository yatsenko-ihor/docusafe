package org.adorsys.documentsafe.layer01persistence;

import org.adorsys.documentsafe.layer00common.exceptions.BaseExceptionHandler;
import org.adorsys.documentsafe.layer01persistence.types.KeyStoreType;
import org.adorsys.documentsafe.layer01persistence.types.complextypes.KeyStoreLocation;
import org.adorsys.encobject.service.BlobStoreConnection;
import org.adorsys.encobject.service.BlobStoreContextFactory;
import org.adorsys.jkeygen.keystore.KeyStoreService;
import org.adorsys.documentsafe.layer01persistence.exceptions.ExtendedPersistenceException;

import javax.security.auth.callback.CallbackHandler;
import java.security.KeyStore;

/**
 * Service in charge of loading and storing user keys.
 * 
 * @author fpo
 *
 */
public class ExtendedKeystorePersistence {

	private BlobStoreConnection blobStoreConnection;

	public ExtendedKeystorePersistence(BlobStoreContextFactory blobStoreContextFactory) {
		this.blobStoreConnection = new BlobStoreConnection(blobStoreContextFactory);
	}

	public void saveKeyStore(KeyStore keystore, CallbackHandler storePassHandler, KeyStoreLocation keyStoreLocation) {
		try {
			// Match store type aggainst file extension
			if(!keyStoreLocation.getKeyStoreType().equals(new KeyStoreType(keystore.getType())))
					throw new ExtendedPersistenceException("Invalid store type - expected : " + keystore.getType() + " but is: " + keyStoreLocation.getKeyStoreType().getValue());
			
			// write keystore to byte array.
			byte[] bs = KeyStoreService.toByteArray(keystore, keyStoreLocation.getLocationHandle().getName(), storePassHandler);
			
			// write byte array to blob store.
			blobStoreConnection.putBlob(keyStoreLocation.getLocationHandle() , bs);
		} catch (Exception e) {
			BaseExceptionHandler.handle(e);
		}
	}
	
	public KeyStore loadKeystore(KeyStoreLocation keyStoreLocation, CallbackHandler handler) {
		try {
			
			// Read bytes
			byte[] ksBytes = blobStoreConnection.getBlob(keyStoreLocation.getLocationHandle());
			
			// Initialize key store
			return KeyStoreService.loadKeyStore(ksBytes, keyStoreLocation.getLocationHandle().getName(), keyStoreLocation.getKeyStoreType().getValue(), handler);
		} catch (Exception e) {
			throw BaseExceptionHandler.handle(e);
		}
	}	
	

}