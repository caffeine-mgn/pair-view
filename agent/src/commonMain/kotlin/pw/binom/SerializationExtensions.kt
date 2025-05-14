package pw.binom

import kotlinx.serialization.descriptors.SerialDescriptor

inline fun <reified T : Any> SerialDescriptor.getAnnotation(): T? =
    annotations.find { it is T } as T?

inline fun <reified T : Any> SerialDescriptor.getElementAnnotation(index: Int): T? =
    getElementAnnotations(index).find { it is T } as T?