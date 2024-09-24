/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

-- noinspection SqlDialectInspectionForFile
-- https://www.red-gate.com/blog/database-devops/flyway-naming-patterns-matter

-------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS document (
    document_id UUID,
    owner_id UUID NOT NULL,
    group_id UUID NOT NULL,
    document_type_id INT NOT NULL,
    description VARCHAR(2048),
    original_name VARCHAR(1024) NOT NULL,
    storage_name VARCHAR(4098) NOT NULL,
    location VARCHAR(8192) NOT NULL,
    is_ciphered BOOLEAN NOT NULL DEFAULT FALSE,
    document_size LONG NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_document_id PRIMARY KEY (document_id)
);

CREATE TRIGGER IF NOT EXISTS tg_document__updated_at
BEFORE UPDATE ON document
FOR EACH ROW
CALL 'kdoc.core.database.utils.UpdateTimestampTrigger';
