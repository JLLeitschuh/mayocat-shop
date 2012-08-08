package org.mayocat.shop.application;

import java.util.Map;

import org.mayocat.shop.base.EventListener;
import org.mayocat.shop.base.HealthCheck;
import org.mayocat.shop.base.Provider;
import org.mayocat.shop.configuration.DataSourceConfiguration;
import org.mayocat.shop.configuration.MayocatShopConfiguration;
import org.mayocat.shop.configuration.MultitenancyConfiguration;
import org.mayocat.shop.rest.resources.Resource;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;

import com.google.common.cache.CacheBuilderSpec;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.bundles.AssetsBundle;
import com.yammer.dropwizard.config.Environment;

public class MayocatShopService extends Service<MayocatShopConfiguration>
{
    private EmbeddableComponentManager componentManager;

    public static void main(String[] args) throws Exception
    {
        new MayocatShopService().run(args);
    }

    private MayocatShopService()
    {
        super("MayocatShop");

        //CacheBuilderSpec cacheSpec = AssetsBundle.DEFAULT_CACHE_SPEC;
        CacheBuilderSpec cacheSpec = CacheBuilderSpec.disableCaching();
        addBundle(new AssetsBundle("/client/", cacheSpec, "/admin/"));
    }

    @Override
    protected void initialize(MayocatShopConfiguration configuration, Environment environment) throws Exception
    {

        // Initialize Rendering components and allow getting instances
        componentManager = new EmbeddableComponentManager();

        this.registerConfigurationsAsComponents(configuration);

        componentManager.initialize(this.getClass().getClassLoader());

        // Registering provider component implementations against the environment...
        Map<String, Resource> providers = componentManager.getInstanceMap(Provider.class);
        for (Map.Entry<String, Resource> provider : providers.entrySet()) {
            environment.addProvider(provider.getValue());
        }

        // Registering resources component implementations against the environment...
        Map<String, Resource> restResources = componentManager.getInstanceMap(Resource.class);
        for (Map.Entry<String, Resource> resource : restResources.entrySet()) {
            environment.addResource(resource.getValue());
        }

        // Registering revent listeners implementations against the environment
        Map<String, EventListener> eventListeners = componentManager.getInstanceMap(EventListener.class);
        for (Map.Entry<String, EventListener> listener : eventListeners.entrySet()) {
            environment.addServletListeners(listener.getValue());
        }

        // Registering health checks implementations against the environment
        Map<String, HealthCheck> healthChecks = componentManager.getInstanceMap(HealthCheck.class);
        for (Map.Entry<String, HealthCheck> check : healthChecks.entrySet()) {
            if (com.yammer.metrics.core.HealthCheck.class.isAssignableFrom(check.getValue().getClass())) {
                environment.addHealthCheck((com.yammer.metrics.core.HealthCheck) check.getValue());
            }
        }

    }

    private void registerConfigurationsAsComponents(MayocatShopConfiguration configuration)
    {
        DefaultComponentDescriptor<MayocatShopConfiguration> cd =
            new DefaultComponentDescriptor<MayocatShopConfiguration>();
        cd.setRoleType(MayocatShopConfiguration.class);
        componentManager.registerComponent(cd, configuration);

        DefaultComponentDescriptor<DataSourceConfiguration> cd2 =
            new DefaultComponentDescriptor<DataSourceConfiguration>();
        cd2.setRoleType(DataSourceConfiguration.class);
        componentManager.registerComponent(cd2, configuration.getDataSourceConfiguration());

        DefaultComponentDescriptor<MultitenancyConfiguration> cd3 =
            new DefaultComponentDescriptor<MultitenancyConfiguration>();
        cd3.setRoleType(MultitenancyConfiguration.class);
        componentManager.registerComponent(cd3, configuration.getMultitenancyConfiguration());
    }

}