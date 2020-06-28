package com.github.cheukbinli.original.cache;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ReferenceResolver;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import java.util.Arrays;

public class KryoEx extends Kryo {

    public KryoEx() {
        this(new DefaultClassResolver(), new MapReferenceResolver());
    }

    public KryoEx(ClassResolver classResolver, ReferenceResolver referenceResolver) {
        super(classResolver, referenceResolver);
        addDefaultSerializer(Arrays.asList("").getClass(), new JavaSerializer());
    }

}
