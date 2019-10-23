package io.hyscale.controller.invoker;

import javax.annotation.PostConstruct;

import io.hyscale.controller.activity.ControllerActivity;
import io.hyscale.controller.builder.K8sAuthConfigBuilder;
import io.hyscale.controller.model.WorkflowContext;
import io.hyscale.controller.plugins.AppDirCleanUpHook;
import io.hyscale.controller.plugins.ServiceDirCleanUpHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.hyscale.commons.component.ComponentInvoker;
import io.hyscale.commons.exception.HyscaleException;
import io.hyscale.commons.logger.WorkflowLogger;
import io.hyscale.commons.models.DeploymentContext;
import io.hyscale.controller.core.exception.ControllerErrorCodes;
import io.hyscale.deployer.services.deployer.Deployer;

/**
 *	Undeploy component acts as a bridge between workflow controller and deployer for undeploy operation
 *	provides link between {@link WorkflowContext} and {@link DeploymentContext}
 */
@Component
public class UndeployComponentInvoker extends ComponentInvoker<WorkflowContext> {

    private static final Logger logger = LoggerFactory.getLogger(UndeployComponentInvoker.class);

    @Autowired
    private Deployer deployer;

    @Autowired
    private K8sAuthConfigBuilder authConfigBuilder;

    @Autowired
    private ServiceDirCleanUpHook serviceDirCleanUpPlugin;

    @Autowired
    private AppDirCleanUpHook appDirCleanUpPlugin;

    @PostConstruct
    public void init() {
        addHook(serviceDirCleanUpPlugin);
        addHook(appDirCleanUpPlugin);
    }
    
    
    @Override
    protected void doExecute(WorkflowContext context) throws HyscaleException {
        if (context == null) {
            return;
        }
        WorkflowLogger.header(ControllerActivity.STARTING_UNDEPLOYMENT);
        DeploymentContext deploymentContext = new DeploymentContext();
        deploymentContext.setAuthConfig(authConfigBuilder.getAuthConfig());
        deploymentContext.setNamespace(context.getNamespace());
        deploymentContext.setAppName(context.getAppName());
        deploymentContext.setServiceName(context.getServiceName());

        try {
            deployer.unDeploy(deploymentContext);
        } catch (HyscaleException ex) {
            WorkflowLogger.error(ControllerActivity.UNDEPLOYMENT_FAILED, ex.getMessage());
            throw ex;
        } finally {
            WorkflowLogger.footer();
        }
    }

    @Override
    protected void onError(WorkflowContext context, HyscaleException he) {
        WorkflowLogger.header(ControllerActivity.ERROR);
        WorkflowLogger.error(ControllerActivity.CAUSE, he != null ?
                he.getMessage() : ControllerErrorCodes.UNDEPLOYMENT_FAILED.getErrorMessage());
        context.setFailed(true);
    }

}