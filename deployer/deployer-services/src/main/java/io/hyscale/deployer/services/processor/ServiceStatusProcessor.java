/**
 * Copyright 2019 Pramati Prism, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hyscale.deployer.services.processor;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.hyscale.commons.exception.HyscaleException;
import io.hyscale.commons.models.AuthConfig;
import io.hyscale.commons.models.DeploymentContext;
import io.hyscale.commons.models.K8sAuthorisation;
import io.hyscale.deployer.core.model.DeploymentStatus;
import io.hyscale.deployer.services.deployer.Deployer;
import io.hyscale.deployer.services.exception.DeployerErrorCodes;
import io.hyscale.deployer.services.factory.PodParentFactory;
import io.hyscale.deployer.services.handler.PodParentHandler;
import io.hyscale.deployer.services.model.PodParent;
import io.hyscale.deployer.services.model.ServiceAddress;
import io.hyscale.deployer.services.provider.K8sClientProvider;
import io.hyscale.deployer.services.util.DeploymentStatusUtil;
import io.kubernetes.client.openapi.ApiClient;

@Component
public class ServiceStatusProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ServiceStatusProcessor.class);

    @Autowired
    private K8sClientProvider clientProvider;

    @Autowired
    private Deployer deployer;
    
    @Autowired
    private PodParentProvider podParentProvider;
    
    @Autowired
    private PodParentFactory podParentFactory;

    public DeploymentStatus getServiceDeploymentStatus(AuthConfig authConfig, String appname, String serviceName,
            String namespace) throws HyscaleException {
        if (StringUtils.isBlank(serviceName)) {
            throw new HyscaleException(DeployerErrorCodes.SERVICE_REQUIRED);
        }
        PodParent podParent = null;
        try {
            ApiClient apiClient = clientProvider.get((K8sAuthorisation) authConfig);
            podParent = podParentProvider.getPodParent(apiClient, appname, serviceName, namespace);
        } catch (HyscaleException e) {
            logger.error("Error while fetching status {} ", e);
            throw e;
        }

        if (podParent == null) {
            return DeploymentStatusUtil.getNotDeployedStatus(serviceName);
        }
        PodParentHandler podParentHandler = podParentFactory.getHandlerOf(podParent.getKind());
        return updateServiceAddress(podParentHandler.buildStatus(podParent.getParent()), authConfig, appname,
                namespace);
    }

    public List<DeploymentStatus> getDeploymentStatus(AuthConfig authConfig, String appname, String namespace)
            throws HyscaleException {
        List<PodParent> podParentList = null;
        try {
            ApiClient apiClient = clientProvider.get((K8sAuthorisation) authConfig);
            podParentList = podParentProvider.getPodParents(apiClient, appname, namespace);
        } catch (HyscaleException e) {
            logger.error("Error while fetching status {} ", e);
            throw e;

        }
        if (podParentList == null || podParentList.isEmpty()) {
            return null;
        }
        List<DeploymentStatus> deploymentStatusList = podParentList.stream().map(each -> {
            PodParentHandler podParentHandler = podParentFactory.getHandlerOf(each.getKind());
            return podParentHandler.buildStatus(each.getParent());
        }).collect(Collectors.toList());
        return deploymentStatusList.stream().map(each -> updateServiceAddress(each, authConfig, appname, namespace))
                .collect(Collectors.toList());
    }

    private DeploymentStatus updateServiceAddress(DeploymentStatus deploymentStatus, AuthConfig authConfig,
            String appName, String namespace) {
        DeploymentContext context = new DeploymentContext();
        context.setAuthConfig(authConfig);
        context.setAppName(appName);
        context.setNamespace(namespace);
        context.setServiceName(deploymentStatus.getServiceName());
        context.setWaitForReadiness(false);
        try {
            ServiceAddress serviceAddress = deployer.getServiceAddress(context);
            if (serviceAddress != null) {
                deploymentStatus.setServiceAddress(serviceAddress.toString());
            }
        } catch (HyscaleException e) {
            if (DeployerErrorCodes.RESOURCE_NOT_FOUND != e.getHyscaleErrorCode()) {
                logger.debug("Failed to get service address {} ", e.getHyscaleErrorCode());
                deploymentStatus.setServiceAddress("Failed to get service address, try again");
            }
        }
        return deploymentStatus;
    }
}
