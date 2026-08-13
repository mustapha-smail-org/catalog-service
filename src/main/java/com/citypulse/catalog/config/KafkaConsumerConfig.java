package com.citypulse.catalog.config;

import com.citypulse.catalog.exception.EventIdentityConflictException;
import com.citypulse.catalog.exception.InvalidKafkaEventException;
import com.citypulse.events.avro.EventAvro;
import io.confluent.kafka.serializers.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.hibernate.TransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.scram.ScramLoginModule;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, EventAvro> eventConsumerFactory(KafkaProperties properties) throws IOException {
        Map<String, Object> config = commonProperties(properties);

        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.consumer().groupId());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.consumer().autoOffsetReset());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        config.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ProducerFactory<Object, Object> deadLetterProducerFactory(KafkaProperties properties) throws IOException {
        Map<String, Object> config = commonProperties(properties);

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<Object, Object> deadLetterKafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public DefaultErrorHandler eventKafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate, KafkaProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(properties.topic().eventsDlt(), record.partition()));

        long backoffMs = properties.retry().backoffMs();
        long maxAttempts = properties.retry().maxAttempts();
        long fixedBackOffRetries = Math.max(0, maxAttempts - 1);

        FixedBackOff backOff = new FixedBackOff(backoffMs, fixedBackOffRetries);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Treat some exceptions as non-retryable to avoid useless retries that stress the DB
        errorHandler.addNotRetryableExceptions(
                InvalidKafkaEventException.class,
                EventIdentityConflictException.class,
                DataIntegrityViolationException.class,
                TransactionException.class,
                TransactionTimedOutException.class
        );

        errorHandler.setCommitRecovered(true);

        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) ->
                        log.warn("Kafka retry topic={}, partition={}, offset={}, attempt={}", record.topic(), record.partition(), record.offset(), deliveryAttempt, exception)
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventAvro> eventKafkaListenerContainerFactory(ConsumerFactory<String, EventAvro> consumerFactory, DefaultErrorHandler eventKafkaErrorHandler, KafkaProperties properties) {
        ConcurrentKafkaListenerContainerFactory<String, EventAvro> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(eventKafkaErrorHandler);
        factory.setConcurrency(properties.consumer().concurrency());
        factory.setAutoStartup(properties.consumer().autoStartup());

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    private Map<String, Object> commonProperties(KafkaProperties properties) throws IOException {
        Map<String, Object> config = new HashMap<>();

        config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        config.put(SaslConfigs.SASL_MECHANISM, properties.saslMechanism());
        config.put(SaslConfigs.SASL_JAAS_CONFIG, buildJaasConfiguration(properties));
        config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
        config.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, properties.caCertificate().getContentAsString(StandardCharsets.UTF_8));

        config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.schemaRegistry().url());
        config.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        config.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, properties.schemaRegistry().username() + ":" + properties.schemaRegistry().password());

        return config;
    }

    private String buildJaasConfiguration(KafkaProperties properties) {
        return "%s required username=\"%s\" password=\"%s\";".formatted(ScramLoginModule.class.getName(), escapeJaasValue(properties.username()), escapeJaasValue(properties.password()));
    }

    private String escapeJaasValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
