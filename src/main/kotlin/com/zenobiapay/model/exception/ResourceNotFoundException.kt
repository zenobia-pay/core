package com.zenobiapay.model.exception

class ResourceNotFoundException(resourceType: String): ZenobiaExternalException("Could not find resource $resourceType")
