package com.smartpark.user;

/**
 * Tri uloge iz FR-03. Nazivi su bez dijakritika (VOZAC umesto VOZAČ)
 * da bi bili isti u bazi, u kodu i u JWT tokenu.
 */
public enum RoleName {
    VOZAC,
    OPERATER,
    ADMINISTRATOR
}
