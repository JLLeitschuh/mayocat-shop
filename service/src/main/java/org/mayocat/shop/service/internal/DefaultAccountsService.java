package org.mayocat.shop.service.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;

import org.mayocat.shop.authorization.PasswordManager;
import org.mayocat.shop.configuration.MultitenancyConfiguration;
import org.mayocat.shop.model.Role;
import org.mayocat.shop.model.Tenant;
import org.mayocat.shop.model.TenantConfiguration;
import org.mayocat.shop.model.User;
import org.mayocat.shop.service.AccountsService;
import org.mayocat.shop.store.EntityAlreadyExistsException;
import org.mayocat.shop.store.EntityDoesNotExistException;
import org.mayocat.shop.store.InvalidEntityException;
import org.mayocat.shop.store.TenantStore;
import org.mayocat.shop.store.UserStore;
import org.xwiki.component.annotation.Component;

@Component
public class DefaultAccountsService implements AccountsService
{
    @Inject
    private Provider<TenantStore> tenantStore;

    @Inject
    private Provider<UserStore> userStore;

    @Inject
    private PasswordManager passwordManager;

    @Inject
    private MultitenancyConfiguration multitenancyConfiguration;

    @Override
    public void createInitialUser(User user) throws EntityAlreadyExistsException, InvalidEntityException
    {
        if (this.hasUsers()) {
            throw new RuntimeException("Illegal attempt at create the initial user");
        }
        this.create(user, Role.ADMIN);
    }

    @Override
    public void createUser(@Valid User user) throws EntityAlreadyExistsException, InvalidEntityException
    {
        this.create(user, Role.NONE);
    }

    @Override
    public Tenant findTenant(String slug)
    {
        return this.tenantStore.get().findBySlug(slug);
    }

    @Override
    public Tenant createDefaultTenant() throws EntityAlreadyExistsException
    {
        if (this.tenantStore.get().findAll(1, 0).size() != 0) {
            throw new EntityAlreadyExistsException("Cannot create default tenant : a tenant already exists");
        }
        String slug = multitenancyConfiguration.getDefaultTenantSlug();
        TenantConfiguration configuration = new TenantConfiguration(0);
        Tenant tenant = new Tenant(slug, configuration);
        try {
            this.tenantStore.get().create(tenant);
        } catch (InvalidEntityException e) {
        }
        return this.tenantStore.get().findBySlug(slug);
    }

    @Override
    public void createTenant(@Valid Tenant tenant) throws EntityAlreadyExistsException, InvalidEntityException
    {
        this.tenantStore.get().create(tenant);
    }

    @Override
    public void updateTenant(@Valid Tenant tenant) throws EntityDoesNotExistException, InvalidEntityException
    {
        this.tenantStore.get().update(tenant);
    }

    @Override
    public boolean hasUsers()
    {
        return this.userStore.get().findAll(1, 0).size() > 0;
    }

    @Override
    public User findUserByEmailOrUserName(String userNameOrEmail)
    {
        return this.userStore.get().findUserByEmailOrUserName(userNameOrEmail);
    }

    @Override
    public List<Role> findRolesForUser(User user)
    {
        return this.userStore.get().findRolesForUser(user);
    }

    private void create(User user, Role initialRole) throws InvalidEntityException, EntityAlreadyExistsException
    {
        user.setPassword(this.passwordManager.hashPassword(user.getPassword()));

        this.userStore.get().create(user, initialRole);
    }

}