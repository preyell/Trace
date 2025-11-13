package com.sybyl.trace.config;

import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final VerticalRepository verticals;
    private final PasswordEncoder enc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure roles exist

        // Ensure a sample vertical exists (for later user accounts)
       // Vertical fin = findOrCreateVertical("Finance");

        // Ensure admin user exists
        users.findByUsernameIgnoreCase("admin").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("admin");
            u.setPassword(enc.encode("admin123"));
            u.setEnabled(true);
            u.setFirstName("System");
            u.setLastName("Administrator");
            u.setEmail("preethiraos@gmail.com");
            u.setLocations(Set.of(Location.KENYA)); 
            u.setLocations(Set.of(Location.TANZANIA));
            u.setRoles(java.util.EnumSet.of(AppRole.ADMIN));

            return users.save(u);
        });
    }

    private Vertical findOrCreateVertical(String name) {
        return verticals.findByNameIgnoreCase(name)
                .orElseGet(() -> new Vertical(name));
        };
}
