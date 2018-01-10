package org.adorsys.documentsafe.layer01persistence.types;

import org.adorsys.documentsafe.layer00common.basetypes.BaseTypeString;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by peter on 29.12.2017 at 14:11:52.
 */
public class KeyStoreType extends BaseTypeString {

	public KeyStoreType() {}

    public KeyStoreType(String value) {
        super(value);
    }

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseTypeString that = (BaseTypeString) o;

        return StringUtils.equalsAnyIgnoreCase(getValue(), that.getValue());
    }
    
    
    
}