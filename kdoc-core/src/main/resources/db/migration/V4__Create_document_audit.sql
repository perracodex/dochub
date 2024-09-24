/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

-- noinspection SqlDialectInspectionForFile

CREATE TABLE IF NOT EXISTS document_audit (
    audit_id UUID,
    operation VARCHAR(512) NOT NULL,
    actor_id UUID NULL,
    document_id UUID NULL,
    group_id UUID NULL,
    owner_id UUID NULL,
    log TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_id PRIMARY KEY (audit_id)
);

CREATE TRIGGER IF NOT EXISTS tg_document_audit__updated_at
BEFORE UPDATE ON document_audit
FOR EACH ROW
CALL 'kdoc.core.database.utils.UpdateTimestampTrigger';
