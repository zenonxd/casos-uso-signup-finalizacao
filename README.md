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

Uma requisição exemplo (o que usuário irá informar):``/products?page=0&size=12&name=ma&categoryId=1,3``

Faremos agora a consulta no Repository para que ele consiga encontrar as categorias e filtrar por nome. A ideia dessa
consulta é encontrar os IDS dos Produtos que vão fazer parte da página.

Depois que pegarmos esses Ids dos Produtos, usaremos ele de argumento na outra consulta, que encontrará os produtos com
as categorias.

Consulta feita no H2:

![img_4.png](img_4.png)

Agora iremos no Repository (Products), e criar o método searchProducts. Seus parâmetros serão exatamente o que está na
requisição (url) acima.

Além disso, retornará um Page do tipo ProductProjection.

O ProductProjection irá representar o retorno da consulta SQL feita no h2: id e name.

![img_5.png](img_5.png)

![img_6.png](img_6.png)

Passar o @Query no método com a consulta customizada. Se for consulta simples: JPQL, mais elaborada: SQL raíz.

Benefício da JPQL é que podemos instanciar a entidade monitorada pela JPA.

Já o SQL raíz, precisamos usar Projection (não monitorada).

Mas o controle que temos da consulta é 100% nosso (usando SQL raíz).

Como nesse caso temos DISTINCT, JOIN, condições de WHERE, usaremos a raíz.

Consulta para referência:
-

A única diferença, é que iremos renomear algumas coisas, passando os parâmetros.

Exemplo: ao invés de usar (1,3) para referenciar a ID das categorias, usaremos IN :categoryIds < parâmetro.

Outra coisa, como temos um pageable, precisamos usar o countQuery.

Como já temos o DISTINCT, no início faremos o SELECT COUNT(*) FROM (). E no final, como é uma subconsulta, usaremos o
AS tb_result.

```java
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

## Capturando parâmetros em requisição

Como podemos ver na requisição ali em cima, nós passamos alguns parâmetros (como name e categoryId). Como capturá-los?

Bom, dentro do método do Controller, iremos usar o @RequestParam com o seu valor.

## Repository

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(nativeQuery = true, value = """
	SELECT DISTINCT tb_product.id, tb_product.name
	FROM tb_product
	INNER JOIN tb_product_category ON tb_product_category.product_id = tb_product.id
	WHERE (:categoryIds IS NULL OR tb_product_category.category_id IN :categoryIds)
	AND (LOWER(tb_product.name) LIKE LOWER(CONCAT('%',:name,'%')))
	ORDER BY tb_product.name
	""")
    Page<ProductProjection> searchProducts(List<Long> categoryIds, String name, Pageable pageable);

    @Query("SELECT obj FROM Product obj JOIN FETCH obj.categories "
    + "WHERE obj.id IN :productIds ORDER BY obj.name")
    List<Product> searchProductWithCategories(List<Long> productIds);
}
```


## Controller

```java
@GetMapping
public ResponseEntity<Page<ProductDTO>> findAll(
        @RequestParam(value = "name", defaultValue = "") String name,
        @RequestParam(value = "categoryId", defaultValue = "0") String categoryId,
        Pageable pageable) {
    Page<ProductDTO> list = service.findAllPaged(name, categoryId, pageable);
    return ResponseEntity.ok().body(list);
}
```

## Service

```java
@Transactional(readOnly = true)
public Page<ProductDTO> findAllPaged(String name, String categoryId, Pageable pageable) {
    //instanciando uma lista vazia de categoryId
    List<Long> categoryIds = Arrays.asList();
    //caso essa lista não tenha "0" (aquele parâmetro que passamos no controller),
    //iremos separar os números, e convertê-los para uma lista de Long
    if (!"0".equals(categoryIds)) {
        categoryIds = Arrays.asList(categoryId.split(",")).stream().map(Long::parseLong).toList();
    }

	//instanciaremos uma Page do tipo Projection, realizando a primeira consulta feita (em sql)
	Page<ProductProjection> page = repository.searchProducts(categoryIds, name, pageable);
	//pega a page acima, e mapeia ela para uma Lista do tipo Long (para inserirmos no segundo método do repository
	//(que fizemos em JPQL)
	List<Long> productIds = page.map(x -> x.getId()).toList();

    /* agora, criamos uma lista do tipo Product e utilizamos o método criado do repository (jpql)
    * visto que ele recebe como parâmetro uma lista de Long*/
	List<Product> entity = repository.searchProductWithCategories(productIds);
	//reconvertendo a lista do tipo Produto para uma do tipo DTO
	List<ProductDTO> dtos = entity.stream().map(x -> new ProductDTO(x, x.getCategories())).toList();

	//Agora, como não é para retornar uma lista e sim Page, instanciaremos uma passando: lista de dto, o get
	//pageable e o totalElements.
	Page<ProductDTO> pageDTO = new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());

	return pageDTO;
}
```

## Quick fix: usando Sort do Pageable

Se você reparar nos métodos do Repository, estamos utilizando ORDER BY ao invés do Sort do pageable, isso não é o ideal.

1. Colocar alias de resultado em ambas as consultas SQL e remover os Order BY.

![img_7.png](img_7.png)

2. Resolver o problema do resultado desordenado 
   - Criar método estático auxiliar para gerar nova lista de entidades ordenada


3. Deixar a solução genérica (OO avançado)
   - Criar interface IdProjection<E>
   - Tipos Product e ProductProjection devem implementar IdProjection<E>
   - Refatorar o método auxiliar para que fique genérico


