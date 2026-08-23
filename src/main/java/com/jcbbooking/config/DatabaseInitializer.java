package com.jcbbooking.config;

import com.jcbbooking.model.*;
import com.jcbbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMenuAccessRepository roleMenuAccessRepository;
    private final RolePermissionAccessRepository rolePermissionAccessRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final DriverRepository driverRepository;
    private final ContractorRepository contractorRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking database state for initialization...");

        // Automatically clean up deprecated verification columns from database if they exist
        try {
            Number emailExists = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'email_verified' AND table_schema = DATABASE()")
                    .getSingleResult();
            if (emailExists != null && emailExists.intValue() > 0) {
                entityManager.createNativeQuery("ALTER TABLE users DROP COLUMN email_verified").executeUpdate();
                log.info("Successfully dropped email_verified column from users table");
            }
        } catch (Exception e) {
            log.warn("Error checking/dropping email_verified column: {}", e.getMessage());
        }

        try {
            Number phoneExists = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'phone_verified' AND table_schema = DATABASE()")
                    .getSingleResult();
            if (phoneExists != null && phoneExists.intValue() > 0) {
                entityManager.createNativeQuery("ALTER TABLE users DROP COLUMN phone_verified").executeUpdate();
                log.info("Successfully dropped phone_verified column from users table");
            }
        } catch (Exception e) {
            log.warn("Error checking/dropping phone_verified column: {}", e.getMessage());
        }

        if (userRepository.count() == 0) {
            log.info("Database is empty. Commencing system seeding...");
            initializeData();
        } else {
            log.info("Database already contains data. Dynamic menus active from database.");
            log.info("Checking password hashes for secure BCrypt formatting...");
            List<User> users = userRepository.findAll();
            int migratedCount = 0;
            for (User user : users) {
                String hash = user.getPasswordHash();
                if (hash != null && !hash.startsWith("$2a$") && !hash.startsWith("$2b$") && !hash.startsWith("$2y$")) {
                    log.info("Legacy plaintext password detected for user: [{}]. Auto-encrypting to secure BCrypt hash...", user.getEmail());
                    user.setPasswordHash(passwordEncoder.encode(hash));
                    userRepository.save(user);
                    migratedCount++;
                }
            }
            if (migratedCount > 0) {
                log.info("Successfully migrated {} legacy passwords to secure BCrypt hashes!", migratedCount);
            }
        }
    }

    private Menu updateOrCreateMenu(String name, String code, Menu parent, String path, String icon, int order) {
        Menu menu = menuRepository.findAll().stream()
                .filter(m -> code.equalsIgnoreCase(m.getMenuCode()))
                .findFirst()
                .orElse(null);
        if (menu == null) {
            menu = Menu.builder()
                    .menuCode(code)
                    .menuName(name)
                    .routePath(path)
                    .icon(icon)
                    .displayOrder(order)
                    .parentMenu(parent)
                    .active(true)
                    .build();
        } else {
            // Preserve user's active/inactive setting for existing menus
            menu.setMenuName(name);
            menu.setRoutePath(path);
            menu.setIcon(icon);
            menu.setDisplayOrder(order);
            if (menu.getActive() == null) {
                menu.setActive(true);
            }
        }
        menu = menuRepository.save(menu);

        if (!roleMenuAccessRepository.existsByRoleAndMenu(Role.ADMIN, menu)) {
            roleMenuAccessRepository.save(RoleMenuAccess.builder().role(Role.ADMIN).menu(menu).build());
        }
        return menu;
    }

    private void initializeData() {
        // 1. Seed Permissions
        log.info("Seeding permissions...");
        List<Permission> permissions = new ArrayList<>();
        
        // Customer permissions
        permissions.add(createPermission("Create Booking", "booking:create"));
        permissions.add(createPermission("Manage Addresses", "address:manage"));
        permissions.add(createPermission("Track Bookings", "booking:track"));
        permissions.add(createPermission("Process Payments", "payment:pay"));
        
        // Driver permissions
        permissions.add(createPermission("Receive Booking requests", "booking:receive"));
        permissions.add(createPermission("Accept Bookings", "booking:accept"));
        permissions.add(createPermission("Complete Booking", "booking:complete"));
        permissions.add(createPermission("Upload Work Proof", "proof:upload"));

        // Admin permissions
        permissions.add(createPermission("Manage Contractors", "contractor:manage"));
        permissions.add(createPermission("Approve Drivers", "driver:approve"));
        permissions.add(createPermission("Monitor Bookings", "booking:monitor"));
        permissions.add(createPermission("Monitor Payments", "payment:monitor"));
        permissions.add(createPermission("Manage Menus", "menu:manage"));
        permissions.add(createPermission("Manage Permissions", "permission:manage"));

        // Contractor permissions
        permissions.add(createPermission("Manage Assigned Drivers", "contractor:driver_manage"));
        permissions.add(createPermission("View Assigned Bookings", "contractor:assigned_bookings"));
        permissions.add(createPermission("View Earnings Summary", "contractor:view_earnings"));

        List<Permission> savedPermissions = permissionRepository.saveAll(permissions);

        // 2. Seed initial default menus if database is empty
        log.info("Seeding initial system menus...");
        Menu dashboard = updateOrCreateMenu("Dashboard", "dashboard", null, "/dashboard", "dashboard-icon", 1);
        Menu partner = updateOrCreateMenu("Partner", "partners", null, "/partners", "people-icon", 2);
        Menu product = updateOrCreateMenu("Product", "products", null, "/products", "box-icon", 3);
        Menu booking = updateOrCreateMenu("Booking", "bookings", null, "/bookings", "calendar-icon", 4);
        Menu payment = updateOrCreateMenu("Payment", "payments", null, "/payments", "money-icon", 5);
        Menu support = updateOrCreateMenu("Support", "support", null, "/support", "chat-icon", 6);
        Menu settings = updateOrCreateMenu("Setting", "settings", null, "/settings", "settings-icon", 7);

        Menu menuSetup = updateOrCreateMenu("Menu Management", "menu_setup", settings, "/settings/menus", "list-icon", 1);
        Menu permissionSetup = updateOrCreateMenu("Permission Setup", "permission_setup", settings, "/settings/permissions", "shield-icon", 2);

        // 4. Set up Role-Permission mappings
        log.info("Mapping permissions to roles...");
        
        // ADMIN gets all operational privileges
        for (Permission perm : savedPermissions) {
            String code = perm.getPermissionCode();
            if (code.startsWith("contractor:") || code.startsWith("driver:") || code.startsWith("booking:monitor") ||
                code.startsWith("payment:monitor") || code.startsWith("menu:") || code.startsWith("permission:")) {
                grantPermissionAccess(Role.ADMIN, perm);
            }
        }

        // CONTRACTOR gets contractor-specific functional scopes
        for (Permission perm : savedPermissions) {
            String code = perm.getPermissionCode();
            if (code.startsWith("contractor:driver_manage") || code.startsWith("contractor:assigned_bookings") ||
                code.startsWith("contractor:view_earnings")) {
                grantPermissionAccess(Role.CONTRACTOR, perm);
            }
        }

        // CUSTOMER gets customer-specific functional scopes
        for (Permission perm : savedPermissions) {
            String code = perm.getPermissionCode();
            if (code.equals("booking:create") || code.equals("address:manage") ||
                code.equals("booking:track") || code.equals("payment:pay")) {
                grantPermissionAccess(Role.CUSTOMER, perm);
            }
        }

        // DRIVER gets driver-specific functional scopes
        for (Permission perm : savedPermissions) {
            String code = perm.getPermissionCode();
            if (code.equals("booking:receive") || code.equals("booking:accept") ||
                code.equals("booking:complete") || code.equals("proof:upload")) {
                grantPermissionAccess(Role.DRIVER, perm);
            }
        }

        // 5. Seed Users for each of the four roles
        log.info("Creating default seed users...");

        // Admin Account
        createSeedUser("System Administrator", "+919876543210", "admin@jcbbooking.com", "AdminPassword123", Role.ADMIN, true, true);

        // Contractor Account (Rule 4: verified=true, active=true to login)
        createSeedUser("John Contractor", "+919876543211", "john@contractor.com", "ContractorPassword123", Role.CONTRACTOR, true, true);

        // Driver Account (Rule 3: verified=true, active=true to login)
        createSeedUser("David Driver", "+919876543212", "david@driver.com", "DriverPassword123", Role.DRIVER, true, true);

        // Customer Account
        createSeedUser("Robert Customer", "+919876543213", "robert@customer.com", "CustomerPassword123", Role.CUSTOMER, true, true);

        // Seed Drivers
        log.info("Seeding Driver applications...");
        User seededDriverUser = userRepository.findByPhone("+919876543212").orElse(null);
        driverRepository.save(Driver.builder()
                .fullName("David Driver")
                .phone("+919876543212")
                .email("david@driver.com")
                .userId(seededDriverUser != null ? seededDriverUser.getId() : null)
                .licenseNumber("DL-0120210012345")
                .aadhaarNumber("999988887777")
                .experience("8 years")
                .rating(4.8)
                .totalJobs(66)
                .totalEarnings(8500.0)
                .status("ACTIVE")
                .build());

        driverRepository.save(Driver.builder()
                .fullName("Rajesh Kumar")
                .phone("+919811122333")
                .email("rajesh.k@example.com")
                .licenseNumber("DL-0120230045678")
                .aadhaarNumber("444455554567")
                .experience("6 years")
                .rating(4.5)
                .totalJobs(145)
                .totalEarnings(12000.0)
                .status("PENDING_VERIFICATION")
                .build());

        driverRepository.save(Driver.builder()
                .fullName("Suresh Kumar")
                .phone("+919811122334")
                .email("suresh.k@example.com")
                .licenseNumber("DL-0120240099999")
                .aadhaarNumber("111122223333")
                .experience("1.5 years")
                .rating(3.8)
                .totalJobs(10)
                .totalEarnings(1500.0)
                .status("OFFLINE")
                .build());

        driverRepository.save(Driver.builder()
                .fullName("Ramesh Kumar")
                .phone("+919811122335")
                .email("ramesh.k@example.com")
                .licenseNumber("DL-0120240088888")
                .aadhaarNumber("555566667777")
                .experience("5 years")
                .rating(4.2)
                .totalJobs(75)
                .totalEarnings(7500.0)
                .status("BUSY")
                .build());

        // Seed Contractors
        log.info("Seeding Contractor applications...");
        User seededContractorUser = userRepository.findByPhone("+919876543211").orElse(null);
        contractorRepository.save(Contractor.builder()
                .fullName("John Contractor")
                .phone("+919876543211")
                .email("john@contractor.com")
                .userId(seededContractorUser != null ? seededContractorUser.getId() : null)
                .companyName("John Construction Co.")
                .gstNumber("29ABCDE1234F1Z5")
                .experience("12 years")
                .rating(4.7)
                .status("ACTIVE")
                .build());

        contractorRepository.save(Contractor.builder()
                .fullName("Beta Builders")
                .phone("+919811122341")
                .email("beta@builders.com")
                .companyName("Beta Builders Pvt Ltd")
                .gstNumber("29BBBBB2222B1Z2")
                .experience("3 years")
                .rating(4.0)
                .status("PENDING_VERIFICATION")
                .build());
        // Migration & Seeding for Products & Pricing Rules
        productRepository.findAll().forEach(p -> {
            String pName = p.getName() != null ? p.getName().toUpperCase() : "";
            String pCode = p.getCode() != null ? p.getCode().toUpperCase() : "";
            String pType = p.getProductType() != null ? p.getProductType().toUpperCase() : "";

            if (pName.contains("JCB") || pCode.contains("JCB") || pName.contains("EXCAVATOR") || pCode.contains("EXC") || "JCB".equals(pType)) {
                p.setProductType("HEAVY_EQUIPMENT");
                productRepository.save(p);
            } else if (pName.contains("BIKE") || pName.contains("AUTO") || pName.contains("CAR") || "BIKE_RIDE".equals(pType)) {
                p.setProductType("RIDE");
                productRepository.save(p);
            }
        });

        if (productRepository.count() == 0) {
            log.info("Seeding default products...");
            Product jcb = productRepository.save(Product.builder()
                    .name("JCB 3CX Backhoe Loader")
                    .code("JCB_3CX")
                    .productType("HEAVY_EQUIPMENT")
                    .category("HEAVY_EQUIPMENT")
                    .description("Heavy duty excavation & backhoe loading machine")
                    .active(true)
                    .build());

            Product bike = productRepository.save(Product.builder()
                    .name("Bike Taxi")
                    .code("BIKE")
                    .productType("RIDE")
                    .category("BIKE")
                    .description("Fast 2-wheeler city bike ride")
                    .active(true)
                    .build());
        }

        log.info("System successfully seeded and ready!");
    }

    private Permission createPermission(String name, String code) {
        return Permission.builder()
                .permissionName(name)
                .permissionCode(code)
                .build();
    }

    private Menu createMenu(String name, String code, Menu parent, String path, String icon, int order) {
        return Menu.builder()
                .menuName(name)
                .menuCode(code)
                .parentMenu(parent)
                .routePath(path)
                .icon(icon)
                .displayOrder(order)
                .active(true)
                .build();
    }

    private void grantMenuAccess(Role role, Menu menu) {
        roleMenuAccessRepository.save(RoleMenuAccess.builder()
                .role(role)
                .menu(menu)
                .build());
    }

    private void grantPermissionAccess(Role role, Permission permission) {
        rolePermissionAccessRepository.save(RolePermissionAccess.builder()
                .role(role)
                .permission(permission)
                .build());
    }

    private void createSeedUser(String name, String phone, String email, String password, Role role, boolean verified, boolean active) {
        User user = User.builder()
                .fullName(name)
                .phone(phone)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .verified(verified)
                .active(active)
                .build();
        userRepository.save(user);
        log.info("Seeded User - Role: [{}], Phone: [{}], Password: [{}]", role, phone, password);
    }
}
