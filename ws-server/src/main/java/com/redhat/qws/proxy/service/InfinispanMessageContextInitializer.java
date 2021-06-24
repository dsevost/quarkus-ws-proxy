package com.redhat.qws.proxy.service;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { CachedMessage.class }, schemaPackageName = "ws.proxy")
public interface InfinispanMessageContextInitializer extends SerializationContextInitializer {
}
