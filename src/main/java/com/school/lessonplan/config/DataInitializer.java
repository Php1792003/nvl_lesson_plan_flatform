package com.school.lessonplan.config;

import com.school.lessonplan.campus.Campus;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import com.school.lessonplan.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           DepartmentRepository departmentRepository,
                           CampusRepository campusRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String adminEmail = appProperties.getInitialAdmin().getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Initial Admin user not found. Creating default admin account: {}", adminEmail);

            Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
            if (adminRole == null) {
                log.warn("ADMIN role not found in database. Initial admin creation skipped.");
                return;
            }

            Department dept = departmentRepository.findAll().stream().findFirst().orElse(null);
            Campus campus = campusRepository.findByIsDefaultTrue().orElse(null);

            User admin = new User();
            admin.setLastName("Ban");
            admin.setGivenName("Giám Hiệu");
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(appProperties.getInitialAdmin().getPassword()));
            admin.setStatus(UserStatus.ACTIVE);
            admin.setDepartment(dept);
            admin.setCampus(campus);
            admin.getRoles().add(adminRole);

            userRepository.save(admin);
            log.info("Default Admin account initialized successfully (Email: {}, Password: {})",
                adminEmail, appProperties.getInitialAdmin().getPassword());
        }
    }
}
