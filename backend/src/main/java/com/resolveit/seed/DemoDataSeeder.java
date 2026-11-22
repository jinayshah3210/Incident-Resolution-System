package com.resolveit.seed;

import com.resolveit.domain.catalog.ItService;
import com.resolveit.domain.catalog.ServiceTechnician;
import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.Role;
import com.resolveit.domain.user.Technician;
import com.resolveit.factory.NewUserSpec;
import com.resolveit.factory.UserFactory;
import com.resolveit.repository.ItServiceRepository;
import com.resolveit.repository.ServiceTechnicianRepository;
import com.resolveit.repository.TechnicianRepository;
import com.resolveit.repository.UserRepository;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo mode only: inserts the demo accounts that are missing (never overwrites existing passwords) and,
 * on a fresh catalog, links technicians to the services matching their skills. Reference data
 * (categories, services, SLA settings) comes from Flyway V2.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {
    public static final String DEMO_PASSWORD = "Password@123";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private final UserRepository users;
    private final TechnicianRepository technicians;
    private final ItServiceRepository services;
    private final ServiceTechnicianRepository links;
    private final UserFactory factory;
    private final TransactionTemplate transaction;
    private final boolean demoMode;
    private final boolean demoIncidents;
    private final DemoIncidentSeeder incidentSeeder;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DemoDataSeeder(UserRepository users, TechnicianRepository technicians, ItServiceRepository services,
                          ServiceTechnicianRepository links, UserFactory factory, PlatformTransactionManager transactions,
                          DemoIncidentSeeder incidentSeeder,
                          @Value("${resolveit.demo-mode:true}") boolean demoMode,
                          @Value("${resolveit.demo-incidents:true}") boolean demoIncidents) {
        this.users = users;
        this.technicians = technicians;
        this.services = services;
        this.links = links;
        this.factory = factory;
        this.transaction = new TransactionTemplate(transactions);
        this.demoMode = demoMode;
        this.demoIncidents = demoIncidents;
        this.incidentSeeder = incidentSeeder;
    }

    public boolean isRunning() { return running.get(); }

    @Override
    public void run(ApplicationArguments args) {
        if (!demoMode) return;
        running.set(true);
        try {
            transaction.executeWithoutResult(tx -> {
                int created = seedUsers();
                int linked = seedServiceLinks();
                if (created + linked > 0) log.info("Demo seed: {} accounts and {} service links added.", created, linked);
            });
            if (demoIncidents) incidentSeeder.seedIfEmpty();
        } finally {
            running.set(false);
        }
    }

    private int seedUsers() {
        int before = (int) users.count();
        admin("A001", "Alex Morgan", "admin@resolveit.test", AdminLevel.ADMINISTRATOR);
        admin("A002", "Ananya Menon", "ananya.menon@resolveit.test", AdminLevel.ADMINISTRATOR);
        admin("A003", "Kabir Sethi", "kabir.sethi@resolveit.test", AdminLevel.ADMINISTRATOR);
        admin("A004", "Rahul Verma", "it.manager@resolveit.test", AdminLevel.IT_MANAGER);
        employee("E101", "Jordan Lee", "e101@resolveit.test", "Finance");
        employee("E102", "Sam Rivera", "e102@resolveit.test", "Operations");
        String[][] employees = {
                {"E301", "Aarav Desai", "Finance"}, {"E302", "Meera Nair", "Human Resources"}, {"E303", "Zoya Khan", "Marketing"},
                {"E304", "Arjun Banerjee", "Operations"}, {"E305", "Isha Reddy", "Sales"}, {"E306", "Gurpreet Kaur", "Procurement"},
                {"E307", "Nikhil Bhosale", "Legal"}, {"E308", "Divya Iyer", "Finance"}, {"E309", "Oliver Bennett", "Customer Success"},
                {"E310", "Sofia Martinez", "Design"}};
        for (String[] e : employees) employee(e[0], e[1], email(e[1]), e[2]);
        technician("T201", "Priya Shah", "t201@resolveit.test", "Application Support", "Application,Software,Authentication", 8, 6, false);
        technician("T202", "Noah Kim", "t202@resolveit.test", "Infrastructure", "Network,Cloud,Storage,Database", 8, 5, false);
        technician("T300", "Kavya Raman", "team.lead@resolveit.test", "Service Desk Lead", "Application,Network,Database,Authentication", 6, 12, true);
        String[][] techs = {
                {"T301", "Rohit Kulkarni", "Networking", "Network,Security", "7", "8"},
                {"T302", "Fatima Sheikh", "Identity Operations", "Authentication,Security,Application", "6", "7"},
                {"T303", "Vivek Subramanian", "Database Operations", "Database,Storage,Cloud", "7", "9"},
                {"T304", "Sneha Patil", "Endpoint Support", "Hardware,Software", "9", "3"},
                {"T305", "Aditya Rao", "Cloud Platform", "Cloud,Storage,Network", "8", "5"},
                {"T306", "Pooja Chatterjee", "Application Support", "Application,Software", "8", "4"},
                {"T307", "Harleen Gill", "Security Operations", "Security,Authentication", "6", "10"},
                {"T308", "Yusuf Ansari", "Enterprise Systems", "Application,Database,Software", "8", "6"},
                {"T309", "Amina Okafor", "Infrastructure", "Network,Cloud,Hardware", "7", "4"},
                {"T310", "Leela Krishnan", "End User Computing", "Hardware,Software,Authentication", "9", "2"}};
        for (String[] t : techs) {
            technician(t[0], t[1], email(t[1]), t[2], t[3], Integer.parseInt(t[4]), Integer.parseInt(t[5]), false);
        }
        return (int) users.count() - before;
    }

    private int seedServiceLinks() {
        if (links.count() > 0) return 0;
        int count = 0;
        for (ItService service : services.findAll()) {
            for (Technician t : technicians.findAll()) {
                if (t.isTeamLead()) continue; // team leads receive escalations, not the automatic queue
                boolean qualified = service.getName().equals("General IT") || t.canHandle(service.getCategory())
                        || (service.getName().equals("Cloud Storage") && t.canHandle("Cloud"));
                if (qualified) {
                    links.save(new ServiceTechnician(service, t.getId()));
                    count++;
                }
            }
        }
        return count;
    }

    private static String email(String name) {
        return name.toLowerCase().replace(' ', '.') + "@resolveit.test";
    }

    private boolean missing(String id, String email) { return !users.existsById(id) && !users.existsByEmailIgnoreCase(email); }

    private void admin(String id, String name, String email, AdminLevel level) {
        if (missing(id, email)) users.save(factory.create(Role.ADMIN, NewUserSpec.admin(id, name, email, DEMO_PASSWORD, level)));
    }

    private void employee(String id, String name, String email, String department) {
        if (missing(id, email)) users.save(factory.create(Role.EMPLOYEE, NewUserSpec.employee(id, name, email, DEMO_PASSWORD, department)));
    }

    private void technician(String id, String name, String email, String department, String skills, int max, int experience, boolean lead) {
        if (missing(id, email)) {
            users.save(factory.create(Role.TECHNICIAN, NewUserSpec.technician(id, name, email, DEMO_PASSWORD, department,
                    Set.of(skills.split(",")), max, experience, lead)));
        }
    }
}
