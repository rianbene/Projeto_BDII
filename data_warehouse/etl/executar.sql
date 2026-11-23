\set ON_ERROR_STOP on
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT etl.executar_carga() AS execucao_id \gset
COMMIT;

SELECT * FROM etl.execucao WHERE id = :execucao_id;
SELECT esquema, tabela, antes, depois, depois - antes AS diferenca
FROM etl.contagem
WHERE execucao_id = :execucao_id
ORDER BY esquema, tabela;
