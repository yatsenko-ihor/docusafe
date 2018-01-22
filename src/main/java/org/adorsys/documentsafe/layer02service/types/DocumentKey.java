package org.adorsys.documentsafe.layer02service.types;

import org.adorsys.documentsafe.layer00common.basetypes.BaseTypePasswordByteArray;
import org.adorsys.documentsafe.layer00common.utils.HexUtil;

import javax.crypto.SecretKey;

/**
 * Created by peter on 29.12.2017 at 13:55:33.
 * 
 * @fpo: we do not need to serialize this class.
 * Ein DocumentKey ist immer symmetrisch
 */
public class DocumentKey {
	
	SecretKey secretKey;
    
	public DocumentKey() {}
	
	public DocumentKey(SecretKey secretKey) {
		super();
		this.secretKey = secretKey;
	}

	public SecretKey getSecretKey() {
		return secretKey;
	}

	@Override
	public String toString() {
		return "DocumentKey{" +
				BaseTypePasswordByteArray.hide(secretKey.getEncoded()) +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DocumentKey that = (DocumentKey) o;

		return secretKey != null ? secretKey.equals(that.secretKey) : that.secretKey == null;

	}

	@Override
	public int hashCode() {
		return secretKey != null ? secretKey.hashCode() : 0;
	}
}
