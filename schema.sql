-- =============================================================================
-- BASE DE DONNÉES SQL POUR L'APPLICATION DE GESTION DES CONGÉS & ABSENCES
-- Compatible avec SQLite (Room), MySQL, PostgreSQL et MariaDB
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TABLE DES UTILISATEURS (COLLABORATEURS & ADMINISTRATEURS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(255) PRIMARY KEY NOT NULL,
    matricule VARCHAR(50) NOT NULL UNIQUE,           -- Identifiant / Matricule employé auto-incrémenté (ex: EMP-0001)
    fullName VARCHAR(255) NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    jobTitle VARCHAR(150) NOT NULL DEFAULT 'Collaborateur',
    department VARCHAR(150) NOT NULL DEFAULT 'Ingénierie & IT',
    phone VARCHAR(50) NOT NULL DEFAULT '+33 6 12 34 56 78',
    hireDate VARCHAR(100) NOT NULL DEFAULT '15 Janvier 2022',
    officeLocation VARCHAR(255) NOT NULL DEFAULT 'Paris - Site A, Étage 3',
    avatarUrl TEXT NOT NULL DEFAULT 'https://i.pravatar.cc/150?img=11',
    paidLeaveAllowance INTEGER NOT NULL DEFAULT 25,
    paidLeaveUsed INTEGER NOT NULL DEFAULT 0,
    rttAllowance INTEGER NOT NULL DEFAULT 10,
    rttUsed INTEGER NOT NULL DEFAULT 0
);

-- -----------------------------------------------------------------------------
-- 2. TABLE DES DEMANDES DE CONGÉS (LEAVE REQUESTS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_requests (
    id VARCHAR(100) PRIMARY KEY NOT NULL,
    employeeEmail VARCHAR(255) NOT NULL,
    employeeName VARCHAR(255) NOT NULL,
    department VARCHAR(150) NOT NULL,
    leaveType VARCHAR(100) NOT NULL,                 -- 'Congés Payés', 'RTT', 'Télétravail', etc.
    startDate VARCHAR(50) NOT NULL,                  -- Format: 'yyyy-MM-dd'
    endDate VARCHAR(50) NOT NULL,                    -- Format: 'yyyy-MM-dd'
    startDay INTEGER NOT NULL,                       -- Jour du mois (1-31)
    endDay INTEGER NOT NULL,                         -- Jour du mois (1-31)
    month INTEGER NOT NULL,                          -- 0 pour Janvier ... 11 pour Décembre
    year INTEGER NOT NULL,                           -- ex: 2026
    daysCount INTEGER NOT NULL,                      -- Nombre de jours demandés
    reason TEXT NOT NULL,                            -- Motif de la demande
    attachmentName VARCHAR(255) DEFAULT NULL,        -- Nom du justificatif éventuel
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- 'PENDING', 'APPROVED', 'REJECTED'
    createdAt BIGINT NOT NULL,                       -- Timestamp en millisecondes
    decisionAt BIGINT DEFAULT NULL,                  -- Timestamp de validation / refus
    adminComment TEXT DEFAULT NULL,                  -- Commentaire du manager / RH
    alertDismissedByEmployee INTEGER NOT NULL DEFAULT 0, -- 0 = Faux, 1 = Vrai
    FOREIGN KEY (employeeEmail) REFERENCES users(email) ON DELETE CASCADE
);

-- Index pour accélérer les recherches de demandes par collaborateur et par statut
CREATE INDEX IF NOT EXISTS idx_requests_employee ON leave_requests (employeeEmail);
CREATE INDEX IF NOT EXISTS idx_requests_status ON leave_requests (status);
CREATE INDEX IF NOT EXISTS idx_requests_created ON leave_requests (createdAt DESC);

-- -----------------------------------------------------------------------------
-- 3. TABLE DES NOTIFICATIONS ET ALERTES (APP ALERTS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_alerts (
    id VARCHAR(100) PRIMARY KEY NOT NULL,
    targetUserEmail VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,                       -- 'SUBMITTED', 'APPROVED', 'REJECTED'
    timestamp BIGINT NOT NULL,                       -- Timestamp en millisecondes
    isRead INTEGER NOT NULL DEFAULT 0,               -- 0 = Non lu, 1 = Lu
    isPopupShown INTEGER NOT NULL DEFAULT 0,         -- 0 = Non affiché, 1 = Déjà affiché
    FOREIGN KEY (targetUserEmail) REFERENCES users(email) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alerts_user ON app_alerts (targetUserEmail);

-- =============================================================================
-- DONNÉES INITIALES PAR DÉFAUT (SEEDS)
-- =============================================================================

-- Administrateur RH Principal
INSERT OR REPLACE INTO users (
    email, matricule, fullName, passwordHash, jobTitle, department, phone, hireDate, officeLocation, avatarUrl, paidLeaveAllowance, paidLeaveUsed, rttAllowance, rttUsed
) VALUES (
    'elmzabitemohamedtaha@gmail.com',
    'ADM-0001',
    'Mohamed Taha El Mzabite',
    'MOHAMEDTAHA123',
    'Responsable RH & Administrateur',
    'Ressources Humaines & Direction',
    '+33 6 12 34 56 78',
    '01 Janvier 2021',
    'Paris - Siège Principal, Étage 4',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300',
    30, 8, 12, 3
);







INSERT OR REPLACE INTO leave_requests (
    id, employeeEmail, employeeName, department, leaveType, startDate, endDate, startDay, endDay, month, year, daysCount, reason, status, createdAt
) VALUES (
    'req_init_002',
    'mouad@gmail.com',
    'mouad elmzabite',
    'Design & Produit',
    'RTT',
    '2026-11-15',
    '2026-11-15',
    15, 15, 10, 2026,
    1,
    'Récupération projet Design System',
    'PENDING',
    1771900000000
);

-- =============================================================================
-- REQUÊTES SQL COURANTES (EXEMPLES D'EXPLOITATION)
-- =============================================================================

-- 1. Lister toutes les demandes en attente de validation (pour les RH) :
-- SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY createdAt ASC;

-- 2. Obtenir le solde restant d'un employé :
-- SELECT fullName, (paidLeaveAllowance - paidLeaveUsed) AS soldeCongesPayes, (rttAllowance - rttUsed) AS soldeRTT FROM users WHERE email = 'mouad@gmail.com';

-- 3. Valider une demande de congé :
-- UPDATE leave_requests SET status = 'APPROVED', decisionAt = 1771950000000, adminComment = 'Approuvé sans réserve' WHERE id = 'req_init_002';
