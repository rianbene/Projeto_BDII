\set ON_ERROR_STOP on
BEGIN;
SELECT pg_advisory_xact_lock(20261122, 2);
\ir ../../docs/DW_ESTRELA/ddl_estrela.sql
\ir ../../docs/DW_FLOCO_DE_NEVE/ddl_floco_de_neve.sql
\ir 01_estrutura.sql
\ir 02_pipeline.sql
COMMIT;
