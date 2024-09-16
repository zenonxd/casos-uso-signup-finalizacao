package com.devsuperior.dscatalog.services;

import com.devsuperior.dscatalog.dto.ProductDTO;
import com.devsuperior.dscatalog.entities.Category;
import com.devsuperior.dscatalog.entities.Product;
import com.devsuperior.dscatalog.factory.Factory;
import com.devsuperior.dscatalog.repositories.CategoryRepository;
import com.devsuperior.dscatalog.repositories.ProductRepository;
import com.devsuperior.dscatalog.services.exceptions.DatabaseException;
import com.devsuperior.dscatalog.services.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
public class ProductServiceTests {

    @InjectMocks
    private ProductService productService;

    //instanciando o objeto de "mentirinha"
    //sempre que instancia um mock devemos configurar
    //seu comportamento simulado
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private Long existingId;
    private Long nonExistingId;
    private Long dependentId;
    private PageImpl<Product> page;
    private Product product;
    private Category category;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() throws Exception {
        existingId = 1L;
        nonExistingId = 1000L;
        dependentId = 3L;
        product = Factory.createProduct();
        category = Factory.createCategory();
        productDTO = Factory.createProductDTO();
        page = new PageImpl<>(List.of(product));


        //para simularmos o valor do findAll (que recebe um Pageable, usaremos
        //ArgumentMatchers (any) para colocar "qualquer" valor.
        //Usar também um Casting de Pageable antes para esse qualquer se tornar Pageable.
        Mockito.when(productRepository.findAll((Pageable) ArgumentMatchers.any())).thenReturn(page);

        //save
        Mockito.when(productRepository.save(ArgumentMatchers.any())).thenReturn(product);

        //findById com ID existente retorna Optional Product
        Mockito.when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        //findById com ID inexistente retorna Optional Product vazio
        Mockito.when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        //dto

        //findbyId
        Mockito.when(productRepository.findById(existingId)).thenReturn(Optional.of(product));
        Mockito.when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());


        //update
        Mockito.when(productRepository.getReferenceById(existingId)).thenReturn(product);
        Mockito.when(productRepository.getReferenceById(nonExistingId)).thenThrow(ResourceNotFoundException.class);

        Mockito.when(categoryRepository.getReferenceById(existingId)).thenReturn(category);
        Mockito.when(categoryRepository.getReferenceById(nonExistingId)).thenThrow(ResourceNotFoundException.class);

        //configurando o comportamento esperado para o método existsById do seu mock productRepository.
        // Isso define o que o mock deve retornar quando chamado com diferentes IDs.
        Mockito.when(productRepository.existsById(existingId)).thenReturn(true);
        Mockito.when(productRepository.existsById(nonExistingId)).thenReturn(false);
        Mockito.when(productRepository.existsById(dependentId)).thenReturn(true);
        Mockito.doThrow(DataIntegrityViolationException.class).when(productRepository).deleteById(dependentId);
    }

    @Test
    public void findAllPagedShouldReturnPage() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<ProductDTO> result = productService.findAllPaged(pageable);

        Assertions.assertNotNull(result);
        Mockito.verify(productRepository).findAll(pageable); //ou Mockito.verify(repository, Mockito.times(1)).findAll(pageble);
    }

    @Test
    public void deleteShouldDoNothingWhenIdExists() {

        Assertions.assertDoesNotThrow(() -> {
            productService.delete(existingId);
        });

        //serve para saber se o método do repository foi chamado alguma vez na ação acima. Recomenda-se fazer isso
        Mockito.verify(productRepository, Mockito.times(1)).deleteById(existingId);
        //o spring não lança exceção quando chama o delete.

    }

    @Test
    public void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesntExist() {

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
           productService.delete(nonExistingId);
        });
    }

    @Test
    public void deleteShouldThrowDatabaseExceptionWhenDependentId() {
        Assertions.assertThrows(DatabaseException.class, () -> {
           productService.delete(dependentId);
        });
    }

    @Test
    public void findByIdShouldReturnProductDtoWhenIdExists() {
        ProductDTO result = productService.findById(existingId);

        Assertions.assertNotNull(result);
    }

    @Test
    public void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesntExist() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
           productService.findById(nonExistingId);
        });
    }

    @Test
    public void updateShouldReturnProductDtoWhenIdExists() {
        ProductDTO result = productService.update(existingId, productDTO);


        Assertions.assertNotNull(result);
    }

    @Test
    public void updateShouldThrowResourceNotFoundExceptionWhenIdDoesntExist() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            productService.update(nonExistingId, productDTO);
        });
    }


}
