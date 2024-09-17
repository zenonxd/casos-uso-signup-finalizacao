<p align="center">
  <img src="https://img.shields.io/static/v1?label=SpringExpert - Dev Superior&message=Testes Automatizados&color=8257E5&labelColor=000000" alt="Testes automatizados na prática com Spring Boot" />
</p>

# Competências

- Realização de caso de uso
  - Consulta detalhada de produtos
  - Signup
  - Recuperação de senha
  - Obter usuário logado
- Consultas ao banco dados
- Envio de email com Gmail


# Tópicos



# Objetivo

Faremos alguns casos de uso em cima do projeto DSCatalog, envio de email, recuperação de senha e mais.

# UML

![img.png](img.png)

# Evitando consulta lenta (ManyToOne) - countQuery

## O que é?

Devemos tomar cuidados para que a nossa consulta não fique ineficiente (olhando os logs SQL).

Ineficiente por sua vez, entenda lento. Em virtude do comoportamento lazy (carregar de forma tardia) os objetos, 
devemos citar o seguinte: enquanto a sessão JPA estiver ATIVA, o acesso a um objeto associado (uma simples consulta) 
pode provocar várias consultas ao banco (voltando nele várias vezes).

Você pode ler mais sobre [aqui](https://olavo-moreira.gitbook.io/studies/v/jpa-consultas-sql-e-jpql/evitando-degradacao-de-performance-lentidao-jpa/analisando-o-carregamento-lazy-dos-funcionarios)

## Como resolver?

Usaremos uma cláusula da JPQL chamada Join Fetch.

Ao invés de usar aquele findAll padrão de sempre, criaremos um novo método no Repository.

O Join Fetch basicamente, força o Join a buscar o Product com as categorias associadas (mesmo com a tabela de associação).

![img_1.png](img_1.png)

Mas a ideia é fazer corretamente com o Pageable, podemos passar ele também no searchAll (no controller/service).

A consulta é parecida, mas agora usamos countQuery (ele serve para dizer ao Spring, quantos elementos vamos buscar).
Usa o count no Obj e não usa o Fetch.

![img_2.png](img_2.png)

# Começando com casos de uso

## Consulta paginada de produtos

![img_3.png](img_3.png)

1. Para o sistema informar id e nome de TODAS as categoria de produto, é só mudar o endpoint. Ao invés de retornar paged
no findAllPaged, irá retornar uma lista (Isso nas classes de Category).


2. Uma requisição exemplo (o que usuário irá informar):``/products?page=0&size=12&name=ma&categoryId=1,3``

Faremos agora a consulta no Repository para que ele consiga encontrar as categorias e filtrar por nome. A ideia dessa
consulta é encontrar os IDS dos Produtos que vão fazer parte da página.

Depois que pegarmos esses Ids dos Produtos, usaremos ele de argumento na outra consulta, que encontrará os produtos com
as categorias.

Consulta feita no H2:

![img_4.png](img_4.png)

Agora iremos no Repository (Products), e criar o método searchProducts. Seus parâmetros serão exatamente o que está na
requisição acima.

Além disso, retornará um Page do tipo ProductProjection.

O ProductProjection irá representar o retorno da consulta SQL feita no h2: id e name.

![img_5.png](img_5.png)

![img_6.png](img_6.png)

Passar o @Query no método com a consulta customizada. Se for consulta simples: JPQL, mais elaborada: SQL raíz.

Benefício da JPQL é que podemos instanciar a entidade monitorada pela JPA.

Já o SQL raíz, precisamos usar Projection (não monitorada).

Mas o controle que temos da consulta é 100% nosso (usando SQL raíz).

Como nesse caso temos DISTINCT, JOIN, condições de WHERE, usaremos a raíz.

Consulta de referência:
-

A unica diferença, é que iremos renomear algumas coisas, passando os parâmetros.

Exemplo: ao invés de usar (1,3) para referenciar a ID das categorias, usaremos IN :categoryIds < parâmetro.

Outra coisa, como temos um pageable, precisamos usar o countQuery.

Como já temos o DISTINCT, no início faremos o SELECT COUNT(*) FROM (). E no final, como é uma subconsulta, usaremos o
AS tb_result.

```
@Query(nativeQuery = true, value = """
	SELECT DISTINCT tb_product.id, tb_product.name
	FROM tb_product
	INNER JOIN tb_product_category ON tb_product_category.product_id = tb_product.id
	WHERE (:categoryIds IS NULL OR tb_product_category.category_id IN :categoryIds)
	AND (LOWER(tb_product.name) LIKE LOWER(CONCAT('%',:name,'%')))
	ORDER BY tb_product.name
	""",
	countQuery = """
	SELECT COUNT(*) FROM (
	SELECT DISTINCT tb_product.id, tb_product.name
	FROM tb_product
	INNER JOIN tb_product_category ON tb_product_category.product_id = tb_product.id
	WHERE (:categoryIds IS NULL OR tb_product_category.category_id IN :categoryIds)
	AND (LOWER(tb_product.name) LIKE LOWER(CONCAT('%',:name,'%')))
	) AS tb_result
	""")
Page<ProductProjection> searchProducts(List<Long> categoryIds, String name, Pageable pageable);

@Query("SELECT obj FROM Product obj JOIN FETCH obj.categories "
		+ "WHERE obj.id IN :productIds ORDER BY obj.name")
List<Product> searchProductsWithCategories(List<Long> productIds);
```




