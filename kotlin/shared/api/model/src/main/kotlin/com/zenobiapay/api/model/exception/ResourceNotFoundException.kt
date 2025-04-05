package com.zenobiapay.api.model.exception

class ResourceNotFoundException(resourceType: String) : ZenobiaExternalException("Could not find resource $resourceType")
