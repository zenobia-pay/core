package com.zenobia.metric

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import jakarta.inject.Inject
import jakarta.inject.Named

const val METRIC_NAMESPACE = "namespace"

class MetricHelper @Inject constructor(private val cloudwatchClient: CloudWatchClient, @Named(METRIC_NAMESPACE) private val namespace: String) {
    fun putMetric(metricName: String, value: Double, dimensions: Map<String, String> = mapOf()) {
        val dimensions: List<Dimension> = dimensions.map {
            Dimension.builder()
                .name(it.key)
                .value(it.value)
                .build()
        }
        cloudwatchClient.putMetricData {
            it.namespace(namespace)
                .metricData(
                    MetricDatum.builder()
                        .metricName(metricName)
                        .value(value)
                        .unit(StandardUnit.COUNT)
                        .dimensions(dimensions)
                        .build()
                )
        }
    }

    fun <T> emitSuccessMetric(metricName: String, dimensions: Map<String, String>, block: () -> T): T {
        try {
            return block().also {
                putMetric(metricName, 1.0, dimensions)
            }
        } catch (e: Exception) {
            putMetric(metricName, 0.0, dimensions)
            throw e
        }
    }
}