package org.adorsys.documentsafe.layer04rest.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.adorsys.documentsafe.layer02service.types.ReadKeyPassword;

import java.io.IOException;

/**
 * Created by peter on 22.01.2018 at 20:06:56.
 */
public class ReadKeyPasswordJsonAdapter extends TypeAdapter<ReadKeyPassword> {
    @Override
    public void write(JsonWriter out, ReadKeyPassword value) throws IOException {
        out.value(value.getValue());
    }
    @Override
    public ReadKeyPassword read(JsonReader in) throws IOException {
        return new ReadKeyPassword(in.nextString());
    }
}