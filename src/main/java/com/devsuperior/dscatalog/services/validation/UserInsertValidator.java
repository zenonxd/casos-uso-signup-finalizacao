package com.devsuperior.dscatalog.services.validation;

import java.util.ArrayList;
import java.util.List;

import com.devsuperior.dscatalog.controllers.exceptions.FieldMessage;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.devsuperior.dscatalog.dto.UserInsertDTO;
import com.devsuperior.dscatalog.entities.User;
import com.devsuperior.dscatalog.repositories.UserRepository;

public class UserInsertValidator implements ConstraintValidator<UserInsertValid, UserInsertDTO> {

    @Autowired
    private UserRepository repository;

    @Override
    public void initialize(UserInsertValid ann) {
    }

    @Override
    public boolean isValid(UserInsertDTO dto, ConstraintValidatorContext context) {

        List<FieldMessage> list = new ArrayList<>();

        // Coloque aqui seus testes de validação, acrescentando objetos FieldMessage à lista
        // pode colocar até um if por exemplo: if (x coisa for true) vai na list e insere um field
        // message nela.


        //exemplo: vendo se já existe email
        //importar UserRepository
        //criar findByEmail no UserRepository

        User user = repository.findByEmail(dto.getEmail());

        if (user != null) {
            list.add(new FieldMessage("email", "Email já existe"));
        }

        for (FieldMessage e : list) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage()).addPropertyNode(e.getFieldName())
                    .addConstraintViolation();
        }
        //verifica se ela está vazia
        // se estiver vazia, quer dizer que nenhum dos testes acima entrou nela (ou seja, n deu nenhum erro).
        return list.isEmpty();
    }
}