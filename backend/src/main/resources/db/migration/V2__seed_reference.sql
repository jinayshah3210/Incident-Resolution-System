-- ResolveIT reference data, version 2: categories + classifier keywords, IT services, SLA policy and strategy settings.
-- Demo people and demo incidents are inserted by the Java DemoDataSeeder (only when demo mode is on).

INSERT INTO categories (name, description, active) VALUES
 ('Hardware',       'Laptops, desktops, printers and peripherals', TRUE),
 ('Software',       'Desktop applications, licences and updates', TRUE),
 ('Network',        'Wi-Fi, VPN, LAN, DNS and internet connectivity', TRUE),
 ('Database',       'Database servers, queries, replication and backups', TRUE),
 ('Cloud',          'Cloud platforms, virtual machines and containers', TRUE),
 ('Security',       'Phishing, malware, suspicious activity and breaches', TRUE),
 ('Authentication', 'Login, passwords, single sign-on and multi-factor access', TRUE),
 ('Application',    'Business applications such as payment, portal and ERP', TRUE),
 ('Storage',        'File shares, cloud drives, disks, quotas and backups', TRUE);

INSERT INTO category_keywords (category_id, keyword, weight)
SELECT c.id, v.keyword, v.weight
FROM categories c
JOIN (VALUES
 ('Hardware','laptop',2),('Hardware','printer',2),('Hardware','keyboard',2),('Hardware','monitor',2),('Hardware','mouse',2),
 ('Hardware','battery',2),('Hardware','screen',1),('Hardware','docking',2),('Hardware','headset',2),('Hardware','webcam',2),
 ('Hardware','charger',2),('Hardware','desktop',1),('Hardware','hardware',2),('Hardware','toner',2),('Hardware','scanner',2),
 ('Hardware','projector',2),('Hardware','overheating',2),('Hardware','paper jam',3),
 ('Software','install',2),('Software','installation',2),('Software','license',2),('Software','licence',2),('Software','update',1),
 ('Software','upgrade',1),('Software','office',1),('Software','excel',2),('Software','word',1),('Software','teams',1),
 ('Software','outlook',1),('Software','crash on open',3),('Software','antivirus',1),('Software','software',2),('Software','adobe',2),
 ('Software','patch',1),('Software','plugin',2),('Software','driver',1),
 ('Network','wifi',3),('Network','wi-fi',3),('Network','vpn',3),('Network','router',2),('Network','switch',1),('Network','latency',2),
 ('Network','dns',2),('Network','internet',2),('Network','lan',2),('Network','network',2),('Network','firewall',1),('Network','packet loss',3),
 ('Network','bandwidth',2),('Network','connectivity',2),('Network','ethernet',2),('Network','proxy',1),('Network','disconnects',1),
 ('Database','sql',3),('Database','query',2),('Database','database',3),('Database','deadlock',3),('Database','replication',3),
 ('Database','table',1),('Database','index',1),('Database','postgres',2),('Database','oracle',2),('Database','schema',2),
 ('Database','db',2),('Database','stored procedure',3),('Database','timeout',1),('Database','connection pool',2),('Database','reporting',1),
 ('Cloud','aws',3),('Cloud','azure',3),('Cloud','cloud',2),('Cloud','vm',2),('Cloud','instance',2),('Cloud','bucket',2),
 ('Cloud','container',2),('Cloud','kubernetes',3),('Cloud','docker',2),('Cloud','lambda',2),('Cloud','gcp',3),('Cloud','autoscaling',2),
 ('Cloud','load balancer',2),('Cloud','pipeline',1),('Cloud','deployment',1),
 ('Security','phishing',3),('Security','malware',3),('Security','virus',3),('Security','breach',3),('Security','suspicious',2),
 ('Security','ransomware',3),('Security','spam',1),('Security','hacked',3),('Security','compromised',3),('Security','attack',2),
 ('Security','encryption',1),('Security','unauthorised',2),('Security','unauthorized',2),('Security','security',2),('Security','trojan',3),
 ('Security','scam',2),
 ('Authentication','login',3),('Authentication','log in',3),('Authentication','password',3),('Authentication','sso',3),('Authentication','otp',3),
 ('Authentication','locked',2),('Authentication','access denied',3),('Authentication','token',1),('Authentication','mfa',3),
 ('Authentication','sign in',3),('Authentication','authentication',3),('Authentication','credentials',2),('Authentication','reset',1),
 ('Authentication','account',1),('Authentication','permission',1),('Authentication','2fa',3),
 ('Application','app',1),('Application','application',2),('Application','payment',3),('Application','transaction',3),
 ('Application','transactions',3),('Application','portal',2),('Application','crash',1),('Application','erp',3),('Application','checkout',3),
 ('Application','invoice',2),('Application','billing',2),('Application','payroll',2),('Application','crm',2),('Application','sap',2),
 ('Application','error 500',2),('Application','api',1),('Application','website',1),('Application','order',1),
 ('Storage','disk',2),('Storage','storage',3),('Storage','backup',2),('Storage','nas',3),('Storage','quota',3),('Storage','drive full',3),
 ('Storage','shared drive',3),('Storage','file share',3),('Storage','onedrive',3),('Storage','sharepoint',2),('Storage','sync',1),
 ('Storage','folder',1),('Storage','files',1),('Storage','restore',2),('Storage','space',1)
) AS v(category, keyword, weight) ON c.name = v.category;

INSERT INTO services (name, description, category, criticality, keywords, active) VALUES
 ('Payment',             'Payment transactions and checkout services',           'Application',    4, 'payment,transaction,transactions,checkout,billing,invoice,card,refund', TRUE),
 ('Authentication',      'User login, identity and access management',           'Authentication', 4, 'login,log in,password,sso,otp,mfa,sign in,locked out,2fa,credentials', TRUE),
 ('Database',            'Databases, queries, replication and backups',           'Database',       4, 'database,sql,query,deadlock,replication,db,postgres,oracle,stored procedure', TRUE),
 ('Network',             'Wi-Fi, VPN and internal connectivity',                  'Network',        3, 'wifi,wi-fi,vpn,network,internet,router,dns,lan,latency,ethernet,firewall', TRUE),
 ('Cloud Storage',       'Cloud files, storage and shared drives',                'Storage',        3, 'cloud,storage,shared drive,onedrive,sharepoint,bucket,quota,file share,nas,backup', TRUE),
 ('Email',               'Corporate email and messaging',                         'Software',       3, 'email,e-mail,outlook,mailbox,inbox,smtp,calendar,mail', TRUE),
 ('ERP',                 'Enterprise resource planning applications',             'Application',    3, 'erp,sap,payroll,procurement,purchase order,inventory,crm', TRUE),
 ('Endpoint / Hardware', 'Laptops, peripherals and office hardware',              'Hardware',       2, 'laptop,printer,keyboard,monitor,mouse,battery,docking,headset,webcam,scanner,projector,charger', TRUE),
 ('General IT',          'General service desk support and triage',               'Application',    1, '', TRUE);

INSERT INTO settings (setting_key, setting_value) VALUES
 ('priority.strategy',          'MULTIPLICATIVE'),
 ('assignment.strategy',        'LEAST_LOAD'),
 ('classifier.strategy',        'KEYWORD'),
 ('sla.warning.percent',        '80'),
 ('escalation.enabled',         'true'),
 ('sla.P1.response.minutes',    '5'),
 ('sla.P1.resolution.minutes',  '30'),
 ('sla.P2.response.minutes',    '15'),
 ('sla.P2.resolution.minutes',  '120'),
 ('sla.P3.response.minutes',    '60'),
 ('sla.P3.resolution.minutes',  '480'),
 ('sla.P4.response.minutes',    '240'),
 ('sla.P4.resolution.minutes',  '1440');
