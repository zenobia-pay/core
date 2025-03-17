package com.zenobiapay.api.exception

class ResourceNotFoundException(resourceType: String) : ZenobiaExternalException("Could not find resource $resourceType")
