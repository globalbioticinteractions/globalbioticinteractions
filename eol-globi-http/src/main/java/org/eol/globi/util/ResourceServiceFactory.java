package org.eol.globi.util;

import org.eol.globi.service.ResourceService;

import java.net.URI;

public interface ResourceServiceFactory {

    public ResourceService serviceForResource(URI resource);
}
