-- SmartPark: lokacije, mesta, cenovnik i rezervacije (FR-06 do FR-12, FR-15)
-- Ako vec postoji migracija sa brojem V2, preimenujte fajl u sledeci slobodan broj (npr. V3__...).
-- Pretpostavka: tabela users (user_id BIGINT) je vec napravljena u V1.

-- Potrebno za EXCLUDE ogranicenje (zabrana preklapanja rezervacija na nivou baze)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS parking_lots (
    lot_id      BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    capacity    INTEGER      NOT NULL CHECK (capacity > 0),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS parking_spots (
    spot_id      BIGSERIAL PRIMARY KEY,
    lot_id       BIGINT      NOT NULL REFERENCES parking_lots (lot_id),
    spot_number  VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'SLOBODNO'
                 CHECK (status IN ('SLOBODNO', 'REZERVISANO', 'ZAUZETO', 'VAN_UPOTREBE')),
    CONSTRAINT uq_spot_number_per_lot UNIQUE (lot_id, spot_number)
);

-- Razlog za "van upotrebe" (FR-08). ADD COLUMN IF NOT EXISTS radi i ako je tabela vec postojala.
ALTER TABLE parking_spots ADD COLUMN IF NOT EXISTS out_of_service_reason VARCHAR(255);

CREATE TABLE IF NOT EXISTS price_lists (
    price_id        BIGSERIAL PRIMARY KEY,
    lot_id          BIGINT        NOT NULL REFERENCES parking_lots (lot_id),
    price_per_hour  NUMERIC(10,2) NOT NULL CHECK (price_per_hour >= 0),
    min_charge      NUMERIC(10,2) NOT NULL CHECK (min_charge >= 0),
    overstay_fine   NUMERIC(10,2) NOT NULL CHECK (overstay_fine >= 0),
    no_show_fine    NUMERIC(10,2) NOT NULL CHECK (no_show_fine >= 0),
    valid_from      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_price_list_version UNIQUE (lot_id, valid_from)
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id  BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      REFERENCES users (user_id) ON DELETE SET NULL,
    spot_id         BIGINT      NOT NULL REFERENCES parking_spots (spot_id),
    plate_number    VARCHAR(15) NOT NULL,
    start_at        TIMESTAMPTZ NOT NULL,
    end_at          TIMESTAMPTZ NOT NULL,
    status          VARCHAR(25) NOT NULL
                    CHECK (status IN ('KREIRANA', 'POTVRDJENA', 'OTKAZANA', 'ISTEKLA',
                                      'AKTIVNA', 'PLACANJE_NEUSPESNO', 'ZAVRSENA')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reservation_interval CHECK (end_at > start_at)
);

-- Dopuna provere iz poslovne logike: dve "zive" rezervacije istog mesta ne smeju da se preklapaju.
ALTER TABLE reservations
    ADD CONSTRAINT reservations_no_overlap
    EXCLUDE USING gist (
        spot_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status IN ('KREIRANA', 'POTVRDJENA', 'AKTIVNA', 'PLACANJE_NEUSPESNO'));

CREATE INDEX IF NOT EXISTS idx_reservations_user   ON reservations (user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations (status, start_at);
CREATE INDEX IF NOT EXISTS idx_spots_lot           ON parking_spots (lot_id);
